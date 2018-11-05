/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.LocationListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager.Wxs;
import com.nadmm.airports.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.cursoradapter.widget.CursorAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class WxListFragmentBase extends LocationListFragmentBase {

    private HashMap<String, Metar> mStationWx = new HashMap<>();
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setHasOptionsMenu( true );

        mReceiver = new WxReceiver();
        mFilter = new IntentFilter();
        mFilter.addAction( NoaaService.ACTION_GET_METAR );
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.registerReceiver( mReceiver, mFilter );
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.unregisterReceiver( mReceiver );
    }

    @Override
    public boolean isRefreshable() {
        return getListAdapter() != null && !getListAdapter().isEmpty();
    }

    @Override
    public void requestDataRefresh() {
        requestMetars( NoaaService.ACTION_GET_METAR, true, true );
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

        getActivityBase().enableDisableSwipeRefresh( isRefreshable() );

        requestMetars( NoaaService.ACTION_GET_METAR, false, true );
    }

    private void requestMetars( String action, boolean force, boolean showAnim ) {
        ActivityBase activity = getActivityBase();

        if ( mStationWx.isEmpty() ) {
            activity.setRefreshing( false );
            return;
        }

        boolean cacheOnly = !NetworkUtils.canDownloadData( activity );

        if ( ( force || !cacheOnly ) && showAnim ) {
            activity.setRefreshing( true );
        }

        ArrayList<String> stationIds = new ArrayList<>( mStationWx.keySet() );
        Intent service = new Intent( activity, MetarService.class );
        service.setAction( action );
        service.putExtra( NoaaService.STATION_IDS, stationIds );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_TEXT );
        if ( force ) {
            service.putExtra( NoaaService.FORCE_REFRESH, true );
        } else if ( cacheOnly ) {
            service.putExtra( NoaaService.CACHE_ONLY, true );
        }
        activity.startService( service );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        WxCursorAdapter adapter = new WxCursorAdapter( context, c );
        adapter.setMetars( mStationWx );
        return adapter;
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        // When getting wx for one station, get for all others too
        requestMetars( NoaaService.ACTION_CACHE_METAR, false, false );
        Cursor c = (Cursor) l.getItemAtPosition( position );
        String icaoCode = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
        Intent intent = new Intent( getActivity(), WxDetailActivity.class );
        Bundle args = new Bundle();
        args.putString( NoaaService.STATION_ID, icaoCode );
        intent.putExtras( args );
        startActivity( intent );
    }

    private final class WxReceiver extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
            mStationWx.put( metar.stationId, metar );

            ListView l = findViewById( android.R.id.list );
            if ( l == null ) {
                // Seen some crashes here
                return;
            }

            int first = l.getFirstVisiblePosition();

            int pos = 0;
            while ( pos <= l.getChildCount() ) {
                View view = l.getChildAt( pos );
                if ( view != null ) {
                    String icaoCode = (String) view.getTag( R.id.TAG_STATION_ID );
                    if ( icaoCode.equals( metar.stationId ) ) {
                        WxCursorAdapter adapter = (WxCursorAdapter) getListAdapter();
                        Cursor c = (Cursor) adapter.getItem( pos+first );
                        if ( c.getPosition() >= 0 ) {
                            adapter.showMetarInfo( view, c, metar );
                        }
                        break;
                    }
                }
                ++pos;
            }

            if ( isRefreshing() ) {
                setRefreshing( false );
            }
        }

    }

}
