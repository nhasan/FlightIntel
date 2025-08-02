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

public class IcingFragment extends WxGraphicFragmentBase {

    private static final Map<String, String> Types = Map.of(
            "F00_cip", "Current",
            "F01_fip", "1 Hour",
            "F02_fip", "2 hour",
            "F03_fip", "3 Hour",
            "F06_fip", "6 Hour",
            "F09_fip", "9 Hour",
            "F12_fip", "12 Hour",
            "F15_fip", "15 Hour",
            "F18_fip", "18 Hour"
    );

    private static final Map<String, String> Altitudes = Map.ofEntries(
        Map.entry("010", "1,000 feet MSL"),
        Map.entry("030", "3,000 feet MSL"),
        Map.entry("060", "5,000 feet MSL"),
        Map.entry("090", "9,000 feet MSL"),
        Map.entry("120", "11,000 feet MSL"),
        Map.entry("150", "15,000 feet MSL"),
        Map.entry("180", "17,000 feet MSL"),
        Map.entry("210", "FL210"),
        Map.entry("240", "FL230"),
        Map.entry("270", "FL270"),
        Map.entry("max", "Max in Column")
    );

    public IcingFragment() {
        super( NoaaService.ACTION_GET_ICING, Altitudes, Types);
        setLabel( "Select Altitude" );
    }

    @Override
    protected String getProduct() {
        return "icing";
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), IcingService.class );
    }

}
