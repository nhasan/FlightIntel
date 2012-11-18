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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.nadmm.airports.R;

public abstract class WxSymbol implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private static Map<String, WxSymbol> sWxSymbolsMap = new HashMap<String, WxSymbol>();
    private static ArrayList<String> sSymbols = new ArrayList<String>();

    private WxSymbol() {}

    private WxSymbol( String symbol )
    {
        mIntensity = "";
        mSymbol = symbol;
        sSymbols.add( symbol );
        sWxSymbolsMap.put( symbol, this );
    }

    public String getSymbol() {
        return mSymbol;
    }

    @Override
    public String toString() {
        String desc = getDescription();
        if ( mIntensity.length() > 0 ) {
            if ( mIntensity.equals( "+" ) ) {
                desc = "Heavy "+desc.toLowerCase( Locale.US );
            } else if ( mIntensity.equals( "-" ) ) {
                desc = "Light "+desc.toLowerCase( Locale.US );
            }
        }
        return desc;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        WxSymbol o = (WxSymbol) super.clone();
        o.mIntensity = new String( mIntensity );
        return o;
    }

    public static WxSymbol get( String name, String intensity ) {
        WxSymbol symbol = null;
        try {
            if ( sWxSymbolsMap.containsKey( name ) ) {
                symbol = (WxSymbol) sWxSymbolsMap.get( name ).clone();
                symbol.mIntensity = intensity;
            }
        } catch ( CloneNotSupportedException e ) {
        }
        return symbol;
    }

    public static ArrayList<String> getSymbols() {
        return sSymbols;
    }

    abstract public int getDrawable();
    abstract protected String getDescription();

    protected String mIntensity;
    protected String mSymbol;

    static {
        new WxSymbol( "BCFG" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.bcfg;
            }

            @Override
            protected String getDescription() {
                return "Patches of fog";
            }
        };

        new WxSymbol( "PRFG" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.prfg;
            }

            @Override
            protected String getDescription() {
                return "Partial fog";
            }
        };

        new WxSymbol( "MIFG" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.mifg;
            }

            @Override
            protected String getDescription() {
                return "Shallow fog";
            }
        };

        new WxSymbol( "BLDU" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sa;
            }

            @Override
            protected String getDescription() {
                return "Blowing dust";
            }
        };

        new WxSymbol( "BLSA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sa;
            }

            @Override
            protected String getDescription() {
                return "Blowing sand";
            }
        };

        new WxSymbol( "BLSN" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.blsn;
            }

            @Override
            protected String getDescription() {
                return "Blowing snow";
            }
        };

        new WxSymbol( "BLPY" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sa;
            }

            @Override
            protected String getDescription() {
                return "Blowing spray";
            }
        };

        new WxSymbol( "VCBR" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.vcbr;
            }

            @Override
            protected String getDescription() {
                return "Mist in the vicinity";
            }
        };

        new WxSymbol( "TSGS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if (mIntensity.equals( "+" ) ) {
                    return R.drawable.h_tsgr;
                } else {
                    return R.drawable.tsgr;
                }
            }

            @Override
            protected String getDescription() {
                return "Thunderstorm with small hail";
            }
        };

        new WxSymbol( "TSGR" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if (mIntensity.equals( "+" ) ) {
                    return R.drawable.h_tsgr;
                } else {
                    return R.drawable.tsgr;
                }
            }

            @Override
            protected String getDescription() {
                return "Thunderstorm with hail";
            }
        };

        new WxSymbol( "VCTS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.vcts;
            }

            @Override
            protected String getDescription() {
                return "Thunderstorm in the vicinity";
            }
        };

        new WxSymbol( "DRDU" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.ss;
            }

            @Override
            protected String getDescription() {
                return "Low drifting dust";
            }
        };

        new WxSymbol( "DRSA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.ss;
            }

            @Override
            protected String getDescription() {
                return "Low drifting sand";
            }
        };

        new WxSymbol( "DRSN" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.drsn;
            }

            @Override
            protected String getDescription() {
                return "Low drifting snow";
            }
        };

        new WxSymbol( "FZFG" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.fzfg;
            }

            @Override
            protected String getDescription() {
                return "Freezing fog";
            }
        };

        new WxSymbol( "FZDZ" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_fzdz;
                } else {
                    return R.drawable.fzdz;
                }
            }

            @Override
            protected String getDescription() {
                return "Freezing drizzle";
            }
        };

        new WxSymbol( "FZRA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_fzra;
                } else {
                    return R.drawable.fzra;
                }
            }

            @Override
            protected String getDescription() {
                return "Freezing rain";
            }
        };

        new WxSymbol( "SHRA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_sh;
                } else {
                    return R.drawable.sh;
                }
            }

            @Override
            protected String getDescription() {
                return "Rainshowers";
            }
        };

        new WxSymbol( "SHSN" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_shsn;
                } else {
                    return R.drawable.shsn;
                }
            }

            @Override
            protected String getDescription() {
                return "Snowshowers";
            }
        };

        new WxSymbol( "SHPL" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.pl;
            }

            @Override
            protected String getDescription() {
                return "Ice pellet showers";
            }
        };

        new WxSymbol( "SHGS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_gs;
                } else {
                    return R.drawable.gs;
                }
            }

            @Override
            protected String getDescription() {
                return "Small hail showers";
            }
        };

        new WxSymbol( "SHGR" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_gr;
                } else {
                    return R.drawable.gr;
                }
            }

            @Override
            protected String getDescription() {
                return "Hail showers";
            }
        };

        new WxSymbol( "VCFG" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.vcfg;
            }

            @Override
            protected String getDescription() {
                return "Fog in the vicinity";
            }
        };

        new WxSymbol( "VCFC" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.fc;
            }

            @Override
            protected String getDescription() {
                return "Funel clouds in the vicinity";
            }
        };

        new WxSymbol( "VCSS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.vcss;
            }

            @Override
            protected String getDescription() {
                return "Sandstorm in the vicinity";
            }
        };

        new WxSymbol( "VCDS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.vcss;
            }

            @Override
            protected String getDescription() {
                return "Duststorm in the vicinity";
            }
        };

        new WxSymbol( "VCSH" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.vcsh;
            }

            @Override
            protected String getDescription() {
                return "Showers in the vicinity";
            }
        };

        new WxSymbol( "VCPO" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.po;
            }

            @Override
            protected String getDescription() {
                return "Dust/sand whirls in the vicinity";
            }
        };

        new WxSymbol( "VCBLDU" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sa;
            }

            @Override
            protected String getDescription() {
                return "Blowing dust in the vicinity";
            }
        };

        new WxSymbol( "VCBLSA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sa;
            }

            @Override
            protected String getDescription() {
                return "Blowing sand in the vicinity";
            }
        };

        new WxSymbol( "VCBLSN" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.blsn;
            }

            @Override
            protected String getDescription() {
                return "Blowing snow in the vicinity";
            }
        };

        new WxSymbol( "TSRA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_tsra;
                } else {
                    return R.drawable.tsra;
                }
            }

            @Override
            protected String getDescription() {
                return "Thunderstorm with rain";
            }
        };

        new WxSymbol( "TSPL" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_tsra;
                } else {
                    return R.drawable.tsra;
                }
            }

            @Override
            protected String getDescription() {
                return "Thunderstorm with ice pellets";
            }
        };

        new WxSymbol( "TSSN" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_tsra;
                } else {
                    return R.drawable.tsra;
                }
            }

            @Override
            protected String getDescription() {
                return "Thunderstorm with snow";
            }
        };

        new WxSymbol( "BR" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.br;
            }

            @Override
            protected String getDescription() {
                return "Mist";
            }
        };

        new WxSymbol( "DU" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.du;
            }

            @Override
            protected String getDescription() {
                return "Widespread dust";
            }
        };

        new WxSymbol( "DZ" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_dz;
                } else if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_dz;
                } else {
                    return R.drawable.dz;
                }
            }

            @Override
            protected String getDescription() {
                return "Drizzle";
            }
        };

        new WxSymbol( "DS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_ss;
                } else {
                    return R.drawable.ss;
                }
            }

            @Override
            protected String getDescription() {
                return "Duststorm";
            }
        };

        new WxSymbol( "FG" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.fg;
            }

            @Override
            protected String getDescription() {
                return "Fog";
            }
        };

        new WxSymbol( "FC" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.fc;
            }

            @Override
            protected String getDescription() {
                return "Funnel clouds";
            }

            @Override
            public String toString() {
                String desc;
                if ( mIntensity.equals( "+" ) ) {
                    desc = "Tornado";
                } else {
                    desc = "Funnel clouds";
                }
                return desc;
            }
        };

        new WxSymbol( "FU" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.fu;
            }

            @Override
            protected String getDescription() {
                return "Smoke";
            }
        };

        new WxSymbol( "GS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_gs;
                } else {
                    return R.drawable.gs;
                }
            }

            @Override
            protected String getDescription() {
                return "Small hail";
            }
        };

        new WxSymbol( "GR" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_gr;
                } else {
                    return R.drawable.gr;
                }
            }

            @Override
            protected String getDescription() {
                return "Hail";
            }
        };

        new WxSymbol( "HZ" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.hz;
            }

            @Override
            protected String getDescription() {
                return "Haze";
            }
        };

        new WxSymbol( "IC" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.ic;
            }

            @Override
            protected String getDescription() {
                return "Ice crystals";
            }
        };

        new WxSymbol( "UP" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.up;
            }

            @Override
            protected String getDescription() {
                return "Unknown precipitation";
            }
        };

        new WxSymbol( "PL" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.pl;
            }

            @Override
            protected String getDescription() {
                return "Ice pellets";
            }
        };

        new WxSymbol( "PO" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.po;
            }

            @Override
            protected String getDescription() {
                return "Dust/sand whirls";
            }
        };

        new WxSymbol( "RA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_ra;
                } else if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_ra;
                } else {
                    return R.drawable.ra;
                }
            }

            @Override
            protected String getDescription() {
                return "Rain";
            }
        };

        new WxSymbol( "SN" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_sn;
                } else if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_sn;
                } else {
                    return R.drawable.sn;
                }
            }

            @Override
            protected String getDescription() {
                return "Snow";
            }
        };

        new WxSymbol( "SG" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sg;
            }

            @Override
            protected String getDescription() {
                return "Snow grains";
            }
        };

        new WxSymbol( "SQ" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sq;
            }

            @Override
            protected String getDescription() {
                return "Squalls";
            }
        };

        new WxSymbol( "SA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.sa;
            }

            @Override
            protected String getDescription() {
                return "Sand";
            }
        };

        new WxSymbol( "SS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "+" ) ) {
                    return R.drawable.h_ss;
                } else {
                    return R.drawable.ss;
                }
            }

            @Override
            protected String getDescription() {
                return "Sandstorm";
            }
        };

        new WxSymbol( "SH" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals( "-" ) ) {
                    return R.drawable.l_sh;
                } else {
                    return R.drawable.sh;
                }
            }

            @Override
            protected String getDescription() {
                return "Showers";
            }
        };

        new WxSymbol( "TS" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.ts;
            }

            @Override
            protected String getDescription() {
                return "Thunderstorm";
            }
        };

        new WxSymbol( "VA" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return R.drawable.fu;
            }

            @Override
            protected String getDescription() {
                return "Volcanic ash";
            }
        };

        new WxSymbol( "NSW" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return 0;
            }

            @Override
            protected String getDescription() {
                return "No significant weather";
            }
        };
    }

    public static void parseWxSymbols( ArrayList<WxSymbol> wxList, String wxString ) {
        String[] groups = wxString.split( "\\s+" );
        ArrayList<String> names = WxSymbol.getSymbols();
        for ( String group : groups ) {
            int offset = 0;
            String intensity = "";
            if ( group.charAt( offset ) == '+' || group.charAt( offset ) == '-' ) {
                intensity = group.substring( offset, offset+1 );
                ++offset;
            }
            while ( offset < group.length() ) {
                WxSymbol wx = null;
                for  ( String name : names  ) {
                    if ( group.substring( offset ).startsWith( name ) ) {
                        wx = WxSymbol.get( name, intensity );
                        intensity = "";
                        wxList.add( wx );
                        offset += wx.getSymbol().length();
                        break;
                    }
                }

                if ( wx == null ) {
                    // No match found, skip to next character and try again
                    ++offset;
                }
            }
        }
    }

}
