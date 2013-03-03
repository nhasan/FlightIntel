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

    private static final String[] mModes = {
        "Find Wind Speed and Direction",
        "Find Heading and Ground Speed",
        "Find Course and Ground Speed"
    };

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
    private int mNormalTextColor;
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

        mNormalTextColor = getResources().getColor( android.R.color.primary_text_light );
        mHighlightTextColor = Color.rgb( 0, 112, 64 );

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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>( getActivity(),
                android.R.layout.simple_spinner_item, mModes );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        Spinner spinner = (Spinner) findViewById( R.id.e6b_wind_triangle_mode );
        spinner.setAdapter( adapter );
        spinner.setOnItemSelectedListener( this );

        mSelectedPos = 0;
    }

    private void processInput() {
        double tas = -1;
        double gs = -1;
        double hdg = -1;
        double crs = -1;
        double ws = -1;
        double wdir = -1;

        if ( mSelectedPos == 0 ) {
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
        } else if ( mSelectedPos == 1 ) {
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
        } else if ( mSelectedPos == 2 ) {
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

    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int pos, long id ) {
        mSelectedPos = pos;

        mTasLabel.setTypeface( null, Typeface.NORMAL );
        mGsLabel.setTypeface( null, Typeface.NORMAL );
        mHdgLabel.setTypeface( null, Typeface.NORMAL );
        mCrsLabel.setTypeface( null, Typeface.NORMAL );
        mWsLabel.setTypeface( null, Typeface.NORMAL );
        mWdirLabel.setTypeface( null, Typeface.NORMAL );

        mTasEdit.setFocusable( true );
        mTasEdit.setFocusableInTouchMode( true );
        mGsEdit.setFocusable( true );
        mGsEdit.setFocusableInTouchMode( true );
        mHdgEdit.setFocusable( true );
        mHdgEdit.setFocusableInTouchMode( true );
        mCrsEdit.setFocusable( true );
        mCrsEdit.setFocusableInTouchMode( true );
        mWsEdit.setFocusable( true );
        mWsEdit.setFocusableInTouchMode( true );
        mWdirEdit.setFocusable( true );
        mWdirEdit.setFocusableInTouchMode( true );

        mTasEdit.removeTextChangedListener( mTextWatcher );
        mHdgEdit.removeTextChangedListener( mTextWatcher );
        mGsEdit.removeTextChangedListener( mTextWatcher );
        mCrsEdit.removeTextChangedListener( mTextWatcher );
        mWsEdit.removeTextChangedListener( mTextWatcher );
        mWdirEdit.removeTextChangedListener( mTextWatcher );

        mTasEdit.setText( "" );
        mGsEdit.setText( "" );
        mHdgEdit.setText( "" );
        mCrsEdit.setText( "" );
        mWsEdit.setText( "" );
        mWdirEdit.setText( "" );

        mTasEdit.setHint( "" );
        mGsEdit.setHint( "" );
        mHdgEdit.setHint( "" );
        mCrsEdit.setHint( "" );
        mWsEdit.setHint( "" );
        mWdirEdit.setHint( "" );

        mTasEdit.setTextColor( mNormalTextColor );
        mGsEdit.setTextColor( mNormalTextColor );
        mHdgEdit.setTextColor( mNormalTextColor );
        mCrsEdit.setTextColor( mNormalTextColor );
        mWsEdit.setTextColor( mNormalTextColor );
        mWdirEdit.setTextColor( mNormalTextColor );

        mTasEdit.setTypeface( null, Typeface.NORMAL );
        mGsEdit.setTypeface( null, Typeface.NORMAL );
        mHdgEdit.setTypeface( null, Typeface.NORMAL );
        mCrsEdit.setTypeface( null, Typeface.NORMAL );
        mWsEdit.setTypeface( null, Typeface.NORMAL );
        mWdirEdit.setTypeface( null, Typeface.NORMAL );

        if ( mSelectedPos == 0 ) {
            // Find wind speed and direction
            mTasEdit.addTextChangedListener( mTextWatcher );
            mTasEdit.setHint( "?" );
            mGsEdit.addTextChangedListener( mTextWatcher );
            mGsEdit.setHint( "?" );
            mHdgEdit.addTextChangedListener( mTextWatcher );
            mHdgEdit.setHint( "?" );
            mCrsEdit.addTextChangedListener( mTextWatcher );
            mCrsEdit.setHint( "?" );
            mWsLabel.setTypeface( null, Typeface.BOLD );
            mWsEdit.setFocusable( false );
            mWsEdit.setFocusableInTouchMode( false );
            mWsEdit.setTextColor( mHighlightTextColor );
            mWsEdit.setTypeface( null, Typeface.BOLD );
            mWdirLabel.setTypeface( null, Typeface.BOLD );
            mWdirEdit.setFocusable( false );
            mWdirEdit.setFocusableInTouchMode( false );
            mWdirEdit.setTextColor( mHighlightTextColor );
            mWdirEdit.setTypeface( null, Typeface.BOLD );
        } else if ( mSelectedPos == 1 ) {
            // Find HDG and GS
            mTasEdit.addTextChangedListener( mTextWatcher );
            mTasEdit.setHint( "?" );
            mGsLabel.setTypeface( null, Typeface.BOLD );
            mGsEdit.setFocusable( false );
            mGsEdit.setFocusableInTouchMode( false );
            mGsEdit.setTextColor( mHighlightTextColor );
            mGsEdit.setTypeface( null, Typeface.BOLD );
            mHdgLabel.setTypeface( null, Typeface.BOLD );
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
        } else if ( mSelectedPos == 2 ) {
            // Find CRS and GS
            mTasEdit.addTextChangedListener( mTextWatcher );
            mTasEdit.setHint( "?" );
            mGsLabel.setTypeface( null, Typeface.BOLD );
            mGsEdit.setFocusable( false );
            mGsEdit.setFocusableInTouchMode( false );
            mGsEdit.setTextColor( mHighlightTextColor );
            mGsEdit.setTypeface( null, Typeface.BOLD );
            mHdgEdit.addTextChangedListener( mTextWatcher );
            mHdgEdit.setHint( "?" );
            mCrsLabel.setTypeface( null, Typeface.BOLD );
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

    @Override
    public void onNothingSelected( AdapterView<?> arg0 ) {
    }

}
