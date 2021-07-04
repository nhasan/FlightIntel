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

public class ClimbRateFragment extends E6bFragmentBase {

    private TextInputLayout mClimbGradEdit;
    private TextInputLayout mGsEdit;
    private TextInputLayout mClimbRateEdit;
    private TextInputLayout mClimbGradPctEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_climb_rate_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mClimbGradEdit = findViewById( R.id.e6b_edit_climb_grad );
        mGsEdit = findViewById( R.id.e6b_edit_gs );
        mClimbRateEdit = findViewById( R.id.e6b_edit_climb_rate );
        mClimbGradPctEdit  = findViewById( R.id.e6b_edit_climb_grad_pct );

        addEditField( mClimbGradEdit );
        addEditField( mGsEdit );
        addReadOnlyField( mClimbRateEdit );
        addReadOnlyField( mClimbGradPctEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Find the minimum required climb rate for a departure procedure";
    }

    @Override
    protected void processInput() {
        try {
            double climbGrad = parseDouble( mClimbGradEdit );
            double gs = parseDouble( mGsEdit );
            double climbRate = climbGrad*gs/60;
            double climbGradPct = ( climbGrad/6076.115 )*100;
            showValue( mClimbRateEdit, climbRate );
            showDecimalValue( mClimbGradPctEdit, climbGradPct );
        } catch ( NumberFormatException ignored ) {
            clearEditText( mClimbRateEdit );
            clearEditText( mClimbGradPctEdit );
        }
    }

}
