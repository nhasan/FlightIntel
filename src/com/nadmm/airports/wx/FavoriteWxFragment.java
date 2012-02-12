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

package com.nadmm.airports.wx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.DatabaseManager.Wxl;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NetworkUtils;

public class FavoriteWxFragment extends ListFragment {

    protected HashMap<String, Metar> mStationWx = new HashMap<String, Metar>();
    protected BroadcastReceiver mReceiver;
    protected FavoriteWxTask mTask;
    protected int mWxUpdates = 0;
    protected WxCursorAdapter mAdapter = null;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        mReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive( Context context, Intent intent ) {
                Metar metar = (Metar) intent.getSerializableExtra( MetarService.RESULT );
                mStationWx.put( metar.stationId, metar );

                ++mWxUpdates;
                if ( mWxUpdates == mStationWx.size() ) {
                    // We have all the wx updates, stop the refresh animation
                    mWxUpdates = 0;
                    ActivityBase activity = (ActivityBase) getActivity();
                    activity.stopRefreshAnimation();
                    CursorAdapter adapter = (CursorAdapter) getListAdapter();
                    adapter.notifyDataSetChanged();
                }
            }
        };

        setHasOptionsMenu( true );
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mAdapter != null ) {
            Cursor c = mAdapter.getCursor();
            c.close();
        }
    }

    @Override
    public void onResume() {
        ActivityBase activity = (ActivityBase) getActivity();
        IntentFilter filter = new IntentFilter();
        filter.addAction( MetarService.ACTION_GET_METAR );
        activity.registerReceiver( mReceiver, filter );
        startTask();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver( mReceiver );
        stopTask();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );
        setEmptyText( "No favorite weather stations yet" );
    }

    @Override
    public void onListItemClick( ListView l, View view, int position, long id ) {
        Cursor c = (Cursor) l.getItemAtPosition( position );
        String icaoCode = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        String stationId = c.getString( c.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
        Intent intent = new Intent( getActivity(), WxDetailActivity.class );
        intent.putExtra( MetarService.STATION_ID, icaoCode );
        intent.putExtra( Awos.WX_SENSOR_IDENT, stationId );
        startActivity( intent );
    }

    public void startTask() {
        mTask = new FavoriteWxTask( this );
        mTask.execute( (Void[]) null );
    }

    public void stopTask() {
        if ( mTask.getStatus() != Status.FINISHED ) {
            mTask.cancel( true );
        }
    }

    public void setCursor( final Cursor c ) {
        // We may get called here after activity has detached
        ActivityBase activity = (ActivityBase) getActivity();
        if ( activity != null ) {
            if ( mAdapter == null ) {
                mAdapter = new WxCursorAdapter( activity, c );
                mAdapter.setMetars( mStationWx );
                setListAdapter( mAdapter );
            } else {
                mAdapter.changeCursor( c );
            }
            requestMetars( false );
        }
    }

    protected void requestMetars( Boolean force ) {
        ActivityBase activity = (ActivityBase) getActivity();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( activity );
        boolean alwaysAutoFetch = prefs.getBoolean(
                PreferencesActivity.KEY_ALWAYS_AUTO_FETCH_WEATHER, false );
        boolean cacheOnly = ( !alwaysAutoFetch 
                && !NetworkUtils.isConnectedToWifi( activity ) );

        if ( force || !cacheOnly ) {
            activity.startRefreshAnimation();
        }

        for ( String icaoCode : mStationWx.keySet() ) {
            Intent service = new Intent( activity, MetarService.class );
            service.setAction( MetarService.ACTION_GET_METAR );
            service.putExtra( MetarService.STATION_ID, icaoCode );
            if ( force ) {
                service.putExtra( MetarService.FORCE_REFRESH, true );
            }
            else if ( cacheOnly ) {
                service.putExtra( MetarService.CACHE_ONLY, true );
            }
            activity.startService( service );
        }
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        ActivityBase activity = (ActivityBase) getActivity();
        activity.setRefreshItemVisible( true );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_refresh:
            requestMetars( true );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

    private final class AwosData implements Comparable<AwosData> {

        public String ICAO_CODE;
        public String SENSOR_IDENT;
        public String SENSOR_TYPE;
        public String FREQUENCY;
        public String FREQUENCY2;
        public String PHONE;
        public String NAME;
        public String CITY;
        public String STATE;
        public int ELEVATION;
        public double LATITUDE;
        public double LONGITUDE;

        public AwosData( String icaoCode, String id, String type, String freq, String freq2,
                String phone, String name, String city, String state,
                int elevation, double lat, double lon ) {
            ICAO_CODE = icaoCode;
            SENSOR_IDENT = id;
            SENSOR_TYPE = type;
            FREQUENCY = freq;
            FREQUENCY2 = freq2;
            PHONE = phone;
            NAME = name;
            CITY = city;
            STATE = state;
            ELEVATION = elevation;
            LATITUDE = lat;
            LONGITUDE = lon;
        }

        @Override
        public int compareTo( AwosData another ) {
            return NAME.compareToIgnoreCase( another.NAME);
        }
    }

    public class FavoriteWxTask extends AsyncTask<Void, Void, Cursor> {

        private final FavoriteWxFragment mFragment;

        public FavoriteWxTask( FavoriteWxFragment fragment ) {
            super();
            mFragment = fragment;
        }

        @Override
        protected Cursor doInBackground( Void... params ) {
            ActivityBase activity = (ActivityBase) getActivity();
            DatabaseManager dbManager = activity.getDbManager();
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );

            ArrayList<String> favorites = dbManager.getWxFavorites();
            String selectionList = "";
            for (String facilityId : favorites ) {
                if ( selectionList.length() > 0 ) {
                    selectionList += ", ";
                }
                selectionList += "'"+facilityId+"'";
            };

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Awos.TABLE_NAME+" w"
                    +" LEFT JOIN "+Airports.TABLE_NAME+" a"
                    +" ON w."+Awos.SITE_NUMBER+" = a."+Airports.SITE_NUMBER
                    +" LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            String selection = Awos.WX_SENSOR_IDENT+" in ("+selectionList+")";
            Cursor wx1 = builder.query( db, new String[] { "w.*, a.*, s.*" },
                    selection, null, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Wxl.TABLE_NAME+" w"
                    +" JOIN "+Airports.TABLE_NAME+" a"
                    +" ON w."+Wxl.LOCATION_ID+" = a."+Airports.FAA_CODE
                    +" LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            selection = Wxl.LOCATION_ID+" in ("+selectionList+")"
                    +" AND "+Wxl.LOCATION_ID+" not in ( select "
                    +Awos.WX_SENSOR_IDENT+" from "+Awos.TABLE_NAME+")";

            Cursor wx2 = builder.query( db, new String[] { "w.*, a.*, s.*" },
                    selection, null, null, null, null, null );

            AwosData[] awosList = new AwosData[ wx1.getCount()+wx2.getCount() ];
            int index = 0;

            if ( wx1.moveToFirst() ) {
                do {
                    String id = wx1.getString( wx1.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
                    String type = wx1.getString( wx1.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
                    String freq = wx1.getString( wx1.getColumnIndex( Awos.STATION_FREQUENCY ) );
                    String freq2 = wx1.getString(
                            wx1.getColumnIndex( Awos.SECOND_STATION_FREQUENCY ) );
                    String phone = wx1.getString(
                            wx1.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
                    String icaoCode = wx1.getString( wx1.getColumnIndex( Airports.ICAO_CODE ) );
                    String name = wx1.getString( wx1.getColumnIndex( Airports.FACILITY_NAME ) );
                    String city = wx1.getString( wx1.getColumnIndex( Airports.ASSOC_CITY ) );
                    String state = wx1.getString( wx1.getColumnIndex( States.STATE_NAME ) );
                    int elevation = wx1.getInt( wx1.getColumnIndex( Airports.ELEVATION_MSL ) );
                    double lat = wx1.getFloat(
                            wx1.getColumnIndex( Awos.STATION_LATTITUDE_DEGREES ) );
                    double lon = wx1.getFloat(
                            wx1.getColumnIndex( Awos.STATION_LONGITUDE_DEGREES ) );
                    AwosData awos = new AwosData( icaoCode, id, type, freq, freq2,
                            phone, name, city, state, elevation, lat, lon );
                    awosList[ index++] = awos;
                } while ( wx1.moveToNext() );
            }
            wx1.close();

            if ( wx2.moveToFirst() ) {
                do {
                    String icaoCode = wx2.getString( wx2.getColumnIndex( Airports.ICAO_CODE ) );
                    String id = wx2.getString( wx2.getColumnIndex( Wxl.LOCATION_ID ) );
                    String type = "AWOS";
                    String freq = "";
                    String phone = "";
                    String name = wx2.getString( wx2.getColumnIndex( Airports.FACILITY_NAME ) );
                    String city = wx2.getString( wx2.getColumnIndex( Airports.ASSOC_CITY ) );
                    String state = wx2.getString( wx2.getColumnIndex( States.STATE_NAME ) );
                    int elevation = wx2.getInt( wx2.getColumnIndex( Wxl.LOC_ELEVATION_FEET ) );
                    double lat = wx2.getFloat(
                            wx2.getColumnIndex( Wxl.LOC_LATITUDE_DEGREES ) );
                    double lon = wx2.getFloat(
                            wx2.getColumnIndex( Wxl.LOC_LONGITUDE_DEGREES ) );
                    AwosData awos = new AwosData( icaoCode, id, type, freq, freq,
                            phone, name, city, state, elevation, lat, lon );
                    awosList[ index++] = awos;
                } while ( wx2.moveToNext() );
            }
            wx2.close();

            Arrays.sort( awosList );

            // Build a cursor out of the sorted wx station list
            String[] columns = new String[] {
                    BaseColumns._ID,
                    Airports.ICAO_CODE,
                    Awos.WX_SENSOR_IDENT,
                    Awos.WX_SENSOR_TYPE,
                    Awos.STATION_FREQUENCY,
                    Awos.SECOND_STATION_FREQUENCY,
                    Awos.STATION_PHONE_NUMBER,
                    Airports.FACILITY_NAME,
                    Airports.ASSOC_CITY,
                    Airports.ASSOC_STATE,
                    Airports.ELEVATION_MSL,
                    Airports.REF_LATTITUDE_DEGREES,
                    Airports.REF_LONGITUDE_DEGREES
            };

            MatrixCursor matrix = new MatrixCursor( columns );

            for ( Object o : awosList ) {
                AwosData awos = (AwosData) o;
                mStationWx.put( awos.ICAO_CODE, null );
                MatrixCursor.RowBuilder row = matrix.newRow();
                row.add( matrix.getPosition() )
                    .add( awos.ICAO_CODE )
                    .add( awos.SENSOR_IDENT )
                    .add( awos.SENSOR_TYPE )
                    .add( awos.FREQUENCY )
                    .add( awos.FREQUENCY2 )
                    .add( awos.PHONE )
                    .add( awos.NAME )
                    .add( awos.CITY )
                    .add( awos.STATE )
                    .add( awos.ELEVATION )
                    .add( awos.LATITUDE )
                    .add( awos.LONGITUDE );
            }

            return matrix;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            mFragment.setCursor( c );
        }

    }

}