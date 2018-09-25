#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2012-2017 Hasan <nhasan@nadmm.com>
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
my $cycle = shift @ARGV;

my $dbh = DBI->connect( "dbi:SQLite:dbname=$BASE_DIR/library_$cycle.db", "", "" );

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
insert_category( "testguide", "TRAINING" );
insert_category( "pts", "PTS" );
insert_category( "misc", "MISCELLANEOUS" );
insert_category( "canada", "CANADA" );

$dbh->do( "DROP TABLE IF EXISTS library" );
$dbh->do( $create_library_table );
$dbh->do( "CREATE INDEX idx_library_book on library ( CATEGORY_CODE, BOOK_NAME );" );
my $sth_library = $dbh->prepare( $insert_library_record );

insert_book( "handbooks", "faa-h-8083-3b-2016.pdf", "Airplane Flying Handbook",
        "2016", "FAA-H-8083-3B", 94300974, "" );
insert_book( "handbooks", "faa-h-8083-25b-2016.pdf", "Pilot's Handbook of Aeronautical Knowledge",
        "2016", "FAA-H-8083-25B", 56400327, "" );
insert_book( "handbooks", "faa-h-8083-15b-2012.pdf", "Instrument Flying Handbook",
        "2012", "FAA-H-8083-15B", 53239138, "" );
insert_book( "handbooks", "faa-h-8083-16b-2017.pdf", "Instrument Procedures Handbook",
        "2017", "FAA-H-8083-16b", 35027015, "" );
insert_book( "handbooks", "faa-h-8083-6-2009.pdf", "Advanced Avionics Handbook",
        "2009", "FAA-H-8083-6", 20664164, "" );
insert_book( "handbooks", "faa-h-8083-27a-2006.pdf", "Student Pilot Guide",
        "2016", "FAA-H-8083-27A", 231353, "" );
insert_book( "handbooks", "faa-h-8083-9a-2008.pdf", "Aviation Instructor's Handbook",
        "2008", "FAA-H-8083-9A", 13672806, "" );
insert_book( "handbooks", "faa-h-8083-1b-2016.pdf", "Weight and Balance Handbook",
        "2016", "FAA-H-8083-1B", 10295944, "" );
insert_book( "handbooks", "faa-h-8083-21a-2012.pdf", "Helicopter Flying Handbook",
        "2012", "FAA-H-8083-21A", 74947815, "" );
insert_book( "handbooks", "faa-h-8083-2-2009.pdf", "Risk Management Handbook",
        "2016", "FAA-H-8083-2", 19766175, "" );
insert_book( "handbooks", "faa-fs-i-8700-1-2003.pdf", "Information for Banner Tow Operations",
        "2003", "FAA/FS-I-8799-1", 845400, "" );
insert_book( "handbooks", "fcm-h1-2005.pdf", "Federal Meteorological Handbook No. 1",
        "2005", "NOAA", 1860432, "" );
insert_book( "handbooks", "fcm-h2-1988.pdf", "Federal Meteorological Handbook No. 2",
        "1988", "NOAA", 16091462, "" );

insert_book( "manuals", "cug_180913.pdf", "Aeronautical Chart User's Guide",
        "Sep 2018", "FAA", 12039219, "" );
insert_book( "manuals", "faa-h-8083-19a-2008.pdf", "Plane Sense - General Aviation Information",
        "2008", "FAA-H-8083-19A", 13246143, "" );
insert_book( "manuals", "ac-00-6a-1975.pdf", "Aviation Weather",
        "1975", "AC-00-6A", 29916885, "" );
insert_book( "manuals", "ac-00-45g-2010.pdf", "Aviation Weather Services",
        "2010", "AC-00-45G", 12250011, "" );
insert_book( "manuals", "00-80T-80.pdf", "Aerodynamics for Naval Aviators",
        "1965", "H. H. Hurt, Jr.", 23020072, "" );

insert_book( "periodicals", "cb_464.pdf", "Callback",
        "Issue 464 (Sep 2018)", "NASA", 181123, "" );
insert_book( "periodicals", "cb_463.pdf", "Callback",
        "Issue 463 (Aug 2018)", "NASA", 235604, "" );
insert_book( "periodicals", "cb_462.pdf", "Callback",
        "Issue 462 (Jul 2018)", "NASA", 171485, "" );
insert_book( "periodicals", "cb_461.pdf", "Callback",
        "Issue 461 (Jun 2018)", "NASA", 153918, "" );
insert_book( "periodicals", "SepOct2018.pdf", "FAA Safety Briefing",
        "Sep/Oct 2018", "FAA", 4837912, "" );
insert_book( "periodicals", "JulAug2018.pdf", "FAA Safety Briefing",
        "Jul/Aug 2018", "FAA", 8635427, "" );
insert_book( "periodicals", "MayJune2018.pdf", "FAA Safety Briefing",
        "May/Jun 2018", "FAA", 4172836, "" );
