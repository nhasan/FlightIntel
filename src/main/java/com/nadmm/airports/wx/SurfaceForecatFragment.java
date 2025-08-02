/*
 * FlightIntel for Pilots
 *
 * Copyright 2017-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import static java.util.Map.entry;

import android.content.Intent;
import java.util.Map;

public class SurfaceForecatFragment extends WxGraphicFragmentBase {

    private static final Map<String, String> Forecasts = Map.ofEntries(
            entry("F03_gfa_clouds", "3 Hour Clouds"),
            entry("F03_gfa_sfc", "3 Hour Surface"),
            entry("F06_gfa_clouds", "6 Hour Clouds"),
            entry("F06_gfa_sfc", "6 Hour Surface"),
            entry("F09_gfa_clouds", "9 Hour Clouds"),
            entry("F09_gfa_sfc", "9 Hour Surface"),
            entry("F12_gfa_clouds", "12 Hour Clouds"),
            entry("F12_gfa_sfc", "12 Hour Surface"),
            entry("F15_gfa_clouds", "15 Hour Clouds"),
            entry("F15_gfa_sfc", "15 Hour Surface"),
            entry("F18_gfa_clouds", "18 Hour Clouds"),
            entry("F18_gfa_sfc", "18 Hour Surface")
    );

    private static final Map<String, String> Regions = Map.of(
            "us", "Continental US",
            "ne", "Northeast",
            "e", "East",
            "se", "Southeast",
            "nc", "North Central",
            "c", "Central",
            "sc", "South Central",
            "nw", "Northwest",
            "w", "West",
            "sw", "Southwest"
    );

    public SurfaceForecatFragment() {
        super(NoaaService.ACTION_GET_GFA, Regions, Forecasts);

        setTitle("Graphical Forecast");
        setGraphicTypeName("Select Forecast");
        setLabel("Select Region");
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
