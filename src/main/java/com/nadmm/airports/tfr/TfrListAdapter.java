/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;

import java.util.Locale;

public class TfrListAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final TfrList mTfrList;
    private final ActivityBase mContext;

    public TfrListAdapter( ActivityBase activity, TfrList tfrList ) {
        mContext = activity;
        mInflater = LayoutInflater.from( mContext );
        mTfrList = tfrList;
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
            tv = convertView.findViewById( R.id.tfr_agency );
            tv.setText( tfr.notamId.substring( 0, index ) );
        }
        tv = convertView.findViewById( R.id.tfr_name );
        if ( tfr.notamId.equals( tfr.name ) ) {
            tv.setText( tfr.notamId );
        } else {
            tv.setText( String.format( Locale.US, "%s - %s", tfr.notamId, tfr.name ) );
        }
        tv = convertView.findViewById( R.id.tfr_location );
        tv.setText( tfr.formatLocation() );
        tv = convertView.findViewById( R.id.tfr_time );
        tv.setText( tfr.formatTimeRange( mContext ) );
        tv = convertView.findViewById( R.id.tfr_active );
        tv.setText( tfr.isExpired()? "Expired" : tfr.isActive()? "Active" : "Inactive" );
        tv = convertView.findViewById( R.id.tfr_type );
        tv.setText( tfr.type );
        tv = convertView.findViewById( R.id.tfr_altitudes );
        tv.setText( tfr.formatAltitudeRange() );

        return convertView;
    }

}
