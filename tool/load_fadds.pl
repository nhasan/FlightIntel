#!/usr/bin/perl

use strict;
use DBI;

my $dbfile = "fadds.db";
my $APT = shift @ARGV;

open( APT_FILE, "<$APT" ) or die "Could not open data file\n";

my $create_airports_table = "CREATE TABLE airports ("
                ."_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                ."SITE_NUMBER TEXT, "
                ."FACILITY_TYPE TEXT, "
                ."FAA_CODE TEXT, "
                ."EFFECTIVE_DATE TEXT, "
                ."REGION_CODE TEXT, "
                ."DISTRICT_CODE TEXT, "
                ."ASSOC_STATE TEXT, "
                ."ASSOC_COUNTY TEXT, "
                ."ASSOC_CITY TEXT, "
                ."FACILITY_NAME TEXT, "
                ."OWNERSHIP_TYPE TEXT, "
                ."FACILITY_USE TEXT, "
                ."OWNER_NAME TEXT, "
                ."OWNER_ADDRESS TEXT, "
                ."OWNER_CITY_STATE_ZIP TEXT, "
                ."OWNER_PHONE TEXT, "
                ."MANAGER_NAME TEXT, "
                ."MANAGER_ADDRESS TEXT, "
                ."MANAGER_CITY_STATE_ZIP TEXT, "
                ."MANAGER_PHONE TEXT, "
                ."REF_LATTITUDE_SECONDS REAL, "
                ."REF_LATTITUDE_DECLINATION TEXT, "
                ."REF_LONGITUDE_SECONDS REAL, "
                ."REF_LONGITUDE_DECLINATION TEXT, "
                ."REF_METHOD TEXT, "
                ."ELEVATION_MSL INTEGER, "
                ."ELEVATION_METHOD TEXT, "
                ."MAGNETIC_VARIATION_DEGREES INTEGER, "
                ."MAGNETIC_VARIATION_YEAR TEXT, "
                ."PATTERN_ALTITUDE_AGL INTEGER, "
                ."SECTIONAL_CHART TEXT, "
                ."DISTANCE_FROM_CITY_NM INTEGER, "
                ."DIRECTION_FROM_CITY TEXT, "
                ."BOUNDARY_ARTCC_ID TEXT, "
                ."BOUNDARY_ARTCC_NAME TEXT, "
                ."FSS_ON_SITE TEXT, "
                ."FSS_ID TEXT, "
                ."FSS_NAME TEXT, "
                ."FSS_LOCAL_PHONE TEXT, "
                ."FSS_TOLLFREE_PHONE TEXT, "
                ."NOTAM_FACILITY_ID TEXT, "
                ."NOTAM_D_AVAILABLE TEXT, "
                ."ACTIVATION_DATE TEXT, "
                ."STATUS_CODE TEXT, "
                ."INTL_ENTRY_AIRPORT TEXT, "
                ."CUSTOMS_LANDING_RIGHTS_AIRPORT TEXT, "
                ."CIVIL_MILITARY_JOINT_USE TEXT, "
                ."MILITARY_LANDING_RIGHTS TEXT, "
                ."FUEL_TYPES TEXT, "
                ."AIRFRAME_REPAIR_SERVICE TEXT, "
                ."POWER_PLANT_REPAIR_SERVICE TEXT, "
                ."BOTTLED_O2_AVAILABLE TEXT, "
                ."BULK_O2_AVAILABLE TEXT, "
                ."LIGHTING_SCHEDULE TEXT, "
                ."TOWER_ON_SITE TEXT, "
                ."UNICOM_FREQS TEXT, "
                ."CTAF_FREQ TEXT, "
                ."SEGMENTED_CIRCLE TEXT, "
                ."BEACON_COLOR TEXT, "
                ."LANDING_FEE TEXT, "
                ."BASED_SINGLE_ENGINE INTEGER, "
                ."BASED_MULTI_ENGINE INTEGER, "
                ."BASED_JET_ENGINE INTEGER, "
                ."BASED_HELICOPTER INTEGER, "
                ."BASED_GLIDER INTEGER, "
                ."BASED_MILITARY INTEGER, "
                ."BASED_ULTRA_LIGHT INTEGER, "
                ."OPS_ANNUAL_COMMERCIAL INTEGER, "
                ."OPS_ANNUAL_COMMUTER INTEGER, "
                ."OPS_ANNUAL_AIRTAXI INTEGER, "
                ."OPS_ANNUAL_GA_LOCAL INTEGER, "
                ."OPS_ANNUAL_GA_OTHER INTEGER, "
                ."OPS_ANNUAL_MILITARY INTEGER, "
                ."OPS_ANNUAL_DATE TEXT, "
                ."STORAGE_FACILITY TEXT, "
                ."OTHER_SERVICES TEXT, "
                ."WIND_INDICATOR TEXT, "
                ."ICAO_CODE TEXT "
                .");";

