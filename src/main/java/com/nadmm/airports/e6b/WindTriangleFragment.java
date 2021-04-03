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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

public class WindTriangleFragment extends E6bFragmentBase {

    private TextInputLayout mEdit1;
    private TextInputLayout mEdit2;
    private TextInputLayout mEdit3;
    private TextInputLayout mEdit4;
    private TextInputLayout mEdit5;
    private TextInputLayout mEdit6;
    private TextView mTextMsg;

    private long mMode;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_wind_triangle_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mMode = R.id.E6B_WIND_TRIANGLE_WIND;
        Bundle args = getArguments();
        if ( args != null ) {
            mMode = args.getLong( ListMenuFragment.MENU_ID );
        }

        setupUi();
        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return null;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void processInput() {
        mTextMsg.setText( "" );

        try {
            if ( mMode == R.id.E6B_WIND_TRIANGLE_WIND ) {
                double tas = parseDouble( mEdit1 );
                double gs = parseDouble( mEdit2 );
                double hdg = parseDirection( mEdit3 );
                double crs = parseDirection( mEdit4 );
                double ws = Math.round( Math.sqrt( Math.pow( tas-gs, 2 )
                        + 4*tas*gs*( Math.pow( Math.sin( ( hdg-crs )/2 ), 2 ) ) ) );
                double wdirRad = crs + Math.atan2( tas*Math.sin( hdg-crs ),
                        ( tas*Math.cos( hdg-crs ) )-gs );

                showValue( mEdit5, ws );
                showDirection( mEdit6, wdirRad );
            } else if ( mMode == R.id.E6B_WIND_TRIANGLE_HDG_GS ) {
                double tas = parseDouble( mEdit1 );
                double ws = parseDouble( mEdit2 );
                double wdir = parseDirection( mEdit3 );
                double crs = parseDirection( mEdit4 );
                double swc = ( ws/tas )*Math.sin( wdir-crs );
                double hdgRad = crs+Math.asin( swc );
                double gs = tas * Math.sqrt( 1.0-Math.pow( swc, 2 ) ) - ws*Math.cos( wdir-crs );
                if ( gs <= 0 || Math.abs( swc ) > 1 ) {
                    mTextMsg.setText( "Course cannot be flown, wind is too strong." );
                }

                showValue( mEdit5, gs );
                showDirection( mEdit6, hdgRad );
            } else if ( mMode == R.id.E6B_WIND_TRIANGLE_CRS_GS ) {
                double tas = parseDouble( mEdit1 );
                double ws = parseDouble( mEdit2 );
                double wdir = parseDirection( mEdit3 );
                double hdg = parseDirection( mEdit4 );
                double gs = Math.sqrt( Math.pow( ws, 2 ) + Math.pow( tas, 2 )
                        - 2*ws*tas*Math.cos( hdg-wdir ) );
                double wca = Math.atan2( ws*Math.sin( hdg-wdir ),
                        tas-( ws*Math.cos( hdg-wdir ) ) );
                double crs = ( hdg+wca )%(2*Math.PI);

                showValue( mEdit5, gs );
                showDirection( mEdit6, crs );
            }
        } catch ( NumberFormatException ignored ) {
            clearEditText( mEdit5 );
            clearEditText( mEdit6 );
        }
    }

    private void setupUi() {
        TextView mLabel1 = findViewById( R.id.e6b_label_value1 );
        TextView mLabel2 = findViewById( R.id.e6b_label_value2 );
        TextView mLabel3 = findViewById( R.id.e6b_label_value3 );
        TextView mLabel4 = findViewById( R.id.e6b_label_value4 );
        TextView mLabel5 = findViewById( R.id.e6b_label_value5 );
        TextView mLabel6 = findViewById( R.id.e6b_label_value6 );
        mEdit1 = findViewById( R.id.e6b_edit_value1 );
        mEdit2 = findViewById( R.id.e6b_edit_value2 );
        mEdit3 = findViewById( R.id.e6b_edit_value3 );
        mEdit4 = findViewById( R.id.e6b_edit_value4 );
        mEdit5 = findViewById( R.id.e6b_edit_value5 );
        mEdit6 = findViewById( R.id.e6b_edit_value6 );
        mTextMsg = findViewById( R.id.e6b_msg );

        if ( mMode == R.id.E6B_WIND_TRIANGLE_WIND ) {
            // Find wind speed and direction
            mLabel1.setText( R.string.tas );
            addEditField( mEdit1, R.string.kts );
            mLabel2.setText( R.string.gs );
            addEditField( mEdit2, R.string.kts );
            mLabel3.setText( R.string.hdg );
            addEditField( mEdit3, R.string.deg );
            mLabel4.setText( R.string.crs );
            addEditField( mEdit4, R.string.deg );
            mEdit4.setHint( R.string.deg );
            mLabel5.setText( R.string.ws );
            addReadOnlyField( mEdit5, R.string.kts );
            mLabel6.setText( R.string.wdir );
            addReadOnlyField( mEdit6, R.string.deg );
        } else if ( mMode == R.id.E6B_WIND_TRIANGLE_HDG_GS ) {
            // Find HDG and GS
            mLabel1.setText( R.string.tas );
            addEditField( mEdit1, R.string.kts );
            mLabel2.setText( R.string.ws );
            addEditField( mEdit2, R.string.kts );
            mLabel3.setText( R.string.wdir );
            addEditField( mEdit3, R.string.deg );
            mLabel4.setText( R.string.crs );
            addEditField( mEdit4, R.string.deg );
            mLabel5.setText( R.string.gs );
            addReadOnlyField( mEdit5, R.string.kts );
            mLabel6.setText( R.string.hdg );
            addReadOnlyField( mEdit6, R.string.deg );
        } else if ( mMode == R.id.E6B_WIND_TRIANGLE_CRS_GS ) {
            // Find CRS and GS
            mLabel1.setText( R.string.tas );
            addEditField( mEdit1, R.string.kts );
            mLabel2.setText( R.string.ws );
            addEditField( mEdit2, R.string.kts );
            mLabel3.setText( R.string.wdir );
            addEditField( mEdit3, R.string.deg );
            mLabel4.setText( R.string.hdg );
            addEditField( mEdit4, R.string.deg );
            mLabel5.setText( R.string.gs );
            addReadOnlyField( mEdit5, R.string.kts );
            mLabel6.setText( R.string.crs );
            addReadOnlyField( mEdit6, R.string.deg );
        }
    }

}
