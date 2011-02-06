#!/usr/bin/perl

#/*
# * Airports for Android
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

my $reTrim = qr/^\s+|\s+$/;

sub substrim($$$)
{
    my ( $string, $offset, $len ) = @_;
    $string = substr( $string, $offset, $len );
    $string =~ s/$reTrim//g;
    return $string;
}

my $dbfile = "fadds.db";
my $APT = shift @ARGV;

open( APT_FILE, "<$APT" ) or die "Could not open data file\n";

my $create_metadata_table = "CREATE TABLE  android_metadata ( locale TEXT );";

my $insert_metadata_record = "INSERT INTO android_metadata VALUES ('en_US');";

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
        ."? ,?, ?, ?, ?, ?, ?, ?, ?)";

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
        ."RECIPROCAL_END_GRADIENT REAL, "
        ."RECIPROCAL_END_GRADIENT_DIRECTION TEXT, "
        ."RECIPROCAL_END_TORA INTEGER, "
        ."RECIPROCAL_END_TODA INTEGER, "
        ."RECIPROCAL_END_ASDA INTEGER, "
        ."RECIPROCAL_END_LDA INTEGER, "
        ."RECIPROCAL_END_LAHSO_DISTANCE INTEGER"
        .");";

my $insert_runways_record = "INSERT INTO runways ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."RUNWAY_LENGTH, "
        ."RUNWAY_WIDTH, "
        ."SURFACE_TYPE, "
        ."SURFACE_TREATMENT, "
        ."EDGE_LIGHTS_INTENSITY, "
        ."BASE_END_ID, "
        ."BASE_END_HEADING, "
        ."BASE_END_ILS_TYPE, "
        ."BASE_END_RIGHT_TRAFFIC, "
        ."BASE_END_MARKING_TYPE, "
        ."BASE_END_MARKING_CONDITION, "
        ."BASE_END_LATTITUDE_SECONDS, "
        ."BASE_END_LONGITUDE_SECONDS, "
        ."BASE_END_RUNWAY_ELEVATION, "
        ."BASE_END_THRESHOLD_CROSSING_HEIGHT, "
        ."BASE_END_GLIDE_ANGLE, "
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
        ."RECIPROCAL_END_ID, "
        ."RECIPROCAL_END_HEADING, "
        ."RECIPROCAL_END_ILS_TYPE, "
        ."RECIPROCAL_END_RIGHT_TRAFFIC, "
        ."RECIPROCAL_END_MARKING_TYPE, "
        ."RECIPROCAL_END_MARKING_CONDITION, "
        ."RECIPROCAL_END_LATTITUDE_SECONDS, "
        ."RECIPROCAL_END_LONGITUDE_SECONDS, "
        ."RECIPROCAL_END_RUNWAY_ELEVATION, "
        ."RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT, "
        ."RECIPROCAL_END_GLIDE_ANGLE, "
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
        ."RECIPROCAL_END_GRADIENT, "
        ."RECIPROCAL_END_GRADIENT_DIRECTION, "
        ."RECIPROCAL_END_TORA, "
        ."RECIPROCAL_END_TODA, "
        ."RECIPROCAL_END_ASDA, "
        ."RECIPROCAL_END_LDA, "
        ."RECIPROCAL_END_LAHSO_DISTANCE"
        .") VALUES ("
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

$dbh->do( "DROP TABLE IF EXISTS android_metadata" );
$dbh->do( $create_metadata_table );
$dbh->do( $insert_metadata_record );

