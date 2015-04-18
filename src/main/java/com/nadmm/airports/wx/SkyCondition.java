/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import com.nadmm.airports.R;
import com.nadmm.airports.utils.FormatUtils;

public abstract class SkyCondition implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String mSkyCover;
    protected int mCloudBaseAGL;

    private SkyCondition() {
    }

    private SkyCondition( String skyCover, int cloudBaseAGL ) {
        mSkyCover = skyCover;
        mCloudBaseAGL = cloudBaseAGL;
    }

    public abstract int getDrawable();

    public String getSkyCover() {
        return mSkyCover;
    }

    public int getCloudBaseAGL() {
        return mCloudBaseAGL;
    }

    static public SkyCondition create( String skyCover, int cloudBaseAGL ) {
        SkyCondition sky = null;

        if ( skyCover.equalsIgnoreCase( "CLR" ) ) {
            sky = new SkyCondition( skyCover, 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return "Sky clear below 12,000 ft AGL";
                }

                public int getDrawable() {
                    return R.drawable.clr;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "SKC" ) ) {
            sky = new SkyCondition( skyCover, 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return "Sky clear";
                }

                public int getDrawable() {
                    return R.drawable.skc;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "FEW" ) ) {
            sky = new SkyCondition( skyCover, cloudBaseAGL ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Few clouds at %s",
                            FormatUtils.formatFeetAgl( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.few;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "SCT" ) ) {
            sky = new SkyCondition( skyCover, cloudBaseAGL ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Scattered clouds at %s",
                            FormatUtils.formatFeetAgl( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.sct;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "BKN" ) ) {
            sky = new SkyCondition( skyCover, cloudBaseAGL ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Broken clouds at %s",
                            FormatUtils.formatFeetAgl( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.bkn;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "OVC" ) ) {
            sky = new SkyCondition( skyCover, cloudBaseAGL ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return String.format( "Overcast clouds at %s",
                            FormatUtils.formatFeetAgl( mCloudBaseAGL ) );
                }

                public int getDrawable() {
                    return R.drawable.ovc;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "OVX" ) ) {
            sky = new SkyCondition( skyCover, 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return "Indefinite ceiling";
                }

                public int getDrawable() {
                    return R.drawable.ovx;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "SKM" ) ) {
            sky = new SkyCondition( skyCover, 0 ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return "Sky condition is missing";
                }

                public int getDrawable() {
                    return R.drawable.skm;
                }
            };
        } else if ( skyCover.equalsIgnoreCase( "NSC" ) ) {
            sky = new SkyCondition( skyCover, cloudBaseAGL ) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toString() {
                    return "No significant clouds";
                }

                public int getDrawable() {
                    return R.drawable.skm;
                }
            };
        }

        return sky;
    }

}
