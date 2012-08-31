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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TextFileViewActivity;
import com.nadmm.airports.utils.UiUtils;

public abstract class WxTextFragmentBase extends FragmentBase {

    private final String mAction;
    private final String[] mWxAreaCodes;
    private final String[] mWxAreaNames;

    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private View mPendingRow;

    public WxTextFragmentBase( String action, String[] areaCodes, String[] areaNames ) {
        mAction = action;
        mWxAreaCodes = areaCodes;
        mWxAreaNames = areaNames;
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( false );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action.equals( mAction ) && mPendingRow != null ) {
                    TextView tv = (TextView) mPendingRow.findViewById( R.id.text );
                    String label = tv.getText().toString();
                    String path = intent.getStringExtra( NoaaService.RESULT );
                    Intent viewer = new Intent( getActivity(), TextFileViewActivity.class );
                    viewer.putExtra( TextFileViewActivity.FILE_PATH, path );
                    viewer.putExtra( TextFileViewActivity.TITLE_TEXT, getTitle() );
                    viewer.putExtra( TextFileViewActivity.LABEL_TEXT, label );
                    getActivity().startActivity( viewer );
                    setProgressBarVisible( false );
                    mPendingRow = null;
                }
            }
        };

        mFilter = new IntentFilter();
        mFilter.addAction( mAction );
    }

    @Override
    public void onResume() {
        mPendingRow = null;
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
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View v = inflate( R.layout.wx_map_detail_view );
        TextView tv = (TextView) v.findViewById( R.id.wx_map_label );
        tv.setText( "Select Area" );
        tv.setVisibility( View.VISIBLE );

        OnClickListener listener = new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                if ( mPendingRow == null ) {
                    mPendingRow = v;
                    String code = (String) v.getTag();
                    requestWxText( code );
                }
            }
        };

        LinearLayout layout = (LinearLayout) v.findViewById( R.id.wx_map_layout );
        for ( int i = 0; i < mWxAreaNames.length; ++i ) {
            View row = addProgressRow( layout, mWxAreaNames[ i ] );
            row.setTag( mWxAreaCodes[ i ] );
            row.setOnClickListener( listener );
            row.setBackgroundResource( UiUtils.getRowSelector( i, mWxAreaNames.length ) );
        }

        return v;
    }

    private void requestWxText( String code ) {
        setProgressBarVisible( true );
        Intent service = getServiceIntent();
        service.setAction( mAction );
        service.putExtra( NoaaService.STATION_ID, code );

        getActivity().startService( service );
    }

    private void setProgressBarVisible( boolean visible ) {
        if ( mPendingRow != null ) {
            View view = mPendingRow.findViewById( R.id.progress );
            view.setVisibility( visible? View.VISIBLE : View.INVISIBLE );
        }
    }

    protected abstract String getTitle();

    protected abstract Intent getServiceIntent();

}
