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

import java.util.HashMap;

public final class DataUtils {

    static final HashMap<String, String> mStates = new HashMap<String, String>();
    
    static {
        mStates.put( "AK", "Alaska" );
        mStates.put( "AL", "Alabama" );
        mStates.put( "AS", "American Samoa" );
        mStates.put( "AZ", "Arizona" );
        mStates.put( "AR", "Arkansas" );
        mStates.put( "CA", "California" );
        mStates.put( "CN", "Canada" );
        mStates.put( "CO", "Colorado" );
        mStates.put( "CQ", "N. Marinara Islands" );
        mStates.put( "CT", "Connecticut" );
        mStates.put( "DE", "Delaware" );
        mStates.put( "DC", "District of Columbia" );
        mStates.put( "FM", "Fed Sts of Micronesia" );
        mStates.put( "FL", "Florida" );
        mStates.put( "GA", "Georgia" );
        mStates.put( "GU", "Guam" );
        mStates.put( "HI", "Hawai" );
        mStates.put( "IA", "Iowa" );
        mStates.put( "ID", "Idaho" );
        mStates.put( "IL", "Illinois" );
        mStates.put( "IN", "Indiana" );
        mStates.put( "IQ", "Pacific Islands" );
        mStates.put( "KS", "Kansas" );
        mStates.put( "KY", "Kentucky" );
        mStates.put( "LA", "Louisiana" );
        mStates.put( "MA", "Massachusetts" );
        mStates.put( "ME", "Maine" );
        mStates.put( "MH", "Marshall Islands" );
        mStates.put( "MD", "Maryland" );
        mStates.put( "MI", "Michigan" );
        mStates.put( "MN", "Minnesota" );
        mStates.put( "MS", "Mississippi" );
        mStates.put( "MO", "Missouri" );
        mStates.put( "MP", "N. Marinara Islands" );
        mStates.put( "MQ", "Midway Islands" );
        mStates.put( "MT", "Montana" );
        mStates.put( "NC", "North Carolina" );
        mStates.put( "ND", "North Dakota" );
        mStates.put( "NE", "Nebraska" );
        mStates.put( "NH", "New Hampshire" );
        mStates.put( "NJ", "New Jersey" );
        mStates.put( "NM", "New Mexico" );
        mStates.put( "NV", "Nevada" );
        mStates.put( "NY", "New York" );
        mStates.put( "OH", "Ohio" );
        mStates.put( "OK", "Oklahoma" );
        mStates.put( "OR", "Oregon" );
        mStates.put( "PA", "Pennsylvania" );
        mStates.put( "PR", "Puerto Rico" );
        mStates.put( "PS", "Palau" );
        mStates.put( "RI", "Rhode Island" );
        mStates.put( "SC", "South Carolina" );
        mStates.put( "SD", "South Dakota" );
        mStates.put( "TN", "Tennessee" );
        mStates.put( "TX", "Texas" );
        mStates.put( "UT", "Utah" );
        mStates.put( "VA", "Virginia" );
        mStates.put( "VI", "Virgin Islands" );
        mStates.put( "VT", "Vermont" );
        mStates.put( "WA", "Washington" );
        mStates.put( "WI", "Wisconsin" );
        mStates.put( "WV", "West Virginia" );
        mStates.put( "WY", "Wyoming" );
    }

    static String getStateName( String state ) {
        return mStates.get( state );
    }
}
