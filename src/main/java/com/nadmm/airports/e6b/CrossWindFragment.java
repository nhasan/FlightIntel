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
import com.nadmm.airports.R;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.WxUtils;

public class CrossWindFragment extends E6bFragmentBase {

    private TextInputLayout mWsEdit;
    private TextInputLayout mWdirEdit;
    private TextInputLayout mDeclnEdit;
    private TextInputLayout mRwyEdit;
    private TextInputLayout mHwndEdit;
    private TextInputLayout mXwndEdit;
    private TextView mTextMsg;

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
        mDeclnEdit = findViewById( R.id.e6b_edit_mag_var );
        mRwyEdit = findViewById( R.id.e6b_edit_runway_id );
        mHwndEdit = findViewById( R.id.e6b_edit_head_wind );
        mXwndEdit = findViewById( R.id.e6b_edit_cross_wind );
        mTextMsg = findViewById( R.id.e6b_msg );

        addEditField( mWsEdit );
        addEditField( mWdirEdit );
        addEditField( mDeclnEdit );
        addEditField( mRwyEdit );
        addReadOnlyField( mHwndEdit );
        addReadOnlyField( mXwndEdit );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return null;
    }

    @SuppressLint( "SetTextI18n" )
    protected void processInput() {
        try {
            double windSpeed = parseDouble( mWsEdit );
            double declination = parseDeclination( mDeclnEdit );
            double windDir = parseDirection( mWdirEdit );
            double runwayId = parseRunway( mRwyEdit );
            windDir = GeoUtils.applyDeclination( Math.toDegrees( windDir ), declination );
            long headWind = WxUtils.getHeadWindComponent( windSpeed, windDir, runwayId*10 );
            long crossWind = WxUtils.getCrossWindComponent( windSpeed, windDir, runwayId*10 );
            showValue( mHwndEdit, headWind );
            showValue( mXwndEdit, crossWind );

            if ( headWind > 0 && crossWind > 0 ) {
                mTextMsg.setText( "Right quartering cross wind with head wind" );
            } else if ( headWind > 0 && crossWind < 0 ) {
                mTextMsg.setText( "Left quartering cross wind with head wind" );
            } else if ( headWind > 0 ) {
                mTextMsg.setText( "Head wind only, no cross wind" );
            } else if ( headWind == 0 && crossWind > 0 ) {
                mTextMsg.setText( "Right cross wind only, no head wind" );
            } else if ( headWind == 0 && crossWind < 0 ) {
                mTextMsg.setText( "Left cross wind only, no head wind" );
            } else if ( headWind < 0 && crossWind < 0 ) {
                mTextMsg.setText( "Left quartering cross wind with tail wind" );
            } else if ( headWind < 0 && crossWind > 0 ) {
                mTextMsg.setText( "Right quartering cross wind with tail wind" );
            } else if ( headWind < 0 ) {
                mTextMsg.setText( "Tail wind only, no cross wind" );
            } else {
                mTextMsg.setText( "Winds calm" );
            }
        } catch ( NumberFormatException ignored ) {
            clearEditText( mHwndEdit );
            clearEditText( mXwndEdit );
            mTextMsg.setText( "" );
        }
    }

}
