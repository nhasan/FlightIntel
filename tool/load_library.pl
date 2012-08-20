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
insert_category( "manuals", "MANUALS" );
insert_category( "periodicals", "PERIODICALS" );
insert_category( "regs", "REGULATIONS" );
insert_category( "risk", "RISK MANAGEMENT" );
insert_category( "circular", "ADVISORY CIRCULARS" );
insert_category( "testguide", "TEST GUIDE" );
insert_category( "misc", "MISCELLANEOUS" );

$dbh->do( "DROP TABLE IF EXISTS library" );
$dbh->do( $create_library_table );
$dbh->do( "CREATE INDEX idx_library_book on library ( CATEGORY_CODE, BOOK_NAME );" );
my $sth_library = $dbh->prepare( $insert_library_record );

insert_book( "handbooks", "faa-h-8083-3a-2004.pdf", "Airplane Flying Handbook", 
        "2004", "FAA-H-8083-3A", 14042341 );
insert_book( "handbooks", "faa-h-8083-25a-2008.pdf", "Pilot's Handbook of Aeronautical Knowledge",
        "2008", "FAA-H-8083-25A", 56064788 );
insert_book( "handbooks", "faa-h-8083-15a-2008.pdf", "Instrument Flying Handbook",
        "2008", "FAA-H-8083-15A", 36124885 );
insert_book( "handbooks", "faa-h-8261-1a-2007.pdf", "Instrument Procedures Handbook",
        "2007", "FAA-H-8261-1A", 18019772 );
insert_book( "handbooks", "faa-h-8083-6-2009.pdf", "Advanced Avionics Handbook",
        "2009", "FAA-H-8083-6", 20664164 );
insert_book( "handbooks", "faa-h-8083-27a-2006.pdf", "Student Pilot Guide",
        "2008", "FAA-H-8083-27A", 231353 );
insert_book( "handbooks", "faa-h-8083-9a-2008.pdf", "Aviation Instructor's Handbook",
        "2008", "FAA-H-8083-9A", 13672806 );
insert_book( "handbooks", "faa-h-8083-1a-2007.pdf", "Aircraft Weight and Balance Handbook",
        "2007", "FAA-H-8083-1A", 12373020 );
insert_book( "handbooks", "faa-h-8083-21-2000.pdf", "Rotorcraft Flying Handbook",
        "2000", "FAA-H-8083-21", 16859688 );
insert_book( "handbooks", "faa-h-8083-2-2009.pdf", "Risk Management Handbook",
        "2009", "FAA-H-8083-2", 11163579 );
insert_book( "handbooks", "faa-fs-i-8700-1-2003.pdf", "Information for Banner Tow Operations",
        "2003", "FAA/FS-I-8799-1", 845400 );

insert_book( "manuals", "Chart_Users_Guide_10thEd.pdf", "Aeronautical Chart User's Guide",
        "10th Edition", "FAA", 17159844 );
insert_book( "manuals", "faa-h-8083-19a-2008.pdf", "Plane Sense - General Aviation Information",
        "2008", "FAA-H-8083-19A", 13246143 );
insert_book( "manuals", "ac-00-6a-1975.pdf", "Aviation Weather",
        "1975", "AC-00-6A", 29916885 );
insert_book( "manuals", "ac-00-45g-2010.pdf", "Aviation Weather Services",
        "2010", "AC-00-45G", 12250011 );
insert_book( "manuals", "00-80T-80.pdf", "Aerodynamics for Naval Aviators",
        "1965", "H. H. Hurt, Jr.", 23020072 );

insert_book( "periodicals", "cb_389.pdf", "Callback", "Issue 389", "NASA", 167235 );
insert_book( "periodicals", "cb_390.pdf", "Callback", "Issue 390", "NASA", 227541 );
insert_book( "periodicals", "cb_391.pdf", "Callback", "Issue 391", "NASA", 211511 );
insert_book( "periodicals", "12jul-front.pdf", "The Front", "Jul 2012", "NOAA", 2071900 );
insert_book( "periodicals", "11nov-front.pdf", "The Front", "Nov 2011", "NOAA", 1030336 );
insert_book( "periodicals", "11jul-front.pdf", "The Front", "Jul 2011", "NOAA", 588846 );
insert_book( "periodicals", "JulAug2012.pdf", "FAA Safety Briefing",
        "Jul/Aug 2012", "FAA", 3930710 );
