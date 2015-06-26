/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.ObservableScrollView;
import com.nadmm.airports.wx.NearbyWxFragment;

public class NearbyWxActivity extends ActivityBase {

    private FragmentBase mCurFragment;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        Bundle args = getIntent().getExtras();
        addFragment( NearbyWxFragment.class, args );

        int actionBarSize = UiUtils.calculateActionBarSize( this );
        setProgressBarTopWhenActionBarShown( actionBarSize );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        super.onFragmentStarted( fragment );

        ListView listView = ( (ListFragmentBase)fragment ).getListView();
        registerActionBarAutoHideListView( listView );

        mCurFragment = fragment;
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
