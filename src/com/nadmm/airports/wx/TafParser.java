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

import android.text.format.Time;
import android.util.TimeFormatException;

public class TafParser {

    public void parse( File xml, Taf taf ) {
        try {
            taf.fetchTime = xml.lastModified();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            TafHandler handler = new TafHandler( taf );
            xmlReader.setContentHandler( handler );
            InputSource input = new InputSource( new FileReader( xml ) );
            xmlReader.parse( input );
        } catch ( Exception e ) {
        }
    }

    protected final class TafHandler extends DefaultHandler {

        protected Taf taf;
        protected Taf.Forecast forecast;
        protected Taf.Temperature temperature;
        private StringBuilder text = new StringBuilder();

        public TafHandler( Taf taf ) {
            this.taf = taf;
        }

        @Override
        public void characters( char[] ch, int start, int length )
                throws SAXException {
            text.append( ch, start, length );
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                Attributes attributes ) throws SAXException {
            if ( qName.equalsIgnoreCase( "taf" ) ) {
            } else if ( qName.equals( "forecast" ) ) {
                forecast = new Taf.Forecast();
            } else if ( qName.equals( "temperature" ) ) {
                temperature = new Taf.Temperature();
            } else if ( qName.equalsIgnoreCase( "sky_condition" ) ) {
                String name = attributes.getValue( "sky_cover" );
                int cloudBase = 0;
                if ( attributes.getIndex( "cloud_base_ft_agl" ) >= 0 ) {
                    cloudBase = Integer.valueOf( attributes.getValue( "cloud_base_ft_agl" ) );
                }
                SkyCondition skyCondition = SkyCondition.create( name, cloudBase );
                forecast.skyConditions.add( skyCondition );
            } else if ( qName.equalsIgnoreCase( "turbulence_condition" ) ) {
                Taf.TurbulenceCondition turbulence = new Taf.TurbulenceCondition();
                String value = attributes.getValue( "turbulence_intensity" );
                if ( value != null ) {
                    turbulence.intensity = Integer.valueOf( value );
                }
                value = attributes.getValue( "turbulence_min_alt_ft_agl" );
                if ( value != null ) {
                    turbulence.minAltitudeFeetAGL = Integer.valueOf( value );
                }
                value = attributes.getValue( "turbulence_max_alt_ft_agl" );
                if ( value != null ) {
                    turbulence.maxAltitudeFeetAGL = Integer.valueOf( value );
                }
                forecast.turbulenceConditions.add( turbulence );
            } else if ( qName.equalsIgnoreCase( "icing_condition" ) ) {
                Taf.IcingCondition icing = new Taf.IcingCondition();
                String value = attributes.getValue( "icing_intensity" );
                if ( value != null ) {
                    icing.intensity = Integer.valueOf( value );
                }
                value = attributes.getValue( "icing_min_alt_ft_agl" );
                if ( value != null ) {
                    icing.minAltitudeFeetAGL = Integer.valueOf( value );
                }
                value = attributes.getValue( "icing_max_alt_ft_agl" );
                if ( value != null ) {
                    icing.maxAltitudeFeetAGL = Integer.valueOf( value );
                }
                forecast.icingConditions.add( icing );
            } else {
                text.setLength( 0 );
            }
        }

        @Override
        public void endElement( String uri, String localName, String qName )
                throws SAXException {
            if ( qName.equalsIgnoreCase( "raw_text" ) ) {
                taf.rawText = text.toString();
            } else if ( qName.equalsIgnoreCase( "issue_time" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    taf.issueTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "bulletin_time" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    taf.bulletinTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "valid_time_from" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    taf.validTimeFrom = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "valid_time_to" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    taf.validTimeTo = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "elevation_m" ) ) {
                taf.stationElevationMeters = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "remarks" ) ) {
                taf.remarks = text.toString();
            } else if ( qName.equalsIgnoreCase( "forecast" ) ) {
                if ( forecast.wxList.isEmpty() ) {
                    forecast.wxList.add( WxSymbol.get( "NSW", "" ) );
                }
                taf.forecasts.add( forecast );
            } else if ( qName.equalsIgnoreCase( "temperature" ) ) {
                forecast.temperatures.add( temperature );
            } else if ( qName.equalsIgnoreCase( "wx_string" ) ) {
                WxSymbol.parseWxSymbols( forecast.wxList, text.toString() );
            } else if ( qName.equalsIgnoreCase( "fcst_time_from" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    forecast.timeFrom = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "fcst_time_to" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    forecast.timeTo = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "time_becoming" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    forecast.timeBecoming = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "change_indicator" ) ) {
                forecast.changeIndicator = text.toString();
            } else if ( qName.equalsIgnoreCase( "probability" ) ) {
                forecast.probability = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_dir_degrees" ) ) {
                forecast.windDirDegrees = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_speed_kt" ) ) {
                forecast.windSpeedKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_gust_kt" ) ) {
                forecast.windGustKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_shear_dir_degrees" ) ) {
                forecast.windShearDirDegrees = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_shear_speed_kt" ) ) {
                forecast.windShearSpeedKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_shear_hgt_ft_agl" ) ) {
                forecast.windShearHeightFeetAGL = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "visibility_statute_mi" ) ) {
                forecast.visibilitySM = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "altim_in_hg" ) ) {
                forecast.altimeterHg = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "vert_vis_ft" ) ) {
                forecast.vertVisibilityFeet = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "valid_time" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    temperature.validTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "sfc_temp_c" ) ) {
                temperature.surfaceTempCentigrade = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "taf" ) ) {
                taf.isValid = true;
            }
        }

    }

}
