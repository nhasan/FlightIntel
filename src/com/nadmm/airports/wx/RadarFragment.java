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

public class RadarFragment extends WxMapFragmentBase {

    private static final String[] sRadarCodes = new String[] {   
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
        "Alaska",
        "Central Great Lakes",
        "Great Lakes",
        "Hawaii",
        "Northeast",
        "Northern Rockies",
        "Pacific Northwest",
        "Pacific Southwest",
        "Southeast",
        "Southen Mississipi Valey",
        "Southern Plains",
        "Southern Rockies",
        "Upper Mississipi Valey"
    };

    public RadarFragment() {
        super( NoaaService.ACTION_GET_RADAR, sRadarCodes, sRadarNames );
        setTitle( "Radar Images" );
        setLabel( "Select Radar Image" );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), RadarService.class );
    }

}
