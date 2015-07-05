/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.e6b;

import java.util.Locale;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

public class MachNumberFragment extends FragmentBase {

    private EditText mTasEdit;
    private EditText mOatEdit;
    private EditText mMachEdit;

    private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged( CharSequence s, int start, int before, int count ) {
        }

        @Override
        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
        }

        @Override
        public void afterTextChanged( Editable s ) {
            processInput();
        }
    };

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_mach_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        String title = getArguments().getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "Speed of sound and hence Mach number varies directly with OAT" );

        mTasEdit = (EditText) findViewById( R.id.e6b_edit_tas );
        mOatEdit = (EditText) findViewById( R.id.e6b_edit_oat );
        mMachEdit = (EditText) findViewById( R.id.e6b_edit_mach );

        mTasEdit.addTextChangedListener( mTextWatcher );
        mOatEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    private void processInput() {
        double tas = Double.MAX_VALUE;
        double oat = Double.MAX_VALUE;

        try {
            tas = Double.parseDouble( mTasEdit.getText().toString() );
            oat = Double.parseDouble( mOatEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( tas != Double.MAX_VALUE && oat != Double.MAX_VALUE ) {
            double mach = tas/( 38.967854*Math.sqrt( oat+273.15 ) );
            mMachEdit.setText( String.format( Locale.US, "%.2f", mach ) );
        } else {
            mMachEdit.setText( "" );
        }
    }

}
