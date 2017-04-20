/*
 * FlightIntel for Pilots
 *
 * Copyright 2017 Nadeem Hasan <nhasan@nadmm.com>
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
import android.support.design.widget.BottomNavigationView;

public abstract class BottomNavigationActivityBase extends ActivityBase
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String SAVED_ITEM = "saved_item";

    private BottomNavigationView mBottomNavigationView;
    private int mSelectedItemId;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_bottom_navigation );

        mBottomNavigationView = (BottomNavigationView) findViewById( R.id.bottom_nav );
        mBottomNavigationView.setOnNavigationItemSelectedListener( this );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        if ( savedInstanceState != null ) {
            mSelectedItemId = savedInstanceState.getInt( SAVED_ITEM );
        } else {
            mSelectedItemId = getInitialItemId();
        }

        mBottomNavigationView.setSelectedItemId( mSelectedItemId );
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        outState.putInt( SAVED_ITEM, mBottomNavigationView.getSelectedItemId() );
    }

    @Override
    protected void requestDataRefresh() {
        super.requestDataRefresh();
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return super.canSwipeRefreshChildScrollUp();
    }

    protected void setMenu( int resId ) {
        mBottomNavigationView.inflateMenu( resId );
    }

    protected abstract FragmentBase getCurrentFragment( int resId );

    protected abstract int getInitialItemId();

}
