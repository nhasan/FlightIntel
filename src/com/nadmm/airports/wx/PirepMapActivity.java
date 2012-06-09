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

public class PirepMapActivity extends WxMapListActivityBase {

    private static final String[] sPirepCodes = {
        "US_IC",
        "US_TB",
        "US_WS",
        "AK_IC",
        "AK_TB",
        "AK_WS",
        "NC_IC",
        "NC_TB",
        "NC_WS",
        "NE_IC",
        "NE_TB",
        "NE_WS",
        "NW_IC",
        "NW_TB",
        "NW_WS",
        "SC_IC",
        "SC_TB",
        "SC_WS",
        "SE_IC",
        "SE_TB",
        "SE_WS",
        "SW_IC",
        "SW_TB",
        "SW_WS"
    };

    private static final String[] sPirepNames = {
        "Contiguous U.S. - Icing",
        "Contiguous U.S. - Turbulence",
        "Contiguous U.S. - Weather/Sky",
        "Alaska - Icing",
        "Alaska - Turbulence",
        "Alaska - Weather/Sky",
        "Northcentral - Icing",
        "Northcentral - Turbulence",
        "Northcentral - Weather/Sky",
        "Northeast - Icing",
        "Northeast - Turbulence",
        "Northeast - Weather/Sky",
        "Northwest - Icing",
        "Northwest - Turbulence",
        "Northwest - Weather/Sky",
        "Southcentral - Icing",
        "Southcentral - Turbulence",
        "Southcentral - Weather/Sky",
        "Southeast - Icing",
        "Southeast - Turbulence",
        "Southeast - Weather/Sky",
        "Southwest - Icing",
        "Southwest - Turbulence",
        "Southwest - Weather/Sky"
    };

    public PirepMapActivity() {
        super( NoaaService.ACTION_GET_PIREP, "PIREP Maps" );
    }

    @Override
    protected ArrayAdapterBase getMapListAdapter() {
        return new ArrayAdapterBase( this, sPirepCodes, sPirepNames );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( this, PirepService.class );
    }

}
