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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;

public final class Metar implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Flags {
        Corrected {
            @Override
            public String toString() {
                return "Corrected METAR";
            }
        },
        AutoStation {
            @Override
            public String toString() {
                return "Automated station";
            }
        },
        AutoReport {
            @Override
            public String toString() {
                return "Automated observation with no human augmentation";
            }
        },
        MaintenanceIndicatorOn {
            @Override
            public String toString() {
                return "Station needs maintenance";
            }
        },
        PresentWeatherSensorOff {
            @Override
            public String toString() {
                return "Present weather sensor is not operating";
            }
        },
        LightningSensorOff {
            @Override
            public String toString() {
                return "Lightning detection sensor is not operating";
            }
        },
        RainSensorOff {
            @Override
            public String toString() {
                return "Rain sensor is not operating";
            }
        },
        FreezingRainSensorOff {
            @Override
            public String toString() {
                return "Freezing rain sensor is not operating";
            }
        };
    }

    public boolean isValid;
    public String stationId;
    public String rawText;
    public long observationTime;
    public long fetchTime;
    public float tempCelsius;
    public float dewpointCelsius;
    public int windDirDegrees;
    public int windSpeedKnots;
    public int windGustKnots;
    public int windPeakKnots;
    public float visibilitySM;
    public float altimeterHg;
    public float seaLevelPressureMb;
    public ArrayList<WxSymbol> wxList;
    public ArrayList<SkyCondition> skyConditions;
    public String flightCategory;
    public float pressureTend3HrMb;
    public float maxTemp6HrCentigrade;
    public float minTemp6HrCentigrade;
    public float maxTemp24HrCentigrade;
    public float minTemp24HrCentigrade;
    public float precipInches;
    public float precip3HrInches;
    public float precip6HrInches;
    public float precip24HrInches;
    public float snowInches;
    public int vertVisibilityFeet;
    public String metarType;
    public float stationElevationMeters;

    public EnumSet<Flags> flags;
    public boolean presrr;
    public boolean presfr;
    public boolean snincr;
    public boolean wshft;
    public boolean fropa;

    public Metar() {
        isValid = false;
        tempCelsius = Float.MAX_VALUE;
        dewpointCelsius = Float.MAX_VALUE;
        windDirDegrees = Integer.MAX_VALUE;
        windSpeedKnots = Integer.MAX_VALUE;
        windGustKnots = Integer.MAX_VALUE;
        windPeakKnots = Integer.MAX_VALUE;
        visibilitySM = Float.MAX_VALUE;
        altimeterHg = Float.MAX_VALUE;
        seaLevelPressureMb = Float.MAX_VALUE;
        pressureTend3HrMb = Float.MAX_VALUE;
        maxTemp6HrCentigrade = Float.MAX_VALUE;
        minTemp6HrCentigrade = Float.MAX_VALUE;
        maxTemp24HrCentigrade = Float.MAX_VALUE;
        minTemp24HrCentigrade = Float.MAX_VALUE;
        precipInches = Float.MAX_VALUE;
        precip3HrInches = Float.MAX_VALUE;
        precip6HrInches = Float.MAX_VALUE;
        precip24HrInches = Float.MAX_VALUE;
        snowInches = Float.MAX_VALUE;
        vertVisibilityFeet = Integer.MAX_VALUE;
        stationElevationMeters = Float.MAX_VALUE;

        wxList = new ArrayList<WxSymbol>();
        skyConditions = new ArrayList<SkyCondition>();

        flags = EnumSet.noneOf( Flags.class );
        presrr = false;
        presfr = false;
        snincr = false;
        wshft = false;
        fropa = false;
    }

    public void computeFlightCategory() {
        int ceiling = 12000;
        for ( SkyCondition sky : skyConditions ) {
            // Ceiling is defined as the lowest layer aloft reported as broken or overcast;
            // or the vertical visibility into an indefinite ceiling
            if ( sky.name().equals( "BKN" ) 
                    || sky.name().equals( "OVC" ) 
                    || sky.name().equals( "OVX" ) ) {
                ceiling =sky.getCloudBase();
                break;
            }
        }
        if ( ceiling < 500 || visibilitySM < 1.0 ) {
            flightCategory = "LIFR";
        } else if ( ceiling < 1000 || visibilitySM < 3.0 ) {
            flightCategory = "IFR";
        } else if ( ceiling <= 3000 || visibilitySM <= 5.0 ) {
            flightCategory = "MVFR";
        } else {
            flightCategory = "VFR";
        }
    }

    public void setMissingFields() {
        if ( flightCategory == null ) {
            computeFlightCategory();
        }
        if ( vertVisibilityFeet < Integer.MAX_VALUE ) {
            // Check to see if we have an OVX layer, if not add it
            boolean found = false;
            for ( SkyCondition sky : skyConditions ) {
                if ( sky.name().equals( "OVX" ) ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                skyConditions.add( SkyCondition.create( "OVX", 0 ) );
            }
        }
        if ( skyConditions.isEmpty() ) {
            // Sky condition is not available in the METAR
            skyConditions.add( SkyCondition.create( "SKM", 0 ) );
        }
    }

}
