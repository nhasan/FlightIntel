#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

use strict;
use DBI;
use LWP::Simple;
use XML::Twig;
use Text::Autoformat;

my $reTrim = qr/^\s+|\s+$/;
my $dbfile = shift @ARGV;

my $STATIONS_FILE = "wx_stations.txt";
my $wx_url = "http://aviationweather.gov/adds/dataserver_current/httpparam?"
        ."dataSource=stations&requestType=retrieve&format=xml&stationString=~us,~ca";
my $count = 0;

sub capitalize($)
{
    my ( $string ) = @_;
    $string = autoformat( $string, { case => 'highlight' } );
    $string =~ s/$reTrim//g;
    return $string;
}

print "Downloading wx station data...";
$| = 1;
my $ret = getstore( $wx_url, $STATIONS_FILE );
if ( $ret != 200 )
{
    die "Unable to download station list";
}
print "done\n";
$| = 1;

my $dbh = DBI->connect( "dbi:SQLite:dbname=$dbfile", "", "" );

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

my $create_wxs_table = "CREATE TABLE wxs ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."STATION_ID TEXT, "
        ."STATION_WMO_ID TEXT, "
        ."STATION_NAME TEXT, "
        ."STATION_LATITUDE_DEGREES REAL, "
        ."STATION_LONGITUDE_DEGREES REAL, "
        ."STATION_STATE TEXT, "
        ."STATION_COUNTRY TEXT, "
        ."STATION_ELEVATION_METER INTEGER, "
        ."STATION_SITE_TYPES TEXT"
        .")";

my $insert_wxs_record = "INSERT INTO wxs ("
        ."STATION_ID, "
        ."STATION_WMO_ID, "
        ."STATION_NAME, "
        ."STATION_LATITUDE_DEGREES, "
        ."STATION_LONGITUDE_DEGREES, "
        ."STATION_STATE, "
        ."STATION_COUNTRY, "
        ."STATION_ELEVATION_METER, "
        ."STATION_SITE_TYPES"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?"
        .")";

$dbh->do( "DROP TABLE IF EXISTS wxs" );
$dbh->do( $create_wxs_table );
$dbh->do( "CREATE INDEX idx_wxs_station_id on wxs ( STATION_ID );" );
my $sth_wxs = $dbh->prepare( $insert_wxs_record );

my $twig= new XML::Twig( twig_handlers =>
                    { errors => \&errors,
                      warnings => \&warnings,
                      Station => \&station } );
$twig->parsefile( $STATIONS_FILE );

print "\rDone loading $count stations\n";

exit;

sub errors
{
    my( $twig, $errors )= @_;
    my $error_text = $errors->text;
    if ( length( $error_text ) > 0 )
    {
        die "ERROR: $error_text\n";
    }
}

sub warnings
{
    my( $twig, $warnings )= @_;
    my $warning_text = $warnings->text;
    if ( length( $warning_text ) > 0 )
    {
        die "WARNING: $warning_text\n";
    }
}

sub station
{
    my( $twig, $station ) = @_;

    my $site_type = $station->child( 0, "site_type" );
    my $site_types = "";
    if ( $site_type )
    {
        my @types = $site_type->children;
        foreach my $type ( @types )
        {
            if ( $type->name eq "METAR" || $type->name eq "TAF" )
            {
                if ( length( $site_types ) > 0 )
                {
                    $site_types .= " ";
                }
                $site_types .= $type->name;
            }
        }
    }

    if ( length( $site_types ) > 0 ) {
        my $station_id = $station->child_text( 0, "station_id" );
        my $wmo_id = $station->child_text( 0, "wmo_id" );
        my $site = capitalize( $station->child_text( 0, "site" ) );
        my $state = $station->child_text( 0, "state" );
        my $country = $station->child_text( 0, "country" );
        my $elevation_m = $station->child_text( 0, "elevation_m" );
        my $latitude = $station->child_text( 0, "latitude" );
        my $longitude = $station->child_text( 0, "longitude" );

        ++$count;

        print "\rLoading # $count...";

        #STATION_ID
        $sth_wxs->bind_param( 1, $station_id );
        #STATION_WMO_ID
        $sth_wxs->bind_param( 2, $wmo_id );
        #STATION_NAME
        $sth_wxs->bind_param( 3, $site );
        #STATION_LATITUDE_DEGREES
        $sth_wxs->bind_param( 4, $latitude );
        #STATION_LONGITUDE_DEGREES
        $sth_wxs->bind_param( 5, $longitude );
        #STATION_STATE
        $sth_wxs->bind_param( 6, $state );
        #STATION_COUNTRY
        $sth_wxs->bind_param( 7, $country );
        #STATION_ELEVATION_METER
        $sth_wxs->bind_param( 8, $elevation_m );
        #STATION_SITE_TYPES
        $sth_wxs->bind_param( 9, $site_types );

        $sth_wxs->execute;
    }

    $twig->purge;
}
