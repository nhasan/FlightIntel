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
import android.text.format.DateUtils;

import com.nadmm.airports.AirportsMain;
import com.nadmm.airports.utils.UiUtils;

public class TafService extends NoaaService {

    private final String TAF_QUERY = "dataSource=tafs&requestType=retrieve"
            +"&format=xml&compression=gzip&hoursBeforeNow=6&mostRecent=true&stationString=";
    private final File TAF_DIR = new File(
            AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/taf" );
    private final long TAF_CACHE_MAX_AGE = 3*DateUtils.HOUR_IN_MILLIS;

    protected TafParser mParser;

    public TafService() {
        super( "TafService" );
        mParser = new TafParser();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if ( !TAF_DIR.exists() ) {
            TAF_DIR.mkdirs();
        }
        // Remove any old METAR files from cache first
        cleanupCache( TAF_DIR, TAF_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        if ( !intent.getAction().equals( ACTION_GET_TAF ) ) {
            return;
        }

        // Get request parameters
        String stationId = intent.getStringExtra( STATION_ID );
        boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
        boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

        File xml = new File( TAF_DIR, "TAF_"+stationId+".xml" );
        if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
            fetchMetarFromNOAA( stationId, xml );
        }

        Taf taf = new Taf();

        if ( xml.exists() ) {
            taf.stationId = stationId;
            mParser.parse( xml, taf );
        }

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_GET_TAF );
        result.putExtra( STATION_ID, stationId );
        result.putExtra( RESULT, taf );
        sendBroadcast( result );
    }

    protected boolean fetchMetarFromNOAA( String stationId, File xml ) {
        try {
            URI uri = URIUtils.createURI( "http", NOAA_HOST, 80, DATASERVER_PATH,
                    TAF_QUERY+stationId, null );
            return fetchFromNOAA( uri, xml );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "Unable to fetch TAF: "+e.getMessage() );
        }
        return false;
    }

}
