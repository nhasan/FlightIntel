/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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

public class RadarFragment extends WxMapFragmentBase {

    private static final String[] sRadarCodes = new String[] {
        "latest",
        "alaska",
        "centgrtlakes",
        "greatlakes",
        "hawaii",
        "northeast",
        "northrockies",
        "pacnorthwest",
        "pacsouthwest",
        "southeast",
        "southmissvly",
        "southplains",
        "southrockies",
        "uppermissvly"
    };

    private static final String[] sRadarNames = new String[] {
        "National Mosaic",
        "Alaska Sector",
        "Central Great Lakes Sector",
        "Great Lakes Sector",
        "Hawaii Sector",
        "Northeast Sector",
        "Northern Rockies Sector",
        "Pacific Northwest Sector",
        "Pacific Southwest Sector",
        "Southeast Sector",
        "Southen Mississippi Valey Sector",
        "Southern Plains Sector",
        "Southern Rockies Sector",
        "Upper Mississippi Valey Sector"
    };

    public RadarFragment() {
        super( NoaaService.ACTION_GET_RADAR, sRadarCodes, sRadarNames );
        setTitle( "NWS Radar Mosaic" );
        setLabel( "Select Radar Sector" );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), RadarService.class );
    }

}
