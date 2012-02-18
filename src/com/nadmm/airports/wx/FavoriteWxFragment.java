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
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.widget.ListView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.Favorites;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NetworkUtils;

public class FavoriteWxFragment extends ListFragment {

    protected HashMap<String, Metar> mStationWx = new HashMap<String, Metar>();
    protected BroadcastReceiver mReceiver;
    protected FavoriteWxTask mTask;
    protected WxCursorAdapter mAdapter = null;

    private final class MetarReceiver extends BroadcastReceiver {

        protected int mWxUpdates = 0;

        @Override
        public void onReceive( Context context, Intent intent ) {
            Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
            mStationWx.put( metar.stationId, metar );

            ListView l =getListView();
            int first = l.getFirstVisiblePosition();

            int pos = 0;
            while ( pos <= l.getChildCount() ) {
                View view = l.getChildAt( pos );
                if ( view != null ) {
                    String icaoCode = (String) view.getTag();
                    if ( icaoCode.equals( metar.stationId ) ) {
                        WxCursorAdapter adapter = (WxCursorAdapter) getListAdapter();
                        Cursor c = (Cursor) adapter.getItem( pos+first );
                        adapter.showMetarInfo( view, c, metar );
                        break;
                    }
                }
                ++pos;
            }

            ++mWxUpdates;
            if ( mWxUpdates == mStationWx.size() ) {
                // We have all the wx updates, stop the refresh animation
                mWxUpdates = 0;
                ActivityBase activity = (ActivityBase) getActivity();
                activity.stopRefreshAnimation();
            }
        }
        
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        mReceiver = new MetarReceiver();
        setHasOptionsMenu( true );
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onDestroy() {
        if ( mAdapter != null ) {
            Cursor c = mAdapter.getCursor();
            c.close();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( NoaaService.ACTION_GET_METAR );
        getActivity().registerReceiver( mReceiver, filter );
        startTask();
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver( mReceiver );
        stopTask();
        super.onPause();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        setEmptyText( "No favorite weather stations yet" );
        super.onActivityCreated( savedInstanceState );
    }

    @Override
    public void onListItemClick( ListView l, View view, int position, long id ) {
        Cursor c = (Cursor) l.getItemAtPosition( position );
        String icaoCode = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
        Intent intent = new Intent( getActivity(), WxDetailActivity.class );
        Bundle args = new Bundle();
        args.putString( NoaaService.STATION_ID, icaoCode );
        intent.putExtras( args );
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

        for ( String stationId : mStationWx.keySet() ) {
            Intent service = new Intent( activity, MetarService.class );
            service.setAction( NoaaService.ACTION_GET_METAR );
            service.putExtra( NoaaService.STATION_ID, stationId );
            if ( force ) {
                service.putExtra( NoaaService.FORCE_REFRESH, true );
            }
            else if ( cacheOnly ) {
                service.putExtra( NoaaService.CACHE_ONLY, true );
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
            if ( favorites.size() == 0 ) {
                return null;
            }

            String selectionList = "";
            for (String stationId : favorites ) {
                if ( selectionList.length() > 0 ) {
                    selectionList += ", ";
                }
                selectionList += "'"+stationId+"'";
            };

            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences( getActivity() );
            boolean migrated = prefs.getBoolean( PreferencesActivity.KEY_WX_FAV_MIGRATED, false );
            if ( !migrated ) {
                //Migrate the favorite wx station ids from FAA code to ICAO code
                favorites.clear();
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables( Airports.TABLE_NAME );
                String selection = Airports.FAA_CODE+" in ("+selectionList+")";
                Cursor c = builder.query( db, new String[] { Airports.ICAO_CODE },
                        selection, null, null, null, null );
                if ( c.moveToFirst() ) {
                    try {
                        SQLiteDatabase userDb = dbManager.getUserDataDb();
                        ContentValues values = new ContentValues();
                        do {
                            // Insert new rows with ICAO code
                            String icaoCode = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
                            favorites.add( icaoCode );
                            values.put( Favorites.TYPE, "WX" );
                            values.put( Favorites.LOCATION_ID, icaoCode );
                            userDb.insertOrThrow( Favorites.TABLE_NAME, null, values );
                        } while ( c.moveToNext() );
                        // Delete the old rows with FAA code
                        selection = Favorites.LOCATION_ID+" in ("+selectionList+")";
                        userDb.delete( Favorites.TABLE_NAME, selection, null );
                    } catch ( Exception e ) {
                    }
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean( PreferencesActivity.KEY_WX_FAV_MIGRATED, true );
                editor.commit();

                // No rebuild the favorites list
                selectionList = "";
                for (String stationId : favorites ) {
                    if ( selectionList.length() > 0 ) {
                        selectionList += ", ";
                    }
                    selectionList += "'"+stationId+"'";
                };
            }

            for (String stationId : favorites ) {
                mStationWx.put( stationId, null );
            }

            String selection = Wxs.STATION_ID+" in ("+selectionList+")";

            String[] wxColumns = new String[] {
                "x."+BaseColumns._ID,
                Wxs.STATION_ID,
                Wxs.STATION_NAME,
                Wxs.STATION_ELEVATOIN_METER,
                "x."+Wxs.STATION_LATITUDE_DEGREES,
                "x."+Wxs.STATION_LONGITUDE_DEGREES,
                Awos.WX_SENSOR_IDENT,
                Awos.WX_SENSOR_TYPE,
                Awos.STATION_FREQUENCY,
                Awos.SECOND_STATION_FREQUENCY,
                Awos.STATION_PHONE_NUMBER,
                Airports.ASSOC_CITY,
                Airports.ASSOC_STATE,
            };

            String sortOrder = Wxs.STATION_NAME;

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Wxs.TABLE_NAME+" x"
                    +" LEFT JOIN "+Airports.TABLE_NAME+" a"
                    +" ON x."+Wxs.STATION_ID+" = a."+Airports.ICAO_CODE
                    +" LEFT JOIN "+Awos.TABLE_NAME+" w"
                    +" ON w."+Awos.WX_SENSOR_IDENT+" = a."+Airports.FAA_CODE );
            Cursor c = builder.query( db, wxColumns, selection, 
                    null, null, null, sortOrder, null );

            return c;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            if ( c != null ) {
                mFragment.setCursor( c );
            }
        }

    }

}