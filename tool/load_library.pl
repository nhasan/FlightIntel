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
        ."CATEGORY_NAME TEXT"
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
        ."DOWNLOAD_SIZE INTEGER, "
        ."FLAG TEXT"
        .")";

my $insert_library_record = "INSERT INTO library ("
        ."CATEGORY_CODE, "
        ."BOOK_NAME, "
        ."BOOK_DESC, "
        ."EDITION, "
        ."AUTHOR, "
        ."DOWNLOAD_SIZE, "
        ."FLAG"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?"
        .")";

$dbh->do( "DROP TABLE IF EXISTS bookcategories" );
$dbh->do( $create_categories_table );
my $sth_categories = $dbh->prepare( $insert_categories_record );

insert_category( "handbooks", "HANDBOOKS" );
insert_category( "manuals", "MANUALS" );
insert_category( "periodicals", "PERIODICALS" );
insert_category( "safety", "SAFETY" );
insert_category( "regs", "REGULATIONS" );
insert_category( "risk", "RISK MANAGEMENT" );
insert_category( "circular", "ADVISORY CIRCULARS" );
insert_category( "testguide", "TEST GUIDE" );
insert_category( "canada", "CANADA" );
insert_category( "misc", "MISCELLANEOUS" );

$dbh->do( "DROP TABLE IF EXISTS library" );
$dbh->do( $create_library_table );
$dbh->do( "CREATE INDEX idx_library_book on library ( CATEGORY_CODE, BOOK_NAME );" );
my $sth_library = $dbh->prepare( $insert_library_record );

insert_book( "handbooks", "faa-h-8083-3a-2004.pdf", "Airplane Flying Handbook", 
        "2004", "FAA-H-8083-3A", 14042341, "" );
insert_book( "handbooks", "faa-h-8083-25a-2008.pdf", "Pilot's Handbook of Aeronautical Knowledge",
        "2008", "FAA-H-8083-25A", 56064788, "" );
insert_book( "handbooks", "faa-h-8083-15b-2012.pdf", "Instrument Flying Handbook",
        "2012", "FAA-H-8083-15B", 53239138, "" );
insert_book( "handbooks", "faa-h-8261-1a-2007.pdf", "Instrument Procedures Handbook",
        "2007", "FAA-H-8261-1A", 18019772, "" );
insert_book( "handbooks", "faa-h-8083-6-2009.pdf", "Advanced Avionics Handbook",
        "2009", "FAA-H-8083-6", 20664164, "" );
insert_book( "handbooks", "faa-h-8083-27a-2006.pdf", "Student Pilot Guide",
        "2008", "FAA-H-8083-27A", 231353, "" );
insert_book( "handbooks", "faa-h-8083-9a-2008.pdf", "Aviation Instructor's Handbook",
        "2008", "FAA-H-8083-9A", 13672806, "" );
insert_book( "handbooks", "faa-h-8083-1a-2007.pdf", "Aircraft Weight and Balance Handbook",
        "2007", "FAA-H-8083-1A", 12373020, "" );
insert_book( "handbooks", "faa-h-8083-21a-2012.pdf", "Rotorcraft Flying Handbook",
        "2012", "FAA-H-8083-21A", 74947815, "" );
insert_book( "handbooks", "faa-h-8083-2-2009.pdf", "Risk Management Handbook",
        "2009", "FAA-H-8083-2", 11163579, "" );
insert_book( "handbooks", "faa-fs-i-8700-1-2003.pdf", "Information for Banner Tow Operations",
        "2003", "FAA/FS-I-8799-1", 845400, "" );
insert_book( "handbooks", "fcm-h1-2005.pdf", "Federal Meteorological Handbook No. 1",
        "2005", "NOAA", 1860432, "" );
insert_book( "handbooks", "fcm-h2-1988.pdf", "Federal Meteorological Handbook No. 2",
        "1988", "NOAA", 16091462, "" );

