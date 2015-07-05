/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

public class TimeSpeedDistanceFragment extends FragmentBase {

    private EditText mTimeEdit;
    private EditText mTime2Edit;
    private EditText mDistanceEdit;
    private EditText mGsEdit;

    private long mMode;

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

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_time_speed_distance_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mTimeEdit = (EditText) findViewById( R.id.e6b_edit_time );
        mTime2Edit = (EditText) findViewById( R.id.e6b_edit_time2 );
        mDistanceEdit = (EditText) findViewById( R.id.e6b_edit_distance );
        mGsEdit = (EditText) findViewById( R.id.e6b_edit_gs );

        Bundle args = getArguments();
        mMode = args.getLong( ListMenuFragment.MENU_ID );
        String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        setupUi();

        setFragmentContentShown( true );
    }

    private void processInput() {
        double time = Double.MAX_VALUE;
        double speed = Double.MAX_VALUE;
        double distance = Double.MAX_VALUE;

        if ( mMode == R.id.E6B_TSD_TIME ) {
            try {
                speed = Double.parseDouble( mGsEdit.getText().toString() );
                distance = Double.parseDouble( mDistanceEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }

            if ( speed != Double.MAX_VALUE && distance != Double.MAX_VALUE ) {
                time = distance*60/speed;
                mTimeEdit.setText( String.format( Locale.US, "%.0f", time ) );
            } else {
                mTimeEdit.setText( "" );
            }
        } else if ( mMode == R.id.E6B_TSD_SPEED ) {
            try {
                time = Double.parseDouble( mTimeEdit.getText().toString() );
                distance = Double.parseDouble( mDistanceEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }

            if ( time != Double.MAX_VALUE && distance != Double.MAX_VALUE ) {
                speed = distance/(time/60);
                mGsEdit.setText( String.valueOf( Math.round( speed ) ) );
            } else {
                mGsEdit.setText( "" );
            }
        } else if ( mMode == R.id.E6B_TSD_DISTANCE ) {
            try {
                time = Double.parseDouble( mTimeEdit.getText().toString() );
                speed = Double.parseDouble( mGsEdit.getText().toString() );
            } catch ( NumberFormatException ignored ) {
            }

            if ( time != Double.MAX_VALUE && speed != Double.MAX_VALUE ) {
                distance = ( time/60 )*speed;
                mDistanceEdit.setText( String.format( Locale.US, "%.0f", distance ) );
            } else {
                mDistanceEdit.setText( "" );
            }
        }

        if ( time != Double.MAX_VALUE ) {
            mTime2Edit.setText( getFormattedTime( time ) );
        } else {
            mTime2Edit.setText( "" );
        }
    }

    private void setupUi() {
        if ( mMode == R.id.E6B_TSD_TIME ) {
            mTimeEdit.setFocusable( false );
            mTimeEdit.setTypeface( null, Typeface.BOLD );
            mTimeEdit.setHint( R.string.min );
            mGsEdit.addTextChangedListener( mTextWatcher );
            mGsEdit.setHint( R.string.input_kts );
            mDistanceEdit.addTextChangedListener( mTextWatcher );
            mDistanceEdit.setHint( R.string.input_nm );
        } else if ( mMode == R.id.E6B_TSD_SPEED ) {
            mTimeEdit.addTextChangedListener( mTextWatcher );
            mTimeEdit.setHint( R.string.input_min );
            mGsEdit.setFocusable( false );
            mGsEdit.setTypeface( null, Typeface.BOLD );
            mGsEdit.setHint( R.string.kts );
            mDistanceEdit.addTextChangedListener( mTextWatcher );
            mDistanceEdit.setHint( R.string.input_nm );
        } else if ( mMode == R.id.E6B_TSD_DISTANCE ) {
            mTimeEdit.addTextChangedListener( mTextWatcher );
            mTimeEdit.setHint( R.string.input_min );
            mGsEdit.addTextChangedListener( mTextWatcher );
            mGsEdit.setHint( R.string.input_kts );
            mDistanceEdit.setFocusable( false );
            mDistanceEdit.setTypeface( null, Typeface.BOLD );
            mDistanceEdit.setHint( R.string.nm );
        }
    }

    private String getFormattedTime( double time ) {
        long secs = Math.round( time*60 );
        long hrs = secs/(60*60);
        secs -= hrs*60*60;
        long mins = secs/60;
        secs -= mins*60;
        return String.format( Locale.US, "%02d:%02d:%02d", hrs, mins, secs );
    }

}
