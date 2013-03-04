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

import android.graphics.Color;
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

public class WindTriangleFragment extends FragmentBase {

    private static final double TWO_PI = 2*Math.PI;

    private EditText mTasEdit;
    private EditText mGsEdit;
    private EditText mHdgEdit;
    private EditText mCrsEdit;
    private EditText mWsEdit;
    private EditText mWdirEdit;

    private long mMode;
    private int mHighlightTextColor;

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

        mHighlightTextColor = Color.rgb( 0, 112, 64 );

        mTasEdit = (EditText) findViewById( R.id.e6b_tas_edit );
        mGsEdit = (EditText) findViewById( R.id.e6b_gs_edit );
        mHdgEdit = (EditText) findViewById( R.id.e6b_hdg_edit );
        mCrsEdit = (EditText) findViewById( R.id.e6b_crs_edit );
        mWsEdit = (EditText) findViewById( R.id.e6b_ws_edit );
        mWdirEdit = (EditText) findViewById( R.id.e6b_wdir_edit );

        Bundle args = getArguments();
        mMode = args.getLong( ListMenuFragment.MENU_ID );
        String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        TextView label = (TextView) findViewById( R.id.e6b_label );
        label.setText( title );

        setupUi();
    }

    private void processInput() {
        double tas = -1;
        double gs = -1;
        double hdg = -1;
        double crs = -1;
        double ws = -1;
        double wdir = -1;

        if ( mMode == R.id.E6B_WIND_TRIANGLE_WIND ) {
            try {
                tas = Double.valueOf( mTasEdit.getText().toString() );
                gs = Double.valueOf( mGsEdit.getText().toString() );
                hdg = Double.valueOf( mHdgEdit.getText().toString() );
                crs = Double.valueOf( mCrsEdit.getText().toString() );

                // Check bounds
                if ( hdg == 0 || hdg > 360 ) {
                    mHdgEdit.setError( "Enter a value between 1-360" );
                    hdg = -1;
                }
                if ( crs == 0 || crs > 360 ) {
                    mCrsEdit    .setError( "Enter a value between 1-360" );
                    crs = -1;
                }
            } catch ( NumberFormatException e ) {
            }

            if ( tas != -1 && gs != -1 && hdg != -1 && crs != -1 ) {
                double hdgRad = Math.toRadians( hdg );
                double crsRad = Math.toRadians( crs );

                // Calculate wind speed and direction
                ws = Math.round( Math.sqrt( Math.pow( tas-gs, 2 )
                            + 4*tas*gs*( Math.pow( Math.sin( ( hdgRad-crsRad )/2 ), 2 ) ) ) );
                double wdirRad = crsRad + Math.atan2( tas*Math.sin( hdgRad-crsRad ),
                            ( tas*Math.cos( hdgRad-crsRad ) )-gs );
                wdirRad = normalizeDir( wdirRad );
                wdir = Math.toDegrees( wdirRad );

                mWsEdit.setText( String.valueOf( Math.round( ws ) ) );
                mWdirEdit.setText( String.valueOf( Math.round( wdir ) ) );
            } else {
                mWsEdit.setText( "" );
                mWdirEdit.setText( "" );
            }
        } else if ( mMode == R.id.E6B_WIND_TRIANGLE_HDG_GS ) {
            try {
                tas = Double.valueOf( mTasEdit.getText().toString() );
                crs = Double.valueOf( mCrsEdit.getText().toString() );
                ws = Double.valueOf( mWsEdit.getText().toString() );
                wdir = Double.valueOf( mWdirEdit.getText().toString() );

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
                double wdirRad = Math.toRadians( wdir );
                double crsRad = Math.toRadians( crs );

                // Calculate heading and ground speed
                double swc = ( ws/tas )*Math.sin( wdirRad-crsRad );
                if ( Math.abs( swc ) > 1 ) {
                    mWsEdit.setError( "Course cannot be flown, wind too strong" );
                    mGsEdit.setText( "" );
                    mHdgEdit.setText( "" );
                } else {
                    double hdgRad = crsRad+Math.asin( swc );
                    hdgRad = normalizeDir( hdgRad );
                    hdg = Math.toDegrees( hdgRad );
                    gs = tas * Math.sqrt( 1.0-Math.pow( swc, 2 ) ) - ws*Math.cos( wdirRad-crsRad );
                    if ( gs <= 0 ) {
                        mWsEdit.setError( "Course cannot be flown, wind too strong" );
                        mGsEdit.setText( "" );
                        mHdgEdit.setText( "" );
                    } else {
                        mGsEdit.setText( String.valueOf( Math.round( gs ) ) );
                        mHdgEdit.setText( String.valueOf( Math.round( hdg ) ) );
                    }
                }
            } else {
                mGsEdit.setText( "" );
                mHdgEdit.setText( "" );
            }
        } else if ( mMode == R.id.E6B_WIND_TRIANGLE_CRS_GS ) {
            try {
                tas = Double.valueOf( mTasEdit.getText().toString() );
                hdg = Double.valueOf( mHdgEdit.getText().toString() );
                ws = Double.valueOf( mWsEdit.getText().toString() );
                wdir = Double.valueOf( mWdirEdit.getText().toString() );

                // Check bounds
                if ( wdir == 0 || wdir > 360 ) {
                    mWdirEdit.setError( "Enter a value between 1-360" );
                    wdir = -1;
                }
                if ( hdg == 0 || hdg > 360 ) {
                    mHdgEdit.setError( "Enter a value between 1-360" );
                    hdg = -1;
                }
            } catch ( NumberFormatException e ) {
            }

            if ( tas != -1 && hdg != -1 && ws != -1 && wdir != -1 ) {
                double wdirRad = Math.toRadians( wdir );
                double hdgRad = Math.toRadians( hdg );

                // Calculate course and ground speed
                gs = Math.sqrt( Math.pow( ws, 2 ) + Math.pow( tas, 2 )
                        - 2*ws*tas*Math.cos( hdgRad-wdirRad ) );
                double wca = Math.atan2( ws*Math.sin( hdgRad-wdirRad ),
                        tas-( ws*Math.cos( hdgRad-wdirRad ) ) );
                crs = Math.toDegrees( ( hdgRad+wca )%TWO_PI );

                mGsEdit.setText( String.valueOf( Math.round( gs ) ) );
                mCrsEdit.setText( String.valueOf( Math.round( crs ) ) );
            } else {
                mGsEdit.setText( "" );
                mCrsEdit.setText( "" );
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

    public void setupUi() {
        if ( mMode == R.id.E6B_WIND_TRIANGLE_WIND ) {
            // Find wind speed and direction
            mTasEdit.addTextChangedListener( mTextWatcher );
            mTasEdit.setHint( "?" );
            mGsEdit.addTextChangedListener( mTextWatcher );
            mGsEdit.setHint( "?" );
            mHdgEdit.addTextChangedListener( mTextWatcher );
            mHdgEdit.setHint( "?" );
            mCrsEdit.addTextChangedListener( mTextWatcher );
            mCrsEdit.setHint( "?" );
            mWsEdit.setFocusable( false );
            mWsEdit.setFocusableInTouchMode( false );
            mWsEdit.setTextColor( mHighlightTextColor );
            mWsEdit.setTypeface( null, Typeface.BOLD );
            mWdirEdit.setFocusable( false );
            mWdirEdit.setFocusableInTouchMode( false );
            mWdirEdit.setTextColor( mHighlightTextColor );
            mWdirEdit.setTypeface( null, Typeface.BOLD );
        } else if ( mMode == R.id.E6B_WIND_TRIANGLE_HDG_GS ) {
            // Find HDG and GS
            mTasEdit.addTextChangedListener( mTextWatcher );
            mTasEdit.setHint( "?" );
            mGsEdit.setFocusable( false );
            mGsEdit.setFocusableInTouchMode( false );
            mGsEdit.setTextColor( mHighlightTextColor );
            mGsEdit.setTypeface( null, Typeface.BOLD );
            mHdgEdit.setFocusable( false );
            mHdgEdit.setFocusableInTouchMode( false );
            mHdgEdit.setTextColor( mHighlightTextColor );
            mHdgEdit.setTypeface( null, Typeface.BOLD );
            mCrsEdit.addTextChangedListener( mTextWatcher );
            mCrsEdit.setHint( "?" );
            mWsEdit.addTextChangedListener( mTextWatcher );
            mWsEdit.setHint( "?" );
            mWdirEdit.addTextChangedListener( mTextWatcher );
            mWdirEdit.setHint( "?" );
        } else if ( mMode == R.id.E6B_WIND_TRIANGLE_CRS_GS ) {
            // Find CRS and GS
            mTasEdit.addTextChangedListener( mTextWatcher );
            mTasEdit.setHint( "?" );
            mGsEdit.setFocusable( false );
            mGsEdit.setFocusableInTouchMode( false );
            mGsEdit.setTextColor( mHighlightTextColor );
            mGsEdit.setTypeface( null, Typeface.BOLD );
            mHdgEdit.addTextChangedListener( mTextWatcher );
            mHdgEdit.setHint( "?" );
            mCrsEdit.setFocusable( false );
            mCrsEdit.setFocusableInTouchMode( false );
            mCrsEdit.setTextColor( mHighlightTextColor );
            mCrsEdit.setTypeface( null, Typeface.BOLD );
            mWsEdit.addTextChangedListener( mTextWatcher );
            mWsEdit.setHint( "?" );
            mWdirEdit.addTextChangedListener( mTextWatcher );
            mWdirEdit.setHint( "?" );
        }
    }

}
