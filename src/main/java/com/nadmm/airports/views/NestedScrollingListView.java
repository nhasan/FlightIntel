/*
 * FlightIntel for Pilots
 *
 * Copyright 2016 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.views;

import android.content.Context;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import android.util.AttributeSet;
import android.widget.ListView;

public class NestedScrollingListView extends ListView implements NestedScrollingChild {

    private NestedScrollingChildHelper mChildHelper;

    public NestedScrollingListView( Context context ) {
        this( context, null );
    }

    public NestedScrollingListView( Context context, AttributeSet attrs ) {
        this( context, attrs, 0 );
    }

    public NestedScrollingListView( Context context, AttributeSet attrs, int defStyleAttr ) {
        super( context, attrs, defStyleAttr );

        mChildHelper = new NestedScrollingChildHelper( this );
        setNestedScrollingEnabled( true );
    }

    @Override
    public void setNestedScrollingEnabled( boolean enabled ) {
        mChildHelper.setNestedScrollingEnabled( enabled );
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll( int axes ) {
        return mChildHelper.startNestedScroll( axes );
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll( int dxConsumed, int dyConsumed, int dxUnconsumed,
                                         int dyUnconsumed, int[] offsetInWindow ) {
        return mChildHelper.dispatchNestedScroll( dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow );
    }

    @Override
    public boolean dispatchNestedPreScroll( int dx, int dy, int[] consumed, int[] offsetInWindow ) {
        return mChildHelper.dispatchNestedPreScroll( dx, dy, consumed, offsetInWindow );
    }

    @Override
    public boolean dispatchNestedFling( float velocityX, float velocityY, boolean consumed ) {
        return mChildHelper.dispatchNestedFling( velocityX, velocityY, consumed );
    }

    @Override
    public boolean dispatchNestedPreFling( float velocityX, float velocityY ) {
        return mChildHelper.dispatchNestedPreFling( velocityX, velocityY );
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mChildHelper.onDetachedFromWindow();
    }

}
