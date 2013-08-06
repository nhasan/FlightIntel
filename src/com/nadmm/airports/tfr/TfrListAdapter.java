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

package com.nadmm.airports.tfr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;

public class TfrListAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private final TfrList mTfrList;
    private Context mContext;
    private int mActiveColor;
    private int mInactiveColor;

    public TfrListAdapter( Context context, TfrList tfrList ) {
        mContext = context;
        mInflater = LayoutInflater.from( mContext );
        mTfrList = tfrList;
        mActiveColor = mContext.getResources().getColor( R.color.red );
        mInactiveColor = mContext.getResources().getColor( R.color.lightgray );
    }

    @Override
    public int getCount() {
        return mTfrList.entries.size();
    }

    @Override
    public Object getItem( int position ) {
        return mTfrList.entries.get( position );
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        if ( convertView == null ) {
            convertView = mInflater.inflate( R.layout.tfr_list_item, null );
        }

        Tfr tfr = (Tfr) getItem( position );

        TextView tv;
        int index = tfr.notamId.indexOf( ' ' );
        if ( index > 0 ) {
            tv = (TextView) convertView.findViewById( R.id.tfr_agency );
            tv.setText( tfr.notamId.substring( 0, index ) );
        }
        tv = (TextView) convertView.findViewById( R.id.tfr_name );
        tv.setText( tfr.name );
        tv = (TextView) convertView.findViewById( R.id.tfr_time );
        tv.setText( tfr.formatTimeRange( mContext ) );
        tv = (TextView) convertView.findViewById( R.id.tfr_active );
        tv.setText( tfr.isExpired()? "Expired" : tfr.isActive()? "Active" : "Inactive" );
        tv.setTextColor( tfr.isActive()? mActiveColor : mInactiveColor );
        tv = (TextView) convertView.findViewById( R.id.tfr_type );
        tv.setText( tfr.type );
        tv = (TextView) convertView.findViewById( R.id.tfr_altitudes );
        tv.setText( tfr.formatAltitudeRange() );

        return convertView;
    }

}
