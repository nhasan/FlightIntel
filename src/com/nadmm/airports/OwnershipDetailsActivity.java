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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.States;

public class OwnershipDetailsActivity extends Activity {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        AirportDetailsTask task = new AirportDetailsTask();
        task.execute( siteNumber );
    }

    private final class AirportDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            DatabaseManager dbManager = DatabaseManager.instance( getApplicationContext() );
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 2 ];

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME+" a INNER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            Cursor c = builder.query( db, new String[] { "*" }, Airports.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            if ( !c.moveToFirst() ) {
                return null;
            }
            cursors[ 0 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            c = builder.query( db, new String[] { "*"  }, Airports.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 1 ] = c;

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );
            if ( result == null ) {
                // TODO: Show an error here
                return;
            }

            View view = mInflater.inflate( R.layout.ownership_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.airport_ownership_layout );

            // Title
            Cursor apt = result[ 0 ];
            GuiUtils.showAirportTitle( mMainLayout, apt );

            showOwnershipType( result );
            showOwnerInfo( result );
            showManagerInfo( result );
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
        String text;
        Cursor apt = result[ 0 ];
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
        String text;
        Cursor apt = result[ 0 ];
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

    protected void addRow( LinearLayout layout, String text ) {
        TextView tv = new TextView( this );
        tv.setText( text );
        tv.setPadding( 0, 1, 0, 1 );
        layout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addPhoneRow( LinearLayout layout, String text ) {
        TextView tv = new TextView( this );
        tv.setText( text );
        tv.setPadding( 0, 1, 0, 1 );
        Linkify.addLinks( tv, Linkify.PHONE_NUMBERS );
        layout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }
}