$dbh->do( "DROP TABLE IF EXISTS airports" );
$dbh->do( $create_airports_table );
#$dbh->do( "CREATE INDEX idx_faa_code on airports ( FAA_CODE );" );
#$dbh->do( "CREATE INDEX idx_icao_code on airports ( ICAO_CODE );" );
#$dbh->do( "CREATE INDEX idx_name on airports ( FACILITY_NAME );" );
#$dbh->do( "CREATE INDEX idx_city on airports ( ASSOC_CITY );" );

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

    my $type = substrim( $line, 0, 3 );

    if ( $type eq "APT" )
    {
        #SITE_NUMBER
        $sth_apt->bind_param(  1, substrim( $line,    3, 11 ) );
        #FACILITY_TYPE
        $sth_apt->bind_param(  2, substrim( $line,   14, 13 ) );
        #FAA_CODE
        $sth_apt->bind_param(  3, substrim( $line,   27,  4 ) );
        #EFFECTIVE_DATE
        $sth_apt->bind_param(  4, substrim( $line,   31, 10 ) );
        #REGION_CODE
        $sth_apt->bind_param(  5, substrim( $line,   41,  3 ) );
        #DISTRICT_CODE
        $sth_apt->bind_param(  6, substrim( $line,   44,  4 ) );
        #ASSOC_STATE
        $sth_apt->bind_param(  7, substrim( $line,   48,  2 ) );
        #ASSOC_COUNTY
        $sth_apt->bind_param(  8, substrim( $line,   70, 21 ) );
        #ASSOC_CITY
        $sth_apt->bind_param(  9, substrim( $line,   93, 40 ) );
        #FACILITY_NAME
        $sth_apt->bind_param( 10, substrim( $line,  133, 42 ) );
        #OWNERSHIP_TYPE
        $sth_apt->bind_param( 11, substrim( $line,  175,  2 ) );
        #FACILITY_USE
        $sth_apt->bind_param( 12, substrim( $line,  177,  2 ) );
        #OWNER_NAME
        $sth_apt->bind_param( 13, substrim( $line,  179, 35 ) );
        #OWNER_ADDRESS
        $sth_apt->bind_param( 14, substrim( $line,  214, 72 ) );
        #OWNER_CITY_STATE_ZIP
        $sth_apt->bind_param( 15, substrim( $line,  286, 45 ) );
        #OWNER_PHONE
        $sth_apt->bind_param( 16, substrim( $line,  331, 16 ) );
        #MANAGER_NAME
        $sth_apt->bind_param( 17, substrim( $line,  347, 35 ) );
        #MANAGER_ADDRESS
        $sth_apt->bind_param( 18, substrim( $line,  382, 72 ) );
        #MANAGER_CITY_STATE_ZIP
        $sth_apt->bind_param( 19, substrim( $line,  454, 45 ) );
        #MANAGER_PHONE
        $sth_apt->bind_param( 20, substrim( $line,  499, 16 ) );
        #REF_LATTITUDE_SECONDS
        $sth_apt->bind_param( 21, substrim( $line,  530, 11 ) );
        #REF_LATTITUDE_DECLINATION
        $sth_apt->bind_param( 22, substrim( $line,  541,  1 ) );
        #REF_LONGITUDE_SECONDS
        $sth_apt->bind_param( 23, substrim( $line,  557, 11 ) );
        #REF_LONGITUDE_DECLINATION
        $sth_apt->bind_param( 24, substrim( $line,  568,  1 ) );
        #REF_METHOD
        $sth_apt->bind_param( 25, substrim( $line,  569,  1 ) );
        #ELEVATION_MSL
        $sth_apt->bind_param( 26, substrim( $line,  570,  5 ) );
        #ELEVATION_METHOD
        $sth_apt->bind_param( 27, substrim( $line,  575,  1 ) );
        #MAGNETIC_VARIATION_DEGREES
        $sth_apt->bind_param( 28, substrim( $line,  576,  3 ) );
        #MAGNETIC_VARIATION_YEAR
        $sth_apt->bind_param( 29, substrim( $line,  579,  4 ) );
        #PATTERN_ALTITUDE_AGL
        $sth_apt->bind_param( 30, substrim( $line,  583,  4 ) );
        #SECTIONAL_CHART
        $sth_apt->bind_param( 31, substrim( $line,  587, 30 ) );
        #DISTANCE_FROM_CITY_NM
        $sth_apt->bind_param( 32, substrim( $line,  617,  2 ) );
        #DIRECTION_FROM_CITY
        $sth_apt->bind_param( 33, substrim( $line,  619,  3 ) );
        #BOUNDARY_ARTCC_ID
        $sth_apt->bind_param( 34, substrim( $line,  627,  4 ) );
        #BOUNDARY_ARTCC_NAME
        $sth_apt->bind_param( 35, substrim( $line,  634, 30 ) );
        #FSS_ON_SITE
        $sth_apt->bind_param( 36, substrim( $line,  701,  1 ) );
        #FSS_ID
        $sth_apt->bind_param( 37, substrim( $line,  702,  4 ) );
        #FSS_NAME
        $sth_apt->bind_param( 38, substrim( $line,  706, 30 ) );
        #FSS_LOCAL_PHONE
        $sth_apt->bind_param( 39, substrim( $line,  736, 16 ) );
        #FSS_TOLLFREE_PHONE
        $sth_apt->bind_param( 40, substrim( $line,  752, 16 ) );
        #NOTAM_FACILITY_ID
        $sth_apt->bind_param( 41, substrim( $line,  818,  4 ) );
        #NOTAM_D_AVAILABLE
        $sth_apt->bind_param( 42, substrim( $line,  822,  1 ) );
        #ACTIVATION_DATE
        $sth_apt->bind_param( 43, substrim( $line,  823,  7 ) );
        #STATUS_CODE
        $sth_apt->bind_param( 44, substrim( $line,  830,  2 ) );
        #INTL_ENTRY_AIRPORT
        $sth_apt->bind_param( 45, substrim( $line,  867,  1 ) );
        #CUSTOMS_LANDING_RIGHTS_AIRPORT
        $sth_apt->bind_param( 46, substrim( $line,  868,  1 ) );
        #CIVIL_MILITARY_JOINT_USE
        $sth_apt->bind_param( 47, substrim( $line,  869,  1 ) );
        #MILITARY_LANDING_RIGHTS
        $sth_apt->bind_param( 48, substrim( $line,  870,  1 ) );
        #FUEL_TYPES
        $sth_apt->bind_param( 49, substrim( $line,  914, 40 ) );
        #AIRFRAME_REPAIR_SERVICE
        $sth_apt->bind_param( 50, substrim( $line,  954,  5 ) );
        #POWER_PLANT_REPAIR_SERVICE
        $sth_apt->bind_param( 51, substrim( $line,  959,  5 ) );
        #BOTTLED_O2_AVAILABLE
        $sth_apt->bind_param( 52, substrim( $line,  964,  8 ) );
        #BULK_O2_AVAILABLE
        $sth_apt->bind_param( 53, substrim( $line,  972,  8 ) );
        #LIGHTING_SCHEDULE
        $sth_apt->bind_param( 54, substrim( $line,  980,  9 ) );
        #TOWER_ON_SITE
        $sth_apt->bind_param( 55, substrim( $line,  989,  1 ) );
        #UNICOM_FREQS
        $sth_apt->bind_param( 56, substrim( $line,  990, 42 ) );
        #CTAF_FREQ
        $sth_apt->bind_param( 57, substrim( $line, 1032,  7 ) );
        #SEGMENTED_CIRCLE
        $sth_apt->bind_param( 58, substrim( $line, 1039,  4 ) );
        #BEACON_COLOR
        $sth_apt->bind_param( 59, substrim( $line, 1043,  3 ) );
        #LANDING_FEE
        $sth_apt->bind_param( 60, substrim( $line, 1046,  1 ) );
        #STORAGE_FACILITY
        $sth_apt->bind_param( 61, substrim( $line, 1168, 12 ) );
        #OTHER_SERVICES
        $sth_apt->bind_param( 62, substrim( $line, 1180, 71 ) );
        #WIND_INDICATOR
        $sth_apt->bind_param( 63, substrim( $line, 1251,  3 ) );
        #ICAO_CODE
        $sth_apt->bind_param( 64, substrim( $line, 1254,  7 ) );

        $sth_apt->execute();
    }
    elsif ( $type eq "RWY" )
    {
        #SITE_NUMBER
        $sth_rwy->bind_param(  1, substrim( $line,    3, 11 ) );
        #RUNWAY_ID
        $sth_rwy->bind_param(  2, substrim( $line,   16,  7 ) );
        #RUNWAY_LENGTH
        $sth_rwy->bind_param(  3, substrim( $line,   23,  5 ) );
        #RUNWAY_WIDTH
        $sth_rwy->bind_param(  4, substrim( $line,   28,  4 ) );
        #SURFACE_TYPE
        $sth_rwy->bind_param(  5, substrim( $line,   32, 12 ) );
        #SURFACE_TREATMENT
        $sth_rwy->bind_param(  6, substrim( $line,   44,  5 ) );
        #EDGE_LIGHTS_INTENSITY
        $sth_rwy->bind_param(  7, substrim( $line,   60,  5 ) );
        #BASE_END_ID
        $sth_rwy->bind_param(  8, substrim( $line,   65,  3 ) );
        #BASE_END_HEADING
        $sth_rwy->bind_param(  9, substrim( $line,   68,  3 ) );
        #BASE_END_ILS_TYPE
        $sth_rwy->bind_param( 10, substrim( $line,   71, 10 ) );
        #BASE_END_RIGHT_TRAFFIC
        $sth_rwy->bind_param( 11, substrim( $line,   81,  1 ) );
        #BASE_END_MARKING_TYPE
        $sth_rwy->bind_param( 12, substrim( $line,   82,  5 ) );
        #BASE_END_MARKING_CONDITION
        $sth_rwy->bind_param( 13, substrim( $line,   87,  1 ) );
        #BASE_END_LATTITUDE_SECONDS
        $sth_rwy->bind_param( 14, substrim( $line,  109, 12 ) );
        #BASE_END_LONGITUDE_SECONDS
        $sth_rwy->bind_param( 15, substrim( $line,  136, 12 ) );
        #BASE_END_RUNWAY_ELEVATION
        $sth_rwy->bind_param( 16, substrim( $line,  148,  7 ) );
        #BASE_END_THRESHOLD_CROSSING_HEIGHT
        $sth_rwy->bind_param( 17, substrim( $line,  155,  3 ) );
        #BASE_END_GLIDE_ANGLE
        $sth_rwy->bind_param( 18, substrim( $line,  158,  4 ) );
        #BASE_END_DISPLACED_THRESHOLD_LENGTH
        $sth_rwy->bind_param( 19, substrim( $line,  223,  4 ) );
        #BASE_END_TDZ_ELEVATION
        $sth_rwy->bind_param( 20, substrim( $line,  227,  7 ) );
        #BASE_END_VISUAL_GLIDE_SLOPE
        $sth_rwy->bind_param( 21, substrim( $line,  234,  5 ) );
        #BASE_END_RVR_LOCATIONS
        $sth_rwy->bind_param( 22, substrim( $line,  239,  3 ) );
        #BASE_END_APCH_LIGHT_SYSTEM
        $sth_rwy->bind_param( 23, substrim( $line,  243,  8 ) );
        #BASE_END_REIL_AVAILABLE
        $sth_rwy->bind_param( 24, substrim( $line,  251,  1 ) );
        #BASE_END_CENTERLINE_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 25, substrim( $line,  252,  1 ) );
        #BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 26, substrim( $line,  253,  1 ) );
        #BASE_END_CONTROLLING_OBJECT
        $sth_rwy->bind_param( 27, substrim( $line,  254, 11 ) );
        #BASE_END_CONTROLLING_OBJECT_LIGHTED
        $sth_rwy->bind_param( 28, substrim( $line,  265,  4 ) );
        #BASE_END_CONTROLLING_OBJECT_SLOPE
        $sth_rwy->bind_param( 29, substrim( $line,  274,  2 ) );
        #BASE_END_CONTROLLING_OBJECT_HEIGHT
        $sth_rwy->bind_param( 30, substrim( $line,  276,  5 ) );
        #BASE_END_CONTROLLING_OBJECT_DISTANCE
        $sth_rwy->bind_param( 31, substrim( $line,  281,  5 ) );
        #BASE_END_CONTROLLING_OBJECT_OFFSET
        $sth_rwy->bind_param( 32, substrim( $line,  286,  7 ) );
        #RECIPROCAL_END_ID
        $sth_rwy->bind_param( 33, substrim( $line,  293,  3 ) );
        #RECIPROCAL_END_HEADING
        $sth_rwy->bind_param( 34, substrim( $line,  296,  3 ) );
        #RECIPROCAL_END_ILS_TYPE
        $sth_rwy->bind_param( 35, substrim( $line,  299, 10 ) );
        #RECIPROCAL_END_RIGHT_TRAFFIC
        $sth_rwy->bind_param( 36, substrim( $line,  309,  1 ) );
        #RECIPROCAL_END_MARKING_TYPE
        $sth_rwy->bind_param( 37, substrim( $line,  310,  5 ) );
        #RECIPROCAL_END_MARKING_CONDITION
        $sth_rwy->bind_param( 38, substrim( $line,  315,  1 ) );
        #RECIPROCAL_END_LATTITUDE_SECONDS
        $sth_rwy->bind_param( 40, substrim( $line,  337, 12 ) );
        #RECIPROCAL_END_LONGITUDE_SECONDS
        $sth_rwy->bind_param( 41, substrim( $line,  364, 12 ) );
        #RECIPROCAL_END_RUNWAY_ELEVATION
        $sth_rwy->bind_param( 42, substrim( $line,  376,  7 ) );
        #RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT
        $sth_rwy->bind_param( 43, substrim( $line,  383,  3 ) );
        #RECIPROCAL_END_GLIDE_ANGLE
        $sth_rwy->bind_param( 44, substrim( $line,  444,  7 ) );
        #RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH
        $sth_rwy->bind_param( 45, substrim( $line,  451,  4 ) );
        #RECIPROCAL_END_TDZ_ELEVATION
        $sth_rwy->bind_param( 46, substrim( $line,  455,  7 ) );
        #RECIPROCAL_END_VISUAL_GLIDE_SLOPE
        $sth_rwy->bind_param( 47, substrim( $line,  462,  5 ) );
        #RECIPROCAL_END_RVR_LOCATIONS
        $sth_rwy->bind_param( 48, substrim( $line,  467,  3 ) );
        #RECIPROCAL_END_APCH_LIGHT_SYSTEM
        $sth_rwy->bind_param( 49, substrim( $line,  471,  8 ) );
        #RECIPROCAL_END_REIL_AVAILABLE
        $sth_rwy->bind_param( 50, substrim( $line,  479,  1 ) );
        #RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 51, substrim( $line,  480,  1 ) );
        #RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 52, substrim( $line,  481,  1 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT
        $sth_rwy->bind_param( 53, substrim( $line,  482, 11 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED
        $sth_rwy->bind_param( 54, substrim( $line,  493,  4 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE
        $sth_rwy->bind_param( 55, substrim( $line,  502,  2 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT
        $sth_rwy->bind_param( 56, substrim( $line,  504,  5 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE
        $sth_rwy->bind_param( 57, substrim( $line,  509,  5 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET
        $sth_rwy->bind_param( 58, substrim( $line,  514,  7 ) );
        #BASE_END_GRADIENT
        $sth_rwy->bind_param( 59, substrim( $line,  571,  5 ) );
        #BASE_END_GRADIENT_DIRECTION
        $sth_rwy->bind_param( 60, substrim( $line,  576,  4 ) );
        #BASE_END_TORA
        $sth_rwy->bind_param( 61, substrim( $line,  710,  5 ) );
        #BASE_END_TODA
        $sth_rwy->bind_param( 62, substrim( $line,  715,  5 ) );
        #BASE_END_ASDA
        $sth_rwy->bind_param( 63, substrim( $line,  720,  5 ) );
        #BASE_END_LDA
        $sth_rwy->bind_param( 64, substrim( $line,  725,  5 ) );
        #BASE_END_LAHSO_DISTANCE
        $sth_rwy->bind_param( 65, substrim( $line,  730,  5 ) );
        #RECIPROCAL_END_GRADIENT
        $sth_rwy->bind_param( 66, substrim( $line,  862,  5  ) );
        #RECIPROCAL_END_GRADIENT_DIRECTION
        $sth_rwy->bind_param( 67, substrim( $line,  867,  4  ) );
        #RECIPROCAL_END_TORA
        $sth_rwy->bind_param( 68, substrim( $line, 1001,  5  ) );
        #RECIPROCAL_END_TODA
        $sth_rwy->bind_param( 69, substrim( $line, 1006,  5  ) );
        #RECIPROCAL_END_ASDA
        $sth_rwy->bind_param( 70, substrim( $line, 1011,  5  ) );
        #RECIPROCAL_END_LDA
        $sth_rwy->bind_param( 71, substrim( $line, 1016,  5  ) );
        #RECIPROCAL_END_LAHSO_DISTANCE
        $sth_rwy->bind_param( 72, substrim( $line, 1021,  5  ) );
 
        $sth_rwy->execute();
    }
    elsif ( $type eq "ATT" )
    {
        #SITE_NUMBER
        $sth_att->bind_param( 1, substrim( $line,  3,  11 ) );
        #SEQUENCE_NUMBER
        $sth_att->bind_param( 2, substrim( $line, 16,   2 ) );
        #ATTENDANCE_SCHEDULE
        $sth_att->bind_param( 3, substrim( $line, 18, 108 ) );
 
        $sth_att->execute();
    }
    elsif ( $type eq "RMK" )
    {
        #SITE_NUMBER
        $sth_rmk->bind_param( 1, substrim( $line,  3,  11 ) );
        #REMARK_NAME
        $sth_rmk->bind_param( 2, substrim( $line, 16,  11 ) );
        #REMARK_TEXT
        $sth_rmk->bind_param( 3, substrim( $line, 18, 700 ) );
 
        $sth_rmk->execute();
    }

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( " Done!\n" );

close APT_FILE;
$dbh->disconnect();

