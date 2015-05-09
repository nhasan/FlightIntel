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
import android.util.TypedValue;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

public class AirportActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        Bundle args = getIntent().getExtras();
        addFragment( AirportDetailsFragment.class, args );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        enableActionBarAutoHide();
        registerHideableHeaderView( findViewById( R.id.headerbar ),
                UiUtils.calculateActionBarSize( this ) );

        enableDisableSwipeRefresh( false );

        setProgressBarTopWhenActionBarShown( (int)
                TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2,
                        getResources().getDisplayMetrics() ) );
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

}
