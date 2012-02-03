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
import java.text.NumberFormat;

import com.nadmm.airports.R;

public abstract class SkyCondition implements Serializable {

    private static final long serialVersionUID = 1L;
    private static NumberFormat sDecimal = NumberFormat.getNumberInstance();

    protected int mCloudBaseAGL;
    protected String mName;

    private SkyCondition() {
    }

    private SkyCondition( String name, int cloudBase ) {
        mName = name;
        mCloudBaseAGL = cloudBase;
    }

    public abstract int getDrawable();

    public String name() {
        return mName;
    }

    public int getCloudBase() {
        return mCloudBaseAGL;
    }

    static public SkyCondition create( String name, int cloudBase ) {
        SkyCondition sky = null;

        if ( name.equalsIgnoreCase( "CLR" ) ) {
            sky = new SkyCondition( name, 0 ) {
                private static final long serialVersionUID = 1L;
    
                @Override
                public String toString() {
                    return String.format( "Sky clear below 12,000 ft AGL" );
                }
    
                public int getDrawable() {
                    return R.drawable.clr;
                }
            };
        } else if ( name.equalsIgnoreCase( "SKC" ) ) {
            sky = new SkyCondition( name, 0 ) {
                private static final long serialVersionUID = 1L;
    
                @Override
                public String toString() {
                    return "Sky clear";
                }
    
                public int getDrawable() {
                    return R.drawable.skc;
                }
            };
        } else if ( name.equalsIgnoreCase( "FEW" ) ) {
            sky = new SkyCondition( name, cloudBase ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Few clouds at %s ft AGL",
                            sDecimal.format( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.few;
                }
            };
        } else if ( name.equalsIgnoreCase( "SCT" ) ) {
            sky = new SkyCondition( name, cloudBase ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Scattered clouds at %s ft AGL",
                            sDecimal.format( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.sct;
                }
            };
        } else if ( name.equalsIgnoreCase( "BKN" ) ) {
            sky = new SkyCondition( name, cloudBase ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Broken clouds at %s ft AGL",
                            sDecimal.format( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.bkn;
                }
            };
        } else if ( name.equalsIgnoreCase( "OVC" ) ) {
            sky = new SkyCondition( name, cloudBase ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Overcast clouds at %s ft AGL",
                            sDecimal.format( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.ovc;
                }
            };
        } else if ( name.equalsIgnoreCase( "OVX" ) ) {
            sky = new SkyCondition( name, 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Indefinite ceiling" );
                }

                public int getDrawable() {
                    return R.drawable.ovx;
                }
            };
        } else if ( name.equalsIgnoreCase( "SKM" ) ) {
            sky = new SkyCondition( name, 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Sky condition is missing" );
                }

                public int getDrawable() {
                    return R.drawable.skm;
                }
            };
        }

        return sky;
    }

}
