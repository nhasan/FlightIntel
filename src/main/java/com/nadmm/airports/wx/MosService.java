/*
 * FlightIntel for Pilots
 *
 * Copyright 2019 Nadeem Hasan <nhasan@nadmm.com>
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

import androidx.annotation.Nullable;

public class MosService extends NoaaService {

    private final String MOS_HOST = "www.nws.noaa.gov";
    private final String MOS_PATH = "/cgi-bin/mos/getmav.pl";
    private final String MOS_QUERY = "sta=%s";
    private final String MOS_FILE= "MOS.%s.txt";
    private static final long CACHE_MAX_AGE = 60*DateUtils.MINUTE_IN_MILLIS;

    public MosService() {
        super( "MOS", CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( @Nullable Intent intent ) {
        String action = intent.getAction();
        if ( action != null ) {
            if ( action.equals( "" ) ) {
                String code = intent.getStringExtra( TEXT_CODE );
                String filename = String.format( Locale.US, MOS_FILE, code );
                String query = String.format( Locale.US, MOS_QUERY, code );
                File file = getDataFile( filename );
                if ( !file.exists() ) {
                    try {
                        String path = MOS_PATH + filename;
                        fetch( MOS_HOST, path, query, file, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch MOS text: " + e.getMessage() );
                    }
                }

                // Broadcast the result
                sendFileResultIntent( action, code, file );
            }
        }

    }
}
