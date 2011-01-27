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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseManager {
    public static final String TAG = "AirportsDatabase";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "fadds.db";

    private final FaddsDbOpenHelper mFaddsDbHelper;
    private final CatalogDbOpenHelper mCatalogDbHelper;
    private final Context mContext;

    public DatabaseManager( Context context ) {
        mContext = context;
        mFaddsDbHelper = new FaddsDbOpenHelper( mContext );
        mCatalogDbHelper = new CatalogDbOpenHelper( mContext );
    }

    public SQLiteDatabase getFaddsDatabase() {
        return mFaddsDbHelper.getReadableDatabase();
    }

    public void closeFaddsDatabase() {
        mFaddsDbHelper.close();
    }

    public SQLiteDatabase getCatalogDatabase() {
        return mCatalogDbHelper.getWritableDatabase();
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
    }

    public static final class Catalog implements BaseColumns {
        public static final String TABLE_NAME = "catalog";
        public static final String TYPE = "TYPE";
        public static final String DESCRIPTION = "DESCRIPTION";
        public static final String VERSION = "VERSION";
        public static final String START_DATE = "START_DATE";
        public static final String END_DATE = "END_DATE";
        public static final String DB_NAME = "DB_NAME";
    }

    public int insertCatalogEntry( ContentValues values ) {
        SQLiteDatabase db = mCatalogDbHelper.getWritableDatabase();
        long id = db.insert( Catalog.TABLE_NAME, null, values );

        if ( id >= 0 ) {
            Log.i( TAG, "Inserted catalog: _id="+id );
        } else {
            Log.i( TAG, "Failed to insert into catalog" );
        }

        return (int) id;
    }

    public class FaddsDbOpenHelper extends SQLiteOpenHelper {
        public FaddsDbOpenHelper( Context context ) {
            super( context, "fadds.db", null, 1 );
        }

        @Override
        public void onCreate( SQLiteDatabase db ) {
        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        }
    }

    public class CatalogDbOpenHelper extends SQLiteOpenHelper {
        public CatalogDbOpenHelper( Context context ) {
            super( context, "catalog.db", null, 1 );
        }

        @Override
        public void onCreate( SQLiteDatabase db ) {
            Log.i( TAG, "Creating Catalog database" );
            db.execSQL( "CREATE TABLE "+Catalog.TABLE_NAME+" ("
                    +Catalog._ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "
                    +Catalog.TYPE+" TEXT not null, "
                    +Catalog.DESCRIPTION+" TEXT not null, "
                    +Catalog.VERSION+" INTEGER not null, "
                    +Catalog.START_DATE+" TEXT not null, "
                    +Catalog.END_DATE+" TEXT not null, "
                    +Catalog.DB_NAME+" TEXT not null)" );
        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        }
    }
}
