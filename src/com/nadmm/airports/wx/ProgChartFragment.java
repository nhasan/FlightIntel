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

import com.actionbarsherlock.view.Menu;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ImageViewActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

public class ProgChartFragment extends FragmentBase {

    private final String mAction = NoaaService.ACTION_GET_PROGCHART;

    private BroadcastReceiver mReceiver;
    private View mPendingRow;

    private static final String[] sProgChartCodes = new String[] {
        "00hr",
        "12hr",
        "24hr",
        "36hr",
        "48hr"
    };

    private static final String[] sProgChartNames = new String[] {
        "Current Analysis",
        "12 hr Forecast",
        "24 hr Forecast",
        "36 hr Forecast",
        "48 hr Forecast" 
    };

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
                        showProgChart( intent );
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
        View view = inflate( R.layout.progchart_detail_view );

        OnClickListener listener = new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                mPendingRow = v;
                String code = (String) v.getTag();
                requestProgChart( code );
            }
        };

        LinearLayout layout = (LinearLayout) view.findViewById( R.id.progchart_layout );
        for ( int i = 0; i < sProgChartCodes.length; ++i ) {
            View row = addProgressRow( layout, sProgChartNames[ i ] );
            row.setTag( sProgChartCodes[ i ] );
            row.setOnClickListener( listener );
            row.setBackgroundResource( UiUtils.getRowSelector( i, sProgChartCodes.length ) );
        }

        return view;
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        setRefreshItemVisible( true );
    }

    protected void requestProgChart( String code ) {
        setProgressBarVisible( true );
        Intent service = new Intent( getActivity(), ProgChartService.class );
        service.setAction( NoaaService.ACTION_GET_PROGCHART );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_IMAGE );
        service.putExtra( NoaaService.IMAGE_CODE, code );
        getActivity().startService( service );
    }

    protected void showProgChart( Intent intent ) {
        String path = intent.getStringExtra( NoaaService.RESULT );
        if ( path != null ) {
            Intent view = new Intent( getActivity(), ImageViewActivity.class );
            view.putExtra( ImageViewActivity.IMAGE_PATH, path );
            view.putExtra( ImageViewActivity.IMAGE_TITLE, "Prognosis Charts" );
            String code = intent.getStringExtra( NoaaService.IMAGE_CODE );
            String name = getDisplayText( code );
            if ( name != null ) {
                view.putExtra( ImageViewActivity.IMAGE_SUBTITLE, name );
            }
            startActivity( view );
        }
        setProgressBarVisible( false );
    }

    protected String getDisplayText( String code ) {
        for ( int i = 0; i < sProgChartCodes.length; ++i ) {
            if ( code.equals( sProgChartCodes[ i ] ) ) {
                return sProgChartNames[ i ];
            }
        }
        return "";
    }

    protected void setProgressBarVisible( boolean visible ) {
        View view = mPendingRow.findViewById( R.id.progress );
        view.setVisibility( visible? View.VISIBLE : View.INVISIBLE );
    }

}
