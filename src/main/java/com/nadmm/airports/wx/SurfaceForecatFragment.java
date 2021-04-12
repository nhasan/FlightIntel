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
            "F03_gfa_clouds",
            "F03_gfa_sfc",
            "F06_gfa_clouds",
            "F06_gfa_sfc",
            "F09_gfa_clouds",
            "F09_gfa_sfc",
            "F12_gfa_clouds",
            "F12_gfa_sfc",
            "F15_gfa_clouds",
            "F15_gfa_sfc",
            "F18_gfa_clouds",
            "F18_gfa_sfc"
    };

    private static final String[] sFcastNames = new String[]{
            "3 Hour Clouds",
            "3 Hour Surface",
            "6 Hour Clouds",
            "6 Hour Surface",
            "9 Hour Clouds",
            "9 Hour Surface",
            "12 Hour Clouds",
            "12 Hour Surface",
            "15 Hour Clouds",
            "15 Hour Surface",
            "18 Hour Clouds",
            "18 Hour Surface"
    };

    private static final String[] sRegionCodes = new String[]{
            "us",
            "c",
            "e",
            "nc",
            "ne",
            "nw",
            "sc",
            "se",
            "sw",
            "w"
    };

    private static final String[] sRegionNames = new String[]{
            "Continental US",
            "Central",
            "East",
            "North Central",
            "Northeast",
            "Northwest",
            "South Central",
            "Southeast",
            "Southwest",
            "West"
    };

    public SurfaceForecatFragment() {
        super( NoaaService.ACTION_GET_GFA, sRegionCodes, sRegionNames,
                sFcastCodes, sFcastNames );

        setTitle( "Graphical Forecast" );
        setMapTypeName( "Select Forecast" );
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
