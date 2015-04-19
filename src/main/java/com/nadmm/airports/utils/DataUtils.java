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

package com.nadmm.airports.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class DataUtils {

    private final static String DOT = "\u2022";
    private final static String DASH = "\u2013";
    private static HashMap<String, String> sMorseCodes = new HashMap<>();
    static {
        sMorseCodes.put( "A", DOT+DASH );
        sMorseCodes.put( "B", DASH+DOT+DOT+DOT );
        sMorseCodes.put( "C", DASH+DOT+DASH+DOT );
        sMorseCodes.put( "D", DASH+DOT+DOT );
        sMorseCodes.put( "E", DOT );
        sMorseCodes.put( "F", DOT+DOT+DASH+DOT );
        sMorseCodes.put( "G", DASH+DASH+DOT );
        sMorseCodes.put( "H", DOT+DOT+DOT+DOT );
        sMorseCodes.put( "I", DOT+DOT );
        sMorseCodes.put( "J", DOT+DASH+DASH+DASH );
        sMorseCodes.put( "K", DASH+DOT+DASH );
        sMorseCodes.put( "L", DOT+DASH+DOT+DOT );
        sMorseCodes.put( "M", DASH+DASH );
        sMorseCodes.put( "N", DASH+DOT );
        sMorseCodes.put( "O", DASH+DASH+DASH );
        sMorseCodes.put( "P", DOT+DASH+DASH+DOT );
        sMorseCodes.put( "Q", DASH+DASH+DOT+DASH );
        sMorseCodes.put( "R", DOT+DASH+DOT );
        sMorseCodes.put( "S", DOT+DOT+DOT );
        sMorseCodes.put( "T", DASH );
        sMorseCodes.put( "U", DOT+DOT+DASH );
        sMorseCodes.put( "V", DOT+DOT+DOT+DASH );
        sMorseCodes.put( "W", DOT+DASH+DASH );
        sMorseCodes.put( "X", DASH+DOT+DOT+DASH );
        sMorseCodes.put( "Y", DASH+DOT+DASH+DASH );
        sMorseCodes.put( "Z", DASH+DASH+DOT+DOT );
        sMorseCodes.put( "1", DOT+DASH+DASH+DASH+DASH );
        sMorseCodes.put( "2", DOT+DOT+DASH+DASH+DASH );
        sMorseCodes.put( "3", DOT+DOT+DOT+DASH+DASH );
        sMorseCodes.put( "4", DOT+DOT+DOT+DOT+DASH );
        sMorseCodes.put( "5", DOT+DOT+DOT+DOT+DOT );
        sMorseCodes.put( "6", DASH+DOT+DOT+DOT+DOT );
        sMorseCodes.put( "7", DASH+DASH+DOT+DOT+DOT );
        sMorseCodes.put( "8", DASH+DASH+DASH+DOT+DOT );
        sMorseCodes.put( "9", DASH+DASH+DASH+DASH+DOT );
        sMorseCodes.put( "0", DASH+DASH+DASH+DASH+DASH );
    }

    public static String decodePhoneNumber( String phone ) {
        int i = 0;
        StringBuilder builder = new StringBuilder();
        while ( i < phone.length() ) {
            char c = phone.charAt( i++ );
            if ( "ABC".indexOf( c ) >= 0 ) {
                builder.append( '2' );
            } else if ( "DEF".indexOf( c ) >= 0 ) {
                builder.append( '3' );
            } else if ( "GHI".indexOf( c ) >= 0 ) {
                builder.append( '4' );
            } else if ( "JKL".indexOf( c ) >= 0 ) {
                builder.append( '5' );
            } else if ( "MNO".indexOf( c ) >= 0 ) {
                builder.append( '6' );
            } else if ( "PQRS".indexOf( c ) >= 0 ) {
                builder.append( '7' );
            } else if ( "TUV".indexOf( c ) >= 0 ) {
                builder.append( '8' );
            } else if ( "WXYZ".indexOf( c ) >= 0 ) {
                builder.append( '9' );
            } else {
                builder.append( c );
            }
        }
        return builder.toString();
    }

    public static String getMorseCode( String text ) {
        String morseCode = "";
        int i = 0;
        while ( i < text.length() ) {
            if ( morseCode.length() > 0 ) {
                morseCode += "  ";
            }
            morseCode += sMorseCodes.get( text.substring( i, i+1 ) );
            ++i;
        }
        return morseCode;
    }

    public static int calculateMagneticHeading( int trueHeading, int variation ) {
        int magneticHeading = ( trueHeading+variation+360 )%360;
        if ( magneticHeading == 0 ) {
            magneticHeading = 360;
        }
        return magneticHeading;
    }

    public static int calculateReciprocalHeading( int heading ) {
        return ( heading+180 )%360;
    }

    public static String decodeLandingFaclityType( String siteNumber ) {
        char type = siteNumber.charAt( siteNumber.length()-1 );
        if ( type == 'A') {
            return "Airport";
        } else if ( type == 'B' ) {
            return "Balloonport";
        } else if ( type == 'C' ) {
            return "Seaplane base";
        } else if ( type == 'G' ) {
            return "Gliderport";
        } else if ( type == 'H' ) {
            return "Heliport";
        } else if ( type == 'S' ) {
            return "STOLport";
        } else if ( type == 'U' ) {
            return "Ultralight park";
        } else {
            return "";
        }
    }

    public static String decodeOwnershipType( String ownership ) {
        if ( ownership.equals( "PU" ) ) {
            return "Public owned";
        } else if ( ownership.equals( "PR" ) ) {
            return "Private owned";
        } else if ( ownership.equals( "MA" ) ) {
            return "Airforce owned";
        } else if ( ownership.equals( "MN" ) ) {
            return "Navy owned";
        } else if ( ownership.equals( "MR" ) ) {
            return "Army owned";
        } else {
            return "Unknown ownership";
        }
    }

    public static String decodeFacilityUse( String use ) {
        if ( use.equals( "PU" ) ) {
            return "Public use";
        } else if ( use.equals( "PR" ) ) {
            return "Private Use";
        } else {
            return "Unknown";
        }
    }

    public static String decodeFuelTypes( String fuelTypes ) {
        String decodedFuel = "";

        int start = 0;
        while ( start < fuelTypes.length() ) {
            int end = Math.min( start+5, fuelTypes.length() );
            String type = fuelTypes.substring( start, end ).trim();

            if ( decodedFuel.length() > 0 ) {
                decodedFuel += ", ";
            }

            if ( type.equals( "80" ) ) {
                decodedFuel += "80";
            } else if ( type.equals( "100" ) ) {
                decodedFuel += "100";
            } else if ( type.equals( "100LL" ) ) {
                decodedFuel += "100LL";
            } else if ( type.equals( "A" ) ) {
                decodedFuel += "JET-A";
            } else if ( type.equals( "A+" ) ) {
                decodedFuel += "JET-A+";
            } else if ( type.equals( "A1" ) ) {
                decodedFuel += "JET-A1";
            } else if ( type.equals( "A1+" ) ) {
                decodedFuel += "JET-A1+";
            } else if ( type.equals( "B" ) ) {
                decodedFuel += "JET-B";
            } else if ( type.equals( "B+" ) ) {
                decodedFuel += "JET-B+";
            } else if ( type.equals( "MOGAS" ) ) {
                decodedFuel += "MOGAS";
            } else {
                decodedFuel += type;
            }

            start = end;
        }

        return decodedFuel;
    }

    public static String decodeStatus( String status ) {
        if ( status.equals( "O" ) ) {
            return "Operational";
        } else if ( status.equals( "CI" ) ) {
            return "Closed Indefinitely";
        } else if ( status.equals( "CP" ) ) {
            return "Closed Permanently";
        }
        return "Unknown";
    }

    public static ArrayList<String> decodeStorage( String s ) {
        ArrayList<String> storages = new ArrayList<>();

        int start = 0;
        while ( start < s.length() ) {
            int end = s.indexOf( ",", start );
            if ( end == -1 ) {
                end = s.length();
            }
            String type = s.substring( start, end ).trim();

            if ( type.equals( "BUOY" ) ) {
                storages.add( "Buoy" );
            } else if ( type.equals( "HGR" ) ) {
                storages.add( "Hangar" );
            } else if ( type.equals( "TIE" ) ) {
                storages.add( "Tiedown" );
            } else {
                storages.add( type );
            }

            start = end+1;
        }

        return storages;
    }

    public static ArrayList<String> decodeServices( String s ) {
        ArrayList<String> services = new ArrayList<>();

        int start = 0;
        while ( start < s.length() ) {
            int end = s.indexOf( ",", start );
            if ( end == -1 ) {
                end = s.length();
            }
            String type = s.substring( start, end ).trim();

            if ( type.equals( "AFRT" ) ) {
                services.add( "Air freight" );
            } else if ( type.equals( "AGRI" ) ) {
                services.add( "Crop dusting" );
            } else if ( type.equals( "AMB" ) ) {
                services.add( "Air ambulance" );
            } else if ( type.equals( "AVNCS" ) ) {
                services.add( "Avionics" );
            } else if ( type.equals( "BCHGR" ) ) {
                services.add( "Beaching gear" );
            } else if ( type.equals( "CARGO" ) ) {
                services.add( "Cargo" );
            } else if ( type.equals( "CHTR" ) ) {
                services.add( "Charter" );
            } else if ( type.equals( "GLD" ) ) {
                services.add( "Glider" );
            } else if ( type.equals( "INSTR" ) ) {
                services.add( "Flight instruction" );
            } else if ( type.equals( "PAJA" ) ) {
                services.add( "Parachute jump activity" );
            } else if ( type.equals( "RNTL" ) ) {
                services.add( "Rental" );
            } else if ( type.equals( "SALES" ) ) {
                services.add( "Sales" );
            } else if ( type.equals( "SURV" ) ) {
                services.add( "Survey" );
            } else if ( type.equals( "TOW" ) ) {
                services.add( "Glider towing" );
            } else {
                services.add( type );
            }

            start = end+1;
        }

        return services;
    }

    public static String decodeSurfaceType( String surfaceType ) {
        String decodedSurfaceType = "";

        int start = 0;
        while ( start < surfaceType.length() ) {
            int end = surfaceType.indexOf( "-", start );
            if ( end == -1 ) {
                end = surfaceType.length();
            }
            String type = surfaceType.substring( start, end ).trim();

            if ( decodedSurfaceType.length() > 0 ) {
                decodedSurfaceType += ", ";
            }

            if ( type.equals( "CONC" ) ) {
                decodedSurfaceType += "Concrete";
            } else if ( type.equals( "ASPH" ) ) {
                decodedSurfaceType += "Asphalt";
            } else if ( type.equals( "SNOW" ) ) {
                decodedSurfaceType += "Snow";
            } else if ( type.equals( "ICE" ) ) {
                decodedSurfaceType += "Ice";
            } else if ( type.equals( "MATS" ) ) {
                decodedSurfaceType += "Landing mats";
            } else if ( type.equals( "TREATED" ) ) {
                decodedSurfaceType += "Treated";
            } else if ( type.equals( "GRAVEL" ) ) {
                decodedSurfaceType += "Gravel";
            } else if ( type.equals( "TURF" ) ) {
                decodedSurfaceType += "Grass";
            } else if ( type.equals( "DIRT" ) ) {
                decodedSurfaceType += "Soil";
            } else if ( type.equals( "WATER" ) ) {
                decodedSurfaceType += "Water";
            } else if ( type.equals( "E" ) ) {
                decodedSurfaceType += "Excellent";
            } else if ( type.equals( "G" ) ) {
                decodedSurfaceType += "Good";
            } else if ( type.equals( "F" ) ) {
                decodedSurfaceType += "Fair";
            } else if ( type.equals( "P" ) ) {
                decodedSurfaceType += "Poor";
            } else if ( type.equals( "L" ) ) {
                decodedSurfaceType += "Failed";
            }

            start = end+1;
        }

        return decodedSurfaceType;
    }

    public static String decodeSurfaceTreatment( String treatment ) {
        if ( treatment.equals( "GRVD" ) ) {
            return "Grooved";
        } else if ( treatment.equals( "PFC" ) ) {
            return "Porous friction course";
        } else if ( treatment.equals( "AFSC" ) ) {
            return "Aggregate friction seal coat";
        } else if ( treatment.equals( "RFSC" ) ) {
            return "Rubberized friction seal coat";
        } else if ( treatment.equals( "WC" ) ) {
            return "Wire comb";
        } else {
            return "None";
        }
    }

    public static String decodeWindIndicator( String windIndicator ) {
        if ( windIndicator.equals( "Y-L" ) ) {
            return "Lighted";
        } else if ( windIndicator.equals( "Y" ) ) {
            return "Unlighted";
        } else {
            return "No";
        }
    }

    public static String decodeBeacon( String beacon ) {
        if ( beacon.equals( "CG" ) ) {
            return "White-green";
        } else if ( beacon.equals( "CY" ) ) {
            return "White-yellow";
        } else if ( beacon.equals( "CGY" ) ) {
            return "White-Green-Yellow";
        } else if ( beacon.equals( "SCG" ) ) {
            return "Split-white-green";
        } else if ( beacon.equals( "C" ) ) {
            return "White";
        } else if ( beacon.equals( "Y" ) ) {
            return "Yellow";
        } else if ( beacon.equals( "G" ) ) {
            return "Green";
        } else {
            return "No";
        }
    }

    public static String decodeRunwayEdgeLights( String edgeLights ) {
        if ( edgeLights.equals( "HIGH" ) ) {
            return "High intensity";
        } else if ( edgeLights.equals( "MED" ) ) {
            return "Medium intensity";
        } else if ( edgeLights.equals( "LOW" ) ) {
            return "Low intensity";
        } else if ( edgeLights.equals( "NSTD" ) ) {
            return "Non-standard";
        } else if ( edgeLights.equals( "PERI" ) ) {
            return "Perimeter";
        } else if ( edgeLights.equals( "STRB" ) ) {
            return "Strobe";
        } else if ( edgeLights.equals( "FLD" ) ) {
            return "Flood";
        } else {
            return "None";
        }
    }

    public static String decodeRunwayMarking( String marking ) {
        if ( marking.equals( "PIR" ) ) {
            return "Precision";
        } else if ( marking.equals( "NPI" ) ) {
            return "Non-precision";
        } else if ( marking.equals( "BSC" ) ) {
            return "Basic";
        } else if ( marking.equals( "NRS" ) ) {
            return "Numbers only";
        } else if ( marking.equals( "NSTD" ) ) {
            return "Non-standard";
        } else if ( marking.equals( "BUOY" ) ) {
            return "Buoys";
        } else if ( marking.equals( "STOL" ) ) {
            return "STOL";
        } else {
            return "None";
        }
    }

    public static String decodeRunwayMarkingCondition( String condition ) {
        if ( condition.equals( "G" ) ) {
            return "Good";
        } else if ( condition.equals( "F" ) ) {
            return "Fair";
        } else if ( condition.equals( "P" ) ) {
            return "Poor";
        } else {
            return "";
        }
    }

    public static String decodeGlideSlope( String glideSlope ) {
        if ( glideSlope.equals( "S2L" ) ) {
            return "2-box SAVASI on left side";
        } else if ( glideSlope.equals( "S2R" ) ) {
            return "2-box SAVASI on right side";
        } else if ( glideSlope.equals( "V2L" ) ) {
            return "2-box VASI on left side";
        } else if ( glideSlope.equals( "V2R" ) ) {
            return "2-box VASI on right side";
        } else if ( glideSlope.equals( "V4L" ) ) {
            return "4-box VASI on left side";
        } else if ( glideSlope.equals( "V4R" ) ) {
            return "4-box VASI on right side";
        } else if ( glideSlope.equals( "V6L" ) ) {
            return "6-box VASI on left side";
        } else if ( glideSlope.equals( "V6R" ) ) {
            return "6-box VASI on right side";
        } else if ( glideSlope.equals( "V12" ) ) {
            return "12-box VASI on both sides";
        } else if ( glideSlope.equals( "V16" ) ) {
            return "16-box VASI on both sides";
        } else if ( glideSlope.equals( "P2L" ) ) {
            return "2-light PAPI on left side";
        } else if ( glideSlope.equals( "P2R" ) ) {
            return "2-light PAPI on right side";
        } else if ( glideSlope.equals( "P4L" ) ) {
            return "4-light PAPI on left side";
        } else if ( glideSlope.equals( "P4R" ) ) {
            return "4-light PAPI on right side";
        } else if ( glideSlope.equals( "NSTD" ) ) {
            return "Non-standars VASI";
        } else if ( glideSlope.equals( "PVT" ) ) {
            return "Private use only";
        } else if ( glideSlope.equals( "VAS" ) ) {
            return "Non-specific VASI";
        } else if ( glideSlope.equals( "NONE" ) ) {
            return "None";
        } else if ( glideSlope.equals( "N" ) ) {
            return "None";
        } else if ( glideSlope.equals( "TRIL" ) ) {
            return "Tri-color VASI on left side";
        } else if ( glideSlope.equals( "TRIR" ) ) {
            return "Tri-color VASI on right side";
        } else if ( glideSlope.equals( "PSIL" ) ) {
            return "Pulsating VASI on left side";
        } else if ( glideSlope.equals( "PSIR" ) ) {
            return "Pulsating VASI on right side";
        } else if ( glideSlope.equals( "PNIL" ) ) {
            return "Panel system on left side";
        } else if ( glideSlope.equals( "PNIR" ) ) {
            return "Panel system on right side";
        } else {
            return glideSlope;
        }
    }

    public static String decodeControllingObjectLighted( String lighted ) {
        if ( lighted.equals( "M" ) ) {
            return "marked";
        } else if ( lighted.equals( "L" ) ) {
            return "lighted";
        } else if ( lighted.equals( "ML" ) ) {
            return "marked & lighted";
        } else {
            return lighted;
        }
    }

    public static int decodeControllingObjectOffset( String offset ) {
        int end = 0;
        while ( end < offset.length()
                && offset.charAt( end ) >= '0' && offset.charAt( end ) <= '9' ) {
            ++end;
        }
        try {
            return Integer.valueOf( offset.substring( 0, end ) );
        } catch ( java.lang.NumberFormatException e ) {
            return 0;
        }
    }

    public static String decodeControllingObjectOffsetDirection( String offset ) {
        int end = 0;
        while ( end < offset.length()
                && offset.charAt( end ) >= '0' && offset.charAt( end ) <= '9' ) {
            ++end;
        }
        String direction = offset.substring( end );
        if ( direction.equals( "R" ) ) {
            return "right";
        } else if ( direction.equals( "L" ) ) {
            return "left";
        } else if ( direction.equals( "B" ) ) {
            return "both sides";
        } else if ( direction.equals( "L/R" ) ) {
            return "both sides";
        } else {
            return "vicinity";
        }
    }

    public static String decodeRVRLocations( String rvr ) {
        String decodedRvr = "";
        int index = 0;
        while ( index < rvr.length() ) {
            if ( decodedRvr.length() > 0 ) {
                decodedRvr += ", ";
            }
            if ( rvr.charAt( index ) == 'T' ) {
                decodedRvr += "touchdown";
            } else if ( rvr.charAt( index ) == 'M' ) {
                decodedRvr += "midpoint";
            } else if ( rvr.charAt( index ) == 'R' ) {
                decodedRvr += "rollout";
            }
            ++index;
        }
        return decodedRvr;
    }

    public static String decodeNavProtectedAltitude( String alt ) {
        if ( alt.equals( "T" ) ) {
            return "Terminal (25NM)";
        } else if ( alt.equals( "L" ) ) {
            return "Low altitude (40NM)";
        } else if ( alt.equals( "H" ) ) {
            return "High altitude (130NM)";
        } else {
            return alt;
        }
    }

    public static String decodeArtcc( String artcc ) {
        if ( artcc.equals( "ZAB" ) ) {
            return "Albuquerque Center";
        } else if ( artcc.equals( "ZAN" ) ) {
            return "Anchorage Center";
        } else if ( artcc.equals( "ZAP" ) ) {
            return "Anchorage Oceanic";
        } else if ( artcc.equals( "ZAU" ) ) {
            return "Chicago Center";
        } else if ( artcc.equals( "ZBW" ) ) {
            return "Boston Center";
        } else if ( artcc.equals( "ZDC" ) ) {
            return "Washington Center";
        } else if ( artcc.equals( "ZDV" ) ) {
            return "Denver Center";
        } else if ( artcc.equals( "ZFW" ) ) {
            return "Fort Worth Center";
        } else if ( artcc.equals( "ZHN" ) ) {
            return "Honolulu Control Facility";
        } else if ( artcc.equals( "ZHU" ) ) {
            return "Houston Center";
        } else if ( artcc.equals( "ZID" ) ) {
            return "Indianapolis Center";
        } else if ( artcc.equals( "ZJX" ) ) {
            return "Jacksonville Center";
        } else if ( artcc.equals( "ZKC" ) ) {
            return "Kansas City Center";
        } else if ( artcc.equals( "ZLA" ) ) {
            return "Los Angeles Center";
        } else if ( artcc.equals( "ZLC" ) ) {
            return "Salt Lake City Center";
        } else if ( artcc.equals( "ZMA" ) ) {
            return "Miami Center";
        } else if ( artcc.equals( "ZME" ) ) {
            return "Memphis Center";
        } else if ( artcc.equals( "ZMP" ) ) {
            return "Minneapolis Center";
        } else if ( artcc.equals( "ZNY" ) ) {
            return "New York Center";
        } else if ( artcc.equals( "ZOA" ) ) {
            return "Oakland Center";
        } else if ( artcc.equals( "ZOB" ) ) {
            return "Cleveland Center";
        } else if ( artcc.equals( "ZSE" ) ) {
            return "Seattle Center";
        } else if ( artcc.equals( "ZSU" ) ) {
            return "San Juan Center";
        } else if ( artcc.equals( "ZTL" ) ) {
            return "Atlanta Center";
        } else if ( artcc.equals( "ZUA" ) ) {
            return "Guam Center";
        }

        // Should never reach here
        return "";
    }

    public static String decodeFaaRegion( String code ) {
        if ( code.equals( "AAL" ) ) {
            return "Alaska";
        } else if ( code.equals( "ACE" ) ) {
            return "Central";
        } else if ( code.equals( "AEA" ) ) {
            return "Eastern";
        } else if ( code.equals( "AGL" ) ) {
            return "Great Lakes";
        } else if ( code.equals( "AIN" ) ) {
            return "International";
        } else if ( code.equals( "ANE" ) ) {
            return "New England";
        } else if ( code.equals( "ANM" ) ) {
            return "Northwest Mountain";
        } else if ( code.equals( "ASO" ) ) {
            return "Southern";
        } else if ( code.equals( "ASW" ) ) {
            return "Southwest";
        } else if ( code.equals( "AWP" ) ) {
            return "Western Pacific";
        }

        // Should never reach here
        return "";
    }

    public static String decodeAirspace( String airspace ) {
        String value = "";
        if ( airspace.charAt( 0 ) == 'Y' ) {
            value += "Class B";
        }
        if ( airspace.charAt( 1 ) == 'Y' ) {
            if ( value.length() > 0 ) {
                value += ", ";
            }
            value += "Class C";
        }
        if ( airspace.charAt( 2 ) == 'Y' ) {
            if ( value.length() > 0 ) {
                value += ", ";
            }
            value += "Class D";
        }
        if ( airspace.charAt( 3 ) == 'Y' ) {
            if ( value.length() > 0 ) {
                value += ", ";
            }
            value += "Class E";
        }
        return value;
    }

    public static String decodeChartCode( String chartCode ) {
        if ( chartCode.equalsIgnoreCase( "APD" ) ) {
            return "Airport Diagram (APD)";
        } else if ( chartCode.equalsIgnoreCase( "MIN" ) ) {
            return "Minimums (MIN)";
        } else if ( chartCode.equalsIgnoreCase( "STAR" ) ) {
            return "Terminal Arrivals (STAR)";
        } else if ( chartCode.equalsIgnoreCase( "IAP" ) ) {
            return "Approach Procedures (IAP)";
        } else if ( chartCode.equalsIgnoreCase( "DP" ) ) {
            return "Departure Procedures (DP)";
        } else if ( chartCode.equalsIgnoreCase( "DPO" ) ) {
            return "Obstacle Departures (DPO)";
        } else if ( chartCode.equalsIgnoreCase( "LAH" ) ) {
            return "LAHSO Operations (LAH)";
        } else if ( chartCode.equalsIgnoreCase( "HOT" ) ) {
            return "Airport Hot Spots (HOT)";
        } else {
            return "Other";
        }
    }

    public static String decodeUserAction( String userAction ) {
        if ( userAction != null && userAction.length() > 0 ) {
            char change = userAction.charAt( 0 );
            if ( change == 'C' ) {
                return "Changed";
            } else if ( change == 'A' ) {
                return "Added";
            } else if ( change == 'D' ) {
                return "Deleted";
            }
        }
        return "";
    }

    public static boolean isDirectionalNavaid( String type ) {
        return type.equals( "VOR" )
            || type.equals( "VOR/DME" )
            || type.equals( "DME" )
            || type.equals( "VORTAC" )
            || type.equals( "VOT" )
            || type.equals( "TACAN" );
    }

    public static double getTacanChannelFrequency( String channel ) {
        double freq = 0;
        if ( channel.length() > 0 ) {
            String type = channel.substring( channel.length()-1 );
            int num = Integer.valueOf( channel.substring( 0, channel.length()-1 ) );

            double offset = ((double)num)/10;
            if ( type.equals( "Y" ) ) {
                offset += 0.05;
            }

            if ( num >= 17 && num <= 59 ) {
                freq = 106.3+offset;
            } else if ( num >= 70 && num <= 126 ) {
                freq = 105.3+offset;
            }
        }

        return freq;
    }

    public static String getApproachLightSystemDescription( String als ) {
        if ( als.equals( "ALSAF" ) ) {
            return "3,000 ft high intensity approach lighting system with centerline "
                    +"sequence flashers";
        } else if ( als.equals( "ALSF1" ) ) {
            return "Standard 2,400 ft high intensity approach lighting system with "
                    +"sequenced flashers, CAT I";
        } else if ( als.equals( "ALSF2" ) ) {
            return "Standard 2,400 ft high intensity approach lighting system with "
                    +"sequenced flashers, CAT II or III";
        } else if ( als.equals( "MALS" ) ) {
            return "1,400 ft medium intensity approach lighting system";
        } else if ( als.equals( "MALSF" ) ) {
            return "1,400 ft medium intensity approach lighting system with sequenced flashers";
        } else if ( als.equals( "MALSR" ) ) {
            return "1,400 ft medium intensity approach lighting system with runway alignment "
                    +"indicator lights";
        } else if ( als.equals( "SSALS" ) ) {
            return "Simplified short approach lighting system";
        } else if ( als.equals( "SSALF" ) ) {
            return "Simplified short approach lighting system with sequenced flashers";
        } else if ( als.equals( "SSALR" ) ) {
            return "Simplified short approach lighting system with runway alignment "
                    +"indicator lights";
        } else if ( als.equals( "NEON" ) ) {
            return "Neon ladder approach lighting system";
        } else if ( als.equals( "ODALS" ) ) {
            return "Omni-directional approach lighting system";
        } else if ( als.equals( "LDIN" ) ) {
            return "Lead-in approach lighting system";
        } else if ( als.equals( "MIL OVRN" ) ) {
            return "Military overrun approach lighting system";
        } else {
            return "";
        }
    }

    public static String[] getNotamSubjects() {
        // Defines the order in which NOTAMs will be displayed by subject
        return new String[] {
                "Aerodrome",
                "Obstructions",
                "Movement and Landing Area",
                "Navigation",
                "Communications",
                "Airspace",
                "Services",
                "Flight Data Center",
                "Other"
        };
    }

    public static String getNotamSubjectFromKeyword( String keyword ) {
        if ( keyword.equals( "RWY" ) ) {
            return "Movement and Landing Area";
        } else if ( keyword.equals( "TWY" ) ) {
            return "Movement and Landing Area";
        } else if ( keyword.equals( "APRON" ) ) {
            return "Movement and Landing Area";
        } else if ( keyword.equals( "RAMP" ) ) {
            return "Movement and Landing Area";
        } else if ( keyword.equals( "AD" ) ) {
            return "Aerodrome";
        } else if ( keyword.equals( "AIRSPACE" ) ) {
            return "Airspace";
        } else if ( keyword.equals( "OBST" ) ) {
            return "Obstructions";
        } else if ( keyword.equals( "NAV" ) ) {
            return "Navigation";
        } else if ( keyword.equals( "COM" ) ) {
            return "Communications";
        } else if ( keyword.equals( "SVC" ) ) {
            return "Services";
        } else if ( keyword.equals( "FDC" ) ) {
            return "Flight Data Center";
        } else {
            return "Other";
        }
    }

    private static final Map<String, String> sTracons = new HashMap<>();
    static {
        sTracons.put( "A80", "Atlanta" );
        sTracons.put( "C90", "Chicago" );
        sTracons.put( "D10", "Regional" );
        sTracons.put( "D01", "Denver" );
        sTracons.put( "I90", "Houston" );
        sTracons.put( "N90", "New York" );
        sTracons.put( "NCT", "Norcal" );
        sTracons.put( "PCT", "Potomac" );
        sTracons.put( "SCT", "Socal" );
    }

    public static String getTraconId( String name ) {
        for ( Entry<String, String> entry : sTracons.entrySet() ) {
            if ( name.equals( entry.getValue() ) ) {
                return entry.getKey();
            }
        }
        return "";
    }

    public static String getTraconName( String id ) {
        return sTracons.get( id );
    }

    public static long metersToFeet( float meters ) {
        return Math.round( 3.2808*meters );
    }

    public static float feetToMeters( long feet ) {
        return (float) ( 0.3048*feet );
    }

    public static int getRunwayHeading( String runwayId ) {
        int index = 0;
        while ( index < runwayId.length() ) {
            if ( !Character.isDigit( runwayId.charAt( index ) ) ) {
                break;
            }
            ++index;
        }

        int heading = 0;
        try {
            heading = Integer.valueOf( runwayId.substring( 0, index ) )*10;
        } catch ( Exception ignored ) {
        }

        return heading;
    }

}
