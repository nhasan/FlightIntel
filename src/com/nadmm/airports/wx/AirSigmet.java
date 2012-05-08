/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

public class AirSigmet implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class AirSigmetPoint implements Serializable {

        private static final long serialVersionUID = 1L;

        public float latitude;
        public float longitude;

        public AirSigmetPoint() {
            latitude = Float.MAX_VALUE;
            longitude = Float.MAX_VALUE;
        }
    }

    public static class AirSigmetEntry implements Serializable {

        private static final long serialVersionUID = 1L;

        public String rawText;
        public long fromTime;
        public long toTime;
        public int minAltitudeFeet;
        public int maxAltitudeFeet;
        public int movementDirDegrees;
        public int movementSpeedKnots;
        public String hazardType;
        public String hazardSeverity;
        public String type;
        ArrayList<AirSigmetPoint> points;

        public AirSigmetEntry() {
            fromTime = Long.MAX_VALUE;
            toTime = Long.MAX_VALUE;
            minAltitudeFeet = Integer.MAX_VALUE;
            maxAltitudeFeet = Integer.MAX_VALUE;
            movementDirDegrees = Integer.MAX_VALUE;
            movementSpeedKnots = Integer.MAX_VALUE;
        }
    }

    public AirSigmet() {
        isValid = false;
        fetchTime = Long.MAX_VALUE;
        entries = new ArrayList<AirSigmetEntry>();
    }

    public boolean isValid;
    public long fetchTime;
    public ArrayList<AirSigmetEntry> entries;

}
