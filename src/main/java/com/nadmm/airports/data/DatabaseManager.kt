/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2026 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nadmm.airports.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.provider.BaseColumns
import android.util.Log
import com.nadmm.airports.utils.SystemUtils
import java.io.File

/**
 * Opens the installed FAA databases by type, and keeps the catalog of
 * installed data and the user's favorites in their own databases.
 *
 * The nested objects name each table and its columns. Tables with an
 * `_id` column use [BaseColumns._ID] for it.
 */
class DatabaseManager private constructor(private val context: Context) {

    private val catalogDbHelper = CatalogDbOpenHelper(context)
    private val userDataDbHelper = UserDataDbOpenHelper(context)
    private val databases = HashMap<String, SQLiteDatabase>()

    object LocationColumns {
        const val LOCATION = "LOCATION"
        const val DISTANCE = "DISTANCE"
        const val BEARING = "BEARING"
        const val RADIUS = "RADIUS"
        const val RADIAL = "RADIAL"
    }

    object Airports {
        const val TABLE_NAME = "airports"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val FAA_CODE = "FAA_CODE"
        const val EFFECTIVE_DATE = "EFFECTIVE_DATE"
        const val REGION_CODE = "REGION_CODE"
        const val ASSOC_STATE = "ASSOC_STATE"
        const val ASSOC_COUNTY = "ASSOC_COUNTY"
        const val ASSOC_CITY = "ASSOC_CITY"
        const val FACILITY_NAME = "FACILITY_NAME"
        const val OWNERSHIP_TYPE = "OWNERSHIP_TYPE"
        const val FACILITY_USE = "FACILITY_USE"
        const val OWNER_NAME = "OWNER_NAME"
        const val OWNER_ADDRESS = "OWNER_ADDRESS"
        const val OWNER_CITY_STATE_ZIP = "OWNER_CITY_STATE_ZIP"
        const val OWNER_PHONE = "OWNER_PHONE"
        const val MANAGER_NAME = "MANAGER_NAME"
        const val MANAGER_ADDRESS = "MANAGER_ADDRESS"
        const val MANAGER_CITY_STATE_ZIP = "MANAGER_CITY_STATE_ZIP"
        const val MANAGER_PHONE = "MANAGER_PHONE"
        const val REF_LATTITUDE_DEGREES = "REF_LATTITUDE_DEGREES"
        const val REF_LONGITUDE_DEGREES = "REF_LONGITUDE_DEGREES"
        const val REF_METHOD = "REFERENCE_METHOD"
        const val ELEVATION_MSL = "ELEVATION_MSL"
        const val ELEVATION_METHOD = "ELEVATION_METHOD"
        const val MAGNETIC_VARIATION_DEGREES = "MAGNETIC_VARIATION_DEGREES"
        const val MAGNETIC_VARIATION_DIRECTION = "MAGNETIC_VARIATION_DIRECTION"
        const val MAGNETIC_VARIATION_YEAR = "MAGNETIC_VARIATION_YEAR"
        const val PATTERN_ALTITUDE_AGL = "PATTERN_ALTITUDE_AGL"
        const val SECTIONAL_CHART = "SECTIONAL_CHART"
        const val DISTANCE_FROM_CITY_NM = "DISTANCE_FROM_CITY_NM"
        const val DIRECTION_FROM_CITY = "DIRECTION_FROM_CITY"
        const val BOUNDARY_ARTCC_ID = "BOUNDARY_ARTCC_ID"
        const val BOUNDARY_ARTCC_NAME = "BOUNDARY_ARTCC_NAME"
        const val RESPONSIBLE_ARTCC_ID = "RESPONSIBLE_ARTCC_ID"
        const val RESPONSIBLE_ARTCC_NAME = "RESPONSIBLE_ARTCC_NAME"
        const val FSS_ON_SITE = "FSS_ON_SITE"
        const val FSS_ID = "FSS_ID"
        const val FSS_NAME = "FSS_NAME"
        const val FSS_LOCAL_PHONE = "FSS_LOCAL_PHONE"
        const val FSS_TOLLFREE_PHONE = "FSS_TOLLFREE_PHONE"
        const val NOTAM_FACILITY_ID = "NOTAM_FACILITY_ID"
        const val NOTAM_D_AVAILABLE = "NOTAM_D_AVAILABLE"
        const val ACTIVATION_DATE = "ACTIVATION_DATE"
        const val STATUS_CODE = "STATUS_CODE"
        const val INTL_ENTRY_AIRPORT = "INTL_ENTRY_AIRPORT"
        const val CUSTOMS_LANDING_RIGHTS_AIRPORT = "CUSTOMS_LANDING_RIGHTS_AIRPORT"
        const val CIVIL_MILITARY_JOINT_USE = "CIVIL_MILITARY_JOINT_USE"
        const val MILITARY_LANDING_RIGHTS = "MILITARY_LANDING_RIGHTS"
        const val FUEL_TYPES = "FUEL_TYPES"
        const val AIRFRAME_REPAIR_SERVICE = "AIRFRAME_REPAIR_SERVICE"
        const val POWER_PLANT_REPAIR_SERVICE = "POWER_PLANT_REPAIR_SERVICE"
        const val BOTTLED_O2_AVAILABLE = "BOTTLED_O2_AVAILABLE"
        const val BULK_O2_AVAILABLE = "BULK_O2_AVAILABLE"
        const val LIGHTING_SCHEDULE = "LIGHTING_SCHEDULE"
        const val BEACON_LIGHTING_SCHEDULE = "BEACON_LIGHTING_SCHEDULE"
        const val TOWER_ON_SITE = "TOWER_ON_SITE"
        const val UNICOM_FREQS = "UNICOM_FREQS"
        const val CTAF_FREQ = "CTAF_FREQ"
        const val SEGMENTED_CIRCLE = "SEGMENTED_CIRCLE"
        const val BEACON_COLOR = "BEACON_COLOR"
        const val LANDING_FEE = "LANDING_FEE"
        const val MEDICAL_USE = "MEDICAL_USE"
        const val SINGLE_ENGINE_COUNT = "SINGLE_ENGINE_COUNT"
        const val MULTI_ENGINE_COUNT = "MULTI_ENGINE_COUNT"
        const val JET_ENGINE_COUNT = "JET_ENGINE_COUNT"
        const val HELI_COUNT = "HELI_COUNT"
        const val GLIDERS_COUNT = "GLIDERS_COUNT"
        const val MILITARY_COUNT = "MILITARY_COUNT"
        const val ULTRA_LIGHT_COUNT = "ULTRA_LIGHT_COUNT"
        const val ANNUAL_COMMERCIAL_OPS = "ANNUAL_COMMERCIAL_OPS"
        const val ANNUAL_COMMUTER_OPS = "ANNUAL_COMMUTER_OPS"
        const val ANNUAL_AIRTAXI_OPS = "ANNUAL_AIRTAXI_OPS"
        const val ANNUAL_GA_LOCAL_OPS = "ANNUAL_GA_LOCAL_OPS"
        const val ANNUAL_GA_ININERANT_OPS = "ANNUAL_GA_ININERANT_OPS"
        const val ANNUAL_MILITARY_OPS = "ANNUAL_MILITARY_OPS"
        const val OPS_REFERENCE_DATE = "OPS_REFERENCE_DATE"
        const val CONTRACT_FUEL_AVAILABLE = "CONTRACT_FUEL_AVAILABLE"
        const val STORAGE_FACILITY = "STORAGE_FACILITY"
        const val OTHER_SERVICES = "OTHER_SERVICES"
        const val WIND_INDICATOR = "WIND_INDICATOR"
        const val ICAO_CODE = "ICAO_CODE"
        const val TIMEZONE_ID = "TIMEZONE_ID"
    }

