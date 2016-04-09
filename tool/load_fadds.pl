#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

my %states = (
            "AK" => "Alaska",
            "AL" => "Alabama",
            "AS" => "American Samoa",
            "AZ" => "Arizona",
            "AR" => "Arkansas",
            "CA" => "California",
            "CN" => "Canada",
            "CO" => "Colorado",
            "CQ" => "N. Marinara Islands",
            "CT" => "Connecticut",
            "DE" => "Delaware",
            "DC" => "District of Columbia",
            "FM" => "Fed Sts of Micronesia",
            "FL" => "Florida",
            "GA" => "Georgia",
            "GU" => "Guam",
            "HI" => "Hawai",
            "IA" => "Iowa",
            "ID" => "Idaho",
            "IL" => "Illinois",
            "IN" => "Indiana",
            "IQ" => "Pacific Islands",
            "KS" => "Kansas",
            "KY" => "Kentucky",
            "LA" => "Louisiana",
            "MA" => "Massachusetts",
            "ME" => "Maine",
            "MH" => "Marshall Islands",
            "MD" => "Maryland",
            "MI" => "Michigan",
            "MN" => "Minnesota",
            "MS" => "Mississippi",
            "MO" => "Missouri",
            "MP" => "N. Marinara Islands",
            "MQ" => "Midway Islands",
            "MT" => "Montana",
            "NC" => "North Carolina",
            "ND" => "North Dakota",
            "NE" => "Nebraska",
            "NH" => "New Hampshire",
            "NJ" => "New Jersey",
            "NM" => "New Mexico",
            "NV" => "Nevada",
            "NY" => "New York",
            "OH" => "Ohio",
            "OK" => "Oklahoma",
            "OR" => "Oregon",
            "PA" => "Pennsylvania",
            "PR" => "Puerto Rico",
            "PS" => "Palau",
            "RI" => "Rhode Island",
            "SC" => "South Carolina",
            "SD" => "South Dakota",
            "TN" => "Tennessee",
            "TX" => "Texas",
            "UT" => "Utah",
            "VA" => "Virginia",
            "VI" => "Virgin Islands",
            "VT" => "Vermont",
            "WA" => "Washington",
            "WI" => "Wisconsin",
            "WV" => "West Virginia",
            "WY" => "Wyoming"
        );

sub substrim($$$)
{
    my ( $string, $offset, $len ) = @_;
    $string = substr( $string, $offset, $len );
    $string =~ s/$reTrim//g;
    return $string;
}

sub capitalize($$$)
{
    my ( $string, $offset, $len ) = @_;
    $string = autoformat( substr( $string, $offset, $len ), { case => 'highlight' } );
    $string =~ s/\s+(Ak|Al|Ar|As|Az|Ca|Co|Cq|Ct|Dc|De|Fl|Ga|Gu|Hi|Ia|Id|Il|In|Ks|Ky|La|Ma|Md|Me|Mi|Mn|Mo|Mq|Ms|Mt|Nc|Nd|Ne|Nh|Nj|Nm|Nv|Ny|Oh|Ok|Or|Pa|Pr|Ri|Sc|Sd|Tn|Tx|Ut|Va|Vi|Vt|Wa|Wi|Wv|Wy|Artcc|Llc|Norcal|Socal|Ifr|Vfr|Dot|Us|Usaf)\s+/ \U$1\E /g;
    $string =~ s/$reTrim//g;
    return $string;
}

my $dbfile = "fadds.db";
my $FADDS_BASE = shift @ARGV;

my %site_number;

my $dbh = DBI->connect( "dbi:SQLite:dbname=$dbfile", "", "" );

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

my $create_metadata_table = "CREATE TABLE android_metadata ( locale TEXT );";
my $insert_metadata_record = "INSERT INTO android_metadata VALUES ( 'en_US' );";

$dbh->do( "DROP TABLE IF EXISTS android_metadata" );
$dbh->do( $create_metadata_table );
$dbh->do( $insert_metadata_record );

my $create_states_table = "CREATE TABLE states ("
        ."STATE_CODE TEXT PRIMARY KEY, "
        ."STATE_NAME TEXT "
        .");";
my $insert_states_record = "INSERT INTO states (STATE_CODE, STATE_NAME) VALUES ( ?, ? );";

$dbh->do( "DROP TABLE IF EXISTS states" );
$dbh->do( $create_states_table );

my $sth_states = $dbh->prepare( $insert_states_record );

foreach my $state_code ( keys %states )
{
    $sth_states->bind_param( 1, $state_code );
    $sth_states->bind_param( 2, $states{$state_code} );
    $sth_states->execute();
}

my $create_airports_table = "CREATE TABLE airports ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
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
        ."REF_LATTITUDE_DEGREES REAL, "
        ."REF_LONGITUDE_DEGREES REAL, "
        ."REF_METHOD TEXT, "
        ."ELEVATION_MSL REAL, "
        ."ELEVATION_METHOD TEXT, "
        ."MAGNETIC_VARIATION_DEGREES INTEGER, "
        ."MAGNETIC_VARIATION_DIRECTION TEXT, "
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
        ."BEACON_LIGHTING_SCHEDULE TEXT, "
        ."TOWER_ON_SITE TEXT, "
        ."UNICOM_FREQS TEXT, "
        ."CTAF_FREQ TEXT, "
        ."SEGMENTED_CIRCLE TEXT, "
        ."BEACON_COLOR TEXT, "
        ."LANDING_FEE TEXT, "
        ."MEDICAL_USE TEXT, "
        ."SINGLE_ENGINE_COUNT INTEGER, "
        ."MULTI_ENGINE_COUNT INTEGER, "
        ."JET_ENGINE_COUNT INTEGER, "
        ."HELI_COUNT INTEGER, "
        ."GLIDERS_COUNT INTEGER, "
        ."MILITARY_COUNT INTEGER, "
        ."ULTRA_LIGHT_COUNT INTEGER, "
        ."ANNUAL_COMMERCIAL_OPS INTEGER, "
        ."ANNUAL_COMMUTER_OPS INTEGER, "
        ."ANNUAL_AIRTAXI_OPS INTEGER, "
        ."ANNUAL_GA_LOCAL_OPS INTEGER, "
        ."ANNUAL_GA_ININERANT_OPS INTEGER, "
        ."ANNUAL_MILITARY_OPS INTEGER, "
        ."OPS_REFERENCE_DATE TEXT, "
        ."CONTRACT_FUEL_AVAILABLE TEXT, "
        ."STORAGE_FACILITY TEXT, "
        ."OTHER_SERVICES TEXT, "
        ."WIND_INDICATOR TEXT, "
        ."ICAO_CODE TEXT, "
        ."TIMEZONE_ID TEXT"
        .");";

my $insert_airports_record = "INSERT INTO airports ("
        ."SITE_NUMBER, "
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
        ."REF_LATTITUDE_DEGREES, "
        ."REF_LONGITUDE_DEGREES, "
        ."REF_METHOD, "
        ."ELEVATION_MSL, "
        ."ELEVATION_METHOD, "
        ."MAGNETIC_VARIATION_DEGREES, "
        ."MAGNETIC_VARIATION_DIRECTION, "
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
        ."BEACON_LIGHTING_SCHEDULE, "
        ."TOWER_ON_SITE, "
        ."UNICOM_FREQS, "
        ."CTAF_FREQ, "
        ."SEGMENTED_CIRCLE, "
        ."BEACON_COLOR, "
        ."LANDING_FEE, "
        ."MEDICAL_USE, "
        ."SINGLE_ENGINE_COUNT, "
        ."MULTI_ENGINE_COUNT, "
        ."JET_ENGINE_COUNT, "
        ."HELI_COUNT, "
        ."GLIDERS_COUNT, "
        ."MILITARY_COUNT, "
        ."ULTRA_LIGHT_COUNT, "
        ."ANNUAL_COMMERCIAL_OPS, "
        ."ANNUAL_COMMUTER_OPS, "
        ."ANNUAL_AIRTAXI_OPS, "
        ."ANNUAL_GA_LOCAL_OPS, "
        ."ANNUAL_GA_ININERANT_OPS, "
        ."ANNUAL_MILITARY_OPS, "
        ."OPS_REFERENCE_DATE, "
        ."CONTRACT_FUEL_AVAILABLE, "
        ."STORAGE_FACILITY, "
        ."OTHER_SERVICES, "
        ."WIND_INDICATOR, "
        ."ICAO_CODE, "
        ."TIMEZONE_ID"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

my $create_runways_table = "CREATE TABLE runways ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."RUNWAY_LENGTH INTEGER, "
        ."RUNWAY_WIDTH INTEGER, "
        ."SURFACE_TYPE TEXT, "
        ."SURFACE_TREATMENT TEXT, "
        ."EDGE_LIGHTS_INTENSITY TEXT, "
        ."BASE_END_ID TEXT, "
        ."BASE_END_HEADING INTEGER, "
        ."BASE_END_ILS_TYPE TEXT, "
        ."BASE_END_RIGHT_TRAFFIC TEXT, "
        ."BASE_END_MARKING_TYPE TEXT, "
        ."BASE_END_MARKING_CONDITION TEXT, "
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
        ."RECIPROCAL_END_HEADING INTEGER, "
        ."RECIPROCAL_END_ILS_TYPE TEXT, "
        ."RECIPROCAL_END_RIGHT_TRAFFIC TEXT, "
        ."RECIPROCAL_END_MARKING_TYPE TEXT, "
        ."RECIPROCAL_END_MARKING_CONDITION TEXT, "
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
        ."BASE_END_LAHSO_RUNWAY TEXT, "
        ."RECIPROCAL_END_GRADIENT REAL, "
        ."RECIPROCAL_END_GRADIENT_DIRECTION TEXT, "
        ."RECIPROCAL_END_TORA INTEGER, "
        ."RECIPROCAL_END_TODA INTEGER, "
        ."RECIPROCAL_END_ASDA INTEGER, "
        ."RECIPROCAL_END_LDA INTEGER, "
        ."RECIPROCAL_END_LAHSO_DISTANCE INTEGER, "
        ."RECIPROCAL_END_LAHSO_RUNWAY TEXT"
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
        ."BASE_END_LAHSO_RUNWAY, "
        ."RECIPROCAL_END_GRADIENT, "
        ."RECIPROCAL_END_GRADIENT_DIRECTION, "
        ."RECIPROCAL_END_TORA, "
        ."RECIPROCAL_END_TODA, "
        ."RECIPROCAL_END_ASDA, "
        ."RECIPROCAL_END_LDA, "
        ."RECIPROCAL_END_LAHSO_DISTANCE, "
        ."RECIPROCAL_END_LAHSO_RUNWAY"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?)";
 
