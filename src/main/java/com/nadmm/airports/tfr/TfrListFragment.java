/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2016 Nadeem Hasan <nhasan@nadmm.com>
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.TimeUtils;

import java.util.Locale;

public class TfrListFragment extends ListFragmentBase {

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
        View view = super.onCreateView( inflater, container, savedInstanceState );

        final ListView listView = getListView();
        View footer = inflater.inflate( R.layout.tfr_list_footer_view, listView, false );
        footer.setLayoutParams( new ListView.LayoutParams(
                ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT ) );
        TextView tv = (TextView) footer.findViewById( R.id.tfr_warning_text );
        tv.setText( "Depicted TFR data may not be a complete listing. Pilots should not use "
                + "the information for flight planning purposes. For the latest information, "
                + "call your local Flight Service Station at 1-800-WX-BRIEF." );
        listView.addFooterView( footer, null, false );
        listView.setFooterDividersEnabled( true );

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
        setActionBarSubtitle( "Loading..." );

        requestTfrList( false );
    }

    @Override
    public boolean isRefreshable() {
        return true;
    }

    @Override
    public void requestDataRefresh() {
        requestTfrList( true );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        ListAdapter adapter = getListView().getAdapter();
        Tfr tfr = (Tfr) adapter.getItem( position );
        if ( tfr != null ) {
            Intent activity = new Intent( getActivity(), TfrDetailActivity.class );
            activity.putExtra( TfrListActivity.EXTRA_TFR, tfr );
            startActivity( activity );
        }
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

            int count = tfrList.entries.size();
            if ( count > 0 ) {
                TextView tv = (TextView) findViewById( R.id.tfr_fetch_time );
                tv.setText( "Fetched "+TimeUtils.formatElapsedTime( tfrList.fetchTime )  );
                setActionBarSubtitle( String.format( Locale.US, "%d TFRs found", count) );
            } else {
                setEmptyText( "Unable to fetch TFR list. Please try again later" );
                setActionBarSubtitle( "" );
            }

            TfrListAdapter adapter = new TfrListAdapter( getActivity(), tfrList );
            setAdapter( adapter );

            if ( isRefreshing() ) {
                setRefreshing( false );
            }
        }
    }

}