    object Runways {
        const val TABLE_NAME = "runways"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val RUNWAY_LENGTH = "RUNWAY_LENGTH"
        const val RUNWAY_WIDTH = "RUNWAY_WIDTH"
        const val SURFACE_TYPE = "SURFACE_TYPE"
        const val SURFACE_TREATMENT = "SURFACE_TREATMENT"
        const val EDGE_LIGHTS_INTENSITY = "EDGE_LIGHTS_INTENSITY"
        const val BASE_END_ID = "BASE_END_ID"
        const val BASE_END_HEADING = "BASE_END_HEADING"
        const val BASE_END_ILS_TYPE = "BASE_END_ILS_TYPE"
        const val BASE_END_RIGHT_TRAFFIC = "BASE_END_RIGHT_TRAFFIC"
        const val BASE_END_MARKING_TYPE = "BASE_END_MARKING_TYPE"
        const val BASE_END_MARKING_CONDITION = "BASE_END_MARKING_CONDITION"
        const val BASE_END_LATTITUDE_DEGREES = "BASE_END_LATTITUDE_DEGREES"
        const val BASE_END_LONGITUDE_DEGREES = "BASE_END_LONGITUDE_DEGREES"
        const val BASE_END_RUNWAY_ELEVATION = "BASE_END_RUNWAY_ELEVATION"
        const val BASE_END_THRESHOLD_CROSSING_HEIGHT = "BASE_END_THRESHOLD_CROSSING_HEIGHT"
        const val BASE_END_GLIDE_ANGLE = "BASE_END_GLIDE_ANGLE"
        const val BASE_END_DISPLACED_THRESHOLD_LENGTH = "BASE_END_DISPLACED_THRESHOLD_LENGTH"
        const val BASE_END_TDZ_ELEVATION = "BASE_END_TDZ_ELEVATION"
        const val BASE_END_VISUAL_GLIDE_SLOPE = "BASE_END_VISUAL_GLIDE_SLOPE"
        const val BASE_END_RVR_LOCATIONS = "BASE_END_RVR_LOCATIONS"
        const val BASE_END_APCH_LIGHT_SYSTEM = "BASE_END_APCH_LIGHT_SYSTEM"
        const val BASE_END_REIL_AVAILABLE = "BASE_END_REIL_AVAILABLE"
        const val BASE_END_CENTERLINE_LIGHTS_AVAILABLE = "BASE_END_CENTERLINE_LIGHTS_AVAILABLE"
        const val BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE = "BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE"
        const val BASE_END_CONTROLLING_OBJECT = "BASE_END_CONTROLLING_OBJECT"
        const val BASE_END_CONTROLLING_OBJECT_LIGHTED = "BASE_END_CONTROLLING_OBJECT_LIGHTED"
        const val BASE_END_CONTROLLING_OBJECT_SLOPE = "BASE_END_CONTROLLING_OBJECT_SLOPE"
        const val BASE_END_CONTROLLING_OBJECT_HEIGHT = "BASE_END_CONTROLLING_OBJECT_HEIGHT"
        const val BASE_END_CONTROLLING_OBJECT_DISTANCE = "BASE_END_CONTROLLING_OBJECT_DISTANCE"
        const val BASE_END_CONTROLLING_OBJECT_OFFSET = "BASE_END_CONTROLLING_OBJECT_OFFSET"
        const val RECIPROCAL_END_ID = "RECIPROCAL_END_ID"
        const val RECIPROCAL_END_HEADING = "RECIPROCAL_END_HEADING"
        const val RECIPROCAL_END_ILS_TYPE = "RECIPROCAL_END_ILS_TYPE"
        const val RECIPROCAL_END_RIGHT_TRAFFIC = "RECIPROCAL_END_RIGHT_TRAFFIC"
        const val RECIPROCAL_END_MARKING_TYPE = "RECIPROCAL_END_MARKING_TYPE"
        const val RECIPROCAL_END_MARKING_CONDITION = "RECIPROCAL_END_MARKING_CONDITION"
        const val RECIPROCAL_END_LATTITUDE_DEGREES = "RECIPROCAL_END_LATTITUDE_DEGREES"
        const val RECIPROCAL_END_LOGITUDE_DEGREES = "RECIPROCAL_END_LONGITUDE_DEGREES"
        const val RECIPROCAL_END_RUNWAY_ELEVATION = "RECIPROCAL_END_RUNWAY_ELEVATION"
        const val RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT = "RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT"
        const val RECIPROCAL_END_GLIDE_ANGLE = "RECIPROCAL_END_GLIDE_ANGLE"
        const val RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH = "RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH"
        const val RECIPROCAL_END_TDZ_ELEVATION = "RECIPROCAL_END_TDZ_ELEVATION"
        const val RECIPROCAL_END_VISUAL_GLIDE_SLOPE = "RECIPROCAL_END_VISUAL_GLIDE_SLOPE"
        const val RECIPROCAL_END_RVR_LOCATIONS = "RECIPROCAL_END_RVR_LOCATIONS"
        const val RECIPROCAL_END_APCH_LIGHT_SYSTEM = "RECIPROCAL_END_APCH_LIGHT_SYSTEM"
        const val RECIPROCAL_END_REIL_AVAILABLE = "RECIPROCAL_END_REIL_AVAILABLE"
        const val RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE = "RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE"
        const val RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE = "RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE"
        const val RECIPROCAL_END_CONTROLLING_OBJECT = "RECIPROCAL_END_CONTROLLING_OBJECT"
        const val RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED = "RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED"
        const val RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE = "RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE"
        const val RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT = "RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT"
        const val RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE = "RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE"
        const val RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET = "RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET"
        const val BASE_END_GRADIENT = "BASE_END_GRADIENT"
        const val BASE_END_GRADIENT_DIRECTION = "BASE_END_GRADIENT_DIRECTION"
        const val BASE_END_TORA = "BASE_END_TORA"
        const val BASE_END_TODA = "BASE_END_TODA"
        const val BASE_END_ASDA = "BASE_END_ASDA"
        const val BASE_END_LDA = "BASE_END_LDA"
        const val BASE_END_LAHSO_DISTANCE = "BASE_END_LAHSO_DISTANCE"
        const val BASE_END_LAHSO_RUNWAY = "BASE_END_LAHSO_RUNWAY"
        const val RECIPROCAL_END_GRADIENT = "RECIPROCAL_END_GRADIENT"
        const val RECIPROCAL_END_GRADIENT_DIRECTION = "RECIPROCAL_END_GRADIENT_DIRECTION"
        const val RECIPROCAL_END_TORA = "RECIPROCAL_END_TORA"
        const val RECIPROCAL_END_TODA = "RECIPROCAL_END_TODA"
        const val RECIPROCAL_END_ASDA = "RECIPROCAL_END_ASDA"
        const val RECIPROCAL_END_LDA = "RECIPROCAL_END_LDA"
        const val RECIPROCAL_END_LAHSO_DISTANCE = "RECIPROCAL_END_LAHSO_DISTANCE"
        const val RECIPROCAL_END_LAHSO_RUNWAY = "RECIPROCAL_END_LAHSO_RUNWAY"
    }

