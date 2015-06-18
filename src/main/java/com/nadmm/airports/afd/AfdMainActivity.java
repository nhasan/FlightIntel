/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.afd;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.TabPagerActivityBase;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.SlidingTabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class AfdMainActivity extends TabPagerActivityBase {

    private final String[] mTabTitles = new String[] {
            "Favorites",
            "Nearby",
            "Browse"
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            FavoriteAirportsFragment.class,
            NearbyAirportsFragment.class,
            BrowseAirportsFragment.class
    };

    private final int ID_FAVORITES = 0;
    private final int ID_NEARBY = 1;
    private final int ID_BROWSE = 2;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setActionBarTitle( "Airports", null );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        super.onFragmentStarted( fragment );

        registerActionBarAutoHideListView( ( (ListFragmentBase) fragment ).getListView() );
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        boolean showNearby = prefs.getBoolean( PreferencesActivity.KEY_ALWAYS_SHOW_NEARBY, false );
        ArrayList<String> fav = getDbManager().getAptFavorites();
        if ( !showNearby && fav.size() > 0 ) {
            return ID_FAVORITES;
        } else {
            return ID_NEARBY;
        }
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_AFD;
    }

}
