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

public class TrueAirspeedFragment extends E6bFragmentBase {

    private TextInputLayout mIndicatedAirSpeedEdit;
    private TextInputLayout mIndicatedAltitudeEdit;
    private TextInputLayout mAltimeterEdit;
    private TextInputLayout mTemperatureEdit;
    private TextInputLayout mTrueAirSpeedEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_tas_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mIndicatedAirSpeedEdit = findViewById( R.id.e6b_edit_ias );
        mIndicatedAltitudeEdit = findViewById( R.id.e6b_edit_altitude );
        mAltimeterEdit = findViewById( R.id.e6b_edit_altimeter );
        mTemperatureEdit = findViewById( R.id.e6b_edit_temperature_c );
        mTrueAirSpeedEdit = findViewById( R.id.e6b_edit_tas );

        addEditField( mIndicatedAirSpeedEdit );
        addEditField( mIndicatedAltitudeEdit );
        addEditField( mAltimeterEdit );
        addEditField( mTemperatureEdit );
        addReadOnlyField( mTrueAirSpeedEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "True airspeed is affected by density altitude. True airspeed" +
                " exceeds indicated airspeed as density altitude increases.";
    }

    @Override
    protected void processInput() {
        try {
            double ias = parseDouble( mIndicatedAirSpeedEdit );
            double altitude = parseDouble( mIndicatedAltitudeEdit );
            double altimeter = parseDouble( mAltimeterEdit );
            double temperatureC = parseDouble( mTemperatureEdit );

            double delta = 145442.2*( 1-Math.pow( altimeter/29.92126, 0.190261 ) );
            double pa = altitude+delta;
            double stdTempK = 15.0-( 0.0019812*altitude )+273.15;
            double actTempK = temperatureC+273.15;
            double da = pa+( stdTempK/0.0019812 )*( 1-Math.pow( stdTempK/actTempK, 0.234969 ) );
            double factor = Math.sqrt( Math.pow( ( stdTempK-( da*0.0019812 ) )/stdTempK,
                    1/0.234969 ) );
            long tas = Math.round( ias/factor )-1;
            showValue( mTrueAirSpeedEdit, tas );
        } catch ( NumberFormatException ignored ) {
            clearEditText( mTrueAirSpeedEdit );
        }
    }

}
