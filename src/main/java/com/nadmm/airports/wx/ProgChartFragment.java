/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2019 Nadeem Hasan <nhasan@nadmm.com>
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

public class ProgChartFragment extends WxGraphicFragmentBase {

    private static final String[] sProgChartCodes = new String[] {
            "F000_wpc_sfc",
            "F006_wpc_prog",
            "F012_wpc_prog",
            "F018_wpc_prog",
            "F024_wpc_prog",
            "F030_wpc_prog",
            "F036_wpc_prog",
            "F048_wpc_prog",
            "F060_wpc_prog",
            "F072_wpc_prog",
            "F096_wpc_prog",
            "F120_wpc_prog",
            "F144_wpc_prog",
            "F168_wpc_prog"
    };

    private static final String[] sProgChartNames = new String[] {
            "Current Surface Analysis",
            "6 hr Surface Prognosis",
            "12 hr Surface Prognosis",
            "18 hr Surface Prognosis",
            "24 hr Surface Prognosis",
            "30 hr Surface Prognosis",
            "36 hr Surface Prognosis",
            "48 hr Surface Prognosis",
            "60 hr Surface Prognosis",
            "3 day Surface Prognosis",
            "4 day Surface Prognosis",
            "5 day Surface Prognosis",
            "6 day Surface Prognosis",
            "7 day Surface Prognosis"
    };

    public ProgChartFragment() {
        super( NoaaService.ACTION_GET_PROGCHART, sProgChartCodes, sProgChartNames );
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
