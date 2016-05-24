package com.nadmm.airports.views;

import android.content.Context;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.util.AttributeSet;
import android.widget.ListView;

public class NestedScrollingListView extends ListView implements NestedScrollingChild {

    private NestedScrollingChildHelper mScrollingChildHelper;

    private void init() {
        mScrollingChildHelper = new NestedScrollingChildHelper( this );
        setNestedScrollingEnabled( true );
    }

    public NestedScrollingListView( Context context ) {
        super( context );
        init();
    }

    public NestedScrollingListView( Context context, AttributeSet attrs ) {
        super( context, attrs );
        init();
    }

    @Override
    public void setNestedScrollingEnabled( boolean enabled ) {
        mScrollingChildHelper.setNestedScrollingEnabled( enabled );
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll( int axes ) {
        return mScrollingChildHelper.startNestedScroll( axes );
    }

    @Override
    public void stopNestedScroll() {
        mScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll( int dxConsumed, int dyConsumed, int dxUnconsumed,
                                         int dyUnconsumed, int[] offsetInWindow ) {
        return mScrollingChildHelper.dispatchNestedScroll( dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow );
    }

    @Override
    public boolean dispatchNestedPreScroll( int dx, int dy, int[] consumed, int[] offsetInWindow ) {
        return mScrollingChildHelper.dispatchNestedPreScroll( dx, dy, consumed, offsetInWindow );
    }

    @Override
    public boolean dispatchNestedFling( float velocityX, float velocityY, boolean consumed ) {
        return mScrollingChildHelper.dispatchNestedFling( velocityX, velocityY, consumed );
    }

    @Override
    public boolean dispatchNestedPreFling( float velocityX, float velocityY ) {
        return mScrollingChildHelper.dispatchNestedPreFling( velocityX, velocityY );
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mScrollingChildHelper.onDetachedFromWindow();
    }

}
