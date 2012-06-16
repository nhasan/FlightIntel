/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.wx;

import android.content.Intent;
import android.os.Bundle;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;

public class AirSigmetMapActivity extends ActivityBase {

    private static final String[] sAirSigmetCodes = new String[] {
        "ALL",
        "ASH",
        "CB",
        "FZLVL",
        "IC",
        "IF",
        "LLWS",
        "SFC_WND",
        "TB"
    };

    private static final String[] sAirSigmetNames = new String[] {
        "All G-AIRMETs and SIGMETs",
        "Volcanic Ash SIGMETs",
        "Convective SIGMETs and Outlooks",
        "Freezing Level G-AIRMETs",
        "Icing G-AIRMETs and SIGMETs",
        "IFR/Mtn. Obsc. G-AIRMETs and Sand/Dust Storm SIGMETs",
        "Low Level Wind Shear G-AIRMETs",
        "Surface Wind G-AIRMETs",
        "Turbulence G-AIRMETs and SIGMETs"
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout );

        Bundle args = getIntent().getExtras();
        addFragment( AirSigmetMapFragment.class, args );
    }

    public static class AirSigmetMapFragment extends WxMapFragmentBase {

        public AirSigmetMapFragment() {
            super( NoaaService.ACTION_GET_AIRSIGMET, sAirSigmetCodes, sAirSigmetNames );
            setLabel( "Select Category" );
        }

        @Override
        protected Intent getServiceIntent() {
            return new Intent( getActivity(), AirSigmetService.class );
        }

    }
    
}
