/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity
            implements OnSharedPreferenceChangeListener {

    public static final String KEY_HOME_AIRPORT = "home_airport";
    public static final String KEY_STARTUP_CHECK_EXPIRED_DATA = "startup_check_expired_data";
    public static final String KEY_LOCATION_USE_GPS = "location_use_gps";
    public static final String KEY_LOCATION_NEARBY_RADIUS = "location_nearby_radius";
    public static final String KEY_SHOW_EXTRA_RUNWAY_DATA = "extra_runway_data";
    public static final String KEY_SHOW_GPS_NOTAMS = "show_gps_notams";
    public static final String KEY_AUTO_DOWNLOAD_ON_3G = "auto_download_on_3G";
    public static final String KEY_DISCLAIMER_AGREED = "disclaimer_agreed";
    public static final String KEY_SHOW_LOCAL_TIME = "show_local_time";
    public static final String KEY_HOME_SCREEN = "home_screen";
    public static final String KEY_ALWAYS_SHOW_NEARBY = "always_show_nearby";

    private SharedPreferences mSharedPrefs;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource( R.xml.preferences );
        mSharedPrefs = getPreferenceScreen().getSharedPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initialize the preference screen
        onSharedPreferenceChanged( mSharedPrefs, KEY_HOME_SCREEN );
        onSharedPreferenceChanged( mSharedPrefs, KEY_LOCATION_NEARBY_RADIUS );
        onSharedPreferenceChanged( mSharedPrefs, KEY_HOME_AIRPORT );

        // Set up a listener whenever a key changes
        mSharedPrefs.registerOnSharedPreferenceChangeListener( this );
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener( this );
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key ) {
        Preference pref = findPreference( key );
        if ( key.equals( KEY_LOCATION_NEARBY_RADIUS ) ) {
            String radius = mSharedPrefs.getString( key, "30" );
            pref.setSummary( "Show locations within "+radius+" NM radius" );
        } else if ( key.equals( KEY_HOME_SCREEN ) ) {
            String home = mSharedPrefs.getString( key, "A/FD" );
            pref.setSummary( "Show "+home+" at startup" );
        } else if ( key.equals( KEY_HOME_AIRPORT ) ) {
            String code = mSharedPrefs.getString( KEY_HOME_AIRPORT, "" );
            if ( code.length() > 0 ) {
                pref.setSummary( "Home airport set to "+code );
            } else {
                pref.setSummary( "Set the code for your home airport" );
            }
        }
    }

}
