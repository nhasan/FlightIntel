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

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.LocationColumns;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.afd.AirportDetailsActivity;
import com.nadmm.airports.utils.NetworkUtils;

public class NearbyWxFragment extends FragmentBase implements LocationListener {

    protected HashMap<String, Metar> mStationWx = new HashMap<String, Metar>();
    protected BroadcastReceiver mReceiver;
    private WxCursorAdapter mListAdapter;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        mReceiver = new MetarReceiver();
        setHasOptionsMenu( true );
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onDestroy() {
        if ( mListAdapter != null ) {
            Cursor c = mListAdapter.getCursor();
            c.close();
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        setFragmentContentShown( false );

        IntentFilter filter = new IntentFilter();
        filter.addAction( NoaaService.ACTION_GET_METAR );
        getActivity().registerReceiver( mReceiver, filter );

        Bundle args = getArguments();
        Location location = args.getParcelable( LocationColumns.LOCATION );
        if ( location != null ) {
            new NearbyWxTask().execute( location );
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver( mReceiver );

        super.onPause();
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.list_view_layout, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        Bundle args = getArguments();
        Location location = (Location) args.getParcelable( LocationColumns.LOCATION );
        onLocationChanged( location );

        super.onActivityCreated( savedInstanceState );
    }

    private final class MetarReceiver extends BroadcastReceiver {

        protected int mWxUpdates = 0;

        @Override
        public void onReceive( Context context, Intent intent ) {
            Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
            mStationWx.put( metar.stationId, metar );

            ListView l = (ListView) findViewById( R.id.list_view );
            int first = l.getFirstVisiblePosition();

            int pos = 0;
            while ( pos <= l.getChildCount() ) {
                View view = l.getChildAt( pos );
                if ( view != null ) {
                    String icaoCode = (String) view.getTag();
                    if ( icaoCode.equals( metar.stationId ) ) {
                        Cursor c = (Cursor) mListAdapter.getItem( pos+first );
                        mListAdapter.showMetarInfo( view, c, metar );
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

    private final class NearbyWxTask extends AsyncTask<Location, Void, Cursor> {

        @Override
        protected Cursor doInBackground( Location... params ) {
            Location location = params[ 0 ];
            Bundle args = getArguments();
            int radius = args.getInt( LocationColumns.RADIUS );
            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );

            return new NearbyWxCursor( db, location, radius );
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            setCursor( c );
        }

    }

    public void setCursor( final Cursor c ) {
        // We may get called here after activity has detached
        ActivityBase activity = (ActivityBase) getActivity();
        if ( activity != null ) {
            if ( mListAdapter == null ) {
                mListAdapter = new WxCursorAdapter( activity, c );
                mListAdapter.setMetars( mStationWx );
                ListView lv = (ListView) findViewById( R.id.list_view );
                lv.setAdapter( mListAdapter );
            } else {
                mListAdapter.changeCursor( c );
            }

            requestMetars( false );
            setFragmentContentShown( true );
        }
    }

    @SuppressWarnings("deprecation")
    protected void showDetails( Cursor c ) {
        TextView title = (TextView) findViewById( R.id.list_title );
        title.setVisibility( View.VISIBLE );

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( getActivity() );
        int radius = Integer.valueOf( prefs.getString( 
                PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "20" ) );
        int count = c.getCount();

        String msg = String.format( "%d wx stations found within %d NM radius", count, radius );
        title.setText( msg );

        ListView listView = (ListView) findViewById( R.id.list_view );
        listView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                Cursor c = mListAdapter.getCursor();
                c.moveToPosition( position );
                String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                Intent intent = new Intent( getActivity(), AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
            }

        } );

        int position = listView.getFirstVisiblePosition();
        if ( mListAdapter == null ) {
            mListAdapter = new WxCursorAdapter( getActivity(), c );
        } else {
            mListAdapter.changeCursor( c );
        }
        listView.setAdapter( mListAdapter );
        listView.setSelection( position );
        getActivityBase().startManagingCursor( c );

        setFragmentContentShown( true );
    }

    protected void requestMetars( Boolean force ) {
        ActivityBase activity = (ActivityBase) getActivity();

        boolean cacheOnly = NetworkUtils.useCacheContentOnly( activity );
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

    @Override
    public void onLocationChanged( Location location ) {
        if ( location != null ) {
            new NearbyWxTask().execute( location );
        }
    }

    @Override
    public void onProviderDisabled( String provider ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onProviderEnabled( String provider ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onStatusChanged( String provider, int status, Bundle extras ) {
        // TODO Auto-generated method stub
        
    }

}
