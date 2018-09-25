#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2018 Nadeem Hasan <nhasan@nadmm.com>
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
use Text::Autoformat;

my $reTrim = qr/^\s+|\s+$/;

sub substrim($$$)
{
    my ( $string, $offset, $len ) = @_;
    $string = substr( $string, $offset, $len );
    $string =~ s/$reTrim//g;
    return $string;
}

my $DOF_BASE = "/home/nhasan/Documents/FlightIntel/DOF";
my $cycle = shift @ARGV;
my $dbfile = "dof_$cycle.db";
$DOF_BASE = $DOF_BASE."/DOF_".$cycle;

my $dbh = DBI->connect( "dbi:SQLite:dbname=$dbfile", "", "" );

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

my $create_metadata_table = "CREATE TABLE android_metadata ( locale TEXT );";
my $insert_metadata_record = "INSERT INTO android_metadata VALUES ( 'en_US' );";

$dbh->do( "DROP TABLE IF EXISTS android_metadata" );
$dbh->do( $create_metadata_table );
$dbh->do( $insert_metadata_record );

my $create_dof_table = "create table dof ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."OAS_CODE TEXT, "
	."VERIFICATION_STATUS TEXT, "
	."LATITUDE_DEGREES REAL, "
	."LONGITUDE_DEGREES REAL, "
	."OBSTACLE_TYPE TEXT, "
	."COUNT INTEGER, "
	."HEIGHT_AGL INTEGER, "
	."HEIGHT_MSL INTEGER, "
	."LIGHTING_TYPE TEXT, "
	."ACCURACY_HOR TEXT, "
	."ACCURACY_VER TEXT, "
	."MARKING_TYPE TEXT, "
	."ACTION_CODE TEXT, "
	."ACTION_DATE TEXT"
	.");";

my $insert_dof_record = "INSERT INTO dof ("
        ."OAS_CODE, "
	."VERIFICATION_STATUS, "
	."LATITUDE_DEGREES, "
	."LONGITUDE_DEGREES, "
	."OBSTACLE_TYPE, "
	."COUNT, "
	."HEIGHT_AGL, "
	."HEIGHT_MSL, "
	."LIGHTING_TYPE, "
	."ACCURACY_HOR, "
	."ACCURACY_VER, "
	."MARKING_TYPE, "
	."ACTION_CODE, "
	."ACTION_DATE"
	.") VALUES ("
	."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
	.")";

$dbh->do( "DROP TABLE IF EXISTS dof" );
$dbh->do( $create_dof_table );

my $sth_dof = $dbh->prepare( $insert_dof_record );

my $ofh = select STDOUT;
$| = 1;
select $ofh;

my $DOF = $DOF_BASE."/DOF.DAT\n";
print( "Loading ".$DOF );

open( DOF_FILE, "<$DOF" ) or die "Could not open data file\n";

my $i = 0;

while ( $i < 4 )
{
    # Skip the 4 header lines
    ++$i;
    my $header = <DOF_FILE>
}

$i = 0;
while ( my $line = <DOF_FILE> )
{
    ++$i;

    if ( ($i % 1000) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    #OAS_CODE
    $sth_dof->bind_param( 1, substrim( $line, 0, 9 ) );
    #VERIFICATION_STATUS
    $sth_dof->bind_param( 2, substrim( $line, 10, 1 ) );
    #LATITUDE_DEGREES
    my $deg = substrim( $line, 35, 2 );
    my $min = substrim( $line, 38, 2 );
    my $sec = substrim( $line, 41, 5 );
    my $latitude = $deg+$min/60+$sec/3600;
    if ( substr( $line, 46, 1 ) eq "S" )
    {
        $latitude *= -1;
    }
    $sth_dof->bind_param( 3, $latitude );
    #LONGITUDE_DEGREES
    $deg = substrim( $line, 48, 3 );
    $min = substrim( $line, 52, 2 );
    $sec = substrim( $line, 55, 5 );
    my $longitude = $deg+$min/60+$sec/3600;
    if ( substr( $line, 60, 1 ) eq "W" )
    {
        $longitude *= -1;
    }
    $sth_dof->bind_param( 4, $longitude );
    #OBSTACLE_TYPE
    $sth_dof->bind_param( 5, substrim( $line, 62, 17 ) );
    #COUNT
    $sth_dof->bind_param( 6, substrim( $line, 81, 1 ) );
    #HEIGHT_AGL
    $sth_dof->bind_param( 7, substrim( $line, 83, 5 ) );
    #HEIGHT_MSL
    $sth_dof->bind_param( 8, substrim( $line, 89, 5 ) );
    #LIGHTING_TYPE
    $sth_dof->bind_param( 9, substrim( $line, 95, 1 ) );
    #ACCURACY_HOR
    $sth_dof->bind_param( 10, substrim( $line, 97, 1 ) );
    #ACCURACY_VER
    $sth_dof->bind_param( 11, substrim( $line, 99, 1 ) );
    #MARKING_TYPE
    $sth_dof->bind_param( 12, substrim( $line, 101, 1 ) );
    #ACTION_CODE
    $sth_dof->bind_param( 13, substrim( $line, 118, 1 ) );
    #ACTION_DATE
    $sth_dof->bind_param( 14, substrim( $line, 120, 7 ) );

    $sth_dof->execute();

    if ( ($i % 1000) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }

}

print( "\rFinished processing $i records.\n" );

close DOF_FILE;

$dbh->disconnect();

exit;