my $create_attendance_table = "CREATE TABLE attendance ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
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
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
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

my $create_ars_table = "CREATE TABLE ars ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."RUNWAY_END_ID TEXT, "
        ."ARRESTING_DEVICE TEXT"
        .");";

my $insert_ars_record = "INSERT INTO ars ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."RUNWAY_END_ID, "
        ."ARRESTING_DEVICE"
        .") VALUES ("
        ."?, ?, ?, ?"
        .")";

my $create_tower1_table = "CREATE TABLE tower1 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."FACILITY_ID TEXT, "
        ."SITE_NUMBER TEXT, "
        ."FACILITY_TYPE TEXT, "
        ."RADIO_CALL_TOWER TEXT, "
        ."RADIO_CALL_APCH TEXT, "
        ."RADIO_CALL_DEP TEXT "
        .");";

my $insert_tower1_record = "INSERT INTO tower1 ("
        ."FACILITY_ID, "
        ."SITE_NUMBER, "
        ."FACILITY_TYPE, "
        ."RADIO_CALL_TOWER, "
        ."RADIO_CALL_APCH, "
        ."RADIO_CALL_DEP"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?"
        .");";

my $create_tower3_table = "CREATE TABLE tower3 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."FACILITY_ID TEXT, "
        ."MASTER_AIRPORT_FREQ TEXT, "
        ."MASTER_AIRPORT_FREQ_USE TEXT"
        .");";

my $insert_tower3_record = "INSERT INTO tower3 ("
        ."FACILITY_ID, "
        ."MASTER_AIRPORT_FREQ, "
        ."MASTER_AIRPORT_FREQ_USE"
        .") VALUES ("
        ."?, ?, ?"
        .");";

my $create_tower6_table = "CREATE TABLE tower6 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."FACILITY_ID TEXT, "
        ."ELEMENT_NUMBER INTEGER, "
        ."REMARK_TEXT TEXT"
        .");";

my $insert_tower6_record = "INSERT INTO tower6 ("
        ."FACILITY_ID, "
        ."ELEMENT_NUMBER, "
        ."REMARK_TEXT"
        .") VALUES ("
        ."?, ?, ?"
        .");";

my $create_tower7_table = "CREATE TABLE tower7 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."FACILITY_ID TEXT, "
        ."SATELLITE_AIRPORT_FREQ_USE TEXT, "
        ."SATELLITE_AIRPORT_SITE_NUMBER TEXT, "
        ."MASTER_AIRPORT_SITE_NUMBER, "
        ."SATELLITE_AIRPORT_FREQ TEXT"
        .");";

my $insert_tower7_record = "INSERT INTO tower7 ("
        ."FACILITY_ID, "
        ."SATELLITE_AIRPORT_FREQ_USE, "
        ."SATELLITE_AIRPORT_SITE_NUMBER, "
        ."MASTER_AIRPORT_SITE_NUMBER, "
        ."SATELLITE_AIRPORT_FREQ"
        .") VALUES ("
        ."?, ?, ?, ?, ?"
        .");";

my $create_tower8_table = "CREATE TABLE tower8 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."FACILITY_ID TEXT, "
        ."AIRSPACE_TYPES TEXT, "
        ."AIRSPACE_HOURS TEXT"
        .");";

my $insert_tower8_record = "INSERT INTO tower8 ("
        ."FACILITY_ID, "
        ."AIRSPACE_TYPES, "
        ."AIRSPACE_HOURS"
        .") VALUES ("
        ."?, ?, ?"
        .");";

my $create_tower9_table = "CREATE TABLE tower9 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."FACILITY_ID TEXT, "
        ."ATIS_SERIAL_NO INTEGER, "
        ."ATIS_HOURS TEXT, "
        ."ATIS_PURPOSE TEXT, "
        ."ATIS_PHONE TEXT"
        .");";

my $insert_tower9_record = "INSERT INTO tower9 ("
        ."FACILITY_ID, "
        ."ATIS_SERIAL_NO, "
        ."ATIS_HOURS, "
        ."ATIS_PURPOSE, "
        ."ATIS_PHONE"
        .") VALUES ("
        ."?, ?, ?, ?, ?"
        .");";

my $create_awos1_table = "CREATE TABLE awos1 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."WX_SENSOR_IDENT TEXT, "
        ."WX_SENSOR_TYPE TEXT, "
        ."COMMISSIONING_STATUS TEXT, "
        ."COMMISSIONING_DATE TEXT, "
        ."NAVAID_FLAG TEXT, "
        ."STATION_LATTITUDE_DEGREES REAL, "
        ."STATION_LONGITUDE_DEGREES REAL, "
        ."STATION_ELEVATION REAL, "
        ."ELEVATION_METHOD TEXT, "
        ."STATION_FREQUENCY TEXT, "
        ."SECOND_STATION_FREQUENCY TEXT, "
        ."STATION_PHONE_NUMBER TEXT, "
        ."SITE_NUMBER TEXT"
        .");";

my $insert_awos1_record = "INSERT INTO awos1 ("
        ."WX_SENSOR_IDENT, "
        ."WX_SENSOR_TYPE, "
        ."COMMISSIONING_STATUS, "
        ."COMMISSIONING_DATE, "
        ."NAVAID_FLAG, "
        ."STATION_LATTITUDE_DEGREES, "
        ."STATION_LONGITUDE_DEGREES, "
        ."STATION_ELEVATION, "
        ."ELEVATION_METHOD, "
        ."STATION_FREQUENCY, "
        ."SECOND_STATION_FREQUENCY, "
        ."STATION_PHONE_NUMBER, "
        ."SITE_NUMBER"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?)";

my $create_awos2_table = "CREATE TABLE awos2 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."WX_SENSOR_IDENT TEXT, "
        ."WX_SENSOR_TYPE TEXT, "
        ."WX_STATION_REMARKS TEXT"
        .");";

my $insert_awos2_record = "INSERT INTO awos2 ("
        ."WX_SENSOR_IDENT, "
        ."WX_SENSOR_TYPE, "
        ."WX_STATION_REMARKS"
        .") VALUES ("
        ."?, ?, ?)";

my $create_nav1_table = "CREATE TABLE nav1 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."NAVAID_ID TEXT, "
        ."NAVAID_TYPE TEXT, "
        ."NAVAID_NAME TEXT, "
        ."ASSOC_CITY TEXT, "
        ."ASSOC_STATE TEXT, "
        ."PUBLIC_USE TEXT, "
        ."NAVAID_CLASS TEXT, "
        ."OPERATING_HOURS TEXT, "
        ."REF_LATTITUDE_DEGREES REAL, "
        ."REF_LONGITUDE_DEGREES REAL, "
        ."ELEVATION_MSL REAL, "
        ."MAGNETIC_VARIATION_DEGREES INTEGER, "
        ."MAGNETIC_VARIATION_DIRECTION TEXT, "
        ."MAGNETIC_VARIATION_YEAR TEXT, "
        ."VOICE_FEATURE TEXT, "
        ."POWER_OUTPUT TEXT, "
        ."AUTOMATIC_VOICE_IDENT TEXT, "
        ."RADIO_VOICE_CALL TEXT, "
        ."TACAN_CHANNEL TEXT, "
        ."NAVAID_FREQUENCY TEXT, "
        ."FANMARKER_TYPE TEXT, "
        ."PROTECTED_FREQUENCY_ALTITUDE TEXT"
        .")";

my $insert_nav1_record = "INSERT INTO nav1 ("
        ."NAVAID_ID, "
        ."NAVAID_TYPE, "
        ."NAVAID_NAME, "
        ."ASSOC_CITY, "
        ."ASSOC_STATE, "
        ."PUBLIC_USE, "
        ."NAVAID_CLASS, "
        ."OPERATING_HOURS, "
        ."REF_LATTITUDE_DEGREES, "
        ."REF_LONGITUDE_DEGREES, "
        ."ELEVATION_MSL, "
        ."MAGNETIC_VARIATION_DEGREES, "
        ."MAGNETIC_VARIATION_DIRECTION, "
        ."MAGNETIC_VARIATION_YEAR, "
        ."VOICE_FEATURE, "
        ."POWER_OUTPUT, "
        ."AUTOMATIC_VOICE_IDENT, "
        ."RADIO_VOICE_CALL, "
        ."TACAN_CHANNEL, "
        ."NAVAID_FREQUENCY, "
        ."FANMARKER_TYPE, "
        ."PROTECTED_FREQUENCY_ALTITUDE"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?)";

my $create_nav2_table = "CREATE TABLE nav2 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."NAVAID_ID TEXT, "
        ."NAVAID_TYPE TEXT, "
        ."REMARK_TEXT TEXT"
        .")";

my $insert_nav2_record = "INSERT INTO nav2 ("
        ."NAVAID_ID, "
        ."NAVAID_TYPE, "
        ."REMARK_TEXT"
        .") VALUES ("
        ."?, ?, ?"
        .")";

my $create_ils1_table = "CREATE TABLE ils1 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."ILS_TYPE TEXT, "
        ."ILS_ID TEXT, "
        ."ILS_CATEGORY TEXT, "
        ."ILS_MAGNETIC_BEARING REAL"
        .")";

my $insert_ils1_record = "INSERT INTO ils1 ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."ILS_TYPE, "
        ."ILS_ID, "
        ."ILS_CATEGORY, "
        ."ILS_MAGNETIC_BEARING"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?)";

my $create_ils2_table = "CREATE TABLE ils2 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."ILS_TYPE TEXT, "
        ."OPERATIONAL_STATUS TEXT, "
        ."OPERATIONAL_EFFECTIVE_DATE TEXT, "
        ."LOCALIZER_FREQUENCY TEXT, "
        ."LOCALIZER_BACK_COURSE_STATUS TEXT, "
        ."LOCALIZER_COURSE_WIDTH TEXT, "
        ."LOCALIZER_COURSE_WIDTH_THRESHOLD TEXT"
        .")";

my $insert_ils2_record = "INSERT INTO ils2 ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."ILS_TYPE, "
        ."OPERATIONAL_STATUS, "
        ."OPERATIONAL_EFFECTIVE_DATE, "
        ."LOCALIZER_FREQUENCY, "
        ."LOCALIZER_BACK_COURSE_STATUS, "
        ."LOCALIZER_COURSE_WIDTH, "
        ."LOCALIZER_COURSE_WIDTH_THRESHOLD"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?"
        .")";

