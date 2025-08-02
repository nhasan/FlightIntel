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

public class WindFragment extends WxGraphicFragmentBase {

    private static final Map<String, String> TypeMap = Map.of(
        "wind", "Wind Speed",
        "windstrm", "Wind Streamlines",
        "temp", "Temperature",
        "isa", "Temperature Difference"
    );

    private static final Map<String, String> WindMap = Map.ofEntries(
            Map.entry("sfc", "Surface"),
            Map.entry("900", "3,000 feet (900 mb)"),
            Map.entry("800", "6,000 feet (800 mb)"),
            Map.entry("725", "9,000 feet (725 mb)"),
            Map.entry("650", "12,000 feet (650 mb)"),
            Map.entry("500", "18,000 feet (500 mb)"),
            Map.entry("400", "FL240 (400 mb)"),
            Map.entry("300", "FL300 (300 mb)"),
            Map.entry("225", "FL360 (225 mb)"),
            Map.entry("175", "FL420 (175 mb)"),
            Map.entry("125", "FL480 (125 mb)")
    );

    public WindFragment() {
        super( NoaaService.ACTION_GET_WIND, WindMap, TypeMap );
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
