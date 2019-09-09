/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2019 Nadeem Hasan <nhasan@nadmm.com>
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
import android.widget.EditText;
import android.widget.TextView;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.WxUtils;

public class CrossWindFragment extends E6bFragmentBase {

    private EditText mWsEdit;
    private EditText mWdirEdit;
    private EditText mMagVar;
    private EditText mRwyEdit;
    private EditText mHwndEdit;
    private EditText mXwndEdit;
    private TextView mWindMsg;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_cross_wind_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mWsEdit = findViewById( R.id.e6b_edit_wind_speed );
        mWdirEdit = findViewById( R.id.e6b_edit_wind_dir );
        mMagVar = findViewById( R.id.e6b_edit_mag_var );
        mRwyEdit = findViewById( R.id.e6b_edit_runway_id );
        mHwndEdit = findViewById( R.id.e6b_edit_head_wind );
        mXwndEdit = findViewById( R.id.e6b_edit_cross_wind );
        mWindMsg = findViewById( R.id.e6b_msg );

        mMagVar.setText( "0" );

        mWsEdit.addTextChangedListener( mTextWatcher );
        mWdirEdit.addTextChangedListener( mTextWatcher );
        mRwyEdit.addTextChangedListener( mTextWatcher );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return null;
    }

    @SuppressLint( "SetTextI18n" )
    protected void processInput() {
        double windSpeed = Double.MAX_VALUE;
        double windDir = Double.MAX_VALUE;
        double magVar = Double.MAX_VALUE;
        double runwayId = Double.MAX_VALUE;

        try {
            windSpeed = Double.parseDouble( mWsEdit.getText().toString() );
            magVar = Double.parseDouble( mMagVar.getText().toString() );
        } catch ( NumberFormatException ignored ) {
        }
        try {
            windDir = Double.parseDouble( mWdirEdit.getText().toString() );
            if ( windDir == 0 || windDir > 360 ) {
                mWdirEdit.setError( "Enter a value between 1 and 360" );
                windDir = Double.MAX_VALUE;
            }
        } catch ( NumberFormatException ignored ) {
        }
        try {
            runwayId = Double.parseDouble( mRwyEdit.getText().toString() );
            if ( runwayId == 0 || runwayId > 36 ) {
                mRwyEdit.setError( "Enter a value between 1 and 36" );
                runwayId = -1;
            }
        } catch ( NumberFormatException ignored ) {
        }

        if ( windSpeed != Double.MAX_VALUE && windDir != Double.MAX_VALUE
                && magVar != Double.MAX_VALUE && runwayId != Double.MAX_VALUE ) {
            windDir = GeoUtils.applyDeclination( windDir, magVar );
            long headWind = WxUtils.getHeadWindComponent( windSpeed, windDir, runwayId*10 );
            long crossWind = WxUtils.getCrossWindComponent( windSpeed, windDir, runwayId*10 );
            mHwndEdit.setText( String.valueOf( headWind ) );
            mXwndEdit.setText( String.valueOf( crossWind ) );

            if ( headWind > 0 && crossWind > 0 ) {
                mWindMsg.setText( "Right quartering cross wind with head wind" );
            } else if ( headWind > 0 && crossWind < 0 ) {
                mWindMsg.setText( "Left quartering cross wind with head wind" );
            } else if ( headWind > 0 ) {
                mWindMsg.setText( "Head wind only, no cross wind" );
            } else if ( headWind == 0 && crossWind > 0 ) {
                mWindMsg.setText( "Right cross wind only, no head wind" );
            } else if ( headWind == 0 && crossWind < 0 ) {
                mWindMsg.setText( "Left cross wind only, no head wind" );
            } else if ( headWind < 0 && crossWind < 0 ) {
                mWindMsg.setText( "Left quartering cross wind with tail wind" );
            } else if ( headWind < 0 && crossWind > 0 ) {
                mWindMsg.setText( "Right quartering cross wind with tail wind" );
            } else if ( headWind < 0 ) {
                mWindMsg.setText( "Tail wind only, no cross wind" );
            } else {
                mWindMsg.setText( "Winds calm" );
            }
        } else {
            mHwndEdit.setText( "" );
            mXwndEdit.setText( "" );
            mWindMsg.setText( "" );
        }
    }

}
