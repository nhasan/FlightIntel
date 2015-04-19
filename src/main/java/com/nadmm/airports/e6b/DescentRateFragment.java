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

public class DescentRateFragment extends FragmentBase {

    EditText mInitAltEdit;
    EditText mCrossAltEdit;
    EditText mGsEdit;
    EditText mFixDistEdit;
    EditText mDscntRateEdit;

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
        return inflate( R.layout.e6b_descent_rate_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "Find the required rate of descent or climb to arrive at a fix." +
                " A positive values indicates descent. A negative value indicates a" +
                " climb to the crossing altitude" );

        mInitAltEdit = (EditText) findViewById( R.id.e6b_edit_initial_alt );
        mCrossAltEdit = (EditText) findViewById( R.id.e6b_edit_crossing_alt );
        mGsEdit = (EditText) findViewById( R.id.e6b_edit_gs );
        mFixDistEdit = (EditText) findViewById( R.id.e6b_edit_fix_distance );
        mDscntRateEdit = (EditText) findViewById( R.id.e6b_edit_descent_rate );

        mInitAltEdit.addTextChangedListener( mTextWatcher );
        mCrossAltEdit.addTextChangedListener( mTextWatcher );
        mGsEdit.addTextChangedListener( mTextWatcher );
        mFixDistEdit.addTextChangedListener( mTextWatcher );
    }

    private void processInput() {
        double initAlt = Double.MAX_VALUE;
        double crossAlt = Double.MAX_VALUE;
        double gs = Double.MAX_VALUE;
        double fixDist = Double.MAX_VALUE;
        double dscntRate = Double.MAX_VALUE;

        try {
            initAlt = Double.parseDouble( mInitAltEdit.getText().toString() );
            crossAlt = Double.parseDouble( mCrossAltEdit.getText().toString() );
            gs = Double.parseDouble( mGsEdit.getText().toString() );
            fixDist = Double.parseDouble( mFixDistEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( initAlt != Double.MAX_VALUE && crossAlt != Double.MAX_VALUE
                && gs != Double.MAX_VALUE && fixDist != Double.MAX_VALUE ) {
            dscntRate = ( initAlt-crossAlt )/( ( fixDist/gs )*60 );
            mDscntRateEdit.setText( String.format( Locale.US, "%d", Math.round( dscntRate ) ) );
        } else {
            mDscntRateEdit.setText( "" );
        }
    }

}
