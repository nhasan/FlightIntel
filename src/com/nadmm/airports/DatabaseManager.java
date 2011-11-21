/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseManager {

    public static final String TAG = DatabaseManager.class.getSimpleName();

    private final CatalogDbOpenHelper mCatalogDbHelper;
    private final UserDataDbOpenHelper mUserDataDbHelper;
    private final Context mContext;
    private final HashMap<String, SQLiteDatabase> mDatabases;

    private static final File EXTERNAL_STORAGE_DATA_DIRECTORY
            = new File( Environment.getExternalStorageDirectory(), 
                    "Android/data/"+DownloadActivity.class.getPackage().getName() );
    public static File DATABASE_DIR = new File( EXTERNAL_STORAGE_DATA_DIRECTORY, "/databases" );
    public static final String DB_FADDS = "FADDS";

    private static final Object sLock = new Object();
    private static DatabaseManager sInstance = null;

    public static final class Airports implements BaseColumns {
        public static final String TABLE_NAME = "airports";
        // Fields for airport table
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String FAA_CODE = "FAA_CODE";
        public static final String EFFECTIVE_DATE = "EFFECTIVE_DATE";
        public static final String REGION_CODE = "REGION_CODE";
        public static final String ASSOC_STATE = "ASSOC_STATE";
        public static final String ASSOC_COUNTY = "ASSOC_COUNTY";
        public static final String ASSOC_CITY = "ASSOC_CITY";
        public static final String FACILITY_NAME = "FACILITY_NAME";
        public static final String OWNERSHIP_TYPE = "OWNERSHIP_TYPE";
        public static final String FACILITY_USE = "FACILITY_USE";
        public static final String OWNER_NAME = "OWNER_NAME";
        public static final String OWNER_ADDRESS = "OWNER_ADDRESS";
        public static final String OWNER_CITY_STATE_ZIP = "OWNER_CITY_STATE_ZIP";
        public static final String OWNER_PHONE = "OWNER_PHONE";
        public static final String MANAGER_NAME = "MANAGER_NAME";
        public static final String MANAGER_ADDRESS = "MANAGER_ADDRESS";
        public static final String MANAGER_CITY_STATE_ZIP = "MANAGER_CITY_STATE_ZIP";
        public static final String MANAGER_PHONE = "MANAGER_PHONE";
        public static final String REF_LATTITUDE_DEGREES = "REF_LATTITUDE_DEGREES";
        public static final String REF_LONGITUDE_DEGREES = "REF_LONGITUDE_DEGREES";
        public static final String REF_METHOD = "REFERENCE_METHOD";
        public static final String ELEVATION_MSL = "ELEVATION_MSL";
        public static final String ELEVATION_METHOD = "ELEVATION_METHOD";
        public static final String MAGNETIC_VARIATION_DEGREES = "MAGNETIC_VARIATION_DEGREES";
        public static final String MAGNETIC_VARIATION_DIRECTION = "MAGNETIC_VARIATION_DIRECTION";
        public static final String MAGNETIC_VARIATION_YEAR = "MAGNETIC_VARIATION_YEAR";
        public static final String PATTERN_ALTITUDE_AGL = "PATTERN_ALTITUDE_AGL";
        public static final String SECTIONAL_CHART = "SECTIONAL_CHART";
        public static final String DISTANCE_FROM_CITY_NM = "DISTANCE_FROM_CITY_NM";
        public static final String DIRECTION_FROM_CITY = "DIRECTION_FROM_CITY";
        public static final String BOUNDARY_ARTCC_ID = "BOUNDARY_ARTCC_ID";
        public static final String BOUNDARY_ARTCC_NAME = "BOUNDARY_ARTCC_NAME";
        public static final String RESPONSIBLE_ARTCC_ID = "RESPONSIBLE_ARTCC_ID";
        public static final String RESPONSIBLE_ARTCC_NAME = "RESPONSIBLE_ARTCC_NAME";
        public static final String FSS_ON_SITE = "FSS_ON_SITE";
        public static final String FSS_ID = "FSS_ID";
        public static final String FSS_NAME = "FSS_NAME";
        public static final String FSS_LOCAL_PHONE = "FSS_LOCAL_PHONE";
        public static final String FSS_TOLLFREE_PHONE = "FSS_TOLLFREE_PHONE";
        public static final String NOTAM_FACILITY_ID = "NOTAM_FACILITY_ID";
        public static final String NOTAM_D_AVAILABLE = "NOTAM_D_AVAILABLE";
        public static final String ACTIVATION_DATE = "ACTIVATION_DATE";
        public static final String STATUS_CODE = "STATUS_CODE";
        public static final String INTL_ENTRY_AIRPORT = "INTL_ENTRY_AIRPORT";
        public static final String CUSTOMS_LANDING_RIGHTS_AIRPORT = "CUSTOMS_LANDING_RIGHTS_AIRPORT";
        public static final String CIVIL_MILITARY_JOINT_USE = "CIVIL_MILITARY_JOINT_USE";
        public static final String MILITARY_LANDING_RIGHTS = "MILITARY_LANDING_RIGHTS";
        public static final String FUEL_TYPES = "FUEL_TYPES";
        public static final String AIRFRAME_REPAIR_SERVICE = "AIRFRAME_REPAIR_SERVICE";
        public static final String POWER_PLANT_REPAIR_SERVICE = "POWER_PLANT_REPAIR_SERVICE";
        public static final String BOTTLED_O2_AVAILABLE = "BOTTLED_O2_AVAILABLE";
        public static final String BULK_O2_AVAILABLE = "BULK_O2_AVAILABLE";
        public static final String LIGHTING_SCHEDULE = "LIGHTING_SCHEDULE";
        public static final String TOWER_ON_SITE = "TOWER_ON_SITE";
        public static final String UNICOM_FREQS = "UNICOM_FREQS";
        public static final String CTAF_FREQ = "CTAF_FREQ";
        public static final String SEGMENTED_CIRCLE = "SEGMENTED_CIRCLE";
        public static final String BEACON_COLOR = "BEACON_COLOR";
        public static final String LANDING_FEE = "LANDING_FEE";
        public static final String STORAGE_FACILITY = "STORAGE_FACILITY";
        public static final String OTHER_SERVICES = "OTHER_SERVICES";
        public static final String WIND_INDICATOR = "WIND_INDICATOR";
        public static final String ICAO_CODE = "ICAO_CODE";
        public static final String TIMEZONE_ID = "TIMEZONE_ID";

        // These are not really columns in the table, but calculated
        public static final String DISTANCE = "DISTANCE";
        public static final String BEARING = "BEARING";
    }

    public static final class Runways implements BaseColumns {
        public static final String TABLE_NAME = "runways";
        // Fields for runway table
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String RUNWAY_ID = "RUNWAY_ID";
        public static final String RUNWAY_LENGTH = "RUNWAY_LENGTH";
        public static final String RUNWAY_WIDTH = "RUNWAY_WIDTH";
        public static final String SURFACE_TYPE = "SURFACE_TYPE";
        public static final String SURFACE_TREATMENT = "SURFACE_TREATMENT";
        public static final String EDGE_LIGHTS_INTENSITY = "EDGE_LIGHTS_INTENSITY";
        public static final String BASE_END_ID = "BASE_END_ID";
        public static final String BASE_END_HEADING = "BASE_END_HEADING";
        public static final String BASE_END_ILS_TYPE = "BASE_END_ILS_TYPE";
        public static final String BASE_END_RIGHT_TRAFFIC = "BASE_END_RIGHT_TRAFFIC";
        public static final String BASE_END_MARKING_TYPE = "BASE_END_MARKING_TYPE";
        public static final String BASE_END_MARKING_CONDITION = "BASE_END_MARKING_CONDITION";
        public static final String BASE_END_ARRESTING_DEVICE_TYPE = "BASE_END_ARRESTING_DEVICE_TYPE";
        public static final String BASE_END_LATTITUDE_DEGREES = "BASE_END_LATTITUDE_DEGREES";
        public static final String BASE_END_LONGITUDE_DEGREES = "BASE_END_LONGITUDE_DEGREES";
        public static final String BASE_END_RUNWAY_ELEVATION = "BASE_END_RUNWAY_ELEVATION";
        public static final String BASE_END_THRESHOLD_CROSSING_HEIGHT = "BASE_END_THRESHOLD_CROSSING_HEIGHT";
        public static final String BASE_END_GLIDE_ANGLE = "BASE_END_GLIDE_ANGLE";
        public static final String BASE_END_DISPLACED_THRESHOLD_LENGTH = "BASE_END_DISPLACED_THRESHOLD_LENGTH";
        public static final String BASE_END_TDZ_ELEVATION = "BASE_END_TDZ_ELEVATION";
        public static final String BASE_END_VISUAL_GLIDE_SLOPE = "BASE_END_VISUAL_GLIDE_SLOPE";
        public static final String BASE_END_RVR_LOCATIONS = "BASE_END_RVR_LOCATIONS";
        public static final String BASE_END_APCH_LIGHT_SYSTEM = "BASE_END_APCH_LIGHT_SYSTEM";
        public static final String BASE_END_REIL_AVAILABLE = "BASE_END_REIL_AVAILABLE";
        public static final String BASE_END_CENTERLINE_LIGHTS_AVAILABLE = "BASE_END_CENTERLINE_LIGHTS_AVAILABLE";
        public static final String BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE = "BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE";
        public static final String BASE_END_CONTROLLING_OBJECT = "BASE_END_CONTROLLING_OBJECT";
        public static final String BASE_END_CONTROLLING_OBJECT_LIGHTED = "BASE_END_CONTROLLING_OBJECT_LIGHTED";
        public static final String BASE_END_CONTROLLING_OBJECT_SLOPE = "BASE_END_CONTROLLING_OBJECT_SLOPE";
        public static final String BASE_END_CONTROLLING_OBJECT_HEIGHT = "BASE_END_CONTROLLING_OBJECT_HEIGHT";
        public static final String BASE_END_CONTROLLING_OBJECT_DISTANCE = "BASE_END_CONTROLLING_OBJECT_DISTANCE";
        public static final String BASE_END_CONTROLLING_OBJECT_OFFSET = "BASE_END_CONTROLLING_OBJECT_OFFSET";
        public static final String RECIPROCAL_END_ID = "RECIPROCAL_END_ID";
        public static final String RECIPROCAL_END_HEADING = "RECIPROCAL_END_HEADING";
        public static final String RECIPROCAL_END_ILS_TYPE = "RECIPROCAL_END_ILS_TYPE";
        public static final String RECIPROCAL_END_RIGHT_TRAFFIC = "RECIPROCAL_END_RIGHT_TRAFFIC";
        public static final String RECIPROCAL_END_MARKING_TYPE = "RECIPROCAL_END_MARKING_TYPE";
        public static final String RECIPROCAL_END_MARKING_CONDITION = "RECIPROCAL_END_MARKING_CONDITION";
        public static final String RECIPROCAL_END_ARRESTING_DEVICE_TYPE = "RECIPROCAL_END_ARRESTING_DEVICE_TYPE";
        public static final String RECIPROCAL_END_LATTITUDE_DEGREES = "RECIPROCAL_END_LATTITUDE_DEGREES";
        public static final String RECIPROCAL_END_LOGITUDE_DEGREES = "RECIPROCAL_END_LONGITUDE_DEGREES";
        public static final String RECIPROCAL_END_RUNWAY_ELEVATION = "RECIPROCAL_END_RUNWAY_ELEVATION";
        public static final String RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT = "RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT";
        public static final String RECIPROCAL_END_GLIDE_ANGLE = "RECIPROCAL_END_GLIDE_ANGLE";
        public static final String RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH = "RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH";
        public static final String RECIPROCAL_END_TDZ_ELEVATION = "RECIPROCAL_END_TDZ_ELEVATION";
        public static final String RECIPROCAL_END_VISUAL_GLIDE_SLOPE = "RECIPROCAL_END_VISUAL_GLIDE_SLOPE";
        public static final String RECIPROCAL_END_RVR_LOCATIONS = "RECIPROCAL_END_RVR_LOCATIONS";
        public static final String RECIPROCAL_END_APCH_LIGHT_SYSTEM = "RECIPROCAL_END_APCH_LIGHT_SYSTEM";
        public static final String RECIPROCAL_END_REIL_AVAILABLE = "RECIPROCAL_END_REIL_AVAILABLE";
        public static final String RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE = "RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE";
        public static final String RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE = "RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE";
        public static final String RECIPROCAL_END_CONTROLLING_OBJECT = "RECIPROCAL_END_CONTROLLING_OBJECT";
        public static final String RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED = "RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED";
        public static final String RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE = "RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE";
        public static final String RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT = "RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT";
        public static final String RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE = "RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE";
        public static final String RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET = "RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET";
        public static final String BASE_END_GRADIENT = "BASE_END_GRADIENT";
        public static final String BASE_END_GRADIENT_DIRECTION = "BASE_END_GRADIENT_DIRECTION";
        public static final String BASE_END_TORA = "BASE_END_TORA";
        public static final String BASE_END_TODA = "BASE_END_TODA";
        public static final String BASE_END_ASDA = "BASE_END_ASDA";
        public static final String BASE_END_LDA = "BASE_END_LDA";
        public static final String BASE_END_LAHSO_DISTANCE = "BASE_END_LAHSO_DISTANCE";
        public static final String BASE_END_LAHSO_RUNWAY = "BASE_END_LAHSO_RUNWAY";
        public static final String RECIPROCAL_END_GRADIENT = "RECIPROCAL_END_GRADIENT";
        public static final String RECIPROCAL_END_GRADIENT_DIRECTION = "RECIPROCAL_END_GRADIENT_DIRECTION";
        public static final String RECIPROCAL_END_TORA = "RECIPROCAL_END_TORA";
        public static final String RECIPROCAL_END_TODA = "RECIPROCAL_END_TODA";
        public static final String RECIPROCAL_END_ASDA = "RECIPROCAL_END_ASDA";
        public static final String RECIPROCAL_END_LDA = "RECIPROCAL_END_LDA";
        public static final String RECIPROCAL_END_LAHSO_DISTANCE = "RECIPROCAL_END_LAHSO_DISTANCE";
        public static final String RECIPROCAL_END_LAHSO_RUNWAY = "RECIPROCAL_END_LAHSO_RUNWAY";
    }

    public static final class Remarks implements BaseColumns {
        public static final String TABLE_NAME = "remarks";
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String REMARK_NAME = "REMARK_NAME";
        public static final String REMARK_TEXT = "REMARK_TEXT";
    }

    public static final class Attendance implements BaseColumns {
        public static final String TABLE_NAME = "attendance";
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String SEQUENCE_NUMBER = "SEQUENCE_NUMBER";
        public static final String ATTENDANCE_SCHEDULE = "ATTENDANCE_SCHEDULE";
    }

    public static final class Tower1 implements BaseColumns {
        public static final String TABLE_NAME = "tower1";
        public static final String FACILITY_ID = "FACILITY_ID";
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String FACILITY_TYPE = "FACILITY_TYPE";
        public static final String RADIO_CALL_TOWER = "RADIO_CALL_TOWER";
        public static final String RADIO_CALL_APCH = "RADIO_CALL_APCH";
        public static final String RADIO_CALL_DEP = "RADIO_CALL_DEP";
    }

    public static final class Tower3 implements BaseColumns {
        public static final String TABLE_NAME = "tower3";
        public static final String FACILITY_ID = "FACILITY_ID";
        public static final String MASTER_AIRPORT_FREQ = "MASTER_AIRPORT_FREQ";
        public static final String MASTER_AIRPORT_FREQ_USE = "MASTER_AIRPORT_FREQ_USE";
    }

    public static final class Tower6 implements BaseColumns {
        public static final String TABLE_NAME = "tower6";
        public static final String FACILITY_ID = "FACILITY_ID";
        public static final String ELEMENT_NUMBER = "ELEMENT_NUMBER";
        public static final String REMARK_TEXT = "REMARK_TEXT";
    }

    public static final class Tower7 implements BaseColumns {
        public static final String TABLE_NAME = "tower7";
        public static final String SATELLITE_AIRPORT_FREQ_USE = "SATELLITE_AIRPORT_FREQ_USE";
        public static final String SATELLITE_AIRPORT_SITE_NUMBER = "SATELLITE_AIRPORT_SITE_NUMBER";
        public static final String MASTER_AIRPORT_SITE_NUMBER = "MASTER_AIRPORT_SITE_NUMBER";
        public static final String SATELLITE_AIRPORT_FREQ = "SATELLITE_AIRPORT_FREQ";
    }

    public static final class Tower8 implements BaseColumns {
        public static final String TABLE_NAME = "tower8";
        public static final String FACILITY_ID = "FACILITY_ID";
        public static final String AIRSPACE_TYPES = "AIRSPACE_TYPES";
        public static final String AIRSPACE_HOURS = "AIRSPACE_HOURS";
    }

    public static final class Awos implements BaseColumns {
        public static final String TABLE_NAME = "awos";
        public static final String WX_SENSOR_IDENT = "WX_SENSOR_IDENT";
        public static final String WX_SENSOR_TYPE = "WX_SENSOR_TYPE";
        public static final String COMMISSIONING_STATUS = "COMMISSIONING_STATUS";
        public static final String STATION_LATTITUDE_DEGREES = "STATION_LATTITUDE_DEGREES";
        public static final String STATION_LONGITUDE_DEGREES = "STATION_LONGITUDE_DEGREES";
        public static final String STATION_FREQUENCY = "STATION_FREQUENCY";
        public static final String SECOND_STATION_FREQUENCY = "SECOND_STATION_FREQUENCY";
        public static final String STATION_PHONE_NUMBER = "STATION_PHONE_NUMBER";
        public static final String SITE_NUMBER = "SITE_NUMBER";
    }

    public static final class Nav1 implements BaseColumns {
        public static final String TABLE_NAME = "nav1";
        public static final String NAVAID_ID = "NAVAID_ID";
        public static final String NAVAID_TYPE = "NAVAID_TYPE";
        public static final String NAVAID_NAME = "NAVAID_NAME";
        public static final String ASSOC_CITY = "ASSOC_CITY";
        public static final String ASSOC_STATE = "ASSOC_STATE";
        public static final String PUBLIC_USE = "PUBLIC_USE";
        public static final String NAVAID_CLASS = "NAVAID_CLASS";
        public static final String OPERATING_HOURS = "OPERATING_HOURS";
        public static final String REF_LATTITUDE_DEGREES = "REF_LATTITUDE_DEGREES";
        public static final String REF_LONGITUDE_DEGREES = "REF_LONGITUDE_DEGREES";
        public static final String ELEVATION_MSL = "ELEVATION_MSL";
        public static final String MAGNETIC_VARIATION_DEGREES = "MAGNETIC_VARIATION_DEGREES";
        public static final String MAGNETIC_VARIATION_DIRECTION = "MAGNETIC_VARIATION_DIRECTION";
        public static final String MAGNETIC_VARIATION_YEAR = "MAGNETIC_VARIATION_YEAR";
        public static final String VOICE_FEATURE = "VOICE_FEATURE";
        public static final String POWER_OUTPUT = "POWER_OUTPUT";
        public static final String AUTOMATIC_VOICE_IDENT = "AUTOMATIC_VOICE_IDENT";
        public static final String TACAN_CHANNEL = "TACAN_CHANNEL";
        public static final String NAVAID_FREQUENCY = "NAVAID_FREQUENCY";
        public static final String PROTECTED_FREQUENCY_ALTITUDE = "PROTECTED_FREQUENCY_ALTITUDE";
    }

    public static final class Nav2 implements BaseColumns {
        public static final String TABLE_NAME = "nav2";
        public static final String NAVAID_ID = "NAVAID_ID";
        public static final String NAVAID_TYPE = "NAVAID_TYPE";
        public static final String REMARK_TEXT = "REMARK_TEXT";
    }

    public static final class Ils1 implements BaseColumns {
        public static final String TABLE_NAME = "ils1";
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String RUNWAY_ID = "RUNWAY_ID";
        public static final String ILS_TYPE = "ILS_TYPE";
        public static final String ILS_CATEGORY = "ILS_CATEGORY";
        public static final String ILS_MAGNETIC_BEARING = "ILS_MAGNETIC_BEARING";
        public static final String LOCALIZER_TYPE = "LOCALIZER_TYPE";
        public static final String LOCALIZER_ID = "LOCALIZER_ID";
        public static final String LOCALIZER_FREQUENCY = "LOCALIZER_FREQUENCY";
        public static final String LOCALIZER_COURSE_WIDTH = "LOCALIZER_COURSE_WIDTH";
        public static final String GLIDE_SLOPE_TYPE = "GLIDE_SLOPE_TYPE";
        public static final String GLIDE_SLOPE_ANGLE = "GLIDE_SLOPE_ANGLE";
        public static final String GLIDE_SLOPE_FREQUENCY = "GLIDE_SLOPE_FREQUENCY";
        public static final String INNER_MARKER_TYPE = "INNER_MARKER_TYPE";
        public static final String INNER_MARKER_DISTANCE = "INNER_MARKER_DISTANCE";
        public static final String MIDDLE_MARKER_TYPE = "MIDDLE_MARKER_TYPE";
        public static final String MIDDLE_MARKER_ID = "MIDDLE_MARKER_ID";
        public static final String MIDDLE_MARKER_NAME = "MIDDLE_MARKER_NAME";
        public static final String MIDDLE_MARKER_FREQUENCY = "MIDDLE_MARKER_FREQUENCY";
        public static final String MIDDLE_MARKER_DISTANCE = "MIDDLE_MARKER_DISTANCE";
        public static final String OUTER_MARKER_TYPE = "OUTER_MARKER_TYPE";
        public static final String OUTER_MARKER_ID = "OUTER_MARKER_ID";
        public static final String OUTER_MARKER_NAME = "OUTER_MARKER_NAME";
        public static final String OUTER_MARKER_FREQUENCY = "OUTER_MARKER_FREQUENCY";
        public static final String OUTER_MARKER_DISTANCE = "OUTER_MARKER_DISTANCE";
        public static final String BACKCOURSE_MARKER_AVAILABLE = "BACKCOURSE_MARKER_AVAILABLE";
    }

    public static final class Ils2 implements BaseColumns {
        public static final String TABLE_NAME = "ils2";
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String RUNWAY_ID = "RUNWAY_ID";
        public static final String ILS_TYPE = "ILS_TYPE";
        public static final String ILS_REMARKS = "ILS_REMARKS";
    }

    public static final class Aff3 implements BaseColumns {
        public static final String TABLE_NAME = "aff3";
        public static final String ARTCC_ID = "ARTCC_ID";
        public static final String SITE_LOCATION = "SITE_LOCATION";
        public static final String FACILITY_TYPE = "FACILITY_TYPE";
        public static final String SITE_FREQUENCY = "SITE_FREQUENCY";
        public static final String FREQ_ALTITUDE = "FREQ_ALTITUDE";
        public static final String FREQ_USAGE_NAME= "FREQ_USAGE_NAME";
        public static final String IFR_FACILITY_ID = "IFR_FACILITY_ID";
    }

    public static final class Com implements BaseColumns {
        public static final String TABLE_NAME = "com";
        public static final String COMM_OUTLET_ID = "COMM_OUTLET_ID";
        public static final String COMM_OUTLET_TYPE = "COMM_OUTLET_TYPE";
        public static final String ASSOC_NAVAID_ID = "ASSOC_NAVAID_ID";
        public static final String COMM_OUTLET_LATITUDE_DEGREES = "COMM_OUTLET_LATITUDE_DEGREES";
        public static final String COMM_OUTLET_LONGITUDE_DEGREES = "COMM_OUTLET_LONGITUDE_DEGREES";
        public static final String COMM_OUTLET_CALL = "COMM_OUTLET_CALL";
        public static final String COMM_OUTLET_FREQS = "COMM_OUTLET_FREQS";
        public static final String FSS_IDENT = "FSS_IDENT";
        public static final String FSS_NAME = "FSS_NAME";
    }

    public static final class Catalog implements BaseColumns {
        public static final String TABLE_NAME = "catalog";
        public static final String TYPE = "TYPE";
        public static final String DESCRIPTION = "DESCRIPTION";
        public static final String VERSION = "VERSION";
        public static final String START_DATE = "START_DATE";
        public static final String END_DATE = "END_DATE";
        public static final String DB_NAME = "DB_NAME";
        public static final String INSTALL_DATE = "INSTALL_DATE";
    }

    public static final class States implements BaseColumns {
        public static final String TABLE_NAME = "states";
        public static final String STATE_CODE = "STATE_CODE";
        public static final String STATE_NAME = "STATE_NAME";
    }

    public static final class Favorites implements BaseColumns {
        public static final String TABLE_NAME = "favorites";
        public static final String SITE_NUMBER = "SITE_NUMBER";
    }

    public static DatabaseManager instance( Context context ) {
        synchronized ( sLock ) {
            if ( sInstance == null ) {
                sInstance = new DatabaseManager( context.getApplicationContext() );
            }
            return sInstance;
        }
    }

    private DatabaseManager( Context context ) {
        mContext = context;
        mCatalogDbHelper = new CatalogDbOpenHelper( mContext );
        mUserDataDbHelper = new UserDataDbOpenHelper( mContext );
        mDatabases = new HashMap<String, SQLiteDatabase>();

        openDatabases();
    }

    public SQLiteDatabase getCatalogDb() {
        return mCatalogDbHelper.getWritableDatabase();
    }

    public SQLiteDatabase getUserDataDb() {
        return mUserDataDbHelper.getWritableDatabase();
    }

    public void close() {
        Log.i( TAG, "Closing databases..." );
        mCatalogDbHelper.close();
        closeDatabases();
    }

    public Cursor getAirportDetails( String siteNumber ) {
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
        if ( db == null ) {
            return null;
        }

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Airports.TABLE_NAME+" a LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
        Cursor c = builder.query( db, new String[] { "*" }, Airports.SITE_NUMBER+"=?",
                new String[] { siteNumber }, null, null, null, null );
        if ( !c.moveToFirst() ) {
            return null;
        }

        return c;
    }

    public Cursor getCurrentFromCatalog() {
        SQLiteDatabase catalogDb = getCatalogDb();
        String query = "SELECT *,"
            +" strftime('%s', "+Catalog.END_DATE+")-strftime('%s', 'now', 'localtime') as age"
            +" FROM "+Catalog.TABLE_NAME+" c1"
            +" WHERE "+Catalog.END_DATE+"=(SELECT max("+Catalog.END_DATE+")"
                +" FROM "+Catalog.TABLE_NAME+" c2 WHERE"
                +" c2."+Catalog.TYPE+"=c1."+Catalog.TYPE
                +" AND strftime('%s', c2."+Catalog.START_DATE
                +") <= strftime('%s', 'now', 'localtime') )";
        Log.i( TAG, query );
        return catalogDb.rawQuery( query, null );
    }

    public Cursor getAllFromCatalog() {
        SQLiteDatabase catalogDb = getCatalogDb();
        String query = "SELECT *,"
            +" strftime('%s', "+Catalog.END_DATE+")-strftime('%s', 'now', 'localtime') as age"
            +" FROM "+Catalog.TABLE_NAME;
        Log.i( TAG, query );
        return catalogDb.rawQuery( query, null );
    }

    protected ArrayList<String> getFavorites() {
        ArrayList<String> favorites = new ArrayList<String>();
        SQLiteDatabase db = getUserDataDb();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Favorites.TABLE_NAME );
        builder.setDistinct( true );
        Cursor c = builder.query( db, new String[] { Favorites.SITE_NUMBER }, 
                null, null, null, null, null );
        if ( c.moveToFirst() ) {
            // Build the list of favorites
            do {
                favorites.add( c.getString( c.getColumnIndex( Favorites.SITE_NUMBER ) ) );
            } while ( c.moveToNext() );
        }
        c.close();

        return favorites;
    }

    public Boolean isFavoriteAirport( String siteNumber ) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Favorites.TABLE_NAME );
        builder.setDistinct( true );
        SQLiteDatabase userDb = getUserDataDb();
        Cursor c = builder.query( userDb, new String[] { Favorites.SITE_NUMBER }, 
                Airports.SITE_NUMBER+"=? ",
                new String[] { siteNumber }, null, null, null, null );
        Boolean isFavorite = c.moveToFirst();
        c.close();
        return isFavorite;
    }

    public long addToFavorites( String siteNumber ) {
        SQLiteDatabase userDataDb = getUserDataDb();
        ContentValues values = new ContentValues();
        values.put( Favorites.SITE_NUMBER, siteNumber );
        return userDataDb.insert( Favorites.TABLE_NAME, null, values );
    }

    public int removeFromFavorites( String siteNumber ) {
        SQLiteDatabase userDataDb = getUserDataDb();
        return userDataDb.delete( Favorites.TABLE_NAME, 
                Airports.SITE_NUMBER+"=?", new String[] { siteNumber } );
    }

    private synchronized void openDatabases() {
        Cursor c = getCurrentFromCatalog();
        if ( c.moveToFirst() ) {
            do {
                String type = c.getString( c.getColumnIndex( Catalog.TYPE ) );
                File dbName = new File( DATABASE_DIR, 
                        c.getString( c.getColumnIndex( Catalog.DB_NAME ) ) );
                Log.i( TAG, "Opening db type="+type+", path="+dbName.getPath() );
                SQLiteDatabase db = null;
                try {
                    db = SQLiteDatabase.openDatabase( dbName.getPath(), null,
                            SQLiteDatabase.OPEN_READONLY );
                    mDatabases.put( type, db );
                } catch ( SQLiteException e ) {
                    Log.i( TAG, "Unable to open db type="+type );
                }
            } while ( c.moveToNext() );
        }
        c.close();
    }

    public SQLiteDatabase getDatabase( String type ) {
        if ( mDatabases.isEmpty() ) {
            // Open databases if not open already
            openDatabases();
        }

        return mDatabases.get( type );
    }

    public synchronized void closeDatabases() {
        Iterator<String> types  = mDatabases.keySet().iterator();
        while ( types.hasNext() ) {
            String type = types.next();
            Log.i( TAG, "Closing db for type="+type );
            mDatabases.get( type ).close();
        }
        mDatabases.clear();
    }

    public int insertCatalogEntry( ContentValues values ) {
        SQLiteDatabase db = mCatalogDbHelper.getWritableDatabase();

        long id = db.insert( Catalog.TABLE_NAME, null, values );
        if ( id >= 0 ) {
            Log.i( TAG, "Inserted catalog entry: _id="+id );
        } else {
            Log.i( TAG, "Failed to insert into catalog entry" );
        }

        db.close();

        return (int) id;
    }

    public class CatalogDbOpenHelper extends SQLiteOpenHelper {

        public CatalogDbOpenHelper( Context context ) {
            super( context, "catalog.db", null, 1 );
        }

        @Override
        public void onCreate( SQLiteDatabase db ) {
            Log.i( TAG, "Creating 'catalog' table" );
            db.execSQL( "CREATE TABLE "+Catalog.TABLE_NAME+" ( "
                    +Catalog._ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "
                    +Catalog.TYPE+" TEXT not null, "
                    +Catalog.DESCRIPTION+" TEXT not null, "
                    +Catalog.VERSION+" INTEGER not null, "
                    +Catalog.START_DATE+" TEXT not null, "
                    +Catalog.END_DATE+" TEXT not null, "
                    +Catalog.DB_NAME+" TEXT not null, "
                    +Catalog.INSTALL_DATE+" TEXT not null "
                    +")" );
        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
            Log.i( TAG, "Ugrading "+Catalog.TABLE_NAME+" db "+oldVersion+" -> "+newVersion );
            db.execSQL( "DROP TABLE "+Catalog.TABLE_NAME );
            onCreate( db );
        }

    }

    public class UserDataDbOpenHelper extends SQLiteOpenHelper {

        public UserDataDbOpenHelper( Context context ) {
            super( context, "userdata.db", null, 1 );
        }

        @Override
        public void onCreate( SQLiteDatabase db ) {
            Log.i( TAG, "Creating 'favorites' table" );
            db.execSQL( "CREATE TABLE "+Favorites.TABLE_NAME+" ( "
                    +Favorites.SITE_NUMBER+" TEXT PRIMARY_KEY "
                    +")" );
        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
            Log.i( TAG, "Ugrading "+Favorites.TABLE_NAME+" db "+oldVersion+" -> "+newVersion );
            db.execSQL( "DROP TABLE "+Favorites.TABLE_NAME );
            onCreate( db );
        }

    }

}
