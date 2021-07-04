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
import android.widget.ArrayAdapter;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FuelWeightFragment extends E6bFragmentBase {

    private TextInputLayout mFuelTotal;
    private TextInputLayout mFuelWeight;
    private MaterialAutoCompleteTextView mTextView;

    private final Map<String, Double> mFuels = new HashMap<String, Double>() {{
        put( "100LL", 6.08 );
        put( "Jet A1", 6.71 );
        put( "Jet A", 6.84 );
        put( "Jet B", 6.36 );
        put( "JP-5", 6.76 );
        put( "JP-8", 6.76 );
    }};

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_fuel_weight_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        String [] names = mFuels.keySet().toArray( new String[ 0 ] );
        Arrays.sort( names );
        ArrayAdapter<String> adapter = new ArrayAdapter<>( getActivity(),
                R.layout.list_item, names );
        TextInputLayout fuelTypes = findViewById( R.id.e6b_fuel_types );
        mTextView = (MaterialAutoCompleteTextView) fuelTypes.getEditText();
        mTextView.setAdapter( adapter );
        mTextView.setOnItemClickListener( ( parent, view, position, id ) -> processInput() );

        mFuelTotal = findViewById( R.id.e6b_edit_total_fuel );
        mFuelWeight = findViewById( R.id.e6b_edit_total_weight );

        addEditField( mFuelTotal );
        addReadOnlyField( mFuelWeight );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return "Fuel density used is valid at 15\u00B0C (59\u00B0F) and is an average" +
                " value. Actual weight varies and depends on the API gravity of the batch" +
                " and the ambient temperature. Use this calculator for only an estimate.";
    }

    @Override
    protected void processInput() {
        try {
            double fuelTotal = parseDouble( mFuelTotal );
            String name = mTextView.getText().toString();
            if ( !name.isEmpty() ) {
                double weight = fuelTotal * mFuels.get( name );
                showDecimalValue( mFuelWeight, weight );
            }
        } catch ( RuntimeException e ) {
            clearEditText( mFuelWeight );
        }
    }

}
