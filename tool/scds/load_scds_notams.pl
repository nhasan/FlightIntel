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
#   libpath-tiny-perl
#   libperlio-gzip-perl
#   libxml-libxml-perl
#

use strict;
use warnings;
use v5.012;

use Config::Simple;
use DBI;
use Path::Tiny;
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
-e $outdir || path($outdir)->mkpath;

my $reCoordinates = qr/^\d{4}[NS]\d{5}[EW]\d{0,3}$/;

my $create_notams_table_sql = 
    "CREATE TABLE notams ( "
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
        . "xovernotamID TEXT, "
        . "xoveraccountID TEXT"
        . ")";

my $insert_notams_row_sql =
    "INSERT OR REPLACE INTO notams ("
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
        . "xovernotamID, "
	. "xoveraccountID"
        . ") VALUES ("
        . "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
        . "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
        . ")";

my $dbh = DBI->connect("dbi:SQLite:dbname=$outdir/$dbname", "", "");

sub create_notams_table() {
    say "Creating notams table.";
    $dbh->do($create_notams_table_sql);
    $dbh->do("CREATE INDEX idx_location on notams ( location );");
    $dbh->do("CREATE INDEX idx_notamID on notams (notamID);");
    $dbh->do("CREATE INDEX idx_xovernotamID on notams (xovernotamID);");
    $dbh->do("CREATE INDEX idx_xoveraccountID on notams (xoveraccountID);");
}

sub drop_notams_table() {
    say "Dropping notams table.";
    $dbh->do("DROP TABLE notams");
}

my $info = $dbh->table_info(undef, undef, "notams")->fetchall_arrayref;
if (scalar @$info == 0) {
    create_notams_table();
}

my $sth_insert_notam = $dbh->prepare($insert_notams_row_sql)
        or die "Can't prepare statement: $DBI::errstr\n";
my $sth_delete_notam_by_id = $dbh->prepare("DELETE FROM notams WHERE id=?1")
        or die "Can't prepare statement: $DBI::errstr\n";
my $sth_select_notam_by_notamid = $dbh->prepare("SELECT * FROM notams WHERE notamID=?1 and location=?2")
        or die "Can't prepare statement: $DBI::errstr\n";
my $sth_select_notam_by_xovernotamid = $dbh->prepare("SELECT * FROM notams WHERE notamID=?1 and xovernotamID=?2")
        or die "Can't prepare statement: $DBI::errstr\n";

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

# Turn off buffering on STDOUT
binmode(STDOUT, ":unix") || die "can't binmode STDOUT to :unix: $!";

