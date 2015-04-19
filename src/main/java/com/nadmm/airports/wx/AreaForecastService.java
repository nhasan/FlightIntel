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

import com.nadmm.airports.utils.UiUtils;

import android.content.Intent;
import android.text.format.DateUtils;

public class AreaForecastService extends NoaaService {

    private final String FA_TEXT_PATH = "/data/products/fa/";

    private static final long FA_CACHE_MAX_AGE = DateUtils.HOUR_IN_MILLIS;

    public AreaForecastService() {
        super( "fa", FA_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_FA ) ) {
            String code = intent.getStringExtra( STATION_ID );
            File file = getDataFile( code );
            if ( !file.exists() ) {
                try {
                    String path = FA_TEXT_PATH+code;
                    fetch( AWC_HOST, path, null, file, false );
                } catch ( Exception e ) {
                    UiUtils.showToast( this, "Unable to fetch FA text: "+e.getMessage() );
                }
            }

            // Broadcast the result
            sendFileResultIntent( action, code, file );
        }
    }

}