my $insert_airports_record = "INSERT INTO airports ("
                ."SITE_NUMBER, "
                ."FACILITY_TYPE, "
                ."FAA_CODE, "
                ."EFFECTIVE_DATE, "
                ."REGION_CODE, "
                ."DISTRICT_CODE, "
                ."ASSOC_STATE, "
                ."ASSOC_COUNTY, "
                ."ASSOC_CITY, "
                ."FACILITY_NAME, "
                ."OWNERSHIP_TYPE, "
                ."FACILITY_USE, "
                ."OWNER_NAME, "
                ."OWNER_ADDRESS, "
                ."OWNER_CITY_STATE_ZIP, "
                ."OWNER_PHONE, "
                ."MANAGER_NAME, "
                ."MANAGER_ADDRESS, "
                ."MANAGER_CITY_STATE_ZIP, "
                ."MANAGER_PHONE, "
                ."REF_LATTITUDE_SECONDS, "
                ."REF_LATTITUDE_DECLINATION, "
                ."REF_LONGITUDE_SECONDS, "
                ."REF_LONGITUDE_DECLINATION, "
                ."REF_METHOD, "
                ."ELEVATION_MSL, "
                ."ELEVATION_METHOD, "
                ."MAGNETIC_VARIATION_DEGREES, "
                ."MAGNETIC_VARIATION_YEAR, "
                ."PATTERN_ALTITUDE_AGL, "
                ."SECTIONAL_CHART, "
                ."DISTANCE_FROM_CITY_NM, "
                ."DIRECTION_FROM_CITY, "
                ."BOUNDARY_ARTCC_ID, "
                ."BOUNDARY_ARTCC_NAME, "
                ."FSS_ON_SITE, "
                ."FSS_ID, "
                ."FSS_NAME, "
                ."FSS_LOCAL_PHONE, "
                ."FSS_TOLLFREE_PHONE, "
                ."NOTAM_FACILITY_ID, "
                ."NOTAM_D_AVAILABLE, "
                ."ACTIVATION_DATE, "
                ."STATUS_CODE, "
                ."INTL_ENTRY_AIRPORT, "
                ."CUSTOMS_LANDING_RIGHTS_AIRPORT, "
                ."CIVIL_MILITARY_JOINT_USE, "
                ."MILITARY_LANDING_RIGHTS, "
                ."FUEL_TYPES, "
                ."AIRFRAME_REPAIR_SERVICE, "
                ."POWER_PLANT_REPAIR_SERVICE, "
                ."BOTTLED_O2_AVAILABLE, "
                ."BULK_O2_AVAILABLE, "
                ."LIGHTING_SCHEDULE, "
                ."TOWER_ON_SITE, "
                ."UNICOM_FREQS, "
                ."CTAF_FREQ, "
                ."SEGMENTED_CIRCLE, "
                ."BEACON_COLOR, "
                ."LANDING_FEE, "
                ."BASED_SINGLE_ENGINE, "
                ."BASED_MULTI_ENGINE, "
                ."BASED_JET_ENGINE, "
                ."BASED_HELICOPTER, "
                ."BASED_GLIDER, "
                ."BASED_MILITARY, "
                ."BASED_ULTRA_LIGHT, "
                ."OPS_ANNUAL_COMMERCIAL, "
                ."OPS_ANNUAL_COMMUTER, "
                ."OPS_ANNUAL_AIRTAXI, "
                ."OPS_ANNUAL_GA_LOCAL, "
                ."OPS_ANNUAL_GA_OTHER, "
                ."OPS_ANNUAL_MILITARY, "
                ."OPS_ANNUAL_DATE, "
                ."STORAGE_FACILITY, "
                ."OTHER_SERVICES, "
                ."WIND_INDICATOR, "
                ."ICAO_CODE"
        .") VALUES ("
        ."?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, "
        ."?)";

