/*
 * FlightIntel for Pilots
 *
 * Copyright 2016 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

public class ClassBUtils {

    static private HashMap<String, String> mClassBNames = new HashMap<>();
    static private HashMap<String, String> mClassBFileNames = new HashMap<>();
    static private String mClassBFacilityList = new String();

    private ClassBUtils() {}

    static {
        mClassBNames.put( "ATL", "Atlanta" );
        mClassBNames.put( "BOS", "Boston" );
        mClassBNames.put( "CLT", "Charlotte" );
        mClassBNames.put( "ORD", "Chicago" );
        mClassBNames.put( "CVG", "Cleveland" );
        mClassBNames.put( "DFW", "Dallas-Ft. Worth" );
        mClassBNames.put( "DEN", "Denver" );
        mClassBNames.put( "DTW", "Detroit" );
        mClassBNames.put( "HNL", "Honolulu" );
        mClassBNames.put( "IAH", "Houston" );
        mClassBNames.put( "MCI", "Kansas City" );
        mClassBNames.put( "LAS", "Las Vegas" );
        mClassBNames.put( "LAX", "Los Angeles" );
        mClassBNames.put( "MEM", "Memphis" );
        mClassBNames.put( "MIA", "Miami" );
        mClassBNames.put( "MSP", "Minneapolis" );
        mClassBNames.put( "MSY", "New Orleans" );
        mClassBNames.put( "EWR", "New York" );
        mClassBNames.put( "JFK", "New York" );
        mClassBNames.put( "LGA", "New York" );
        mClassBNames.put( "MCO", "Orlando" );
        mClassBNames.put( "PHL", "Philadelphia" );
        mClassBNames.put( "PHX", "Phoenix" );
        mClassBNames.put( "PIT", "Pittsburgh" );
        mClassBNames.put( "STL", "St. Louis" );
        mClassBNames.put( "SLC", "Salt Lake City" );
        mClassBNames.put( "SAN", "San Diego" );
        mClassBNames.put( "NKX", "San Diego" );
        mClassBNames.put( "SFO", "San Francisco" );
        mClassBNames.put( "SEA", "Seattle" );
        mClassBNames.put( "TPA", "Tampa" );
        mClassBNames.put( "ADW", "Washington Tri-Area" );
        mClassBNames.put( "BWI", "Washington Tri-Area" );
        mClassBNames.put( "DCA", "Washington Tri-Area" );
        mClassBNames.put( "IAD", "Washington Tri-Area" );

        mClassBFileNames.put( "ATL", "Atlanta_Class_B.pdf" );
        mClassBFileNames.put( "BOS", "Boston_Class_B.pdf" );
        mClassBFileNames.put( "CLT", "Charlotte_Class_B.pdf" );
        mClassBFileNames.put( "ORD", "Chicago_Class_B.pdf" );
        mClassBFileNames.put( "CVG", "Cleveland_Class_B.pdf" );
        mClassBFileNames.put( "DFW", "Dallas-Ft_Worth_Class_B.pdf" );
        mClassBFileNames.put( "DEN", "Denver_Class_B.pdf" );
        mClassBFileNames.put( "DTW", "Detroit_Class_B.pdf" );
        mClassBFileNames.put( "HNL", "Honolulu_Class_B.pdf" );
        mClassBFileNames.put( "IAH", "Houston_Class_B.pdf" );
        mClassBFileNames.put( "MCI", "Kansas_City_Class_B.pdf" );
        mClassBFileNames.put( "LAS", "Las_Vegas_Class_B.pdf" );
        mClassBFileNames.put( "LAX", "Los_Angeles_Class_B.pdf" );
        mClassBFileNames.put( "MEM", "Memphis_Class_B.pdf" );
        mClassBFileNames.put( "MIA", "Miami_Class_B.pdf" );
        mClassBFileNames.put( "MSP", "Minneapolis_Class_B.pdf" );
        mClassBFileNames.put( "MSY", "New_Orleans_Class_B.pdf" );
        mClassBFileNames.put( "EWR", "New_York_Class_B.pdf" );
        mClassBFileNames.put( "JFK", "New_York_Class_B.pdf" );
        mClassBFileNames.put( "LGA", "New_York_Class_B.pdf" );
        mClassBFileNames.put( "MCO", "Orlando_Class_B.pdf" );
        mClassBFileNames.put( "PHL", "Philadelphia_Class_B.pdf" );
        mClassBFileNames.put( "PHX", "Phoenix_Class_B.pdf" );
        mClassBFileNames.put( "PIT", "Pittsburgh_Class_B.pdf" );
        mClassBFileNames.put( "STL", "St._Louis_Class_B.pdf" );
        mClassBFileNames.put( "SLC", "Salt_Lake_Class_B.pdf" );
        mClassBFileNames.put( "SAN", "San_Diego_Class_B.pdf" );
        mClassBFileNames.put( "NKX", "San_Diego_Class_B.pdf" );
        mClassBFileNames.put( "SFO", "San_Francisco_Class_B.pdf" );
        mClassBFileNames.put( "SEA", "Seattle_Class_B.pdf" );
        mClassBFileNames.put( "TPA", "Tampa_Class_B.pdf" );
        mClassBFileNames.put( "ADW", "Washington_Tri-Area_Class_B.pdf" );
        mClassBFileNames.put( "BWI", "Washington_Tri-Area_Class_B.pdf" );
        mClassBFileNames.put( "DCA", "Washington_Tri-Area_Class_B.pdf" );
        mClassBFileNames.put( "IAD", "Washington_Tri-Area_Class_B.pdf" );

        Set<String> facilities = mClassBNames.keySet();
        for ( String facility : facilities ) {
            if ( !mClassBFacilityList.isEmpty() ) {
                mClassBFacilityList = mClassBFacilityList.concat( ", " );
            }
            mClassBFacilityList = mClassBFacilityList.concat(
                    String.format( Locale.US, "'%s'", facility ) );
        }
    }

    static public String getClassBFacilityList() {
        return mClassBFacilityList;
    }

    static public String getClassBName( String facility ) {
        return mClassBNames.get( facility );
    }

    static public String getClassBFilename( String facility ) {
        return mClassBFileNames.get( facility );
    }

}
