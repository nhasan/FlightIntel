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

import com.nadmm.airports.tfr.TfrList.AltitudeType;
import com.nadmm.airports.tfr.TfrList.Tfr;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class TfrParser {

    public void parse( File xml, TfrList tfrList ) {
        try {
            tfrList.fetchTime = xml.lastModified();
            InputSource input = new InputSource( new FileReader( xml ) );
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            TfrHandler handler = new TfrHandler( tfrList );
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler( handler );
            xmlReader.parse( input );
            Collections.sort( tfrList.entries );
        } catch ( Exception ignored ) {
        }
    }

    protected final class TfrHandler extends DefaultHandler {

        private final TfrList tfrList;
        private Tfr tfr;
        private final StringBuilder text;
        private final SimpleDateFormat sdf;

        public TfrHandler( TfrList tfrList ) {
            this.tfrList = tfrList;
            sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US );
            sdf.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
            text = new StringBuilder();
        }

        @Override
        public void characters( char[] ch, int start, int length ) {
            text.append( ch, start, length );
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                Attributes attributes ) {
            if ( qName.toUpperCase( Locale.US ).matches( "TFRLIST" ) ) {
                tfrList.fetchTime = parseDateTime( attributes.getValue( "timestamp" ) );
            } else if ( qName.toUpperCase( Locale.US ).matches( "TFR" ) ) {
                tfr = new Tfr();
            }

            text.setLength( 0 );
        }

        @Override
        public void endElement( String uri, String localName, String qName ) {
            if ( qName.equalsIgnoreCase( "NID" ) ) {
                tfr.notamId = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "NAME" ) ) {
                tfr.name = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "CITY" ) ) {
                tfr.city = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "STATE" ) ) {
                tfr.state = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "FACILITY" ) ) {
                tfr.facility = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "FACILITYTYPE" ) ) {
                tfr.facilityType = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "SRC" ) ) {
                tfr.text = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "TYPE" ) ) {
                tfr.type = text.toString().trim();
            } else if ( qName.equalsIgnoreCase( "MINALT" ) ) {
                parseMinAlt( text.toString() );
            } else if ( qName.equalsIgnoreCase( "MAXALT" ) ) {
                parseMaxAlt( text.toString() );
            } else if ( qName.equals( "CREATED" ) ) {
                tfr.createTime = parseDateTime( text.toString() );
            } else if ( qName.equals( "MODIFIED" ) ) {
                tfr.modifyTime = parseDateTime( text.toString() );
            } else if ( qName.equals( "ACTIVE" ) ) {
                tfr.activeTime = parseDateTime( text.toString() );
            } else if ( qName.equals( "EXPIRES" ) ) {
                tfr.expireTime = parseDateTime(text.toString());
            } else if ( qName.equals( "TYPE" ) ) {
                tfr.type = text.toString().trim();
            } else if ( qName.toUpperCase( Locale.US ).matches( "TFR" ) ) {
                if ( !tfr.name.equalsIgnoreCase( "Latest Update" ) ) {
                    if ( tfr.modifyTime == 0 )
                    {
                        tfr.modifyTime = tfr.createTime;
                    }
                    tfrList.entries.add( tfr );
                }
            }
        }

        private long parseDateTime( String text )
        {
            long dt = Long.MAX_VALUE;
            try {
                dt = sdf.parse( text.trim() ).getTime();
            } catch ( ParseException ignored ) {
                String msg = ignored.getMessage();
            }
            return dt;
        }

        private void parseMinAlt( String alt ) {
            tfr.minAltitudeFeet = Integer.parseInt( alt.substring( 0, alt.length()-1 ) );
            tfr.minAltitudeType = alt.startsWith( "A", alt.length()-1 ) ?
                    AltitudeType.AGL : AltitudeType.MSL;
        }

        private void parseMaxAlt( String alt ) {
            tfr.maxAltitudeFeet = Integer.parseInt( alt.substring( 0, alt.length()-1 ) );
            tfr.maxAltitudeType = alt.startsWith( "A", alt.length()-1 ) ?
                    AltitudeType.AGL : AltitudeType.MSL;
        }

    }

}
