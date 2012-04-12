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

package com.nadmm.airports.utils;

import java.io.File;
import java.util.List;

import com.nadmm.airports.FlightIntel;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;

public class SystemUtils {

    public static final String EXTERNAL_STORAGE_DATA_DIRECTORY
            = Environment.getExternalStorageDirectory()
            + "/Android/data/"+FlightIntel.class.getPackage().getName();

    private final static String MIME_TYPE_PDF = "application/pdf";

    public static boolean canDisplayMimeType( Context context, String mimeType ) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent( Intent.ACTION_VIEW );
        intent.setType( mimeType );
        List<ResolveInfo> list = pm.queryIntentActivities( intent,
                PackageManager.MATCH_DEFAULT_ONLY );
        return !list.isEmpty();
    }

    public static boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals( state );
    }

    public static void startPDFViewer( Context context, String path ) {
        if ( SystemUtils.canDisplayMimeType( context, MIME_TYPE_PDF ) ) {
            // Fire an intent to view the PDF chart
            Intent viewChart = new Intent( Intent.ACTION_VIEW );
            Uri pdf = Uri.fromFile( new File( path ) );
            viewChart.setDataAndType( pdf, MIME_TYPE_PDF );
            context.startActivity( viewChart );
        } else {
            // No PDF viewer is installed, send user to Play Store
            UiUtils.showToast( context, "Please install a PDF viewer app first" );
            Intent market = new Intent( Intent.ACTION_VIEW );
            Uri uri = Uri.parse( "market://details?id=org.ebookdroid" );
            market.setData( uri );
            context.startActivity( market );
        }
    }

    public static File getExternalDir( String dir ) {
        return new File( EXTERNAL_STORAGE_DATA_DIRECTORY, dir );
    }

}
