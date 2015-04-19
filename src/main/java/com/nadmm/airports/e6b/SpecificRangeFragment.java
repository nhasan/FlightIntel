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

public class SpecificRangeFragment extends FragmentBase {

    private EditText mFuelTotalEdit;
    private EditText mFuelRateEdit;
    private EditText mGsEdit;
    private EditText mRangeEdit;

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
        return inflate( R.layout.e6b_specific_range_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "You can directly substitute Gallons with Pounds for Jet fuel" );

        mFuelTotalEdit = (EditText) findViewById( R.id.e6b_edit_total_fuel );
        mFuelRateEdit = (EditText) findViewById( R.id.e6b_edit_burn_rate );
        mGsEdit = (EditText) findViewById( R.id.e6b_edit_gs );
        mRangeEdit = (EditText) findViewById( R.id.e6b_edit_range );

        mFuelTotalEdit.addTextChangedListener( mTextWatcher );
        mFuelRateEdit.addTextChangedListener( mTextWatcher );
        mGsEdit.addTextChangedListener( mTextWatcher );
    }

    private void processInput() {
        double fuelTotal = Double.MAX_VALUE;
        double fuelRate = Double.MAX_VALUE;
        double gs = Double.MAX_VALUE;

        try {
            fuelTotal = Double.parseDouble( mFuelTotalEdit.getText().toString() );
            fuelRate = Double.parseDouble( mFuelRateEdit.getText().toString() );
            gs = Double.parseDouble( mGsEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( fuelTotal != Double.MAX_VALUE && fuelRate != Double.MAX_VALUE
                && gs != Double.MAX_VALUE ) {
            double range = ( fuelTotal/fuelRate )*gs;
            mRangeEdit.setText( String.valueOf( Math.round( range ) ) );
        } else {
            mRangeEdit.setText( "" );
        }
    }

}
