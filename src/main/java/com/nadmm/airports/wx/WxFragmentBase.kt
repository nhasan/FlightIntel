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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.utils.UiUtils;

public abstract class WxFragmentBase extends FragmentBase {
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.registerReceiver( mReceiver, mFilter );
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.unregisterReceiver( mReceiver );
    }

    protected void setupBroadcastFilter( String action ) {
        mFilter = new IntentFilter();
        mFilter.addAction( action );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action.equals( mFilter.getAction( 0 ) ) ) {
                    handleBroadcast( intent );
                }
            }
        };
    }

    protected View addWxRow( LinearLayout layout, String label, String code ) {
        View row = addProgressRow( layout, label );
        row.setTag( code );
        int background = UiUtils.getSelectableItemBackgroundResource( getActivity() );
        row.setBackgroundResource( background );
        return row;
    }

    protected AutoCompleteTextView getAutoCompleteTextView( TextInputLayout textInputLayout) {
        return (AutoCompleteTextView) textInputLayout.getEditText();
    }

    protected int getSelectedItemPos( TextInputLayout textInputLayout )
    {
        AutoCompleteTextView textView = getAutoCompleteTextView( textInputLayout );
        if ( textView == null ) return -1;

        ArrayAdapter<?> adapter = (ArrayAdapter<?>) textView.getAdapter();
        if ( adapter == null ) return -1;

        String text = textView.getText().toString();
        if ( text != null && !text.isEmpty() ) {
            int count = adapter.getCount();
            for ( int i = 0; i < count; ++i ) {
                Object o = adapter.getItem( i );
                if ( o.toString().equals( text ) ) return i;
            }
        }
        return -1;
    }

    protected void handleBroadcast( Intent intent ) {
    }

    protected abstract String getProduct();

}
