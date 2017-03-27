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

public class AltitudesFragment extends E6bFragmentBase {

    private EditText mElevationEdit;
    private EditText mAltimeterEdit;
    private EditText mTemperatureEdit;
    private EditText mPressureAltitudeEdit;
    private EditText mDensityAltitudeEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_altitudes_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mElevationEdit = (EditText) findViewById( R.id.e6b_edit_elevation );
        mAltimeterEdit = (EditText) findViewById( R.id.e6b_edit_altimeter_inhg );
        mTemperatureEdit = (EditText) findViewById( R.id.e6b_edit_temperature_c );
        mPressureAltitudeEdit = (EditText) findViewById( R.id.e6b_edit_pa );
        mDensityAltitudeEdit = (EditText) findViewById( R.id.e6b_edit_da );

        mElevationEdit.addTextChangedListener( mTextWatcher );
        mAltimeterEdit.addTextChangedListener( mTextWatcher );
        mTemperatureEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "At sea level on a standard day, temperature is 15\u00B0C or 59\u00B0F"
            + " and pressure is 29.92126 inHg or 1013.25 mB";
    }

    @Override
    protected void processInput() {
        double elevation = Double.MAX_VALUE;
        double altimeter = Double.MAX_VALUE;
        double temperatureC = Double.MAX_VALUE;

        try {
            elevation = Double.parseDouble( mElevationEdit.getText().toString() );
            altimeter = Double.parseDouble( mAltimeterEdit.getText().toString() );
            temperatureC = Double.parseDouble( mTemperatureEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( elevation != Double.MAX_VALUE
                && altimeter != Double.MAX_VALUE
                && temperatureC != Double.MAX_VALUE ) {
            double delta = 145442.2*( 1-Math.pow( altimeter/29.92126, 0.190261 ) );
            long pa = Math.round( elevation+delta );
            mPressureAltitudeEdit.setText( String.valueOf( pa ) );

            double stdTempK = 15.0-( 0.0019812*elevation )+273.15;
            double actTempK = temperatureC+273.15;
            long da = Math.round( pa
                    +( stdTempK/0.0019812 )*( 1-Math.pow( stdTempK/actTempK, 0.234969 ) ) );
            mDensityAltitudeEdit.setText( String.valueOf( da ) );
        } else {
            mPressureAltitudeEdit.setText( "" );
            mDensityAltitudeEdit.setText( "" );
        }
    }

}
