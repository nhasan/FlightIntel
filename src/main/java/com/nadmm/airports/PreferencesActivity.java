/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2019 Nadeem Hasan <nhasan@nadmm.com>
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
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.nadmm.airports.utils.UiUtils;

public class PreferencesActivity extends FragmentActivityBase {

    public static final String KEY_HOME_AIRPORT = "home_airport";
    public static final String KEY_LOCATION_USE_GPS = "location_use_gps";
    public static final String KEY_LOCATION_NEARBY_RADIUS = "location_nearby_radius";
    public static final String KEY_SHOW_EXTRA_RUNWAY_DATA = "extra_runway_data";
    public static final String KEY_SHOW_GPS_NOTAMS = "show_gps_notams";
    public static final String KEY_AUTO_DOWNLOAD_ON_3G = "auto_download_on_3G";
    public static final String KEY_DISCLAIMER_AGREED = "disclaimer_agreed";
    public static final String KEY_SHOW_LOCAL_TIME = "show_local_time";
    public static final String KEY_HOME_SCREEN = "home_screen";
    public static final String KEY_ALWAYS_SHOW_NEARBY = "always_show_nearby";
    public static final String KEY_THEME = "theme";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Toolbar toolbar = findViewById( R.id.toolbar_actionbar );

        setSupportActionBar( toolbar );
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setDisplayHomeAsUpEnabled( true );
            actionBar.setDisplayShowHomeEnabled( true );
        }

        addPreferencesFragment();
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        if ( item.getItemId() == android.R.id.home ) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return R.id.navdrawer_settings;
    }

    protected void addPreferencesFragment() {
        Class clss = PreferencesFragment.class;
        String tag = clss.getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag( tag );
        if ( f == null ) {
            f = fm.getFragmentFactory().instantiate(getClassLoader(), clss.getName() );
            f.setArguments(getIntent().getExtras());
            FragmentTransaction ft = fm.beginTransaction();
            ft.add( R.id.fragment_container, f, tag );
            ft.commit();
        }
    }

    public static int getNighMode( String theme ) {
        int mode = AppCompatDelegate.MODE_NIGHT_YES;
        switch ( theme ) {
            case "Light":
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "Dark":
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "BatterySaver":
                mode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
                break;
            case "SystemDefault":
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        return mode;
    }

    public static class PreferencesFragment extends PreferenceFragmentCompat
            implements OnSharedPreferenceChangeListener {

        private SharedPreferences mSharedPrefs;

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );
        }

        @Override
        public void onCreatePreferences( Bundle bundle, String s ) {
            addPreferencesFromResource( R.xml.preferences );
            mSharedPrefs = getPreferenceScreen().getSharedPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();
            // Initialize the preference screen
            onSharedPreferenceChanged( mSharedPrefs, KEY_THEME );
            onSharedPreferenceChanged( mSharedPrefs, KEY_LOCATION_NEARBY_RADIUS );
            onSharedPreferenceChanged( mSharedPrefs, KEY_HOME_AIRPORT );
            onSharedPreferenceChanged( mSharedPrefs, KEY_HOME_SCREEN );

            // Set up a listener whenever a key changes
            mSharedPrefs.registerOnSharedPreferenceChangeListener( this );
        }

        @Override
        public void onPause() {
            super.onPause();
            // Unregister the listener whenever a key changes
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener( this );
        }

        @Override
        public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key ) {
            Preference pref = findPreference( key );
            switch ( key ) {
                case KEY_LOCATION_NEARBY_RADIUS:
                    String radius = mSharedPrefs.getString( key, "30" );
                    pref.setSummary( "Show locations within " + radius + " NM radius" );
                    break;
                case KEY_HOME_AIRPORT: {
                    String code = mSharedPrefs.getString( KEY_HOME_AIRPORT, "" );
                    if ( code.isEmpty() ) {
                        pref.setSummary( "Home airport is not set" );
                    } else {
                        pref.setSummary( "Home airport set to " + code );
                    }
                    break;
                }
                case KEY_HOME_SCREEN: {
                    String code = mSharedPrefs.getString( KEY_HOME_SCREEN, "" );
                    pref.setSummary( "Show " + code + " screen on startup" );
                    break;
                }
                case KEY_THEME:
                    UiUtils.clearDrawableCache();
                    String theme = mSharedPrefs.getString( KEY_THEME,
                            getResources().getString( R.string.theme_default ) );
                    int mode = PreferencesActivity.getNighMode( theme );
                    AppCompatDelegate.setDefaultNightMode( mode );
                    pref.setSummary( getThemeDescription( theme ) );
                    break;
            }
        }

        private String getThemeDescription( String theme ) {
            switch ( theme ) {
                case "SystemDefault":
                    return "System Default";
                case "BatterySaver":
                    return "Set by Battery Saver";
                default:
                    return theme+" mode";
            }
        }
    }
}
