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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;

public abstract class NoaaService extends IntentService {

    protected final String NOAA_HOST = "weather.aero";
    protected final String RADAR_HOST = "radar.weather.gov";
    protected final String AWC_HOST = "aviationweather.gov";

    protected final String NOAA_DATASERVER_PATH = "/dataserver1_4/httpparam";

    public static final String STATION_ID = "STATION_ID";
    public static final String STATION_IDS = "STATION_IDS";
    public static final String CACHE_ONLY = "CACHE_ONLY";
    public static final String FORCE_REFRESH = "FORCE_REFRESH";
    public static final String RADIUS_NM = "RADIUS_NM";
    public static final String LOCATION = "LOCATION";
    public static final String HOURS_BEFORE = "HOURS_BEFORE";
    public static final String COORDS_BOX = "COORDS_BOX";
    public static final String IMAGE_TYPE = "IMAGE_TYPE";
    public static final String IMAGE_CODE = "IMAGE_CODE";
    public static final String RESULT = "RESULT";

    public static final String TYPE = "TYPE";
    public static final String TYPE_TEXT = "TYPE_TEXT";
    public static final String TYPE_IMAGE = "TYPE_IMAGE";

    public static final String ACTION_GET_METAR = "flightintel.intent.wx.action.GET_METAR";
    public static final String ACTION_CACHE_METAR = "flightintel.intent.wx.action.CACHE_METAR";
    public static final String ACTION_GET_TAF = "flightintel.intent.wx.action.GET_TAF";
    public static final String ACTION_GET_PIREP = "flightintel.intent.wx.action.GET_PIREP";
    public static final String ACTION_GET_AIRSIGMET = "flightintel.intent.wx.action.GET_AIRSIGMET";
    public static final String ACTION_GET_RADAR = "flightintel.intent.wx.action.GET_RADAR";
    public static final String ACTION_GET_PROGCHART = "flightintel.intent.wx.action.GET_PROGCHART";
    public static final String ACTION_GET_WIND = "flightintel.intent.wx.action.GET_WIND";
    public static final String ACTION_GET_SIGWX = "flightintel.intent.action.wx.GET_SIGWX";
    public static final String ACTION_GET_CVA = "flightintel.intent.wx.action.GET_CVA";
    public static final String ACTION_GET_ICING = "flightintel.intent.action.wx.GET_ICING";
    public static final String ACTION_GET_FA = "flightintel.intent.action.wx.GET_FA";
    public static final String ACTION_GET_FB = "flightintel.intent.action.wx.GET_FB";

    private File mDataDir;
    private HttpClient mHttpClient;

    public NoaaService( String name, long age ) {
        super( name );
        mHttpClient = NetworkUtils.getHttpClient();
        mDataDir = SystemUtils.getExternalDir( "wx/"+name );

        // Remove any old files from cache first
        cleanupCache( mDataDir, age );
    }

    private void cleanupCache( File dir, long maxAge ) {
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

    protected boolean fetchFromNoaa( String query, File file, boolean compressed ) 
            throws Exception {
        return fetchFromNoaa( NOAA_DATASERVER_PATH, query, file, compressed );
    }

    protected boolean fetchFromNoaa( String path, String query, File file, boolean compressed ) 
            throws Exception {
        return fetch( NOAA_HOST, path, query, file, compressed );
    }

    protected boolean fetch( String host, String path, String query, File file, boolean compressed ) 
            throws Exception {
        URI uri = URIUtils.createURI( "http", host, 80, path, query, null );
        return fetch( uri, file, compressed );
    }

    protected boolean fetch( URI uri, File file, boolean compressed ) throws Exception {
        return NetworkUtils.doHttpGet( this, mHttpClient, uri, file, null, null,
                compressed? GZIPInputStream.class : null );
    }

    protected File getDataFile( String name ) {
        return new File( mDataDir, name );
    }

    protected void sendSerializableResultIntent( String action, String stationId,
            Serializable result ) {
        Intent intent = makeResultIntent( action, TYPE_TEXT );
        intent.putExtra( STATION_ID, stationId );
        intent.putExtra( RESULT, result );
        sendResultIntent( intent );
    }

    protected void sendImageResultIntent( String action, String code, File result ) {
        Intent intent = makeResultIntent( action, TYPE_IMAGE );
        intent.putExtra( IMAGE_CODE, code );
        if ( result.exists() ) {
            intent.putExtra( RESULT, result.getAbsolutePath() );
        }
        sendResultIntent( intent );
    }

    protected void sendFileResultIntent( String action, String stationId, File result ) {
        Intent intent = makeResultIntent( action, TYPE_TEXT );
        intent.putExtra( STATION_ID, stationId );
        if ( result.exists() ) {
            intent.putExtra( RESULT, result.getAbsolutePath() );
        }
        sendResultIntent( intent );
    }

    private Intent makeResultIntent( String action, String type ) {
        Intent intent = new Intent();
        intent.setAction( action );
        intent.putExtra( TYPE, type );
        return intent;
    }

    private void sendResultIntent( Intent intent ) {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( intent );
    }

    protected void writeObject( Object object, File objFile ) {
        try {
            ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream( objFile ) );
            out.writeObject( object );
            out.close();
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    protected Object readObject( File objFile ) {
        Object object = null;
        try {
            ObjectInputStream in = new ObjectInputStream( new FileInputStream( objFile ) );
            object = in.readObject();
            in.close();
            return object;
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        } catch ( ClassNotFoundException e ) {
            objFile.delete();
            e.printStackTrace();
        }
        return object;
    }

}
