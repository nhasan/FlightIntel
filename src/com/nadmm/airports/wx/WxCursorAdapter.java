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
import com.nadmm.airports.utils.*;

import java.util.HashMap;
import java.util.Locale;

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
        if ( info.length() > 0 ) {
            tv.setText( info.toString() );
        } else {
            tv.setText( "Location not available" );
        }

        String freq = c.getString( c.getColumnIndex( Awos1.STATION_FREQUENCY ) );
        if ( freq == null || freq.length() == 0 ) {
            freq = c.getString( c.getColumnIndex( Awos1.SECOND_STATION_FREQUENCY ) );
        }
        tv = (TextView) view.findViewById( R.id.wx_station_freq );
        if ( freq != null && freq.length() > 0 ) {
            try {
                tv.setText( FormatUtils.formatFreq( Float.valueOf( freq ) ) );
            } catch ( NumberFormatException e ) {
                tv.setText( freq );
            }
        } else {
            tv.setText( "" );
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
        info.append( " elev." );
        tv = (TextView) view.findViewById( R.id.wx_station_info2 );
        tv.setText( info.toString() );

        tv = (TextView) view.findViewById( R.id.wx_station_phone );
        String phone = c.getString( c.getColumnIndex( Awos1.STATION_PHONE_NUMBER ) );
        if ( phone != null && phone.length() > 0 ) {
            tv.setText( phone );
        } else {
            tv.setText( "" );
        }

        tv = (TextView) view.findViewById( R.id.wx_station_distance );
        if ( c.getColumnIndex( LocationColumns.DISTANCE ) >= 0
                && c.getColumnIndex( LocationColumns.BEARING ) >= 0 ) {
            float distance = c.getFloat( c.getColumnIndex( LocationColumns.DISTANCE ) );
            float bearing = c.getFloat( c.getColumnIndex( LocationColumns.BEARING ) );
            info.setLength( 0 );
            info.append( FormatUtils.formatNauticalMiles( distance ) );
            info.append( " " );
            info.append( GeoUtils.getCardinalDirection( bearing ) );
            tv.setText( info.toString() );
            tv.setVisibility( View.VISIBLE );
        } else {
            tv.setVisibility( View.GONE );
        }

        if ( mStationWx != null ) {
            Metar metar = mStationWx.get( stationId );
            showMetarInfo( view, c, metar );
        }
    }

    protected void showMetarInfo( View view, Cursor c, Metar metar ) {
        if ( metar != null && metar.isValid ) {
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

            if ( metar.visibilitySM < Float.MAX_VALUE ) {
                info.append( ", " );
                info.append( FormatUtils.formatStatuteMiles( metar.visibilitySM ) );
            }

            if ( metar.windSpeedKnots < Integer.MAX_VALUE ) {
                info.append( ", " );
                if ( metar.windSpeedKnots == 0 ) {
                    info.append( "calm" );
                } else if ( metar.windGustKnots < Integer.MAX_VALUE ) {
                    info.append( String.format( "%dG%dKT",
                            metar.windSpeedKnots, metar.windGustKnots ) );
                } else {
                    info.append( String.format( "%dKT", metar.windSpeedKnots ) );
                }
                if ( metar.windSpeedKnots > 0
                        && metar.windDirDegrees >= 0
                        && metar.windDirDegrees < Integer.MAX_VALUE ) {
                    info.append( "/"+FormatUtils.formatDegrees( metar.windDirDegrees ) );
                }
            }

            if ( metar.wxList.size() > 0 ) {
                for ( WxSymbol wx : metar.wxList ) {
                    if ( !wx.getSymbol().equals( "NSW" ) ) {
                        info.append( ", " );
                        info.append( wx.toString().toLowerCase( Locale.US ) );
                    }
                }
            }

            tv = (TextView) view.findViewById( R.id.wx_station_wx );
            tv.setVisibility( View.VISIBLE );
            tv.setText( info.toString() );

            info.setLength( 0 );
            SkyCondition sky = WxUtils.getCeiling( metar.skyConditions );
            int ceiling = sky.getCloudBaseAGL();
            String skyCover = sky.getSkyCover();
            if ( !skyCover.equals( "NSC" ) ) {
                info.append( "Ceiling "+skyCover+" "+FormatUtils.formatFeet( ceiling ) );
            } else {
                if ( !metar.skyConditions.isEmpty() ) {
                    sky = metar.skyConditions.get( 0 );
                    skyCover = sky.getSkyCover();
                    if ( skyCover.equals( "CLR" ) || skyCover.equals( "SKC" ) ) {
                        info.append( "Sky clear" );
                    } else if ( !skyCover.equals( "SKM" ) ) {
                        info.append(
                                skyCover+" "+FormatUtils.formatFeet( sky.getCloudBaseAGL() ) );
                    }
                }
            }
            if ( info.length() > 0 ) {
                info.append( ", " );
            }

            // Do some basic sanity checks on values
            if ( metar.tempCelsius < Float.MAX_VALUE
                    && metar.dewpointCelsius < Float.MAX_VALUE ) {
                info.append( FormatUtils.formatTemperatureF( metar.tempCelsius ) );
                info.append( "/" );
                info.append( FormatUtils.formatTemperatureF( metar.dewpointCelsius ) );
                info.append( ", " );
            }
            if ( metar.altimeterHg < Float.MAX_VALUE ) {
                info.append( FormatUtils.formatAltimeterHg( metar.altimeterHg ) );
            }

            tv = (TextView) view.findViewById( R.id.wx_station_wx2 );
            tv.setVisibility( View.VISIBLE );
            tv.setText( info.toString() );

            tv = (TextView) view.findViewById( R.id.wx_report_age );
            tv.setVisibility( View.VISIBLE );
            tv.setText( TimeUtils.formatElapsedTime( metar.observationTime ) );
       } else {
            TextView tv = (TextView) view.findViewById( R.id.wx_station_name );
            WxUtils.setColorizedWxDrawable( tv, metar, 0 );
            tv = (TextView) view.findViewById( R.id.wx_station_wx );
            tv.setText( "Unable to get weather details" );
            tv = (TextView) view.findViewById( R.id.wx_station_wx2 );
            tv.setVisibility( View.GONE );
            tv = (TextView) view.findViewById( R.id.wx_report_age );
            tv.setVisibility( View.GONE );
        }
    }

}
