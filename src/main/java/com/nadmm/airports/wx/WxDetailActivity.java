/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2016 Nadeem Hasan <nhasan@nadmm.com>
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

import android.os.Bundle;

import com.nadmm.airports.TabPagerActivityBase;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.notams.AirportNotamFragment;

public class WxDetailActivity extends TabPagerActivityBase {

    private final String[] mTabTitles = new String[] {
            "METAR",
            "TAF",
            "PIREP",
            "GFA",
            "AREA FORECAST",
            "AIRMET/SIGMET",
            "PROG CHARTS",
            "WINDS ALOFT",
            "ICING"
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            MetarFragment.class,
            TafFragment.class,
            PirepFragment.class,
            SurfaceForecatFragment.class,
            AreaForecastFragment.class,
            AirSigmetFragment.class,
            ProgChartFragment.class,
            WindsAloftFragment.class,
            IcingFragment.class
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setActionBarTitle( "Weather", null );

        Bundle args = getIntent().getExtras();
        for ( int i=0; i<mTabTitles.length; ++i ) {
            if ( mClasses[ i ] == AirportNotamFragment.class
                && !args.containsKey( Airports.SITE_NUMBER ) )
            {
                // Do not add NOTAM tab if not accessed from an airport screen
                continue;
            }
            addTab( mTabTitles[ i ], mClasses[ i ], args );
        }
    }

}
