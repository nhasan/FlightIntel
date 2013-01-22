/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.View;

import com.nadmm.airports.DatabaseManager.LocationColumns;
import com.nadmm.airports.afd.NearbyAirportsFragment;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TabsAdapter;
import com.nadmm.airports.wx.NearbyWxFragment;

public class NearbyActivity extends ActivityBase {

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation;
    private TabsAdapter mTabsAdapter;
    private ArrayList<LocationListener> mLocationListeners;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        View view = inflate( R.layout.fragment_pager_layout );
        view.setKeepScreenOn( true );
        setContentView( view );

        mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        mLocationListener = new AirportsLocationListener();

        // Get the last known location to use a starting point
        Location location = mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
        if ( location == null ) {
            // Try to get last location from network provider
            location = mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
        }

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( this );
        int radius = Integer.valueOf( prefs.getString(
                PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );

        Bundle args = new Bundle();
        args.putParcelable( LocationColumns.LOCATION, location );
        args.putInt( LocationColumns.RADIUS, radius );

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        mTabsAdapter = new TabsAdapter( this, pager );
        mTabsAdapter.addTab( "AIRPORTS", NearbyAirportsFragment.class, args );
        mTabsAdapter.addTab( "WEATHER", NearbyWxFragment.class, args );

        PagerTabStrip tabs = (PagerTabStrip) findViewById( R.id.pager_tabs );
        tabs.setTabIndicatorColor( getResources().getColor( R.color.tab_indicator ) );

        setActionBarSubtitle( String.format( "Within %d NM Radius", radius ) );

        if ( savedInstanceState != null ) {
            pager.setCurrentItem( savedInstanceState.getInt( "nearbytab" ) );
        }
    }

    @Override
    protected void onResume() {
        requestLocationUpdates();
        setSlidingMenuActivatedItem( SlidingMenuFragment.ITEM_ID_AFD );

        super.onResume();
    }

    @Override
    protected void onPause() {
        if ( mLocationManager != null ) {
            mLocationManager.removeUpdates( mLocationListener );
        }

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "nearbytab", pager.getCurrentItem() );
    }

    @Override
    public void onAttachFragment( Fragment fragment ) {
        if ( fragment instanceof LocationListener ) {
            if ( mLocationListeners == null ) {
                mLocationListeners = new ArrayList<LocationListener>();
            }
            mLocationListeners.add( (LocationListener)fragment );
        }
        super.onAttachFragment( fragment );
    }

    protected void requestLocationUpdates() {
        if ( mLocationManager != null ) {
            mLocationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER,
                    30*DateUtils.SECOND_IN_MILLIS, 0.5f*GeoUtils.METERS_PER_STATUTE_MILE,
                    mLocationListener );

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
            boolean useGps = prefs.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
            if ( useGps ) {
                mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                        30*DateUtils.SECOND_IN_MILLIS, 0.5f*GeoUtils.METERS_PER_STATUTE_MILE,
                        mLocationListener );
            }
        }
    }

    private final class AirportsLocationListener implements LocationListener {

        @Override
        public void onLocationChanged( Location location ) {
            if ( mLocationListeners == null ) {
                return;
            }

            if ( GeoUtils.isBetterLocation( location, mLastLocation ) ) {
                mLastLocation = location;
                for ( LocationListener l : mLocationListeners ) {
                    l.onLocationChanged( location );
                }
            }
        }

        @Override
        public void onProviderDisabled( String provider ) {
        }

        @Override
        public void onProviderEnabled( String provider ) {
        }

        @Override
        public void onStatusChanged( String provider, int status, Bundle extras ) {
        }
        
    };

}
