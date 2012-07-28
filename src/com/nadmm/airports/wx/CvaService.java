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

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.UiUtils;

public class CvaService extends NoaaService {

    private final String CVA_IMAGE_NAME = "cva_%s_%s.gif";
    private final String CVA_IMAGE_PATH = "/tools/weatherproducts/cva/default/"
            + "loadImage/region/%s/product/%s/zoom/true";

    private static final long CVA_CACHE_MAX_AGE = 30*DateUtils.MINUTE_IN_MILLIS;
    public static final String CVA_TYPE = "CVA_TYPE";

    public CvaService() {
        super( "cva", CVA_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_CVA ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                String cva = intent.getStringExtra( CVA_TYPE );
                String imageName = String.format( CVA_IMAGE_NAME, cva, code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = String.format( CVA_IMAGE_PATH, code, cva );
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch CVA image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                sendResultIntent( action, code, imageFile );
            }
        }
    }

}
