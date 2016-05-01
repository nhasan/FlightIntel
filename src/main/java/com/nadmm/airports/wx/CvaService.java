/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2016 Nadeem Hasan <nhasan@nadmm.com>
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
import java.util.Locale;

import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

import com.nadmm.airports.utils.UiUtils;

public class CvaService extends NoaaService {

    private final String CVA_IMAGE_NAME = "NCVA%s.gif";
    private final String CVA_IMAGE_PATH = "/adds/data/ceil_vis/";

    private static final long CVA_CACHE_MAX_AGE = 10*DateUtils.MINUTE_IN_MILLIS;

    public CvaService() {
        super( "cva", CVA_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_CVA ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_IMAGE ) ) {
                String imgType = intent.getStringExtra( IMAGE_TYPE );
                String code = intent.getStringExtra( IMAGE_CODE );
                String suffix = imgType;
                if ( !code.equals( "INA" ) ) {
                    suffix += "_";
                    suffix += code;
                }
                String imageName = String.format( Locale.US, CVA_IMAGE_NAME, suffix );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = CVA_IMAGE_PATH+imageName;
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch CVA image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                sendImageResultIntent( action, code, imageFile );
            }
        }
    }

}
