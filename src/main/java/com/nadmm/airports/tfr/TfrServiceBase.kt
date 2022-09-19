/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.tfr;

import android.app.IntentService;
import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.SystemUtils;

import java.io.File;
import java.util.Date;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class TfrServiceBase extends IntentService {

    private  static final String SERVICE_NAME = "tfr";

    private File mDataDir;
    private final long TFR_CACHE_MAX_AGE = 15*DateUtils.MINUTE_IN_MILLIS;

    public TfrServiceBase() {
        super( SERVICE_NAME );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDataDir = SystemUtils.getExternalDir( this, SERVICE_NAME );

        // Remove any old files from cache first
        cleanupCache( mDataDir, TFR_CACHE_MAX_AGE );
    }

    protected void cleanupCache( File dir, long maxAge ) {
        // Delete all files that are older
        Date now = new Date();
        File[] files = dir.listFiles();
        for ( File file : files ) {
            long age = now.getTime()-file.lastModified();
            if ( age > maxAge ) {
                file.delete();
            }
        }
    }

    protected File getFile( String name ) {
        return new File( mDataDir, name );
    }

    protected Intent makeResultIntent( String action ) {
        Intent intent = new Intent();
        intent.setAction( action );
        return intent;
    }

    protected void sendResultIntent( Intent intent ) {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( intent );
    }

}
