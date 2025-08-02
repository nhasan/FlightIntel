/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.Map;

public class ProgChartFragment extends WxGraphicFragmentBase {

    private static final Map<String, String> ProgChartCodes = Map.ofEntries(
            Map.entry("F000_wpc_sfc", "Current Surface Analysis"),
            Map.entry("F006_wpc_prog", "6 hr Surface Prognosis"),
            Map.entry("F012_wpc_prog", "12 hr Surface Prognosis"),
            Map.entry("F018_wpc_prog", "18 hr Surface Prognosis"),
            Map.entry("F024_wpc_prog", "24 hr Surface Prognosis"),
            Map.entry("F030_wpc_prog", "30 hr Surface Prognosis"),
            Map.entry("F036_wpc_prog", "36 hr Surface Prognosis"),
            Map.entry("F048_wpc_prog", "48 hr Surface Prognosis"),
            Map.entry("F060_wpc_prog", "60 hr Surface Prognosis"),
            Map.entry("F072_wpc_prog", "3 day Surface Prognosis"),
            Map.entry("F096_wpc_prog", "4 day Surface Prognosis"),
            Map.entry("F120_wpc_prog", "5 day Surface Prognosis"),
            Map.entry("F144_wpc_prog", "6 day Surface Prognosis"),
            Map.entry("F168_wpc_prog", "7 day Surface Prognosis")
    );

    public ProgChartFragment() {
        super( NoaaService.ACTION_GET_PROGCHART, ProgChartCodes );
        setTitle( "Prognosis Charts" );
        setLabel( "Select Prognosis Chart" );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), ProgChartService.class );
    }

    @Override
    protected String getProduct() {
        return "progchart";
    }
}
