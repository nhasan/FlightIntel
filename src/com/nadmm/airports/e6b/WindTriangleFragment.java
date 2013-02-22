/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.e6b;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;

public class WindTriangleFragment extends FragmentBase {

    EditText mWindSpeed;
    EditText mwindDir;
    EditText mRunway;
    EditText mHeadWind;
    EditText mCrossWind;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflate( R.layout.e6b_wind_triangle_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mWindSpeed = (EditText) findViewById( R.id.e6b_wind_speed );
        mwindDir = (EditText) findViewById( R.id.e6b_wind_dir );
        mRunway = (EditText) findViewById( R.id.e6b_runway_id );
        mHeadWind = (EditText) findViewById( R.id.e6b_head_wind );
        mCrossWind = (EditText) findViewById( R.id.e6b_cross_wind );

        mHeadWind.setFocusable( false );
        mCrossWind.setFocusable( false );
    }

}