insert_book( "manuals", "Chart_Users_Guide_11thEd.pdf", "Aeronautical Chart User's Guide",
        "11th Edition", "FAA", 17533800, "" );
insert_book( "manuals", "faa-h-8083-19a-2008.pdf", "Plane Sense - General Aviation Information",
        "2008", "FAA-H-8083-19A", 13246143, "" );
insert_book( "manuals", "ac-00-6a-1975.pdf", "Aviation Weather",
        "1975", "AC-00-6A", 29916885, "" );
insert_book( "manuals", "ac-00-45g-2010.pdf", "Aviation Weather Services",
        "2010", "AC-00-45G", 12250011, "" );
insert_book( "manuals", "00-80T-80.pdf", "Aerodynamics for Naval Aviators",
        "1965", "H. H. Hurt, Jr.", 23020072, "" );

insert_book( "periodicals", "cb_405.pdf", "Callback", "Issue 405 (Oct 2013)", "NASA", 585763, "N" );
insert_book( "periodicals", "cb_404.pdf", "Callback", "Issue 404 (Sep 2013)", "NASA", 597480, "" );
insert_book( "periodicals", "cb_403.pdf", "Callback", "Issue 403 (Aug 2013)", "NASA", 587239, "" );
insert_book( "periodicals", "cb_402.pdf", "Callback", "Issue 402 (Jul 2013)", "NASA", 300935, "" );
insert_book( "periodicals", "13jun-front.pdf", "The Front", "Jun 2013", "NOAA", 2677124, "" );
insert_book( "periodicals", "13apr-front.pdf", "The Front", "Apr 2013", "NOAA", 2475943, "" );
insert_book( "periodicals", "12dec-front.pdf", "The Front", "Dec 2012", "NOAA", 2150530, "" );
insert_book( "periodicals", "12jul-front.pdf", "The Front", "Jul 2012", "NOAA", 2071900, "" );
insert_book( "periodicals", "SepOct2013.pdf", "FAA Safety Briefing",
        "Sep/Oct 2013", "FAA", 4083620, "" );
insert_book( "periodicals", "JulAug2013.pdf", "FAA Safety Briefing",
        "Jul/Aug 2013", "FAA", 5423441, "" );
insert_book( "periodicals", "MayJun2013.pdf", "FAA Safety Briefing",
        "May/Jun 2013", "FAA", 5208186, "" );
insert_book( "periodicals", "MarApr2013.pdf", "FAA Safety Briefing",
        "Mar/Apr 2013", "FAA", 2751314, "" );
insert_book( "periodicals", "ATB-2013-1.pdf", "Air Traffic Bulletin",
        "2013-Q1", "FAA", 46744, "" );
insert_book( "periodicals", "ATB-2012-4.pdf", "Air Traffic Bulletin",
        "2012-Q4", "FAA", 243345, "" );
insert_book( "periodicals", "ATB-2012-3.pdf", "Air Traffic Bulletin",
        "2012-Q3", "FAA", 167092, "" );

insert_book( "safety", "faa-p-8740-02-2008.pdf", "Density Altitude",
        "2008", "FAA", 829656, "" );
insert_book( "safety", "faa-p-8740-12-2008.pdf", "Thunderstorms",
        "2008", "FAA", 615586, "" );
insert_book( "safety", "faa-p-8740-30-2008.pdf", "How to Obtain a Good Weather Briefing",
        "2008", "FAA", 768237, "" );
insert_book( "safety", "faa-p-8740-36-2008.pdf", "Proficiency and the Private Pilot",
        "2008", "FAA", 728809, "" );
insert_book( "safety", "faa-p-8740-40-2008.pdf", "Wind Shear",
        "2008", "FAA", 1572833, "" );
insert_book( "safety", "faa-p-8740-48-2008.pdf", "On Landings Part 1",
        "2008", "FAA", 7959581, "" );
