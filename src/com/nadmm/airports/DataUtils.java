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

    public static int calculateMagneticHeading( int trueHeading, int variation ) {
        int magneticHeading = ( trueHeading+variation+360 ) % 360;
        if ( magneticHeading == 0 ) {
            magneticHeading = 360;
        }
        return magneticHeading;
    }

    public static String decodeOwnershipType( String ownership ) {
        if ( ownership.equals( "PU" ) ) {
            return "Publicly owned";
        } else if ( ownership.equals( "PR" ) ) {
            return "Privately owned";
        } else if ( ownership.equals( "MA" ) ) {
            return "Airforce owned";
        } else if ( ownership.equals( "MN" ) ) {
            return "Navy Owned";
        } else if ( ownership.equals( "MA" ) ) {
            return "Army owned";
        } else {
            return "Unknown ownership";
        }
    }

    public static String decodeFacilityUse( String use ) {
        if ( use.equals( "PU" ) ) {
            return "Open to the public";
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

    public static String decodeStorage( String storage ) {
        String decodedStorage = "";

        int start = 0;
        while ( start < storage.length() ) {
            int end = storage.indexOf( ",", start );
            if ( end == -1 ) {
                end = storage.length();
            }
            String type = storage.substring( start, end ).trim();

            if ( decodedStorage.length() > 0 ) {
                decodedStorage += ", ";
            }

            if ( type.equals( "BUOY" ) ) {
                decodedStorage += "Buoy";
            } else if ( type.equals( "HGR" ) ) {
                decodedStorage += "Hanger";
            } else if ( type.equals( "TIE" ) ) {
                decodedStorage += "Tiedown";
            } else {
                decodedStorage += type;
            }

            start = end+1;
        }

        return decodedStorage;
    }

    public static String decodeServices( String services ) {
        String decodedServices = "";

        int start = 0;
        while ( start < services.length() ) {
            int end = services.indexOf( ",", start );
            if ( end == -1 ) {
                end = services.length();
            }
            String type = services.substring( start, end ).trim();

            if ( decodedServices.length() > 0 ) {
                decodedServices += ", ";
            }

            if ( type.equals( "AFRT" ) ) {
                decodedServices += "Air freight";
            } else if ( type.equals( "AGRI" ) ) {
                decodedServices += "Crop dusting";
            } else if ( type.equals( "AMB" ) ) {
                decodedServices += "Air ambulance";
            } else if ( type.equals( "AVNCS" ) ) {
                decodedServices += "Avionics";
            } else if ( type.equals( "BCHGR" ) ) {
                decodedServices += "Beaching gear";
            } else if ( type.equals( "CARGO" ) ) {
                decodedServices += "Cargo";
            } else if ( type.equals( "CHTR" ) ) {
                decodedServices += "Charter";
            } else if ( type.equals( "GLD" ) ) {
                decodedServices += "Glider";
            } else if ( type.equals( "INSTR" ) ) {
                decodedServices += "Flight training";
            } else if ( type.equals( "PAJA" ) ) {
                decodedServices += "Para jump";
            } else if ( type.equals( "RNTL" ) ) {
                decodedServices += "Rental";
            } else if ( type.equals( "SALES" ) ) {
                decodedServices += "Sales";
            } else if ( type.equals( "SURV" ) ) {
                decodedServices += "Survey";
            } else if ( type.equals( "TOW" ) ) {
                decodedServices += "Glider towing";
            } else {
                decodedServices += type;
            }

            start = end+1;
        }

        return decodedServices;
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

    public static String decodeUnicomFreq( String unicom ) {
        String decodedUnicom = "";

        int start = 0;
        while ( start < unicom.length() ) {
            if ( decodedUnicom.length() > 0 ) {
                decodedUnicom += ", ";
            }
            int end = start+7;
            decodedUnicom += unicom.substring( start, end );
            start = end;
        }

        return decodedUnicom;
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
        return Integer.valueOf( offset.substring( 0, offset.length()-1 ) );
    }

    public static String decodeControllingObjectOffsetDirection( String offset ) {
        String direction = offset.substring( offset.length()-1 );
        if ( direction.equals( "R" ) ) {
            return "right";
        } else if ( direction.equals( "L" ) ) {
            return "left";
        } else if ( direction.equals( "B" ) ) {
            return "both sides";
        } else {
            return direction;
        }
    }

}
