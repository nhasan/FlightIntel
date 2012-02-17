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

import java.io.Serializable;
import java.util.ArrayList;

public class Taf implements Serializable  {

    private static final long serialVersionUID = 1L;

    public static class TurbulenceCondition {
        public int intensity;
        public int minAltitudeFeetAGL;
        public int maxAltitudeFeetAGL;
    }

    public static class IcingCondition {
        public int intensity;
        public int minAltitudeFeetAGL;
        public int maxAltitudeFeetAGL;
    }

    public static class Temperature {
        public long validTime;
        public float surfaceTempCentigrade;
        public float maxTempCentigrade;
        public float minTempCentigrade;
    }

    public static class Forecast {
        public long timeFrom;
        public long timeto;
        public String changeIndicator;
        public long timeBecoming;
        public int probability;
        public int windDirDegrees;
        public int windSpeedKnots;
        public int windGustKnots;
        public int windShearDirDegrees;
        public int windShearSpeedKnots;
        public int windShearHeightFeetAGL;
        public float visibilitySM;
        public float altimeterHg;
        public int vertVisibilityFeet;
        public ArrayList<WxSymbol> wxList;
        public ArrayList<SkyCondition> skyConditions;
        public ArrayList<TurbulenceCondition> turbulenceConditions;
        public ArrayList<IcingCondition> icingConditions;
        public ArrayList<Temperature> temperatures;
    }

    public boolean isValid;
    public String stationId;
    public String rawText;
    public long issueTime;
    public long bulletinTime;
    public long validTimeFrom;
    public long validTimeTo;
    public float stationElevationMeters;
    public ArrayList<Forecast> forecasts;

    public Taf() {
        isValid = false;
        issueTime = 0;
        bulletinTime = 0;
        validTimeFrom = 0;
        validTimeTo = 0;
        stationElevationMeters = Float.MAX_VALUE;
    }

}
