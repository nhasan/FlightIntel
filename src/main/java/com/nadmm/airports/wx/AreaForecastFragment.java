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

public class AreaForecastFragment extends WxTextFragmentBase {

    private final static String[] mAreaCodes = {
            "fa_alaska_n1.txt",
            "fa_alaska_n2.txt",
            "fa_alaska_sc1.txt",
            "fa_alaska_sc2.txt",
            "fa_alaska_se1.txt",
            "fa_alaska_se2.txt",
            "fa_alaska_bswa.txt",
            "fa_carib.txt",
            "fa_gulf.txt",
            "fa_hawaii.txt",
    };

    private final static String[] mAreaNames = {
            "Alaska North Part 1",
            "Alaska North Part 2",
            "Alaska Southcentral Part 1",
            "Alaska Southcentral Part 2",
            "Alaska Southeast Part 1",
            "Alaska Southeast Part 2",
            "Alaska Southwest",
            "Carribean",
            "Gulf of Mexico",
            "Hawaii",
    };

    public AreaForecastFragment() {
        super( NoaaService.ACTION_GET_FA, mAreaCodes, mAreaNames );
    }

    @Override
    protected String getTitle() {
        return "Area Forecast";
    }

    @Override
    protected String getProduct() {
        return "fa";
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), AreaForecastService.class );
    }

}
