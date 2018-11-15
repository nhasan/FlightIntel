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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nadmm.airports.R;

public class ArrayAdapterBase extends BaseAdapter {

    private LayoutInflater mInflater;
    private String[] mCodes;
    private String[] mNames;
    private int mPendingPos;

    public ArrayAdapterBase( Context context, String[] codes, String[] names ) {
        mInflater = LayoutInflater.from( context );
        mCodes = codes;
        mNames = names;
        clearPendingPos();
    }

    @Override
    public int getCount() {
        return mCodes.length;
    }

    @Override
    public Object getItem( int position ) {
        return mCodes[ position ];
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        if ( convertView == null ) {
            convertView = mInflater.inflate( R.layout.list_item_text1, parent );
        }
        TextView tv = convertView.findViewById( R.id.text );
        tv.setText( mNames[ position ] );
        View view = convertView.findViewById( R.id.progress );
        view.setVisibility( position == mPendingPos? View.VISIBLE : View.INVISIBLE );
        return convertView;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mPendingPos == -1;
    }

    public void setPendingPos( int position ) {
        mPendingPos = position;
        notifyDataSetChanged();
    }

    public int getPendingPos() {
        return mPendingPos;
    }

    public void clearPendingPos() {
        setPendingPos( -1 );
    }

    public String getDisplayText( String code ) {
        for ( int i=0; i<mCodes.length; ++i ) {
            if ( code.equals( mCodes[ i ] ) ) {
                return mNames[ i ];
            }
        }
        return null;
    }

}
