/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2017 Nadeem Hasan <nhasan@nadmm.com>
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

public class IcingFragment extends WxMapFragmentBase {

    private static final String[] sTypeCodes = {
        "00_cip",
        "01_fip",
        "02_fip",
        "03_fip",
        "06_fip",
        "09_fip",
        "12_fip",
        "15_fip",
        "18_fip"
    };

    private static final String[] sTypeNames = {
        "CIP Analysis",
        "FIP 1-hr Forecast",
        "FIP 2-hr Forecast",
        "FIP 3-hr Forecast",
        "FIP 6-hr Forecast",
        "FIP 9-hr Forecast",
        "FIP 12-hr Forecast",
        "FIP 15-hr Forecast",
        "FIP 18-hr Forecast"
    };

    private static final String[] sMapCodes = new String[] {
        "010",
        "030",
        "050",
        "070",
        "090",
        "110",
        "130",
        "150",
        "170",
        "190",
        "210",
        "230",
        "250",
        "270",
        "290",
        "max"
    };

    private static final String[] sMapNames = new String[] {
        "1,000 feet MSL",
        "3,000 feet MSL",
        "5,000 feet MSL",
        "7,000 feet MSL",
        "9,000 feet MSL",
        "11,000 feet MSL",
        "13,000 feet MSL",
        "15,000 feet MSL",
        "17,000 feet MSL",
        "19,000 feet MSL",
        "FL210",
        "FL230",
        "FL250",
        "FL270",
        "FL290",
        "Max in Column"
    };

    public IcingFragment() {
        super( NoaaService.ACTION_GET_ICING,
                sMapCodes, sMapNames, sTypeCodes, sTypeNames );
        setTitle( "Icing (CIP/FIP)" );
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
