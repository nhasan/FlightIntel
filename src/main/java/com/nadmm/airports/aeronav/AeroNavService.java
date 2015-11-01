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

package com.nadmm.airports.aeronav;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.nadmm.airports.utils.FileUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

import java.io.File;

public abstract class AeroNavService extends IntentService {

    public static final String ACTION_GET_AFD = "flightintel.intent.action.GET_AFD";
    public static final String ACTION_CHECK_AFD = "flightintel.intent.action.CHECK_AFD";
    public static final String ACTION_GET_CHARTS = "flightintel.intent.action.GET_CHARTS";
    public static final String ACTION_CHECK_CHARTS = "flightintel.intent.action.CHECK_CHARTS";
    public static final String ACTION_DELETE_CHARTS = "flightintel.intent.action.DELETE_CHARTS";
    public static final String ACTION_COUNT_CHARTS = "flightintel.intent.action.COUNT_CHARTS";

    public static final String CYCLE_NAME = "CYCLE_NAME";
    public static final String TPP_VOLUME = "TPP_VOLUME";
    public static final String PDF_NAME = "PDF_NAME";
    public static final String PDF_PATH = "PDF_PATH";
    public static final String PDF_NAMES = "PDF_NAMES";
    public static final String PDF_COUNT = "PDF_COUNT";
    public static final String DOWNLOAD_IF_MISSING = "DOWNLOAD_IF_MISSING";

    private final String AERONAV_HOST = "aeronav.faa.gov";
    private final String mName;

    private File mDataDir;

    public AeroNavService( String name ) {
        super( name );

        mName = name;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDataDir = SystemUtils.getExternalDir( this, mName );
    }

    protected File getCycleDir( String cycle ) {
        File dir = new File( mDataDir, cycle );
        if ( !dir.exists() ) {
            cleanupOldCycles();
            dir.mkdir();
        }
        return dir;
    }

    protected boolean fetch( String path, File file ) {
        try {
            return NetworkUtils.doHttpGet( this, AERONAV_HOST, 80, path, null, file );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "Error: "+e.getMessage() );
        }
        return false;
    }

    protected void sendResult( String action, String cycle, File pdfFile ) {
        Bundle extras = new Bundle();
        extras.putString( CYCLE_NAME, cycle );
        extras.putString( PDF_NAME, pdfFile.getName() );
        if ( pdfFile.exists() ) {
            extras.putString( PDF_PATH, pdfFile.getAbsolutePath() );
        }
        sendResult( action, extras );
    }

    protected void sendResult( String action, Bundle extras ) {
        Intent result = new Intent();
        result.setAction( action );
        result.putExtras( extras );
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( result );
    }

    protected void cleanupOldCycles() {
        File[] cycles = mDataDir.listFiles();
        if ( cycles != null ) {
            for ( File cycle : cycles ) {
                FileUtils.removeDir( cycle );
            }
        }
    }

}
