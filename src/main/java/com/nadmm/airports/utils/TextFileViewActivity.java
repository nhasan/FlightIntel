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
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;

import java.io.IOException;

public class TextFileViewActivity extends ActivityBase {

    public static final String FILE_PATH = "FILE_PATH";
    public static final String LABEL_TEXT = "LABEL_TEXT";
    public static final String TITLE_TEXT = "TITLE_TEXT";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.text_view );

        Bundle args = getIntent().getExtras();
        String title = args.getString( TITLE_TEXT );
        String label = args.getString( LABEL_TEXT );
        String path = args.getString( FILE_PATH );

        setTitle( title );

        TextView tv = (TextView) findViewById( R.id.text_label );
        tv.setText( label );
        tv = (TextView) findViewById( R.id.text_content );
        try {
            String text = FileUtils.readFile( path );
            tv.setText( text );
        } catch ( IOException e ) {
            tv.setText( "Unable to read FA file: "+e.getMessage() );
        }
    }

}
