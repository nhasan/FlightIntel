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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.*;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.TimeUtils;

import java.util.Locale;

public class TfrListFragment extends FragmentBase {

    private ListView mListView;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setHasOptionsMenu( true );

        mReceiver = new TfrReceiver();
        mFilter = new IntentFilter();
        mFilter.addAction( TfrService.ACTION_GET_TFR_LIST );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.list_view_layout, container, false );
        mListView = (ListView) view.findViewById( android.R.id.list );
        View footer = inflater.inflate( R.layout.tfr_list_footer_view, mListView, false );
        footer.setLayoutParams( new ListView.LayoutParams(
                ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT ) );
        TextView tv = (TextView) footer.findViewById( R.id.tfr_warning_text );
        tv.setText( "Depicted TFR data may not be a complete listing. Pilots should not use "
                + "the information for flight planning purposes. For the latest information, "
                + "call your local Flight Service Station at 1-800-WX-BRIEF." );
        mListView.addFooterView( footer );
        mListView.setFooterDividersEnabled( true );
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                onListItemClick( mListView, view, position );
            }
        } );

        return createContentView( view );
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
        setActionBarSubtitle( "Loading..." );

        requestTfrList( false );
    }

    @Override
    public void registerActionbarAutoHideView() {
        getActivityBase().registerActionBarAutoHideListView( mListView );
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return ViewCompat.canScrollVertically( mListView, -1 );
    }

    @Override
    protected void applyContentTopClearance( int clearance ) {
        mListView.setPadding( mListView.getPaddingLeft(), clearance,
                mListView.getPaddingRight(), mListView.getPaddingBottom() );
    }

    @Override
    public boolean isRefreshable() {
        return true;
    }

    @Override
    public void requestDataRefresh() {
        requestTfrList( true );
    }

    private void onListItemClick( ListView l, View v, int position ) {
        ListAdapter adapter = mListView.getAdapter();
        Tfr tfr = (Tfr) adapter.getItem( position );
        Intent activity = new Intent( getActivity(), TfrDetailActivity.class );
        activity.putExtra( TfrListActivity.EXTRA_TFR, tfr );
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
            if ( tfrList.fetchTime > 0 ) {
                tv.setText( "Fetched "+TimeUtils.formatElapsedTime( tfrList.fetchTime )  );
            } else {
                tv.setText( "Unable to fetch TFR list. Please try again later" );
            }

            if ( isRefreshing() ) {
                setRefreshing( false );
            }

            setFragmentContentShown( true );
        }
    }

}
