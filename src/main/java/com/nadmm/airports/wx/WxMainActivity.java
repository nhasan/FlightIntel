package com.nadmm.airports.wx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.nadmm.airports.DrawerActivityBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NavAdapter;

import java.util.ArrayList;

public final class WxMainActivity extends DrawerActivityBase {

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
        setupActionbarSpinner();

        Bundle args = getIntent().getExtras();
        mFragmentId = getInitialFragmentId();
        addFragment( mClasses[ mFragmentId ], args );
    }

    protected void setupActionbarSpinner() {
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setTitle( "" );

        View spinnerContainer = LayoutInflater.from( this )
                .inflate( R.layout.actionbar_spinner, toolbar, false );
        Spinner spinner = (Spinner) spinnerContainer.findViewById( R.id.actionbar_spinner );
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
        toolbar.addView( spinnerContainer, lp );

        NavAdapter adapter = new NavAdapter( getBaseContext(),
                R.string.weather, mOptions );

        spinner.setAdapter( adapter );
        spinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
                if ( id != mFragmentId ) {
                    mFragmentId = (int) id;
                    replaceFragment( mClasses[ mFragmentId ], null, false );
                }
            }

            @Override
            public void onNothingSelected( AdapterView<?> parent ) {
            }
        } );
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        setRefreshItemVisible( !isNavDrawerOpen() );

        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_WX;
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
