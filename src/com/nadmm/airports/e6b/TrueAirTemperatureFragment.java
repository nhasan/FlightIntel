/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

public class TrueAirTemperatureFragment extends FragmentBase {

    private EditText mIatEdit;
    private EditText mRecoveryFactorEdit;
    private EditText mTasEdit;
    private EditText mOatEdit;

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
        return inflate( R.layout.e6b_altimetry_tat_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        String title = getArguments().getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "The recovery factor depends on installation, and is usually" +
        		" in the range of 0.95 to 1.0, but can be as low as 0.7" );

        mIatEdit = (EditText) findViewById( R.id.e6b_edit_iat );
        mRecoveryFactorEdit = (EditText) findViewById( R.id.e6b_edit_recovery_factor );
        mTasEdit = (EditText) findViewById( R.id.e6b_edit_tas );
        mOatEdit = (EditText) findViewById( R.id.e6b_edit_oat );

        // Use a comon case default
        mRecoveryFactorEdit.setText( "0.95" );

        mIatEdit.addTextChangedListener( mTextWatcher );
        mRecoveryFactorEdit.addTextChangedListener( mTextWatcher );
        mTasEdit.addTextChangedListener( mTextWatcher );
    }

    private void processInput() {
        double iat = Double.MAX_VALUE;
        double k = Double.MAX_VALUE;
        double tas = Double.MAX_VALUE;
        double oat = Double.MAX_VALUE;

        try {
            iat = Double.parseDouble( mIatEdit.getText().toString() );
            k = Double.parseDouble( mRecoveryFactorEdit.getText().toString() );
            tas = Double.parseDouble( mTasEdit.getText().toString() );
        } catch ( NumberFormatException e ) {
        }

        if ( iat != Double.MAX_VALUE && k != Double.MAX_VALUE && tas != Double.MAX_VALUE ) {
            oat = iat - k*Math.pow( tas, 2 )/7592;
            mOatEdit.setText( String.format( Locale.US, "%.1f", oat ) );
        } else {
            mOatEdit.setText( "" );
        }
    }

}
