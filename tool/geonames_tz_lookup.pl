#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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
use JSON;

my $dbfile = "fadds.db";
my $FADDS_BASE = shift @ARGV;

my $site_number;
my $latitude;
my $longitude;
my $tz;

my $geonames_url = "http://api.geonames.org/timezoneJSON?";

my $dbh = DBI->connect( "dbi:SQLite:dbname=$dbfile", "", "" );

my $update_airports_tz = "update airports set TIMEZONE_ID=? where SITE_NUMBER=?";
my $sth_upd = $dbh->prepare( $update_airports_tz );

my $sth = $dbh->prepare( 
            "SELECT SITE_NUMBER, REF_LATTITUDE_DEGREES, REF_LONGITUDE_DEGREES, TIMEZONE_ID"
            ." FROM airports" ) or die "Can't prepare statement: $DBI::errstr\n";
$sth->execute or die "Can't execute statement: $DBI::errstr\n";

my $row = 0;

while ( ( $site_number, $latitude, $longitude, $tz ) = $sth->fetchrow_array )
{
    if ( length( $tz ) > 0 )
    {
        print "Skipping row=[$row], site=[$site_number], lat=[$latitude], lon=[$longitude]\n";
        print "timezone=[$tz]\n";
        ++$row;
        next;
    }
    print "Processing row=[$row], site=[$site_number], lat=[$latitude], lon=[$longitude]\n";
    my $json_text = get $geonames_url."lat=$latitude&lng=$longitude&username=nhasan";
    my $json = decode_json( $json_text );
    $tz = $json->{timezoneId};
    if ( length( $tz ) == 0 )
    {
        print $json->{status}{message}."\n";
        next;
    }
    print "timezone=[$tz]\n";
    $sth_upd->bind_param( 1, $json->{timezoneId} );
    $sth_upd->bind_param( 2, $site_number );
    $sth_upd->execute();
    sleep( 2 );
    ++$row;
}

$dbh->disconnect();

exit;
