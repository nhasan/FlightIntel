package com.nadmm.airports.afd;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.widget.ArrayAdapter;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.MainActivity;
import com.nadmm.airports.R;

public abstract class AfdFragmentBase extends FragmentBase implements OnNavigationListener {

    private final String[] mOptions = new String[] {
            "Favorites",
            "Nearby",
            "Browse"
            };

    protected final int ID_FAVORITES = 0;
    protected final int ID_NEARBY = 1;
    protected final int ID_BROWSE = 2;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onResume() {
        super.onResume();
        getSupportActionBar().setSelectedNavigationItem( getActivityId() );
    }


    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );
        // Setup list navigation mode
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                actionBar.getThemedContext(), R.layout.support_simple_spinner_dropdown_item,
                mOptions );
        adapter.setDropDownViewResource( R.layout.support_simple_spinner_dropdown_item );
        actionBar.setListNavigationCallbacks( adapter, this );
        actionBar.setDisplayShowTitleEnabled( false );
    }

    @Override
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        if ( itemId != getActivityId() ) {
            MainActivity activity = (MainActivity) getActivity();
            if ( itemId == ID_FAVORITES ) {
                activity.replaceFragment( FavoritesFragment.class, null );
            } else if ( itemId == ID_NEARBY ) {
                activity.replaceFragment( NearbyFragment.class, null );
            } else if ( itemId == ID_BROWSE ) {
                activity.replaceFragment( BrowseFragment.class, null );
            }
        }
        return true;
    }

    protected abstract int getActivityId();

}
