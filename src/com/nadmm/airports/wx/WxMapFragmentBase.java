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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ImageViewActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

public abstract class WxMapFragmentBase extends FragmentBase {

    private String mAction;
    private BroadcastReceiver mReceiver;
    private String[] mWxMapCodes;
    private String[] mWxMapNames;
    private String mLabel;
    private View mPendingRow;

    public WxMapFragmentBase( String action, String[] codes, String[] names, String label ) {
        mAction = action;
        mWxMapCodes = codes;
        mWxMapNames = names;
        mLabel = label;
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( false );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action.equals( mAction ) ) {
                    String type = intent.getStringExtra( NoaaService.TYPE );
                    if ( type.equals( NoaaService.TYPE_IMAGE ) ) {
                        showWxMap( intent );
                    }
                }
            }
        };
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( mAction );
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
        View v = inflate( R.layout.wx_map_detail_view );
        TextView tv = (TextView) v.findViewById( R.id.wx_map_label );
        if ( mLabel != null ) {
            tv.setText( mLabel );
        } else {
            tv.setVisibility( View.GONE );
        }

        OnClickListener listener = new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                mPendingRow = v;
                String code = getMapCode( v );
                requestWxMap( code );
            }
        };

        LinearLayout layout = (LinearLayout) v.findViewById( R.id.wx_map_layout );
        for ( int i = 0; i < mWxMapCodes.length; ++i ) {
            View row = addProgressRow( layout, mWxMapNames[ i ] );
            row.setTag( mWxMapCodes[ i ] );
            row.setOnClickListener( listener );
            row.setBackgroundResource( UiUtils.getRowSelector( i, mWxMapCodes.length ) );
        }

        return v;
    }

    private void requestWxMap( String code ) {
        setProgressBarVisible( true );
        Intent service = new Intent( getActivity(), RadarService.class );
        service.setAction( mAction );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_IMAGE );
        service.putExtra( NoaaService.IMAGE_CODE, code );
        getActivity().startService( service );
    }

    private void showWxMap( Intent intent ) {
        String path = intent.getStringExtra( NoaaService.RESULT );
        if ( path != null ) {
            Intent view = new Intent( getActivity(), ImageViewActivity.class );
            view.putExtra( ImageViewActivity.IMAGE_PATH, path );
            view.putExtra( ImageViewActivity.IMAGE_TITLE, "Radar Images" );
            String code = intent.getStringExtra( NoaaService.IMAGE_CODE );
            String name = getDisplayText( code );
            if ( name != null ) {
                view.putExtra( ImageViewActivity.IMAGE_SUBTITLE, name );
            }
            startActivity( view );
        }
        setProgressBarVisible( false );
    }

    protected String getMapCode( View v ) {
        return (String) v.getTag();
    }

    protected String getDisplayText( String code ) {
        for ( int i = 0; i < mWxMapCodes.length; ++i ) {
            if ( code.equals( mWxMapCodes[ i ] ) ) {
                return mWxMapNames[ i ];
            }
        }
        return "";
    }

    protected void setProgressBarVisible( boolean visible ) {
        View view = mPendingRow.findViewById( R.id.progress );
        view.setVisibility( visible? View.VISIBLE : View.INVISIBLE );
    }

    protected abstract Intent getServiceIntent();
}
