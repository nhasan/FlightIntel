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
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.text.format.Time;
import android.util.TimeFormatException;

import com.nadmm.airports.wx.AirSigmet.AirSigmetEntry;
import com.nadmm.airports.wx.AirSigmet.AirSigmetPoint;

public class AirSigmetParser {

    public void parse( File xml, AirSigmet airSigmet ) {
        try {
            airSigmet.fetchTime = xml.lastModified();
            InputSource input = new InputSource( new FileReader( xml ) );
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            AirSigmetHandler handler = new AirSigmetHandler( airSigmet );
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler( handler );
            xmlReader.parse( input );
        } catch ( Exception e ) {
        }
    }

    protected final class AirSigmetHandler extends DefaultHandler {

        private AirSigmet airSigmet;
        private AirSigmetEntry entry;
        private AirSigmetPoint point;
        private Date now;

        private StringBuilder text = new StringBuilder();

        public AirSigmetHandler( AirSigmet airSigmet ) {
            this.airSigmet = airSigmet;
            now = new Date();
        }

        @Override
        public void characters( char[] ch, int start, int length )
                throws SAXException {
            text.append( ch, start, length );
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                Attributes attributes ) throws SAXException {
            String attr;
            if ( qName.equalsIgnoreCase( "AIRSIGMET" ) ) {
                entry = new AirSigmetEntry();
            } else if ( qName.equalsIgnoreCase( "altitude" ) ) {
               attr = attributes.getValue( "min_ft_msl" );
               if ( attr != null ) {
                   entry.minAltitudeFeet = Integer.valueOf( attr );
               }
               attr = attributes.getValue( "max_ft_msl" );
               if ( attr != null ) {
                   entry.maxAltitudeFeet = Integer.valueOf( attr );
               }
            } else if ( qName.equalsIgnoreCase( "hazard" ) ) {
                entry.hazardType = attributes.getValue( "type" );
                entry.hazardSeverity = attributes.getValue( "severity" );
            } else if ( qName.equalsIgnoreCase( "area" ) ) {
                entry.points = new ArrayList<AirSigmetPoint>();
            } else if ( qName.equalsIgnoreCase( "point" ) ) {
                point = new AirSigmetPoint();
            } else {
                text.setLength( 0 );
            }
        }

        @Override
        public void endElement( String uri, String localName, String qName )
                throws SAXException {
            if ( qName.equalsIgnoreCase( "raw_text" ) ) {
                entry.rawText = text.toString();
            } else if ( qName.equalsIgnoreCase( "valid_time_from" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    entry.fromTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "valid_time_to" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    entry.toTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "airsigmet_type" ) ) {
                entry.type = text.toString();
            } else if ( qName.equalsIgnoreCase( "movement_dir_degrees" ) ) {
                entry.movementDirDegrees = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "movement_speed_kt" ) ) {
                entry.movementSpeedKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "latitude" ) ) {
                point.latitude = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "latitude" ) ) {
                point.longitude = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "point" ) ) {
                entry.points.add( point );
            } else if ( qName.equalsIgnoreCase( "AIRSIGMET" ) ) {
                if ( now.getTime() <= entry.toTime ) {
                    airSigmet.entries.add( entry );
                }
            } else if ( qName.equalsIgnoreCase( "data" ) ) {
                if ( !airSigmet.entries.isEmpty() ) {
                    airSigmet.isValid = true;
                }
            }
        }

    }

}
