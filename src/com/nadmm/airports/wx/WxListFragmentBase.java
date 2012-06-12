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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NetworkUtils;

public class WxListFragmentBase extends ListFragmentBase {

    private HashMap<String, Metar> mStationWx = new HashMap<String, Metar>();
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        mReceiver = new WxReceiver();
        setHasOptionsMenu( true );
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( NoaaService.ACTION_GET_METAR );
        getActivity().registerReceiver( mReceiver, filter );

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver( mReceiver );

        super.onPause();
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

    public void setCursor( Cursor c ) {
        mStationWx.clear();
        if ( getActivity() != null ) {
            if ( c.moveToFirst() ) {
                do {
                    String stationId = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
                    mStationWx.put( stationId, null );
                } while ( c.moveToNext() );
            }
        }

        super.setCursor( c );

        requestMetars( false );
    }

    protected void requestMetars( Boolean force ) {
        if ( mStationWx.size() == 0 ) {
            return;
        }

        ActivityBase activity = (ActivityBase) getActivity();
        boolean cacheOnly = NetworkUtils.useCacheContentOnly( activity );
        if ( force || !cacheOnly ) {
            activity.startRefreshAnimation();
        }

        for ( String stationId : mStationWx.keySet() ) {
            Intent service = new Intent( activity, MetarService.class );
            service.setAction( NoaaService.ACTION_GET_METAR );
            service.putExtra( NoaaService.STATION_ID, stationId );
            service.putExtra( NoaaService.TYPE, NoaaService.TYPE_TEXT );
            if ( force ) {
                service.putExtra( NoaaService.FORCE_REFRESH, true );
            }
            else if ( cacheOnly ) {
                service.putExtra( NoaaService.CACHE_ONLY, true );
            }
            activity.startService( service );
        }
    }

    private final class WxReceiver extends BroadcastReceiver {

        protected int mWxUpdates = 0;

        @Override
        public void onReceive( Context context, Intent intent ) {
            Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
            mStationWx.put( metar.stationId, metar );

            ListView l = (ListView) findViewById( android.R.id.list );
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
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        WxCursorAdapter adapter = new WxCursorAdapter( context, c );
        adapter.setMetars( mStationWx );
        return adapter;
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        Cursor c = (Cursor) l.getItemAtPosition( position );
        String icaoCode = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
        Intent intent = new Intent( getActivity(), WxDetailActivity.class );
        Bundle args = new Bundle();
        args.putString( NoaaService.STATION_ID, icaoCode );
        intent.putExtras( args );
        startActivity( intent );
    }

}