my $create_ils3_table = "CREATE TABLE ils3 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."ILS_TYPE TEXT, "
        ."OPERATIONAL_STATUS TEXT, "
        ."OPERATIONAL_EFFECTIVE_DATE TEXT, "
        ."GLIDE_SLOPE_TYPE TEXT, "
        ."GLIDE_SLOPE_ANGLE TEXT, "
        ."GLIDE_SLOPE_FREQUENCY TEXT"
        .")";

my $insert_ils3_record = "INSERT INTO ils3 ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."ILS_TYPE, "
        ."OPERATIONAL_STATUS, "
        ."OPERATIONAL_EFFECTIVE_DATE, "
        ."GLIDE_SLOPE_TYPE, "
        ."GLIDE_SLOPE_ANGLE, "
        ."GLIDE_SLOPE_FREQUENCY"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?"
        .")";

my $create_ils4_table = "CREATE TABLE ils4 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."ILS_TYPE TEXT, "
        ."OPERATIONAL_STATUS TEXT, "
        ."OPERATIONAL_EFFECTIVE_DATE TEXT, "
        ."DME_CHANNEL TEXT"
        .")";

my $insert_ils4_record = "INSERT INTO ils4 ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."ILS_TYPE, "
        ."OPERATIONAL_STATUS, "
        ."OPERATIONAL_EFFECTIVE_DATE, "
        ."DME_CHANNEL"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?"
        .")";

my $create_ils5_table = "CREATE TABLE ils5 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."ILS_TYPE TEXT, "
        ."MARKER_TYPE TEXT, "
        ."OPERATIONAL_STATUS TEXT, "
        ."OPERATIONAL_EFFECTIVE_DATE TEXT, "
        ."MARKER_DISTANCE TEXT, "
        ."MARKER_DISTANCE_CENTERLINE TEXT, "
        ."MARKER_DIRECTION_CENTERLINE TEXT, "
        ."MARKER_ELEVATION_MSL REAL, "
        ."MARKER_FACILITY_TYPE TEXT, "
        ."MARKER_BEACON_ID TEXT, "
        ."MARKER_BEACON_NAME TEXT, "
        ."MARKER_BEACON_FREQUENCY TEXT, "
        ."MARKER_NAVAID_ID TEXT, "
        ."MARKER_SERVICE TEXT"
        .")";

my $insert_ils5_record = "INSERT INTO ils5 ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."ILS_TYPE, "
        ."MARKER_TYPE, "
        ."OPERATIONAL_STATUS, "
        ."OPERATIONAL_EFFECTIVE_DATE, "
        ."MARKER_DISTANCE, "
        ."MARKER_DISTANCE_CENTERLINE, "
        ."MARKER_DIRECTION_CENTERLINE, "
        ."MARKER_ELEVATION_MSL, "
        ."MARKER_FACILITY_TYPE, "
        ."MARKER_BEACON_ID, "
        ."MARKER_BEACON_NAME, "
        ."MARKER_BEACON_FREQUENCY, "
        ."MARKER_NAVAID_ID, "
        ."MARKER_SERVICE"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        ."?, ?, ?, ?, ?, ?"
        .")";

my $create_ils6_table = "CREATE TABLE ils6 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."SITE_NUMBER TEXT, "
        ."RUNWAY_ID TEXT, "
        ."ILS_TYPE TEXT, "
        ."ILS_REMARKS TEXT"
        .")";

my $insert_ils6_record = "INSERT INTO ils6 ("
        ."SITE_NUMBER, "
        ."RUNWAY_ID, "
        ."ILS_TYPE, "
        ."ILS_REMARKS"
        .") VALUES ("
        ."?, ?, ?, ?"
        .")";

my $create_aff1_table = "CREATE TABLE aff1 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."ARTCC_ID TEXT, "
        ."ARTCC_NAME TEXT, "
        ."SITE_LOCATION TEXT, "
        ."FACILITY_TYPE TEXT, "
        ."ARTCC_LATTITUDE_DEGREES REAL, "
        ."ARTCC_LONGITUDE_DEGREES REAL"
        .")";

my $insert_aff1_record = "INSERT INTO aff1 ("
        ."ARTCC_ID, "
        ."ARTCC_NAME, "
        ."SITE_LOCATION, "
        ."FACILITY_TYPE, "
        ."ARTCC_LATTITUDE_DEGREES, "
        ."ARTCC_LONGITUDE_DEGREES"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?"
        .")";

my $create_aff2_table = "CREATE TABLE aff2 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."ARTCC_ID TEXT, "
        ."SITE_LOCATION TEXT, "
        ."FACILITY_TYPE TEXT, "
        ."REMARK_ELEMENT_NO INTEGER, "
        ."REMARK_TEXT TEXT"
        .")";

my $insert_aff2_record = "INSERT INTO aff2 ("
        ."ARTCC_ID, "
        ."SITE_LOCATION, "
        ."FACILITY_TYPE, "
        ."REMARK_ELEMENT_NO, "
        ."REMARK_TEXT"
        .") VALUES ("
        ."?, ?, ?, ?, ?"
        .")";

my $create_aff3_table = "CREATE TABLE aff3 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."ARTCC_ID TEXT, "
        ."SITE_LOCATION TEXT, "
        ."FACILITY_TYPE TEXT, "
        ."SITE_FREQUENCY TEXT, "
        ."FREQ_ALTITUDE TEXT, "
        ."FREQ_USAGE_NAME TEXT, "
        ."IFR_FACILITY_ID TEXT"
        .")";

my $insert_aff3_record = "INSERT INTO aff3 ("
        ."ARTCC_ID, "
        ."SITE_LOCATION, "
        ."FACILITY_TYPE, "
        ."SITE_FREQUENCY, "
        ."FREQ_ALTITUDE, "
        ."FREQ_USAGE_NAME, "
        ."IFR_FACILITY_ID"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?"
        .")";

my $create_aff4_table = "CREATE TABLE aff4 ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."ARTCC_ID TEXT, "
        ."SITE_LOCATION TEXT, "
        ."FACILITY_TYPE TEXT, "
        ."REMARK_FREQUENCY TEXT, "
        ."REMARKS_ELEMENT_NO INTEGER, "
        ."REMARK_TEXT TEXT"
        .")";

my $insert_aff4_record = "INSERT INTO aff4 ("
        ."ARTCC_ID, "
        ."SITE_LOCATION, "
        ."FACILITY_TYPE, "
        ."REMARK_FREQUENCY, "
        ."REMARKS_ELEMENT_NO, "
        ."REMARK_TEXT"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?"
        .")";

my $create_wxl_table = "CREATE TABLE wxl ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."LOCATION_ID TEXT, "
        ."LOC_LATITUDE_DEGREES REAL, "
        ."LOC_LONGITUDE_DEGREES REAL, "
        ."ASSOC_CITY TEXT, "
        ."ASSOC_STATE TEXT, "
        ."LOC_ELEVATION_FEET INTEGER, "
        ."LOC_ELEVATION_ACCURACY TEXT, "
        ."WX_SERVICE_TYPES TEXT"
        .")";

my $insert_wxl_record = "INSERT INTO wxl ("
        ."LOCATION_ID, "
        ."LOC_LATITUDE_DEGREES, "
        ."LOC_LONGITUDE_DEGREES, "
        ."ASSOC_CITY, "
        ."ASSOC_STATE, "
        ."LOC_ELEVATION_FEET, "
        ."LOC_ELEVATION_ACCURACY, "
        ."WX_SERVICE_TYPES"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?"
        .")";

my $create_com_table = "CREATE TABLE com ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."COMM_OUTLET_ID TEXT, "
        ."COMM_OUTLET_TYPE TEXT, "
        ."ASSOC_NAVAID_ID TEXT, "
        ."COMM_OUTLET_LATITUDE_DEGREES REAL, "
        ."COMM_OUTLET_LONGITUDE_DEGREES REAL, "
        ."COMM_OUTLET_CALL TEXT, "
        ."COMM_OUTLET_FREQS TEXT, "
        ."FSS_IDENT TEXT, "
        ."FSS_NAME TEXT"
        .")";

my $insert_com_record = "INSERT INTO com ("
        ."COMM_OUTLET_ID, "
        ."COMM_OUTLET_TYPE, "
        ."ASSOC_NAVAID_ID, "
        ."COMM_OUTLET_LATITUDE_DEGREES, "
        ."COMM_OUTLET_LONGITUDE_DEGREES, "
        ."COMM_OUTLET_CALL, "
        ."COMM_OUTLET_FREQS, "
        ."FSS_IDENT, "
        ."FSS_NAME"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?"
        .")";

$dbh->do( "DROP TABLE IF EXISTS airports" );
$dbh->do( $create_airports_table );
$dbh->do( "CREATE INDEX idx_apt_site_number on airports ( SITE_NUMBER );" );
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

$dbh->do( "DROP TABLE IF EXISTS ars" );
$dbh->do( $create_ars_table );
$dbh->do( "CREATE INDEX idx_ars_site_number on ars ( SITE_NUMBER );" );

$dbh->do( "DROP TABLE IF EXISTS tower1" );
$dbh->do( $create_tower1_table );
$dbh->do( "CREATE INDEX idx_twr1_site_number on tower1 ( SITE_NUMBER );" );

$dbh->do( "DROP TABLE IF EXISTS tower3" );
$dbh->do( $create_tower3_table );
$dbh->do( "CREATE INDEX idx_twr3_facility_id on tower3 ( FACILITY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS tower6" );
$dbh->do( $create_tower6_table );
$dbh->do( "CREATE INDEX idx_twr6_facility_id on tower6 ( FACILITY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS tower7" );
$dbh->do( $create_tower7_table );
$dbh->do( "CREATE INDEX idx_twr7_site_number on tower7 ( SATELLITE_AIRPORT_SITE_NUMBER );" );

$dbh->do( "DROP TABLE IF EXISTS tower8" );
$dbh->do( $create_tower8_table );
$dbh->do( "CREATE INDEX idx_twr8_facility_id on tower8 ( FACILITY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS tower9" );
$dbh->do( $create_tower9_table );
$dbh->do( "CREATE INDEX idx_twr9_facility_id on tower9 ( FACILITY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS awos1" );
$dbh->do( $create_awos1_table );
$dbh->do( "CREATE INDEX idx_awos1_sensor_ident on awos1 ( WX_SENSOR_IDENT );" );

