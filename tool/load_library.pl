#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2012-2019 Hasan <nhasan@nadmm.com>
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

my $base = shift @ARGV;
my $dbfile = shift @ARGV;

my $dbh = DBI->connect( "dbi:SQLite:dbname=$dbfile", "", "" );

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
        "2016", "FAA-H-8083-3B", "" );
insert_book( "handbooks", "faa-h-8083-25b-2016.pdf", "Pilot's Handbook of Aeronautical Knowledge",
        "2016", "FAA-H-8083-25B", "" );
insert_book( "handbooks", "faa-h-8083-15b-2012.pdf", "Instrument Flying Handbook",
        "2012", "FAA-H-8083-15B", "" );
insert_book( "handbooks", "faa-h-8083-16b-2017.pdf", "Instrument Procedures Handbook",
        "2017", "FAA-H-8083-16b", "" );
insert_book( "handbooks", "faa-h-8083-6-2009.pdf", "Advanced Avionics Handbook",
        "2009", "FAA-H-8083-6", "" );
insert_book( "handbooks", "faa-h-8083-27a-2006.pdf", "Student Pilot Guide",
        "2016", "FAA-H-8083-27A", "" );
insert_book( "handbooks", "faa-h-8083-9a-2008.pdf", "Aviation Instructor's Handbook",
        "2008", "FAA-H-8083-9A", "" );
insert_book( "handbooks", "faa-h-8083-1b-2016.pdf", "Weight and Balance Handbook",
        "2016", "FAA-H-8083-1B", "" );
insert_book( "handbooks", "faa-h-8083-21a-2012.pdf", "Helicopter Flying Handbook",
        "2012", "FAA-H-8083-21A", "" );
insert_book( "handbooks", "faa-fs-i-8700-1-2003.pdf", "Information for Banner Tow Operations",
        "2003", "FAA/FS-I-8799-1", "" );
insert_book( "handbooks", "fcm-h1-2017.pdf", "Federal Meteorological Handbook No. 1",
        "2017", "NOAA", "" );
insert_book( "handbooks", "fcm-h2-1988.pdf", "Federal Meteorological Handbook No. 2",
        "1988", "NOAA", "" );

insert_book( "manuals", "cug_190620.pdf", "Aeronautical Chart User's Guide",
        "Jun 2019", "FAA", "" );
insert_book( "manuals", "faa-h-8083-19a-2008.pdf", "Plane Sense - General Aviation Information",
        "2008", "FAA-H-8083-19A", "" );
insert_book( "manuals", "ac-00-6b.pdf", "Aviation Weather",
        "2016", "AC-00-6B", "" );
insert_book( "manuals", "ac-00-45h_chg_1.pdf", "Aviation Weather Services",
        "2016", "AC-00-45H", "" );
insert_book( "manuals", "00-80T-80.pdf", "Aerodynamics for Naval Aviators",
        "1965", "H. H. Hurt, Jr.", "" );

insert_book( "periodicals", "cb_474.pdf", "Callback",
        "Issue 474 (Jul 2019)", "NASA", "" );
insert_book( "periodicals", "cb_473.pdf", "Callback",
        "Issue 473 (Jun 2019)", "NASA", "" );
insert_book( "periodicals", "cb_472.pdf", "Callback",
        "Issue 472 (May 2019)", "NASA", "" );
insert_book( "periodicals", "cb_471.pdf", "Callback",
        "Issue 471 (Apr 2019)", "NASA", "" );
insert_book( "periodicals", "JulAug2019.pdf", "FAA Safety Briefing",
        "Jul/Aug 2019", "FAA", "" );
insert_book( "periodicals", "MayJun2019.pdf", "FAA Safety Briefing",
        "May/Jun 2019", "FAA", "" );
insert_book( "periodicals", "JanFeb2019.pdf", "FAA Safety Briefing",
        "Jan/Feb 2019", "FAA", "" );
insert_book( "periodicals", "NovDec2018.pdf", "FAA Safety Briefing",
        "Nov/Dec 2018", "FAA", "" );

insert_book( "safety", "ac_90-48d-chg_1.pdf", "Pilots’ Role in Collision Avoidance",
        "2016", "FAA", "" );
insert_book( "safety", "safety_advisor_non-towered_airports.pdf", "Operations at Nontowered Airports",
        "2007", "FAA", "" );
insert_book( "safety", "faa-p-8740-02-2008.pdf", "Density Altitude",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-12-2008.pdf", "Thunderstorms",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-30-2008.pdf", "How to Obtain a Good Weather Briefing",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-36-2008.pdf", "Proficiency and the Private Pilot",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-48-2008.pdf", "On Landings Part 1",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-49-2008.pdf", "On Landings Part 2",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-50-2008.pdf", "On Landings Part 3",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-66-2008.pdf", "Flying Light Twins Safely",
        "2008", "FAA", "" );
