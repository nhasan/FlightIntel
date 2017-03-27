/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2017 Nadeem Hasan <nhasan@nadmm.com>
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

public class WindFragment extends WxMapFragmentBase {

    private static final String[] sTypeCodes = {
        "wind",
        "windstrm",
        "temp",
        "isa"
    };

    private static final String[] sTypeNames = {
        "Wind Speed",
        "Wind Streamlines",
        "Temperature",
        "Temperature Difference"
    };

    private static final String[] sWindCodes = new String[] {
        "sfc",
        "900",
        "800",
        "725",
        "650",
        "500",
        "400",
        "300",
        "225",
        "175",
        "125"
    };

    private static final String[] sWindNames = new String[] {
        "Surface",
        "3,000 feet (900 mb)",
        "6,000 feet (800 mb)",
        "9,000 feet (725 mb)",
        "12,000 feet (650 mb)",
        "18,000 feet (500 mb)",
        "FL240 (400 mb)",
        "FL300 (300 mb)",
        "FL360 (225 mb)",
        "FL420 (175 mb)",
        "FL480 (125 mb)"
    };

    public WindFragment() {
        super( NoaaService.ACTION_GET_WIND,
                sWindCodes, sWindNames, sTypeCodes, sTypeNames );
        setTitle( "Wind Images" );
        setLabel( "Select Altitude" );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), WindService.class );
    }

    @Override
    protected String getProduct() {
        return "wind";
    }
}