sub load_notams_from_file($) {
    my ($fh) = @_;

    my $msg_pattern = XML::LibXML::Pattern->new('//ns13:AIXMBasicMessage',
        { "ns13" => "http://www.aixm.aero/schema/5.1/message" });

    my $new = 0;
    my $reader = XML::LibXML::Reader->new(IO => $fh);
    while ($reader->read) {
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
        my ($notam) = $xpc->findnodes(".//ns11:textNOTAM", $timeslice);
        my $series = $xpc->findvalue(".//ns11:series", $notam) // "";
        my $number = $xpc->findvalue(".//ns11:number", $notam) // 0;
        my $year = $xpc->findvalue(".//ns11:year", $notam) // 0;
        my $issued = $xpc->findvalue(".//ns11:issued", $notam);
        my $type = $xpc->findvalue(".//ns11:NOTAM/ns11:type", $notam) // "";
        my $text = $xpc->findvalue(".//ns11:text", $notam) // "";
        my $location = $xpc->findvalue(".//ns11:location", $notam) // "";
        my ($extension) = $xpc->findnodes(".//ns6:EventExtension", $msg);
        my $classification = $xpc->findvalue(".//ns6:classification", $extension) // "";
        my $lastUpdated = $xpc->findvalue(".//ns6:lastUpdated", $extension) // "";

        my $notamID = q{};
        if (rindex($series, "SW", 0) == 0) {
            # This is a SNOWTAM
            $notamID = $series.sprintf("%04s", $number);
        } elsif ($classification eq "INTL" or $classification eq "LMIL" or $classification eq "MIL") {
            $notamID = $series.sprintf("%04s", $number)."/".substr($year, 2, 2);
        } elsif ($classification eq "DOM") {
            $notamID = substr($issued, 5, 2)."/".sprintf("%03s", $number);
        } elsif ($classification eq "FDC") {
            $notamID = substr($issued, 3, 1)."/".sprintf("%04s", $number);
        }

        if ($type eq "N" or $type eq "") {
            say "Inserting ($notamID) ($location) ($id)";
        } elsif ($type eq "R") {
            my $row = get_notam_by_notamid($notamID, $location);
            if (defined $row) {
                if ($lastUpdated gt $row->{lastUpdated}) {
                    say "Replacing ($notamID) ($location) ($id)";
                } else {
                    # We already have a more recent record
                    say "Skipping replace ($notamID) ($location) ($id)";
                    next;
                }
            } else {
                say "Inserting ($notamID) ($location) ($id)";
            }
        } elsif ($type eq "C") {
                # We got a cancel event
                my $cancelID;
                if ($classification eq "FDC") {
                    $text = $xpc->findvalue(".//ns11:simpleText", $notam) // "";
                    my @tokens = split(/\s/, $text, 7);
                    if (scalar @tokens >= 6 and $tokens[3] eq "CANCEL") {
                        $cancelID = $tokens[4];
                        $location = $tokens[5];
                    }    
                } else {
                    my @tokens = split(/\s/, $text, 4);
                    if (scalar @tokens >= 3 and $tokens[1] eq "NOTAMC") {
                        $cancelID = $tokens[2];
                    } elsif (scalar @tokens >= 2 and $tokens[1] eq "NOTAMN") {
                        $cancelID = $tokens[0];
                    }
                }
                if (length $cancelID and length $location) {
                    my $row = get_notam_by_notamid($cancelID, $location);
                    if ($row) {
                        if (length $row->{xovernotamID}) {
                            my $xover = get_notam_by_xovernotamid($row->{xovernotamID}, $cancelID);
                            if ($xover) {
                                say "Deleting* ($row->{xovernotamID}) ($location) ($xover->{id})";
                                delete_notam_by_id($xover->{id});
                            }
                        }
                        say "Deleting ($cancelID) ($location) ($row->{id})";
                        delete_notam_by_id($row->{id});
                    } else {
                        say "Skipping cancel ($cancelID) ($location) ($id)";
                    }
                } else {
                    say "Unknown CANCEL format => " . (split /\n/, $text )[0];
                }
                next;
        }

        my $effectiveStart = $xpc->findvalue(".//ns5:beginPosition", $timeslice) // "";
        my $effectiveEnd = $xpc->findvalue(".//ns5:endPosition", $timeslice) // "";
        my $estimatedEnd = (($xpc->findvalue(q{.//ns5:endPosition/@indeterminatePosition},
            $timeslice) // "") eq "unknown")? "Y" : "N";
        my $xovernotamID = $xpc->findvalue(".//ns6:xovernotamID", $extension) // "";
        my $xoveraccountID = $xpc->findvalue(".//ns6:xoveraccountID", $extension) // "";
        my $icaoLocation = $xpc->findvalue(".//ns6:icaoLocation", $extension) // "";

        my $affectedFIR = $xpc->findvalue(".//ns11:affectedFIR", $notam) // "";
        my $selectionCode = $xpc->findvalue(".//ns11:selectionCode", $notam) // "";
        my $traffic = $xpc->findvalue(".//ns11:traffic", $notam) // "";
        my $purpose = $xpc->findvalue(".//ns11:purpose", $notam) // "";
        my $scope = $xpc->findvalue(".//ns11:scope", $notam) // "";
        my $minimumFL = $xpc->findvalue(".//ns11:minimumFL", $notam) // 0;
        my $maximumFL = $xpc->findvalue(".//ns11:maximumFL", $notam) // 999;
        my $coordinates = $xpc->findvalue(".//ns11:coordinates", $notam) // "";
        my $radius = $xpc->findvalue(".//ns11:radius", $notam) // 0;
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

        if ($sth_insert_notam->execute(
                $id, $notamID, $series, $number, $year, $type, $issued, $lastUpdated,
                $effectiveStart, $effectiveEnd, $estimatedEnd, $location, $icaoLocation,
                $affectedFIR, $selectionCode, $traffic, $purpose, $scope, $minimumFL,
                $maximumFL, $latitude,  $longitude, $radius, $classification, $schedule,
                $text, $xovernotamID, $xoveraccountID)) {
            ++$new;
        } else {
            say "Could not insert $id: $DBI::errstr";
        }
    }

    $reader->finish;

    if ($new > 1) {
        say "Inserted $new Notams.";
    }
}

sub get_notam_by_notamid($$) {
    my ($notamID, $location) = @_;
    $sth_select_notam_by_notamid->execute($notamID, $location)
            or die "Can't execute statement: $DBI::errstr\n";
    return $sth_select_notam_by_notamid->fetchrow_hashref;
}

sub get_notam_by_xovernotamid($$) {
    my ($notamID, $xovernotamID) = @_;
    $sth_select_notam_by_xovernotamid->execute($notamID, $xovernotamID)
            or die "Can't execute statement: $DBI::errstr\n";
    return $sth_select_notam_by_xovernotamid->fetchrow_hashref;
}

sub delete_notam_by_id($) {
    my ($id) = @_;
    $sth_delete_notam_by_id->execute($id)
            or die "Can't execute statement: $DBI::errstr\n";
}

sub process_jms($) {
    my ($file) = @_;
    if (open my $fh, "<", $file) {
        load_notams_from_file($fh);
        close $fh || warn "close failed: $!";
        unlink $file or die "Can't unlink $file: $!";
    } else {
        say "Unable to open $file: $!";
    }
}

sub process_fil($) {
    my ($file) = @_;
    if (open my $fh, '<:gzip', $file) {
        drop_notams_table();
        create_notams_table();
        load_notams_from_file($fh);
        close $fh || warn "close failed: $!";
        unlink $file or die "Can't unlink $file: $!";
    } else {
        say "Unable to open $file: $!";
    }
}

say 'Starting NOTAM loop...';

while (1) {
    sleep(3);

    my @fil_files = map { $_->stringify } path($filpath)->children();
    next if grep { /^lock$/ } @fil_files;
    foreach my $file (@fil_files) {
        process_fil($file);
    }

    my @jms_files = map { $_->stringify } path($jmspath)->children(qr/\d+/);
    foreach my $file (@jms_files) {
        process_jms($file);
    }
}