    object Ars {
        const val TABLE_NAME = "ars"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val RUNWAY_END_ID = "RUNWAY_END_ID"
        const val ARRESTING_DEVICE = "ARRESTING_DEVICE"
    }

    object Remarks {
        const val TABLE_NAME = "remarks"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val REMARK_NAME = "REMARK_NAME"
        const val REMARK_TEXT = "REMARK_TEXT"
    }

    object Attendance {
        const val TABLE_NAME = "attendance"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val SEQUENCE_NUMBER = "SEQUENCE_NUMBER"
        const val ATTENDANCE_SCHEDULE = "ATTENDANCE_SCHEDULE"
    }

    object Tower1 {
        const val TABLE_NAME = "tower1"
        const val FACILITY_ID = "FACILITY_ID"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val FACILITY_TYPE = "FACILITY_TYPE"
        const val RADIO_CALL_TOWER = "RADIO_CALL_TOWER"
        const val RADIO_CALL_APCH = "RADIO_CALL_APCH"
        const val RADIO_CALL_DEP = "RADIO_CALL_DEP"
        const val RADIO_CALL_APCH_PRIMARY = "RADIO_CALL_APCH_PRIMARY"
        const val RADIO_CALL_DEP_PRIMARY = "RADIO_CALL_DEP_PRIMARY"
        const val RADIO_CALL_APCH_SECONDARY = "RADIO_CALL_APCH_SECONDARY"
        const val RADIO_CALL_DEP_SECONDARY = "RADIO_CALL_DEP_SECONDARY"
    }

