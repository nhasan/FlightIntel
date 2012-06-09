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

import com.nadmm.airports.utils.ArrayAdapterBase;

public class AirSigmetMapActivity extends WxMapListActivityBase {

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

    public AirSigmetMapActivity() {
        super( NoaaService.ACTION_GET_AIRSIGMET, "AIRSIGMET Maps" );
    }

    @Override
    protected ArrayAdapterBase getMapListAdapter() {
        return new ArrayAdapterBase( this, sAirSigmetCodes, sAirSigmetNames );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( this, AirSigmetService.class );
    }

}
