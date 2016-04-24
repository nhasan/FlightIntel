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

package com.nadmm.airports.tfr;

import android.content.Context;
import android.location.Location;

import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.TimeUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class TfrList implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum AltitudeType {
        AGL {
            @Override
            public String toString() {
                return "AGL";
            }
        },
        MSL {
            @Override
            public String toString() {
                return "MSL";
            }
        },
        Unknown {
            @Override
            public String toString() {
                return "???";
            }
        }
    }

    public static class Tfr implements Serializable, Comparable<Tfr> {

        private static final long serialVersionUID = 1L;

        public String notamId;
        public String name;
        public String type;
        public String text;
        public int minAltitudeFeet;
        public AltitudeType minAltitudeType;
        public int maxAltitudeFeet;
        public AltitudeType maxAltitudeType;
        public Location location;
        public long createTime;
        public long modifyTime;
        public long activeTime;
        public long expireTime;

        public Tfr() {
            minAltitudeFeet = Integer.MAX_VALUE;
            minAltitudeType = AltitudeType.Unknown;
            maxAltitudeFeet = Integer.MAX_VALUE;
            maxAltitudeType = AltitudeType.Unknown;
            createTime = Long.MAX_VALUE;
            modifyTime = Long.MAX_VALUE;
            activeTime = Long.MAX_VALUE;
            expireTime = Long.MAX_VALUE;
        }

        public String formatAltitudeRange() {
            StringBuilder sb = new StringBuilder();
            if ( minAltitudeFeet < Integer.MAX_VALUE && maxAltitudeFeet < Integer.MAX_VALUE ) {
                if ( minAltitudeType == AltitudeType.AGL && minAltitudeFeet == 0 ) {
                    sb.append( "Surface" );
                } else {
                    sb.append( formatAltitude( minAltitudeFeet, minAltitudeType ) );
                }
                sb.append( " to " );
                sb.append( formatAltitude( maxAltitudeFeet, maxAltitudeType ) );
            } else if ( minAltitudeFeet < Integer.MAX_VALUE
                    && maxAltitudeFeet == Integer.MAX_VALUE ) {
                sb.append( formatAltitude( minAltitudeFeet, minAltitudeType ) );
                sb.append( " and above" );
            } else if ( minAltitudeFeet == Integer.MAX_VALUE
                    && maxAltitudeFeet < Integer.MAX_VALUE ) {
                sb.append( formatAltitude( maxAltitudeFeet, maxAltitudeType ) );
                sb.append( " and below" );
            } else {
                sb.append( "No altitudes" );
            }

            return sb.toString();
        }

        private String formatAltitude( int altitude, AltitudeType type ) {
            if ( type == AltitudeType.AGL ) {
                return FormatUtils.formatFeetAgl( altitude );
            } else {
                return FormatUtils.formatFeetMsl( altitude );
            }
        }

        public String formatTimeRange( Context context ) {
            StringBuilder sb = new StringBuilder();
            if ( activeTime < Long.MAX_VALUE && expireTime < Long.MAX_VALUE ) {
                sb.append( TimeUtils.formatDateRange( context, activeTime, expireTime ) );
            } else if ( activeTime < Long.MAX_VALUE ) {
                sb.append( TimeUtils.formatDateTimeYear( context, activeTime ) );
                sb.append( " onwards" );
            } else {
                sb.append( "Until further notice" );
            }

            return sb.toString();
        }

        public boolean isActive() {
            Long now = new Date().getTime();
            if ( activeTime < Long.MAX_VALUE && expireTime < Long.MAX_VALUE ) {
                return now >= activeTime && now < expireTime;
            } else if ( activeTime < Long.MAX_VALUE ) {
                return now >= activeTime;
            } else {
                return true;
            }
        }

        public boolean isExpired() {
            Long now = new Date().getTime();
            if ( expireTime < Long.MAX_VALUE ) {
                return now >= expireTime;
            } else {
                return false;
            }
        }

        @Override
        public int compareTo( Tfr another ) {
            return modifyTime == another.modifyTime? 0 : modifyTime < another.modifyTime? 1 : -1;
        }
    }

    public TfrList() {
        isValid = false;
        fetchTime = Long.MAX_VALUE;
        entries = new ArrayList<Tfr>();
    }

    public boolean isValid;
    public long fetchTime;
    public ArrayList<Tfr> entries;

}
