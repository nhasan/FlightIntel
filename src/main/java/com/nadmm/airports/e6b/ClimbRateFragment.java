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

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.R;

import java.util.Locale;

public class ClimbRateFragment extends E6bFragmentBase {

    private EditText mClimbGradEdit;
    private EditText mGsEdit;
    private EditText mClimbRateEdit;
    private EditText mClimbGradPctEdit;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_climb_rate_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        TextInputLayout layout;
        layout = findViewById( R.id.e6b_edit_climb_grad );
        mClimbGradEdit = layout.getEditText();
        layout = findViewById( R.id.e6b_edit_gs );
        mGsEdit = layout.getEditText();
        layout = findViewById( R.id.e6b_edit_climb_rate );
        mClimbRateEdit = layout.getEditText();
        setEditTextReadOnly( mClimbRateEdit );
        layout  = findViewById( R.id.e6b_edit_climb_grad_pct );
        mClimbGradPctEdit = layout.getEditText();
        setEditTextReadOnly( mClimbGradPctEdit );

        mClimbGradEdit.addTextChangedListener( mTextWatcher );
        mGsEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Find the minimum required climb rate for a departure procedure";
    }

    @Override
    protected void processInput() {
        double climbGrad = Double.MAX_VALUE;
        double gs = Double.MAX_VALUE;

        try {
            climbGrad = Double.parseDouble( mClimbGradEdit.getText().toString() );
            gs = Double.parseDouble( mGsEdit.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }

        if ( climbGrad != Double.MAX_VALUE && gs != Double.MAX_VALUE ) {
            double climbRate = climbGrad*gs/60;
            double climbGradPct = ( climbGrad/6076.115 )*100;
            mClimbRateEdit.setText( String.format( Locale.US, "%d", Math.round( climbRate ) ) );
            mClimbGradPctEdit.setText( String.format( Locale.US, "%.1f", climbGradPct ) );
        } else {
            mClimbRateEdit.setText( "" );
            mClimbGradPctEdit.setText( "" );
        }
    }

}
