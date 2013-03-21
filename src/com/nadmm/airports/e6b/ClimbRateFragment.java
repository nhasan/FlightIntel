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

public class ClimbRateFragment extends FragmentBase {

    private EditText mClimbGradEdit;
    private EditText mGsEdit;
    private EditText mClimbRateEdit;
    private EditText mClimbGradPctEdit;

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
        return inflate( R.layout.e6b_climb_rate_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "Find the minimum required climb rate for a departure procedure" );

        mClimbGradEdit = (EditText) findViewById( R.id.e6b_edit_climb_grad );
        mGsEdit = (EditText) findViewById( R.id.e6b_edit_gs );
        mClimbRateEdit = (EditText) findViewById( R.id.e6b_edit_climb_rate );
        mClimbGradPctEdit = (EditText) findViewById( R.id.e6b_edit_climb_grad_pct );

        mClimbGradEdit.addTextChangedListener( mTextWatcher );
        mGsEdit.addTextChangedListener( mTextWatcher );
    }

    private void processInput() {
        double climbGrad = Double.MAX_VALUE;
        double gs = Double.MAX_VALUE;
        double climbRate = Double.MAX_VALUE;
        double climbGradPct = Double.MAX_VALUE;

        try {
            climbGrad = Double.parseDouble( mClimbGradEdit.getText().toString() );
            gs = Double.parseDouble( mGsEdit.getText().toString() );
        } catch ( NumberFormatException e ) {
        }

        if ( climbGrad != Double.MAX_VALUE && gs != Double.MAX_VALUE ) {
            climbRate = climbGrad*gs/60;
            climbGradPct = ( climbGrad/6076.115 )*100;
            mClimbRateEdit.setText( String.format( Locale.US, "%d", Math.round( climbRate ) ) );
            mClimbGradPctEdit.setText( String.format( Locale.US, "%.1f", climbGradPct ) );
        } else {
            mClimbRateEdit.setText( "" );
            mClimbGradPctEdit.setText( "" );
        }
    }

}
