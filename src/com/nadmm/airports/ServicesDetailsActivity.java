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

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.States;

public class ServicesDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        ServicesDetailsTask task = new ServicesDetailsTask();
        task.execute( siteNumber );
    }

    private final class ServicesDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
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
            View view = mInflater.inflate( R.layout.services_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.services_top_layout );

            // Title
            Cursor apt = result[ 0 ];
            showAirportTitle( mMainLayout, apt );

            showAirportServices( result );
            showFaaServices( result );
        }

    }

    protected void showAirportServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                R.id.detail_airport_services_layout );
        String otherServices = DataUtils.decodeServices(
                apt.getString( apt.getColumnIndex( Airports.OTHER_SERVICES ) ) );
        otherServices += ","+DataUtils.decodeStorage( 
                apt.getString( apt.getColumnIndex( Airports.STORAGE_FACILITY ) ) );
        String bottledOxygen = apt.getString( apt.getColumnIndex(
                Airports.BOTTLED_O2_AVAILABLE ) );
        if ( bottledOxygen.equals( "Y" ) ) {
            otherServices += ","+"Bottled Oxygen";
        }
        String bulkOxygen = apt.getString( apt.getColumnIndex(
                Airports.BULK_O2_AVAILABLE ) );
        if ( bulkOxygen.equals( "Y" ) ) {
            otherServices += ","+"Bulk Oxygen";
        }
        String[] services = otherServices.split( ",\\s*" );
        for ( String service : services ) {
            if ( service.length() > 0 ) {
                addBulletRow( layout, service );
            }
        }
    }

    protected void showFaaServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById(
                R.id.detail_faa_services_layout );
        String artccId = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_ID ) );
        String artccName = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_NAME ) );
        addRow( layout, "ARTCC", artccId+" ("+artccName+")" );
        addSeparator( layout );
        String fssId = apt.getString( apt.getColumnIndex( Airports.FSS_ID ) );
        String fssName = apt.getString( apt.getColumnIndex( Airports.FSS_NAME ) );
        addRow( layout, "Flight Service", fssId+" ("+fssName+")" );
        addSeparator( layout );
        String fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_LOCAL_PHONE ) );
        if ( fssPhone.length() == 0 ) {
            fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_TOLLFREE_PHONE ) );
        }
        addPhoneRow( layout, "FSS Phone", fssPhone );
        addSeparator( layout );
        String notamFacility = apt.getString( apt.getColumnIndex( Airports.NOTAM_FACILITY_ID ) );
        addRow( layout, "NOTAM Facility", notamFacility );
        addSeparator( layout );
        String notamD = apt.getString( apt.getColumnIndex( Airports.NOTAM_D_AVAILABLE ) );
        addRow( layout, "NOTAM D Available", notamD.equals( "Y" )? "Yes" : "No" );
    }

    protected void addRow( TableLayout table, String label, String value ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tv = new TextView( this );
        tv.setText( label );
        tv.setSingleLine();
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 2, 2 );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setText( value );
        tv.setMarqueeRepeatLimit( -1 );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 2, 2, 2 );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addPhoneRow( TableLayout table, String label, final String phone ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tv = new TextView( this );
        tv.setText( label );
        tv.setSingleLine();
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 2, 2 );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setMarqueeRepeatLimit( -1 );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 2, 2, 2 );
        tv.setText( phone );
        makeClickToCall( tv );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addBulletRow( LinearLayout layout, String service ) {
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 4, 2, 4 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 4, 12, 4 );
        tv.setText( service );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( LinearLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.FILL_PARENT, 1 ) );
    }

}
