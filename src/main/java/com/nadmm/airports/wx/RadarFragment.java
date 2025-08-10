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

import androidx.annotation.NonNull;

import java.util.Map;

public class RadarFragment extends WxGraphicFragmentBase {

    private static final Map<String, String> TypeCodes = Map.of(
            "cref", "Composite Reference",
            "rala", "Refl at Lowest Alt",
            "tops-18", "Echo Tops 18K"
    );

    public RadarFragment() {
        super( NoaaService.ACTION_GET_RADAR, WxRegions.INSTANCE.getRegionCodes(), TypeCodes );
        setGraphicLabel( "Select Region" );
    }

    @NonNull
    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), RadarService.class );
    }

    @Override
    protected String getProduct() {
        return "radar";
    }
}