    object Tower2 {
        const val TABLE_NAME = "tower2"
        const val FACILITY_ID = "FACILITY_ID"
        const val PRIMARY_APPROACH_HOURS = "PRIMARY_APPROACH_HOURS"
        const val SECONDARY_APPROACH_HOURS = "SECONDARY_APPROACH_HOURS"
        const val PRIMARY_DEPARTURE_HOURS = "PRIMARY_DEPARTURE_HOURS"
        const val SECONDARY_DEPARTURE_HOURS = "SECONDARY_DEPARTURE_HOURS"
        const val CONTROL_TOWER_HOURS = "CONTROL_TOWER_HOURS"
    }

    object Tower3 {
        const val TABLE_NAME = "tower3"
        const val FACILITY_ID = "FACILITY_ID"
        const val MASTER_AIRPORT_FREQ = "MASTER_AIRPORT_FREQ"
        const val MASTER_AIRPORT_FREQ_USE = "MASTER_AIRPORT_FREQ_USE"
    }

    object Tower4 {
        const val TABLE_NAME = "tower4"
        const val FACILITY_ID = "FACILITY_ID"
        const val MASTER_AIRPORT_SERVICES = "MASTER_AIRPORT_SERVICES"
    }

    object Tower5 {
        const val TABLE_NAME = "tower5"
        const val FACILITY_ID = "FACILITY_ID"
        const val PRIMARY_APPROACH_RADAR = "PRIMARY_APPROACH_RADAR"
        const val SECONDARY_APPROACH_RADAR = "SECONDARY_APPROACH_RADAR"
        const val PRIMARY_DEPARTURE_RADAR = "PRIMARY_DEPARTURE_RADAR"
        const val SECONDARY_DEPARTURE_RADAR = "SECONDARY_DEPARTURE_RADAR"
        const val TOWER_RADAR_TYPE_1 = "TOWER_RADAR_TYPE_1"
        const val TOWER_RADAR_HOURS_1 = "TOWER_RADAR_HOURS_1"
        const val TOWER_RADAR_TYPE_2 = "TOWER_RADAR_TYPE_2"
        const val TOWER_RADAR_HOURS_2 = "TOWER_RADAR_HOURS_2"
        const val TOWER_RADAR_TYPE_3 = "TOWER_RADAR_TYPE_3"
        const val TOWER_RADAR_HOURS_3 = "TOWER_RADAR_HOURS_3"
        const val TOWER_RADAR_TYPE_4 = "TOWER_RADAR_TYPE_4"
        const val TOWER_RADAR_HOURS_4 = "TOWER_RADAR_HOURS_4"
    }

    object Tower6 {
        const val TABLE_NAME = "tower6"
        const val FACILITY_ID = "FACILITY_ID"
        const val ELEMENT_NUMBER = "ELEMENT_NUMBER"
        const val REMARK_TEXT = "REMARK_TEXT"
    }

    object Tower7 {
        const val TABLE_NAME = "tower7"
        const val SATELLITE_AIRPORT_FREQ_USE = "SATELLITE_AIRPORT_FREQ_USE"
        const val SATELLITE_AIRPORT_SITE_NUMBER = "SATELLITE_AIRPORT_SITE_NUMBER"
        const val MASTER_AIRPORT_SITE_NUMBER = "MASTER_AIRPORT_SITE_NUMBER"
        const val SATELLITE_AIRPORT_FREQ = "SATELLITE_AIRPORT_FREQ"
    }

    object Tower8 {
        const val TABLE_NAME = "tower8"
        const val FACILITY_ID = "FACILITY_ID"
        const val AIRSPACE_TYPES = "AIRSPACE_TYPES"
        const val AIRSPACE_HOURS = "AIRSPACE_HOURS"
    }

    object Tower9 {
        const val TABLE_NAME = "tower9"
        const val FACILITY_ID = "FACILITY_ID"
        const val ATIS_SERIAL_NO = "ATIS_SERIAL_NO"
        const val ATIS_HOURS = "ATIS_HOURS"
        const val ATIS_PURPOSE = "ATIS_PURPOSE"
        const val ATIS_PHONE = "ATIS_PHONE"
    }

    object Awos1 {
        const val TABLE_NAME = "awos1"
        const val WX_SENSOR_IDENT = "WX_SENSOR_IDENT"
        const val WX_SENSOR_TYPE = "WX_SENSOR_TYPE"
        const val COMMISSIONING_STATUS = "COMMISSIONING_STATUS"
        const val STATION_LATTITUDE_DEGREES = "STATION_LATTITUDE_DEGREES"
        const val STATION_LONGITUDE_DEGREES = "STATION_LONGITUDE_DEGREES"
        const val STATION_FREQUENCY = "STATION_FREQUENCY"
        const val SECOND_STATION_FREQUENCY = "SECOND_STATION_FREQUENCY"
        const val STATION_PHONE_NUMBER = "STATION_PHONE_NUMBER"
        const val SITE_NUMBER = "SITE_NUMBER"
    }

