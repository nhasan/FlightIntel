/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2017 Nadeem Hasan <nhasan@nadmm.com>
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

public class SigWxFragment extends WxGraphicFragmentBase {

    private static final String[] sSigWxCodes = new String[] {
        "12_0",
        "18_0",
        "00_0",
        "06_0",
        "00_1",
        "06_1",
        "12_1",
        "18_1"
    };

    private static final String[] sSigWxNames = new String[] {
        "12 hr Prognosis (Vaild 0000 UTC)",
        "12 hr Prognosis (Valid 0600 UTC)",
        "12 hr Prognosis (Valid 1200 UTC)",
        "12 hr Prognosis (Valid 1800 UTC)",
        "24 hr Prognosis (Valid 0000 UTC)",
        "24 hr Prognosis (Valid 0600 UTC)",
        "24 hr Prognosis (Valid 1200 UTC)",
        "24 hr Prognosis (Valid 1800 UTC)",
    };

    public SigWxFragment() {
        super( NoaaService.ACTION_GET_SIGWX, sSigWxCodes, sSigWxNames );
        setTitle( "Significant Wx");
        setLabel( "Select SigWx Image" );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), SigWxService.class );
    }

    @Override
    protected String getProduct() {
        return "sigwx";
    }

}
