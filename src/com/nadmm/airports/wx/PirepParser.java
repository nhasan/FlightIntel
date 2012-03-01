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

import java.io.File;
import java.io.FileReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.location.Location;
import android.text.format.Time;
import android.util.TimeFormatException;

import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.wx.Pirep.IcingCondition;
import com.nadmm.airports.wx.Pirep.PirepEntry;
import com.nadmm.airports.wx.Pirep.SkyCondition;
import com.nadmm.airports.wx.Pirep.TurbulenceCondition;

public class PirepParser {

    Location mLocation;
    int mRadiusNM;
    float mDeclination;

    public void parse( File xml, Pirep pirep, Location location, int radiusNM ) {
        try {
            mLocation = location;
            mRadiusNM = radiusNM;
            mDeclination = GeoUtils.getMagneticDeclination( location );
            pirep.fetchTime = xml.lastModified();
            InputSource input = new InputSource( new FileReader( xml ) );
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            PirepHandler handler = new PirepHandler( pirep );
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler( handler );
            xmlReader.parse( input );
        } catch ( Exception e ) {
        }
    }

    protected final class PirepHandler extends DefaultHandler {

        private Pirep pirep;
        private PirepEntry entry;
        private StringBuilder text = new StringBuilder();

        public PirepHandler( Pirep pirep ) {
            this.pirep = pirep;
        }

        @Override
        public void characters( char[] ch, int start, int length )
                throws SAXException {
            text.append( ch, start, length );
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                Attributes attributes ) throws SAXException {
            if ( qName.equalsIgnoreCase( "AircraftReport" ) ) {
                entry = new PirepEntry();
            } else if ( qName.equalsIgnoreCase( "sky_condition" ) ) {
                String skyCover = attributes.getValue( "sky_cover" );
                int cloudBaseMSL = Integer.MAX_VALUE;
                String attr = attributes.getValue( "cloud_base_ft_msl" );
                if ( attr != null ) {
                    cloudBaseMSL = Integer.valueOf( attr );
                }
                int cloudTopMSL = Integer.MAX_VALUE;
                attr = attributes.getValue( "cloud_top_ft_msl" );
                if ( attr != null ) {
                    cloudTopMSL = Integer.valueOf( attr );
                }
                SkyCondition sky = new SkyCondition( skyCover, cloudBaseMSL, cloudTopMSL );
                entry.skyConditions.add( sky );
            } else if ( qName.equalsIgnoreCase( "turbulence_condition" ) ) {
                String type = attributes.getValue( "turbulence_type" );
                String intensity = attributes.getValue( "turbulence_intensity" );
                String frequency = attributes.getValue( "turbulence_freq" );
                int turbulenceBaseMSL = Integer.MAX_VALUE;
                String attr = attributes.getValue( "turbulence_base_ft_msl" );
                if ( attr != null ) {
                    turbulenceBaseMSL = Integer.valueOf( attr );
                }
                int turbulenceTopMSL = Integer.MAX_VALUE;
                attr = attributes.getValue( "turbulence_top_ft_msl" );
                if ( attr != null ) {
                    turbulenceTopMSL = Integer.valueOf( attr );
                }
                entry.turbulenceConditions.add( new TurbulenceCondition( type, intensity,
                        frequency, turbulenceBaseMSL, turbulenceTopMSL ) );
            } else if ( qName.equalsIgnoreCase( "icing_condition" ) ) {
                String type = attributes.getValue( "icing_type" );
                String intensity = attributes.getValue( "icing_intensity" );
                int icingBaseMSL = Integer.MAX_VALUE;
                String attr = attributes.getValue( "icing_base_ft_msl" );
                if ( attr != null ) {
                    icingBaseMSL = Integer.valueOf( attr );
                }
                int icingTopMSL = Integer.MAX_VALUE;
                attr = attributes.getValue( "icing_top_ft_msl" );
                if ( attr != null ) {
                    icingTopMSL = Integer.valueOf( attr );
                }
                entry.icingConditions.add( new IcingCondition( type, intensity,
                        icingBaseMSL, icingTopMSL ) );
            } else {
                text.setLength( 0 );
            }
        }

        @Override
        public void endElement( String uri, String localName, String qName )
                throws SAXException {
            if ( qName.equalsIgnoreCase( "raw_text" ) ) {
                entry.rawText = text.toString();
            } else if ( qName.equalsIgnoreCase( "report_type" ) ) {
                entry.reportType = text.toString();
            } else if ( qName.equalsIgnoreCase( "receipt_time" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    entry.receiptTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "observation_time" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    entry.observationTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "wx_string" ) ) {
                WxSymbol.parseWxSymbols( entry.wxList, text.toString() );
            } else if ( qName.equalsIgnoreCase( "aircraft_ref" ) ) {
                entry.aircraftRef = text.toString();
            } else if ( qName.equalsIgnoreCase( "latitude" ) ) {
                entry.latitude = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "longitude" ) ) {
                entry.longitude = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "altitude_ft_msl" ) ) {
                entry.altitudeFeetMSL = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "visibiliy_statue_mi" ) ) {
                entry.visibilitySM = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "temp_c" ) ) {
                entry.tempCelsius = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_dir_degrees" ) ) {
                entry.windDirDegrees = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_speed_kt" ) ) {
                entry.windSpeedKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "vert_gust_kt" ) ) {
                entry.vertGustKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "AircraftReport" ) ) {
                Location reportLocation = new Location( "" );
                reportLocation.setLatitude( entry.latitude );
                reportLocation.setLongitude( entry.longitude );

                float[] results = new float[ 2 ];
                Location.distanceBetween( mLocation.getLatitude(), mLocation.getLongitude(),
                        reportLocation.getLatitude(), reportLocation.getLongitude(), results );

                entry.distanceNM = (long) (results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE);
                if ( entry.distanceNM <= mRadiusNM ) {
                    entry.bearing = ( results[ 1 ]+mDeclination+360 )%360;
                    entry.isValid = true;
                    pirep.entries.add( entry );
                }
            }
        }

    }

}