    object Awos2 {
        const val TABLE_NAME = "awos2"
        const val WX_SENSOR_IDENT = "WX_SENSOR_IDENT"
        const val WX_SENSOR_TYPE = "WX_SENSOR_TYPE"
        const val WX_STATION_REMARKS = "WX_STATION_REMARKS"
    }

    object Nav1 {
        const val TABLE_NAME = "nav1"
        const val NAVAID_ID = "NAVAID_ID"
        const val NAVAID_TYPE = "NAVAID_TYPE"
        const val NAVAID_NAME = "NAVAID_NAME"
        const val ASSOC_CITY = "ASSOC_CITY"
        const val ASSOC_STATE = "ASSOC_STATE"
        const val PUBLIC_USE = "PUBLIC_USE"
        const val NAVAID_CLASS = "NAVAID_CLASS"
        const val OPERATING_HOURS = "OPERATING_HOURS"
        const val REF_LATTITUDE_DEGREES = "REF_LATTITUDE_DEGREES"
        const val REF_LONGITUDE_DEGREES = "REF_LONGITUDE_DEGREES"
        const val ELEVATION_MSL = "ELEVATION_MSL"
        const val MAGNETIC_VARIATION_DEGREES = "MAGNETIC_VARIATION_DEGREES"
        const val MAGNETIC_VARIATION_DIRECTION = "MAGNETIC_VARIATION_DIRECTION"
        const val MAGNETIC_VARIATION_YEAR = "MAGNETIC_VARIATION_YEAR"
        const val VOICE_FEATURE = "VOICE_FEATURE"
        const val POWER_OUTPUT = "POWER_OUTPUT"
        const val AUTOMATIC_VOICE_IDENT = "AUTOMATIC_VOICE_IDENT"
        const val TACAN_CHANNEL = "TACAN_CHANNEL"
        const val NAVAID_FREQUENCY = "NAVAID_FREQUENCY"
        const val FANMARKER_TYPE = "FANMARKER_TYPE"
        const val PROTECTED_FREQUENCY_ALTITUDE = "PROTECTED_FREQUENCY_ALTITUDE"
    }

    object Nav2 {
        const val TABLE_NAME = "nav2"
        const val NAVAID_ID = "NAVAID_ID"
        const val NAVAID_TYPE = "NAVAID_TYPE"
        const val REMARK_TEXT = "REMARK_TEXT"
    }

    object Ils1 {
        const val TABLE_NAME = "ils1"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val ILS_TYPE = "ILS_TYPE"
        const val ILS_ID = "ILS_ID"
        const val ILS_CATEGORY = "ILS_CATEGORY"
        const val ILS_MAGNETIC_BEARING = "ILS_MAGNETIC_BEARING"
    }

    object Ils2 {
        const val TABLE_NAME = "ils2"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val ILS_TYPE = "ILS_TYPE"
        const val OPERATIONAL_STATUS = "OPERATIONAL_STATUS"
        const val OPERATIONAL_EFFECTIVE_DATE = "OPERATIONAL_EFFECTIVE_DATE"
        const val LOCALIZER_FREQUENCY = "LOCALIZER_FREQUENCY"
        const val LOCALIZER_BACK_COURSE_STATUS = "LOCALIZER_BACK_COURSE_STATUS"
        const val LOCALIZER_COURSE_WIDTH = "LOCALIZER_COURSE_WIDTH"
    }

    object Ils3 {
        const val TABLE_NAME = "ils3"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val ILS_TYPE = "ILS_TYPE"
        const val OPERATIONAL_STATUS = "OPERATIONAL_STATUS"
        const val OPERATIONAL_EFFECTIVE_DATE = "OPERATIONAL_EFFECTIVE_DATE"
        const val GLIDE_SLOPE_TYPE = "GLIDE_SLOPE_TYPE"
        const val GLIDE_SLOPE_ANGLE = "GLIDE_SLOPE_ANGLE"
        const val GLIDE_SLOPE_FREQUENCY = "GLIDE_SLOPE_FREQUENCY"
    }

    object Ils4 {
        const val TABLE_NAME = "ils4"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val ILS_TYPE = "ILS_TYPE"
        const val OPERATIONAL_STATUS = "OPERATIONAL_STATUS"
        const val OPERATIONAL_EFFECTIVE_DATE = "OPERATIONAL_EFFECTIVE_DATE"
        const val DME_CHANNEL = "DME_CHANNEL"
    }

