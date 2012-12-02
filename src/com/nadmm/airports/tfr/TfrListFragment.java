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

package com.nadmm.airports.tfr;

import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.TimeUtils;

public class TfrListFragment extends FragmentBase {

    private ListView mListView;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        setHasOptionsMenu( true );

        mReceiver = new TfrReceiver();
        mFilter = new IntentFilter();
        mFilter.addAction( TfrService.ACTION_GET_TFR_LIST );

        super.onCreate( savedInstanceState );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.tfr_list_view, container, false );
        mListView = (ListView) view.findViewById( android.R.id.list );
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                onListItemClick( mListView, view, position );
            }
        } );

        return view;
    }

    @Override
    public void onResume() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.registerReceiver( mReceiver, mFilter );

        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.unregisterReceiver( mReceiver );

        super.onPause();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( "TFR List" );
        requestTfrList( false );
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        ActivityBase activity = (ActivityBase) getActivity();
        activity.setRefreshItemVisible( true );
    }

    private void onListItemClick( ListView l, View v, int position ) {
        ListAdapter adapter = mListView.getAdapter();
        Tfr tfr = (Tfr) adapter.getItem( position );
        Intent activity = new Intent( getActivity(), TfrActivity.class );
        activity.putExtra( TfrActivity.EXTRA_TFR, tfr );
        startActivity( activity );
    }

    private void requestTfrList( boolean force ) {
        Intent service = new Intent( getActivity(), TfrService.class );
        service.setAction( TfrService.ACTION_GET_TFR_LIST );
        service.putExtra( TfrService.FORCE_REFRESH, force );
        getActivity().startService( service );
    }

    private final class TfrReceiver extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            TfrList tfrList = (TfrList) intent.getSerializableExtra( TfrService.TFR_LIST );
            TfrListAdapter adapter = new TfrListAdapter( getActivity(), tfrList );
            mListView.setAdapter( adapter );
            setActionBarSubtitle( String.format( Locale.US, "%d TFRs found",
                    tfrList.entries.size() ) );
            TextView tv = (TextView) findViewById( R.id.tfr_fetch_time );
            tv.setText( "Fetched on "+TimeUtils.formatDateTime( context, tfrList.fetchTime )  );
            stopRefreshAnimation();
            setContentShown( true );
        }
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_refresh:
            requestTfrList( true );
            startRefreshAnimation();
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
