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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ImageViewActivity;
import com.nadmm.airports.R;

public abstract class WxMapFragmentBase extends FragmentBase {

    private String mAction;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private String[] mWxTypeCodes;
    private String[] mWxTypeNames;
    private String[] mWxMapCodes;
    private String[] mWxMapNames;
    private String mLabel;
    private String mTitle;
    private String mHelpText;
    private View mPendingRow;
    private Spinner mSpinner;

    public WxMapFragmentBase( String action, String[] mapCodes, String[] mapNames ) {
        mAction = action;
        mWxMapCodes = mapCodes;
        mWxMapNames = mapNames;
        mWxTypeCodes = null;
        mWxTypeNames = null;
    }

    public WxMapFragmentBase( String action, String[] mapCodes, String[] mapNames,
            String[] typeCodes, String[] typeNames ) {
        mAction = action;
        mWxMapCodes = mapCodes;
        mWxMapNames = mapNames;
        mWxTypeCodes = typeCodes;
        mWxTypeNames = typeNames;
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
        if ( mLabel != null && mLabel.length() > 0 ) {
            TextView tv = (TextView) v.findViewById( R.id.wx_map_label );
            tv.setText( mLabel );
            tv.setVisibility( View.VISIBLE );
        }

        if ( mHelpText != null && mHelpText.length() > 0 ) {
            TextView tv = (TextView) v.findViewById( R.id.help_text );
            tv.setText( mHelpText );
            tv.setVisibility( View.VISIBLE );
        }

        OnClickListener listener = new OnClickListener() {

            @Override
            public void onClick( View v ) {
                if ( mPendingRow == null ) {
                    mPendingRow = v;
                    String code = getMapCode( v );
                    requestWxMap( code );
                }
            }
        };

        LinearLayout layout = (LinearLayout) v.findViewById( R.id.wx_map_layout );
        for ( int i = 0; i < mWxMapCodes.length; ++i ) {
            View row = addProgressRow( layout, mWxMapNames[ i ] );
            row.setTag( mWxMapCodes[ i ] );
            row.setOnClickListener( listener );
            row.setBackgroundResource( R.drawable.row_selector_middle );
        }

        if ( mWxTypeCodes != null ) {
            TextView tv = (TextView) v.findViewById( R.id.wx_map_type_label );
            tv.setVisibility( View.VISIBLE );
            layout = (LinearLayout) v.findViewById( R.id.wx_map_type_layout );
            layout.setVisibility( View.VISIBLE );
            mSpinner = (Spinner) v.findViewById( R.id.map_type );
            ArrayAdapter<String> adapter = new ArrayAdapter<String>( getActivity(),
                    android.R.layout.simple_spinner_item, mWxTypeNames );
            adapter.setDropDownViewResource( R.layout.support_simple_spinner_dropdown_item );
            mSpinner.setAdapter( adapter );
        }

        return v;
    }

    private void requestWxMap( String code ) {
        setProgressBarVisible( true );
        Intent service = getServiceIntent();
        service.setAction( mAction );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_IMAGE );
        service.putExtra( NoaaService.IMAGE_CODE, code );
        if ( mSpinner != null ) {
            int pos = mSpinner.getSelectedItemPosition();
            service.putExtra( NoaaService.IMAGE_TYPE, mWxTypeCodes[ pos ] );
        }

        setServiceParams( service );

        getActivity().startService( service );
    }

    protected void setServiceParams( Intent intent ) {
    }

    private void showWxMap( Intent intent ) {
        String path = intent.getStringExtra( NoaaService.RESULT );
        if ( path != null ) {
            Intent view = new Intent( getActivity(), WxImageViewActivity.class );
            view.putExtra( ImageViewActivity.IMAGE_PATH, path );
            if ( mTitle != null ) {
                view.putExtra( ImageViewActivity.IMAGE_TITLE, mTitle );
            } else {
                view.putExtra( ImageViewActivity.IMAGE_TITLE, getActivity().getTitle() );
            }
            String code = intent.getStringExtra( NoaaService.IMAGE_CODE );
            String name = getDisplayText( code );
            if ( name != null ) {
                view.putExtra( ImageViewActivity.IMAGE_SUBTITLE, name );
            }
            startActivity( view );
        } else {
            mPendingRow = null;
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

    protected void setLabel( String label ) {
        mLabel = label;
    }

    protected void setTitle( String title ) {
        mTitle = title;
    }

    protected void setHelpText( String text ) {
        mHelpText = text;
    }

    private void setProgressBarVisible( boolean visible ) {
        if ( mPendingRow != null ) {
            View view = mPendingRow.findViewById( R.id.progress );
            view.setVisibility( visible? View.VISIBLE : View.INVISIBLE );
        }
    }

    protected abstract Intent getServiceIntent();

}
