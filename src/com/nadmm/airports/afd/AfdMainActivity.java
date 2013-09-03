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

import java.util.ArrayList;

import android.os.Bundle;

import com.nadmm.airports.MainActivity;
import com.nadmm.airports.views.DrawerListView;

public final class AfdMainActivity extends MainActivity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setDrawerItemChecked( DrawerListView.ITEM_ID_AFD );

        Bundle args = getIntent().getExtras();
        addFragment( getHomeFragmentClass(), args );
    }

    protected Class<?> getHomeFragmentClass() {
        ArrayList<String> fav = getDbManager().getAptFavorites();
        Class<?> clss = null;
        if ( fav.size() > 0 ) {
            clss = FavoritesFragment.class;
        } else {
            clss = NearbyFragment.class;
        }
        return clss;
    }

}
