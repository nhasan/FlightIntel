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

package com.nadmm.airports.wx;

import java.io.File;
import java.net.URI;

import org.apache.http.client.utils.URIUtils;

import android.content.Intent;
import android.location.Location;
import android.text.format.DateUtils;

import com.nadmm.airports.AirportsMain;
import com.nadmm.airports.utils.UiUtils;

public class PirepService extends NoaaService {

    private final String PIREP_QUERY_BASE = "dataSource=aircraftreports&requestType=retrieve"
            +"&format=xml&compression=gzip";
    private final File PIREP_DIR = new File(
            AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/pirep" );
    private final long PIREP_CACHE_MAX_AGE = 1*DateUtils.HOUR_IN_MILLIS;

    public static final String PIREP_RADIUS_SM = "PIREP_RADIUS_SM";
    public static final String PIREP_LOCATION = "PIREP_LOCATION";
    public static final String PIREP_HOURS_BEFORE = "PIREP_HOURS_BEFORE";

    protected PirepParser mParser;

    public PirepService() {
        super( "PirepService" );
        mParser = new PirepParser();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if ( !PIREP_DIR.exists() ) {
            PIREP_DIR.mkdirs();
        }
        // Remove any old METAR files from cache first
        cleanupCache( PIREP_DIR, PIREP_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        if ( !intent.getAction().equals( ACTION_GET_PIREP ) ) {
            return;
        }

        // Get request parameters
        String stationId = intent.getStringExtra( STATION_ID );
        boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
        boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

        File xml = new File( PIREP_DIR, "PIREP_"+stationId+".xml" );
        if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
            fetchPirepFromNoaa( intent, xml );
        }

        Pirep pirep = new Pirep();

        if ( xml.exists() ) {
            mParser.parse( xml, pirep );
        }

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_GET_PIREP );
        result.putExtra( STATION_ID, stationId );
        result.putExtra( RESULT, pirep );
        sendBroadcast( result );
    }

    protected boolean fetchPirepFromNoaa( Intent intent, File xml ) {
        try {
            int radius = intent.getIntExtra( PIREP_RADIUS_SM, 100 );
            int hoursBefore = intent.getIntExtra( PIREP_HOURS_BEFORE, 3 );
            Location location = intent.getParcelableExtra( PIREP_LOCATION );
            String query = String.format( "%s&hoursBeforeNow=%d&radialDistance=%d;%.2f,%.2f",
                    PIREP_QUERY_BASE, hoursBefore, radius,
                    location.getLongitude(), location.getLatitude() );
            URI uri = URIUtils.createURI( "http", NOAA_HOST, 80, DATASERVER_PATH, query, null );
            return fetchFromNoaa( uri, xml );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "Unable to fetch PIREP: "+e.getMessage() );
        }
        return false;
    }

}
