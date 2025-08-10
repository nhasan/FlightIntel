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
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;

import java.util.Map;

public class TafGraphicActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout_no_toolbar );

        Bundle args = getIntent().getExtras();
        addFragment( TafGraphicFragment.class, args );
    }

    @Override
    public void onFragmentStarted(@NonNull FragmentBase fragment ) {
        // Do not call the parent implementation
    }

    public static class TafGraphicFragment extends WxGraphicFragmentBase {

        private static final Map<String, String> Periods = Map.ofEntries(
                Map.entry("F01", "1 Hour"), Map.entry("F02", "2 Hours"),
                Map.entry("F03", "3 Hours"), Map.entry("F04", "4 Hours"),
                Map.entry("F05", "5 Hours"), Map.entry("F06", "6 Hours"),
                Map.entry("F07", "7 Hours"), Map.entry("F08", "8 Hours"),
                Map.entry("F09", "9 Hours"), Map.entry("F10", "10 Hours"),
                Map.entry("F11", "11 Hours"), Map.entry("F12", "12 Hours"),
                Map.entry("F13", "13 Hours"), Map.entry("F14", "14 Hours"),
                Map.entry("F15", "15 Hours"), Map.entry("F16", "16 Hours"),
                Map.entry("F17", "17 Hours"), Map.entry("F18", "18 Hours"),
                Map.entry("F19", "19 Hours"), Map.entry("F20", "20 Hours"),
                Map.entry("F21", "21 Hours"), Map.entry("F22", "22 Hours"),
                Map.entry("F23", "23 Hours")
        );


        public TafGraphicFragment() {
            super( NoaaService.ACTION_GET_TAF,
                   WxRegions.INSTANCE.getRegionCodes(), Periods );
            setGraphicTypeLabel( "Valid From" );
            setGraphicLabel( "Select Region" );
        }

        @NonNull
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
