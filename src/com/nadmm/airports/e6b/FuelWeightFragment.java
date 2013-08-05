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

import java.util.Locale;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

public class FuelWeightFragment extends FragmentBase {

    private Spinner mFuelTypes;
    private EditText mFuelTotal;
    private EditText mFuelWeight;

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

    private static abstract class FuelWeight {
        public abstract double lbsPerGallon();
    }

    private FuelWeight[] mFuels = new FuelWeight[] {
            
        new FuelWeight() {
    
            @Override
            public double lbsPerGallon() {
                return 6.08;
            }
    
            @Override
            public String toString() {
                return "100LL";
            }
        },

        new FuelWeight() {

            @Override
            public double lbsPerGallon() {
                return 6.71;
            }

            @Override
            public String toString() {
                return "JetA1";
            }
        },

        new FuelWeight() {

            @Override
            public double lbsPerGallon() {
                return 6.84;
            }

            @Override
            public String toString() {
                return "JetA";
            }
        },

        new FuelWeight() {

            @Override
            public double lbsPerGallon() {
                return 6.36;
            }

            @Override
            public String toString() {
                return "JetB";
            }
        },

        new FuelWeight() {

            @Override
            public double lbsPerGallon() {
                return 6.76;
            }

            @Override
            public String toString() {
                return "JP-5";
            }
        },

        new FuelWeight() {

            @Override
            public double lbsPerGallon() {
                return 6.76;
            }

            @Override
            public String toString() {
                return "JP-8";
            }
        }

    };
 
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflate( R.layout.e6b_fuel_weight_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        TextView msg = (TextView) findViewById( R.id.e6b_msg );
        msg.setText( "Fuel density used is valid at 15\u00B0C (59\u00B0F) and is an average" +
        		" value. Actual weight varies and depends on the API gravity of the batch" +
        		" and the ambient temperature. Use this calculator for only an estimate." );

        mFuelTypes = (Spinner) findViewById( R.id.e6b_fuel_types );
        ArrayAdapter<FuelWeight> adapter = new ArrayAdapter<FuelWeight>( getActivity(),
                R.layout.spinner_item, mFuels );
        adapter.setDropDownViewResource( R.layout.spinner_dropdown_item );
        mFuelTypes.setAdapter( adapter );

        mFuelTypes.setOnItemSelectedListener( new OnItemSelectedListener() {

            @Override
            public void onItemSelected( AdapterView<?> parent, View view, int pos, long id ) {
                processInput();
            }

            @Override
            public void onNothingSelected( AdapterView<?> arg0 ) {
            }
        } );

        mFuelTotal = (EditText) findViewById( R.id.e6b_edit_total_fuel );
        mFuelWeight = (EditText) findViewById( R.id.e6b_edit_total_weight );

        mFuelTotal.addTextChangedListener( mTextWatcher );
    }

    private void processInput() {
        double fuelTotal = Double.MAX_VALUE;

        try {
            fuelTotal = Double.parseDouble( mFuelTotal.getText().toString() );
        } catch ( NumberFormatException e ) {
        }

        if ( fuelTotal != Double.MAX_VALUE ) {
            FuelWeight fuel = (FuelWeight) mFuelTypes.getSelectedItem();
            double weight = fuelTotal*fuel.lbsPerGallon();
            mFuelWeight.setText( String.format( Locale.US, "%.0f", weight ) );
        } else {
            mFuelWeight.setText( "" );
        }
    }

}