insert_book( "periodicals", "MarApr2018.pdf", "FAA Safety Briefing",
        "Mar/Apr 2018", "FAA", 4593305, "" );
insert_book( "periodicals", "SE_Topic_18_08.pdf", "Safety Fact Sheets",
        "Aug 2018", "FAA", 1036542, "" );
insert_book( "periodicals", "SE_Topic_18_07.pdf", "Safety Fact Sheets",
        "Jul 2018", "FAA", 833646, "" );
insert_book( "periodicals", "SE_Topic_18_06.pdf", "Safety Fact Sheets",
        "Jun 2018", "FAA", 729778, "" );
insert_book( "periodicals", "SE_Topic_18_05.pdf", "Safety Fact Sheets",
        "May 2018", "FAA", 1027633, "" );

insert_book( "safety", "ac_90-48d-chg_1.pdf", "Pilots’ Role in Collision Avoidance",
        "2016", "FAA", 159341, "" );
insert_book( "safety", "safety_advisor_non-towered_airports.pdf", "Operations at Nontowered Airports",
        "2007", "FAA", 707797, "" );
insert_book( "safety", "faa-p-8740-02-2008.pdf", "Density Altitude",
        "2008", "FAA", 829656, "" );
insert_book( "safety", "faa-p-8740-12-2008.pdf", "Thunderstorms",
        "2008", "FAA", 615586, "" );
insert_book( "safety", "faa-p-8740-30-2008.pdf", "How to Obtain a Good Weather Briefing",
        "2008", "FAA", 768237, "" );
insert_book( "safety", "faa-p-8740-36-2008.pdf", "Proficiency and the Private Pilot",
        "2008", "FAA", 728809, "" );
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
insert_book( "safety", "AP_UpsetRecovery_Book.pdf", "Upset Recovery Training Aid",
        "2008", "FAA", 22359572, "" );
insert_book( "safety", "tfrweb_2003.pdf.gz", "Pilot's Guide to TFR",
        "2003", "FAA", 1794432, "" );

insert_book( "regs", "aim_180913.pdf", "AIM Change 1 & 2",
        "Sep 2018", "FAA", , "14717067" );
insert_book( "regs", "CFR-2018-title14-vol1.pdf", "FAR Volume 1",
        "Parts 1-59", "US GPO 2018", 10860978, "" );
insert_book( "regs", "CFR-2018-title14-vol2.pdf", "FAR Volume 2",
        "Parts 60-109", "US GPO 2018", 20370363, "" );
insert_book( "regs", "CFR-2018-title14-vol3.pdf", "FAR Volume 3",
        "Parts 110-199", "US GPO 2018", 5432865432869, "" );
insert_book( "regs", "CFR-2018-title14-vol4.pdf", "FAR Volume 4",
        "Parts 200-1199", "US GPO 2018", 16820917, "" );
insert_book( "regs", "CFR-2018-title14-vol5.pdf", "FAR Volume 5",
        "Parts 1200-1399", "US GPO 2018", 2014602, "" );

insert_book( "risk", "wx_decision.pdf", "Weather Decision Making", "2009", "FAA", 659230, "" );
insert_book( "risk", "local_vfr.pdf", "Local VFR Flying", "2012", "FAA", 300310, "" );
insert_book( "risk", "night_vfr.pdf", "Night VFR Flying", "2012", "FAA", 294394, "" );
insert_book( "risk", "pract_riskman.pdf", "Practical Risk Management", "2012", "FAA", 452295, "" );
insert_book( "risk", "vfr_xc.pdf", "VFR XC Flying", "2012", "FAA", 488931, "" );

insert_book( "circular", "ac-60-22-1991.pdf", "Aeronautical Decision Making",
        "1991", "AC-60-22", 3915178, "" );
insert_book( "circular", "ac-91-13c-1979.pdf", "Cold Weather Operation of Aircraft",
        "1979", "AC-91-13C", 553456, "" );
insert_book( "circular", "ac-91-74b-2015.pdf", "Flight In Icing Conditions",
        "2015", "AC-91-74B", 1001510, "" );
insert_book( "circular", "ac-00-54-1988.pdf", "Pilot Windshear Guide",
        "1988", "AC-00-54", 4475314, "" );
insert_book( "circular", "ac-00-46e-2011.pdf", "Aviation Safety Reporting Program",
        "2011", "AC-00-46E", 46601, "" );
insert_book( "circular", "ac-61-134-2003.pdf", "Controlled Flight Into Terrain Awareness",
        "2003", "AC-61-134", 259747, "" );
insert_book( "circular", "ac-61-65f-2016.pdf", "Pilots and Flight and Ground Instructors",
        "2016", "AC-61-65F", 407258, "" );
insert_book( "circular", "ac-120-76c-2014.pdf", "Use of Electronics Flight Bags",
        "2014", "AC-120-76C", 483260, "" );
