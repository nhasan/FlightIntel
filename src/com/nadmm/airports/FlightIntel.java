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


import java.io.File;
import java.util.Arrays;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.nadmm.airports.utils.SystemUtils;

public class FlightIntel extends ActivityBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        PreferenceManager.setDefaultValues( this, R.xml.preferences, false );
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );

        cleanupOldDirs();

        boolean agreed = prefs.getBoolean( PreferencesActivity.KEY_DISCLAIMER_AGREED, false );
        if ( !agreed ) {
            // User has not yet agreed to the disclaimer, show it now
            Intent disclaimer = new Intent( this, DisclaimerActivity.class );
            startActivity( disclaimer );
            finish();
            return;
        }

        Intent intent = checkData();
        if ( intent != null ) {
            startActivity( intent );
            finish();
            return;
        }

        startHomeActivity();
        finish();
    }

    private void cleanupOldDirs() {
        final String[] oldNames = new String[] {
            "airsigmet",
            "metar",
            "pirep",
            "taf"
        };

        File root = SystemUtils.getExternalDir( "" );
        File[] files = root.listFiles();
        if ( files != null ) {
            for ( File file : files ) {
                if ( Arrays.binarySearch( oldNames, file.getName() ) >= 0 ) {
                    removeDir( file );
                }
            }
        }
    }

    private void removeDir( File dir ) {
        File[] files = dir.listFiles();
        if ( files != null ) {
            for ( File file : files ) {
                if ( file.isDirectory() ) {
                    removeDir( file );
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

}
