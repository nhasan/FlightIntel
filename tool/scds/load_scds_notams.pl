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

sub load_current_notam_ids($) {
    my $dbh = shift;
    
    my $sth = $dbh->prepare("SELECT id FROM notams;");
    $sth->execute or die "Can't execute statement: $DBI::errstr\n";

    while ( ( my $id ) = $sth->fetchrow_array )
    {
        $notams{$id} = 0;
    }

    my $size = scalar keys %notams;
    say "Loaded $size existing Notams.";
}

sub load_notams_from_file($$$) {
    my ($fh, $dbh, $sth_insert) = @_;

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
        if (exists $notams{$id}) {
            # This Notam already exists, skip it.
            $notams{$id}++;
            next;
        }

        my ($timeslice) = $xpc->findnodes(".//ns11:EventTimeSlice", $msg);
        my $effectiveStart = $xpc->findvalue(".//ns5:beginPosition", $timeslice) // "";
        my $effectiveEnd = $xpc->findvalue(".//ns5:endPosition", $timeslice) // "";
        my ($notam) = $xpc->findnodes(".//ns11:textNOTAM", $timeslice);

        my ($extension) = $xpc->findnodes(".//ns6:EventExtension", $msg);
        my $classification = $xpc->findvalue(".//ns6:classification", $extension) // "";
        my $lastUpdated = $xpc->findvalue(".//ns6:lastUpdated", $extension) // "";


        my $type = $xpc->findvalue(".//ns11:NOTAM/ns11:type", $notam);
        if ($type eq "C") {
            # This notam is cancelled, skip it.
            next;
        }
        my $series = $xpc->findvalue(".//ns11:series", $notam) // "";
        my $number = $xpc->findvalue(".//ns11:number", $notam);
        my $year = $xpc->findvalue(".//ns11:year", $notam);
        my $issued = $xpc->findvalue(".//ns11:issued", $notam);
        my $affectedFIR = $xpc->findvalue(".//ns11:affectedFIR", $notam) // "";
        my $selectionCode = $xpc->findvalue(".//ns11:selectionCode", $notam) // "";
        my $traffic = $xpc->findvalue(".//ns11:traffic", $notam) // "";
        my $purpose = $xpc->findvalue(".//ns11:purpose", $notam) // "";
        my $scope = $xpc->findvalue(".//ns11:scope", $notam) // "";
        my $minimumFL = $xpc->findvalue(".//ns11:minimumFL", $notam) // "";
        my $maximumFL = $xpc->findvalue(".//ns11:maximumFL", $notam) // "";
        my $coordinates = $xpc->findvalue(".//ns11:coordinates", $notam) // "";
        my $radius = $xpc->findvalue(".//ns11:radius", $notam) // "";
        my $location = $xpc->findvalue(".//ns11:location", $notam) // "";
        my $text = eval {
            if ($classification eq "DOM" or $classification eq "FDC") {
                $xpc->findvalue(".//ns11:simpleText", $notam);
            } else {
                $xpc->findvalue(".//ns11:text", $notam);
            }
        } // "";

        my $latittude = 0;
        my $longitude = 0;
        if ($coordinates =~ /$reCoordinates/) {
            $latittude = substr($coordinates, 0, 4)/100.0;
            if ( substr($coordinates, 4, 1) eq "S" )
            {
                $latittude *= -1;
            }
            $longitude = substr($coordinates, 5, 5)/100.0;
            if ( substr($coordinates, 10, 1) eq "W" )
            {
                $longitude *= -1;
            }
        }

        say "Inserting notam $id -> $location";

        $sth_insert->bind_param(1, $id);
        $sth_insert->bind_param(2, $series);
        $sth_insert->bind_param(3, $number);
        $sth_insert->bind_param(4, $year);
        $sth_insert->bind_param(5, $type);
        $sth_insert->bind_param(6, $issued);
        $sth_insert->bind_param(7, $lastUpdated);
        $sth_insert->bind_param(8, $effectiveStart);
        $sth_insert->bind_param(9, $effectiveEnd);
        $sth_insert->bind_param(10, $location);
        $sth_insert->bind_param(11, $affectedFIR);
        $sth_insert->bind_param(12, $selectionCode);
        $sth_insert->bind_param(13, $traffic);
        $sth_insert->bind_param(14, $purpose);
        $sth_insert->bind_param(15, $scope);
        $sth_insert->bind_param(16, $minimumFL);
        $sth_insert->bind_param(17, $maximumFL);
        $sth_insert->bind_param(18, $latittude);
        $sth_insert->bind_param(19, $longitude);
        $sth_insert->bind_param(20, $radius);
        $sth_insert->bind_param(21, $classification);
        $sth_insert->bind_param(22, $text);

        if (!$sth_insert->execute()) {
            say "Could not insert $id: $DBI::errstr";
        } else {
            ++$new;
        }
    }

    say "Inserted $new new Notams.";
}

