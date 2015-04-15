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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.nadmm.airports.DrawerActivityBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NavAdapter;
import com.nadmm.airports.views.DrawerListView;

import java.util.ArrayList;

public final class AfdMainActivity extends DrawerActivityBase {

    private final String[] mOptions = new String[] {
            "Favorites",
            "Nearby",
            "Browse"
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            FavoriteAirportsFragment.class,
            NearbyAirportsFragment.class,
            BrowseStateFragment.class
    };

    private final int ID_FAVORITES = 0;
    private final int ID_NEARBY = 1;
    private final int ID_BROWSE = 2;

    private int mFragmentId;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Setup list navigation mode
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled( false );
        NavAdapter adapter = new NavAdapter( actionBar.getThemedContext(),
                R.string.airports, mOptions );

        Spinner spinner = setupActionbarSpinner();
        spinner.setAdapter( adapter );
        spinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
                if ( id != mFragmentId ) {
                    mFragmentId = (int) id;
                    replaceFragment( mClasses[ mFragmentId ], null, false );
                }
            }

            @Override
            public void onNothingSelected( AdapterView<?> parent ) {
            }
        } );

        Bundle args = getIntent().getExtras();
        mFragmentId = getInitialFragmentId();
        addFragment( mClasses[ mFragmentId ], args );
    }

    @Override
    protected void onResume() {
        super.onResume();

        setDrawerItemChecked( DrawerListView.ITEM_ID_AFD );
    }

    protected int getInitialFragmentId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        boolean showNearby = prefs.getBoolean( PreferencesActivity.KEY_ALWAYS_SHOW_NEARBY, false );
        ArrayList<String> fav = getDbManager().getAptFavorites();
        if ( !showNearby && fav.size() > 0 ) {
            return ID_FAVORITES;
        } else {
            return ID_NEARBY;
        }
    }

}
