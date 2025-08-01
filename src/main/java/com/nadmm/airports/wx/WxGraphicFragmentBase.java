/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.ImageViewActivity;
import com.nadmm.airports.R;

public abstract class WxGraphicFragmentBase extends WxFragmentBase {

    private final String mAction;
    private final String[] mWxTypeCodes;
    private final String[] mWxTypeNames;
    private final String[] mWxMapCodes;
    private final String[] mWxMapNames;
    private String mTypeName;
    private String mLabel;
    private String mTitle;
    private String mHelpText;
    private View mPendingRow;
    private TextInputLayout mSpinner;

    public WxGraphicFragmentBase(String action, String[] mapCodes, String[] mapNames ) {
        mAction = action;
        mWxMapCodes = mapCodes;
        mWxMapNames = mapNames;
        mWxTypeCodes = null;
        mWxTypeNames = null;
    }

    public WxGraphicFragmentBase(String action, String[] mapCodes, String[] mapNames,
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

        setupBroadcastFilter( mAction );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.wx_map_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated( view, savedInstanceState );

        if ( mLabel != null && mLabel.length() > 0 ) {
            TextView tv = view.findViewById( R.id.wx_map_label );
            tv.setText( mLabel );
            tv.setVisibility( View.VISIBLE );
        }

        if ( mHelpText != null && mHelpText.length() > 0 ) {
            TextView tv = view.findViewById( R.id.wx_help_text);
            tv.setText( mHelpText );
            tv.setVisibility( View.VISIBLE );
        }

        OnClickListener listener = v -> {
            if ( mPendingRow == null ) {
                mPendingRow = v;
                String code = getMapCode( v );
                requestWxMap( code );
            }
        };

        LinearLayout layout = view.findViewById( R.id.wx_map_layout );
        for ( int i = 0; i < mWxMapCodes.length; ++i ) {
            View row = addWxRow( layout, mWxMapNames[ i ], mWxMapCodes[ i ] );
            row.setOnClickListener( listener );
        }

        if ( mWxTypeCodes != null && mWxTypeNames != null) {
            TextView tv = view.findViewById( R.id.wx_map_type_label );
            tv.setVisibility( View.VISIBLE );
            if ( mTypeName != null && mTypeName.length() > 0 ) {
                tv.setText( mTypeName );
            }
            layout = view.findViewById( R.id.wx_map_type_layout );
            layout.setVisibility( View.VISIBLE );
            mSpinner = view.findViewById( R.id.wx_map_type);
            ArrayAdapter<String> adapter = new ArrayAdapter<>( getActivity(),
                    R.layout.list_item, mWxTypeNames );
            AutoCompleteTextView textView = getAutoCompleteTextView( mSpinner );
            textView.setAdapter( adapter );
            textView.setText( mWxTypeNames[0], false);
        }

        setFragmentContentShown( true );
    }

    @Override
    public void onResume() {
        super.onResume();

        mPendingRow = null;
    }

    @Override
    protected void handleBroadcast( Intent intent ) {
        String type = intent.getStringExtra( NoaaService.TYPE );
        if ( type.equals( NoaaService.TYPE_GRAPHIC) ) {
            showWxMap( intent );
        }
    }

    private void requestWxMap( String code ) {
        setProgressBarVisible( true );

        Intent service = getServiceIntent();
        service.setAction( mAction );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_GRAPHIC);
        service.putExtra( NoaaService.IMAGE_CODE, code );
        if ( mSpinner != null && mWxTypeCodes != null) {
            int pos = getSelectedItemPos( mSpinner );
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

    private String getMapCode( View v ) {
        return (String) v.getTag();
    }

    private String getDisplayText( String code ) {
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

    protected void setMapTypeName( String typeName ) {
        mTypeName = typeName;
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
