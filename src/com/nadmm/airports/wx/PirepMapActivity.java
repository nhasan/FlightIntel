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

import com.google.analytics.tracking.android.EasyTracker;
import com.nadmm.airports.FragmentActivityBase;

public class PirepMapActivity extends FragmentActivityBase {

    private static final String[] sTypeCodes = {
        "IC",
        "TB",
        "WS"
    };

    private static final String[] sTypeNames = {
        "Icing",
        "Turbulence",
        "Weather/Sky"
    };

    private static final String[] sPirepCodes = {
        "US",
        "AK",
        "NC",
        "NE",
        "NW",
        "SC",
        "SE",
        "SW",
    };

    private static final String[] sPirepNames = {
        "Contiguous U.S.",
        "Alaska",
        "Northcentral",
        "Northeast",
        "Northwest",
        "Southcentral",
        "Southeast",
        "Southwest",
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Bundle args = getIntent().getExtras();
        addFragment( PirepMapFragment.class, args );
    }

    @Override
    public void onStart() {
      super.onStart();
      EasyTracker.getInstance( this ).activityStart( this );
    }

    @Override
    public void onStop() {
      super.onStop();
      EasyTracker.getInstance( this ).activityStop( this );
    }

    public static class PirepMapFragment extends WxMapFragmentBase {

        public PirepMapFragment() {
            super( NoaaService.ACTION_GET_PIREP,
                    sPirepCodes, sPirepNames, sTypeCodes, sTypeNames );
            setLabel( "Select Region and Category" );
        }

        @Override
        protected Intent getServiceIntent() {
            return new Intent( getActivity(), PirepService.class );
        }

    }

}
