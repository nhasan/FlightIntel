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

package com.nadmm.airports;

import android.os.Bundle;
import android.util.Log;

public class FragmentActivityBase extends ActivityBase {

    private FragmentBase mCurFragment;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        super.onFragmentStarted( fragment );

        mCurFragment = fragment;
        Log.d( "FragmentStarted", mCurFragment.getClass().getSimpleName() );
        enableDisableSwipeRefresh( mCurFragment.isRefreshable() );
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return mCurFragment.canSwipeRefreshChildScrollUp();
    }

    @Override
    protected void requestDataRefresh() {
        mCurFragment.requestDataRefresh();
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

}
