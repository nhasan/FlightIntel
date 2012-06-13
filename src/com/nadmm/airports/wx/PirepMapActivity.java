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
import android.os.Bundle;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;

public class PirepMapActivity extends ActivityBase {

    private static final String[] sPirepCodes = {
        "US_IC",
        "US_TB",
        "US_WS",
        "AK_IC",
        "AK_TB",
        "AK_WS",
        "NC_IC",
        "NC_TB",
        "NC_WS",
        "NE_IC",
        "NE_TB",
        "NE_WS",
        "NW_IC",
        "NW_TB",
        "NW_WS",
        "SC_IC",
        "SC_TB",
        "SC_WS",
        "SE_IC",
        "SE_TB",
        "SE_WS",
        "SW_IC",
        "SW_TB",
        "SW_WS"
    };

    private static final String[] sPirepNames = {
        "Contiguous U.S. - Icing",
        "Contiguous U.S. - Turbulence",
        "Contiguous U.S. - Weather/Sky",
        "Alaska - Icing",
        "Alaska - Turbulence",
        "Alaska - Weather/Sky",
        "Northcentral - Icing",
        "Northcentral - Turbulence",
        "Northcentral - Weather/Sky",
        "Northeast - Icing",
        "Northeast - Turbulence",
        "Northeast - Weather/Sky",
        "Northwest - Icing",
        "Northwest - Turbulence",
        "Northwest - Weather/Sky",
        "Southcentral - Icing",
        "Southcentral - Turbulence",
        "Southcentral - Weather/Sky",
        "Southeast - Icing",
        "Southeast - Turbulence",
        "Southeast - Weather/Sky",
        "Southwest - Icing",
        "Southwest - Turbulence",
        "Southwest - Weather/Sky"
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout );

        Bundle args = getIntent().getExtras();
        addFragment( PirepMapFragment.class, args );
    }

    public static class PirepMapFragment extends WxMapFragmentBase {

        public PirepMapFragment() {
            super( NoaaService.ACTION_GET_PIREP, sPirepCodes, sPirepNames,
                    "Select Region and Category" );
        }

        @Override
        protected Intent getServiceIntent() {
            return new Intent( getActivity(), PirepService.class );
        }

    }

}