sub delete_notams($$) {
    my ($dbh, $sth_delete) = @_;

    my @delete_ids = grep {$notams{$_} == 0} keys %notams;
    my $size = scalar @delete_ids;
    foreach my $id (@delete_ids) {
        say "Deleting Notam $id.";
        $sth_delete->bind_param(1, $id);
        $sth_delete->execute() or die "Could not delete $id: $DBI::errstr\n";
    }
    say "Deleted $size Notams.";
}

sub process_jms($$$) {
    my ($file, $dbh, $sth_insert_notam) = @_;
    if (open my $fh, "<", $file) {
        load_notams_from_file($fh, $dbh, $sth_insert_notam);
        close $fh || warn "close failed: $!";
        unlink $file
    } else {
        say "Unable to open $file: $!";
    }
}

sub process_fil($$$$) {
    my ($file, $dbh, $sth_insert_notam, $sth_delete_notam) = @_;
    if (open my $fh, '<:gzip', $file) {
        load_current_notam_ids($dbh);
        load_notams_from_file($fh, $dbh, $sth_insert_notam);
        delete_notams($dbh, $sth_delete_notam);
        unlink $file
    } else {
        say "Unable to open $file: $!";
    }
}

my $dbh = DBI->connect("dbi:SQLite:dbname=$outdir/$dbname", "", "");
my $tablename = "notams";
my $info = $dbh->table_info(undef, undef, $tablename)->fetchall_arrayref;
if (scalar @$info == 0) {
    say "Creating table $tablename.";
    my $create_notams_table = 
        "CREATE TABLE $tablename ( "
            . "id TEXT PRIMARY KEY, "
            . "series TEXT, "
            . "number INTEGER, "
            . "year INTEGER, "
            . "type TEXT, "
            . "issued TEXT, "
            . "lastUpdated TEXT, "
            . "effectiveStart TEXT, "
            . "effectiveEnd TEXT, "
            . "location TEXT, "
            . "affectedFIR TEXT, "
            . "selectionCode TEXT, "
            . "traffic TEXT, "
            . "purpose TEXT, "
            . "scope TEXT, "
            . "minimumFL INTEGER, "
            . "maximumFL INTEGER, "
            . "latittude REAL, "
            . "longitude REAL, "
            . "radius INTEGER, "
            . "classification TEXT, "
            . "text TEXT "
            . ")";
    $dbh->do($create_notams_table);
    $dbh->do("CREATE INDEX idx_location on notams ( location );");
}

my $insert_notams_row =
    "INSERT OR IGNORE INTO notams ("
        . "id, "
        . "series, "
        . "number, "
        . "year, "
        . "type, "
        . "issued, "
        . "lastUpdated, "
        . "effectiveStart, "
        . "effectiveEnd, "
        . "location, "
        . "affectedFIR, "
        . "selectionCode, "
        . "traffic, "
        . "purpose, "
        . "scope, "
        . "minimumFL, "
        . "maximumFL, "
        . "latittude, "
        . "longitude, "
        . "radius, "
        . "classification, "
        . "text"
        . ") VALUES ("
        . "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        . "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
        . ")";
my $sth_insert_notam = $dbh->prepare($insert_notams_row);

my $delete_notams_row = "DELETE FROM notams WHERE id=?";
my $sth_delete_notam = $dbh->prepare($delete_notams_row);

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

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
    process_fil($file, $dbh, $sth_insert_notam, $sth_delete_notam);
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
    process_jms($file, $dbh, $sth_insert_notam);
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
                    process_jms($file, $dbh, $sth_insert_notam);
                }
                elsif (rindex($file, $filpath, 0) == 0) {
                    process_fil($file, $dbh, $sth_insert_notam, $sth_delete_notam);
                }
            }
        }
    }
    sleep(3);
}
