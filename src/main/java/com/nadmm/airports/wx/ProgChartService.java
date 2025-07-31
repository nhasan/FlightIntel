/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2019 Nadeem Hasan <nhasan@nadmm.com>
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

public class ProgChartService extends NoaaService {

    private final String PROGCHART_IMAGE_NAME = "%s.gif";
    private final String PROGCHART_IMAGE_PATH = "/data/products/progs/";
    private static final long PROGCHART_CACHE_MAX_AGE = 240*DateUtils.MINUTE_IN_MILLIS;

    public ProgChartService() {
        super( "progchart", PROGCHART_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_PROGCHART ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals(TYPE_GRAPHIC) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format( PROGCHART_IMAGE_NAME, code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        StringBuilder path = new StringBuilder();
                        path.append( PROGCHART_IMAGE_PATH ).append( imageName );
                        fetchFromNoaa( path.toString(), null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch PROGCHART image: "
                                +e.getMessage() );
                    }
                }

                // Broadcast the result
                sendImageResultIntent( action, code, imageFile );
            }
        }
    }

}
