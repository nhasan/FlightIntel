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
import android.os.Bundle;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;

public class TafMapActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout_no_toolbar );

        Bundle args = getIntent().getExtras();
        addFragment( TafMapFragment.class, args );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        // Do not call the parent implementation
    }

    public static class TafMapFragment extends WxMapFragmentBase {

        private static final String[] sTypeCodes = new String[]{
                "F01",
                "F02",
                "F03",
                "F04",
                "F05",
                "F06",
                "F07",
                "F08",
                "F09",
                "F10",
                "F11",
                "F12",
                "F13",
                "F14",
                "F15",
                "F16",
                "F17",
                "F18",
                "F19",
                "F20",
                "F21",
                "F22",
                "F23",
        };

        private static final String[] sTypeNames = new String[]{
                "1 Hour",
                "2 Hours",
                "3 Hours",
                "4 Hours",
                "5 Hours",
                "6 Hours",
                "7 Hours",
                "8 Hours",
                "9 Hours",
                "10 Hours",
                "11 Hours",
                "12 Hours",
                "13 Hours",
                "14 Hours",
                "15 Hours",
                "16 Hours",
                "17 Hours",
                "18 Hours",
                "19 Hours",
                "20 Hours",
                "21 Hours",
                "22 Hours",
                "23 Hours",
        };

        public TafMapFragment() {
            super( NoaaService.ACTION_GET_TAF,
                    WxRegions.sWxRegionCodes, WxRegions.sWxRegionNames,
                    sTypeCodes, sTypeNames );
            setMapTypeName( "Valid From" );
            setLabel( "Select Region" );
        }

        @Override
        protected Intent getServiceIntent() {
            return new Intent( getActivity(), TafService.class );
        }

        @Override
        protected String getProduct() {
            return "tafmap";
        }
    }

}
