/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.donate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.format.Time;

import com.nadmm.airports.billing.Consts.PurchaseState;

public class DonateDatabase {

    private static final String DB_NAME = "donations.db";
    private static final int DB_VERSION = 2;

    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDb;

    private static final String[] sDonationsColumns = {
        Donations.PRODUCT_ID,
        Donations.PURCHASE_TIME,
    };

    public DonateDatabase( Context context ) {
        mDatabaseHelper = new DatabaseHelper( context );
        mDb = mDatabaseHelper.getWritableDatabase();
    }

    public void close() {
        mDatabaseHelper.close();
    }

    public class Donations implements BaseColumns {
        public static final String TABLE_NAME = "donations";
        public static final String PRODUCT_ID = "PRODUCT_ID";
        public static final String PURCHASE_TIME = "PURCHASE_TIME";
    }

    public void updateDonation( String orderId, String productId, PurchaseState state, long time ) {
        if ( state == PurchaseState.PURCHASED ) {
            ContentValues values = new ContentValues();
            values.put( Donations._ID, orderId );
            values.put( Donations.PRODUCT_ID, productId );
            Time purchasetime = new Time();
            purchasetime.set( time );
            values.put( Donations.PURCHASE_TIME, purchasetime.format3339( false ) );
            mDb.replace( Donations.TABLE_NAME, null, values );
        } else if ( state == PurchaseState.REFUNDED ) {
            mDb.delete( Donations.TABLE_NAME, Donations._ID+"=?", new String[] { orderId } );
        } else if ( state == PurchaseState.CANCELED ) {
            mDb.delete( Donations.TABLE_NAME, Donations._ID+"=?", new String[] { orderId } );
        }
    }

    public Cursor queryAlldonations() {
        return mDb.query( Donations.TABLE_NAME, sDonationsColumns, null, null, null, null, null );
    }

    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper( Context context ) {
            super( context, DB_NAME, null, DB_VERSION );
        }

        @Override
        public void onCreate( SQLiteDatabase db ) {
            db.execSQL( "CREATE TABLE "+Donations.TABLE_NAME+" ("
                    +Donations._ID+" TEXT PRIMARY KEY, "
                    +Donations.PRODUCT_ID+" TEXT, "
                    +Donations.PURCHASE_TIME+" TEXT)" );
        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
            db.execSQL( "DROP TABLE "+Donations.TABLE_NAME );
            onCreate( db );
        }

    }

}
