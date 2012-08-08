/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.net.ConnectivityManagerCompat;

import com.nadmm.airports.PreferencesActivity;

public class NetworkUtils {

    private static final byte[] sBuffer = new byte[ 32*1024 ];

    public static final String RESULT_PROGRESS = "RESULT_PROGRESS";
    public static final String RESULT_LENGTH = "RESULT_LENGTH";

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

    public static boolean isConnectedToMeteredNetwork( Context context ) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService( 
                Context.CONNECTIVITY_SERVICE );
        ConnectivityManagerCompat cm2 = new ConnectivityManagerCompat();
        return cm2.isActiveNetworkMetered( cm );
    }

    public static void checkNetworkAndDownload( Context context, final Runnable runnable ) {
        if ( !NetworkUtils.isNetworkAvailable( context ) ) {
            UiUtils.showToast( context, "Please check your internet connection" );
        }

        if ( NetworkUtils.isConnectedToMeteredNetwork( context ) ) {
            AlertDialog.Builder builder = new AlertDialog.Builder( context );
            builder.setMessage( "You are conneteced to a metered network such as mobile data"
                    +" or tethered to mobile data.\nContinue download?" )
                   .setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int id ) {
                            runnable.run();
                        }
                   } )
                   .setNegativeButton( "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int id ) {
                        }
                   } );
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            runnable.run();
        }
    }

    public static boolean useCacheContentOnly( Context context ) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( context );
        boolean alwaysAutoFetch = prefs.getBoolean(
                PreferencesActivity.KEY_AUTO_DOWNLOAD_ON_3G, false );
        boolean cacheOnly = ( !alwaysAutoFetch 
                && NetworkUtils.isConnectedToMeteredNetwork( context ) );
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

    public static boolean doHttpGet( Context context, HttpClient httpClient, String host,
            String path, String query, File file, ResultReceiver receiver,
            Class<? extends FilterInputStream> filter ) throws Exception {
        return doHttpGet( context, httpClient, host, 80, path, query, file, receiver, filter );
    }

    public static boolean doHttpGet( Context context, HttpClient httpClient, String host,
            int port, String path, String query, File file, ResultReceiver receiver,
            Class<? extends FilterInputStream> filter ) throws Exception {
        URI uri = URIUtils.createURI( "http", host, port, path, query, null );
        return doHttpGet( context, httpClient, uri, file, receiver, filter );
    }

    public static boolean doHttpGet( Context context, HttpClient httpClient, URI uri,
            File file, ResultReceiver receiver, Class<? extends FilterInputStream> filter )
            throws Exception {
        if ( !NetworkUtils.isNetworkAvailable( context ) ) {
            return false;
        }

        InputStream in = null;
        OutputStream out = null;

        try {
            HttpGet get = new HttpGet( uri );
            HttpResponse response = httpClient.execute( get );

            int status = response.getStatusLine().getStatusCode();
            if ( status != HttpStatus.SC_OK ) {
                throw new Exception( response.getStatusLine().getReasonPhrase() );
            }

            long length = response.getEntity().getContentLength();

            out = new FileOutputStream( file );
            in = response.getEntity().getContent();
            if ( filter != null ) {
                @SuppressWarnings("unchecked")
                Constructor<FilterInputStream> ctor =
                        (Constructor<FilterInputStream>) filter.getConstructor( InputStream.class );
                in = ctor.newInstance( new BufferedInputStream( in ) );
            }

            int count;
            long progress = 0;
            while ( ( count = in.read( sBuffer, 0, sBuffer.length ) ) != -1 ) {
                out.write( sBuffer, 0, count );
                if ( receiver != null ) {
                    progress += count;
                    Bundle bundle = new Bundle();
                    bundle.putLong( RESULT_PROGRESS, progress );
                    bundle.putLong( RESULT_LENGTH, length );
                    receiver.send( 0, bundle );
                }
            }
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
        return true;
    }

}
