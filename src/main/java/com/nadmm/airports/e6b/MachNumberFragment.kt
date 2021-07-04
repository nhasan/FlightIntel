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

public class MachNumberFragment extends E6bFragmentBase {

    private TextInputLayout mTasEdit;
    private TextInputLayout mOatEdit;
    private TextInputLayout mMachEdit;

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

        addEditField( mTasEdit );
        addEditField( mOatEdit );
        addReadOnlyField( mMachEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Speed of sound and hence Mach number varies directly with OAT";
    }

    @Override
    protected void processInput() {
        try {
            double tas = parseDouble( mTasEdit );
            double oat = parseDouble( mOatEdit );
            double mach = tas/( 38.967854*Math.sqrt( oat+273.15 ) );
            showDecimalValue( mMachEdit, mach, 2 );
        } catch ( NumberFormatException ignored ) {
            clearEditText( mMachEdit );
        }
    }

}
