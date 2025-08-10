/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2016 Nadeem Hasan <nhasan@nadmm.com>
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

public class Pirep implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class SkyCondition implements Serializable {
        private static final long serialVersionUID = 1L;

        public String skyCover;
        public int baseFeetMSL;
        public int topFeetMSL;

        public SkyCondition( String type, int baseFeetMSL, int topFeetMSL ) {
            this.skyCover = type;
            this.baseFeetMSL = baseFeetMSL;
            this.topFeetMSL = topFeetMSL;
        }
    }

    public static class TurbulenceCondition implements Serializable {
        private static final long serialVersionUID = 1L;

        public String type;
        public String intensity;
        String frequency;
        public int baseFeetMSL;
        public int topFeetMSL;

        public TurbulenceCondition( String type, String intensity, String frequency,
                int baseFeetMSL, int topFeetMSL ) {
            this.type = type;
            this.intensity = intensity;
            this.frequency = frequency;
            this.baseFeetMSL = baseFeetMSL;
            this.topFeetMSL = topFeetMSL;
        }
    }

    public static class IcingCondition implements Serializable {
        private static final long serialVersionUID = 1L;

        public String type;
        public String intensity;
        public int baseFeetMSL;
        public int topFeetMSL;

        public IcingCondition( String type, String intensity, int baseFeetMSL, int topFeetMSL ) {
            this.type = type;
            this.intensity = intensity;
            this.baseFeetMSL = baseFeetMSL;
            this.topFeetMSL = topFeetMSL;
        }
    }

    public enum Flags {
        MidPointAssumed {
            @Override
            public String toString() {
                return "Mid-point assumed";
            }
        },
        NoTimeStamp {
            @Override
            public String toString() {
                return "No timestamp";
            }
        },
        FlightLevelRange {
            @Override
            public String toString() {
                return "Flight-level range";
            }
        },
        AglIndicated {
            @Override
            public String toString() {
                return "AGL indicated";
            }
        },
        NoFlightLevel {
            @Override
            public String toString() {
                return "No flight-level";
            }
        },
        BadLocation {
            @Override
            public String toString() {
                return "Bad location";
            }
        }
    }

    public static class PirepEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean isValid;
        public long receiptTime;
        public long observationTime;
        public String reportType;
        public String rawText;
        public String aircraftRef;
        public float latitude;
        public float longitude;
        public float distanceNM;
        public float bearing;
        public int altitudeFeetMSL;
        public int visibilitySM;
        public int tempCelsius;
        public int windDirDegrees;
        public int windSpeedKnots;
        public int vertGustKnots;
        ArrayList<Flags> flags;
        ArrayList<SkyCondition> skyConditions;
        ArrayList<WxSymbol> wxList;
        ArrayList<TurbulenceCondition> turbulenceConditions;
        ArrayList<IcingCondition> icingConditions;

        public PirepEntry() {
            isValid = false;
            receiptTime = 0;
            observationTime = 0;
            latitude = 0;
            longitude = 0;
            distanceNM = 0;
            bearing = 0;
            altitudeFeetMSL = Integer.MAX_VALUE;
            visibilitySM = Integer.MAX_VALUE;
            tempCelsius = Integer.MAX_VALUE;
            windDirDegrees = Integer.MAX_VALUE;
            windSpeedKnots = Integer.MAX_VALUE;
            vertGustKnots = Integer.MAX_VALUE;
            flags = new ArrayList<>();
            skyConditions = new ArrayList<>();
            wxList = new ArrayList<>();
            turbulenceConditions = new ArrayList<>();
            icingConditions = new ArrayList<>();
        }
    }

    public long fetchTime;
    public ArrayList<PirepEntry> entries;

    public Pirep() {
        fetchTime = 0;
        entries = new ArrayList<>();
    }

}
