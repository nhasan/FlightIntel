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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;

public class AirportDetailsActivity extends Activity {

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
            Cursor[] cursors = new Cursor[ 1 ];
            DatabaseManager dbManager = DatabaseManager.instance();
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME+" a INNER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            Cursor c = builder.query( db, new String[] { "*"  }, Airports.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            if ( !c.moveToFirst() ) {
                return null;
            }
            cursors[ 0 ] = c;

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );
            if ( result == null ) {
                // TODO: Show an error here
                return;
            }

            View view = mInflater.inflate( R.layout.airport_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.detail_view_layout );

            Cursor apt = result[ 0 ];
            TextView tv;

            // Title
            tv = (TextView) mMainLayout.findViewById( R.id.airport_title );
            String code = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( code == null || code.length() == 0 ) {
                code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            }
            String name = apt.getString( apt.getColumnIndex( Airports.FACILITY_NAME ) );
            String title = code + " - " + name;
            tv.setText( title );
            tv = (TextView) mMainLayout.findViewById( R.id.airport_info );
            String type = apt.getString( apt.getColumnIndex( Airports.FACILITY_TYPE ) );
            String city = apt.getString( apt.getColumnIndex( Airports.ASSOC_CITY ) );
            String state = apt.getString( apt.getColumnIndex( States.STATE_NAME ) );
            String info = type+", "+city+", "+state;
            tv.setText( info );
            tv = (TextView) mMainLayout.findViewById( R.id.airport_info2 );
            int distance = apt.getInt( apt.getColumnIndex( Airports.DISTANCE_FROM_CITY_NM ) );
            String dir = apt.getString( apt.getColumnIndex( Airports.DIRECTION_FROM_CITY ) );
            String status = apt.getString( apt.getColumnIndex( Airports.STATUS_CODE ) );
            tv.setText( DataUtils.decodeStatus( status )+", "
                    +String.valueOf( distance )+" miles "+dir+" of city center" );
            tv = (TextView) mMainLayout.findViewById( R.id.airport_info3 );
            int elev_msl = apt.getInt( apt.getColumnIndex( Airports.ELEVATION_MSL ) );
            String info2 = String.valueOf( elev_msl )+"' MSL elevation, ";
            int tpa_agl = apt.getInt( apt.getColumnIndex( Airports.PATTERN_ALTITUDE_AGL ) );
            String est = "";
            if ( tpa_agl == 0 ) {
                tpa_agl = 1000;
                est = " (est.)";
            }
            info2 += String.valueOf( elev_msl+tpa_agl)+"' MSL TPA"+est;
            tv.setText( info2 );

            // Frequency section
            TableLayout freqLayout = (TableLayout) mMainLayout.findViewById( 
                    R.id.detail_freq_layout );
            String ctaf = apt.getString( apt.getColumnIndex( Airports.CTAF_FREQ ) );
            addRow( freqLayout, "CTAF", ctaf );
            addSeparator( freqLayout );
            String unicom = apt.getString( apt.getColumnIndex( Airports.UNICOM_FREQS ) );
            addRow( freqLayout, "Unicom", unicom );

            for ( Cursor c : result ) {
                c.close();
            }
        }

    }

    protected void addRow( TableLayout table, String label, String value ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tvLabel = new TextView( this );
        tvLabel.setText( label );
        tvLabel.setGravity( Gravity.LEFT );
        row.addView( tvLabel );
        TextView tvValue = new TextView( this );
        tvValue.setText( value );
        tvValue.setGravity( Gravity.RIGHT );
         tvValue.setBackgroundResource( R.drawable.rounded_rectangle );
        row.addView( tvValue );
        table.addView( row );        
    }

    protected void addSeparator( TableLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, 
                new TableLayout.LayoutParams( TableLayout.LayoutParams.FILL_PARENT, 1 ) );
    }
}
