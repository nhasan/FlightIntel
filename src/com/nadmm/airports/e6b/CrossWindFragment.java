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

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.wx.WxUtils;

public class CrossWindFragment extends FragmentBase {

    private EditText mWindSpeed;
    private EditText mWindDir;
    private EditText mRunway;
    private EditText mHeadWind;
    private EditText mCrossWind;
    private TextView mWindMsg;

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
        return inflate( R.layout.e6b_cross_wind_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mWindSpeed = (EditText) findViewById( R.id.e6b_wind_speed );
        mWindDir = (EditText) findViewById( R.id.e6b_wind_dir );
        mRunway = (EditText) findViewById( R.id.e6b_runway_id );
        mHeadWind = (EditText) findViewById( R.id.e6b_head_wind );
        mCrossWind = (EditText) findViewById( R.id.e6b_cross_wind );
        mWindMsg = (TextView) findViewById( R.id.e6b_wind_msg );

        TextView tv = (TextView) findViewById( R.id.e6b_wind_label );
        tv.setText( "Enter values for wind speed, wind direction and runway " +
        		"to calculate head wind and cross wind components." );

        mHeadWind.setFocusable( false );
        mCrossWind.setFocusable( false );
        mWindSpeed.addTextChangedListener( mTextWatcher );
        mWindDir.addTextChangedListener( mTextWatcher );
        mRunway.addTextChangedListener( mTextWatcher );
    }

    protected void processInput() {
        int windSpeed = -1;
        int windDir = -1;
        int runwayId = -1;
        try {
            windSpeed = Integer.valueOf( mWindSpeed.getText().toString() );
        } catch ( NumberFormatException e ) {
        }
        try {
            windDir = Integer.valueOf( mWindDir.getText().toString() );
            if ( windDir == 0 || windDir > 360 ) {
                mWindDir.setError( "Enter a value between 1-360" );
                windDir = -1;
            }
        } catch ( NumberFormatException e ) {
        }
        try {
            runwayId = Integer.valueOf( mRunway.getText().toString() );
            if ( runwayId == 0 || runwayId > 36 ) {
                mRunway.setError( "Enter a value between 1 and 36" );
                runwayId = -1;
            }
        } catch ( NumberFormatException e ) {
        }

        if ( windSpeed == -1 || windDir == -1 || runwayId == -1 ) {
            mHeadWind.setText( "" );
            mCrossWind.setText( "" );
            mWindMsg.setText( "" );
            return;
        }

        long headWind = WxUtils.getHeadWindComponent( windSpeed, windDir, runwayId*10 );
        long crossWind = WxUtils.getCrossWindComponent( windSpeed, windDir, runwayId*10 );
        mHeadWind.setText( String.valueOf( headWind ) );
        mCrossWind.setText( String.valueOf( crossWind ) );

        if ( headWind > 0 && crossWind > 0 ) {
            mWindMsg.setText( "Right quartering cross wind with head wind" );
        } else if ( headWind > 0 && crossWind < 0 ) {
            mWindMsg.setText( "Left quartering cross wind with head wind" );
        } else if ( headWind > 0 && crossWind == 0 ) {
            mWindMsg.setText( "Head wind only, no cross wind" );
        } else if ( headWind == 0 && crossWind > 0 ) {
            mWindMsg.setText( "Right cross wind only, no head wind" );
        } else if ( headWind == 0 && crossWind < 0 ) {
            mWindMsg.setText( "Left cross wind only, no head wind" );
        } else if ( headWind < 0 && crossWind < 0 ) {
            mWindMsg.setText( "Left quartering cross wind with tail wind" );
        } else if ( headWind < 0 && crossWind > 0 ) {
            mWindMsg.setText( "Right quartering cross wind with tail wind" );
        } else if ( headWind < 0 && crossWind == 0 ) {
            mWindMsg.setText( "Tail wind only, no cross wind" );
        } else if ( headWind == 0 && crossWind == 0 ) {
            mWindMsg.setText( "Calm winds" );
        } else {
            mWindMsg.setText( "Unknown wind condition, please report" );
        }
    }

}
