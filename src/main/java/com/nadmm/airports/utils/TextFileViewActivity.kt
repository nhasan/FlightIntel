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

package com.nadmm.airports.utils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nadmm.airports.FragmentActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;

import java.io.IOException;

public class TextFileViewActivity extends FragmentActivityBase {

    public static final String FILE_PATH = "FILE_PATH";
    public static final String LABEL_TEXT = "LABEL_TEXT";
    public static final String TITLE_TEXT = "TITLE_TEXT";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Bundle args = getIntent().getExtras();
        if ( args != null ) {
            String title = args.getString( TITLE_TEXT );
            setActionBarTitle( title );
        }

        addFragment( TextViewFragment.class, args );
    }

    public static class TextViewFragment extends FragmentBase {

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState ) {
            View view = null;
            Bundle args = getArguments();
            if ( args != null ) {
                String label = args.getString( LABEL_TEXT );
                String path = args.getString( FILE_PATH );

                view = inflate( R.layout.text_view );
                TextView tv = view.findViewById( R.id.text_label );
                tv.setText( label );
                tv = view.findViewById( R.id.text_content );
                try {
                    String text = FileUtils.readFile( path );
                    tv.setText( text );
                } catch ( IOException e ) {
                    tv.setText( "Unable to read FA file: " + e.getMessage() );
                }
            }

            return view;
        }
    }

}
