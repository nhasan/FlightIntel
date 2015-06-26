/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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
import android.view.View;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.TabPagerActivityBase;
import com.nadmm.airports.views.ObservableScrollView;

public class WxDetailActivity extends TabPagerActivityBase {

    private final String[] mTabTitles = new String[] {
            "METAR",
            "TAF",
            "PIREP",
            "AIRMET/SIGMET",
            "RADAR",
            "PROGNOSIS CHARTS",
            "WINDS/TEMPERATURE",
            "WINDS ALOFT",
            "SIG WX",
            "CEILING/VISIBILIY",
            "ICING",
            "AREA FORECAST"
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            MetarFragment.class,
            TafFragment.class,
            PirepFragment.class,
            AirSigmetFragment.class,
            RadarFragment.class,
            ProgChartFragment.class,
            WindFragment.class,
            WindsAloftFragment.class,
            SigWxFragment.class,
            CvaFragment.class,
            IcingFragment.class,
            AreaForecastFragment.class
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setActionBarTitle( "Weather", null );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        super.onFragmentStarted( fragment );

        ObservableScrollView scrollView = (ObservableScrollView) findViewById( R.id.scroll_content );
        if ( scrollView != null ) {
            registerActionBarAutoHideScrollView( scrollView );
        }
    }

    @Override
    protected String[] getTabTitles() {
        return mTabTitles;
    }

    @Override
    protected Class<?>[] getTabFragments() {
        return mClasses;
    }

    @Override
    protected int getInitialTabIndex() {
        return 0;
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

}
