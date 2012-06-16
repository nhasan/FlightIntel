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

package com.nadmm.airports.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.nadmm.airports.PreferencesActivity;

public class NetworkUtils {

    public static boolean isNetworkAvailable( Context context ) {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService( 
                Context.CONNECTIVITY_SERVICE );
        NetworkInfo network = connMan.getActiveNetworkInfo();
        if ( network == null || !network.isConnected() ) {
            UiUtils.showToast( context, "Please check your internet connection" );
            return false;
        }

        return true;
    }

    public static boolean isConnectedToWifi( Context context ) {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService( 
                Context.CONNECTIVITY_SERVICE );
        NetworkInfo network = connMan.getActiveNetworkInfo();
        return ( network != null && network.getType() == ConnectivityManager.TYPE_WIFI );
    }

    public static boolean useCacheContentOnly( Context context ) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( context );
        boolean alwaysAutoFetch = prefs.getBoolean(
                PreferencesActivity.KEY_AUTO_DOWNLOAD_ON_3G, false );
        boolean cacheOnly = ( !alwaysAutoFetch 
                && !NetworkUtils.isConnectedToWifi( context ) );
        return cacheOnly;
    }

    public static HttpClient getHttpClient() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register( new Scheme( "http", PlainSocketFactory.getSocketFactory(), 80 ) );
        HttpParams params = new BasicHttpParams();
        ClientConnectionManager cm = new ThreadSafeClientConnManager( params, registry );
        HttpClient client = new DefaultHttpClient( cm, params );
        return client;
    }

    public static void doHttpGet( Context context, HttpClient httpClient, HttpHost target,
            String path, File pdfFile ) {
        InputStream in = null;
        FileOutputStream out = null;

        try {
            URI uri = new URI( path );
            HttpGet get = new HttpGet( uri );
            HttpResponse response = httpClient.execute( target, get );

            int status = response.getStatusLine().getStatusCode();
            if ( status != HttpStatus.SC_OK ) {
                throw new Exception( response.getStatusLine().getReasonPhrase() );
            }

            byte[] buffer = new byte[ 32*1024 ];
            int count;
            out = new FileOutputStream( pdfFile );
            HttpEntity entity = response.getEntity();
            in = entity.getContent();

            while ( ( count = in.read( buffer, 0, buffer.length ) ) != -1 ) {
                out.write( buffer, 0, count );
            }
        } catch ( Exception e ) {
            UiUtils.showToast( context, "Unable to download file: "+e.getMessage() );
        } finally {
            try {
                if ( in != null ) {
                    in.close();
                }
                if ( out != null ) {
                    out.close();
                }
            } catch ( IOException e ) {
            }
        }
    }

}