insert_book( "safety", "faa-p-8740-69-2008.pdf", "Aeronautical Decision Making",
        "2008", "FAA", "" );
insert_book( "safety", "takeoff_safety.pdf", "Pilot Guide to Takeoff Safety",
        "2008", "FAA", "" );
insert_book( "safety", "AP_UpsetRecovery_Book.pdf", "Upset Recovery Training Aid",
        "2008", "FAA", "" );
insert_book( "safety", "tfrweb_2003.pdf.gz", "Pilot's Guide to TFR",
        "2003", "FAA", "" );

insert_book( "regs", "aim_190228.pdf", "AIM Change 1, 2 and 3",
        "Feb 2019", "FAA", "" );
insert_book( "regs", "CFR-2019-title14-vol1.pdf", "FAR Volume 1",
        "Parts 1-59", "US GPO 2019", "" );
insert_book( "regs", "CFR-2019-title14-vol2.pdf", "FAR Volume 2",
        "Parts 60-109", "US GPO 2019", "" );
insert_book( "regs", "CFR-2019-title14-vol3.pdf", "FAR Volume 3",
        "Parts 110-199", "US GPO 2019", "" );
insert_book( "regs", "CFR-2019-title14-vol4.pdf", "FAR Volume 4",
        "Parts 200-1199", "US GPO 2019", "" );
insert_book( "regs", "CFR-2019-title14-vol5.pdf", "FAR Volume 5",
        "Parts 1200-1399", "US GPO 2019", "" );

insert_book( "risk", "faa-h-8083-2-2009.pdf", "Risk Management Handbook", "2016", "FAA-H-8083-2", "" );
insert_book( "risk", "wx_decision.pdf", "Weather Decision Making", "2009", "FAA", "" );
insert_book( "risk", "local_vfr.pdf", "Local VFR Flying", "2012", "FAA", "" );
insert_book( "risk", "night_vfr.pdf", "Night VFR Flying", "2012", "FAA", "" );
insert_book( "risk", "pract_riskman.pdf", "Practical Risk Management", "2012", "FAA", "" );
insert_book( "risk", "vfr_xc.pdf", "VFR XC Flying", "2012", "FAA", "" );

insert_book( "circular", "ac-60-22-1991.pdf", "Aeronautical Decision Making",
        "1991", "AC-60-22", "" );
insert_book( "circular", "ac-120-100-2010.pdf", "Basics of Aviation Fatigue",
        "2010", "AC-120-100", "" );
insert_book( "circular", "ac-91-74b-2015.pdf", "Flight In Icing Conditions",
        "2015", "AC-91-74B", "" );
insert_book( "circular", "ac-00-24c-2013.pdf", "Thunderstorms",
        "2013", "AC-00-24C", "" );
insert_book( "circular", "ac-00-54-1988.pdf", "Pilot Windshear Guide",
        "1988", "AC-00-54", "" );
insert_book( "circular", "ac-00-46e-2011.pdf", "Aviation Safety Reporting Program",
        "2011", "AC-00-46E", "" );
insert_book( "circular", "ac-61-134-2003.pdf", "Controlled Flight Into Terrain Awareness",
        "2003", "AC-61-134", "" );
insert_book( "circular", "ac-61-65h-2018.pdf", "Pilots and Flight and Ground Instructors",
        "2018", "AC-61-65H", "" );
insert_book( "circular", "ac-120-76d-2017.pdf", "Use of Electronics Flight Bags",
        "2017", "AC-120-76D", "" );
insert_book( "circular", "ac-120-12a-1986.pdf", "Private Carriage vs Common Carriage",
        "1986", "AC-120-12A", "" );

insert_book( "testguide", "FAA-G-8082-17I_1702.pdf", "Private Pilot Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-17I", "" );
insert_book( "testguide", "FAA-G-8082-13I_1702.pdf", "Instrument Rating Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-13I", "" );
insert_book( "testguide", "FAA-G-8082-5H_1702.pdf", "Commercial Pilot Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-5H", "" );
insert_book( "testguide", "FAA-G-8082-7I_1702.pdf", "Flight Instructor Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-7I", "" );
insert_book( "testguide", "FAA-G-8082-4D_1702.pdf", "Sport Pilot Knowledge Test Guide",
        "Feb 2017", "FAA-G-8082-4D", "" );
insert_book( "testguide", "IPC_Guidance_150909.pdf", "Instrument Proficiency Check Guidance",
        "Sep 2015", "", "" );
insert_book( "testguide", "PARSampleExam_181015.pdf", "Airmen Knowledge Test Question Bank",
        "Oct 2018", "Private Airplane", "" );
insert_book( "testguide", "PRHSampleExam_190628.pdf", "Airmen Knowledge Test Question Bank",
        "Jun 2019", "Private Helicopter", "" );
