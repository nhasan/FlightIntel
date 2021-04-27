#! /usr/bin/perl

#/*
# * FlightIntel
# *
# * Copyright 2021 Nadeem Hasan <nhasan@nadmm.com>
# *
# * This program is free software: you can redistribute it and/or modify
# * it under the terms of the GNU General Public License as published by
# * the Free Software Foundation, either version 3 of the License, or
# * (at your option) any later version.
# *
# * This program is distributed in the hope that it will be useful,
# * but WITHOUT ANY WARRANTY; without even the implied warranty of
# * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# * GNU General Public License for more details.
# *
# * You should have received a copy of the GNU General Public License
# * along with this program.  If not, see <http://www.gnu.org/licenses/>.
# */

# Ubuntu package dependencies
#   libconfig-simple-perl
#   libdbi-perl
#   libdbd-sqlite3-perl
#   libfile-monitor-perl
#   libperlio-gzip-perl
#   libxml-libxml-perl
#

use strict;
use warnings;
use v5.10;

use Config::Simple;
use DBI;
use File::Monitor;
use File::Path qw(make_path);
use XML::LibXML::Reader;

my $cfgfile = shift or die "Missing config file parameter.";
-f $cfgfile or die "Config file not found.";
my $cfg = new Config::Simple($cfgfile) or die Config::Simple->error();

my $jmspath = $cfg->param("JMS.outdir") or die "Missing config 'JMS.outdir'.";
my $filpath = $cfg->param("FIL.outdir") or die "Missing config 'FIL.outdir'.";
my $outdir = $cfg->param("NOTAM.outdir") or die "Missing config 'NOTAM.outdir'.";
my $dbname = $cfg->param("NOTAM.dbname") or die "Missing config 'NOTAM.dbname'.";
my $watch = $cfg->param("NOTAM.watch") // 0;

# Create the output directories if missing
-e $outdir || make_path($outdir);

my %notams = ();
my $reCoordinates = qr/^\d{4}[NS]\d{5}[EW]\d{0,3}$/;

my $dbh = DBI->connect("dbi:SQLite:dbname=$outdir/$dbname", "", "");
my $tablename = "notams";
my $info = $dbh->table_info(undef, undef, $tablename)->fetchall_arrayref;
if (scalar @$info == 0) {
    say "Creating table $tablename.";
    my $create_notams_table = 
        "CREATE TABLE $tablename ( "
            . "id TEXT PRIMARY KEY, "
            . "notamID TEXT, "
            . "series TEXT, "
            . "number INTEGER, "
            . "year INTEGER, "
            . "type TEXT, "
            . "issued TEXT, "
            . "lastUpdated TEXT, "
            . "effectiveStart TEXT, "
            . "effectiveEnd TEXT, "
            . "estimatedEnd TEXT, "
            . "location TEXT, "
            . "icaoLocation TEXT, "
            . "affectedFIR TEXT, "
            . "selectionCode TEXT, "
            . "traffic TEXT, "
            . "purpose TEXT, "
            . "scope TEXT, "
            . "minimumFL INTEGER, "
            . "maximumFL INTEGER, "
            . "latitude REAL, "
            . "longitude REAL, "
            . "radius INTEGER, "
            . "classification TEXT, "
            . "schedule TEXT, "
            . "text TEXT, "
            . "xovernotamID TEXT"
            . ")";
    $dbh->do($create_notams_table);
    $dbh->do("CREATE INDEX idx_location on notams ( location );");
    $dbh->do("CREATE INDEX idx_notamID on notams (notamID);");
}

my $insert_notams_row =
    "INSERT OR IGNORE INTO notams ("
        . "id, "
        . "notamID, "
        . "series, "
        . "number, "
        . "year, "
        . "type, "
        . "issued, "
        . "lastUpdated, "
        . "effectiveStart, "
        . "effectiveEnd, "
        . "estimatedEnd, "
        . "location, "
        . "icaoLocation, "
        . "affectedFIR, "
        . "selectionCode, "
        . "traffic, "
        . "purpose, "
        . "scope, "
        . "minimumFL, "
        . "maximumFL, "
        . "latitude, "
        . "longitude, "
        . "radius, "
        . "classification, "
        . "schedule, "
        . "text, "
	    . "xovernotamID"
        . ") VALUES ("
        . "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
        . "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
        . ")";
my $sth_insert_notam = $dbh->prepare($insert_notams_row);

my $delete_notams_row = "DELETE FROM notams WHERE id=?";
my $sth_delete_notam = $dbh->prepare($delete_notams_row);

my $select_notams_row = "SELECT * FROM notams WHERE id=?";
my $sth_select_notam = $dbh->prepare($select_notams_row);

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

sub load_current_notam_ids() {
    my $sth = $dbh->prepare("SELECT id FROM notams;");
    $sth->execute or die "Can't execute statement: $DBI::errstr\n";

    while ( ( my $id ) = $sth->fetchrow_array )
    {
        $notams{$id} = 0;
    }

    my $size = scalar keys %notams;
    say "Loaded $size existing Notams.";
}

