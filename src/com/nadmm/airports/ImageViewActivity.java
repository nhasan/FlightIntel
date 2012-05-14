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

package com.nadmm.airports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class ImageViewActivity extends ActivityBase {

    public static final String IMAGE_TITLE = "IMAGE_TITLE";
    public static final String IMAGE_SUBTITLE = "IMAGE_SUBTITLE";
    public static final String IMAGE_PATH = "IMAGE_PATH";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout );

        Bundle args = getIntent().getExtras();
        addFragment( ImageViewFragment.class, args );
    }

    public static class ImageViewFragment extends FragmentBase {

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            WebView view = new WebView( getActivity() );
            view.getSettings().setBuiltInZoomControls( true );

            Bundle args = getArguments();
            String path = args.getString( IMAGE_PATH );
            view.loadUrl( "file://"+path );

            String title = args.getString( IMAGE_TITLE );
            setActionBarTitle( title );
            String subtitle = args.getString( IMAGE_SUBTITLE );
            setActionBarSubtitle( subtitle );

            return view;
        }

    }

}