insert_book( "safety", "faa-p-8740-49-2008.pdf", "On Landings Part 2",
        "2008", "FAA", 2122307, "" );
insert_book( "safety", "faa-p-8740-50-2008.pdf", "On Landings Part 3",
        "2008", "FAA", 2478467, "" );
insert_book( "safety", "faa-p-8740-66-2008.pdf", "Flying Light Twins Safely",
        "2008", "FAA", 988401, "" );
insert_book( "safety", "faa-p-8740-69-2008.pdf", "Aeronautical Decision Making",
        "2008", "FAA", 1331791, "" );
insert_book( "safety", "takeoff_safety.pdf", "Pilot Guide to Takeoff Safety",
        "2008", "FAA", 2678525, "" );

insert_book( "regs", "aim_2012_r3.pdf", "Aeronautical Information Manual (AIM)",
        "Aug 2013 (Change 3)", "FAA", 9017835, "" );
insert_book( "regs", "cfr-2013-title14-vol1.pdf", "Federal Aviation Regulations (FAR) 2013",
        "Parts 1-59", "US GPO", 9869469, "" );
insert_book( "regs", "cfr-2013-title14-vol2.pdf", "Federal Aviation Regulations (FAR) 2013",
        "Parts 60-109", "US GPO", 15653683, "" );
insert_book( "regs", "cfr-2013-title14-vol3.pdf", "Federal Aviation Regulations (FAR) 2013",
        "Parts 110-199", "US GPO", 5352778, "" );
insert_book( "regs", "cfr-2013-title14-vol4.pdf", "Federal Aviation Regulations (FAR) 2013",
        "Parts 200-1199", "US GPO", 16915045, "" );
insert_book( "regs", "cfr-2013-title14-vol5.pdf", "Federal Aviation Regulations (FAR) 2013",
        "Parts 1200-1310", "US GPO", 2400725, "" );

insert_book( "risk", "ga_weather_decision_making.pdf", "General Aviation Pilot’s Guide",
        "2009", "FAA", 659230, "" );
insert_book( "risk", "local_vfr.pdf", "Local VFR Flying", "2012", "FAA", 300310, "" );
insert_book( "risk", "night_vfr.pdf", "Night VFR Flying", "2012", "FAA", 294394, "" );
insert_book( "risk", "pract_riskman.pdf", "Practical Risk Management", "2012", "FAA", 452295, "" );
insert_book( "risk", "vfr_xc.pdf", "VFR XC Flying", "2012", "FAA", 488931, "" );

insert_book( "circular", "ac-60-22-1991.pdf", "Aeronautical Decision Making",
        "1991", "AC-60-22", 3915178, "" );
insert_book( "circular", "ac-91-13c-1979.pdf", "Cold Weather Operation of Aircraft",
        "1979", "AC-91-13C", 553456, "" );
insert_book( "circular", "ac-91-74a-2007.pdf", "Flight In Icing Conditions",
        "2007", "AC-91-74A", 584587, "" );
insert_book( "circular", "ac-00-54-1988.pdf", "Pilot Windshear Guide",
        "1988", "AC-00-54", 4475314, "" );
insert_book( "circular", "ac-00-46e-2011.pdf", "Aviation Safety Reporting Program",
        "2011", "AC-00-46E", 46601, "" );

insert_book( "testguide", "FAA-G-8082-17I.pdf", "Private Pilot Knowledge Test Guide",
        "Feb 2013", "FAA-G-8082-17I", 410108, "" );
insert_book( "testguide", "FAA-G-8082-13I.pdf", "Instrument Rating Knowledge Test Guide",
        "Feb 2013", "FAA-G-8082-13I", 331406, "" );
insert_book( "testguide", "FAA-G-8082-5H.pdf", "Commercial Pilot Knowledge Test Guide",
        "Feb 2013", "FAA-G-8082-5H", 559979, "" );
