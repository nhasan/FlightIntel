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

import android.graphics.Typeface;
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
import com.nadmm.airports.R;

public class WindTriangleFragment extends FragmentBase implements OnItemSelectedListener {

    private static final double TWO_PI = 2*Math.PI;

    private TextView mTasLabel;
    private TextView mGsLabel;
    private TextView mHdgLabel;
    private TextView mCrsLabel;
    private TextView mWsLabel;
    private TextView mWdirLabel;

    private EditText mTasEdit;
    private EditText mGsEdit;
    private EditText mHdgEdit;
    private EditText mCrsEdit;
    private EditText mWsEdit;
    private EditText mWdirEdit;

    private int mSelectedPos;

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
        return inflate( R.layout.e6b_wind_triangle_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mTasLabel = (TextView) findViewById( R.id.e6b_tas_label );
        mGsLabel = (TextView) findViewById( R.id.e6b_gs_label );
        mHdgLabel = (TextView) findViewById( R.id.e6b_hdg_label );
        mCrsLabel = (TextView) findViewById( R.id.e6b_crs_label );
        mWsLabel = (TextView) findViewById( R.id.e6b_ws_label );
        mWdirLabel = (TextView) findViewById( R.id.e6b_wdir_label );

        mTasEdit = (EditText) findViewById( R.id.e6b_tas_edit );
        mGsEdit = (EditText) findViewById( R.id.e6b_gs_edit );
        mHdgEdit = (EditText) findViewById( R.id.e6b_hdg_edit );
        mCrsEdit = (EditText) findViewById( R.id.e6b_crs_edit );
        mWsEdit = (EditText) findViewById( R.id.e6b_ws_edit );
        mWdirEdit = (EditText) findViewById( R.id.e6b_wdir_edit );

        String[] modes = {
                "Wind Speed and Direction",
                "Heading and Ground Speed",
                "Course and Ground Speed"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( getActivity(),
                android.R.layout.simple_spinner_item, modes );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        Spinner spinner = (Spinner) findViewById( R.id.e6b_wind_triangle_mode );
        spinner.setAdapter( adapter );
        spinner.setOnItemSelectedListener( this );

        mSelectedPos = 0;
    }

    private void processInput() {
        long tas = -1;
        long gs = -1;
        long hdg = -1;
        long crs = -1;
        long ws = -1;
        long wdir = -1;

        if ( mSelectedPos == 0 ) {
            try {
                tas = Long.valueOf( mTasEdit.getText().toString() );
                gs = Long.valueOf( mGsEdit.getText().toString() );
                hdg = Long.valueOf( mHdgEdit.getText().toString() );
                crs = Long.valueOf( mCrsEdit.getText().toString() );

                // Check bounds
                if ( hdg == 0 || hdg > 360 ) {
                    mHdgEdit.setError( "Enter a value between 1-360" );
                    hdg = -1;
                }
                if ( crs == 0 || crs > 360 ) {
                    mCrsEdit.setError( "Enter a value between 1-360" );
                    crs = -1;
                }
            } catch ( NumberFormatException e ) {
            }

            if ( tas != -1 && gs != -1 && hdg != -1 && crs != -1 ) {
                // Convert to radians
                double hdgRad = Math.toRadians( hdg );
                double crsRad = Math.toRadians( crs );

                // Calculate wind speed and direction
                ws = Math.round( Math.sqrt( Math.pow( tas-gs, 2 )
                            + 4*tas*gs*( Math.pow( Math.sin( ( hdgRad-crsRad )/2 ), 2 ) ) ) );
                double wdirRad = crsRad + Math.atan2( tas*Math.sin( hdgRad-crsRad ),
                            ( tas*Math.cos( hdgRad-crsRad ) )-gs );
                wdirRad = normalizeDir( wdirRad );
                wdir = Math.round( Math.toDegrees( wdirRad ) );

                mWsEdit.setText( String.valueOf( ws ) );
                mWdirEdit.setText( String.valueOf( wdir ) );
            } else {
                mWsEdit.setText( "" );
                mWdirEdit.setText( "" );
            }
        } else if ( mSelectedPos == 1 ) {
            try {
                tas = Long.valueOf( mTasEdit.getText().toString() );
                crs = Long.valueOf( mCrsEdit.getText().toString() );
                ws = Long.valueOf( mWsEdit.getText().toString() );
                wdir = Long.valueOf( mWdirEdit.getText().toString() );

                // Check bounds
                if ( wdir == 0 || wdir > 360 ) {
                    mWdirEdit.setError( "Enter a value between 1-360" );
                    wdir = -1;
                }
                if ( crs == 0 || crs > 360 ) {
                    mCrsEdit.setError( "Enter a value between 1-360" );
                    crs = -1;
                }
            } catch ( NumberFormatException e ) {
            }

            if ( tas != -1 && crs != -1 && ws != -1 && wdir != -1 ) {
                // Convert to radians
                double wdirRad = Math.toRadians( wdir );
                double crsRad = Math.toRadians( crs );

                // Calculate heading and ground speed
                double swc = ( ws*1.0/tas )*Math.sin( wdirRad-crsRad );
                if ( Math.abs( swc ) > 1 ) {
                    mWsEdit.setError( "Course cannot be flown, wind too strong" );
                    mGsEdit.setText( "" );
                    mHdgEdit.setText( "" );
                } else {
                    double hdgRad = crsRad+Math.asin( swc );
                    hdgRad = normalizeDir( hdgRad );
                    hdg = Math.round( Math.toDegrees( hdgRad ) );
                    gs = Math.round( tas*Math.sqrt( 1.0-Math.pow( swc, 2 ) )
                                - ws*Math.cos( wdirRad-crsRad ) );
                    if ( gs <= 0 ) {
                        mWsEdit.setError( "Course cannot be flown, wind too strong" );
                        mGsEdit.setText( "" );
                        mHdgEdit.setText( "" );
                    } else {
                        mGsEdit.setText( String.valueOf( gs ) );
                        mHdgEdit.setText( String.valueOf( hdg ) );
                    }
                }
            } else {
                mGsEdit.setText( "" );
                mHdgEdit.setText( "" );
            }
        }
    }

    private double normalizeDir( double radians ) {
        if ( radians <= 0 ) {
            return radians+TWO_PI;
        } else if ( radians > TWO_PI ) {
            return radians-TWO_PI;
        }
        return radians;
    }

    private void clearFields() {
        mTasEdit.setText( "" );
        mGsEdit.setText( "" );
        mHdgEdit.setText( "" );
        mCrsEdit.setText( "" );
        mWsEdit.setText( "" );
        mWdirEdit.setText( "" );
    }

    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int pos, long id ) {
        mSelectedPos = pos;
        clearFields();

        if ( mSelectedPos == 0 ) {
            // Find wind speed and direction
            mTasLabel.setTypeface( null, Typeface.BOLD );
            mTasEdit.setFocusable( true );
            mTasEdit.setFocusableInTouchMode( true );
            mTasEdit.addTextChangedListener( mTextWatcher );
            mGsLabel.setTypeface( null, Typeface.BOLD );
            mGsEdit.setFocusable( true );
            mGsEdit.setFocusableInTouchMode( true );
            mGsEdit.addTextChangedListener( mTextWatcher );
            mHdgLabel.setTypeface( null, Typeface.BOLD );
            mHdgEdit.setFocusable( true );
            mHdgEdit.setFocusableInTouchMode( true );
            mHdgEdit.addTextChangedListener( mTextWatcher );
            mCrsLabel.setTypeface( null, Typeface.BOLD );
            mCrsEdit.setFocusable( true );
            mCrsEdit.setFocusableInTouchMode( true );
            mCrsEdit.addTextChangedListener( mTextWatcher );
            mWsLabel.setTypeface( null, Typeface.NORMAL );
            mWsEdit.setFocusable( false );
            mWsEdit.setFocusableInTouchMode( false );
            mWsEdit.removeTextChangedListener( mTextWatcher );
            mWdirLabel.setTypeface( null, Typeface.NORMAL );
            mWdirEdit.setFocusable( false );
            mWdirEdit.setFocusableInTouchMode( false );
            mWdirEdit.removeTextChangedListener( mTextWatcher );
        } else if ( mSelectedPos == 1 ) {
            // Find HDG and GS
            mTasLabel.setTypeface( null, Typeface.BOLD );
            mTasEdit.setFocusable( true );
            mTasEdit.setFocusableInTouchMode( true );
            mTasEdit.addTextChangedListener( mTextWatcher );
            mGsLabel.setTypeface( null, Typeface.NORMAL );
            mGsEdit.setFocusable( false );
            mGsEdit.setFocusableInTouchMode( false );
            mGsEdit.removeTextChangedListener( mTextWatcher );
            mHdgLabel.setTypeface( null, Typeface.NORMAL );
            mHdgEdit.setFocusable( false );
            mHdgEdit.setFocusableInTouchMode( false );
            mHdgEdit.removeTextChangedListener( mTextWatcher );
            mCrsLabel.setTypeface( null, Typeface.BOLD );
            mCrsEdit.setFocusable( true );
            mCrsEdit.setFocusableInTouchMode( true );
            mCrsEdit.addTextChangedListener( mTextWatcher );
            mWsLabel.setTypeface( null, Typeface.BOLD );
            mWsEdit.setFocusable( true );
            mWsEdit.setFocusableInTouchMode( true );
            mWsEdit.addTextChangedListener( mTextWatcher );
            mWdirLabel.setTypeface( null, Typeface.BOLD );
            mWdirEdit.setFocusable( true );
            mWdirEdit.setFocusableInTouchMode( true );
            mWdirEdit.addTextChangedListener( mTextWatcher );
        } else if ( mSelectedPos == 2 ) {
            // Find CRS and GS
            mTasLabel.setTypeface( null, Typeface.BOLD );
            mTasEdit.setFocusable( true );
            mTasEdit.setFocusableInTouchMode( true );
            mTasEdit.addTextChangedListener( mTextWatcher );
            mGsLabel.setTypeface( null, Typeface.NORMAL );
            mGsEdit.setFocusable( false );
            mGsEdit.setFocusableInTouchMode( false );
            mGsEdit.removeTextChangedListener( mTextWatcher );
            mHdgLabel.setTypeface( null, Typeface.BOLD );
            mHdgEdit.setFocusable( true );
            mHdgEdit.setFocusableInTouchMode( true );
            mHdgEdit.addTextChangedListener( mTextWatcher );
            mCrsLabel.setTypeface( null, Typeface.NORMAL );
            mCrsEdit.setFocusable( false );
            mCrsEdit.setFocusableInTouchMode( false );
            mCrsEdit.removeTextChangedListener( mTextWatcher );
            mWsLabel.setTypeface( null, Typeface.BOLD );
            mWsEdit.setFocusable( true );
            mWsEdit.setFocusableInTouchMode( true );
            mWsEdit.addTextChangedListener( mTextWatcher );
            mWdirLabel.setTypeface( null, Typeface.BOLD );
            mWdirEdit.setFocusable( true );
            mWdirEdit.setFocusableInTouchMode( true );
            mWdirEdit.addTextChangedListener( mTextWatcher );
        }
    }

    @Override
    public void onNothingSelected( AdapterView<?> arg0 ) {
    }

}
