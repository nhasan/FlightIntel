/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

public class IsaFragment extends FragmentBase {

    private EditText mAltitudeEdit;
    private EditText mTemperatureCEdit;
    private EditText mTemperatureFEdit;
    private EditText mPressureInEdit;
    private EditText mPressureMbEdit;

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
        View view = inflater.inflate( R.layout.e6b_altimetry_isa_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        String title = getArguments().getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "ISA temperature lapse rate is 1.9812\u00B0C/1,000 ft" +
        		" or 3.56\u00B0F/1,000 ft upto 36,090 ft, then constant at" +
        		" -56.5\u00B0C or -69.7\u00B0F upto 65,620 ft" );

        mAltitudeEdit = (EditText) findViewById( R.id.e6b_edit_altitude );
        mTemperatureCEdit = (EditText) findViewById( R.id.e6b_edit_temperature_c );
        mTemperatureFEdit = (EditText) findViewById( R.id.e6b_edit_temperature_f );
        mPressureInEdit = (EditText) findViewById( R.id.e6b_edit_pressure_inHg );
        mPressureMbEdit = (EditText) findViewById( R.id.e6b_edit_pressure_mb );

        mAltitudeEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    private void processInput() {
        double altitude = Double.MAX_VALUE;

        try {
            altitude = Double.parseDouble( mAltitudeEdit.getText().toString() );
            if ( altitude < 0 || altitude > 65620 ) {
                mAltitudeEdit.setError( "Enter a value between 0 and 65,620" );
                altitude = Double.MAX_VALUE;
            }
        } catch ( NumberFormatException ignored ) {
        }

        if ( altitude != Double.MAX_VALUE ) {
            double isaTempC = -56.5;
            if ( altitude <= 36089.24 ) {
                isaTempC = 15.0 - 0.0019812*altitude;
            }
            double isaTempF = ( isaTempC*9/5 )+32;
            mTemperatureCEdit.setText( String.format( "%.1f", isaTempC ) );
            mTemperatureFEdit.setText( String.format( "%.1f", isaTempF ) );

            double isaPressureInHg;
            if ( altitude < 36089.24 ) {
                isaPressureInHg = 29.92126*Math.pow( 1-6.8755856e-6*altitude, 5.2558797 );
            } else {
                altitude -= 36089.24;
                isaPressureInHg = 29.92126*0.2233609*Math.exp( -4.806346*10e-5*altitude );
            }
            double isaPressureMbar = isaPressureInHg*33.863753;
            mPressureInEdit.setText( String.format( "%.2f", isaPressureInHg ) );
            mPressureMbEdit.setText( String.format( "%.1f", isaPressureMbar ) );
        } else {
            mTemperatureCEdit.setText( "" );
            mTemperatureFEdit.setText( "" );
            mPressureInEdit.setText( "" );
            mPressureMbEdit.setText( "" );
        }
    }

}
