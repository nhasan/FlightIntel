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

public class IsaFragment extends E6bFragmentBase {

    private TextInputLayout mAltitudeEdit;
    private TextInputLayout mTemperatureCEdit;
    private TextInputLayout mTemperatureFEdit;
    private TextInputLayout mPressureInEdit;
    private TextInputLayout mPressureMbEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_isa_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mAltitudeEdit = findViewById( R.id.e6b_edit_altitude );
        mTemperatureCEdit = findViewById( R.id.e6b_edit_temperature_c );
        mTemperatureFEdit = findViewById( R.id.e6b_edit_temperature_f );
        mPressureInEdit = findViewById( R.id.e6b_edit_pressure_inHg );
        mPressureMbEdit = findViewById( R.id.e6b_edit_pressure_mb );

        addEditField( mAltitudeEdit );
        addReadOnlyField( mTemperatureCEdit );
        addReadOnlyField( mTemperatureFEdit );
        addReadOnlyField( mPressureInEdit );
        addReadOnlyField( mPressureMbEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "ISA temperature lapse rate is 1.9812\u00B0C/1,000 ft" +
                " or 3.56\u00B0F/1,000 ft upto 36,090 ft, then constant at" +
                " -56.5\u00B0C or -69.7\u00B0F upto 65,620 ft";
    }

    @Override
    protected void processInput() {
        try {
            double altitude = parseAltitude( mAltitudeEdit );

            double isaTempC = -56.5;
            if ( altitude <= 36089.24 ) {
                isaTempC = 15.0 - 0.0019812*altitude;
            }
            double isaTempF = ( isaTempC*9/5 )+32;
            showDecimalValue( mTemperatureCEdit, isaTempC );
            showDecimalValue( mTemperatureFEdit, isaTempF );

            double isaPressureInHg;
            if ( altitude < 36089.24 ) {
                isaPressureInHg = 29.92126*Math.pow( 1-6.8755856e-6*altitude, 5.2558797 );
            } else {
                altitude -= 36089.24;
                isaPressureInHg = 29.92126*0.2233609*Math.exp( -4.806346*10e-5*altitude );
            }
            double isaPressureMbar = isaPressureInHg*33.863753;
            showDecimalValue( mPressureInEdit, isaPressureInHg, 2 );
            showDecimalValue( mPressureMbEdit, isaPressureMbar );

        } catch ( NumberFormatException ignored ) {
            clearEditText( mTemperatureCEdit );
            clearEditText( mTemperatureFEdit );
            clearEditText( mPressureInEdit );
            clearEditText( mPressureMbEdit );
        }
    }

}
