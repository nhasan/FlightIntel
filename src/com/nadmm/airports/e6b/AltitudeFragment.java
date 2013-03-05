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

public class AltitudeFragment extends FragmentBase {

    private EditText mElevationEdit;
    private EditText mAltimeterEdit;
    private EditText mTemperatureEdit;
    private EditText mPressureAltitudeEdit;
    private EditText mDensityAltitudeEdit;

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
        return inflate( R.layout.e6b_altimetry_altitude );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        String title = getArguments().getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_isa_msg );
        msg.setText( "At sea level on a standard day, temperature is 15\u00B0C or 59\u00B0F" +
        		" and pressure is 29.92126 inHg or 1013.25 mB" );

        mElevationEdit = (EditText) findViewById( R.id.e6b_elevation );
        mAltimeterEdit = (EditText) findViewById( R.id.e6b_altimeter_in );
        mTemperatureEdit = (EditText) findViewById( R.id.e6b_temperature_c );
        mPressureAltitudeEdit = (EditText) findViewById( R.id.e6b_pressure_altitude );
        mDensityAltitudeEdit = (EditText) findViewById( R.id.e6b_density_altitude );

        mElevationEdit.addTextChangedListener( mTextWatcher );
        mAltimeterEdit.addTextChangedListener( mTextWatcher );
        mTemperatureEdit.addTextChangedListener( mTextWatcher );
    }

    private void processInput() {
        long elevation = -1;
        double altimeter = -1;
        double temperature = -1;

        try {
            elevation = Long.valueOf( mElevationEdit.getText().toString() );
            altimeter = Double.valueOf( mAltimeterEdit.getText().toString() );
            temperature = Double.valueOf( mTemperatureEdit.getText().toString() );
        } catch ( NumberFormatException e ) {
        }

        if ( elevation != -1 && altimeter != -1 ) {
            long delta = Math.round( 145442.2*( 1-Math.pow( altimeter/29.92126, 0.190261 ) ) );
            long pressureAltitude = elevation+delta;
            mPressureAltitudeEdit.setText( String.valueOf( pressureAltitude ) );

            double stdTempK = 15.0-( 0.0019812*elevation )+273.15;
            double actTempK = temperature+273.15;
            long densityAltitude = Math.round( pressureAltitude
                    +( stdTempK/0.0019812 )*( 1-Math.pow( stdTempK/actTempK, 0.234969 ) ) );
            mDensityAltitudeEdit.setText( String.valueOf( densityAltitude ) );
        } else {
            mPressureAltitudeEdit.setText( "" );
            mDensityAltitudeEdit.setText( "" );
        }
    }

}
