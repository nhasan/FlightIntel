/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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
        "F00_cip",
        "F01_fip",
        "F02_fip",
        "F03_fip",
        "F06_fip",
        "F09_fip",
        "F12_fip",
        "F15_fip",
        "F18_fip"
    };

    private static final String[] sTypeNames = {
        "Current",
        "1 Hour",
        "2 hour",
        "3 Hour",
        "6 Hour",
        "9 Hour",
        "12 Hour",
        "15 Hour",
        "18 Hour"
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
