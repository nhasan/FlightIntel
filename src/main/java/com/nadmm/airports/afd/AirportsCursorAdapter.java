/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.afd;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.LocationColumns;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;

import java.util.Locale;

public class AirportsCursorAdapter extends ResourceCursorAdapter {

    private final StringBuilder mStringBuilder = new StringBuilder();

    static class ViewHolder {
        TextView name;
        TextView id;
        TextView location;
        TextView distance;
        TextView other;
    }

    public AirportsCursorAdapter( Context context, Cursor c ) {
        super( context, R.layout.airport_list_item, c, 0 );
    }

    @Override
    public void bindView( View view, Context context, Cursor c ) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if ( holder == null ) {
            holder = new ViewHolder();
            holder.name = view.findViewById( R.id.facility_name );
            holder.id = view.findViewById( R.id.facility_id );
            holder.location = view.findViewById( R.id.location );
            holder.distance = view.findViewById( R.id.distance );
            holder.other = view.findViewById( R.id.other_info );
            view.setTag( holder );
        }

        String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String type = DataUtils.decodeLandingFaclityType( siteNumber );
        holder.name.setText( String.format( "%s %s", name, type ) );
        String id = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( id == null || id.trim().length() == 0 ) {
            id = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        holder.id.setText( id );
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
        String use = c.getString( c.getColumnIndex( Airports.FACILITY_USE ) );
        holder.location.setText( String.format( "%s, %s, %s", city, state,
                DataUtils.decodeFacilityUse( use ) ) );

        if ( c.getColumnIndex( LocationColumns.DISTANCE ) >= 0
                && c.getColumnIndex( LocationColumns.BEARING ) >= 0 ) {
            // Check if we have distance information
            float distance = c.getFloat( c.getColumnIndex( LocationColumns.DISTANCE ) );
            float bearing = c.getFloat( c.getColumnIndex( LocationColumns.BEARING ) );
            holder.distance.setText( String.format( Locale.US, "%.1f NM %s, initial course %.0f\u00B0 M",
                    distance, GeoUtils.getCardinalDirection( bearing ), bearing ) );
        } else {
            holder.distance.setVisibility( View.GONE );
        }

        String fuel = c.getString( c.getColumnIndex( Airports.FUEL_TYPES ) );
        float elev = c.getFloat( c.getColumnIndex( Airports.ELEVATION_MSL ) );
        String ctaf = c.getString( c.getColumnIndex( Airports.CTAF_FREQ ) );
        String unicom = c.getString( c.getColumnIndex( Airports.UNICOM_FREQS ) );
        String status = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
        mStringBuilder.setLength( 0 );
        if ( status.equals( "O" ) ) {
            mStringBuilder.append( FormatUtils.formatFeetMsl( elev ) );
            if ( ctaf != null && ctaf.length() > 0 ) {
                mStringBuilder.append( ", " );
                mStringBuilder.append( ctaf );
            } else if ( unicom != null && unicom.length() > 0 ) {
                mStringBuilder.append( ", " );
                mStringBuilder.append( unicom );
            }
            if ( fuel != null && fuel.length() > 0 ) {
                mStringBuilder.append( ", " );
                mStringBuilder.append( DataUtils.decodeFuelTypes( fuel ) );
            }
        } else {
            mStringBuilder.append( type );
            mStringBuilder.append( ", " );
            mStringBuilder.append( DataUtils.decodeStatus( status ) );
        }
        holder.other.setText( mStringBuilder.toString() );
    }

}
