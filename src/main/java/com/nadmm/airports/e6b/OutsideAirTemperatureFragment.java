/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.nadmm.airports.R;

public class OutsideAirTemperatureFragment extends E6bFragmentBase {

    private EditText mIatEdit;
    private EditText mRecoveryFactorEdit;
    private EditText mTasEdit;
    private EditText mOatEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_oat_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mIatEdit = findViewById( R.id.e6b_edit_iat );
        mRecoveryFactorEdit = findViewById( R.id.e6b_edit_recovery_factor );
        mTasEdit = findViewById( R.id.e6b_edit_tas );
        mOatEdit = findViewById( R.id.e6b_edit_oat );

        // Use a comon case default
        mRecoveryFactorEdit.setText( "0.95" );

        mIatEdit.addTextChangedListener( mTextWatcher );
        mRecoveryFactorEdit.addTextChangedListener( mTextWatcher );
        mTasEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "The recovery factor depends on installation, and is usually" +
                " in the range of 0.95 to 1.0, but can be as low as 0.7";
    }

    @Override
    protected void processInput() {
        double iat = Double.MAX_VALUE;
        double k = Double.MAX_VALUE;
        double tas = Double.MAX_VALUE;

        try {
            iat = Double.parseDouble( mIatEdit.getText().toString() );
            k = Double.parseDouble( mRecoveryFactorEdit.getText().toString() );
            tas = Double.parseDouble( mTasEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( iat != Double.MAX_VALUE && k != Double.MAX_VALUE && tas != Double.MAX_VALUE ) {
            double oat = iat - k*Math.pow( tas, 2 )/7592;
            mOatEdit.setText( String.valueOf( Math.round( oat ) ) );
        } else {
            mOatEdit.setText( "" );
        }
    }

}
