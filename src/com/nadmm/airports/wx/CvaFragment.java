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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.nadmm.airports.R;


public class CvaFragment extends WxMapFragmentBase {

    private static final String[] mCvaTypeNames = {
        "CVA - Flight Category",
        "CVA - Ceiling",
        "CVA - Visibility"
    };

    private static final String[] mCvaTypeCodes = {
        "metarsNCVAfcat",
        "metarsNCVAceil",
        "metarsNCVAvis"
    };

    private Spinner mSpinner;

    public CvaFragment() {
        super( NoaaService.ACTION_GET_CVA, WxRegions.sWxRegionCodes,
                WxRegions.sWxRegionNames, R.layout.wx_cav_detail_view );
        setLabel( "Select Region" );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View v = super.onCreateView( inflater, container, savedInstanceState );
        mSpinner = (Spinner) v.findViewById( R.id.cav_type );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( getActivity(),
                R.layout.sherlock_spinner_item, mCvaTypeNames );
        adapter.setDropDownViewResource( R.layout.sherlock_spinner_dropdown_item );
        mSpinner.setAdapter( adapter );
        return v;
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), CvaService.class );
    }

    @Override
    protected void setServiceParams( Intent intent ) {
        int pos = mSpinner.getSelectedItemPosition();
        intent.putExtra( CvaService.CVA_TYPE, mCvaTypeCodes[ pos ] );
        String region = intent.getStringExtra( NoaaService.IMAGE_CODE );
        if ( region.equals( "INA" ) ) {
            intent.putExtra( NoaaService.IMAGE_CODE, "US" );
        }
    }

}
