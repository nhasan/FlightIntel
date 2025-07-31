/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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

public class IcingService extends NoaaService {

    private static final long ICING_CACHE_MAX_AGE = 30*DateUtils.MINUTE_IN_MILLIS;

    public IcingService() {
        super( "icing", ICING_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_ICING ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals(TYPE_GRAPHIC) ) {
                String imgType = intent.getStringExtra( IMAGE_TYPE );
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format( "%s_%s_sev.gif", imgType, code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = "/data/products/icing/" +imageName;
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch icing image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                sendImageResultIntent( action, code, imageFile );
            }
        }
    }

}
