/*
 * Airports for Android
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class AirportsDatabase {
    public static final String TAG = "AirportsDatabase";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "fadds.db";

    private final DbOpenHelper mDbOpenHelper;
    private final Context mContext;

    public AirportsDatabase( Context context ) {
        mContext = context;
        mDbOpenHelper = new DbOpenHelper( mContext );
    }

    public SQLiteDatabase getWritableDatabase() {
    	return mDbOpenHelper.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
    	return mDbOpenHelper.getReadableDatabase();
    }

    public static final class Airports implements BaseColumns {
        public static final String TABLE_NAME = "airport";
        // Fields for airport table
        public static final String SITE_NUMBER = "SITE_NUMBER";
        public static final String FACILITY_TYPE = "FACILITY_TYPE";
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
        public static final String REF_LATTITUDE_SECONDS = "REF_LATTITUDE_SECONDS";
        public static final String REF_LATTITUDE_DECLINATION = "REF_LATTITUDE_DECLINATION";
        public static final String REF_LONGITUDE_SECONDS = "REF_LONGITUDE_SECONDS";
        public static final String REF_LONGITUDE_DECLINATION = "REF_LONGITUDE_DECLINATION";
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
        public static final String BASED_SINGLE_ENGINE = "BASED_SINGLE_ENGINE";
        public static final String BASED_MULTI_ENGINE = "BASED_MULTI_ENGINE";
        public static final String BASED_JET_ENGINE = "BASED_JET_ENGINE";
        public static final String BASED_HELICOPTER = "BASED_HELICOPTER";
        public static final String BASED_GLIDER = "BASED_GLIDER";
        public static final String BASED_MILITARY = "BASED_MILITARY";
        public static final String BASED_ULTRA_LIGHT = "BASED_ULTRA_LIGHT";
        public static final String OPS_ANNUAL_COMMERCIAL = "OPS_ANNUAL_COMMERCIAL";
        public static final String OPS_ANNUAL_COMMUTER = "OPS_ANNUAL_COMMUTER";
        public static final String OPS_ANNUAL_AIRTAXI = "OPS_ANNUAL_AIRTAXI";
        public static final String OPS_ANNUAL_GA_LOCAL = "OPS_ANNUAL_GA_LOCAL";
        public static final String OPS_ANNUAL_GA_OTHER = "OPS_ANNUAL_GA_OTHER";
        public static final String OPS_ANNUAL_MILITARY = "OPS_ANNUAL_MILITARY";
        public static final String OPS_ANNUAL_DATE = "OPS_ANNUAL_DATE";
        public static final String STORAGE_FACILITY = "STORAGE_FACILITY";
        public static final String OTHER_SERVICES = "OTHER_SERVICES";
        public static final String WIND_INDICATOR = "IND_INDICATOR";
        public static final String ICAO_CODE = "ICAO_CODE";
    
        private static final String CREATE_TABLE = 
        "CREATE TABLE "+TABLE_NAME+" ("
                +_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "
                +SITE_NUMBER+" TEXT,"
                +FACILITY_TYPE+" TEXT,"
                +FAA_CODE+" TEXT,"
                +EFFECTIVE_DATE+" TEXT,"
                +REGION_CODE+" TEXT,"
                +ASSOC_STATE+" TEXT,"
                +ASSOC_COUNTY+" TEXT,"
                +ASSOC_CITY+" TEXT,"
                +FACILITY_NAME+" TEXT,"
                +OWNERSHIP_TYPE+" TEXT,"
                +FACILITY_USE+" TEXT,"
                +OWNER_NAME+" TEXT,"
                +OWNER_ADDRESS+" TEXT,"
                +OWNER_CITY_STATE_ZIP+" TEXT,"
                +OWNER_PHONE+" TEXT,"
                +MANAGER_NAME+" TEXT,"
                +MANAGER_ADDRESS+" TEXT,"
                +MANAGER_CITY_STATE_ZIP+" TEXT,"
                +MANAGER_PHONE+" TEXT,"
                +REF_LATTITUDE_SECONDS+" REAL,"
                +REF_LATTITUDE_DECLINATION+" TEXT,"
                +REF_LONGITUDE_SECONDS+" REAL,"
                +REF_LONGITUDE_DECLINATION+" TEXT,"
                +REF_METHOD+" TEXT,"
                +ELEVATION_MSL+" INTEGER,"
                +ELEVATION_METHOD+" TEXT,"
                +MAGNETIC_VARIATION_DEGREES+" INTEGER,"
                +MAGNETIC_VARIATION_YEAR+" TEXT,"
                +PATTERN_ALTITUDE_AGL+" INTEGER,"
                +SECTIONAL_CHART+" TEXT,"
                +DISTANCE_FROM_CITY_NM+" INTEGER,"
                +DIRECTION_FROM_CITY+" TEXT,"
                +BOUNDARY_ARTCC_ID+" TEXT,"
                +BOUNDARY_ARTCC_NAME+" TEXT,"
                +FSS_ON_SITE+" TEXT,"
                +FSS_ID+" TEXT,"
                +FSS_NAME+" TEXT,"
                +FSS_LOCAL_PHONE+" TEXT,"
                +FSS_TOLLFREE_PHONE+" TEXT,"
                +NOTAM_FACILITY_ID+" TEXT,"
                +NOTAM_D_AVAILABLE+" TEXT,"
                +ACTIVATION_DATE+" TEXT,"
                +STATUS_CODE+" TEXT,"
                +INTL_ENTRY_AIRPORT+" TEXT,"
                +CUSTOMS_LANDING_RIGHTS_AIRPORT+" TEXT,"
                +CIVIL_MILITARY_JOINT_USE+" TEXT,"
                +MILITARY_LANDING_RIGHTS+" TEXT,"
                +FUEL_TYPES+" TEXT,"
                +AIRFRAME_REPAIR_SERVICE+" TEXT,"
                +POWER_PLANT_REPAIR_SERVICE+" TEXT,"
                +BOTTLED_O2_AVAILABLE+" TEXT,"
                +BULK_O2_AVAILABLE+" TEXT,"
                +LIGHTING_SCHEDULE+" TEXT,"
                +TOWER_ON_SITE+" TEXT,"
                +UNICOM_FREQS+" TEXT,"
                +CTAF_FREQ+" TEXT,"
                +SEGMENTED_CIRCLE+" TEXT,"
                +BEACON_COLOR+" TEXT,"
                +LANDING_FEE+" TEXT,"
                +BASED_SINGLE_ENGINE+" INTEGER,"
                +BASED_MULTI_ENGINE+" INTEGER,"
                +BASED_JET_ENGINE+" INTEGER,"
                +BASED_HELICOPTER+" INTEGER,"
                +BASED_GLIDER+" INTEGER,"
                +BASED_MILITARY+" INTEGER,"
                +BASED_ULTRA_LIGHT+" INTEGER,"
                +OPS_ANNUAL_COMMERCIAL+" INTEGER,"
                +OPS_ANNUAL_COMMUTER+" INTEGER,"
                +OPS_ANNUAL_AIRTAXI+" INTEGER,"
                +OPS_ANNUAL_GA_LOCAL+" INTEGER,"
                +OPS_ANNUAL_GA_OTHER+" INTEGER,"
                +OPS_ANNUAL_MILITARY+" INTEGER,"
                +OPS_ANNUAL_DATE+" TEXT,"
                +STORAGE_FACILITY+" TEXT,"
                +OTHER_SERVICES+" TEXT,"
                +WIND_INDICATOR+" TEXT,"
                +ICAO_CODE+" TEXT"
                +" )";
    };

    public class DbOpenHelper extends SQLiteOpenHelper {
        public DbOpenHelper( Context context ) {
            super( context, DATABASE_NAME, null, DATABASE_VERSION );
        }

        @Override
        public void onCreate( SQLiteDatabase db ) {
        	db.execSQL( "DROP TABLE IF EXISTS "+Airports.TABLE_NAME );
            db.execSQL( Airports.CREATE_TABLE );
            db.execSQL( "CREATE INDEX faacode_idx ON "+Airports.TABLE_NAME+" ("
            		+Airports.FAA_CODE+")" );
            db.execSQL( "CREATE INDEX icaocode_idx ON "+Airports.TABLE_NAME+" ("
            		+Airports.ICAO_CODE+")" );
            db.execSQL( "CREATE INDEX name_idx ON "+Airports.TABLE_NAME+" ("
            		+Airports.FACILITY_NAME+")" );
            db.execSQL( "CREATE INDEX city_idx ON "+Airports.TABLE_NAME+" ("
            		+Airports.ASSOC_CITY+")" );
        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        }
    };

    public void parseRecord( byte[] line )
    {
        String recType = new String( line, 0, 3 );
        
        if ( recType.equals( "APT" ) )
        {
            if ( line.length != 1263 )
            {
                Log.e( TAG, "Invalid APT record. length="+line.length );
                return;
            }

            insertAirport( line );
        }
    }

    public void insertAirport( byte[] line )
    {
        int offset = 3;
        ContentValues values = new ContentValues();

        values.put( Airports.SITE_NUMBER, new String( line, offset, 11 ) );
        offset += 11;
        values.put( Airports.FACILITY_TYPE, new String( line, offset, 13 ) );
        offset += 13;
        values.put( Airports.FAA_CODE, new String( line, offset, 4 ) );
        offset += 4;
        values.put( Airports.EFFECTIVE_DATE, new String( line, offset, 10 ) );
        offset += 10;
        values.put( Airports.REGION_CODE, new String( line, offset, 3 ) );
        offset += 7;
        values.put( Airports.ASSOC_STATE, new String( line, offset, 2 ) );
        offset += 22;
        values.put( Airports.ASSOC_COUNTY, new String( line, offset, 21 ) );
        offset += 23;
        values.put( Airports.ASSOC_CITY, new String( line, offset, 40 ) );
        offset += 40;
        values.put( Airports.FACILITY_NAME, new String( line, offset, 42 ) );
        offset += 42;
        values.put( Airports.OWNERSHIP_TYPE, new String( line, offset, 2 ) );
        offset += 2;
        values.put( Airports.FACILITY_USE, new String( line, offset, 2 ) );
        offset += 2;
        values.put( Airports.OWNER_NAME, new String( line, offset, 35 ) );
        offset += 35;
        values.put( Airports.OWNER_ADDRESS, new String( line, offset, 72 ) );
        offset += 72;
        values.put( Airports.OWNER_CITY_STATE_ZIP, new String( line, offset, 45 ) );
        offset += 45;
        values.put( Airports.OWNER_PHONE, new String( line, offset, 16 ).trim() );
        offset += 16;
        values.put( Airports.MANAGER_NAME, new String( line, offset, 35 ) );
        offset += 35;
        values.put( Airports.MANAGER_ADDRESS, new String( line, offset, 72 ) );
        offset += 72;
        values.put( Airports.MANAGER_CITY_STATE_ZIP, new String( line, offset, 45 ) );
        offset += 45;
        values.put( Airports.MANAGER_PHONE, new String( line, offset, 16 ).trim() );
        offset += 31;
        values.put( Airports.REF_LATTITUDE_SECONDS, new String( line, offset, 11 ) );
        offset += 11;
        values.put( Airports.REF_LATTITUDE_DECLINATION, new String( line, offset, 1 ) );
        offset += 16;
        values.put( Airports.REF_LONGITUDE_SECONDS, new String( line, offset, 11 ) );
        offset += 11;
        values.put( Airports.REF_LONGITUDE_DECLINATION, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.REF_METHOD, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.ELEVATION_MSL, new String( line, offset, 5 ) );
        offset += 5;
        values.put( Airports.ELEVATION_METHOD, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.MAGNETIC_VARIATION_DEGREES, new String( line, offset, 2 ) );
        offset += 2;
        values.put( Airports.MAGNETIC_VARIATION_DIRECTION, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.MAGNETIC_VARIATION_YEAR, new String( line, offset, 4 ) );
        offset += 4;
        values.put( Airports.PATTERN_ALTITUDE_AGL, new String( line, offset, 4 ) );
        offset += 4;
        values.put( Airports.SECTIONAL_CHART, new String( line, offset, 30 ) );
        offset += 30;
        values.put( Airports.DISTANCE_FROM_CITY_NM, new String( line, offset, 2 ) );
        offset += 2;
        values.put( Airports.DIRECTION_FROM_CITY, new String( line, offset, 3 ) );
        offset += 8;
        values.put( Airports.BOUNDARY_ARTCC_ID, new String( line, offset, 4 ) );
        offset += 7;
        values.put( Airports.BOUNDARY_ARTCC_NAME, new String( line, offset, 30 ) );
        offset += 30;
        values.put( Airports.RESPONSIBLE_ARTCC_ID, new String( line, offset, 4 ) );
        offset += 7;
        values.put( Airports.RESPONSIBLE_ARTCC_NAME, new String( line, offset, 30 ) );
        offset += 30;
        values.put( Airports.FSS_ON_SITE, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.FSS_ID, new String( line, offset, 4 ) );
        offset += 4;
        values.put( Airports.FSS_NAME, new String( line, offset, 30 ) );
        offset += 30;
        values.put( Airports.FSS_LOCAL_PHONE, new String( line, offset, 16 ) );
        offset += 16;
        values.put( Airports.FSS_TOLLFREE_PHONE, new String( line, offset, 16 ) );
        offset += 66;
        values.put( Airports.NOTAM_FACILITY_ID, new String( line, offset, 4 ) );
        offset += 4;
        values.put( Airports.NOTAM_D_AVAILABLE, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.ACTIVATION_DATE, new String( line, offset, 7 ) );
        offset += 7;
        values.put( Airports.STATUS_CODE, new String( line, offset, 2 ) );
        offset += 37;
        values.put( Airports.INTL_ENTRY_AIRPORT, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.CUSTOMS_LANDING_RIGHTS_AIRPORT, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.CIVIL_MILITARY_JOINT_USE, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.MILITARY_LANDING_RIGHTS, new String( line, offset, 1 ) );
        offset += 44;
        values.put( Airports.FUEL_TYPES, new String( line, offset, 40 ) );
        offset += 40;
        values.put( Airports.AIRFRAME_REPAIR_SERVICE, new String( line, offset, 5 ) );
        offset += 5;
        values.put( Airports.POWER_PLANT_REPAIR_SERVICE, new String( line, offset, 5 ) );
        offset += 5;
        values.put( Airports.BOTTLED_O2_AVAILABLE, new String( line, offset, 8 ) );
        offset += 8;
        values.put( Airports.BULK_O2_AVAILABLE, new String( line, offset, 8 ) );
        offset += 8;
        values.put( Airports.LIGHTING_SCHEDULE, new String( line, offset, 9 ) );
        offset += 9;
        values.put( Airports.TOWER_ON_SITE, new String( line, offset, 1 ) );
        offset += 1;
        values.put( Airports.UNICOM_FREQS, new String( line, offset, 42 ) );
        offset += 42;
        values.put( Airports.CTAF_FREQ, new String( line, offset, 7 ) );
        offset += 7;
        values.put( Airports.SEGMENTED_CIRCLE, new String( line, offset, 4 ) );
        offset += 4;
        values.put( Airports.BEACON_COLOR, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.LANDING_FEE, new String( line, offset, 1 ) );
        offset += 2;
        values.put( Airports.BASED_SINGLE_ENGINE, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.BASED_MULTI_ENGINE, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.BASED_JET_ENGINE, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.BASED_HELICOPTER, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.BASED_GLIDER, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.BASED_MILITARY, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.BASED_ULTRA_LIGHT, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.OPS_ANNUAL_COMMERCIAL, new String( line, offset, 6 ) );
        offset += 6;
        values.put( Airports.OPS_ANNUAL_COMMUTER, new String( line, offset, 6 ) );
        offset += 6;
        values.put( Airports.OPS_ANNUAL_AIRTAXI, new String( line, offset, 6 ) );
        offset += 6;
        values.put( Airports.OPS_ANNUAL_GA_LOCAL, new String( line, offset, 6 ) );
        offset += 6;
        values.put( Airports.OPS_ANNUAL_GA_OTHER, new String( line, offset, 6 ) );
        offset += 6;
        values.put( Airports.OPS_ANNUAL_MILITARY, new String( line, offset, 6 ) );
        offset += 6;
        values.put( Airports.OPS_ANNUAL_DATE, new String( line, offset, 10 ) );
        offset += 63;
        values.put( Airports.STORAGE_FACILITY, new String( line, offset, 12 ) );
        offset += 12;
        values.put( Airports.OTHER_SERVICES, new String( line, offset, 71 ) );
        offset += 71;
        values.put( Airports.WIND_INDICATOR, new String( line, offset, 3 ) );
        offset += 3;
        values.put( Airports.ICAO_CODE, new String( line, offset, 7 ) );
        offset += 7;

    	Log.v( "AIRPORT", "offset="+offset );

    	Set<Map.Entry<String, Object>> entrySet = values.valueSet();
        Iterator<Map.Entry<String, Object>> it = entrySet.iterator();
        while ( it.hasNext() ) {
        	Map.Entry<String, Object> entry = it.next();
        	Log.v( "AIRPORT", entry.getKey()+"="+entry.getValue() );
        }
    }
};
