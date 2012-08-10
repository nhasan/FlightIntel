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

import android.content.Intent;


public class CvaFragment extends WxMapFragmentBase {

    private static final String[] sTypeCodes = {
        "metarsNCVAfcat",
        "metarsNCVAceil",
        "metarsNCVAvis"
    };

    private static final String[] sTypeNames = {
        "CVA - Flight Category",
        "CVA - Ceiling",
        "CVA - Visibility"
    };

    public CvaFragment() {
        super( NoaaService.ACTION_GET_CVA,
                WxRegions.sWxRegionCodes, WxRegions.sWxRegionNames, sTypeCodes, sTypeNames );
        setTitle( "Ceiling and Visibility");
        setLabel( "Select Region" );
        setHelpText( "By FAA policy, CVA is a Supplementary Weather Product for "
                + "enhanced situational awareness only. CVA must only be used with primary "
                + "products such as METARs, TAFs and AIRMETs." );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), CvaService.class );
    }

    @Override
    protected void setServiceParams( Intent intent ) {
        String region = intent.getStringExtra( NoaaService.IMAGE_CODE );
        if ( region.equals( "INA" ) ) {
            intent.putExtra( NoaaService.IMAGE_CODE, "US" );
        }
    }

}