insert_book( "circular", "ac-120-12a-1986.pdf", "Private Carriage vs Common Carriage",
        "1986", "AC-120-12A", 57908, "" );

insert_book( "testguide", "FAA-G-8082-17I_1702.pdf", "Private Pilot Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-17I", 89464, "" );
insert_book( "testguide", "FAA-G-8082-13I_1702.pdf", "Instrument Rating Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-13I", 171874, "" );
insert_book( "testguide", "FAA-G-8082-5H_1702.pdf", "Commercial Pilot Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-5H", 115759, "" );
insert_book( "testguide", "FAA-G-8082-7I_1702.pdf", "Flight Instructor Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-7I", 101717, "" );
insert_book( "testguide", "FAA-G-8082-4D_1702.pdf", "Sport Pilot Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-4D", 123443, "" );
insert_book( "testguide", "IPC_Guidance_150909.pdf", "Instrument Proficiency Check Guidance",
        "Sep 2015", "", 544288, "" );
insert_book( "testguide", "PARSampleExam_170612.pdf", "Airmen Knowledge Test Question Bank",
        "Jun 2017", "Private", 135578, "" );
insert_book( "testguide", "IRASampleExam_171016.pdf", "Airmen Knowledge Test Question Bank",
        "Oct 2017", "Instrument", 111272, "" );
insert_book( "testguide", "LSPSampleExam_171016.pdf", "Airmen Knowledge Test Question Bank",
        "Oct 2017", "Light Sport", 87390, "" );
insert_book( "testguide", "cax_sample_exam_171016.pdf", "Airmen Knowledge Test Question Bank",
        "Oct 2017", "Commercial", 114146, "" );

insert_book( "pts", "faa-s-8081-12c.pdf", "Commercial Pilot PTS",
        "Jun 2012", "FAA-S-8081-12C", 472242, "" );
insert_book( "pts", "faa-s-8081-14b.pdf", "Private Pilot PTS",
        "Jun 2012", "FAA-S-8081-14B", 490370, "" );
insert_book( "pts", "faa-s-8081-29.pdf", "Sport Pilot PTS",
        "Dec 2004", "FAA-S-8081-29", 583474, "" );
insert_book( "pts", "faa-s-8081-3a.pdf", "Recreational Pilot PTS",
        "Aug 2006", "FAA-S-8081-3A", 439253, "" );
insert_book( "pts", "faa-s-8081-4e.pdf", "Instrument Rating PTS",
        "Jan 2010", "FAA-S-8081-4E", 283337, "" );
insert_book( "pts", "faa-s-8081-5f.pdf", "Airline Transport Pilot PTS",
        "Jul 2008", "FAA-S-8081-5F", 391519, "" );
insert_book( "pts", "faa-s-8081-6d.pdf", "Flight Instructor PTS",
        "Dec 2012", "FAA-S-8081-6D", 645207, "" );
insert_book( "pts", "faa-s-8081-9d.pdf", "Flight Instructor Instrument PTS",
        "Jul 2010", "FAA-S-8081-9D", 169064, "" );

insert_book( "misc", "aip_180913.pdf.gz", "Aeronautical Information Publication",
        "Sep 2018", "FAA", 34997720, "" );
insert_book( "misc", "NAT_IGA_2004.pdf", "North Atlantic Operations Manual",
        "3rd Edition", "USA", 717842, "" );
insert_book( "misc", "pcg_180913.pdf", "Pilot/Controller Glossary",
        "Sep 2018", "FAA", 561083, "" );
insert_book( "misc", "atc_180913.pdf", "Air Trafffic Control",
        "Sep 2018", "FAA", 4567618, "" );
insert_book( "misc", "fss_180913.pdf", "Flight Services",
        "Sep 2018", "FAA", 3087677, "" );

insert_book( "canada", "AWS-Guide-EN_1602.pdf", "Aviation Weather Service Guide",
        "Feb 2016", "Nav Canada", 5043235, "" );
insert_book( "canada", "Customer-Guide-Charges-EN_1311.pdf", "Customer Guide to Charges",
        "Mov 2013", "Nav Canada", 2272225, "" );
insert_book( "canada", "VFR_Phraseology_1505.pdf", "VFR Phraseology",
        "May 2015", "Nav Canada", 3662663, "" );
insert_book( "canada", "NOTAM-Manual-EN_1603.pdf", "Canadian NOTAM Procedures Manual",
        "Mar 2016", "Nav Canada", 1600832, "" );
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
    #EDITION
    my $edition = shift;
    $sth_library->bind_param( 4, $edition );
    #AUTHOR
    my $author = shift;
    $sth_library->bind_param( 5, $author );
    #DOWNLOAD_SIZE
    my $size = shift;
    $sth_library->bind_param( 6, $size );
    #FLAG
    my $flag = shift;
    $sth_library->bind_param( 7, $flag );

    $sth_library->execute();
}
