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

import java.util.Locale;

public class MachNumberFragment extends E6bFragmentBase {

    private EditText mTasEdit;
    private EditText mOatEdit;
    private EditText mMachEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_altimetry_mach_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mTasEdit = findViewById( R.id.e6b_edit_tas );
        mOatEdit = findViewById( R.id.e6b_edit_oat );
        mMachEdit = findViewById( R.id.e6b_edit_mach );

        mTasEdit.addTextChangedListener( mTextWatcher );
        mOatEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Speed of sound and hence Mach number varies directly with OAT";
    }

    @Override
    protected void processInput() {
        double tas = Double.MAX_VALUE;
        double oat = Double.MAX_VALUE;

        try {
            tas = Double.parseDouble( mTasEdit.getText().toString() );
            oat = Double.parseDouble( mOatEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( tas != Double.MAX_VALUE && oat != Double.MAX_VALUE ) {
            double mach = tas/( 38.967854*Math.sqrt( oat+273.15 ) );
            mMachEdit.setText( String.format( Locale.US, "%.2f", mach ) );
        } else {
            mMachEdit.setText( "" );
        }
    }

}
