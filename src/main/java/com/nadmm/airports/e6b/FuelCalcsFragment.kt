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
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

public class FuelCalcsFragment extends E6bFragmentBase {

    private TextInputLayout mEdit1;
    private TextInputLayout mEdit2;
    private TextInputLayout mEdit3;

    private long mMenuId;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_fuel_calcs_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mMenuId = getArguments().getLong( ListMenuFragment.MENU_ID );
        setupUi();
        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "You can directly substitute Gallons with Pounds for Jet fuel";
    }

    @Override
    protected void processInput() {
        try {
            if ( mMenuId == R.id.E6B_FUEL_ENDURANCE ) {
                double fuelTotal = parseDouble( mEdit1 );
                double fuelRate = parseDouble( mEdit2 );
                double endurance = fuelTotal/( fuelRate/60 );
                showValue( mEdit3, endurance );
            } else if ( mMenuId == R.id.E6B_FUEL_TOTAL_BURNED ) {
                double fuelRate = parseDouble( mEdit1 );
                double endurance = parseDouble( mEdit2 );
                double fuelTotal = ( endurance/60 )*fuelRate;
                showValue( mEdit3, fuelTotal );
            } else if ( mMenuId == R.id.E6B_FUEL_BURN_RATE ) {
                double fuelTotal = parseDouble( mEdit1 );
                double endurance = parseDouble( mEdit2 );
                double fuelRate = fuelTotal/( endurance/60 );
                showDecimalValue( mEdit3, fuelRate );
            }
        } catch ( NumberFormatException ignored ) {
            clearEditText( mEdit3 );
        }
    }

    private void setupUi() {
        TextView label1 = findViewById( R.id.e6b_label_value1 );
        TextView label2 = findViewById( R.id.e6b_label_value2 );
        TextView label3 = findViewById( R.id.e6b_label_value3 );
        mEdit1 = findViewById( R.id.e6b_edit_value1 );
        mEdit2 = findViewById( R.id.e6b_edit_value2 );
        mEdit3 = findViewById( R.id.e6b_edit_value3 );

        if ( mMenuId == R.id.E6B_FUEL_ENDURANCE ) {
            label1.setText( R.string.total_fuel );
            addEditField( mEdit1, R.string.gal );
            label2.setText( R.string.burn_rate );
            addEditField( mEdit2, R.string.gph );
            label3.setText( R.string.endurance );
            addReadOnlyField( mEdit3, R.string.min );
        } else if ( mMenuId == R.id.E6B_FUEL_BURN_RATE ) {
            label1.setText( R.string.total_fuel );
            addEditField( mEdit1, R.string.gal );
            label2.setText( R.string.endurance );
            addEditField( mEdit2, R.string.min );
            label3.setText( R.string.burn_rate );
            addReadOnlyField( mEdit3, R.string.gph );
        } else if ( mMenuId == R.id.E6B_FUEL_TOTAL_BURNED ) {
            label1.setText( R.string.burn_rate );
            addEditField( mEdit1, R.string.gph );
            label2.setText( R.string.endurance );
            addEditField( mEdit2, R.string.min );
            label3.setText( R.string.total_fuel );
            addReadOnlyField( mEdit3, R.string.gal );
        }
    }

}