my $create_runways_table = "CREATE TABLE runways ("
        ."_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."RUNWAY_LENGTH INTEGER, "
        ."RUNWAY_WIDTH INTEGER, "
        ."SURFACE_TYPE TEXT, "
        ."SURFACE_TREATMENT TEXT, "
        ."EDGE_LIGHTS_INTENSITY TEXT, "
        ."BASE_END_ID TEXT,"
        ."BASE_END_HEADING TEXT, "
        ."BASE_END_ILS_TYPE TEXT, "
        ."BASE_END_RIGHT_TRAFFIC TEXT, "
        ."BASE_END_MARKING_TYPE TEXT, "
        ."BASE_END_MARKING_CONDITION TEXT, "
        ."BASE_END_ARRESTING_DEVICE_TYPE TEXT, "
        ."BASE_END_LATTITUDE_SECONDS REAL, "
        ."BASE_END_LONGITUDE_SECONDS REAL, "
        ."BASE_END_RUNWAY_ELEVATION REAL, "
        ."BASE_END_THRESHOLD_CROSSING_HEIGHT INTEGER, "
        ."BASE_END_GLIDE_ANGLE REAL, "
        ."BASE_END_DISPLACED_THRESHOLD_ELEVATION REAL, "
        ."BASE_END_DISPLACED_THRESHOLD_LENGTH INTEGER, "
        ."BASE_END_TDZ_ELEVATION INTEGER, "
        ."BASE_END_VISUAL_GLIDE_SLOPE TEXT, "
        ."BASE_END_RVR_LOCATIONS TEXT, "
        ."BASE_END_APCH_LIGHT_SYSTEM TEXT, "
        ."BASE_END_REIL_AVAILABLE TEXT, "
        ."BASE_END_CENTERLINE_LIGHTS_AVAILABLE TEXT, "
        ."BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE TEXT, "
        ."BASE_END_CONTROLLING_OBJECT TEXT, "
        ."BASE_END_CONTROLLING_OBJECT_LIGHTED TEXT, "
        ."BASE_END_CONTROLLING_OBJECT_SLOPE TEXT, "
        ."BASE_END_CONTROLLING_OBJECT_HEIGHT INTEGER, "
        ."BASE_END_CONTROLLING_OBJECT_DISTANCE INTEGER, "
        ."BASE_END_CONTROLLING_OBJECT_OFFSET TEXT, "
        ."RECIPROCAL_END_ID TEXT,"
        ."RECIPROCAL_END_HEADING TEXT, "
        ."RECIPROCAL_END_ILS_TYPE TEXT, "
        ."RECIPROCAL_END_RIGHT_TRAFFIC TEXT, "
        ."RECIPROCAL_END_MARKING_TYPE TEXT, "
        ."RECIPROCAL_END_MARKING_CONDITION TEXT, "
        ."RECIPROCAL_END_ARRESTING_DEVICE_TYPE TEXT, "
        ."RECIPROCAL_END_LATTITUDE_SECONDS REAL, "
        ."RECIPROCAL_END_LONGITUDE_SECONDS REAL, "
        ."RECIPROCAL_END_RUNWAY_ELEVATION REAL, "
        ."RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT INTEGER, "
        ."RECIPROCAL_END_GLIDE_ANGLE REAL, "
        ."RECIPROCAL_END_DISPLACED_THRESHOLD_ELEVATION REAL, "
        ."RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH INTEGER, "
        ."RECIPROCAL_END_TDZ_ELEVATION INTEGER, "
        ."RECIPROCAL_END_VISUAL_GLIDE_SLOPE TEXT, "
        ."RECIPROCAL_END_RVR_LOCATIONS TEXT, "
        ."RECIPROCAL_END_APCH_LIGHT_SYSTEM TEXT, "
        ."RECIPROCAL_END_REIL_AVAILABLE TEXT, "
        ."RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE TEXT, "
        ."RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE TEXT, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT TEXT, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED TEXT, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE TEXT, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT INTEGER, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE INTEGER, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET TEXT, "
        ."BASE_END_GRADIENT REAL, "
        ."BASE_END_GRADIENT_DIRECTION TEXT, "
        ."BASE_END_TORA INTEGER, "
        ."BASE_END_TODA INTEGER, "
        ."BASE_END_ASDA INTEGER, "
        ."BASE_END_LDA INTEGER, "
        ."BASE_END_LAHSO_DISTANCE INTEGER, "
        ."BASE_END_LAHSO_ENTITY_ID TEXT, "
        ."BASE_END_LAHSO_ENTITY_DESCRIPTION TEXT, "
        ."RECIPROCAL_END_GRADIENT REAL, "
        ."RECIPROCAL_END_GRADIENT_DIRECTION TEXT, "
        ."RECIPROCAL_END_TORA INTEGER, "
        ."RECIPROCAL_END_TODA INTEGER, "
        ."RECIPROCAL_END_ASDA INTEGER, "
        ."RECIPROCAL_END_LDA INTEGER, "
        ."RECIPROCAL_END_LAHSO_DISTANCE INTEGER, "
        ."RECIPROCAL_END_LAHSO_ENTITY_ID TEXT, "
        ."RECIPROCAL_END_LAHSO_ENTITY_DESCRIPTION TEXT"
        .");";

