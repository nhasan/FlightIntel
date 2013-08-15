package com.nadmm.airports.afd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.widget.ArrayAdapter;

import com.nadmm.airports.R;

public abstract class HomeActivityBase extends AfdActivityBase implements OnNavigationListener {

    private final String[] mOptions = new String[] {
            "Favorites",
            "Nearby",
            "Browse"
            };

    protected final int ID_FAVORITES = 0;
    protected final int ID_NEARBY = 1;
    protected final int ID_BROWSE = 2;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
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
    protected void onResume() {
        super.onResume();
        getSupportActionBar().setSelectedNavigationItem( getActivityId() );
    }

    @Override
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        if ( itemId != getActivityId() ) {
            Intent intent = null;
            if ( itemId == ID_FAVORITES ) {
                intent = new Intent( this, FavoritesActivity.class );
            } else if ( itemId == ID_NEARBY ) {
                intent = new Intent( this, NearbyActivity.class );
            } else if ( itemId == ID_BROWSE ) {
                intent = new Intent( this, BrowseActivity.class );
            }
            if ( intent != null ) {
                intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
                startActivity( intent );
            }
        }
        return true;
    }

    protected abstract int getActivityId();

}
