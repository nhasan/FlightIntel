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

public class TopOfDescentFragment extends FragmentBase {

    EditText mInitAltEdit;
    EditText mDesiredAltEdit;
    EditText mGsEdit;
    EditText mDscntRateEdit;
    EditText mDistanceEdit;

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
        return inflate( R.layout.e6b_top_of_descent_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "Find the distance at which to start the descent to arrive at the" +
                " destination at the desired altitude." );

        mInitAltEdit = (EditText) findViewById( R.id.e6b_edit_initial_alt );
        mDesiredAltEdit = (EditText) findViewById( R.id.e6b_edit_desired_alt );
        mGsEdit = (EditText) findViewById( R.id.e6b_edit_gs );
        mDscntRateEdit = (EditText) findViewById( R.id.e6b_edit_descent_rate );
        mDistanceEdit = (EditText) findViewById( R.id.e6b_edit_distance );

        mInitAltEdit.addTextChangedListener( mTextWatcher );
        mDesiredAltEdit.addTextChangedListener( mTextWatcher );
        mGsEdit.addTextChangedListener( mTextWatcher );
        mDscntRateEdit.addTextChangedListener( mTextWatcher );
    }

    private void processInput() {
        double initAlt = Double.MAX_VALUE;
        double desiredAlt = Double.MAX_VALUE;
        double gs = Double.MAX_VALUE;
        double dscntRate = Double.MAX_VALUE;
        double distance = Double.MAX_VALUE;

        try {
            initAlt = Double.parseDouble( mInitAltEdit.getText().toString() );
            desiredAlt = Double.parseDouble( mDesiredAltEdit.getText().toString() );
            gs = Double.parseDouble( mGsEdit.getText().toString() );
            dscntRate = Double.parseDouble( mDscntRateEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( initAlt != Double.MAX_VALUE && desiredAlt != Double.MAX_VALUE
                && gs != Double.MAX_VALUE && dscntRate != Double.MAX_VALUE ) {
            distance = gs*( ( initAlt-desiredAlt )/( dscntRate*60 ) );
            mDistanceEdit.setText( String.format( Locale.US,  "%.1f", distance ) );
        } else {
            mDistanceEdit.setText( "" );
        }
    }

}
