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
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

public class TimeSpeedDistanceFragment extends E6bFragmentBase {

    private TextInputLayout mEdit1;
    private TextInputLayout mEdit2;
    private TextInputLayout mEdit3;

    private long mMode;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_time_speed_distance_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mMode = getArguments().getLong( ListMenuFragment.MENU_ID );
        setupUi();
        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return null;
    }

    @Override
    protected void processInput() {
        try {
            if ( mMode == R.id.E6B_TSD_TIME ) {
                double speed = parseDouble( mEdit1 );
                double distance = parseDouble( mEdit2 );
                double time = distance*60/speed;
                showValue( mEdit3, time );
            } else if ( mMode == R.id.E6B_TSD_SPEED ) {
                double distance = parseDouble( mEdit1 );
                double time = parseDouble( mEdit2 );
                double speed = distance/(time/60);
                showValue( mEdit3, speed );
            } else if ( mMode == R.id.E6B_TSD_DISTANCE ) {
                double speed = parseDouble( mEdit1 );
                double time = parseDouble( mEdit2 );
                double distance = ( time/60 )*speed;
                showDecimalValue( mEdit3, distance );
            }
        } catch ( NumberFormatException ignored ) {
            clearEditText( mEdit3 );
        }
    }

    private void setupUi() {
        TextView mLabel1 = findViewById( R.id.e6b_label_value1 );
        TextView mLabel2 = findViewById( R.id.e6b_label_value2 );
        TextView mLabel3 = findViewById( R.id.e6b_label_value3 );
        mEdit1 = findViewById( R.id.e6b_edit_value1 );
        mEdit2 = findViewById( R.id.e6b_edit_value2 );
        mEdit3 = findViewById( R.id.e6b_edit_value3 );

        if ( mMode == R.id.E6B_TSD_TIME ) {
            mLabel1.setText( R.string.gs );
            addEditField( mEdit1, R.string.kts );
            mLabel2.setText( R.string.distance_flown );
            addEditField( mEdit2, R.string.nm );
            mLabel3.setText( R.string.time_flown );
            addReadOnlyField( mEdit3, R.string.min );
        } else if ( mMode == R.id.E6B_TSD_SPEED ) {
            mLabel1.setText( R.string.distance_flown );
            addEditField( mEdit1, R.string.nm );
            mLabel2.setText( R.string.time_flown );
            addEditField( mEdit2, R.string.min );
            mLabel3.setText( R.string.gs );
            addReadOnlyField( mEdit3, R.string.kts );
        } else if ( mMode == R.id.E6B_TSD_DISTANCE ) {
            mLabel1.setText( R.string.gs );
            addEditField( mEdit1, R.string.kts );
            mLabel2.setText( R.string.time_flown );
            addEditField( mEdit2, R.string.min );
            mLabel3.setText( R.string.distance_flown );
            addReadOnlyField( mEdit3, R.string.nm );
        }
    }

}
