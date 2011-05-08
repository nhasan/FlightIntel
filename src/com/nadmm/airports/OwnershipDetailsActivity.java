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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;

public class OwnershipDetailsActivity extends Activity {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        AirportDetailsTask task = new AirportDetailsTask();
        task.execute( siteNumber );
    }

    private final class AirportDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            DatabaseManager dbManager = DatabaseManager.instance( getApplicationContext() );
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 2 ];

            cursors[ 0 ] = dbManager.getAirportDetails( siteNumber );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            cursors[ 1 ] = builder.query( db, new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND "+Remarks.REMARK_NAME+" in ('A11', 'A12', 'A13', 'A14', 'A15', 'A16')",
                    new String[] { siteNumber }, null, null, null, null );

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            if ( result == null ) {
                // TODO: Show an error here
                return;
            }

            View view = mInflater.inflate( R.layout.ownership_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.ownership_top_layout );

            // Title
            Cursor apt = result[ 0 ];
            GuiUtils.showAirportTitle( mMainLayout, apt );

            showOwnershipType( result );
            showOwnerInfo( result );
            showManagerInfo( result );
            showRemarks( result );
        }

    }

    protected void showOwnershipType( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                R.id.detail_ownership_type_layout );
        String ownership = DataUtils.decodeOwnershipType(
                apt.getString( apt.getColumnIndex( Airports.OWNERSHIP_TYPE ) ) );
        String use = DataUtils.decodeFacilityUse(
                apt.getString( apt.getColumnIndex( Airports.FACILITY_USE ) ) );
        addRow( layout, ownership+" / "+use );
    }

    protected void showOwnerInfo( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String text;
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.detail_owner_layout );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_NAME ) );
        addRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_ADDRESS ) );
        addRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_CITY_STATE_ZIP ) );
        addRow( layout, text );
        layout = (LinearLayout) mMainLayout.findViewById( R.id.detail_owner_phone_layout );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_PHONE ) );
        if ( text.length() > 0 ) {
            addPhoneRow( layout, text );
        } else {
            layout.setVisibility( View.GONE );
            mMainLayout.findViewById( R.id.detail_owner_phone_label ).setVisibility( View.GONE );
        }
    }

    protected void showManagerInfo( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String text;
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.detail_manager_layout );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_NAME ) );
        addRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_ADDRESS ) );
        addRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_CITY_STATE_ZIP ) );
        addRow( layout, text );
        layout = (LinearLayout) mMainLayout.findViewById( R.id.detail_manager_phone_layout );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_PHONE ) );
        if ( text.length() > 0 ) {
            addPhoneRow( layout, text );
        } else {
            layout.setVisibility( View.GONE );
            mMainLayout.findViewById( R.id.detail_manager_phone_label ).setVisibility( View.GONE );
        }
    }

    protected void showRemarks( Cursor[] result ) {
        Cursor rmk = result[ 1 ];
        if ( !rmk.moveToFirst() ) {
            return;
        }
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.detail_remarks_layout );
        layout.setVisibility( View.VISIBLE );
        do {
            String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
            addRemarkRow( layout, remark );
        } while ( rmk.moveToNext() );
    }

    protected void addRow( LinearLayout layout, String text ) {
        TextView tv = new TextView( this );
        tv.setText( text );
        tv.setPadding( 0, 1, 0, 1 );
        layout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addPhoneRow( LinearLayout layout, String text ) {
        TextView tv = new TextView( this );
        tv.setPadding( 0, 1, 0, 1 );
        tv.setText( text );
        Linkify.addLinks( tv, Linkify.PHONE_NUMBERS );
        layout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRemarkRow( LinearLayout layout, String remark ) {
        int index = remark.indexOf( ' ' );
        if ( index != -1 ) {
            while ( remark.charAt( index ) == ' ' ) {
                ++index;
            }
            remark = remark.substring( index );
        }
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( remark );
        Linkify.addLinks( tv, Linkify.PHONE_NUMBERS );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

}