    object Ils5 {
        const val TABLE_NAME = "ils5"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val ILS_TYPE = "ILS_TYPE"
        const val MARKER_TYPE = "MARKER_TYPE"
        const val OPERATIONAL_STATUS = "OPERATIONAL_STATUS"
        const val OPERATIONAL_EFFECTIVE_DATE = "OPERATIONAL_EFFECTIVE_DATE"
        const val MARKER_DISTANCE = "MARKER_DISTANCE"
        const val MARKER_DISTANCE_CENTERLINE = "MARKET_DISTANCE_CENTERLINE"
        const val MARKER_DIRECTION_CENTERLINE = "MARKET_DIRECTION_CENTERLINE"
        const val MARKER_ELEVATION_MSL = "MARKER_ELEVATION_MSL"
        const val MARKER_FACILITY_TYPE = "MARKER_FACILITY_TYPE"
        const val MARKER_BEACON_ID = "MARKER_BEACON_ID"
        const val MARKER_BEACON_NAME = "MARKER_BEACON_NAME"
        const val MARKER_BEACON_FREQUENCY = "MARKER_BEACON_FREQUENCY"
        const val MARKER_NAVAID_ID = "MARKER_NAVAID_ID"
        const val MARKER_SERVICE = "MARKER_SERVICE"
    }

    object Ils6 {
        const val TABLE_NAME = "ils6"
        const val SITE_NUMBER = "SITE_NUMBER"
        const val RUNWAY_ID = "RUNWAY_ID"
        const val ILS_TYPE = "ILS_TYPE"
        const val ILS_REMARKS = "ILS_REMARKS"
    }

    object Aff1 {
        const val TABLE_NAME = "aff1"
        const val ARTCC_ID = "ARTCC_ID"
        const val ARTCC_NAME = "ARTCC_NAME"
        const val SITE_LOCATION = "SITE_LOCATION"
        const val FACILITY_TYPE = "FACILITY_TYPE"
        const val ARTCC_LATTITUDE_DEGREES = "ARTCC_LATTITUDE_DEGREES"
        const val ARTCC_LONGITUDE_DEGREES = "ARTCC_LONGITUDE_DEGREES"
    }

    object Aff3 {
        const val TABLE_NAME = "aff3"
        const val ARTCC_ID = "ARTCC_ID"
        const val SITE_LOCATION = "SITE_LOCATION"
        const val FACILITY_TYPE = "FACILITY_TYPE"
        const val SITE_FREQUENCY = "SITE_FREQUENCY"
        const val FREQ_ALTITUDE = "FREQ_ALTITUDE"
        const val FREQ_USAGE_NAME = "FREQ_USAGE_NAME"
        const val IFR_FACILITY_ID = "IFR_FACILITY_ID"
    }

    object Com {
        const val TABLE_NAME = "com"
        const val COMM_OUTLET_ID = "COMM_OUTLET_ID"
        const val COMM_OUTLET_TYPE = "COMM_OUTLET_TYPE"
        const val ASSOC_NAVAID_ID = "ASSOC_NAVAID_ID"
        const val COMM_OUTLET_LATITUDE_DEGREES = "COMM_OUTLET_LATITUDE_DEGREES"
        const val COMM_OUTLET_LONGITUDE_DEGREES = "COMM_OUTLET_LONGITUDE_DEGREES"
        const val COMM_OUTLET_CALL = "COMM_OUTLET_CALL"
        const val COMM_OUTLET_FREQS = "COMM_OUTLET_FREQS"
        const val FSS_IDENT = "FSS_IDENT"
        const val FSS_NAME = "FSS_NAME"
    }

    object Wxs {
        const val TABLE_NAME = "wxs"
        const val STATION_ID = "STATION_ID"
        const val STATION_WMO_ID = "STATION_WMO_ID"
        const val STATION_NAME = "STATION_NAME"
        const val STATION_LATITUDE_DEGREES = "STATION_LATITUDE_DEGREES"
        const val STATION_LONGITUDE_DEGREES = "STATION_LONGITUDE_DEGREES"
        const val STATION_STATE = "STATION_STATE"
        const val STATION_COUNTRY = "STATION_COUNTRY"
        const val STATION_ELEVATOIN_METER = "STATION_ELEVATION_METER"
        const val STATION_SITE_TYPES = "STATION_SITE_TYPES"
    }

    object AtcPhones {
        const val TABLE_NAME = "atcphones"
        const val FACILITY_ID = "FACILITY_ID"
        const val FACILITY_TYPE = "FACILITY_TYPE"
        const val DUTY_OFFICE_PHONE = "DUTY_OFFICE_PHONE"
        const val BUSINESS_HOURS = "BUSINESS_HOURS"
        const val BUSINESS_PHONE = "BUSINESS_PHONE"
    }

    object DOF {
        const val TABLE_NAME = "dof"
        const val OAS_CODE = "OAS_CODE"
        const val VERIFICATION_STATUS = "VERIFICATION_STATUS"
        const val LATITUDE_DEGREES = "LATITUDE_DEGREES"
        const val LONGITUDE_DEGREES = "LONGITUDE_DEGREES"
        const val OBSTACLE_TYPE = "OBSTACLE_TYPE"
        const val COUNT = "COUNT"
        const val HEIGHT_AGL = "HEIGHT_AGL"
        const val HEIGHT_MSL = "HEIGHT_MSL"
        const val LIGHTING_TYPE = "LIGHTING_TYPE"
        const val ACCURACY_HOR = "ACCURACY_HOR"
        const val ACCURACY_VER = "ACCURACY_VER"
        const val MARKING_TYPE = "MARKING_TYPE"
        const val ACTION_CODE = "ACTION_CODE"
        const val ACTION_DATE = "ACTION_DATE"
    }

