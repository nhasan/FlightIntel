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

my $BASE_DIR = shift @ARGV;
my $dbfile = shift @ARGV;
my $ATC_PHONES_FILE = "$BASE_DIR/atc_phones.csv";

open( FILE, "<$ATC_PHONES_FILE" ) or die "Could not open data file\n";

my $dbh = DBI->connect( "dbi:SQLite:dbname=$BASE_DIR/$dbfile", "", "" );

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

my $create_atcphones_table = "CREATE TABLE atcphones ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."FACILITY_ID TEXT, "
        ."FACILITY_TYPE TEXT, "
        ."DUTY_OFFICE_PHONE TEXT, "
        ."BUSINESS_HOURS TEXT, "
        ."BUSINESS_PHONE TEXT"
        .");";

my $insert_atcphones_record = "INSERT INTO atcphones ("
        ."FACILITY_ID, "
        ."FACILITY_TYPE, "
        ."DUTY_OFFICE_PHONE, "
        ."BUSINESS_HOURS, "
        ."BUSINESS_PHONE"
        .") VALUES ("
        ."?, ?, ?, ?, ?"
        .");";

$dbh->do( "DROP TABLE IF EXISTS atcphones" );
$dbh->do( $create_atcphones_table );
$dbh->do( "CREATE INDEX idx_atcphones_facility_id on atcphones ( FACILITY_ID );" );
my $sth_atcphones = $dbh->prepare( $insert_atcphones_record );

while ( <FILE> )
{
    chomp;
    my @fields = split ",";
    #FACILITY_ID
    $sth_atcphones->bind_param( 1, $fields[ 0 ] );
    #FACILITY_TYPE
    $sth_atcphones->bind_param( 2, $fields[ 1 ] );
    #DUTY_OFFICE_PHONE
    $sth_atcphones->bind_param( 3, $fields[ 2 ] );
    #BUSINESS_HOURS
    $sth_atcphones->bind_param( 4, $fields[ 3 ] );
    #BUSINESS_PHONE
    $sth_atcphones->bind_param( 5, $fields[ 4 ] );

    $sth_atcphones->execute;
}

$dbh->disconnect();

