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

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import com.google.analytics.tracking.android.EasyTracker;
import com.nadmm.airports.DrawerActivityBase;
import com.nadmm.airports.utils.NavAdapter;
import com.nadmm.airports.views.DrawerListView;

import java.util.ArrayList;

public final class AfdMainActivity extends DrawerActivityBase
        implements ActionBar.OnNavigationListener {

    private final String[] mOptions = new String[] {
            "Favorites",
            "Nearby",
            "Browse"
    };

    protected final int ID_FAVORITES = 0;
    protected final int ID_NEARBY = 1;
    protected final int ID_BROWSE = 2;

    private int mFragmentId;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Setup list navigation mode
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled( false );
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
        NavAdapter adapter = new NavAdapter( actionBar.getThemedContext(), "Airports", mOptions );
        actionBar.setListNavigationCallbacks( adapter, this );

        Bundle args = getIntent().getExtras();
        mFragmentId = getInitialFragmentId();
        addFragment( getFragmentClass( mFragmentId ), args );
    }

    @Override
    protected void onResume() {
        super.onResume();

        setDrawerItemChecked( DrawerListView.ITEM_ID_AFD );
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST ) {
            actionBar.setSelectedNavigationItem( mFragmentId );
        }
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

    @Override
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        if ( itemId != mFragmentId ) {
            mFragmentId = (int) itemId;
            Class<?> clss = getFragmentClass( mFragmentId );
            replaceFragment( clss, null, false );
        }
        return true;
    }

    protected int getInitialFragmentId() {
        ArrayList<String> fav = getDbManager().getAptFavorites();
        if ( fav.size() > 0 ) {
            return ID_FAVORITES;
        } else {
            return ID_NEARBY;
        }
    }

    protected Class<?> getFragmentClass( int id ) {
        Class<?> clss = null;
        if ( id == ID_FAVORITES ) {
            clss = FavoriteAirportsFragment.class;
        } else if ( id == ID_NEARBY ) {
            clss = NearbyAirportsFragment.class;
        } else if ( id == ID_BROWSE ) {
            clss = BrowseStateFragment.class;
        }
        return clss;
    }

}
