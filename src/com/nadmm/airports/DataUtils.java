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

    public static String decodeRunwaySurfaceType( String surfaceType ) {
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
                decodedSurfaceType += "Excellent condition";
            } else if ( type.equals( "G" ) ) {
                decodedSurfaceType += "Good condition";
            } else if ( type.equals( "F" ) ) {
                decodedSurfaceType += "Fair condition";
            } else if ( type.equals( "P" ) ) {
                decodedSurfaceType += "Poor condition";
            } else if ( type.equals( "L" ) ) {
                decodedSurfaceType += "Failed condition";
            }

            start = end+1;
        }

        return decodedSurfaceType;
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

}