my $insert_runways_record = "INSERT INTO runways ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."RUNWAY_LENGTH, "
        ."RUNWAY_WIDTH, "
        ."SURFACE_TYPE, "
        ."SURFACE_TREATMENT, "
        ."EDGE_LIGHTS_INTENSITY, "
        ."BASE_END_ID,"
        ."BASE_END_HEADING, "
        ."BASE_END_ILS_TYPE, "
        ."BASE_END_RIGHT_TRAFFIC, "
        ."BASE_END_MARKING_TYPE, "
        ."BASE_END_MARKING_CONDITION, "
        ."BASE_END_ARRESTING_DEVICE_TYPE, "
        ."BASE_END_LATTITUDE_SECONDS, "
        ."BASE_END_LONGITUDE_SECONDS, "
        ."BASE_END_RUNWAY_ELEVATION, "
        ."BASE_END_THRESHOLD_CROSSING_HEIGHT, "
        ."BASE_END_GLIDE_ANGLE, "
        ."BASE_END_DISPLACED_THRESHOLD_ELEVATION, "
        ."BASE_END_DISPLACED_THRESHOLD_LENGTH, "
        ."BASE_END_TDZ_ELEVATION, "
        ."BASE_END_VISUAL_GLIDE_SLOPE, "
        ."BASE_END_RVR_LOCATIONS, "
        ."BASE_END_APCH_LIGHT_SYSTEM, "
        ."BASE_END_REIL_AVAILABLE, "
        ."BASE_END_CENTERLINE_LIGHTS_AVAILABLE, "
        ."BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE, "
        ."BASE_END_CONTROLLING_OBJECT, "
        ."BASE_END_CONTROLLING_OBJECT_LIGHTED, "
        ."BASE_END_CONTROLLING_OBJECT_SLOPE, "
        ."BASE_END_CONTROLLING_OBJECT_HEIGHT, "
        ."BASE_END_CONTROLLING_OBJECT_DISTANCE, "
        ."BASE_END_CONTROLLING_OBJECT_OFFSET, "
        ."RECIPROCAL_END_ID,"
        ."RECIPROCAL_END_HEADING, "
        ."RECIPROCAL_END_ILS_TYPE, "
        ."RECIPROCAL_END_RIGHT_TRAFFIC, "
        ."RECIPROCAL_END_MARKING_TYPE, "
        ."RECIPROCAL_END_MARKING_CONDITION, "
        ."RECIPROCAL_END_ARRESTING_DEVICE_TYPE, "
        ."RECIPROCAL_END_LATTITUDE_SECONDS, "
        ."RECIPROCAL_END_LONGITUDE_SECONDS, "
        ."RECIPROCAL_END_RUNWAY_ELEVATION, "
        ."RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT, "
        ."RECIPROCAL_END_GLIDE_ANGLE, "
        ."RECIPROCAL_END_DISPLACED_THRESHOLD_ELEVATION, "
        ."RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH, "
        ."RECIPROCAL_END_TDZ_ELEVATION, "
        ."RECIPROCAL_END_VISUAL_GLIDE_SLOPE, "
        ."RECIPROCAL_END_RVR_LOCATIONS, "
        ."RECIPROCAL_END_APCH_LIGHT_SYSTEM, "
        ."RECIPROCAL_END_REIL_AVAILABLE, "
        ."RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE, "
        ."RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE, "
        ."RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET, "
        ."BASE_END_GRADIENT, "
        ."BASE_END_GRADIENT_DIRECTION, "
        ."BASE_END_TORA, "
        ."BASE_END_TODA, "
        ."BASE_END_ASDA, "
        ."BASE_END_LDA, "
        ."BASE_END_LAHSO_DISTANCE, "
        ."BASE_END_LAHSO_ENTITY_ID, "
        ."BASE_END_LAHSO_ENTITY_DESCRIPTION, "
        ."RECIPROCAL_END_GRADIENT, "
        ."RECIPROCAL_END_GRADIENT_DIRECTION, "
        ."RECIPROCAL_END_TORA, "
        ."RECIPROCAL_END_TODA, "
        ."RECIPROCAL_END_ASDA, "
        ."RECIPROCAL_END_LDA, "
        ."RECIPROCAL_END_LAHSO_DISTANCE, "
        ."RECIPROCAL_END_LAHSO_ENTITY_ID, "
        ."RECIPROCAL_END_LAHSO_ENTITY_DESCRIPTION"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?)";
 
