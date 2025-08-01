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
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TextFileViewActivity;

public abstract class WxTextFragmentBase extends WxFragmentBase {

    private final String mAction;
    private final String[] mWxTypeCodes;
    private final String[] mWxTypeNames;
    private final String[] mWxAreaCodes;
    private final String[] mWxAreaNames;
    private TextInputLayout mSpinner;
    private String mHelpText;

    private View mPendingRow;

    public WxTextFragmentBase( String action, String[] areaCodes, String[] areaNames ) {
        mAction = action;
        mWxAreaCodes = areaCodes;
        mWxAreaNames = areaNames;
        mWxTypeCodes = null;
        mWxTypeNames = null;
    }

    public WxTextFragmentBase( String action, String[] areaCodes, String[] areaNames,
                               String[] typeCodes, String[] typeNames) {
        mAction = action;
        mWxAreaCodes = areaCodes;
        mWxAreaNames = areaNames;
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
        View view = inflate( R.layout.wx_map_detail_view );
        return createContentView( view );
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated( view, savedInstanceState );

        TextView tv = view.findViewById( R.id.wx_map_label );
        tv.setText( "Select Area" );
        tv.setVisibility( View.VISIBLE );

        OnClickListener listener = v -> {
            if ( mPendingRow == null ) {
                mPendingRow = v;
                String code = (String) v.getTag();
                requestWxText( code );
            }
        };

        LinearLayout layout = view.findViewById( R.id.wx_map_layout );
        for ( int i = 0; i < mWxAreaNames.length; ++i ) {
            View row = addWxRow( layout, mWxAreaNames[ i ], mWxAreaCodes[ i ] );
            row.setOnClickListener( listener );
        }

        if ( mHelpText != null && mHelpText.length() > 0 ) {
            tv = view.findViewById( R.id.help_text );
            tv.setText( mHelpText );
            tv.setVisibility( View.VISIBLE );
        }

        if ( mWxTypeCodes != null && mWxTypeNames != null ) {
            tv = view.findViewById( R.id.wx_map_type_label );
            tv.setVisibility( View.VISIBLE );
            layout = view.findViewById( R.id.wx_map_type_layout );
            layout.setVisibility( View.VISIBLE );
            mSpinner = view.findViewById( R.id.map_type );
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
        if ( mPendingRow != null ) {
            TextView tv = mPendingRow.findViewById( R.id.text );
            String label = tv.getText().toString();
            String path = intent.getStringExtra( NoaaService.RESULT );
            Intent viewer = new Intent( getActivity(), TextFileViewActivity.class );
            viewer.putExtra( TextFileViewActivity.FILE_PATH, path );
            viewer.putExtra( TextFileViewActivity.TITLE_TEXT, getTitle() );
            viewer.putExtra( TextFileViewActivity.LABEL_TEXT, label );
            if ( getActivity() != null ) {
                getActivity().startActivity( viewer );
            }
            setProgressBarVisible( false );
            mPendingRow = null;
        }
    }

    private void requestWxText( String code ) {
        setProgressBarVisible( true );

        Intent service = getServiceIntent();
        service.setAction( mAction );
        service.putExtra( NoaaService.TEXT_CODE, code );
        if ( mSpinner != null && mWxTypeCodes != null ) {
            int pos = getSelectedItemPos( mSpinner );
            service.putExtra( NoaaService.TEXT_TYPE, mWxTypeCodes[ pos ] );
        }
        if ( getActivity() != null ) {
            getActivity().startService( service );
        }
    }

    private void setProgressBarVisible( boolean visible ) {
        if ( mPendingRow != null ) {
            View view = mPendingRow.findViewById( R.id.progress );
            view.setVisibility( visible? View.VISIBLE : View.INVISIBLE );
        }
    }

    protected void setHelpText( String text ) {
        mHelpText = text;
    }

    protected abstract String getTitle();

    protected abstract Intent getServiceIntent();

}
