/*
 * FlightIntel for Pilots
 *
 * Copyright 2017-2018 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.UiUtils;

import java.io.File;
import java.util.Locale;


public class WindsAloftService extends NoaaService {

    private final String FB_TEXT_PATH = "/data/products/fbwind/";
    private final String FB_TEXT_NAME = "F%s_fbwind_low_%s.txt";

    private static final long FB_CACHE_MAX_AGE = DateUtils.HOUR_IN_MILLIS/2;

    public WindsAloftService() {
        super( "fb", FB_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action != null ) {
            if ( action.equals( ACTION_GET_FB ) ) {
                String code = intent.getStringExtra( TEXT_CODE );
                String type = intent.getStringExtra( TEXT_TYPE );
                String filename = String.format( Locale.US, FB_TEXT_NAME, type, code );
                File file = getDataFile( filename );
                if ( !file.exists() ) {
                    try {
                        String path = FB_TEXT_PATH + filename;
                        fetch( AWC_HOST, path, null, file, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch FB text: " + e.getMessage() );
                    }
                }

                // Broadcast the result
                sendFileResultIntent( action, code, file );
            }
        }
    }

}
