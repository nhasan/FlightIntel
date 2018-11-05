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

public class TrueAltitudeFragment extends E6bFragmentBase {

    private EditText mIaEdit;
    private EditText mOatEdit;
    private EditText mAltimeterEdit;
    private EditText mStationAltitudeEdit;
    private EditText mTrueAltitudeEdit;

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
        mAltimeterEdit = findViewById( R.id.e6b_edit_altimeter );
        mStationAltitudeEdit = findViewById( R.id.e6b_edit_station_altitude );
        mTrueAltitudeEdit = findViewById( R.id.e6b_edit_ta );

        mIaEdit.addTextChangedListener( mTextWatcher );
        mOatEdit.addTextChangedListener( mTextWatcher );
        mAltimeterEdit.addTextChangedListener( mTextWatcher );
        mStationAltitudeEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Use altitude of the wx station whose altimeter setting is being used";
    }

    @Override
    protected void processInput() {
        double ia = Double.MAX_VALUE;
        double oat = Double.MAX_VALUE;
        double altimeter = Double.MAX_VALUE;
        double stationAltitude = Double.MAX_VALUE;

        try {
            ia = Double.parseDouble( mIaEdit.getText().toString() );
            oat = Double.parseDouble( mOatEdit.getText().toString() );
            altimeter = Double.parseDouble( mAltimeterEdit.getText().toString() );
            stationAltitude = Double.parseDouble( mStationAltitudeEdit.getText().toString() );

            if ( ia > 65620 ) {
                mIaEdit.setError( "Enter a value between 0 and 65,620" );
                ia = Double.MAX_VALUE;
            }
        } catch ( NumberFormatException ignored ) {
        }

        if ( ia != Double.MAX_EXPONENT && oat != Double.MAX_EXPONENT
                && altimeter != Double.MAX_EXPONENT && stationAltitude != Double.MAX_VALUE ) {
            double isaTempC = -56.5;
            if ( ia <= 36089.24 ) {
                isaTempC = 15.0 - 0.0019812*ia;
            }
            double ta = ia + ( ( ia-stationAltitude )*( oat-isaTempC )/( 273.15+oat ) );
            mTrueAltitudeEdit.setText( String.valueOf( Math.round( ta ) ) );
        } else {
            mTrueAltitudeEdit.setText( "" );
        }
    }

}
