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

package com.nadmm.airports.e6b;

import android.os.Bundle;

import com.google.analytics.tracking.android.EasyTracker;
import com.nadmm.airports.DrawerActivity;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;
import com.nadmm.airports.views.DrawerListView;

public class E6bActivity extends DrawerActivity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Bundle args = new Bundle();
        args.putLong( ListMenuFragment.MENU_ID, R.id.CATEGORY_MAIN );
        args.putString( ListMenuFragment.SUBTITLE_TEXT, "No More Slide Rule" );
        addFragment( E6bMenuFragment.class, args );
    }

    @Override
    protected void onResume() {
        super.onResume();

        setDrawerItemChecked( DrawerListView.ITEM_ID_E6B );
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

}
