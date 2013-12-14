/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.nadmm.airports.R;

public class NavAdapter extends ArrayAdapter<String> {

    private LayoutInflater mInflater;
    private String mTitle;

    public NavAdapter( Context context, int title, String[] values ) {
        super( context, 0, values );
        Resources res = context.getResources();
        mTitle = res.getString( title );
        mInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        setDropDownViewResource( R.layout.support_simple_spinner_dropdown_item );
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        if ( convertView == null ) {
            convertView = mInflater.inflate( R.layout.actionbar_spinner_item_2, null );
            TextView tv = (TextView) convertView.findViewById( android.R.id.text1 );
            tv.setText( mTitle );
        }
        TextView tv = (TextView) convertView.findViewById( android.R.id.text2 );
        tv.setText( getItem( position ) );
        return convertView;
    }
}
