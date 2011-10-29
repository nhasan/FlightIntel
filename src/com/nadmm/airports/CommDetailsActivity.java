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

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Aff3;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Tower1;
import com.nadmm.airports.DatabaseManager.Tower3;
import com.nadmm.airports.DatabaseManager.Tower6;
import com.nadmm.airports.DatabaseManager.Tower7;

public class CommDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        CommDetailsTask task = new CommDetailsTask();
        task.execute( siteNumber );
    }

    private final class CommDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 6 ];
            
            Cursor apt = mDbManager.getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower1.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "*" },
                    Tower1.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 1 ] = c;

            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower3.FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, Tower3.MASTER_AIRPORT_FREQ_USE, null );
            cursors[ 2 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower6.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower3.FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, null, null );
            cursors[ 3 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower7.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower7.SATELLITE_AIRPORT_SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 4 ] = c;

            String faa_code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Aff3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Aff3.IFR_FACILITY_ID+"=?",
                    new String[] { faa_code }, null, null, null, null );
            cursors[ 5 ] = c;

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            View view = mInflater.inflate( R.layout.comm_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.comm_top_layout );

            // Title
            Cursor apt = result[ 0 ];
            showAirportTitle( mMainLayout, apt );

            showAirportFrequencies( result );
            showAtcFrequencies( result );
            showRemarks( result );

            // Cleanup cursors
            for ( Cursor c : result ) {
                if ( c != null ) {
                    c.close();
                }
            }
        }

    }

    protected void showAirportFrequencies( Cursor[] result ) {
        Cursor twr1 = result[ 1 ];
        String towerRadioCall = "";
        if ( twr1.moveToFirst() ) {
            towerRadioCall = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_TOWER ) );
        }

        Cursor twr3 = result[ 2 ];
        if ( twr3.moveToFirst() ) {
            HashMap<String, ArrayList<Pair<String, String>>> map =
                new HashMap<String, ArrayList<Pair<String, String>>>();
            do {
                String freq = twr3.getString( twr3.getColumnIndex( Tower3.MASTER_AIRPORT_FREQ ) );
                String extra = "";
                String freqUse = twr3.getString( twr3.getColumnIndex(
                        Tower3.MASTER_AIRPORT_FREQ_USE ) );
                // Remove any text past the frequency
                int i = 0;
                while ( i < freq.length() ) {
                    char c = freq.charAt( i );
                    if ( ( c >= '0' && c <= '9' ) || c == '.' ) {
                        ++i;
                        continue;
                    }
                    extra = freq.substring( i );
                    freq = freq.substring( 0, i );
                    break;
                }
                if ( freqUse.contains( "LCL" ) ) {
                    addFrequencyToMap( map, towerRadioCall+" Tower", freq, extra );
                }
                if ( freqUse.contains( "GND" ) ) {
                    addFrequencyToMap( map, towerRadioCall+" Ground", freq, extra );
                }
                if ( freqUse.contains( "CD" ) || freqUse.contains( "CLNC" ) ) {
                    addFrequencyToMap( map, "Clearance Delivery", freq, extra );
                }
                if ( freqUse.contains( "CLASS B" ) ) {
                    addFrequencyToMap( map, "Class B", freq, extra );
                }
                if ( freqUse.contains( "CLASS C" ) ) {
                    addFrequencyToMap( map, "Class C", freq, extra );
                }
                if ( freqUse.contains( "ATIS" ) ) {
                    if ( freqUse.contains( "D-ATIS" ) ) {
                        addFrequencyToMap( map, "D-ATIS", freq, extra );
                    } else {
                        addFrequencyToMap( map, "ATIS", freq, extra );
                    }
                }
                if ( freqUse.contains( "RADAR" ) || freqUse.contains( "RDR" ) ) {
                    addFrequencyToMap( map, "Radar", freq, extra );
                }
                if ( freqUse.contains( "TRSA" ) ) {
                    addFrequencyToMap( map, "TRSA", freq, extra );
                }
                if ( freqUse.contains( "TAXI CLNC" ) ) {
                    addFrequencyToMap( map, "Pre-taxi Clearance", freq, extra );
                }
                if ( freqUse.contains( "EMERG" ) ) {
                    addFrequencyToMap( map, "Emergency", freq, extra );
                }
            } while ( twr3.moveToNext() );

            TableLayout layout = (TableLayout) mMainLayout.findViewById(
                    R.id.airport_comm_details );
            int row = 0;
            for ( String key : map.keySet() ) {
                for ( Pair<String, String> pair : map.get( key ) ) {
                    if ( row > 0 ) {
                        addSeparator( layout );
                    }
                    addRow( layout, key, pair );
                    ++row;
                }
            }
        } else {
            finish();
        }
    }

    protected void showAtcFrequencies( Cursor[] result ) {
        Cursor twr1 = result[ 1 ];
        String apchRadioCall = "";
        String depRadioCall = "";
        if ( twr1.moveToFirst() ) {
            apchRadioCall = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_APCH ) );
            depRadioCall = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_DEP ) );
        }

        HashMap<String, ArrayList<Pair<String, String>>> map =
            new HashMap<String, ArrayList<Pair<String, String>>>();

        Cursor twr3 = result[ 2 ];
        if ( twr3.moveToFirst() ) {
            do {
                String freq = twr3.getString( twr3.getColumnIndex( Tower3.MASTER_AIRPORT_FREQ ) );
                String extra = "";
                String freqUse = twr3.getString( twr3.getColumnIndex(
                        Tower3.MASTER_AIRPORT_FREQ_USE ) );
                // Remove any text past the frequency
                int i = 0;
                while ( i < freq.length() ) {
                    char c = freq.charAt( i );
                    if ( ( c >= '0' && c <= '9' ) || c == '.' ) {
                        ++i;
                        continue;
                    }
                    extra = freq.substring( i );
                    freq = freq.substring( 0, i );
                    break;
                }
                if ( freqUse.contains( "APCH" ) || freqUse.contains( "ARRIVAL" ) ) {
                    addFrequencyToMap( map, apchRadioCall+" Approach", freq, extra );
                }
                if ( freqUse.contains( "DEP" ) ) {
                    addFrequencyToMap( map, depRadioCall+" Departure", freq, extra );
                }
            } while ( twr3.moveToNext() );
        }

        Cursor twr7 = result[ 4 ];
        if ( twr7.moveToFirst() ) {
            do {
                String freq = twr7.getString( twr7.getColumnIndex(
                        Tower7.SATELLITE_AIRPORT_FREQ ) );
                String extra = "";
                String freqUse = twr7.getString( twr7.getColumnIndex(
                        Tower7.SATELLITE_AIRPORT_FREQ_USE ) );
                int i = 0;
                while ( i < freq.length() ) {
                    char c = freq.charAt( i );
                    if ( ( c >= '0' && c <= '9' ) || c == '.' ) {
                        ++i;
                        continue;
                    }
                    extra = freq.substring( i );
                    freq = freq.substring( 0, i );
                    break;
                }
                if ( freqUse.contains( "APCH" ) || freqUse.contains( "ARRIVAL" ) ) {
                    addFrequencyToMap( map, apchRadioCall+" Approach", freq, extra );
                }
                if ( freqUse.contains( "DEP" ) ) {
                    addFrequencyToMap( map, depRadioCall+" Departure", freq, extra );
                }
                if ( freqUse.contains( "CD" ) || freqUse.contains( "CLNC" ) ) {
                    addFrequencyToMap( map, "Clearance Delivery", freq, extra );
                }
                if ( freqUse.contains( "OPNS" ) ) {
                    addFrequencyToMap( map, "Operations", freq, extra );
                }
                if ( freqUse.contains( "FINAL" ) ) {
                    addFrequencyToMap( map, "Final Vector", freq, extra );
                }
                if ( freqUse.contains( "RADAR" ) || freqUse.contains( "RDR" ) ) {
                    addFrequencyToMap( map, "Radar", freq, extra );
                }
                if ( freqUse.contains( "CLASS B" ) ) {
                    addFrequencyToMap( map, "Class B", freq, extra );
                }
                if ( freqUse.contains( "CLASS C" ) ) {
                    addFrequencyToMap( map, "Class C", freq, extra );
                }
            } while ( twr7.moveToNext() );
        }

        Cursor aff3 = result[ 5 ];
        if ( aff3.moveToFirst() ) {
            do {
                String artcc = aff3.getString( aff3.getColumnIndex( Aff3.ARTCC_ID ) );
                String freq = aff3.getString( aff3.getColumnIndex( Aff3.SITE_FREQUENCY ) );
                String alt = aff3.getString( aff3.getColumnIndex( Aff3.FREQ_ALTITUDE ) );
                String extra = "("+alt+" altitude)";
                String type = aff3.getString( aff3.getColumnIndex( Aff3.FACILITY_TYPE ) );
                if ( !type.equals( "ARTCC" ) ) {
                    extra = aff3.getString( aff3.getColumnIndex( Aff3.SITE_LOCATION ) )
                            +" "+type+" "+extra;
                }
                addFrequencyToMap( map, DataUtils.decodeArtcc( artcc ), freq, extra );
            } while ( aff3.moveToNext() );
        }

        if ( !map.isEmpty() ) {
            TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.atc_comm_details );
            int row = 0;
            for ( String key : map.keySet() ) {
                for ( Pair<String, String> pair : map.get( key ) ) {
                    if ( row > 0 ) {
                        addSeparator( layout );
                    }
                    addRow( layout, key, pair );
                    ++row;
                }
            }
        } else {
            TableLayout layout = (TableLayout) mMainLayout.findViewById(
                    R.id.atc_comm_details );
            layout.setVisibility( View.GONE );
        }
    }

    protected void addFrequencyToMap( HashMap<String, ArrayList<Pair<String, String>>> map,
            String key, String freq, String extra ) {
        ArrayList<Pair<String, String>> list = map.get( key );
        if ( list == null ) {
            list = new ArrayList<Pair<String, String>>();
        }
        list.add( Pair.create( String.format( "%.3f", Double.valueOf( freq ) ), extra.trim() ) );
        map.put( key, list );
    }

    protected void showRemarks( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.comm_remarks_layout );
        Cursor twr6 = result[ 3 ];
        if ( twr6.moveToFirst() ) {
            int row = 0;
            do {
                if ( row > 0 ) {
                    addSeparator( layout );
                }
                String remark = twr6.getString( twr6.getColumnIndex( Tower6.REMARK_TEXT ) );
                addRow( layout, remark );
            } while ( twr6.moveToNext() );
        } else {
            layout.setVisibility( View.GONE );
        }
    }

    protected void addRow( TableLayout table, String freqUse, Pair<String, String> data ) {
        RelativeLayout layout = (RelativeLayout) mInflater.inflate(
                R.layout.comm_detail_item, null );
        TextView tv = (TextView) layout.findViewById( R.id.comm_freq_use );
        tv.setText( freqUse );
        tv = (TextView) layout.findViewById( R.id.comm_freq_value );
        tv.setText( data.first );
        tv = (TextView) layout.findViewById( R.id.comm_freq_extra );
        if ( data.second.length() > 0 ) {
            tv.setText( data.second );
            tv.setVisibility( View.VISIBLE );
        }
        table.addView( layout, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRow( LinearLayout layout, String remark ) {
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
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( LinearLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, 
                new TableLayout.LayoutParams( TableLayout.LayoutParams.FILL_PARENT, 1 ) );
    }

}