insert_book( "testguide", "IRASampleExam_190628.pdf", "Airmen Knowledge Test Question Bank",
        "Jun 2019", "Instrument", "" );
insert_book( "testguide", "LSPSampleExam_181015.pdf", "Airmen Knowledge Test Question Bank",
        "Oct 2018", "Light Sport", "" );
insert_book( "testguide", "cax_sample_exam_190628.pdf", "Airmen Knowledge Test Question Bank",
        "Jun 2019", "Commercial", "" );

insert_book( "pts", "FAA-S-ACS-7A_180611.pdf", "Commercial Pilot ACS",
        "Jun 2018", "FAA-S-ACS-7A", "" );
insert_book( "pts", "FAA-S-ACS-6B_180611.pdf", "Private Pilot ACS",
        "Jun 2018", "FAA-S-ACS-6B", "" );
insert_book( "pts", "faa-s-8081-29.pdf", "Sport Pilot PTS",
        "May 2014", "FAA-S-8081-29", "" );
insert_book( "pts", "faa-s-8081-3a.pdf", "Recreational Pilot PTS",
        "Aug 2006", "FAA-S-8081-3A", "" );
insert_book( "pts", "FAA-S-ACS-8B_180611.pdf", "Instrument Rating ACS",
        "Jun 2018", "FAA-S-ACS-8B", "" );
insert_book( "pts", "faa-s-8081-5f.pdf", "Airline Transport Pilot PTS",
        "Jul 2014", "FAA-S-8081-5F", "" );
insert_book( "pts", "FAA-S-8081-6D_180419.pdf", "Flight Instructor PTS",
        "Apr 2018", "FAA-S-8081-6D", "" );
insert_book( "pts", "faa-s-8081-9d.pdf", "Flight Instructor Instrument PTS",
        "Jul 2010", "FAA-S-8081-9D", "" );

insert_book( "misc", "aip_190228.pdf", "Aeronautical Information Publication",
        "Feb 2019", "FAA", "" );
insert_book( "misc", "NAT_IGA_2004.pdf", "North Atlantic Operations Manual",
        "3rd Edition", "USA", "" );
insert_book( "misc", "pcg_190228.pdf", "Pilot/Controller Glossary",
        "Feb 2019", "FAA", "" );
insert_book( "misc", "7110.10AA_fss_190815.pdf", "Flight Services",
        "Aug 2019", "JO 7110.10AA", "" );
insert_book( "misc", "7110.65Y_atc_190815.pdf", "Air Traffic Control",
        "Aug 2019", "JO 7110.65Y", "" );
insert_book( "misc", "7110.118A_lahso_160130.pdf", "Land and Hold Short Operations",
        "Jan 2016", "JO 7110.118A", "" );
insert_book( "misc", "faa_airspacecard_2014.pdf", "Airspace Card",
        "Jun 2014", "FAA", "" );

insert_book( "canada", "AIM_2018-2_EN_181011.pdf.gz", "Aeronautical Information Manual (TC AIM)",
        "Oct 2018", "Nav Canada", "" );
insert_book( "canada", "AWS-Guide-EN_1705.pdf", "Aviation Weather Service Guide",
        "May 2017", "Nav Canada", "" );
insert_book( "canada", "Customer-Guide-Charges-EN_1809.pdf", "Customer Guide to Charges",
        "Sep 2018", "Nav Canada", "" );
insert_book( "canada", "VFR_Phraseology_1505.pdf", "VFR Phraseology",
        "May 2015", "Nav Canada", "" );
insert_book( "canada", "NOTAM-Manual-EN_1603.pdf", "Canadian NOTAM Procedures Manual",
        "Mar 2016", "Nav Canada", "" );
insert_book( "canada", "A34E-W.pdf", "The Weather of Atlantic Canada and Eastern Quebec",
        "Nov 2005", "Nav Canada", "" );
insert_book( "canada", "BC31E-W.pdf", "The Weather of British Columbia",
        "Nov 2005", "Nav Canada", "" );
insert_book( "canada", "N3637E-W.pdf", "The Weather of Nunavut and the Arctic",
        "Oct 2005", "Nav Canada", "" );
insert_book( "canada", "OQ33E-W.pdf", "The Weather of Ontario and Quebec",
        "Nov 2005", "Nav Canada", "" );
insert_book( "canada", "P32E-W.pdf", "The Weather of the Canadian Prairies",
        "Nov 2005", "Nav Canada", "" );
insert_book( "canada", "Y35E-W.pdf", "The Weather of the Yukon, NWT and Western Nunavut",
        "Nov 2005", "Nav Canada", "" );

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

sub insert_book( $$$$$$ )
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
    my $size = -s $base."/".$category."/".$name.".gz";
    $sth_library->bind_param( 6, $size );
    #FLAG
    my $flag = shift;
    $sth_library->bind_param( 7, $flag );

    $sth_library->execute();
}
