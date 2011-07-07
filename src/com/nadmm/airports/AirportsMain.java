/*
 * Airports for Android
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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class AirportsMain extends Activity {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        PreferenceManager.setDefaultValues( this, R.xml.preferences, false );
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );

        boolean agreed = prefs.getBoolean( PreferencesActivity.KEY_DISCLAIMER_AGREED, false );
        if ( !agreed ) {
            // User has not yet agreed to the disclaimer, show it now
            Intent disclaimer = new Intent( this, DisclaimerActivity.class );
            startActivity( disclaimer );
            finish();
            return;
        }

        // Check if we have any data installed. If not, then redirect to the download activity
        DatabaseManager dbManager = DatabaseManager.instance( this );
        Cursor c = dbManager.getCurrentFromCatalog();
        if ( !c.moveToFirst() ) {
            c.close();
            Intent download = new Intent( this, DownloadActivity.class );
            download.putExtra( "MSG", "Please install data before using the application" );
            startActivity( download );
            finish();
            return;
        }

        boolean checkData = prefs.getBoolean( PreferencesActivity.KEY_STARTUP_CHECK_EXPIRED_DATA, 
                true );
        if ( checkData ) {
            // Check if we have any expired data. If yes, then redirect to download activity
            do {
                int age = c.getInt( c.getColumnIndex( "age" ) );
                if ( age <= 0 ) {
                    // We have some expired data
                    c.close();
                    Intent download = new Intent( this, DownloadActivity.class );
                    download.putExtra( "MSG", "One or more data items have expired" );
                    startActivity( download );
                    finish();
                    return;
                }
            } while ( c.moveToNext() );
        }

        c.close();

        String startupActivity = prefs.getString( 
                PreferencesActivity.KEY_STARTUP_SHOW_ACTIVITY,  "browse" );
        Intent intent = null;
        if ( startupActivity.equals( "browse" ) ) {
            intent = new Intent( this, BrowseActivity.class );
            intent.putExtras( new Bundle() );
        } else if ( startupActivity.equals( "favorite" ) ) {
            intent = new Intent( this, FavoritesActivity.class );
        } else if ( startupActivity.equals( "nearby" ) ) {
            intent = new Intent( this, NearbyActivity.class );
        }
        startActivity( intent );
        finish();
    }

}