    object DtppCycle {
        const val TABLE_NAME = "cycle"
        const val TPP_CYCLE = "TPP_CYCLE"
        const val FROM_DATE = "FROM_DATE"
        const val TO_DATE = "TO_DATE"
    }

    object Dtpp {
        const val TABLE_NAME = "dtpp"
        const val TPP_VOLUME = "TPP_VOLUME"
        const val FAA_CODE = "FAA_CODE"
        const val CHART_SEQ = "CHART_SEQ"
        const val CHART_CODE = "CHART_CODE"
        const val CHART_NAME = "CHART_NAME"
        const val USER_ACTION = "USER_ACTION"
        const val PDF_NAME = "PDF_NAME"
        const val FAANFD18_CODE = "FAANFD18_CODE"
        const val MILITARY_USE = "MILITARY_USE"
        const val COPTER_USE = "COPTER_USE"
    }

    object DafdCycle {
        const val TABLE_NAME = "cycle"
        const val AFD_CYCLE = "AFD_CYCLE"
        const val FROM_DATE = "FROM_DATE"
        const val TO_DATE = "TO_DATE"
    }

    object Dafd {
        const val TABLE_NAME = "dafd"
        const val FAA_CODE = "FAA_CODE"
        const val PDF_NAME = "PDF_NAME"
    }

    object BookCategories {
        const val TABLE_NAME = "bookcategories"
        const val CATEGORY_CODE = "CATEGORY_CODE"
        const val CATEGORY_NAME = "CATEGORY_NAME"
    }

    object Library {
        const val TABLE_NAME = "library"
        const val CATEGORY_CODE = "CATEGORY_CODE"
        const val BOOK_NAME = "BOOK_NAME"
        const val BOOK_DESC = "BOOK_DESC"
        const val AUTHOR = "AUTHOR"
        const val EDITION = "EDITION"
        const val DOWNLOAD_SIZE = "DOWNLOAD_SIZE"
        const val FLAG = "FLAG"
    }

    object Catalog {
        const val TABLE_NAME = "catalog"
        const val TYPE = "TYPE"
        const val DESCRIPTION = "DESCRIPTION"
        const val VERSION = "VERSION"
        const val START_DATE = "START_DATE"
        const val END_DATE = "END_DATE"
        const val DB_NAME = "DB_NAME"
        const val INSTALL_DATE = "INSTALL_DATE"
    }

    object States {
        const val TABLE_NAME = "states"
        const val STATE_CODE = "STATE_CODE"
        const val STATE_NAME = "STATE_NAME"
    }

    object Favorites {
        const val TABLE_NAME = "favorites"
        const val TYPE = "TYPE"
        const val LOCATION_ID = "LOCATION_ID"
    }

    fun getDatabaseFile(dbName: String): File =
        SystemUtils.getExternalFile(context, "databases", dbName)

    val catalogDb: SQLiteDatabase
        get() = catalogDbHelper.writableDatabase

    val userDataDb: SQLiteDatabase
        get() = userDataDbHelper.writableDatabase

    /** The newest entry of each type that has taken effect, or null if the catalog can't be read. */
    val currentFromCatalog: Cursor?
        get() = try {
            val query = "SELECT *" +
                " FROM ${Catalog.TABLE_NAME} c1" +
                " WHERE ${Catalog.END_DATE}=(SELECT max(${Catalog.END_DATE})" +
                " FROM ${Catalog.TABLE_NAME} c2 WHERE" +
                " c2.${Catalog.TYPE}=c1.${Catalog.TYPE}" +
                " AND strftime('%s', c2.${Catalog.START_DATE}) <= strftime('%s', 'now') )"
            Log.d(TAG, query)
            catalogDb.rawQuery(query, null)
        } catch (e: Exception) {
            Log.d(e.javaClass.simpleName, e.message ?: "")
            null
        }

    val allFromCatalog: Cursor
        get() = catalogDb.rawQuery("SELECT * FROM ${Catalog.TABLE_NAME}", null)

    val aptFavorites: ArrayList<String>
        get() = getFavorites(FAVORITE_APT)

    val wxFavorites: ArrayList<String>
        get() = getFavorites(FAVORITE_WX)

    private fun getFavorites(type: String): ArrayList<String> {
        val favorites = ArrayList<String>()
        SQLiteQueryBuilder().apply {
            tables = Favorites.TABLE_NAME
            setDistinct(true)
        }.query(userDataDb, arrayOf(Favorites.LOCATION_ID),
            "${Favorites.TYPE}=?", arrayOf(type), null, null, null).use { c ->
            while (c.moveToNext()) {
                favorites.add(c.getString(c.getColumnIndexOrThrow(Favorites.LOCATION_ID)))
            }
        }
        return favorites
    }

    private fun isFavorite(type: String, id: String): Boolean =
        SQLiteQueryBuilder().apply {
            tables = Favorites.TABLE_NAME
            setDistinct(true)
        }.query(userDataDb, arrayOf(Favorites.LOCATION_ID),
            "${Favorites.LOCATION_ID}=? AND ${Favorites.TYPE}=?", arrayOf(id, type),
            null, null, null, null).use { it.moveToFirst() }

    private fun addFavorite(type: String, id: String): Long {
        val values = ContentValues().apply {
            put(Favorites.TYPE, type)
            put(Favorites.LOCATION_ID, id)
        }
        return userDataDb.insert(Favorites.TABLE_NAME, null, values)
    }

