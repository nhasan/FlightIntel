package com.nadmm.airports.wx;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.google.analytics.tracking.android.EasyTracker;
import com.nadmm.airports.DrawerActivityBase;
import com.nadmm.airports.R;
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

    private int mFragmentId;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Setup list navigation mode
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled( false );
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
        NavAdapter adapter = new NavAdapter( actionBar.getThemedContext(), mOptions );
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
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        if ( itemId != mFragmentId ) {
            mFragmentId = (int) itemId;
            replaceFragment( mClasses[ mFragmentId ], null );
        }
        return true;
    }

    protected int getInitialFragmentId() {
        ArrayList<String> fav = getDbManager().getWxFavorites();
        return ( fav.size() > 0 )? 0 : 1;
    }

    private class NavAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;

        public NavAdapter( Context context, String[] values ) {
            super( context, 0, values );
            mInflater = (LayoutInflater) context.getSystemService( LAYOUT_INFLATER_SERVICE );
            setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            if ( convertView == null ) {
                convertView = mInflater.inflate( R.layout.actionbar_spinner_item_2, null );
                TextView tv = (TextView) convertView.findViewById( android.R.id.text1 );
                tv.setText( "Weather" );
            }
            TextView tv = (TextView) convertView.findViewById( android.R.id.text2 );
            tv.setText( getItem( position ) );
            return convertView;
        }
    }

}
