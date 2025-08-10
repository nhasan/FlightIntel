/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.Map;
import android.content.Intent;

import androidx.annotation.NonNull;

public class SatelliteFragment extends WxGraphicFragmentBase {

    private static final Map<String, String> SatTypes = Map.of(
            "vis", "Visible",
            "wv", "Water Vapor",
            "ircol", "Infrared",
            "irbw", "Infrared (B/W)");

    public SatelliteFragment() {
        super( NoaaService.ACTION_GET_SATELLITE, WxRegions.INSTANCE.getRegionCodes(), SatTypes );

        setTitle( "Satellite" );
        setGraphicLabel( "Select Region" );
    }

    @NonNull
    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), SatelliteService.class );
    }

    @Override
    protected String getProduct() {
        return "satellite";
    }
}
