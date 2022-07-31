/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.nadmm.airports.afd.AfdMainActivity;
import com.nadmm.airports.data.DatabaseManager.Catalog;
import com.nadmm.airports.data.DownloadActivity;
import com.nadmm.airports.utils.ExternalStorageActivity;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.wx.WxMainActivity;

import java.util.ArrayList;
import java.util.Date;

public class FlightIntel extends ActivityBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Application.version = pInfo.versionName;
        } catch ( PackageManager.NameNotFoundException ignored ) {
        }

        new LaunchTask().execute();
    }

    private void startMainActivity( ArrayList<Date> installed ) {
        String msg = null;

        PreferenceManager.setDefaultValues( this, R.xml.preferences, false );

        FirebaseMessaging firebaseMsg = FirebaseMessaging.getInstance();
        if ( getPrefAlertsEnabled() ) {
            firebaseMsg.subscribeToTopic( "all" );
            if ( getPrefHomeAirport().equals( "TEST" ) ) {
                firebaseMsg.subscribeToTopic( "test" );
            }
        } else {
            firebaseMsg.unsubscribeFromTopic( "all" );
            firebaseMsg.unsubscribeFromTopic( "test" );
        }

        boolean agreed = getPrefDisclaimerAgreed();
        Intent intent = !agreed? new Intent( this, DisclaimerActivity.class ) : null;
        if ( intent == null ) {
            if ( !SystemUtils.isExternalStorageAvailable() ) {
                intent = new Intent( this, ExternalStorageActivity.class );
            }
        }

        if ( intent == null ) {
            if ( installed.size() < 4 ) {
                // Required databases are not installed
                msg = "Please download the required database";
                intent = new Intent( this, DownloadActivity.class );
            }
        }

        if ( intent == null ) {
            Resources res = getResources();
            String afd = res.getString( R.string.afd );
            String home = getPrefHomeScreen();
            if ( home.equals( afd ) ) {
                intent = new Intent( this, AfdMainActivity.class );
            } else {
                intent = new Intent( this, WxMainActivity.class );
            }
        }

        if ( msg != null ) {
            UiUtils.showToast( this, msg );
        }

        startActivity( intent );

        finish();
    }

    private final class LaunchTask extends AsyncTask<Void, Void, ArrayList<Date>> {
        @Override
        protected ArrayList<Date> doInBackground( Void... params ) {
            Cursor c = getDbManager().getCurrentFromCatalog();
            ArrayList<Date> installed = new ArrayList<>();

            if ( c != null && c.moveToFirst() ) {
                do {
                    String s = c.getString( c.getColumnIndex( Catalog.END_DATE ) );
                    Date end = TimeUtils.parse3339( s );
                    if ( end == null ) {
                        break;
                    }

                    // Try to make sure we can open the databases
                    String type = c.getString( c.getColumnIndex( Catalog.TYPE ) );
                    SQLiteDatabase db = getDbManager().getDatabase( type );
                    if ( db == null ) {
                        break;
                    }

                    installed.add( end );
                } while ( c.moveToNext() );

                c.close();
            }

            return installed;
        }

        protected void onPostExecute( ArrayList<Date> result ) {
            startMainActivity( result );
        }
    }

}