insert_book( "periodicals", "MayJun2012.pdf", "FAA Safety Briefing",
        "May/Jun 2012", "FAA", 3162563 );
insert_book( "periodicals", "MarApr2012.pdf", "FAA Safety Briefing",
        "Mar/Apr 2012", "FAA", 6996937 );

insert_book( "regs", "aim_2012_r1.pdf", "Aeronautical Information Manual (AIM)",
        "2012", "FAA", 8640900 );
insert_book( "regs", "cfr-2012-title14-vol1.pdf", "Federal Aviation Regulations (FAR)",
        "Parts 1-59", "US GPO", 9855793 );
insert_book( "regs", "cfr-2012-title14-vol2.pdf", "Federal Aviation Regulations (FAR)",
        "Parts 60-109", "US GPO", 15171981 );
insert_book( "regs", "cfr-2012-title14-vol3.pdf", "Federal Aviation Regulations (FAR)",
        "Parts 110-199", "US GPO", 4961436 );
insert_book( "regs", "cfr-2012-title14-vol4.pdf", "Federal Aviation Regulations (FAR)",
        "Parts 200-1199", "US GPO", 16738367 );
insert_book( "regs", "cfr-2012-title14-vol5.pdf", "Federal Aviation Regulations (FAR)",
        "Parts 1200-End", "US GPO", 2181427 );

insert_book( "risk", "ga_weather_decision_making.pdf", "General Aviation Pilot’s Guide",
        "2009", "FAA", 659230 );
insert_book( "risk", "local_vfr.pdf", "Local VFR Flying", "2012", "FAA", 300310 );
insert_book( "risk", "night_vfr.pdf", "Night VFR Flying", "2012", "FAA", 294394 );
insert_book( "risk", "pract_riskman.pdf", "Practical Risk Management", "2012", "FAA", 452295 );
insert_book( "risk", "vfr_xc.pdf", "VFR XC Flying", "2012", "FAA", 488931 );

insert_book( "circular", "ac-60-22-1991.pdf", "Aeronautical Decision Making",
        "1991", "AC-60-22", 3915178 );
insert_book( "circular", "ac-91-13c-1979.pdf", "Cold Weather Operation of Aircraft",
        "1979", "AC-91-13C", 553456 );
insert_book( "circular", "ac-91-74a-2007.pdf", "Flight In Icing Conditions",
        "2007", "AC-91-74A", 584587 );
insert_book( "circular", "ac-00-54-1988.pdf", "Pilot Windshear Guide",
        "1988", "AC-00-54", 4475314 );
insert_book( "circular", "ac-00-46e-2011.pdf", "Aviation Safety Reporting Program",
        "2011", "AC-00-46E", 46601 );

insert_book( "testguide", "FAA-G-8082-17F.pdf", "Private Pilot Knowledge Test Guide",
        "2011", "FAA-G-8082-17F", 559673 );
insert_book( "testguide", "FAA-G-8082-13F.pdf", "Instrument Rating Knowledge Test Guide",
        "2012", "FAA-G-8082-13F", 432103 );
insert_book( "testguide", "FAA-G-8082-5E.pdf", "Commercial Pilot Knowledge Test Guide",
        "2011", "FAA-G-8082-5E", 708182 );
insert_book( "testguide", "FAA-G-8082-7F.pdf", "Flight & Ground Instructor Knowledge Test Guide",
        "2012", "FAA-G-8082-7F", 578458 );

insert_book( "misc", "aip.pdf", "Aeronautical Information Publication",
        "21st Edition", "FAA", 14850803 );
insert_book( "misc", "NAT_IGA_2004.pdf", "North Atlantic Operations Manual",
        "3rd Edition", "USA", 717842 );
insert_book( "misc", "pcg.pdf", "Pilot/Controller Glossary",
        "2012", "FAA", 436621 );

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
