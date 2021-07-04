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

public class DescentRateFragment extends E6bFragmentBase {

    private TextInputLayout mInitAltEdit;
    private TextInputLayout mCrossAltEdit;
    private TextInputLayout mGsEdit;
    private TextInputLayout mFixDistanceEdit;
    private TextInputLayout mDescentRateEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_descent_rate_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mInitAltEdit = findViewById( R.id.e6b_edit_initial_alt );
        mCrossAltEdit = findViewById( R.id.e6b_edit_crossing_alt );
        mGsEdit = findViewById( R.id.e6b_edit_gs );
        mFixDistanceEdit = findViewById( R.id.e6b_edit_fix_distance );
        mDescentRateEdit = findViewById( R.id.e6b_edit_descent_rate );

        addEditField( mFixDistanceEdit );
        addEditField( mGsEdit );
        addEditField( mCrossAltEdit );
        addEditField( mInitAltEdit );
        addReadOnlyField( mDescentRateEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Find the required rate of descent or climb to arrive at a fix."
            + " A positive values indicates descent. A negative value indicates"
            + " a climb to the crossing altitude";
    }

    @Override
    protected void processInput() {
        try {
            double initAlt = parseDouble( mInitAltEdit );
            double crossAlt = parseDouble( mCrossAltEdit );
            double gs = parseDouble( mGsEdit );
            double fixDist = parseDouble( mFixDistanceEdit );
            double descentRate = ( initAlt-crossAlt )/( ( fixDist/gs )*60 );
            showValue( mDescentRateEdit, descentRate );
        } catch ( NumberFormatException ignored ) {
            clearEditText( mDescentRateEdit );
        }
    }

}
