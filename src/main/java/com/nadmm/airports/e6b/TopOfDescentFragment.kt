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

public class TopOfDescentFragment extends E6bFragmentBase {

    private TextInputLayout mInitAltEdit;
    private TextInputLayout mDesiredAltEdit;
    private TextInputLayout mGsEdit;
    private TextInputLayout mDescentRateEdit;
    private TextInputLayout mFixDistanceEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_top_of_descent_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mInitAltEdit = findViewById( R.id.e6b_edit_initial_alt );
        mDesiredAltEdit = findViewById( R.id.e6b_edit_desired_alt );
        mGsEdit = findViewById( R.id.e6b_edit_gs );
        mDescentRateEdit = findViewById( R.id.e6b_edit_descent_rate );
        mFixDistanceEdit = findViewById( R.id.e6b_edit_fix_distance );

        addEditField( mInitAltEdit );
        addEditField( mDesiredAltEdit );
        addEditField( mGsEdit );
        addEditField( mDescentRateEdit );
        addReadOnlyField( mFixDistanceEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Find the distance at which to start the descent to arrive at the" +
                " destination at the desired altitude.";
    }

    @Override
    protected void processInput() {
        try {
            double initAlt = parseDouble( mInitAltEdit );
            double desiredAlt = parseDouble( mDesiredAltEdit );
            double gs = parseDouble( mGsEdit );
            double descentRate = parseDouble( mDescentRateEdit );
            double distance = gs*( ( initAlt-desiredAlt )/( descentRate*60 ) );
            showDecimalValue( mFixDistanceEdit, distance );
        } catch ( NumberFormatException ignored ) {
            clearEditText( mFixDistanceEdit );
        }
    }

}
