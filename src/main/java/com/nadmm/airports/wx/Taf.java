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

    public static class TurbulenceCondition implements Serializable {
        private static final long serialVersionUID = 1L;

        public int intensity;
        public int minAltitudeFeetAGL;
        public int maxAltitudeFeetAGL;

        public TurbulenceCondition() {
            intensity = Integer.MAX_VALUE;
            minAltitudeFeetAGL = Integer.MAX_VALUE;
            maxAltitudeFeetAGL = Integer.MAX_VALUE;
        }
    }

    public static class IcingCondition implements Serializable {
        private static final long serialVersionUID = 1L;

        public int intensity;
        public int minAltitudeFeetAGL;
        public int maxAltitudeFeetAGL;

        public IcingCondition() {
            intensity = Integer.MAX_VALUE;
            minAltitudeFeetAGL = Integer.MAX_VALUE;
            maxAltitudeFeetAGL = Integer.MAX_VALUE;
        }
    }

    public static class Temperature implements Serializable {
        private static final long serialVersionUID = 1L;

        public long validTime;
        public float surfaceTempCentigrade;
        public float maxTempCentigrade;
        public float minTempCentigrade;

        public Temperature() {
            validTime = Long.MAX_VALUE;
            surfaceTempCentigrade = Float.MAX_VALUE;
            maxTempCentigrade = Float.MAX_VALUE;
            minTempCentigrade = Float.MAX_VALUE;
        }
    }

    public static class Forecast implements Serializable {
        private static final long serialVersionUID = 1L;

        public String changeIndicator;
        public long timeFrom;
        public long timeTo;
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

        public Forecast() {
            timeFrom = 0;
            timeTo = 0;
            timeBecoming = 0;
            probability = Integer.MAX_VALUE;
            windDirDegrees = Integer.MAX_VALUE;
            windSpeedKnots = Integer.MAX_VALUE;
            windGustKnots = Integer.MAX_VALUE;
            windShearDirDegrees = Integer.MAX_VALUE;
            windShearSpeedKnots = Integer.MAX_VALUE;
            windShearHeightFeetAGL = Integer.MAX_VALUE;
            visibilitySM = Float.MAX_VALUE;
            altimeterHg = Float.MAX_VALUE;
            vertVisibilityFeet = Integer.MAX_VALUE;
            wxList = new ArrayList<WxSymbol>();
            skyConditions = new ArrayList<SkyCondition>();
            turbulenceConditions = new ArrayList<TurbulenceCondition>();
            icingConditions = new ArrayList<IcingCondition>();
            temperatures = new ArrayList<Temperature>();
        }
    }

    public boolean isValid;
    public String stationId;
    public String rawText;
    public long fetchTime;
    public long issueTime;
    public long bulletinTime;
    public long validTimeFrom;
    public long validTimeTo;
    public float stationElevationMeters;
    public String remarks;
    public ArrayList<Forecast> forecasts;

    public Taf() {
        isValid = false;
        fetchTime = 0;
        issueTime = 0;
        bulletinTime = 0;
        validTimeFrom = 0;
        validTimeTo = 0;
        stationElevationMeters = Float.MAX_VALUE;
        forecasts = new ArrayList<Forecast>();
    }

}