$dbh->do( "DROP TABLE IF EXISTS awos2" );
$dbh->do( $create_awos2_table );
$dbh->do( "CREATE INDEX idx_awos2_sensor_ident on awos2 ( WX_SENSOR_IDENT );" );

$dbh->do( "DROP TABLE IF EXISTS nav1" );
$dbh->do( $create_nav1_table );
$dbh->do( "CREATE INDEX idx_nav1_navaid_id on nav1 ( NAVAID_ID );" );

$dbh->do( "DROP TABLE IF EXISTS nav2" );
$dbh->do( $create_nav2_table );
$dbh->do( "CREATE INDEX idx_nav2_navaid_id on nav2 ( NAVAID_ID );" );

$dbh->do( "DROP TABLE IF EXISTS ils1" );
$dbh->do( $create_ils1_table );
$dbh->do( "CREATE INDEX idx_ils1_runway_id on ils1 ( SITE_NUMBER, RUNWAY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS ils2" );
$dbh->do( $create_ils2_table );
$dbh->do( "CREATE INDEX idx_ils2_runway_id on ils2 ( SITE_NUMBER, RUNWAY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS ils3" );
$dbh->do( $create_ils3_table );
$dbh->do( "CREATE INDEX idx_ils3_runway_id on ils3 ( SITE_NUMBER, RUNWAY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS ils4" );
$dbh->do( $create_ils4_table );
$dbh->do( "CREATE INDEX idx_ils4_runway_id on ils4 ( SITE_NUMBER, RUNWAY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS ils5" );
$dbh->do( $create_ils5_table );
$dbh->do( "CREATE INDEX idx_ils5_runway_id on ils5 ( SITE_NUMBER, RUNWAY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS ils6" );
$dbh->do( $create_ils6_table );
$dbh->do( "CREATE INDEX idx_ils6_runway_id on ils2 ( SITE_NUMBER, RUNWAY_ID );" );

$dbh->do( "DROP TABLE IF EXISTS aff1" );
$dbh->do( $create_aff1_table );
$dbh->do( "CREATE INDEX idx_aff1_artcc_id on aff1 ( ARTCC_ID );" );

$dbh->do( "DROP TABLE IF EXISTS aff2" );
$dbh->do( $create_aff2_table );
$dbh->do( "CREATE INDEX idx_aff2_artcc_id on aff2 ( ARTCC_ID );" );

$dbh->do( "DROP TABLE IF EXISTS aff3" );
$dbh->do( $create_aff3_table );
$dbh->do( "CREATE INDEX idx_aff3_artcc_id on aff3 ( ARTCC_ID );" );

$dbh->do( "DROP TABLE IF EXISTS aff4" );
$dbh->do( $create_aff4_table );
$dbh->do( "CREATE INDEX idx_aff4_artcc_id on aff4 ( ARTCC_ID );" );

$dbh->do( "DROP TABLE IF EXISTS wxl" );
$dbh->do( $create_wxl_table );
$dbh->do( "CREATE INDEX idx_wxl_location_id on wxl ( LOCATION_ID );" );

$dbh->do( "DROP TABLE IF EXISTS com" );
$dbh->do( $create_com_table );
$dbh->do( "CREATE INDEX idx_com_navaid_id on com ( ASSOC_NAVAID_ID );" );

my $sth_apt = $dbh->prepare( $insert_airports_record );
my $sth_rwy = $dbh->prepare( $insert_runways_record );
my $sth_att = $dbh->prepare( $insert_attendance_record );
my $sth_rmk = $dbh->prepare( $insert_remarks_record );
my $sth_ars = $dbh->prepare( $insert_ars_record );
my $sth_twr1 = $dbh->prepare( $insert_tower1_record );
my $sth_twr3 = $dbh->prepare( $insert_tower3_record );
my $sth_twr6 = $dbh->prepare( $insert_tower6_record );
my $sth_twr7 = $dbh->prepare( $insert_tower7_record );
my $sth_twr8 = $dbh->prepare( $insert_tower8_record );
my $sth_twr9 = $dbh->prepare( $insert_tower9_record );
my $sth_awos1 = $dbh->prepare( $insert_awos1_record );
my $sth_awos2 = $dbh->prepare( $insert_awos2_record );
my $sth_nav1 = $dbh->prepare( $insert_nav1_record );
my $sth_nav2 = $dbh->prepare( $insert_nav2_record );
my $sth_ils1 = $dbh->prepare( $insert_ils1_record );
my $sth_ils2 = $dbh->prepare( $insert_ils2_record );
my $sth_ils3 = $dbh->prepare( $insert_ils3_record );
my $sth_ils4 = $dbh->prepare( $insert_ils4_record );
my $sth_ils5 = $dbh->prepare( $insert_ils5_record );
my $sth_ils6 = $dbh->prepare( $insert_ils6_record );
my $sth_aff1 = $dbh->prepare( $insert_aff1_record );
my $sth_aff2 = $dbh->prepare( $insert_aff2_record );
my $sth_aff3 = $dbh->prepare( $insert_aff3_record );
my $sth_aff4 = $dbh->prepare( $insert_aff4_record );
my $sth_wxl = $dbh->prepare( $insert_wxl_record );
my $sth_com = $dbh->prepare( $insert_com_record );

my $i = 0;

my $ofh = select STDOUT;
$| = 1;
select $ofh;

###########################################################################

my $APT = $FADDS_BASE."/APT.txt";
print( "$APT\n" );
open( APT_FILE, "<$APT" ) or die "Could not open data file\n";