my $create_attendance_table = "CREATE TABLE attendance ("
        ."_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."SEQUENCE_NUMBER INTEGER, "
        ."ATTENDANCE_SCHEDULE TEXT"
        .");";

my $insert_attendance_record = "INSERT INTO attendance ("
        ."SITE_NUMBER, "
        ."SEQUENCE_NUMBER, "
        ."ATTENDANCE_SCHEDULE"
        .") VALUES ("
        ."?, ?, ?"
        .")";

my $create_remarks_table = "CREATE TABLE remarks ("
        ."_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."REMARK_NAME TEXT, "
        ."REMARK_TEXT TEXT"
        .");";

my $insert_remarks_record = "INSERT INTO remarks ("
        ."SITE_NUMBER, "
        ."REMARK_NAME, "
        ."REMARK_TEXT"
        .") VALUES ("
        ."?, ?, ?"
        .")";

my $dbh = DBI->connect( "dbi:SQLite:dbname=$dbfile", "", "" );

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

$dbh->do( "DROP TABLE IF EXISTS airports" );
$dbh->do( $create_airports_table );
$dbh->do( "CREATE INDEX idx_faa_code on airports ( FAA_CODE );" );
$dbh->do( "CREATE INDEX idx_icao_code on airports ( ICAO_CODE );" );
$dbh->do( "CREATE INDEX idx_name on airports ( FACILITY_NAME );" );
$dbh->do( "CREATE INDEX idx_city on airports ( ASSOC_CITY );" );

$dbh->do( "DROP TABLE IF EXISTS runways" );
$dbh->do( $create_runways_table );
$dbh->do( "CREATE INDEX idx_rwy_site_number on runways ( SITE_NUMBER );" );

$dbh->do( "DROP TABLE IF EXISTS attendance" );
$dbh->do( $create_attendance_table );
$dbh->do( "CREATE INDEX idx_att_site_number on attendance ( SITE_NUMBER );" );

$dbh->do( "DROP TABLE IF EXISTS remarks" );
$dbh->do( $create_remarks_table );
$dbh->do( "CREATE INDEX idx_rmk_site_number on remarks ( SITE_NUMBER );" );

my $sth_apt = $dbh->prepare( $insert_airports_record );
my $sth_rwy = $dbh->prepare( $insert_runways_record );
my $sth_att = $dbh->prepare( $insert_attendance_record );
my $sth_rmk = $dbh->prepare( $insert_remarks_record );

my $i = 0;

my $ofh = select STDOUT;
$| = 1;
select $ofh;

