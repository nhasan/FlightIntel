/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2019 Nadeem Hasan <nhasan@nadmm.com>
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

import android.text.format.Time;
import android.util.TimeFormatException;

import com.nadmm.airports.utils.WxUtils;
import com.nadmm.airports.wx.Metar.Flags;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public final class MetarParser {

    private long mFetchTime;
    private HashMap<String, Metar> mMetars = new HashMap<>();

    public ArrayList<Metar> parse( File xmlFile, ArrayList<String> stationIds )
            throws ParserConfigurationException, SAXException, IOException {
        mFetchTime = xmlFile.lastModified();
        InputSource input = new InputSource( new FileReader( xmlFile ) );
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        MetarHandler handler = new MetarHandler();
        XMLReader xmlReader = parser.getXMLReader();
        xmlReader.setContentHandler( handler );
        xmlReader.parse( input );
        ArrayList<Metar> metars = new ArrayList<>( mMetars.values() );
        // Now put the missing ones
        for ( String stationId : stationIds ) {
            if ( !mMetars.containsKey( stationId ) ) {
                Metar metar = new Metar();
                metar.stationId = stationId;
                metar.fetchTime = mFetchTime;
                metars.add( metar );
            }
        }
        return metars;
    }

    private final class MetarHandler extends DefaultHandler {

        private Metar metar;
        private StringBuilder text = new StringBuilder();

        @Override
        public void characters( char[] ch, int start, int length ) {
            text.append( ch, start, length );
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                Attributes attributes ) {
            if ( qName.equalsIgnoreCase( "metar" ) ) {
                metar = new Metar();
                metar.fetchTime = mFetchTime;
            } else if ( qName.equalsIgnoreCase( "sky_condition" ) ) {
                String name = attributes.getValue( "sky_cover" );
                int cloudBaseAGL = 0;
                String attr = attributes.getValue( "cloud_base_ft_agl" );
                if ( attr != null ) {
                    cloudBaseAGL = Integer.valueOf( attr );
                }
                SkyCondition skyCondition = SkyCondition.create( name, cloudBaseAGL );
                metar.skyConditions.add( skyCondition );
            } else {
                text.delete( 0, text.length() );
            }
        }

        @Override
        public void endElement( String uri, String localName, String qName ) {
            if ( qName.equalsIgnoreCase( "metar" ) ) {
                metar.isValid = true;
                parseRemarks( metar );
                setMissingFields( metar );
                mMetars.put( metar.stationId, metar );
            } else if ( qName.equalsIgnoreCase( "raw_text" ) ) {
                metar.rawText = text.toString();
            } else if ( qName.equalsIgnoreCase( "observation_time" ) ) {
                try {
                    Time time = new Time();
                    time.parse3339( text.toString() );
                    metar.observationTime = time.toMillis( true );
                } catch ( TimeFormatException ignored ) {
                }
            } else if ( qName.equalsIgnoreCase( "station_id" ) ) {
                metar.stationId = text.toString();
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
            } else if ( qName.equalsIgnoreCase( "no_signal" ) ) {
                if ( text.toString().equalsIgnoreCase( "true" ) ) {
                    metar.flags.add( Flags.NoSignal ) ;
                }
            } else if ( qName.equalsIgnoreCase( "wx_string" ) ) {
                WxSymbol.parseWxSymbols( metar.wxList, text.toString() );
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
            }
        }

    }

    private void parseRemarks( Metar metar ) {
        int index = metar.rawText.indexOf( "RMK" );
        if ( index == -1 ) {
            return;
        }

        String[] rmks = metar.rawText.substring( index ).split( "\\s+" );
        index = 0;
        while ( index < rmks.length ) {
            String rmk = rmks[ index++ ];
            switch (rmk) {
                case "PRESRR":
                    metar.presrr = true;
                    break;
                case "PRESFR":
                    metar.presfr = true;
                    break;
                case "SNINCR":
                    metar.snincr = true;
                    break;
                case "WSHFT":
                    metar.wshft = true;
                    break;
                case "FROPA":
                    metar.fropa = true;
                    break;
                case "PNO":
                    metar.flags.add(Flags.RainSensorOff);
                    break;
                case "PK":
                    rmk = rmks[index++];
                    if (rmk.equals("WND")) {
                        rmk = rmks[index++];
                        String speed = rmk.substring(3, rmk.indexOf('/'));
                        metar.windPeakKnots = Integer.valueOf(speed);
                    }
                    break;
            }
        }
    }

    private void setMissingFields( Metar metar ) {
        if ( metar.flightCategory == null ) {
            metar.flightCategory =
                    WxUtils.computeFlightCategory( metar.skyConditions, metar.visibilitySM );
        }
        if ( metar.vertVisibilityFeet < Integer.MAX_VALUE ) {
            // Check to see if we have an OVX layer, if not add it
            boolean found = false;
            for ( SkyCondition sky : metar.skyConditions ) {
                if ( sky.getSkyCover().equals( "OVX" ) ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                metar.skyConditions.add( SkyCondition.create( "OVX", 0 ) );
            }
        }
        if ( metar.wxList.isEmpty() ) {
            metar.wxList.add( WxSymbol.get( "NSW", "" ) );
        }
        if ( metar.skyConditions.isEmpty() ) {
            // Sky condition is not available in the METAR
            metar.skyConditions.add( SkyCondition.create( "SKM", 0 ) );
        }
    }

}
