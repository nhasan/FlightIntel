/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2016 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;

import com.nadmm.airports.afd.AfdMainActivity;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DownloadActivity;
import com.nadmm.airports.utils.ExternalStorageActivity;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

import java.util.Date;
import java.util.HashSet;

public class FlightIntel extends ActivityBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent;
        PreferenceManager.setDefaultValues( this, R.xml.preferences, false );
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );

        // Check if user has agreed with the disclaimer
        boolean agreed = prefs.getBoolean( PreferencesActivity.KEY_DISCLAIMER_AGREED, false );
        intent = !agreed? new Intent( this, DisclaimerActivity.class ) : null;
        if ( intent == null ) {
            // User has already agreed to the disclaimer, check data validity
            intent = checkData();
            if ( intent == null ) {
                // Data is good, start normally
                intent = new Intent( this, AfdMainActivity.class );
            }
        }

        startActivity( intent );
        finish();
    }

    private Intent checkData() {
        if ( !SystemUtils.isExternalStorageAvailable() ) {
            return new Intent( this, ExternalStorageActivity.class );
        }

        String msg = null;
        HashSet<String> installed = new HashSet<>();

        Cursor c = getDbManager().getCurrentFromCatalog();
        if ( c.moveToFirst() ) {
            Date now = new Date();
            do {
                String s = c.getString( c.getColumnIndex( DatabaseManager.Catalog.END_DATE ) );
                Date end = TimeUtils.parse3339( s );
                if ( msg == null && !now.before( end ) ) {
                    msg = "You are using expired data";
                }

                // Try to make sure we can open the databases
                String type = c.getString( c.getColumnIndex( DatabaseManager.Catalog.TYPE ) );
                SQLiteDatabase db = getDbManager().getDatabase( type );
                if ( db == null ) {
                    msg = "Database is corrupted. Please delete and re-install";
                    break;
                }

                installed.add( type );
            } while ( c.moveToNext() );
        }
        c.close();

        Intent intent = null;
        if ( installed.size() < 4 ) {
            // This should really happen only on first install
            if ( msg == null ) {
                msg = "Please download the required database";
            }
            intent = new Intent( this, DownloadActivity.class );
            intent.putExtra( "MSG", msg );
        } else if ( msg != null ) {
            UiUtils.showToast( this, msg );
        }

        return intent;
    }

}
