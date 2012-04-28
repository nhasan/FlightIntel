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

package com.nadmm.airports.wx;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos1;
import com.nadmm.airports.DatabaseManager.LocationColumns;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public final class WxCursorAdapter extends ResourceCursorAdapter {

    private HashMap<String, Metar> mStationWx;

    public WxCursorAdapter( Context context, Cursor c ) {
        super( context, R.layout.wx_list_item, c, 0 );
    }

    public void setMetars( HashMap<String, Metar> wx ) {
        mStationWx = wx;
    }

    @Override
    public void bindView( View view, Context context, Cursor c ) {
        String name = c.getString( c.getColumnIndex( Wxs.STATION_NAME ) );
        TextView tv = (TextView) view.findViewById( R.id.wx_station_name );
        if ( name != null && name.length() > 0 ) {
            tv.setText( name );
        }

        String stationId = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
        tv = (TextView) view.findViewById( R.id.wx_station_id );
        tv.setText( stationId );

        view.setTag( stationId );

        StringBuilder info = new StringBuilder();
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        if ( city != null && city.length() > 0 ) {
            if ( info.length() > 0 ) {
                info.append( ", " );
            }
            info.append( city );
        }
        String state = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
        if ( state != null && state.length() > 0 ) {
            if ( info.length() > 0 ) {
                info.append( ", " );
            }
            info.append( state );
        }
        tv = (TextView) view.findViewById( R.id.wx_station_info );
        tv.setText( info.toString() );

        String freq = c.getString( c.getColumnIndex( Awos1.STATION_FREQUENCY ) );
        if ( freq == null || freq.length() == 0 ) {
            freq = c.getString( c.getColumnIndex( Awos1.SECOND_STATION_FREQUENCY ) );
        }
        if ( freq != null && freq.length() > 0 ) {
            try {
                tv = (TextView) view.findViewById( R.id.wx_station_freq );
                tv.setText( FormatUtils.formatFreq( Float.valueOf( freq ) ) );
            } catch ( NumberFormatException e ) {
            }
        }

        info.setLength( 0 );
        String type = c.getString( c.getColumnIndex( Awos1.WX_SENSOR_TYPE ) );
        if ( type == null || type.length() == 0 ) {
            type = "ASOS/AWOS";
        }
        info.append( type );
        info.append( ", " );
        int elevation = c.getInt( c.getColumnIndex( Wxs.STATION_ELEVATOIN_METER ) );
        info.append( FormatUtils.formatFeetMsl( DataUtils.metersToFeet( elevation ) ) );
        tv = (TextView) view.findViewById( R.id.wx_station_info2 );
        tv.setText( info.toString() );

        tv = (TextView) view.findViewById( R.id.wx_station_phone );
        String phone = c.getString( c.getColumnIndex( Awos1.STATION_PHONE_NUMBER ) );
        if ( phone != null && phone.length() > 0 ) {
            tv.setText( phone );
        }

        if ( c.getColumnIndex( LocationColumns.DISTANCE ) >= 0 
                && c.getColumnIndex( LocationColumns.BEARING ) >= 0 ) {
            tv = (TextView) view.findViewById( R.id.distance );
            float distance = c.getFloat( c.getColumnIndex( LocationColumns.DISTANCE ) );
            float bearing = c.getFloat( c.getColumnIndex( LocationColumns.BEARING ) );
            tv.setText( String.format( "%.1f NM %s",
                    distance, GeoUtils.getCardinalDirection( bearing ) ) );
            tv.setVisibility( View.VISIBLE );
        }

        Metar metar = mStationWx.get( stationId );
        showMetarInfo( view, c, metar );
    }

    protected void showMetarInfo( View view, Cursor c, Metar metar ) {
        if ( metar != null ) {
            if ( metar.isValid ) {
                // We have METAR for this station
                double lat = c.getDouble(
                        c.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
                double lon = c.getDouble(
                        c.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
                Location location = new Location( "" );
                location.setLatitude( lat );
                location.setLongitude( lon );
                float declination = GeoUtils.getMagneticDeclination( location );
    
                TextView tv = (TextView) view.findViewById( R.id.wx_station_name );
                WxUtils.setColorizedWxDrawable( tv, metar, declination );
    
                StringBuilder info = new StringBuilder();
                info.append( metar.flightCategory );
                if ( metar.wxList.size() > 0 ) {
                    for ( WxSymbol wx : metar.wxList ) {
                        if ( !wx.getSymbol().equals( "NSW" ) ) {
                            info.append( ", " );
                            info.append( wx.toString().toLowerCase() );
                        }
                    }
                }
                if ( metar.windGustKnots < Integer.MAX_VALUE ) {
                    info.append( ", gusting winds" );
                } else if ( metar.windSpeedKnots > 0 && metar.windDirDegrees == 0 ) {
                    info.append( ", variable winds" );
                } else if ( metar.windSpeedKnots > 10 ) {
                    info.append( ", strong winds" );
                } else if ( metar.windSpeedKnots > 0 ) {
                    info.append( ", light winds" );
                } else {
                    info.append( ", calm winds" );
                }
    
                tv = (TextView) view.findViewById( R.id.wx_station_wx );
                tv.setVisibility( View.VISIBLE );
                tv.setText( info.toString() );
    
                tv = (TextView) view.findViewById( R.id.wx_report_age );
                tv.setVisibility( View.VISIBLE );
                tv.setText( TimeUtils.formatElapsedTime( metar.observationTime ) );
            } else {
                TextView tv = (TextView) view.findViewById( R.id.wx_station_name );
                WxUtils.setColorizedWxDrawable( tv, metar, 0 );
            }
        } else {
            TextView tv = (TextView) view.findViewById( R.id.wx_station_name );
            UiUtils.setTextViewDrawable( tv, null );
        }
    }

}