    private fun removeFavorite(type: String, id: String): Int =
        userDataDb.delete(Favorites.TABLE_NAME,
            "${Favorites.LOCATION_ID}=? AND ${Favorites.TYPE}=?", arrayOf(id, type))

    fun isFavoriteAirport(siteNumber: String) = isFavorite(FAVORITE_APT, siteNumber)

    fun addToFavoriteAirports(siteNumber: String) = addFavorite(FAVORITE_APT, siteNumber)

    fun removeFromFavoriteAirports(siteNumber: String) = removeFavorite(FAVORITE_APT, siteNumber)

    fun isFavoriteWx(facilityId: String) = isFavorite(FAVORITE_WX, facilityId)

    fun addToFavoriteWx(facilityId: String) = addFavorite(FAVORITE_WX, facilityId)

    fun removeFromFavoriteWx(facilityId: String) = removeFavorite(FAVORITE_WX, facilityId)

    /** Opens, read-only, the current database of each type in the catalog. */
    @Synchronized
    fun openDatabases() {
        currentFromCatalog?.use { c ->
            while (c.moveToNext()) {
                val type = c.getString(c.getColumnIndexOrThrow(Catalog.TYPE))
                val dbFile = getDatabaseFile(c.getString(c.getColumnIndexOrThrow(Catalog.DB_NAME)))
                Log.i(TAG, "Opening db type=$type, path=${dbFile.path}")
                try {
                    databases[type] = SQLiteDatabase.openDatabase(dbFile.path, null,
                        SQLiteDatabase.OPEN_READONLY)
                } catch (_: SQLiteException) {
                    Log.i(TAG, "Unable to open db type=$type")
                }
            }
        }
    }

    /** The open database of this type, opening them all first if none are; null if it is not installed. */
    fun getDatabase(type: String): SQLiteDatabase? {
        if (databases.isEmpty()) {
            // Open databases if not open already
            openDatabases()
        }
        return databases[type]
    }

    @Synchronized
    fun closeDatabases() {
        for ((type, db) in databases) {
            Log.i(TAG, "Closing db for type=$type")
            db.close()
        }
        databases.clear()
    }

    fun insertCatalogEntry(values: ContentValues): Int {
        val db = catalogDbHelper.writableDatabase
        val id = db.insert(Catalog.TABLE_NAME, null, values)
        if (id >= 0) {
            Log.i(TAG, "Inserted catalog entry: _id=$id")
        } else {
            Log.i(TAG, "Failed to insert into catalog entry")
        }
        db.close()
        return id.toInt()
    }

    private class CatalogDbOpenHelper(context: Context) :
        SQLiteOpenHelper(context, "catalog.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            Log.i(TAG, "Creating 'catalog' table")
            db.execSQL("CREATE TABLE ${Catalog.TABLE_NAME} ( " +
                "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${Catalog.TYPE} TEXT not null, " +
                "${Catalog.DESCRIPTION} TEXT not null, " +
                "${Catalog.VERSION} INTEGER not null, " +
                "${Catalog.START_DATE} TEXT not null, " +
                "${Catalog.END_DATE} TEXT not null, " +
                "${Catalog.DB_NAME} TEXT not null, " +
                "${Catalog.INSTALL_DATE} TEXT not null " +
                ")")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(TAG, "Upgrading ${Catalog.TABLE_NAME} db $oldVersion -> $newVersion")
            db.execSQL("DROP TABLE ${Catalog.TABLE_NAME}")
            onCreate(db)
        }
    }

    private class UserDataDbOpenHelper(context: Context) :
        SQLiteOpenHelper(context, "userdata.db", null, 2) {

        override fun onCreate(db: SQLiteDatabase) {
            Log.i(TAG, "Creating 'favorites' table")
            db.execSQL("CREATE TABLE ${Favorites.TABLE_NAME} ( " +
                "${Favorites.TYPE} TEXT, " +
                "${Favorites.LOCATION_ID} TEXT" +
                " )")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion == 1 && newVersion == 2) {
                Log.i(TAG, "Upgrading ${Favorites.TABLE_NAME} db $oldVersion -> $newVersion")
                val oldTable = "${Favorites.TABLE_NAME}_old"
                db.execSQL("ALTER TABLE ${Favorites.TABLE_NAME} RENAME TO $oldTable")
                onCreate(db)
                db.execSQL("INSERT INTO ${Favorites.TABLE_NAME} SELECT 'APT', SITE_NUMBER FROM $oldTable")
                db.execSQL("DROP TABLE $oldTable")
            }
        }
    }

    companion object {
        val TAG: String = DatabaseManager::class.java.simpleName

        const val DB_FADDS = "FADDS"
        const val DB_DTPP = "DTPP"
        const val DB_DAFD = "DAFD"
        const val DB_LIBRARY = "LIBRARY"
        const val DB_DOF = "DOF"

        private const val FAVORITE_APT = "APT"
        private const val FAVORITE_WX = "WX"

        @Volatile
        private var instance: DatabaseManager? = null

        @JvmStatic
        fun instance(context: Context): DatabaseManager =
            instance ?: synchronized(this) {
                instance ?: DatabaseManager(context.applicationContext).also { instance = it }
            }
    }
}
