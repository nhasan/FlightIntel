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

my $dbh = DBI->connect( "dbi:SQLite:dbname=$BASE_DIR/$dbfile", "", "" );

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

my $create_metadata_table = "CREATE TABLE android_metadata ( locale TEXT );";
my $insert_metadata_record = "INSERT INTO android_metadata VALUES ( 'en_US' );";

$dbh->do( "DROP TABLE IF EXISTS android_metadata" );
$dbh->do( $create_metadata_table );
$dbh->do( $insert_metadata_record );

my $create_categories_table = "CREATE TABLE bookcategories ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."CATEGORY_CODE TEXT, "
        ."CATEGORY_NAME"
        .")";

my $insert_categories_record = "INSERT INTO bookcategories ("
        ."CATEGORY_CODE, "
        ."CATEGORY_NAME"
        .") VALUES ("
        ."?, ?"
        .")";

my $create_library_table = "CREATE TABLE library ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."CATEGORY_CODE TEXT, "
        ."BOOK_NAME TEXT, "
        ."BOOK_DESC TEXT, "
        ."EDITION TEXT, "
        ."AUTHOR TEXT, "
        ."DOWNLOAD_SIZE INTEGER"
        .")";

my $insert_library_record = "INSERT INTO library ("
        ."CATEGORY_CODE, "
        ."BOOK_NAME, "
        ."BOOK_DESC, "
        ."EDITION, "
        ."AUTHOR, "
        ."DOWNLOAD_SIZE"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?"
        .")";

$dbh->do( "DROP TABLE IF EXISTS bookcategories" );
$dbh->do( $create_categories_table );
my $sth_categories = $dbh->prepare( $insert_categories_record );

insert_category( "handbooks", "HANDBOOKS" );
insert_category( "periodicals", "PERIODICALS" );
insert_category( "regs", "REGULATIONS" );

$dbh->do( "DROP TABLE IF EXISTS library" );
$dbh->do( $create_library_table );
$dbh->do( "CREATE INDEX idx_library_book on library ( CATEGORY_CODE, BOOK_NAME );" );
my $sth_library = $dbh->prepare( $insert_library_record );

insert_book( "handbooks", "faa-h-8083-3a-2004", "Airplane Flying Handbook", 
        "2004", "FAA", 14042494 );
insert_book( "handbooks", "faa-h-8083-25a-2008", "Pilot's Handbook of Aeronautical Knowledge",
        "2008", "FAA", 56064942 );
insert_book( "handbooks", "faa-h-8083-27a-2006", "Student Pilot Guide",
        "2008", "FAA", 231507 );
insert_book( "handbooks", "faa-h-8083-15a-2008", "Instrument Flying Handbook",
        "2008", "FAA", 36125039 );
insert_book( "handbooks", "faa-h-8261-1a-2007", "Instrument Procedures Handbook",
        "2007", "FAA", 18019925 );
insert_book( "handbooks", "Chart_Users_Guide_10thEd", "Aeronautical Chart User's Guide",
        "10th Edition", "FAA", 17160003 );

insert_book( "periodicals", "cb_389", "Callback", "Issue 389", "NASA", 167376 );
insert_book( "periodicals", "cb_390", "Callback", "Issue 390", "NASA", 227682 );
insert_book( "periodicals", "JulAug2012", "FAA Safety Briefing", "Jul/Aug 2012", "FAA", 3930855 );
insert_book( "periodicals", "MayJun2012", "FAA Safety Briefing", "May/Jun 2012", "FAA", 3162708 );
insert_book( "periodicals", "MarApr2012", "FAA Safety Briefing", "Mar/Apr 2012", "FAA", 6997082 );

insert_book( "regs", "aim_2012_r1", "Aeronautical Information Manual (AIM)",
        "2012", "FAA", 8641046 );
insert_book( "regs", "cfr-2012-title14-vol1", "Federal Aviation Regulations (FAR)",
        "Parts 1-59", "US GPO", 9855949 );
insert_book( "regs", "cfr-2012-title14-vol2", "Federal Aviation Regulations (FAR)",
        "Parts 60-109", "US GPO", 15172137 );
insert_book( "regs", "cfr-2012-title14-vol3", "Federal Aviation Regulations (FAR)",
        "Parts 110-199", "US GPO", 4961592 );
insert_book( "regs", "cfr-2012-title14-vol4", "Federal Aviation Regulations (FAR)",
        "Parts 200-1199", "US GPO", 16738523 );
insert_book( "regs", "cfr-2012-title14-vol5", "Federal Aviation Regulations (FAR)",
        "Parts 1200-End", "US GPO", 2181583 );

exit;

sub insert_category( $$ )
{
    #CATEGORY
    my $category = shift;
    $sth_categories->bind_param( 1, $category );
    my $name = shift;
    $sth_categories->bind_param( 2, $name );

    $sth_categories->execute();
}

sub insert_book( $$$$$$$$ )
{
    #CATEGORY_CODE
    my $category = shift;
    $sth_library->bind_param( 1, $category );
    #BOOK_NAME
    my $name = shift;
    $sth_library->bind_param( 2, $name );
    #BOOK_DESC
    my $desc = shift;
    $sth_library->bind_param( 3, $desc );
    #AUTHOR
    my $author = shift;
    $sth_library->bind_param( 4, $author );
    #EDITION
    my $edition = shift;
    $sth_library->bind_param( 5, $edition );
    #DOWNLOAD_SIZE
    my $size = shift;
    $sth_library->bind_param( 6, $size );

    $sth_library->execute();
}
