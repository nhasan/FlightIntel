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

public class TrueAirspeedFragment extends FragmentBase {

    private EditText mIndicatedAirSpeedEdit;
    private EditText mIndicatedAltitudeEdit;
    private EditText mAltimeterEdit;
    private EditText mTemperatureEdit;
    private EditText mTrueAirSpeedEdit;

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
        return inflate( R.layout.e6b_altimetry_tas_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        String title = getArguments().getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "True airspeed is affected by density altitude. True airspeed" +
                " exceeds indicated airspeed as density altitude increases." );

        mIndicatedAirSpeedEdit = (EditText) findViewById( R.id.e6b_edit_ias );
        mIndicatedAltitudeEdit = (EditText) findViewById( R.id.e6b_edit_ia );
        mAltimeterEdit = (EditText) findViewById( R.id.e6b_edit_altimeter );
        mTemperatureEdit = (EditText) findViewById( R.id.e6b_edit_temperature_c );
        mTrueAirSpeedEdit = (EditText) findViewById( R.id.e6b_edit_tas );

        mIndicatedAirSpeedEdit.addTextChangedListener( mTextWatcher );
        mIndicatedAltitudeEdit.addTextChangedListener( mTextWatcher );
        mAltimeterEdit.addTextChangedListener( mTextWatcher );
        mTemperatureEdit.addTextChangedListener( mTextWatcher );
    }

    void processInput() {
        double ias = -1;
        double ia = -1;
        double altimeter = -1;
        double temperatureC = -1;

        try {
            ias = Double.parseDouble( mIndicatedAirSpeedEdit.getText().toString() );
            ia = Double.parseDouble( mIndicatedAltitudeEdit.getText().toString() );
            altimeter = Double.parseDouble( mAltimeterEdit.getText().toString() );
            temperatureC = Double.parseDouble( mTemperatureEdit.getText().toString() );
        } catch ( NumberFormatException e ) {
        }

        if ( ias != -1 && ia != -1 && altimeter != -1 && temperatureC != -1 ) {
            double delta = 145442.2*( 1-Math.pow( altimeter/29.92126, 0.190261 ) );
            double pa = ia+delta;
            double stdTempK = 15.0-( 0.0019812*ia )+273.15;
            double actTempK = temperatureC+273.15;
            double da = pa+( stdTempK/0.0019812 )*( 1-Math.pow( stdTempK/actTempK, 0.234969 ) );
            double factor = Math.sqrt( Math.pow( ( stdTempK-( da*0.0019812 ) )/stdTempK,
                    1/0.234969 ) );
            long tas = Math.round( ias/factor )-1;
            mTrueAirSpeedEdit.setText( String.valueOf( tas ) );
        } else {
            mTrueAirSpeedEdit.setText( "" );
        }
    }

}
