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

public final class DataUtils {

    public static String decodeOwnershipType( String ownership ) {
        if ( ownership.equals( "PU" ) ) {
            return "Publicly Owned";
        } else if ( ownership.equals( "PR" ) ) {
            return "Privately Owned";
        } else if ( ownership.equals( "MA" ) ) {
            return "Airforce Owned";
        } else if ( ownership.equals( "MN" ) ) {
            return "Navy Owned";
        } else if ( ownership.equals( "MA" ) ) {
            return "Army Owned";
        } else {
            return "Unknown Ownership";
        }
    }

    public static String decodeFacilityUse( String use ) {
        if ( use.equals( "PU" ) ) {
            return "Open to Public";
        } else if ( use.equals( "PR" ) ) {
            return "Private Use";
        } else {
            return "Unknown use";
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
                decodedFuel += "JET A";
            } else if ( type.equals( "A+" ) ) {
                decodedFuel += "JET A+";
            } else if ( type.equals( "A1" ) ) {
                decodedFuel += "JET A1";
            } else if ( type.equals( "A1+" ) ) {
                decodedFuel += "JET A1+";
            } else if ( type.equals( "B" ) ) {
                decodedFuel += "JET B";
            } else if ( type.equals( "B+" ) ) {
                decodedFuel += "JET B+";
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
}