while ( my $line = <APT_FILE> )
{
    ++$i;

    if ( ($i % 1000) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    my $type = substrim( $line, 0, 3 );

    if ( $type eq "APT" )
    {
        my $own = substrim( $line,  183,  2 );
        my $use = substrim( $line,  185,  2 );

        # Skip the private use airports
        next if ( $own eq "PR" && $use eq "PR" );
        next if ( $own eq "PU" && $use eq "PR" );

        # Store the site number for later use
        $site_number{ substrim( $line, 3, 11 ) } = 1;

        #SITE_NUMBER
        $sth_apt->bind_param(  1, substrim( $line,    3, 11 ) );
        #FAA_CODE
        $sth_apt->bind_param(  2, substrim( $line,   27,  4 ) );
        #EFFECTIVE_DATE
        $sth_apt->bind_param(  3, substrim( $line,   31, 10 ) );
        #REGION_CODE
        $sth_apt->bind_param(  4, substrim( $line,   41,  3 ) );
        #DISTRICT_CODE
        $sth_apt->bind_param(  5, substrim( $line,   44,  4 ) );
        #ASSOC_STATE
        $sth_apt->bind_param(  6, substrim( $line,   48,  2 ) );
        #ASSOC_COUNTY
        $sth_apt->bind_param(  7, capitalize( $line, 70, 21 ) );
        #ASSOC_CITY
        $sth_apt->bind_param(  8, capitalize( $line, 93, 40 ) );
        #FACILITY_NAME
        $sth_apt->bind_param(  9, capitalize( $line, 133, 42 ) );
        #OWNERSHIP_TYPE
        $sth_apt->bind_param( 10, substrim( $line,  183,  2 ) );
        #FACILITY_USE
        $sth_apt->bind_param( 11, substrim( $line,  185,  2 ) );
        #OWNER_NAME
        $sth_apt->bind_param( 12, capitalize( $line, 187, 35 ) );
        #OWNER_ADDRESS
        $sth_apt->bind_param( 13, capitalize( $line, 222, 72 ) );
        #OWNER_CITY_STATE_ZIP
        $sth_apt->bind_param( 14, capitalize( $line, 294, 45 ) );
        #OWNER_PHONE
        $sth_apt->bind_param( 15, substrim( $line,  339, 16 ) );
        #MANAGER_NAME
        $sth_apt->bind_param( 16, capitalize( $line, 355, 35 ) );
        #MANAGER_ADDRESS
        $sth_apt->bind_param( 17, capitalize( $line, 390, 72 ) );
        #MANAGER_CITY_STATE_ZIP
        $sth_apt->bind_param( 18, capitalize( $line, 462, 45 ) );
        #MANAGER_PHONE
        $sth_apt->bind_param( 19, substrim( $line,  507, 16 ) );
        #REF_LATTITUDE_DEGREES
        my $lattitude = substrim( $line,  538, 11 )/3600.0;
        if ( substr( $line, 549, 1 ) eq "S" )
        {
            $lattitude *= -1;
        }
        $sth_apt->bind_param( 20, $lattitude );
        #REF_LONGITUDE_DEGREES
        my $longitude = substrim( $line,  565, 11 )/3600.0;
        if ( substr( $line, 576, 1 ) eq "W" )
        {
            $longitude *= -1;
        }
        $sth_apt->bind_param( 21, $longitude );
        #REF_METHOD
        $sth_apt->bind_param( 22, substrim( $line,  577,  1 ) );
        #ELEVATION_MSL
        $sth_apt->bind_param( 23, substrim( $line,  578,  5 ) );
        #ELEVATION_METHOD
        $sth_apt->bind_param( 24, substrim( $line,  585,  1 ) );
        #MAGNETIC_VARIATION_DEGREES
        $sth_apt->bind_param( 25, substrim( $line,  586,  2 ) );
        #MAGNETIC_VARIATION_DIRECTION
        $sth_apt->bind_param( 26, substrim( $line,  588,  1 ) );
        #MAGNETIC_VARIATION_YEAR
        $sth_apt->bind_param( 27, substrim( $line,  589,  4 ) );
        #PATTERN_ALTITUDE_AGL
        $sth_apt->bind_param( 28, substrim( $line,  593,  4 ) );
        #SECTIONAL_CHART
        $sth_apt->bind_param( 29, capitalize( $line,  597, 30 ) );
        #DISTANCE_FROM_CITY_NM
        $sth_apt->bind_param( 30, substrim( $line,  627,  2 ) );
        #DIRECTION_FROM_CITY
        $sth_apt->bind_param( 31, substrim( $line,  629,  3 ) );
        #BOUNDARY_ARTCC_ID
        $sth_apt->bind_param( 32, substrim( $line,  637,  4 ) );
        #BOUNDARY_ARTCC_NAME
        $sth_apt->bind_param( 33, capitalize( $line,  644, 30 ) );
        #FSS_ON_SITE
        $sth_apt->bind_param( 34, substrim( $line,  711,  1 ) );
        #FSS_ID
        $sth_apt->bind_param( 35, substrim( $line,  712,  4 ) );
        #FSS_NAME
        $sth_apt->bind_param( 36, capitalize( $line,  716, 30 ) );
        #FSS_LOCAL_PHONE
        $sth_apt->bind_param( 37, substrim( $line,  746, 16 ) );
        #FSS_TOLLFREE_PHONE
        $sth_apt->bind_param( 38, substrim( $line,  762, 16 ) );
        #NOTAM_FACILITY_ID
        $sth_apt->bind_param( 39, substrim( $line,  828,  4 ) );
        #NOTAM_D_AVAILABLE
        $sth_apt->bind_param( 40, substrim( $line,  832,  1 ) );
        #ACTIVATION_DATE
        $sth_apt->bind_param( 41, substrim( $line,  833,  7 ) );
        #STATUS_CODE
        $sth_apt->bind_param( 42, substrim( $line,  840,  2 ) );
        #INTL_ENTRY_AIRPORT
        $sth_apt->bind_param( 43, substrim( $line,  877,  1 ) );
        #CUSTOMS_LANDING_RIGHTS_AIRPORT
        $sth_apt->bind_param( 44, substrim( $line,  878,  1 ) );
        #CIVIL_MILITARY_JOINT_USE
        $sth_apt->bind_param( 45, substrim( $line,  879,  1 ) );
        #MILITARY_LANDING_RIGHTS
        $sth_apt->bind_param( 46, substrim( $line,  880,  1 ) );
        #FUEL_TYPES
        $sth_apt->bind_param( 47, substrim( $line,  900, 40 ) );
        #AIRFRAME_REPAIR_SERVICE
        $sth_apt->bind_param( 48, capitalize( $line,  940,  5 ) );
        #POWER_PLANT_REPAIR_SERVICE
        $sth_apt->bind_param( 49, capitalize( $line,  945,  5 ) );
        #BOTTLED_O2_AVAILABLE
        $sth_apt->bind_param( 50, substrim( $line,  950,  8 ) );
        #BULK_O2_AVAILABLE
        $sth_apt->bind_param( 51, substrim( $line,  958,  8 ) );
        #LIGHTING_SCHEDULE
        $sth_apt->bind_param( 52, substrim( $line,  966,  7 ) );
        #BEACON_LIGHTING_SCHEDULE
        $sth_apt->bind_param( 53, substrim( $line,  973,  7 ) );
        #TOWER_ON_SITE
        $sth_apt->bind_param( 54, substrim( $line,  980,  1 ) );
        #UNICOM_FREQS
        $sth_apt->bind_param( 55, substrim( $line,  981, 7 ) );
        #CTAF_FREQ
        $sth_apt->bind_param( 56, substrim( $line, 988,  7 ) );
        #SEGMENTED_CIRCLE
        $sth_apt->bind_param( 57, substrim( $line, 995,  4 ) );
        #BEACON_COLOR
        $sth_apt->bind_param( 58, substrim( $line, 999,  3 ) );
        #LANDING_FEE
        $sth_apt->bind_param( 59, substrim( $line, 1002,  1 ) );
        #MEDICAL_USE
        $sth_apt->bind_param( 60, substrim( $line, 1003,  1 ) );
        #SINGLE_ENGINE_COUNT
        $sth_apt->bind_param( 61, substrim( $line, 1004,  3 ) );
        #MULTI_ENGINE_COUNT
        $sth_apt->bind_param( 62, substrim( $line, 1007,  3 ) );
        #JET_ENGINE_COUNT
        $sth_apt->bind_param( 63, substrim( $line, 1010,  3 ) );
        #HELI_COUNT
        $sth_apt->bind_param( 64, substrim( $line, 1013,  3 ) );
        #GLIDERS_COUNT
        $sth_apt->bind_param( 65, substrim( $line, 1016,  3 ) );
        #MILITARY_COUNT
        $sth_apt->bind_param( 66, substrim( $line, 1019,  3 ) );
        #ULTRA_LIGHT_COUNT
        $sth_apt->bind_param( 67, substrim( $line, 1022,  3 ) );
        #ANNUAL_COMMERCIAL_OPS
        $sth_apt->bind_param( 68, substrim( $line, 1025,  6 ) );
        #ANNUAL_COMMUTER_OPS
        $sth_apt->bind_param( 69, substrim( $line, 1031,  6 ) );
        #ANNUAL_AIRTAXI_OPS
        $sth_apt->bind_param( 70, substrim( $line, 1037,  6 ) );
        #ANNUAL_GA_LOCAL_OPS
        $sth_apt->bind_param( 71, substrim( $line, 1043,  6 ) );
        #ANNUAL_GA_ININERANT_OPS
        $sth_apt->bind_param( 72, substrim( $line, 1049,  6 ) );
        #ANNUAL_MILITARY_OPS
        $sth_apt->bind_param( 73, substrim( $line, 1055,  6 ) );
        #OPS_REFERENCE_DATE
        $sth_apt->bind_param( 74, substrim( $line, 1061, 10 ) );
        #CONTRACT_FUEL_AVAILABLE
        $sth_apt->bind_param( 75, substrim( $line, 1123,  1 ) );
        #STORAGE_FACILITY
        $sth_apt->bind_param( 76, substrim( $line, 1124, 12 ) );
        #OTHER_SERVICES
        $sth_apt->bind_param( 77, substrim( $line, 1136, 71 ) );
        #WIND_INDICATOR
        $sth_apt->bind_param( 78, substrim( $line, 1207,  3 ) );
        #ICAO_CODE
        $sth_apt->bind_param( 79, substrim( $line, 1210,  7 ) );
        #TIMEZONE_ID
        $sth_apt->bind_param( 80, "" );

        $sth_apt->execute();
    }
    elsif ( $type eq "RWY" )
    {
        next if ( !exists( $site_number{ substrim( $line, 3, 11 ) } ) );

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
        #BASE_END_RUNWAY_ELEVATION
        $sth_rwy->bind_param( 14, substrim( $line,  142,  7 ) );
        #BASE_END_THRESHOLD_CROSSING_HEIGHT
        $sth_rwy->bind_param( 15, substrim( $line,  149,  3 ) );
        #BASE_END_GLIDE_ANGLE
        $sth_rwy->bind_param( 16, substrim( $line,  152,  4 ) );
        #BASE_END_DISPLACED_THRESHOLD_LENGTH
        $sth_rwy->bind_param( 17, substrim( $line,  217,  4 ) );
        #BASE_END_TDZ_ELEVATION
        $sth_rwy->bind_param( 18, substrim( $line,  221,  7 ) );
        #BASE_END_VISUAL_GLIDE_SLOPE
        $sth_rwy->bind_param( 19, substrim( $line,  228,  5 ) );
        #BASE_END_RVR_LOCATIONS
        $sth_rwy->bind_param( 20, substrim( $line,  233,  3 ) );
        #BASE_END_APCH_LIGHT_SYSTEM
        $sth_rwy->bind_param( 21, substrim( $line,  237,  8 ) );
        #BASE_END_REIL_AVAILABLE
        $sth_rwy->bind_param( 22, substrim( $line,  245,  1 ) );
        #BASE_END_CENTERLINE_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 23, substrim( $line,  246,  1 ) );
        #BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 24, substrim( $line,  247,  1 ) );
        #BASE_END_CONTROLLING_OBJECT
        $sth_rwy->bind_param( 25, substrim( $line,  248, 11 ) );
        #BASE_END_CONTROLLING_OBJECT_LIGHTED
        $sth_rwy->bind_param( 26, substrim( $line,  259,  4 ) );
        #BASE_END_CONTROLLING_OBJECT_SLOPE
        $sth_rwy->bind_param( 27, substrim( $line,  268,  2 ) );
        #BASE_END_CONTROLLING_OBJECT_HEIGHT
        $sth_rwy->bind_param( 28, substrim( $line,  270,  5 ) );
        #BASE_END_CONTROLLING_OBJECT_DISTANCE
        $sth_rwy->bind_param( 29, substrim( $line,  275,  5 ) );
        #BASE_END_CONTROLLING_OBJECT_OFFSET
        $sth_rwy->bind_param( 30, substrim( $line,  280,  7 ) );
        #RECIPROCAL_END_ID
        $sth_rwy->bind_param( 31, substrim( $line,  287,  3 ) );
        #RECIPROCAL_END_HEADING
        $sth_rwy->bind_param( 32, substrim( $line,  290,  3 ) );
        #RECIPROCAL_END_ILS_TYPE
        $sth_rwy->bind_param( 33, substrim( $line,  293, 10 ) );
        #RECIPROCAL_END_RIGHT_TRAFFIC
        $sth_rwy->bind_param( 34, substrim( $line,  303,  1 ) );
        #RECIPROCAL_END_MARKING_TYPE
        $sth_rwy->bind_param( 35, substrim( $line,  304,  5 ) );
        #RECIPROCAL_END_MARKING_CONDITION
        $sth_rwy->bind_param( 36, substrim( $line,  309,  1 ) );
        #RECIPROCAL_END_RUNWAY_ELEVATION
        $sth_rwy->bind_param( 37, substrim( $line,  364,  7 ) );
        #RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT
        $sth_rwy->bind_param( 38, substrim( $line,  371,  3 ) );
        #RECIPROCAL_END_GLIDE_ANGLE
        $sth_rwy->bind_param( 39, substrim( $line,  374,  4 ) );
        #RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH
        $sth_rwy->bind_param( 40, substrim( $line,  439,  4 ) );
        #RECIPROCAL_END_TDZ_ELEVATION
        $sth_rwy->bind_param( 41, substrim( $line,  443,  7 ) );
        #RECIPROCAL_END_VISUAL_GLIDE_SLOPE
        $sth_rwy->bind_param( 42, substrim( $line,  450,  5 ) );
        #RECIPROCAL_END_RVR_LOCATIONS
        $sth_rwy->bind_param( 43, substrim( $line,  455,  3 ) );
        #RECIPROCAL_END_APCH_LIGHT_SYSTEM
        $sth_rwy->bind_param( 44, substrim( $line,  459,  8 ) );
        #RECIPROCAL_END_REIL_AVAILABLE
        $sth_rwy->bind_param( 45, substrim( $line,  467,  1 ) );
        #RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 46, substrim( $line,  468,  1 ) );
        #RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE
        $sth_rwy->bind_param( 47, substrim( $line,  469,  1 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT
        $sth_rwy->bind_param( 48, substrim( $line,  470, 11 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED
        $sth_rwy->bind_param( 49, substrim( $line,  481,  4 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE
        $sth_rwy->bind_param( 50, substrim( $line,  490,  2 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT
        $sth_rwy->bind_param( 51, substrim( $line,  492,  5 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE
        $sth_rwy->bind_param( 52, substrim( $line,  497,  5 ) );
        #RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET
        $sth_rwy->bind_param( 53, substrim( $line,  502,  7 ) );
        #BASE_END_GRADIENT
        $sth_rwy->bind_param( 54, substrim( $line,  559,  5 ) );
        #BASE_END_GRADIENT_DIRECTION
        $sth_rwy->bind_param( 55, substrim( $line,  564,  4 ) );
        #BASE_END_TORA
        $sth_rwy->bind_param( 56, substrim( $line,  698,  5 ) );
        #BASE_END_TODA
        $sth_rwy->bind_param( 57, substrim( $line,  703,  5 ) );
        #BASE_END_ASDA
        $sth_rwy->bind_param( 58, substrim( $line,  708,  5 ) );
        #BASE_END_LDA
        $sth_rwy->bind_param( 59, substrim( $line,  713,  5 ) );
        #BASE_END_LAHSO_DISTANCE
        $sth_rwy->bind_param( 60, substrim( $line,  718,  5 ) );
        #BASE_END_LAHSO_RUNWAY
        $sth_rwy->bind_param( 61, substrim( $line,  723,  7 ) );
        #RECIPROCAL_END_GRADIENT
        $sth_rwy->bind_param( 62, substrim( $line,  850,  5  ) );
        #RECIPROCAL_END_GRADIENT_DIRECTION
        $sth_rwy->bind_param( 63, substrim( $line,  855,  4  ) );
        #RECIPROCAL_END_TORA
        $sth_rwy->bind_param( 64, substrim( $line,  989,  5  ) );
        #RECIPROCAL_END_TODA
        $sth_rwy->bind_param( 65, substrim( $line,  994,  5  ) );
        #RECIPROCAL_END_ASDA
        $sth_rwy->bind_param( 66, substrim( $line,  999,  5  ) );
        #RECIPROCAL_END_LDA
        $sth_rwy->bind_param( 67, substrim( $line, 1004,  5  ) );
        #RECIPROCAL_END_LAHSO_DISTANCE
        $sth_rwy->bind_param( 68, substrim( $line, 1009,  5  ) );
        #RECIPROCAL_END_LAHSO_RUNWAY
        $sth_rwy->bind_param( 69, substrim( $line, 1014,  7  ) );
 
        $sth_rwy->execute();
    }
    elsif ( $type eq "ATT" )
    {
        next if ( !exists( $site_number{ substrim( $line, 3, 11 ) } ) );

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
        next if ( !exists( $site_number{ substrim( $line, 3, 11 ) } ) );

        #SITE_NUMBER
        $sth_rmk->bind_param( 1, substrim( $line,  3,   11 ) );
        #REMARK_NAME
        $sth_rmk->bind_param( 2, substrim( $line, 16,   13 ) );
        #REMARK_TEXT
        $sth_rmk->bind_param( 3, substrim( $line, 29, 1500 ) );
 
        $sth_rmk->execute();
    }
    elsif ( $type eq "ARS" )
    {
        next if ( !exists( $site_number{ substrim( $line, 3, 11 ) } ) );

        #SITE_NUMBER
        $sth_ars->bind_param( 1, substrim( $line,  3,  11 ) );
        #RUNWAY_ID
        $sth_ars->bind_param( 2, substrim( $line, 16,   7 ) );
        #RUNWAY_END_ID
        $sth_ars->bind_param( 3, substrim( $line, 23,   3 ) );
        #ARRESTING_DEVICE
        $sth_ars->bind_param( 4, substrim( $line, 26,   9 ) );
 
        $sth_ars->execute();
    }

    if ( ($i % 1000) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

# We need to do this to fallback to faa code if icao code is not assigned
$dbh->do( "update airports set icao_code=null where length(icao_code)=0;" );

print( "\rFinished processing $i records.\n" );
close APT_FILE;

###########################################################################

my $TWR = $FADDS_BASE."/TWR.txt";
open( TWR_FILE, "<$TWR" ) or die "Could not open data file\n";

$i = 0;
print( "$TWR\n" );
while ( my $line = <TWR_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    my $type = substrim( $line, 0, 4 );

    if ( $type eq "TWR1" )
    {
        #FACILITY_ID
        $sth_twr1->bind_param( 1, substrim( $line,   4,  4 ) );
        #SITE_NUMBER
        $sth_twr1->bind_param( 2, substrim( $line,  18, 11 ) );
        #FACILITY_TYPE
        $sth_twr1->bind_param( 3, substrim( $line, 238, 12 ) );
        #RADIO_CALL_TOWER
        $sth_twr1->bind_param( 4, capitalize( $line, 804, 26 ) );
        #RADIO_CALL_APCH
        $sth_twr1->bind_param( 5, capitalize( $line, 856, 26 ) );
        #RADIO_CALL_DEP
        $sth_twr1->bind_param( 6, capitalize( $line, 908, 26 ) );
 
        $sth_twr1->execute();
    }
    elsif ( $type eq "TWR3" )
    {
        my $facility_id = substrim( $line, 4, 4 );
        my $j = 0;
        while ( $j < 9 )
        {
            my $freq = substrim( $line, 854+$j*60, 60 );
            my $freq_use = substrim( $line, 52+$j*94, 50 );
            if ( $freq eq "" )
            {
                last;
            }
            #FACILITY_ID
            $sth_twr3->bind_param( 1, $facility_id );
            #MASTER_AIRPORT_FREQ
            $sth_twr3->bind_param( 2, $freq );
            #MASTER_AIRPORT_FREQ_USE
            $sth_twr3->bind_param( 3, $freq_use );
 
            $sth_twr3->execute();
            ++$j;
        }
    }
    elsif ( $type eq "TWR6" )
    {
        #FACILITY_ID
        $sth_twr6->bind_param( 1, substrim( $line,  4,   4 ) );
        #ELEMENT_NUMBER
        $sth_twr6->bind_param( 2, substrim( $line,  8,   5 ) );
        #REMARK_TEXT
        $sth_twr6->bind_param( 3, substrim( $line, 13, 800 ) );
 
        $sth_twr6->execute();
    }
    elsif ( $type eq "TWR7" )
    {
        #FACILITY_ID
        $sth_twr7->bind_param( 1, substrim( $line,   4,  4 ) );
        #SATELLITE_AIRPORT_FREQ_USE
        $sth_twr7->bind_param( 2, substrim( $line,  52, 50 ) );
        #SATELLITE_AIRPORT_SITE_NUMBER
        $sth_twr7->bind_param( 3, substrim( $line, 102, 11 ) );
        #MASTER_AIRPORT_SITE_NUMBER
        $sth_twr7->bind_param( 4, substrim( $line, 326, 11 ) );
        #SATELLITE_AIRPORT_FREQ_FULL
        $sth_twr7->bind_param( 5, substrim( $line, 462, 60 ) );
 
        $sth_twr7->execute();
    }
    elsif ( $type eq "TWR8" )
    {
        #FACILITY_ID
        substr( $line, 8, 4 ) =~ s/ /N/g;
        $sth_twr8->bind_param( 1, substrim( $line,  4,   4 ) );
        #AIRSPACE_TYPES
        $sth_twr8->bind_param( 2, substrim( $line,  8,   4 ) );
        #AIRSPACE_HOURS
        $sth_twr8->bind_param( 3, substrim( $line, 12, 300 ) );
 
        $sth_twr8->execute();
    }
    elsif ( $type eq "TWR9" )
    {
        #FACILITY_ID
        $sth_twr9->bind_param( 1, substrim( $line,   4,   4 ) );
        #ATIS_SERIAL_NO
        $sth_twr9->bind_param( 2, substrim( $line,   8,   4 ) );
        #ATIS_HOURS
        $sth_twr9->bind_param( 3, substrim( $line,  12, 200 ) );
        #ATIS_PURPOSE
        $sth_twr9->bind_param( 4, substrim( $line, 212, 100 ) );
        #ATIS_PHONE
        $sth_twr9->bind_param( 5, substrim( $line, 312,  18 ) );
 
        $sth_twr9->execute();
    }

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( "\rFinished processing $i records.\n" );
close TWR_FILE;

###########################################################################

my $AWOS = $FADDS_BASE."/AWOS.txt";
open( AWOS_FILE, "<$AWOS" ) or die "Could not open data file\n";

$i = 0;
print( "$AWOS\n" );
while ( my $line = <AWOS_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    my $type = substrim( $line, 0, 5 );

    if ( $type eq "AWOS1" )
    {
        #WX_SENSOR_IDENT
        $sth_awos1->bind_param(  1, substrim( $line,   5,  4 ) );
        #WX_SENSOR_TYPE
        $sth_awos1->bind_param(  2, substrim( $line,   9, 10 ) );
        #COMMISSIONING_STATUS
        $sth_awos1->bind_param(  3, substrim( $line,  19,  1 ) );
        #COMMISSIONING_DATE
        $sth_awos1->bind_param(  4, substrim( $line,  20, 10 ) );
        #NAVAID_FLAG
        $sth_awos1->bind_param(  5, substrim( $line,  30,  1 ) );
        #STATION_LATTITUDE_DEGREES
        my $lat_dms = substrim( $line, 31, 14 );
        $sth_awos1->bind_param(  6, geo_parse_lat_dms( $lat_dms ) );
        #STATION_LONGITUDE_DEGREES
        my $lon_dms = substrim( $line, 45, 15 );
        $sth_awos1->bind_param(  7, geo_parse_lon_dms( $lon_dms ) );
        #STATION_ELEVATION
        $sth_awos1->bind_param(  8, substrim( $line,  60,  7 ) );
        #ELEVATION_METHOD
        $sth_awos1->bind_param(  9, substrim( $line,  67,  1 ) );
        #STATION_FREQUENCY
        $sth_awos1->bind_param( 10, substrim( $line,  68,  7 ) );
        #SECOND_STATION_FREQUENCY
        $sth_awos1->bind_param( 11, substrim( $line,  75,  7 ) );
        #STATION_PHONE_NUMBER
        $sth_awos1->bind_param( 12, substrim( $line,  82, 14 ) );
        #SITE_NUMBER
        $sth_awos1->bind_param( 13, substrim( $line, 110, 11 ) );

        $sth_awos1->execute();
    }
    elsif ( $type eq "AWOS2" )
    {
        #WX_SENSOR_IDENT
        $sth_awos2->bind_param( 1, substrim( $line,  5,   4 ) );
        #WX_SENSOR_TYPE
        $sth_awos2->bind_param( 2, substrim( $line,  9,  10 ) );
        #WX_STATION_REMARKS
        $sth_awos2->bind_param( 3, substrim( $line, 19, 236 ) );

        $sth_awos2->execute();
    }

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

$dbh->do( "update awos1 set station_lattitude_degrees=("
        ."select ref_lattitude_degrees from airports where airports.faa_code=awos1.wx_sensor_ident"
        .") where station_lattitude_degrees=0;" );

$dbh->do( "update awos1 set station_longitude_degrees=("
        ."select ref_longitude_degrees from airports where airports.faa_code=awos1.wx_sensor_ident"
        .") where station_longitude_degrees=0;" );

print( "\rFinished processing $i records.\n" );
close AWOS_FILE;

###########################################################################

my $NAV = $FADDS_BASE."/NAV.txt";
open( NAV_FILE, "<$NAV" ) or die "Could not open data file\n";

$i = 0;
print( "$NAV\n" );
while ( my $line = <NAV_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    my $type = substrim( $line, 0, 4 );

    if ( $type eq "NAV1" )
    {
        #NAVAID_ID
        $sth_nav1->bind_param(  1, substrim( $line,   4,  4 ) );
        #NAVAID_TYPE
        $sth_nav1->bind_param(  2, substrim( $line,   8, 20 ) );
        #NAVAID_NAME
        $sth_nav1->bind_param(  3, capitalize( $line,  42, 30 ) );
        #ASSOC_CITY
        $sth_nav1->bind_param(  4, capitalize( $line,  72, 40 ) );
        #ASSOC_STATE
        $sth_nav1->bind_param(  5, substrim( $line, 142,  2 ) );
        #PUBLIC_USE
        $sth_nav1->bind_param(  6, substrim( $line, 280,  1 ) );
        #NAVAID_CLASS
        $sth_nav1->bind_param(  7, substrim( $line, 281, 11 ) );
        #OPERATING_HOURS
        $sth_nav1->bind_param(  8, substrim( $line, 292, 11 ) );
        #REF_LATTITUDE_DEGREES
        my $lattitude = substrim( $line, 385, 10 )/3600.0;
        if ( substrim( $line, 395, 1 ) eq "S" )
        {
            $lattitude *= -1;
        }
        $sth_nav1->bind_param(  9, $lattitude );
        #REF_LONGITUDE_DEGREES
        my $longitude = substrim( $line, 410, 10 )/3600.0;
        if ( substrim( $line, 420, 1 ) eq "W" )
        {
            $longitude *= -1;
        }
        $sth_nav1->bind_param( 10, $longitude );
        #ELEVATION_MSL
        $sth_nav1->bind_param( 11, substrim( $line, 472,  7 ) );
        #MAGNETIC_VARIATION_DEGREES
        $sth_nav1->bind_param( 12, substrim( $line, 479,  4 ) );
        #MAGNETIC_VARIATION_DIRECTION
        $sth_nav1->bind_param( 13, substrim( $line, 483,  1 ) );
        #MAGNETIC_VARIATION_YEAR
        $sth_nav1->bind_param( 14, substrim( $line, 484,  4 ) );
        #VOICE_FEATURE
        $sth_nav1->bind_param( 15, substrim( $line, 488,  3 ) );
        #POWER_OUTPUT
        $sth_nav1->bind_param( 16, substrim( $line, 491,  4 ) );
        #AUTOMATIC_VOICE_IDENT
        $sth_nav1->bind_param( 17, substrim( $line, 495,  3 ) );
        #RADIO_VOICE_CALL
        $sth_nav1->bind_param( 18, capitalize( $line, 499, 30 ) );
        #TACAN_CHANNEL
        $sth_nav1->bind_param( 19, substrim( $line, 529,  4 ) );
        #NAVAID_FREQUENCY
        $sth_nav1->bind_param( 20, substrim( $line, 533,  6 ) );
        #FANMARKER_TYPE
        $sth_nav1->bind_param( 21, capitalize( $line, 563, 10 ) );
        #PROTECTED_FREQUENCY_ALTITUDE
        $sth_nav1->bind_param( 22, substrim( $line, 576,  1 ) );

        $sth_nav1->execute();
    }
    elsif ( $type eq "NAV2" )
    {
        #NAVAID_ID
        $sth_nav2->bind_param( 1, substrim( $line,  4,   4 ) );
        #NAVAID_TYPE
        $sth_nav2->bind_param( 2, substrim( $line,  8,  20 ) );
        #NAVAID_REMARK_TEXT
        $sth_nav2->bind_param( 3, substrim( $line, 28, 600 ) );

        $sth_nav2->execute();
    }

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( "\rFinished processing $i records.\n" );
close NAVAID_FILE;

###########################################################################

my $ILS = $FADDS_BASE."/ILS.txt";
open( ILS_FILE, "<$ILS" ) or die "Could not open data file\n";

$i = 0;
print( "$ILS\n" );
while ( my $line = <ILS_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    my $type = substrim( $line, 0, 4 );

    if ( $type eq "ILS1" )
    {
        #SITE_NUMBER
        $sth_ils1->bind_param(  1, substrim( $line,   4, 11 ) );
        #RUNWAY_ID
        $sth_ils1->bind_param(  2, substrim( $line,  15,  3 ) );
        #ILS_TYPE
        $sth_ils1->bind_param(  3, substrim( $line,  18, 10 ) );
        #ILS_ID TEXT
        $sth_ils1->bind_param(  4, substrim( $line,  28,  6 ) );
        #ILS_CATEGORY
        $sth_ils1->bind_param(  5, substrim( $line, 172,  9 ) );
        #ILS_MAGNETIC_BEARING
        $sth_ils1->bind_param(  6, substrim( $line, 281,  6 ) );

        $sth_ils1->execute();
    }
    elsif ( $type eq "ILS2" )
    {
        #SITE_NUMBER
        $sth_ils2->bind_param( 1, substrim( $line,   4, 11 ) );
        #RUNWAY_ID
        $sth_ils2->bind_param( 2, substrim( $line,  15,  3 ) );
        #ILS_TYPE
        $sth_ils2->bind_param( 3, substrim( $line,  18, 10 ) );
        #OPERATIONAL_STATUS
        $sth_ils2->bind_param( 4, capitalize( $line,  28, 22 ) );
        #OPERATIONAL_EFFECTIVE_DATE
        $sth_ils2->bind_param( 5, substrim( $line,  50, 10 ) );
        #LOCALIZER_FREQUENCY
        $sth_ils2->bind_param( 6, substrim( $line, 133,  7 ) );
        #LOCALIZER_BACK_COURSE_STATUS
        $sth_ils2->bind_param( 7, capitalize( $line, 140, 15 ) );
        #LOCALIZER_COURSE_WIDTH
        $sth_ils2->bind_param( 8, substrim( $line, 155,  5 ) );
        #LOCALIZER_COURSE_WIDTH_THRESHOLD
        $sth_ils2->bind_param( 9, substrim( $line, 160,  7 ) );

        $sth_ils2->execute();
    }
    elsif ( $type eq "ILS3" )
    {
        #SITE_NUMBER
        $sth_ils3->bind_param( 1, substrim( $line,   4, 11 ) );
        #RUNWAY_ID
        $sth_ils3->bind_param( 2, substrim( $line,  15,  3 ) );
        #ILS_TYPE
        $sth_ils3->bind_param( 3, substrim( $line,  18, 10 ) );
        #OPERATIONAL_STATUS
        $sth_ils3->bind_param( 4, capitalize( $line,  28, 22 ) );
        #OPERATIONAL_EFFECTIVE_DATE
        $sth_ils3->bind_param( 5, substrim( $line,  50, 10 ) );
        #GLIDE_SLOPE_TYPE
        $sth_ils3->bind_param( 6, substrim( $line, 133,  15 ) );
        #GLIDE_SLOPE_ANGLE
        $sth_ils3->bind_param( 7, substrim( $line, 148,   5 ) );
        #GLIDE_SLOPE_FREQUENCY
        $sth_ils3->bind_param( 8, substrim( $line, 153,   7 ) );

        $sth_ils3->execute();
    }
    elsif ( $type eq "ILS4" )
    {
        #SITE_NUMBER
        $sth_ils4->bind_param( 1, substrim( $line,   4, 11 ) );
        #RUNWAY_ID
        $sth_ils4->bind_param( 2, substrim( $line,  15,  3 ) );
        #ILS_TYPE
        $sth_ils4->bind_param( 3, substrim( $line,  18, 10 ) );
        #OPERATIONAL_STATUS
        $sth_ils4->bind_param( 4, capitalize( $line,  28, 22 ) );
        #OPERATIONAL_EFFECTIVE_DATE
        $sth_ils4->bind_param( 5, substrim( $line,  50, 10 ) );
        #DME_CHANNEL
        $sth_ils4->bind_param( 6, substrim( $line, 133,  4 ) );

        $sth_ils4->execute();
    }
    elsif ( $type eq "ILS5" )
    {
        #SITE_NUMBER
        $sth_ils5->bind_param(  1, substrim( $line,   4, 11 ) );
        #RUNWAY_ID
        $sth_ils5->bind_param(  2, substrim( $line,  15,  3 ) );
        #ILS_TYPE
        $sth_ils5->bind_param(  3, substrim( $line,  18, 10 ) );
        #MARKER_TYPE
        $sth_ils5->bind_param(  4, substrim( $line,  28,  2 ) );
        #OPERATIONAL_STATUS
        $sth_ils5->bind_param(  5, capitalize( $line, 30, 22 ) );
        #OPERATIONAL_EFFECTIVE_DATE
        $sth_ils5->bind_param(  6, substrim( $line,  52, 10 ) );
        #MARKER_DISTANCE
        $sth_ils5->bind_param(  7, substrim( $line, 114,  7 ) );
        #MARKER_DISTANCE_CENTERLINE
        $sth_ils5->bind_param(  8, substrim( $line, 121,  4 ) );
        #MARKER_DIRECTION_CENTERLINE
        $sth_ils5->bind_param(  9, substrim( $line, 125,  1 ) );
        #MARKER_ELEVATION_MSL
        $sth_ils5->bind_param( 10, substrim( $line, 128,  7 ) );
        #MARKER_FACILITY_TYPE
        $sth_ils5->bind_param( 11, substrim( $line, 135, 15 ) );
        #MARKER_BEACON_ID
        $sth_ils5->bind_param( 12, substrim( $line, 150,  2 ) );
        #MARKER_BEACON_NAME
        $sth_ils5->bind_param( 13, substrim( $line, 152, 30 ) );
        #MARKER_BEACON_FREQUENCY
        $sth_ils5->bind_param( 14, substrim( $line, 182,  3 ) );
        #MARKER_NAVAID_ID
        $sth_ils5->bind_param( 15, substrim( $line, 185, 25 ) );
        #MARKER_SERVICE
        $sth_ils5->bind_param( 16, substrim( $line, 232, 30 ) );

        $sth_ils5->execute();
    }
    elsif ( $type eq "ILS6" )
    {
        #SITE_NUMBER
        $sth_ils6->bind_param( 1, substrim( $line,   4,  11 ) );
        #RUNWAY_ID
        $sth_ils6->bind_param( 2, substrim( $line,  15,   3 ) );
        #ILS_TYPE
        $sth_ils6->bind_param( 3, substrim( $line,  18,  10 ) );
        #ILS_REMARKS
        $sth_ils6->bind_param( 4, substrim( $line,  28, 400 ) );

        $sth_ils6->execute();
    }

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( "\rFinished processing $i records.\n" );
close ILS_FILE;

###########################################################################

my $AFF = $FADDS_BASE."/AFF.txt";
open( AFF_FILE, "<$AFF" ) or die "Could not open data file\n";

$i = 0;
print( "$AFF\n" );
while ( my $line = <AFF_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    my $type = substrim( $line, 0, 4 );

    if ( $type eq "AFF1" )
    {
        #ARTCC_ID
        $sth_aff1->bind_param( 1, substrim( $line,   4,  4 ) );
        #ARTCC_NAME
        $sth_aff1->bind_param( 2, capitalize( $line, 8, 40 ) );
        #SITE_LOCATION
        $sth_aff1->bind_param( 3, capitalize( $line, 48, 30 ) );
        #FACILITY_TYPE
        $sth_aff1->bind_param( 4, substrim( $line, 128,  5 ) );
        #ARTCC_LATTITUDE_DEGREES
        my $lattitude = substrim( $line, 189, 10 )/3600.0;
        if ( substr( $line, 199, 1 ) eq "S" )
        {
            $lattitude *= -1;
        }
        $sth_aff1->bind_param( 5, $lattitude );
        #ARTCC_LONGITUDE_DEGREES
        my $longitude = substrim( $line, 214, 10 )/3600.0;
        if ( substr( $line, 224, 1 ) eq "W" )
        {
            $longitude *= -1;
        }
        $sth_aff1->bind_param( 6, $longitude );

        $sth_aff1->execute();
    }
    elsif ( $type eq "AFF2" ) 
    {
        #ARTCC_ID
        $sth_aff2->bind_param( 1, substrim( $line,  4,   4 ) );
        #SITE_LOCATION
        $sth_aff2->bind_param( 2, capitalize( $line,  8,  30 ) );
        #FACILITY_TYPE
        $sth_aff2->bind_param( 3, substrim( $line, 38,   5 ) );
        #REMARK_ELEMENT_NO
        $sth_aff2->bind_param( 4, substrim( $line, 43,   4 ) );
        #REMARK_TEXT
        $sth_aff2->bind_param( 5, substrim( $line, 47, 200 ) );

        $sth_aff2->execute();
    }
    elsif ( $type eq "AFF3" ) 
    {
        #ARTCC_ID
        $sth_aff3->bind_param( 1, substrim( $line,  4,  4 ) );
        #SITE_LOCATION
        $sth_aff3->bind_param( 2, capitalize( $line,  8, 30 ) );
        #FACILITY_TYPE
        $sth_aff3->bind_param( 3, substrim( $line, 38,  5 ) );
        #SITE_FREQUENCY
        $sth_aff3->bind_param( 4, substrim( $line, 43,  8 ) );
        #FREQ_ALTITUDE
        $sth_aff3->bind_param( 5, capitalize( $line, 51, 10 ) );
        #FREQ_USAGE_NAME
        $sth_aff3->bind_param( 6, capitalize( $line, 61, 17 ) );
        #IFR_FACILITY_ID
        $sth_aff3->bind_param( 7, substrim( $line, 78,  4 ) );

        $sth_aff3->execute();
    }
    elsif ( $type eq "AFF4" ) 
    {
        #ARTCC_ID
        $sth_aff4->bind_param( 1, substrim( $line,  4,   4 ) );
        #SITE_LOCATION
        $sth_aff4->bind_param( 2, capitalize( $line,  8,  30 ) );
        #FACILITY_TYPE
        $sth_aff4->bind_param( 3, substrim( $line, 38,   5 ) );
        #REMARK_FREQUENCY
        $sth_aff4->bind_param( 4, substrim( $line, 43,   8 ) );
        #REMARKS_ELEMENT_NO
        $sth_aff4->bind_param( 5, substrim( $line, 51,   2 ) );
        #REMARK_TEXT
        $sth_aff4->bind_param( 6, substrim( $line, 53, 200 ) );

        $sth_aff4->execute();
    }

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( "\rFinished processing $i records.\n" );
close AFF_FILE;

###########################################################################

my $WXL = $FADDS_BASE."/WXL.txt";
open( WXL_FILE, "<$WXL" ) or die "Could not open data file\n";

$i = 0;
print( "$WXL\n" );
while ( my $line = <WXL_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    if ( substr( $line, 0, 1 ) eq "*" )
    {
        #Ignore and skip the continuation record
        next;
    }

    #LOCAION_ID
    $sth_wxl->bind_param( 1, substrim( $line,  0,  5 ) );
    #LOC_LATITUDE_DEGREES
    my $deg = substr( $line, 5, 2 );
    my $min = substr( $line, 7, 2 );
    my $sec = substr( $line, 9, 3 );
    my $lattitude = $deg+$min/60+$sec/36000;
    if ( substr( $line, 12, 1 ) eq "S" )
    {
        $lattitude *= -1;
    }
    $sth_wxl->bind_param( 2, $lattitude );
    #LOC_LONGITUDE_DEGREES
    my $deg = substr( $line, 13, 3 );
    my $min = substr( $line, 16, 2 );
    my $sec = substr( $line, 18, 3 );
    my $longitude = $deg+$min/60+$sec/36000;
    if ( substr( $line, 21, 1 ) eq "W" )
    {
        $longitude *= -1;
    }
    $sth_wxl->bind_param( 3, $longitude );
    #ASSOC_CITY
    $sth_wxl->bind_param( 4, capitalize( $line, 22, 26 ) );
    #ASSOC_STATE
    $sth_wxl->bind_param( 5, substrim( $line, 48,  2 ) );
    #LOC_ELEVATION_FEET
    $sth_wxl->bind_param( 6, substrim( $line, 53,  5 ) );
    #LOC_ELEVATION_ACCURACY
    $sth_wxl->bind_param( 7, substrim( $line, 58,  1 ) );
    #WX_SERICES_TYPES
    $sth_wxl->bind_param( 8, substrim( $line, 59, 60 ) );

    $sth_wxl->execute();

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( "\rFinished processing $i records.\n" );
close WXL_FILE;

###########################################################################

my $COM = $FADDS_BASE."/COM.txt";
open( COM_FILE, "<$COM" ) or die "Could not open data file\n";

$i = 0;
print( "$COM\n" );
while ( my $line = <COM_FILE> )
{
    ++$i;

    if ( ($i % 100) == 0 )
    {
        $dbh->do( "PRAGMA synchronous=ON" );
    }

    #COMM_OUTLET_ID
    $sth_com->bind_param( 1, substrim( $line,   0,   4 ) );
    #COMM_OUTLET_TYPE
    $sth_com->bind_param( 2, substrim( $line,   4,   7 ) );
    #ASSOC_NAVAID_ID
    my $navaid_id = substrim( $line,  11,   4 );
    $sth_com->bind_param( 3, $navaid_id );
    #COMM_OUTLET_LATITUDE_DEGREES
    my $offset = 186;
    if ( length( $navaid_id ) > 0 )
    {
        $offset = 89;
    }
    my $lat = substrim( $line, $offset, 14 );
    $sth_com->bind_param( 4, geo_parse_lat_dms( $lat ) );
    #COMM_OUTLET_LONGITUDE_DEGREES
    $offset = 200;
    if ( length( $navaid_id ) > 0 )
    {
        $offset = 103;
    }
    my $lon = substrim( $line, $offset, 14 );
    $sth_com->bind_param( 5, geo_parse_lon_dms( $lon ) );
    #COMM_OUTLET_CALL
    $sth_com->bind_param( 6, capitalize( $line, 214,  26 ) );
    #COMM_OUTLET_FREQS
    $sth_com->bind_param( 7, substrim( $line, 240, 144 ) );
    #FSS_IDENT
    $sth_com->bind_param( 8, substrim( $line, 384,   4 ) );
    #FSS_NAME
    $sth_com->bind_param( 9, capitalize( $line, 392,  26 ) );

    $sth_com->execute();

    if ( ($i % 100) == 0 )
    {
        print( "\rProcessed $i records..." );
        $| = 1;
        $dbh->do( "PRAGMA synchronous=OFF" );
    }
}

print( "\rFinished processing $i records.\n" );
close COM_FILE;

###########################################################################

$dbh->disconnect();

exit;

###########################################################################

sub geo_parse_lat_dms
{
    my $lat = 0;
    my $dms = shift;
    if ( $dms =~ /(\d+)-(\d+)-(\d+\.\d+)([NS])/ )
    {
        $lat = $1+$2/60+$3/3600;
        if ( $4 eq "S" )
        {
            $lat *= -1;
        }
    }
    return $lat;
}

sub geo_parse_lon_dms
{
    my $lon = 0;
    my $dms = shift;
    if ( $dms =~ /(\d+)-(\d+)-(\d+\.\d+)([EW])/ )
    {
        $lon = $1+$2/60+$3/3600;
        if ( $4 eq "W" )
        {
            $lon *= -1;
        }
    }
    return $lon;
}
