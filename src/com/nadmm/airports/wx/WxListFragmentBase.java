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
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NetworkUtils;

public class WxListFragmentBase extends FragmentBase {

    protected HashMap<String, Metar> mStationWx = new HashMap<String, Metar>();
    protected BroadcastReceiver mReceiver;
    protected WxCursorAdapter mListAdapter;
    protected ListView mListView;

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
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.list_view_layout, container, false );
        mListView = (ListView) view.findViewById( R.id.list_view );
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                Cursor c = (Cursor) mListView.getItemAtPosition( position );
                String icaoCode = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
                Intent intent = new Intent( getActivity(), WxDetailActivity.class );
                Bundle args = new Bundle();
                args.putString( NoaaService.STATION_ID, icaoCode );
                intent.putExtras( args );
                startActivity( intent );
            }

        } );

        return createContentView( view );
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
        ActivityBase activity = (ActivityBase) getActivity();
        if ( activity != null ) {
            if ( mListAdapter == null ) {
                mListAdapter = new WxCursorAdapter( activity, c );
                if ( c.moveToFirst() ) {
                    do {
                        String stationId = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
                        mStationWx.put( stationId, null );
                    } while ( c.moveToNext() );
                }
                mListAdapter.setMetars( mStationWx );
            } else {
                mListAdapter.changeCursor( c );
            }
            if ( mListView.getAdapter() == null ) {
                mListView.setAdapter( mListAdapter );
            }

            requestMetars( false );
            setFragmentContentShown( true );
        }
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

    private final class MetarReceiver extends BroadcastReceiver {

        protected int mWxUpdates = 0;

        @Override
        public void onReceive( Context context, Intent intent ) {
            Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
            if ( metar.isValid ) {
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

}
