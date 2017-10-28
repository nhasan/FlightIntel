/*
 * FlightIntel for Pilots
 *
 * Copyright 2017 Nadeem Hasan <nhasan@nadmm.com>
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

public class SurfaceForecatFragment extends WxMapFragmentBase {

    private static final String[] sFcastCodes = new String[]{
            "F03",
            "F06",
            "F09",
            "F12",
            "F15",
            "F18"
    };

    private static final String[] sFcastNames = new String[]{
            "3 Hour",
            "6 Hour",
            "9 Hour",
            "12 Hour",
            "15 Hour",
            "18 Hour"
    };

    private static final String[] sRegionCodes = new String[]{
            "us",
            "nc",
            "ne",
            "nw",
            "sc",
            "se",
            "sw"
    };

    private static final String[] sRegionNames = new String[]{
            "Continental US",
            "North Central",
            "Northeast",
            "Northwest",
            "South Central",
            "Southeast",
            "Southwest"
    };

    public SurfaceForecatFragment() {
        super( NoaaService.ACTION_GET_GFA, sRegionCodes, sRegionNames,
                sFcastCodes, sFcastNames );

        setTitle( "Surface Forecast" );
        setLabel( "Select Region" );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), SurfaceForecastService.class );
    }

    @Override
    protected String getProduct() {
        return "gfa";
    }
}