while ( my $line = <APT_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    my $type = substr( $line, 0, 3 );

    if ( $type eq "APT" )
    {
        $sth_apt->bind_param(  1, substr( $line,    3, 11 ) );
        $sth_apt->bind_param(  2, substr( $line,   14, 13 ) );
        $sth_apt->bind_param(  3, substr( $line,   27,  4 ) );
        $sth_apt->bind_param(  4, substr( $line,   31, 10 ) );
        $sth_apt->bind_param(  5, substr( $line,   41,  3 ) );
        $sth_apt->bind_param(  6, substr( $line,   44,  4 ) );
        $sth_apt->bind_param(  7, substr( $line,   48,  2 ) );
        $sth_apt->bind_param(  8, substr( $line,   70, 21 ) );
        $sth_apt->bind_param(  9, substr( $line,   93, 40 ) );
        $sth_apt->bind_param( 10, substr( $line,  133, 42 ) );
        $sth_apt->bind_param( 11, substr( $line,  175,  2 ) );
        $sth_apt->bind_param( 12, substr( $line,  177,  2 ) );
        $sth_apt->bind_param( 13, substr( $line,  179, 35 ) );
        $sth_apt->bind_param( 14, substr( $line,  214, 72 ) );
        $sth_apt->bind_param( 15, substr( $line,  286, 45 ) );
        $sth_apt->bind_param( 16, substr( $line,  331, 16 ) );
        $sth_apt->bind_param( 17, substr( $line,  347, 35 ) );
        $sth_apt->bind_param( 18, substr( $line,  382, 72 ) );
        $sth_apt->bind_param( 19, substr( $line,  454, 45 ) );
        $sth_apt->bind_param( 20, substr( $line,  499, 16 ) );
        $sth_apt->bind_param( 21, substr( $line,  530, 11 ) );
        $sth_apt->bind_param( 22, substr( $line,  541,  1 ) );
        $sth_apt->bind_param( 23, substr( $line,  557, 11 ) );
        $sth_apt->bind_param( 24, substr( $line,  568,  1 ) );
        $sth_apt->bind_param( 25, substr( $line,  569,  1 ) );
        $sth_apt->bind_param( 26, substr( $line,  570,  5 ) );
        $sth_apt->bind_param( 27, substr( $line,  575,  1 ) );
        $sth_apt->bind_param( 28, substr( $line,  576,  3 ) );
        $sth_apt->bind_param( 29, substr( $line,  579,  4 ) );
        $sth_apt->bind_param( 30, substr( $line,  583,  4 ) );
        $sth_apt->bind_param( 31, substr( $line,  587, 30 ) );
        $sth_apt->bind_param( 32, substr( $line,  617,  2 ) );
        $sth_apt->bind_param( 33, substr( $line,  619,  3 ) );
        $sth_apt->bind_param( 34, substr( $line,  627,  4 ) );
        $sth_apt->bind_param( 35, substr( $line,  634, 30 ) );
        $sth_apt->bind_param( 36, substr( $line,  701,  1 ) );
        $sth_apt->bind_param( 37, substr( $line,  702,  4 ) );
        $sth_apt->bind_param( 38, substr( $line,  706, 30 ) );
        $sth_apt->bind_param( 39, substr( $line,  736, 16 ) );
        $sth_apt->bind_param( 40, substr( $line,  752, 16 ) );
        $sth_apt->bind_param( 41, substr( $line,  818,  4 ) );
        $sth_apt->bind_param( 42, substr( $line,  822,  1 ) );
        $sth_apt->bind_param( 43, substr( $line,  823,  7 ) );
        $sth_apt->bind_param( 44, substr( $line,  830,  2 ) );
        $sth_apt->bind_param( 45, substr( $line,  867,  1 ) );
        $sth_apt->bind_param( 46, substr( $line,  868,  1 ) );
        $sth_apt->bind_param( 47, substr( $line,  869,  1 ) );
        $sth_apt->bind_param( 48, substr( $line,  870,  1 ) );
        $sth_apt->bind_param( 49, substr( $line,  914, 40 ) );
        $sth_apt->bind_param( 50, substr( $line,  954,  5 ) );
        $sth_apt->bind_param( 51, substr( $line,  959,  5 ) );
        $sth_apt->bind_param( 52, substr( $line,  964,  8 ) );
        $sth_apt->bind_param( 53, substr( $line,  972,  8 ) );
        $sth_apt->bind_param( 54, substr( $line,  980,  9 ) );
        $sth_apt->bind_param( 55, substr( $line,  989,  1 ) );
        $sth_apt->bind_param( 56, substr( $line,  990, 42 ) );
        $sth_apt->bind_param( 57, substr( $line, 1032,  7 ) );
        $sth_apt->bind_param( 58, substr( $line, 1039,  4 ) );
        $sth_apt->bind_param( 59, substr( $line, 1043,  3 ) );
        $sth_apt->bind_param( 60, substr( $line, 1046,  1 ) );
        $sth_apt->bind_param( 61, substr( $line, 1048,  3 ) );
        $sth_apt->bind_param( 62, substr( $line, 1051,  3 ) );
        $sth_apt->bind_param( 63, substr( $line, 1054,  3 ) );
        $sth_apt->bind_param( 64, substr( $line, 1057,  3 ) );
        $sth_apt->bind_param( 65, substr( $line, 1060,  3 ) );
        $sth_apt->bind_param( 66, substr( $line, 1063,  3 ) );
        $sth_apt->bind_param( 67, substr( $line, 1066,  3 ) );
        $sth_apt->bind_param( 68, substr( $line, 1069,  6 ) );
        $sth_apt->bind_param( 69, substr( $line, 1075,  6 ) );
        $sth_apt->bind_param( 70, substr( $line, 1081,  6 ) );
        $sth_apt->bind_param( 71, substr( $line, 1087,  6 ) );
        $sth_apt->bind_param( 72, substr( $line, 1093,  6 ) );
        $sth_apt->bind_param( 73, substr( $line, 1099,  6 ) );
        $sth_apt->bind_param( 74, substr( $line, 1105, 10 ) );
        $sth_apt->bind_param( 75, substr( $line, 1168, 12 ) );
        $sth_apt->bind_param( 76, substr( $line, 1180, 71 ) );
        $sth_apt->bind_param( 77, substr( $line, 1251,  3 ) );
        $sth_apt->bind_param( 78, substr( $line, 1254,  7 ) );

        $sth_apt->execute();
    }
    elsif ( $type eq "RWY" )
    {
        $sth_rwy->bind_param(  1, substr( $line,    3, 11 ) );
        $sth_rwy->bind_param(  2, substr( $line,   16,  7 ) );
        $sth_rwy->bind_param(  3, substr( $line,   23,  5 ) );
        $sth_rwy->bind_param(  4, substr( $line,   28,  4 ) );
        $sth_rwy->bind_param(  5, substr( $line,   32, 12 ) );
        $sth_rwy->bind_param(  6, substr( $line,   44,  5 ) );
        $sth_rwy->bind_param(  7, substr( $line,   60,  5 ) );
        $sth_rwy->bind_param(  8, substr( $line,   65,  3 ) );
        $sth_rwy->bind_param(  9, substr( $line,   68,  3 ) );
        $sth_rwy->bind_param( 10, substr( $line,   71, 10 ) );
        $sth_rwy->bind_param( 11, substr( $line,   81,  1 ) );
        $sth_rwy->bind_param( 12, substr( $line,   82,  5 ) );
        $sth_rwy->bind_param( 13, substr( $line,   87,  1 ) );
        $sth_rwy->bind_param( 14, substr( $line,   88,  6 ) );
        $sth_rwy->bind_param( 15, substr( $line,  109, 12 ) );
        $sth_rwy->bind_param( 16, substr( $line,  136, 12 ) );
        $sth_rwy->bind_param( 17, substr( $line,  148,  7 ) );
        $sth_rwy->bind_param( 18, substr( $line,  155,  3 ) );
        $sth_rwy->bind_param( 19, substr( $line,  158,  4 ) );
        $sth_rwy->bind_param( 20, substr( $line,  216,  7 ) );
        $sth_rwy->bind_param( 21, substr( $line,  223,  4 ) );
        $sth_rwy->bind_param( 22, substr( $line,  227,  7 ) );
        $sth_rwy->bind_param( 23, substr( $line,  234,  5 ) );
        $sth_rwy->bind_param( 24, substr( $line,  239,  3 ) );
        $sth_rwy->bind_param( 25, substr( $line,  243,  8 ) );
        $sth_rwy->bind_param( 26, substr( $line,  251,  1 ) );
        $sth_rwy->bind_param( 27, substr( $line,  252,  1 ) );
        $sth_rwy->bind_param( 28, substr( $line,  253,  1 ) );
        $sth_rwy->bind_param( 29, substr( $line,  254, 11 ) );
        $sth_rwy->bind_param( 30, substr( $line,  265,  4 ) );
        $sth_rwy->bind_param( 31, substr( $line,  274,  2 ) );
        $sth_rwy->bind_param( 32, substr( $line,  276,  5 ) );
        $sth_rwy->bind_param( 33, substr( $line,  281,  5 ) );
        $sth_rwy->bind_param( 34, substr( $line,  286,  7 ) );
        $sth_rwy->bind_param( 35, substr( $line,  293,  3 ) );
        $sth_rwy->bind_param( 36, substr( $line,  296,  3 ) );
        $sth_rwy->bind_param( 37, substr( $line,  299, 10 ) );
        $sth_rwy->bind_param( 38, substr( $line,  309,  1 ) );
        $sth_rwy->bind_param( 39, substr( $line,  310,  5 ) );
        $sth_rwy->bind_param( 40, substr( $line,  315,  1 ) );
        $sth_rwy->bind_param( 41, substr( $line,  316,  6 ) );
        $sth_rwy->bind_param( 42, substr( $line,  337, 12 ) );
        $sth_rwy->bind_param( 43, substr( $line,  364, 12 ) );
        $sth_rwy->bind_param( 44, substr( $line,  376,  7 ) );
        $sth_rwy->bind_param( 45, substr( $line,  383,  3 ) );
        $sth_rwy->bind_param( 46, substr( $line,  386,  4 ) );
        $sth_rwy->bind_param( 47, substr( $line,  444,  7 ) );
        $sth_rwy->bind_param( 48, substr( $line,  451,  4 ) );
        $sth_rwy->bind_param( 49, substr( $line,  455,  7 ) );
        $sth_rwy->bind_param( 50, substr( $line,  462,  5 ) );
        $sth_rwy->bind_param( 51, substr( $line,  467,  3 ) );
        $sth_rwy->bind_param( 52, substr( $line,  471,  8 ) );
        $sth_rwy->bind_param( 53, substr( $line,  479,  1 ) );
        $sth_rwy->bind_param( 54, substr( $line,  480,  1 ) );
        $sth_rwy->bind_param( 55, substr( $line,  481,  1 ) );
        $sth_rwy->bind_param( 56, substr( $line,  482, 11 ) );
        $sth_rwy->bind_param( 57, substr( $line,  493,  4 ) );
        $sth_rwy->bind_param( 58, substr( $line,  502,  2 ) );
        $sth_rwy->bind_param( 59, substr( $line,  504,  5 ) );
        $sth_rwy->bind_param( 60, substr( $line,  509,  5 ) );
        $sth_rwy->bind_param( 61, substr( $line,  514,  7 ) );
        $sth_rwy->bind_param( 62, substr( $line,  571,  5 ) );
        $sth_rwy->bind_param( 63, substr( $line,  576,  4 ) );
        $sth_rwy->bind_param( 64, substr( $line,  710,  5 ) );
        $sth_rwy->bind_param( 65, substr( $line,  715,  5 ) );
        $sth_rwy->bind_param( 66, substr( $line,  720,  5 ) );
        $sth_rwy->bind_param( 67, substr( $line,  725,  5 ) );
        $sth_rwy->bind_param( 68, substr( $line,  730,  5 ) );
        $sth_rwy->bind_param( 69, substr( $line,  735,  7 ) );
        $sth_rwy->bind_param( 70, substr( $line,  742, 40 ) );
        $sth_rwy->bind_param( 71, substr( $line,  862,  5  ) );
        $sth_rwy->bind_param( 72, substr( $line,  867,  4  ) );
        $sth_rwy->bind_param( 73, substr( $line, 1001,  5  ) );
        $sth_rwy->bind_param( 74, substr( $line, 1006,  5  ) );
        $sth_rwy->bind_param( 75, substr( $line, 1011,  5  ) );
        $sth_rwy->bind_param( 76, substr( $line, 1016,  5  ) );
        $sth_rwy->bind_param( 77, substr( $line, 1021,  5  ) );
        $sth_rwy->bind_param( 78, substr( $line, 1026,  7  ) );
        $sth_rwy->bind_param( 79, substr( $line, 1033, 40  ) );
 
        $sth_rwy->execute();
    }
    elsif ( $type eq "ATT" )
    {
        $sth_att->bind_param( 1, substr( $line,  3,  11 ) );
        $sth_att->bind_param( 2, substr( $line, 16,   2 ) );
        $sth_att->bind_param( 3, substr( $line, 18, 108 ) );
 
        $sth_att->execute();
    }
    elsif ( $type eq "RMK" )
    {
        $sth_rmk->bind_param( 1, substr( $line,  3,  11 ) );
        $sth_rmk->bind_param( 2, substr( $line, 16,  11 ) );
        $sth_rmk->bind_param( 3, substr( $line, 18, 700 ) );
 
        $sth_rmk->execute();
    }

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( "Done loading $i records.\n" );

close APT_FILE;
$dbh->disconnect();

