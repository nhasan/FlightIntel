/*
 * Airports for Android
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.SimpleAdapter;

public class SearchActivity extends ListActivity {
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();
        
        if ( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
            String query = intent.getStringExtra( SearchManager.QUERY );
            String result = "Result: "+query;
            //doMySearch(query);

            ArrayList<HashMap<String, String>> rows = new ArrayList<HashMap<String, String>>();

            HashMap<String, String> row = new HashMap<String, String>();
            row.put( "query", query );
            row.put( "result", result );
            rows.add( row );

            SimpleAdapter adapter = new SimpleAdapter( this, rows, 
                    android.R.layout.two_line_list_item,
                    new String[] { "query", "result" },
                    new int[] {android.R.id.text1, android.R.id.text2} );

            setListAdapter( adapter );
        }
    }
}
