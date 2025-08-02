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


public class CvaFragment extends WxGraphicFragmentBase {

    private static final Map<String, String> TypeCode = Map.of(
            "fltcat", "CVA - Flight Category",
            "ceil", "CVA - Ceiling",
            "vis", "CVA - Visibility");

    public CvaFragment() {
        super( NoaaService.ACTION_GET_CVA, WxRegions.INSTANCE.getRegionCodes(), TypeCode );
        setTitle( "Ceiling and Visibility");
        setLabel( "Select Region" );
        setHelpText( "By FAA policy, CVA is a Supplementary Weather Product for "
                + "enhanced situational awareness only. CVA must only be used with primary "
                + "products such as METARs, TAFs and AIRMETs." );
    }

    @Override
    protected String getProduct() {
        return "cva";
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), CvaService.class );
    }
}
