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
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Aff3;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.AtcPhones;
import com.nadmm.airports.DatabaseManager.Tower1;
import com.nadmm.airports.DatabaseManager.Tower3;
import com.nadmm.airports.DatabaseManager.Tower6;
import com.nadmm.airports.DatabaseManager.Tower7;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.UiUtils;

public class CommDetailsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.comm_detail_view ) );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        CommDetailsTask task = new CommDetailsTask();
        task.execute( siteNumber );
    }

    private final class CommDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 11 ];
            
            Cursor apt = getAirportDetails( siteNumber );
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

            builder = new SQLiteQueryBuilder();
            builder.setTables( Aff3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Aff3.IFR_FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, null, null );
            cursors[ 5 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "MAIN", "MAIN" }, null, null, null, null );
            cursors[ 6 ] = c;

            String faaRegion = apt.getString( apt.getColumnIndex( Airports.REGION_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "REGION", faaRegion }, null, null, null, null );
            cursors[ 7 ] = c;

            String artccId = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_ID ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "ARTCC", artccId }, null, null, null, null );
            cursors[ 8 ] = c;

            Cursor twr1 = cursors[ 1 ];
            if ( twr1.moveToFirst() ) {
                String apch = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_APCH ) );
                String tracon = DataUtils.getTraconId( apch );
                if ( tracon.length() == 0 ) {
                    String dep = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_DEP ) );
                    tracon = DataUtils.getTraconId( dep );
                }
                if ( tracon.length() > 0 ) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables( AtcPhones.TABLE_NAME );
                    c = builder.query( db, new String[] { "*" },
                            "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                            new String[] { "TRACON", tracon }, null, null, null, null );
                    cursors[ 9 ] = c;
                }
            }

            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "ATCT", faaCode }, null, null, null, null );
            cursors[ 10 ] = c;

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            showDetails( result );
        }

    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        setActionBarTitle( apt );
        showAirportTitle( apt );
        showAirportFrequencies( result );
        showAtcFrequencies( result );
        showAtcPhones( result );
        showRemarks( result );

        setContentShown( true );
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

            if ( !map.isEmpty() ) {
                TextView tv = (TextView) findViewById( R.id.airport_comm_label );
                tv.setVisibility( View.VISIBLE );
                LinearLayout layout = (LinearLayout) findViewById( R.id.airport_comm_details );
                layout.setVisibility( View.VISIBLE );
                int row = 0;
                for ( String key : map.keySet() ) {
                    for ( Pair<String, String> pair : map.get( key ) ) {
                        if ( row > 0 ) {
                            addSeparator( layout );
                        }
                        addRow( layout, key, pair.first, pair.second );
                        ++row;
                    }
                }
            }
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
            TextView tv = (TextView) findViewById( R.id.atc_comm_label );
            tv.setVisibility( View.VISIBLE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.atc_comm_details );
            layout.setVisibility( View.VISIBLE );
            int row = 0;
            for ( String key : map.keySet() ) {
                for ( Pair<String, String> pair : map.get( key ) ) {
                    if ( row > 0 ) {
                        addSeparator( layout );
                    }
                    addRow( layout, key, pair.first, pair.second );
                    ++row;
                }
            }
        }
    }

    protected void showAtcPhones( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.atc_phones_details );

        Cursor main = result[ 6 ];
        if ( main.moveToFirst() ) {
            String phone = main.getString( main.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            View row = addRow( layout, "Command center", phone );
            TextView tv = (TextView) row.findViewById( R.id.item_value );
            UiUtils.makeClickToCall( this, tv );
        }

        Cursor region = result[ 7 ];
        if ( region.moveToFirst() ) {
            String facility = region.getString( region.getColumnIndex( AtcPhones.FACILITY_ID ) );
            String phone = region.getString( region.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addSeparator( layout );
            View row = addRow( layout, DataUtils.decodeFaaRegion( facility )+" region", phone );
            TextView tv = (TextView) row.findViewById( R.id.item_value );
            UiUtils.makeClickToCall( this, tv );
        }

        Cursor artcc = result[ 8 ];
        if ( artcc.moveToFirst() ) {
            String facility = artcc.getString( artcc.getColumnIndex( AtcPhones.FACILITY_ID ) );
            String phone = artcc.getString( artcc.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addSeparator( layout );
            View row = addRow( layout, DataUtils.decodeArtcc( facility ), phone, 
                    "Regional duty office", "(24 Hr)" );
            TextView tv = (TextView) row.findViewById( R.id.item_value );
            UiUtils.makeClickToCall( this, tv );
            phone = artcc.getString( artcc.getColumnIndex( AtcPhones.BUSINESS_PHONE ) );
            String hours = artcc.getString( artcc.getColumnIndex( AtcPhones.BUSINESS_HOURS ) );
            addSeparator( layout );
            row = addRow( layout, DataUtils.decodeArtcc( facility ), phone,
                    "Business office", "("+hours+")" );
            tv = (TextView) row.findViewById( R.id.item_value );
            UiUtils.makeClickToCall( this, tv );
        }

        Cursor tracon = result[ 9 ];
        if ( tracon != null && tracon.moveToFirst() ) {
            String faaCode = tracon.getString( tracon.getColumnIndex( AtcPhones.FACILITY_ID ) );
            String phone = tracon.getString( tracon.getColumnIndex( AtcPhones.BUSINESS_PHONE ) );
            String hours = tracon.getString( tracon.getColumnIndex( AtcPhones.BUSINESS_HOURS ) );
            addSeparator( layout );
            String name = DataUtils.getTraconName( faaCode );
            View row = addRow( layout, name+" TRACON", phone, "Business office", "("+hours+")" );
            TextView tv = (TextView) row.findViewById( R.id.item_value );
            UiUtils.makeClickToCall( this, tv );
        }

        Cursor atct = result[ 10 ];
        if ( atct.moveToFirst() ) {
            Cursor tower1 = result[ 1 ];
            String name = tower1.getString( tower1.getColumnIndex( Tower1.RADIO_CALL_TOWER ) );
            String phone = atct.getString( atct.getColumnIndex( AtcPhones.BUSINESS_PHONE ) );
            String hours = atct.getString( atct.getColumnIndex( AtcPhones.BUSINESS_HOURS ) );
            addSeparator( layout );
            View row = addRow( layout, name+" Tower", phone, "Business office", "("+hours+")" );
            TextView tv = (TextView) row.findViewById( R.id.item_value );
            UiUtils.makeClickToCall( this, tv );
        }
    }

    protected void showRemarks( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.comm_remarks_layout );
        Cursor twr6 = result[ 3 ];
        if ( twr6.moveToFirst() ) {
            int row = 0;
            do {
                if ( row > 0 ) {
                    addSeparator( layout );
                }
                String remark = twr6.getString( twr6.getColumnIndex( Tower6.REMARK_TEXT ) );
                addBulletedRow( layout, remark );
            } while ( twr6.moveToNext() );
        }
        addBulletedRow( layout, "Facilities can be contacted by phone through the"
        		+" regional duty officer during non-business hours." );
    }

    protected void addFrequencyToMap( HashMap<String, ArrayList<Pair<String, String>>> map,
            String key, String freq, String extra ) {
        ArrayList<Pair<String, String>> list = map.get( key );
        if ( list == null ) {
            list = new ArrayList<Pair<String, String>>();
        }
        list.add( Pair.create( FormatUtils.formatFreq( Float.valueOf( freq ) ), extra.trim() ) );
        map.put( key, list );
    }

}
