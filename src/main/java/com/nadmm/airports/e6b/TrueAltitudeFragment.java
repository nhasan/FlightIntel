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

public class TrueAltitudeFragment extends E6bFragmentBase {

    private TextInputLayout mIaEdit;
    private TextInputLayout mOatEdit;
    private TextInputLayout mStationAltitudeEdit;
    private TextInputLayout mTrueAltitudeEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_ta_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mIaEdit = findViewById( R.id.e6b_edit_ia );
        mOatEdit = findViewById( R.id.e6b_edit_oat );
        mStationAltitudeEdit = findViewById( R.id.e6b_edit_station_altitude );
        mTrueAltitudeEdit = findViewById( R.id.e6b_edit_ta );

        addEditField( mIaEdit );
        addEditField( mOatEdit );
        addEditField( mStationAltitudeEdit );
        addReadOnlyField( mTrueAltitudeEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Use altitude of the wx station whose altimeter setting is being used";
    }

    @Override
    protected void processInput() {
        try {
            double ia = parseDouble( mIaEdit );
            double oat = parseDouble( mOatEdit );
            double stationAltitude = parseDouble( mStationAltitudeEdit );

            if ( ia > 65620 ) {
                mIaEdit.setError( "Valid values: 0 to 65,620" );
                throw new NumberFormatException();
            } else {
                mIaEdit.setError( "" );
            }

            double isaTempC = -56.5;
            if ( ia <= 36089.24 ) {
                isaTempC = 15.0 - 0.0019812*ia;
            }
            double ta = Math.round( ia + ( ( ia-stationAltitude )*( oat-isaTempC )/( 273.15+oat ) ) );
            showValue( mTrueAltitudeEdit, ta );
        } catch ( NumberFormatException ignored ) {
            clearEditText( mTrueAltitudeEdit );
        }
    }

}
