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


public class ProgChartFragment extends WxMapFragmentBase {

    private static final String[] sProgChartCodes = new String[] {
        "sfc_analysis",
        "12_fcst",
        "24_fcst",
        "36_fcst",
        "48_fcst",
        "60_fcst",
        "mid_072",
        "mid_096",
        "mid_120",
        "mid_144",
        "mid_168"
    };

    private static final String[] sProgChartNames = new String[] {
        "Current Surface Analysis",
        "12 hr Surface Prognosis",
        "24 hr Surface Prognosis",
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

}
