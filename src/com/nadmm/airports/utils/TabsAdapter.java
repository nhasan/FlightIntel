package com.nadmm.airports.utils;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.TitleProvider;

public class TabsAdapter extends FragmentPagerAdapter implements
        TitleProvider, ViewPager.OnPageChangeListener {

    private final Context mContext;
    private final ViewPager mViewPager;
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

    public final class TabInfo {
        private final Class<?> clss;
        private final Bundle args;
        private final String label;
        private Fragment fragment;

        TabInfo( String _label, Class<?> _class, Bundle _args ) {
            clss = _class;
            args = _args;
            label = _label;
            fragment = null;
        }

        public Fragment getFragment() {
            return fragment;
        }
    }

    public TabsAdapter( FragmentActivity activity, ViewPager pager ) {
        super( activity.getSupportFragmentManager() );
        mContext = activity;
        mViewPager = pager;
        mViewPager.setAdapter( this );
    }

    public void addTab( String label, Class<?> clss, Bundle args ) {
        TabInfo info = new TabInfo( label, clss, args );
        mTabs.add( info );
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem( int position ) {
        TabInfo info = mTabs.get( position );
        if ( info.getFragment() == null ) {
            info.fragment = Fragment.instantiate( mContext, info.clss.getName(), info.args );
        }
        return info.getFragment();
    }

    @Override
    public void onPageScrolled( int position, float positionOffset,
            int positionOffsetPixels ) {
    }

    @Override
    public void onPageScrollStateChanged( int state ) {
    }

    @Override
    public String getTitle( int position ) {
        TabInfo info = mTabs.get( position );
        return info.label;
    }

    @Override
    public void onPageSelected( int position ) {
    }

    public TabInfo getTabInfo( int index ) {
        return mTabs.get( index );
    }

}