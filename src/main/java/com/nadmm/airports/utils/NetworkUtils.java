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

package com.nadmm.airports.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.net.ConnectivityManagerCompat;
import android.support.v7.preference.PreferenceManager;

import com.nadmm.airports.PreferencesActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.zip.GZIPInputStream;

@SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
public class NetworkUtils {

    private static final byte[] sBuffer = new byte[ 32 * 1024 ];

    public static final String CONTENT_PROGRESS = "CONTENT_PROGRESS";
    public static final String CONTENT_LENGTH = "CONTENT_LENGTH";
    public static final String CONTENT_NAME = "CONTENT_NAME";

    private NetworkUtils() {}

    public static boolean isNetworkAvailable( Context context ) {
        return isNetworkAvailable( context, true );
    }

    public static boolean isNetworkAvailable( Context context, boolean showMsg ) {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE );
        NetworkInfo network = connMan.getActiveNetworkInfo();
        if ( showMsg && ( network == null || !network.isConnected() ) ) {
            UiUtils.showToast( context, "Please check your internet connection" );
            return false;
        }

        return true;
    }

    public static boolean isConnectedToMeteredNetwork( Context context ) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE );
        return ConnectivityManagerCompat.isActiveNetworkMetered( cm );
    }

    public static void checkNetworkAndDownload( Context context, final Runnable runnable ) {
        if ( !NetworkUtils.isNetworkAvailable( context ) ) {
            UiUtils.showToast( context, "Please check your internet connection" );
        }

        if ( NetworkUtils.isConnectedToMeteredNetwork( context ) ) {
            AlertDialog.Builder builder = new AlertDialog.Builder( context );
            builder.setMessage( "You are conneteced to a metered network such as mobile data"
                    + " or tethered to mobile data.\nContinue download?" )
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
        } else {
            runnable.run();
        }
    }

    public static boolean canDownloadData( Context context ) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( context );
        boolean alwaysAutoFetch = prefs.getBoolean(
                PreferencesActivity.KEY_AUTO_DOWNLOAD_ON_3G, false );
        return ( alwaysAutoFetch
                || !NetworkUtils.isConnectedToMeteredNetwork( context ) );
    }

    /*
     *  An input stream that works as a pass-through filter but counts the bytes read from
     *  the underlying stream. It is needed for compressed streams that are wrapped by a
     *  GzipInputStream filter that only reports the decompressed byte count. Wrapping raw
     *  stream with this filter stream allows us to keep track of the download progress
     *  when all we know is the compressed size not the decompressed size.
     */
    private static class CountingInputStream extends BufferedInputStream {

        // Count of bytes actually read from the raw stream
        private int mCount;

        public CountingInputStream( InputStream in ) {
            super( in );
            mCount = 0;
        }

        @Override
        public int read( byte[] buffer, int offset, int byteCount ) throws IOException {
            int count = super.read( buffer, offset, byteCount );
            if ( count != -1 ) {
                mCount += count;
            }
            return count;
        }

        public int getCount() {
            return mCount;
        }
    }

    public static boolean doHttpGetGzip( Context context, URL url,
                                     File file, ResultReceiver receiver, Bundle result )
            throws Exception {
        return doHttpGet( context, url, file, receiver, result, GZIPInputStream.class );
    }

    public static boolean doHttpGet( Context context, String host, String path, File file )
            throws Exception {
        return doHttpGet( context, "http", host, 80, path, null, file, null, null, null );
    }

    public static boolean doHttpsGet( Context context, String host, String path, File file )
            throws Exception {
        return doHttpGet( context, "https", host, 443, path, null, file, null, null, null );
    }

    public static boolean doHttpGet( Context context, String host, int port, String path,
                                     String query, File file, ResultReceiver receiver,
                                     Bundle result, Class<? extends FilterInputStream> filter )
            throws Exception {
        URI uri = new URI( "http", null, host, port, path, query, null );
        return doHttpGet( context, uri.toURL(), file, receiver, result, filter );
    }

    public static boolean doHttpGet( Context context, String scheme, String host, int port,
                                     String path, String query, File file, ResultReceiver receiver,
                                     Bundle result, Class<? extends FilterInputStream> filter )
            throws Exception {
        URI uri = new URI( scheme, null, host, port, path, query, null );
        return doHttpGet( context, uri.toURL(), file, receiver, result, filter );
    }

    public static boolean doHttpGet( Context context, URL url,
                                     File file, ResultReceiver receiver, Bundle result,
                                     Class<? extends FilterInputStream> filter )
            throws Exception {
        if ( !NetworkUtils.isNetworkAvailable( context ) ) {
            return false;
        }

        if ( receiver != null && result == null ) {
            throw new Exception( "Result cannot be null when receiver is passed" );
        }

        InputStream f = null;
        CountingInputStream in = null;
        OutputStream out = null;

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int status = conn.getResponseCode();
            if ( status != HttpURLConnection.HTTP_OK ) {
                if ( receiver != null ) {
                    // Signal the receiver that download is aborted
                    result.putLong( CONTENT_LENGTH, 0 );
                    result.putLong( CONTENT_PROGRESS, 0 );
                    receiver.send( 2, result );
                }
                throw new Exception( conn.getResponseMessage() );
            }

            long length = conn.getContentLength();

            if ( receiver != null ) {
                result.putLong( CONTENT_LENGTH, length );
            }

            out = new FileOutputStream( file );
            in = new CountingInputStream( conn.getInputStream() );

            if ( filter != null ) {
                @SuppressWarnings("unchecked")
                Constructor<FilterInputStream> ctor =
                        (Constructor<FilterInputStream>) filter.getConstructor( InputStream.class );
                f = ctor.newInstance( in );
            } else {
                f = in;
            }

            long chunk = Math.max( length/100, 16*1024 );
            long last = 0;

            int count;
            while ( ( count = f.read( sBuffer ) ) != -1 ) {
                out.write( sBuffer, 0, count );
                if ( receiver != null ) {
                    long current = in.getCount();
                    long delta = current - last;
                    if ( delta >= chunk ) {
                        result.putLong( CONTENT_PROGRESS, current );
                        receiver.send( 0, result );
                        last = current;
                    }
                }
            }
            if ( receiver != null ) {
                // If compressed, the filter stream may not read the entire source stream
                result.putLong( CONTENT_PROGRESS, length );
                receiver.send( 1, result );
            }
        } finally {
            try {
                if ( f != null ) {
                    f.close();
                }
                if ( out != null ) {
                    out.close();
                }
            } catch ( IOException ignored ) {
            }
        }
        return true;

    }

}
