/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

public class SystemUtils {

    private final static String MIME_TYPE_PDF = "application/pdf";

    private SystemUtils() {}

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
            File pdfFile = new File( path );
            Uri pdfUri = FileProvider.getUriForFile( context,
                    "com.nadmm.airports.fileprovider", pdfFile );
            Intent intent = new Intent( Intent.ACTION_VIEW );
            intent.setDataAndType( pdfUri, MIME_TYPE_PDF );
            intent.setFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );
            context.startActivity( intent );
        } else {
            // No PDF viewer is installed, send user to Play Store
            UiUtils.showToast( context, "Please install a PDF viewer app first" );
            Intent market = new Intent( Intent.ACTION_VIEW );
            Uri uri = Uri.parse( "market://details?id=com.google.android.apps.pdfviewer" );
            market.setData( uri );
            context.startActivity( market );
        }
    }

    public static File getExternalDir( Context context, String dirName ) {
        return context.getExternalFilesDirs( dirName )[ 0 ];
    }

    public static File getExternalFile( Context context, String dirName, String fileName ) {
        File dir = SystemUtils.getExternalDir( context, dirName );
        return new File( dir, fileName );
    }

}
