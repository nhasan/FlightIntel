package com.nadmm.airports.wx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import com.google.analytics.tracking.android.EasyTracker;
import com.nadmm.airports.DrawerActivityBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NavAdapter;
import com.nadmm.airports.views.DrawerListView;

import java.util.ArrayList;

public final class WxMainActivity extends DrawerActivityBase
        implements ActionBar.OnNavigationListener {

    private final String[] mOptions = new String[] {
            "Favorites",
            "Nearby",
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            FavoriteWxFragment.class,
            NearbyWxFragment.class
    };

    private final int ID_FAVORITES = 0;
    private final int ID_NEARBY = 1;

    private int mFragmentId;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Setup list navigation mode
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled( false );
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
        NavAdapter adapter = new NavAdapter( actionBar.getThemedContext(),
                R.string.weather, mOptions );
        actionBar.setListNavigationCallbacks( adapter, this );

        Bundle args = getIntent().getExtras();
        mFragmentId = getInitialFragmentId();
        addFragment( mClasses[ mFragmentId ], args );
    }

    @Override
    protected void onResume() {
        super.onResume();

        setDrawerItemChecked( DrawerListView.ITEM_ID_WX );
        getSupportActionBar().setSelectedNavigationItem( mFragmentId );
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

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        setRefreshItemVisible( !isDrawerOpen() );

        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        if ( itemId != mFragmentId ) {
            mFragmentId = (int) itemId;
            replaceFragment( mClasses[ mFragmentId ], null );
        }
        return true;
    }

    protected int getInitialFragmentId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        boolean showNearby = prefs.getBoolean( PreferencesActivity.KEY_ALWAYS_SHOW_NEARBY, false );
        ArrayList<String> fav = getDbManager().getWxFavorites();
        if ( !showNearby && fav.size() > 0 ) {
            return ID_FAVORITES;
        } else {
            return ID_NEARBY;
        }
    }

}