sub load_notams_from_file($) {
    my ($fh) = @_;

    my $msg_pattern = XML::LibXML::Pattern->new('//ns13:AIXMBasicMessage',
        { "ns13" => "http://www.aixm.aero/schema/5.1/message" });

    my $new = 0;
    my $reader = XML::LibXML::Reader->new(IO => $fh);
    while ( 1 ) {
        my $result = eval { $reader->read };
        last unless ($result);
        next unless $reader->matchesPattern($msg_pattern);
        my $msg = $reader->copyCurrentNode(1);
        $reader->next;
        my $xpc = XML::LibXML::XPathContext->new($msg);
        $xpc->registerNs("ns1", "http://www.opengis.net/ows/1.1");
        $xpc->registerNs("ns2", "http://www.w3.org/1999/xlink");
        $xpc->registerNs("ns3", "http://www.opengis.net/wfs/2.0");
        $xpc->registerNs("ns4", "http://www.opengis.net/fes/2.0");
        $xpc->registerNs("ns5", "http://www.opengis.net/gml/3.2");
        $xpc->registerNs("ns6", "http://www.aixm.aero/schema/5.1/extensions/FAA/FNSE");
        $xpc->registerNs("ns7", "http://www.isotc211.org/2005/gco");
        $xpc->registerNs("ns8", "http://www.isotc211.org/2005/gmd");
        $xpc->registerNs("ns9", "http://www.aixm.aero/schema/5.1");
        $xpc->registerNs("ns10", "http://www.isotc211.org/2005/gts");
        $xpc->registerNs("ns11", "http://www.aixm.aero/schema/5.1/event");
        $xpc->registerNs("nns12", "urn:us.gov.dot.faa.aim.fns");
        $xpc->registerNs("ns13", "http://www.aixm.aero/schema/5.1/message");
        $xpc->registerNs("ns14", "http://www.opengis.net/wfs-util/2.0");

        my $id = $xpc->findvalue('./@ns5:id');
        my ($timeslice) = $xpc->findnodes(".//ns11:EventTimeSlice", $msg);
        my $effectiveStart = $xpc->findvalue(".//ns5:beginPosition", $timeslice) // "";
        my $effectiveEnd = $xpc->findvalue(".//ns5:endPosition", $timeslice) // "";
        my $estimatedEnd = (($xpc->findvalue(q{.//ns5:endPosition/@indeterminatePosition},
            $timeslice) // "") eq "unknown")? "Y" : "N";
        my ($extension) = $xpc->findnodes(".//ns6:EventExtension", $msg);
        my $classification = $xpc->findvalue(".//ns6:classification", $extension) // "";
        my $lastUpdated = $xpc->findvalue(".//ns6:lastUpdated", $extension) // "";
        my $xovernotamID = $xpc->findvalue(".//ns6:xovernotamID", $extension) // "";
        my $icaoLocation = $xpc->findvalue(".//ns6:icaoLocation", $extension) // "";

        my ($notam) = $xpc->findnodes(".//ns11:textNOTAM", $timeslice);
        my $type = $xpc->findvalue(".//ns11:NOTAM/ns11:type", $notam) // "";
        my $text = $xpc->findvalue(".//ns11:text", $notam) // "";

        if ($type eq "R") {
            if (exists $notams{$id}) {
                my $row = get_notam($id);
                say "$id => $type => $lastUpdated => $row->{lastUpdated}";
                if ($lastUpdated gt $row->{lastUpdated}) {
                    # We got a replace event with an updated record
                    say "Replacing => $id ($icaoLocation)";
                    delete_notam($id);
                    delete $notams{$id};
                } else {
                    # We already have a more recent record
                    next;
                }
            } 
        } elsif ($type eq "C") {
                # We got a cancel event
                my @tokens = split(/\s/, $text, 4);
                my $cancelID = $tokens[2];
                if ($cancelID && exists $notams{$cancelID}) {
                    say "Deleting => $cancelID ($icaoLocation)";
                    delete_notam($cancelID);
                }
                next;
        }

        my $series = $xpc->findvalue(".//ns11:series", $notam) // "";
        my $number = $xpc->findvalue(".//ns11:number", $notam) // 0;
        my $year = $xpc->findvalue(".//ns11:year", $notam) // 0;
        my $issued = $xpc->findvalue(".//ns11:issued", $notam);
        my $affectedFIR = $xpc->findvalue(".//ns11:affectedFIR", $notam) // "";
        my $selectionCode = $xpc->findvalue(".//ns11:selectionCode", $notam) // "";
        my $traffic = $xpc->findvalue(".//ns11:traffic", $notam) // "";
        my $purpose = $xpc->findvalue(".//ns11:purpose", $notam) // "";
        my $scope = $xpc->findvalue(".//ns11:scope", $notam) // "";
        my $minimumFL = $xpc->findvalue(".//ns11:minimumFL", $notam) // 0;
        my $maximumFL = $xpc->findvalue(".//ns11:maximumFL", $notam) // 999;
        my $coordinates = $xpc->findvalue(".//ns11:coordinates", $notam) // "";
        my $radius = $xpc->findvalue(".//ns11:radius", $notam) // 0;
        my $location = $xpc->findvalue(".//ns11:location", $notam) // "";
        my $schedule = $xpc->findvalue(".//ns11:schedule", $notam) // "";

        my $latitude = 0;
        my $longitude = 0;
        if ($coordinates =~ /$reCoordinates/) {
            $latitude = substr($coordinates, 0, 4)/100.0;
            if ( substr($coordinates, 4, 1) eq "S" )
            {
                $latitude *= -1;
            }
            $longitude = substr($coordinates, 5, 5)/100.0;
            if ( substr($coordinates, 10, 1) eq "W" )
            {
                $longitude *= -1;
            }
        }

        my $notamID = q{};
        if ($classification eq "INTL" or $classification eq "LMIL" or $classification eq "MIL") {
            $notamID = $series.sprintf("%04s", $number)."/".substr($year, 2, 2);
        } elsif ($classification eq "DOM") {
            $notamID = substr($issued, 5, 2)."/".sprintf("%03s", $number);
        } elsif ($classification eq "FDC") {
            $notamID = substr($issued, 3, 1)."/".sprintf("%04s", $number);
        }

        $notams{$id} = 1;

        #say "Inserting notam $id -> $location";

        if (!$sth_insert_notam->execute(
                ($id, $notamID, $series, $number, $year, $type, $issued, $lastUpdated,
                $effectiveStart, $effectiveEnd, $estimatedEnd, $location, $icaoLocation,
                $affectedFIR, $selectionCode, $traffic, $purpose, $scope, $minimumFL,
                $maximumFL, $latitude,  $longitude, $radius, $classification, $schedule,
                $text, $xovernotamID)
                )) {
            say "Could not insert $id: $DBI::errstr";
        } else {
            ++$new;
        }
    }

    say "Inserted $new new Notams.";
}

sub get_notam($) {
    my ($id) = @_;
    $sth_select_notam->execute(($id)) or die "Can't execute statement: $DBI::errstr\n";
    return $sth_select_notam->fetchrow_hashref;
}

sub delete_notam($) {
    my ($id) = @_;
    $sth_delete_notam->execute(($id)) or die "Could not delete $id: $DBI::errstr\n";
}

sub delete_notams() {
    my @delete_ids = grep {$notams{$_} == 0} keys %notams;
    my $size = scalar @delete_ids;
    foreach my $id (@delete_ids) {
        say "Deleting Notam $id.";
        delete_notam($id);
    }
    say "Deleted $size Notams.";
}

sub process_jms($) {
    my ($file) = @_;
    if (open my $fh, "<", $file) {
        load_notams_from_file($fh);
        close $fh || warn "close failed: $!";
        unlink $file
    } else {
        say "Unable to open $file: $!";
    }
}

sub process_fil($) {
    my ($file) = @_;
    if (open my $fh, '<:gzip', $file) {
        load_current_notam_ids();
        load_notams_from_file($fh);
        delete_notams();
        unlink $file
    } else {
        say "Unable to open $file: $!";
    }
}

my $monitor = File::Monitor->new();

if ($watch) {
    say "Watching $jmspath";
    $monitor->watch( { name => "$jmspath", files => 1 } );
    say "Watching $filpath";
    $monitor->watch( { name => "$filpath", files => 1 } );
    $monitor->scan;
}

# Process existing NOTAM files first
say "Checking existing FIL files.";
opendir(DIR, $filpath) or die "can't opendir $filpath: $!";
while (defined(my $file = readdir(DIR))) {
    next if $file =~ /^\.\.?$/;
    $file = "$filpath/$file";
    say "$file was found.";
    process_fil($file);
}
closedir(DIR);

# Process existing NOTAM files first
say 'Checking existing JMS files.';
opendir(DIR, $jmspath) or die "can't opendir $jmspath: $!";
while (defined(my $file = readdir(DIR))) {
    next if $file =~ /^\.\.?$/;
    next if $file =~ /^messages\.log$/;
    $file = "$jmspath/$file";
    say "$file was found.";
    process_jms($file);
}
closedir(DIR);

while ($watch) {
    my @changes = $monitor->scan;
    if (scalar @changes) {
        # Wait a little to make sure files are completely written to disk
        sleep(1);
        for my $change (@changes) {
            for my $file ($change->files_created) {
                next if $file =~ /.*\/messages\.log$/;
                say "$file was created.";
                if (rindex($file, $jmspath, 0) == 0) {
                    process_jms($file);
                }
                elsif (rindex($file, $filpath, 0) == 0) {
                    process_fil($file);
                }
            }
        }
    }
    sleep(3);
}
