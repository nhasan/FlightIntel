/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2021 Nadeem Hasan <nhasan@nadmm.com>
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

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.R;

public class AltitudesFragment extends E6bFragmentBase {

    private TextInputLayout mElevationEdit;
    private TextInputLayout mAltimeterEdit;
    private TextInputLayout mTemperatureEdit;
    private TextInputLayout mDewpointEdit;
    private TextInputLayout mPressureAltitudeEdit;
    private TextInputLayout mDensityAltitudeEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_altitudes_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mElevationEdit = findViewById( R.id.e6b_edit_elevation );
        mAltimeterEdit = findViewById( R.id.e6b_edit_altimeter_inhg );
        mTemperatureEdit = findViewById( R.id.e6b_edit_temperature_c );
        mDewpointEdit = findViewById( R.id.e6b_edit_dewpoint_c );
        mPressureAltitudeEdit = findViewById( R.id.e6b_edit_pa );
        mDensityAltitudeEdit = findViewById( R.id.e6b_edit_da );

        addEditField( mElevationEdit );
        addEditField( mAltimeterEdit );
        addEditField( mTemperatureEdit );
        addEditField( mDewpointEdit );
        addReadOnlyField( mPressureAltitudeEdit );
        addReadOnlyField( mDensityAltitudeEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "At sea level on a standard day, temperature is 15\u00B0C or 59\u00B0F"
            + " and pressure is 29.92126 inHg or 1013.25 mB";
    }

    @Override
    protected void processInput() {
        try {
            long elevation = parseLong( mElevationEdit );
            double altimeterHg = parseDouble( mAltimeterEdit );
            double altimeterMb = 33.8639*altimeterHg;
            double temperatureC = parseDouble( mTemperatureEdit );
            double dewPointC = parseDouble( mDewpointEdit );
            double temperatureK = temperatureC+273.16;

            // Source: https://www.weather.gov/media/epz/wxcalc/pressureAltitude.pdf
            long pa = elevation+Math.round((1-Math.pow( altimeterMb/1013.25, 0.190284 ))*145366.45);
            showValue( mPressureAltitudeEdit, pa );

            // Source: https://www.weather.gov/media/epz/wxcalc/densityAltitude.pdf
            // Calculate vapor pressure first
            double e = 6.11 * Math.pow( 10, ( 7.5*dewPointC/( 237.7+dewPointC ) ) );
            // Next, calculate virtual temperature in Kelvin
            double tv = temperatureK/( 1-( e/altimeterMb )*( 1-0.622 ) );
            // Convert Kelvin to Rankin to use in the next step
            tv = ( 9*( tv-273.16 )/5+32 )+459.69;
            long da = elevation + Math.round( 145366*( 1-Math.pow( 17.326*altimeterHg/tv, 0.235 ) ) );
            showValue( mDensityAltitudeEdit, da );
        } catch ( NumberFormatException ignored ) {
            clearEditText( mPressureAltitudeEdit );
            clearEditText( mDensityAltitudeEdit );
        }
    }
}
