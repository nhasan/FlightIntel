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

package com.nadmm.airports.afd;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.ObservableScrollView;

public class AirportActivity extends ActivityBase {

    private FragmentBase mCurFragment;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener( new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                FragmentManager fm = getSupportFragmentManager();
                mCurFragment = (FragmentBase) fm.findFragmentById( R.id.fragment_container );
                enableDisableSwipeRefresh( mCurFragment.isRefreshable() );
            }
        } );

        Bundle args = getIntent().getExtras();
        addFragment( AirportDetailsFragment.class, args );

        int actionBarSize = UiUtils.calculateActionBarSize( this );
        setProgressBarTopWhenActionBarShown( actionBarSize );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        super.onFragmentStarted( fragment );

        // Action bar may be hidden when this fragment was started so make sure it is visible
        autoShowOrHideActionBar( true );

        View view = fragment.getView();
        if ( view != null ) {
            view = view.findViewById( R.id.scroll_content );
            if ( view != null && view instanceof ObservableScrollView ) {
                ObservableScrollView scrollView = (ObservableScrollView) view;
                registerActionBarAutoHideScrollView( scrollView );
            }
        }

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