insert_book( "testguide", "FAA-G-8082-7I.pdf", "Flight & Ground Instructor Knowledge Test Guide",
        "Feb 2013", "FAA-G-8082-7I", 812208, "" );
insert_book( "testguide", "FAA-G-8082-4D.pdf", "Sport Pilot Knowledge Test Guide",
        "Feb 2013", "FAA-G-8082-4D", 979987, "" );
insert_book( "testguide", "IPC_Guidance.pdf", "Instrument Proficiency Check Guidance",
        "Mar 2010", "", 340448, "" );

insert_book( "misc", "AIP_22nd_Edition.pdf", "Aeronautical Information Publication",
        "22nd Edition", "FAA", 15562404, "" );
insert_book( "misc", "NAT_IGA_2004.pdf", "North Atlantic Operations Manual",
        "3rd Edition", "USA", 717842, "" );
insert_book( "misc", "pcg_082213.pdf", "Pilot/Controller Glossary",
        "Aug 2013", "FAA", 557346, "N" );
insert_book( "misc", "RNProadmap.pdf", "Roadmap for Performance-Based Navigation",
        "2006", "FAA", 1513616, "" );

insert_book( "canada", "AWS_Guide_EN.pdf", "Aviation Weather Service Guide",
        "Aug 2011", "Nav Canada", 1092048, "" );
insert_book( "canada", "Customer_Guide_New_en.pdf", "Customer Guide to Charges",
        "Sep 2008", "Nav Canada", 1838506, "" );
insert_book( "canada", "Notam_Manual_Current_en.pdf", "Canadian NOTAM Procedures Manual",
        "Nov 2012", "Nav Canada", 1883233, "" );
insert_book( "canada", "A34E-W.pdf", "The Weather of Atlantic Canada and Eastern Quebec",
        "Nov 2005", "Nav Canada", 5529975, "" );
insert_book( "canada", "BC31E-W.pdf", "The Weather of British Columbia",
        "Nov 2005", "Nav Canada", 5192648, "" );
insert_book( "canada", "N3637E-W.pdf", "The Weather of Nunavut and the Arctic",
        "Oct 2005", "Nav Canada", 6972336, "" );
insert_book( "canada", "OQ33E-W.pdf", "The Weather of Ontario and Quebec",
        "Nov 2005", "Nav Canada", 5146715, "" );
insert_book( "canada", "P32E-W.pdf", "The Weather of the Canadian Prairies",
        "Nov 2005", "Nav Canada", 6266643, "" );
insert_book( "canada", "Y35E-W.pdf", "The Weather of the Yukon, NWT and Western Nunavut",
        "Nov 2005", "Nav Canada", 5597294, "" );
insert_book( "canada", "charts_alberta_20130502.pdf", "Canadian Airport Charts - Alberta",
        "2 May-27 Jun", "Nav Canada", 4964294, "" );
insert_book( "canada", "charts_atlantic_20130502.pdf", "Canadian Airport Charts - Atlantic",
        "2 May-27 Jun", "Nav Canada", 2452850, "" );
insert_book( "canada", "charts_bc_20130502.pdf", "Canadian Airport Charts - British Columbia",
        "2 May-27 Jun", "Nav Canada", 3426437, "" );
insert_book( "canada", "charts_ontario_20130502.pdf", "Canadian Airport Charts - Ontario",
        "2 May-27 Jun", "Nav Canada", 6940887, "" );
insert_book( "canada", "charts_quebec_20130502.pdf", "Canadian Airport Charts - Quebec",
        "2 May-27 Jun", "Nav Canada", 8726130, "" );
insert_book( "canada", "charts_yukon_20130502.pdf", "Canadian Airport Charts - Yukon",
        "2 May-27 Jun", "Nav Canada", 2406846, "" );

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

sub insert_book( $$$$$$$ )
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
    #FLAG
    my $flag = shift;
    $sth_library->bind_param( 7, $flag );

    $sth_library->execute();
}
