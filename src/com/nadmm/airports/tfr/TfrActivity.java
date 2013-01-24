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

package com.nadmm.airports.tfr;

import android.os.Bundle;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.SlidingMenuFragment;

public class TfrActivity extends ActivityBase {

    public static final String EXTRA_TFR = "TFR";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.fragment_activity_layout ) );

        Bundle args = getIntent().getExtras();

        if ( args == null || args.getSerializable( EXTRA_TFR ) == null ) {
            addFragment( TfrListFragment.class, args );
        } else {
            addFragment( TfrDetailFragment.class, args );
        }
    }

    @Override
    protected void onResume() {
        setSlidingMenuActivatedItem( SlidingMenuFragment.ITEM_ID_TFR );

        super.onResume();
    }

}
