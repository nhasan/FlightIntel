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

public class WindsAloftFragment extends WxTextFragmentBase {

    private static final String[] mTypeCodes = {
            "06",
            "12",
            "24"
    };

    private static final String[] mTypeNames = {
            "6 Hour",
            "12 Hour",
            "24 Hour"
    };

    private static final String[] mAreaCodes = {
            "us",
            "alaska",
            "bos",
            "canada",
            "chi",
            "dfw",
            "hawaii",
            "mia",
            "pacific",
            "sfo",
            "slc",
    };

    private static final String[] mAreaNames = {
            "Continental US",
            "Alaska",
            "Boston",
            "Canada",
            "Chicago",
            "Dallas/Fort Worth",
            "Hawaii",
            "Miami",
            "Pacific",
            "San Francisco",
            "Salt Lake City",
    };

    public WindsAloftFragment() {
        super( NoaaService.ACTION_GET_FB, mAreaCodes, mAreaNames, mTypeCodes, mTypeNames );
    }

    @Override
    protected String getTitle() {
        return "Winds Aloft";
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), WindsAloftService.class );
    }

    @Override
    protected String getProduct() {
        return "windsaloft";
    }
}
