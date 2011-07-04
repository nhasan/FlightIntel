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

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;

public class AirportsCursorAdapter extends SectionedCursorAdapter {

    public AirportsCursorAdapter( Context context, Cursor c ) {
        super( context, R.layout.airport_list_item, c, R.id.list_section );
    }

    @Override
    public String getSectionName() {
        return null;
    }

    @Override
    public void bindView( View view, Context context, Cursor c ) {
        TextView tv;
        String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        tv = (TextView) view.findViewById( R.id.facility_name );
        tv.setText( name );
        tv = (TextView) view.findViewById( R.id.facility_id );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.trim().length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        tv.setText( code );
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        String state_name = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        tv = (TextView) view.findViewById( R.id.location );
        tv.setText( city+", "+state_name );

        if ( c.getColumnIndex( Airports.DISTANCE ) >= 0 
                && c.getColumnIndex( Airports.BEARING ) >= 0 ) {
            // Check if we have distance information
            float distance = c.getFloat( c.getColumnIndex( Airports.DISTANCE ) );
            float bearing = c.getFloat( c.getColumnIndex( Airports.BEARING ) );
            tv = (TextView) view.findViewById( R.id.distance );
            tv.setText( String.format( "%.1f NM %s, ",
                    distance, GeoUtils.getCardinalDirection( bearing ) ) );
            tv = (TextView) view.findViewById( R.id.bearing );
            tv.setText( String.format( "initial course %.0f\u00B0 M", bearing ) );
        } else {
            tv = (TextView) view.findViewById( R.id.distance );
            tv.setVisibility( View.GONE );
            tv = (TextView) view.findViewById( R.id.bearing );
            tv.setVisibility( View.GONE );
        }

        tv = (TextView) view.findViewById( R.id.other_info );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String type = DataUtils.decodeLandingFaclityType( siteNumber );
        String fuel = c.getString( c.getColumnIndex( Airports.FUEL_TYPES ) );
        String elev = c.getString( c.getColumnIndex( Airports.ELEVATION_MSL ) );
        String ctaf = c.getString( c.getColumnIndex( Airports.CTAF_FREQ ) );
        String unicom = c.getString( c.getColumnIndex( Airports.UNICOM_FREQS ) );
        String status = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
        if ( status.equals( "O" ) ) {
            tv.setText( type+", "+elev+"' MSL"
                    +( ctaf != null && ctaf.length() > 0? ", "+ctaf : 
                        (unicom.length() > 0? ", "+unicom : "") )
                    +( fuel.length() > 0? ", "+DataUtils.decodeFuelTypes( fuel ) : "" ) );
        } else {
            tv.setText( type+", "+DataUtils.decodeStatus( status ) );
        }
    }

}
