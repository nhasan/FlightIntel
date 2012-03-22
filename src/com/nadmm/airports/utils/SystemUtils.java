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

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;

public class SystemUtils {

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
        if ( !Environment.MEDIA_MOUNTED.equals( state ) ) {
            return false;
        }
        else if ( Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return false;
        }
        return true;
    }

}
