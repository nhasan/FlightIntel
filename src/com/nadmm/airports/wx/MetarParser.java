/*
 * FlightIntel for Pilots
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

package com.nadmm.airports.wx;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.text.format.Time;
import android.util.TimeFormatException;

import com.nadmm.airports.wx.Metar.Flags;

public final class MetarParser {

    public void parse( File xml, Metar metar ) {
        try {
            metar.fetchTime = xml.lastModified();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            MetarHandler handler = new MetarHandler( metar );
            xmlReader.setContentHandler( handler );
            InputSource input = new InputSource( new FileReader( xml ) );

            xmlReader.parse( input );
        } catch ( Exception e ) {
        }
    }

    protected void parseWxGroups( Metar metar, String wxString ) {
        String[] groups = wxString.split( "\\s+" );
        ArrayList<String> names = WxSymbol.getNames();
        for ( String group : groups ) {
            int offset = 0;
            String intensity = "";
            if ( group.charAt( offset ) == '+' || group.charAt( offset ) == '-' ) {
                intensity = group.substring( offset, offset+1 );
                ++offset;
            }
            while ( offset < group.length() ) {
                WxSymbol wx = null;
                for  ( String name : names  ) {
                    if ( group.substring( offset ).startsWith( name ) ) {
                        wx = WxSymbol.get( name );
                        if ( intensity.length() > 0 ) {
                            wx.setIntensity( intensity );
                            intensity = "";
                        }
                        metar.wxList.add( wx );
                        offset += wx.getName().length();
                        break;
                    }
                }

                if ( wx == null ) {
                    // No match found, skip to next character and try again
                    ++offset;
                }
            }
        }
    }

    protected void parseRemarks( Metar metar ) {
        int index = metar.rawText.indexOf( "RMK" );
        if ( index == -1 ) {
            return;
        }

        String[] rmks = metar.rawText.substring( index ).split( "\\s+" );
        index = 0;
        while ( index < rmks.length ) {
            String rmk = rmks[ index++ ];
            if ( rmk.equals( "PRESRR" ) ) {
                metar.presrr = true;
            } else if ( rmk.equals( "PRESFR" ) ) {
                metar.presfr = true;
            } else if ( rmk.equals( "SNINCR" ) ) {
                metar.snincr = true;
            } else if ( rmk.equals( "WSHFT" ) ) {
                metar.wshft = true;
            } else if ( rmk.equals( "FROPA" ) ) {
                metar.fropa = true;
            } else if ( rmk.equals( "PNO" ) ) {
                metar.flags.add( Flags.RainSensorOff );
            } else if ( rmk.equals( "PK" ) ) {
                rmk = rmks[ index++ ];
                if ( rmk.equals( "WND" ) ) {
                    rmk = rmks[ index++ ];
                    String speed = rmk.substring( 3, rmk.indexOf( '/' ) );
                    metar.windPeakKnots = Integer.valueOf( speed );
                }
            }
        }
    }

    protected final class MetarHandler extends DefaultHandler {

        protected Metar metar;
        private StringBuilder text = new StringBuilder();

        public MetarHandler( Metar metar ) {
            this.metar = metar;
        }

        @Override
        public void characters( char[] ch, int start, int length )
                throws SAXException {
            text.append( ch, start, length );
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                Attributes attributes ) throws SAXException {
            if ( qName.equalsIgnoreCase( "metar" ) ) {
            } else if ( qName.equalsIgnoreCase( "sky_condition" ) ) {
                String name = attributes.getValue( "sky_cover" );
                int cloudBase = 0;
                if ( attributes.getIndex( "cloud_base_ft_agl" ) >= 0 ) {
                    cloudBase = Integer.valueOf( attributes.getValue( "cloud_base_ft_agl" ) );
                }
                SkyCondition skyCondition = SkyCondition.create( name, cloudBase );
                metar.skyConditions.add( skyCondition );
            } else {
                text.delete( 0, text.length() );
            }
        }

        @Override
        public void endElement( String uri, String localName, String qName )
                throws SAXException {
            if ( qName.equalsIgnoreCase( "raw_text" ) ) {
                metar.rawText = text.toString();
                parseRemarks( metar );
            } else if ( qName.equalsIgnoreCase( "station_id" ) ) {
                metar.stationId = text.toString();
            } else if ( qName.equalsIgnoreCase( "observation_time" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    metar.observationTime = time.toMillis( true );
                } catch ( TimeFormatException e ) {
                }
            } else if ( qName.equalsIgnoreCase( "elevation_m" ) ) {
                metar.stationElevationMeters = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "temp_c" ) ) {
                metar.tempCelsius = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "dewpoint_c" ) ) {
                metar.dewpointCelsius = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_dir_degrees" ) ) {
                metar.windDirDegrees = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_speed_kt" ) ) {
                metar.windSpeedKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "wind_gust_kt" ) ) {
                metar.windGustKnots = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "visibility_statute_mi" ) ) {
                metar.visibilitySM = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "altim_in_hg" ) ) {
                metar.altimeterHg = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "sea_level_pressure_mb" ) ) {
                metar.seaLevelPressureMb = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "corrected" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.Corrected ) ;
                }
            } else if ( qName.equalsIgnoreCase( "auto_station" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.AutoStation ) ;
                }
            } else if ( qName.equalsIgnoreCase( "auto" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.AutoReport ) ;
                }
            } else if ( qName.equalsIgnoreCase( "maintenance_indicator_on" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.MaintenanceIndicatorOn ) ;
                }
            } else if ( qName.equalsIgnoreCase( "present_weather_sensor_off" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.PresentWeatherSensorOff ) ;
                }
            } else if ( qName.equalsIgnoreCase( "lightning_sensor_off" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.LightningSensorOff ) ;
                }
            } else if ( qName.equalsIgnoreCase( "freezing_rain_sensor_off" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.FreezingRainSensorOff ) ;
                }
            } else if ( qName.equalsIgnoreCase( "wx_string" ) ) {
                parseWxGroups( metar, text.toString() );
            } else if ( qName.equalsIgnoreCase( "flight_category" ) ) {
                metar.flightCategory = text.toString();
            } else if ( qName.equalsIgnoreCase( "three_hr_pressure_tendency_mb" ) ) {
                metar.pressureTend3HrMb = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "maxt_c" ) ) {
                metar.maxTemp6HrCentigrade = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "mint_c" ) ) {
                metar.minTemp6HrCentigrade = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "maxt24hr_c" ) ) {
                metar.maxTemp24HrCentigrade = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "mint24hr_c" ) ) {
                metar.minTemp24HrCentigrade = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "precip_in" ) ) {
                metar.precipInches = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "pcp3hr_in" ) ) {
                metar.precip3HrInches = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "pcp6hr_in" ) ) {
                metar.precip6HrInches = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "pcp24hr_in" ) ) {
                metar.precip24HrInches = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "snow_in" ) ) {
                metar.snowInches = Float.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "vert_vis_ft" ) ) {
                metar.vertVisibilityFeet = Integer.valueOf( text.toString() );
            } else if ( qName.equalsIgnoreCase( "metar_type" ) ) {
                metar.metarType = text.toString();
            } else if ( qName.equalsIgnoreCase( "metar" ) ) {
                metar.isValid = true;
                metar.setMissingFields();
            }
        }
    }

}
