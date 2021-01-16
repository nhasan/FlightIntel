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
#   libnet-sftp-foreign-perl
#   libperlio-gzip-perl
#   libxml-libxml-perl
#

use strict;
use warnings;
use v5.10;

use Config::Simple;
use DBI;
use IO::Uncompress::Gunzip qw(gunzip $GunzipError);
use Net::SFTP::Foreign;
use XML::LibXML;

my $cfg = new Config::Simple("load_notam_fil.cfg");

my $host = $cfg->param("SWIM.host");
my $user = $cfg->param("SWIM.user");
my $timestampfile = $cfg->param("SWIM.timestampfile");
my $datafile = $cfg->param("SWIM.datafile");
my $localtimestamp = $cfg->param("FIL.timestamp");
my $dbname = $cfg->param("OUTPUT.dbname");

my $tablename = "notams";
#my $aixm_file = "initial_load_aixm.xml";
my $aixm_file = "sample_fil_aixm.xml";

my $reCoordinates = qr/^\d{4}[NS]\d{5}[EW]$/;

my $dbh = DBI->connect("dbi:SQLite:dbname=$dbname", "", "");

my $info = $dbh->table_info(undef, undef, $tablename)->fetchall_arrayref;
if (scalar @$info == 0) {
    print "Creating table $tablename.\n";
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
            . "icaoLocation TEXT, "
            . "airportname TEXT, "
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
    $dbh->do("CREATE INDEX idx_icaoLocation on notams ( icaoLocation );");
}

if (0) {

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

print "Connecting to $user\@$host...\n";

my $sftp = Net::SFTP::Foreign->new($host, user => $user, queue_size => 1);
$sftp->error and die "SFTP failed: " . $sftp->error;

print "Created SFTP session.\n";

my $remotetimestamp = $sftp->get_content($timestampfile)
    or die "$timestampfile failed: " . $sftp->error;
chomp($remotetimestamp);

if ($localtimestamp ne $remotetimestamp) {
    print "New data file found on the server.\n";

    #print "Fetching data file $datafile\n";
    #$sftp->get($datafile, $datafile)
    #    or die "$datafile failed: " . $sftp->error;

    my $output = $datafile;
    $output =~ s/\.gz$//;
    print "Uncompressing to $output\n";
    gunzip $datafile => $output
        or die "Error uncompressing: $GunzipError\n";

    $cfg->param(-block => 'FIL', -values => {'timestamp' => $remotetimestamp});
    $cfg->save();
}

print "Closing SFTP session.\n";
undef $sftp;
}

my $dom = eval {
    XML::LibXML->load_xml(location => $aixm_file, no_blanks => 1);
};
if ( $@ ) {
    print "Error parsing $aixm_file: $@\n";
    exit 1;
}

my $xpc = XML::LibXML::XPathContext->new($dom);
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

foreach my $msg ($xpc->findnodes("//ns13:AIXMBasicMessage")) {
    my $id = $msg->getAttribute("ns5:id");
    my ($timeslice) = $xpc->findnodes(".//ns11:EventTimeSlice", $msg);
    my ($notam) = $xpc->findnodes(".//ns11:textNOTAM/ns11:NOTAM", $timeslice);
    my ($series) = $xpc->findvalue(".//ns11:series", $notam) // "";
    my ($number) = $xpc->findvalue(".//ns11:number", $notam);
    my ($year) = $xpc->findvalue(".//ns11:year", $notam);
    my ($type) = $xpc->findvalue(".//ns11:type", $notam);
    my ($issued) = $xpc->findvalue(".//ns11:issued", $notam);
    my ($affectedFIR) = $xpc->findvalue(".//ns11:affectedFIR", $notam) // "";
    my ($selectionCode) = $xpc->findvalue(".//ns11:selectionCode", $notam) // "";
    my ($traffic) = $xpc->findvalue(".//ns11:traffic", $notam) // "";
    my ($purpose) = $xpc->findvalue(".//ns11:purpose", $notam) // "";
    my ($scope) = $xpc->findvalue(".//ns11:scope", $notam) // "";
    my ($minimumFL) = $xpc->findvalue(".//ns11:minimumFL", $notam) // "";
    my ($maximumFL) = $xpc->findvalue(".//ns11:maximumFL", $notam) // "";
    my ($coordinates) = $xpc->findvalue(".//ns11:coordinates", $notam) 
            if $xpc->exists(".//ns11:coordinates", $notam);
    if ( $id eq "FNS_ID_58381588" ) {
        say $id." ".$xpc->exists(".//ns11:coordinates", $notam);
    }
    $coordinates //= "";
    my ($radius) = $xpc->findvalue(".//ns11:radius", $notam) // "";
    my ($text) = $xpc->findvalue(".//ns11:text", $notam) // "";

    my ($effectiveStart) = $xpc->findvalue(".//ns5:beginPosition", $timeslice) // "";
    my ($effectiveEnd) = $xpc->findvalue(".//ns5:endPosition", $timeslice) // "";

    my ($extension) = $xpc->findnodes(".//ns6:EventExtension", $msg);
    my ($classification) = $xpc->findvalue(".//ns6:classification", $extension) // "";
    my ($icaoLocation) = $xpc->findvalue(".//ns6:icaoLocation", $extension) // "";
    my ($lastUpdated) = $xpc->findvalue(".//ns6:lastUpdated", $extension) // "";
    my ($airportname) = $xpc->findvalue(".//ns6:airportname", $extension) // "";

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
    } else {
        say "Bad format: $id" if length($coordinates) > 100;
    }

    #say "$id, $series, $number, $year, $type, $issued, $affectedFIR, $selectionCode, $traffic, $purpose, $scope, ";
    say "$id, $series, $number, $year, $type, $issued, $affectedFIR, $selectionCode, $traffic, $purpose, $scope, "
        ."$minimumFL, $maximumFL, $latittude, $longitude, $radius, $effectiveStart, $effectiveEnd, $classification, "
        ."$icaoLocation, $airportname, $lastUpdated";
}

$dbh->disconnect();
