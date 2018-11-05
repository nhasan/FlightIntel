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

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

import java.util.Locale;

public class FuelCalcsFragment extends E6bFragmentBase {

    private EditText mFuelTotalEdit;
    private EditText mFuelRateEdit;
    private EditText mTimeEdit;
    private EditText mTime2Edit;

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
        double fuelTotal = Double.MAX_VALUE;
        double fuelRate = Double.MAX_VALUE;
        double endurance = Double.MAX_VALUE;

        if ( mMenuId == R.id.E6B_FUEL_ENDURANCE ) {
            try {
                fuelTotal = Double.parseDouble( mFuelTotalEdit.getText().toString() );
                fuelRate = Double.parseDouble( mFuelRateEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }

            if ( fuelTotal != Double.MAX_VALUE && fuelRate != Double.MAX_VALUE ) {
                endurance = fuelTotal/( fuelRate/60 );
                mTimeEdit.setText( String.format( Locale.US, "%.0f", endurance ) );
            } else {
                mTimeEdit.setText( "" );
            }
        } else if ( mMenuId == R.id.E6B_FUEL_BURN_RATE ) {
            try {
                fuelTotal = Double.parseDouble( mFuelTotalEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }
            try {
                endurance = Double.parseDouble( mTimeEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }


            if ( fuelTotal != Double.MAX_VALUE && endurance != Double.MAX_VALUE ) {
                fuelRate = fuelTotal/( endurance/60 );
                mFuelRateEdit.setText( String.format( Locale.US, "%.1f", fuelRate  ) );
            } else {
                mFuelRateEdit.setText( "" );
            }
        } else if ( mMenuId == R.id.E6B_FUEL_TOTAL_BURNED ) {
            try {
                fuelRate = Double.parseDouble( mFuelRateEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }
            try {
                endurance = Double.parseDouble( mTimeEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }


            if ( fuelRate != Double.MAX_VALUE && endurance != Double.MAX_VALUE ) {
                fuelTotal = ( endurance/60 )*fuelRate;
                mFuelTotalEdit.setText( String.format( Locale.US, "%.1f", fuelTotal  ) );
            } else {
                mFuelTotalEdit.setText( "" );
            }
        }

        if ( endurance != Double.MAX_VALUE ) {
            mTime2Edit.setText( getFormattedTime( endurance ) );
        } else {
            mTime2Edit.setText( "" );
        }
    }

    private void setupUi() {
        mFuelTotalEdit = findViewById( R.id.e6b_edit_total_fuel );
        mFuelRateEdit = findViewById( R.id.e6b_edit_burn_rate );
        mTimeEdit = findViewById( R.id.e6b_edit_time );
        mTime2Edit = findViewById( R.id.e6b_edit_time2 );

        if ( mMenuId == R.id.E6B_FUEL_ENDURANCE ) {
            mFuelTotalEdit.addTextChangedListener( mTextWatcher );
            mFuelTotalEdit.setHint( R.string.input_gal );
            mFuelRateEdit.addTextChangedListener( mTextWatcher );
            mFuelRateEdit.setHint( R.string.input_gph );
            mTimeEdit.setFocusable( false );
            mTimeEdit.setTypeface( null, Typeface.BOLD );
            mTimeEdit.setHint( R.string.min );
        } else if ( mMenuId == R.id.E6B_FUEL_BURN_RATE ) {
            mFuelTotalEdit.addTextChangedListener( mTextWatcher );
            mFuelTotalEdit.setHint( R.string.input_gal );
            mFuelRateEdit.setFocusable( false );
            mFuelRateEdit.setTypeface( null, Typeface.BOLD );
            mFuelRateEdit.setHint( R.string.gph );
            mTimeEdit.addTextChangedListener( mTextWatcher );
            mTimeEdit.setHint( R.string.input_min );
        } else if ( mMenuId == R.id.E6B_FUEL_TOTAL_BURNED ) {
            mFuelTotalEdit.setFocusable( false );
            mFuelTotalEdit.setTypeface( null, Typeface.BOLD );
            mFuelTotalEdit.setHint( R.string.gal );
            mFuelRateEdit.addTextChangedListener( mTextWatcher );
            mFuelRateEdit.setHint( R.string.input_gph );
            mTimeEdit.addTextChangedListener( mTextWatcher );
            mTimeEdit.setHint( R.string.input_min );
        }
    }

    private String getFormattedTime( double time ) {
        long secs = Math.round( time*60 );
        long hrs = secs/(60*60);
        secs -= hrs*60*60;
        long mins = secs/60;
        return String.format( Locale.US, "%d h %d m", hrs, mins );
    }

}
