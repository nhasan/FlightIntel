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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.nadmm.airports.R;

public abstract class WxSymbol implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    public static final String MINUS_SIGN = "-";
    public static final String PLUS_SIGN = "+";

    private static Map<String, WxSymbol> sWxSymbolsMap = new HashMap<String, WxSymbol>();
    private static ArrayList<String> sSymbols = new ArrayList<String>();


    protected String mIntensity;
    protected String mSymbol;

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
            if ( mIntensity.equals(PLUS_SIGN) ) {
                desc = "Heavy "+desc.toLowerCase( Locale.US );
            } else if ( mIntensity.equals(MINUS_SIGN) ) {
                desc = "Light "+desc.toLowerCase( Locale.US );
            }
        }
        return desc;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        WxSymbol o = (WxSymbol) super.clone();
        o.mIntensity = mIntensity;
        return o;
    }

    public static WxSymbol get( String name, String intensity ) {
        WxSymbol symbol = null;
        try {
            if ( sWxSymbolsMap.containsKey( name ) ) {
                symbol = (WxSymbol) sWxSymbolsMap.get( name ).clone();
                symbol.mIntensity = intensity;
            }
        } catch ( CloneNotSupportedException ignored ) {
        }
        return symbol;
    }

    public static ArrayList<String> getSymbols() {
        return sSymbols;
    }

    abstract public int getDrawable();
    abstract protected String getDescription();

    static {
        createWxSymbol("BCFG", R.drawable.bcfg, "Patches of fog");

        createWxSymbol("PRFG", R.drawable.prfg, "Partial fog");

        createWxSymbol("MIFG", R.drawable.mifg, "Shallow fog");

        createWxSymbol("BLDU", R.drawable.sa, "Blowing dust");

        createWxSymbol("BLSA", R.drawable.sa, "Blowing sand");

        createWxSymbol("BLSN", R.drawable.blsn, "Blowing snow");

        createWxSymbol("BLPY", R.drawable.sa, "Blowing spray");

        createWxSymbol("VCBR", R.drawable.vcbr, "Mist in the vicinity");

        createWxSymbol("TSGS", R.drawable.h_tsgr, R.drawable.tsgr, "Thunderstorm with small hail", PLUS_SIGN);

        createWxSymbol("TSGR", R.drawable.h_tsgr, R.drawable.tsgr, "Thunderstorm with hail", PLUS_SIGN);

        createWxSymbol("VCTS", R.drawable.vcts, "Thunderstorm in the vicinity");

        createWxSymbol("DRDU", R.drawable.ss, "Low drifting dust");

        createWxSymbol("DRSA", R.drawable.ss, "Low drifting sand");

        createWxSymbol("DRSN", R.drawable.drsn, "Low drifting snow");

        createWxSymbol("FZFG", R.drawable.fzfg, "Freezing fog");

        createWxSymbol("FZDZ", R.drawable.l_fzdz, R.drawable.fzdz, "Freezing drizzle", MINUS_SIGN);

        createWxSymbol("FZRA", R.drawable.l_fzra, R.drawable.fzra, "Freezing rain", MINUS_SIGN);

        createWxSymbol("SHRA", R.drawable.l_sh, R.drawable.sh, "Rainshowers", MINUS_SIGN);

        createWxSymbol("SHSN", R.drawable.l_shsn, R.drawable.shsn, "Snowshowers", MINUS_SIGN);

        createWxSymbol("SHPL", R.drawable.pl, "Ice pellet showers");

        createWxSymbol("SHGS", R.drawable.l_gs, R.drawable.gs, "Small hail showers", MINUS_SIGN);

        createWxSymbol("SHGR", R.drawable.l_gr, R.drawable.gr, "Hail showers", MINUS_SIGN);

        createWxSymbol("VCFG", R.drawable.vcfg, "Fog in the vicinity");

        createWxSymbol("VCFC", R.drawable.fc, "Funel clouds in the vicinity");

        createWxSymbol("VCSS", R.drawable.vcss, "Sandstorm in the vicinity");

        createWxSymbol("VCDS", R.drawable.vcss, "Duststorm in the vicinity");

        createWxSymbol("VCSH", R.drawable.vcsh, "Showers in the vicinity");

        createWxSymbol("VCPO", R.drawable.po, "Dust/sand whirls in the vicinity");

        createWxSymbol("VCBLDU", R.drawable.sa, "Blowing dust in the vicinity");

        createWxSymbol("VCBLSA", R.drawable.sa, "Blowing sand in the vicinity");

        createWxSymbol("VCBLSN", R.drawable.blsn, "Blowing snow in the vicinity");

        createWxSymbol("TSRA", R.drawable.h_tsra, R.drawable.tsra, "Thunderstorm with rain", PLUS_SIGN);

        createWxSymbol("TSPL", R.drawable.h_tsra, R.drawable.tsra, "Thunderstorm with ice pellets", PLUS_SIGN);

        createWxSymbol("TSSN", R.drawable.h_tsra, R.drawable.tsra, "Thunderstorm with snow", PLUS_SIGN);

        createWxSymbol("BR", R.drawable.br, "Mist");

        createWxSymbol("DU", R.drawable.du, "Widespread dust");

        createWxSymbol("DZ", R.drawable.h_dz, R.drawable.l_dz, R.drawable.dz, "Drizzle");

        createWxSymbol("DS", R.drawable.h_ss, R.drawable.ss, "Duststorm", PLUS_SIGN);

        createWxSymbol("FG", R.drawable.fg, "Fog");

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
                if ( mIntensity.equals(PLUS_SIGN) ) {
                    desc = "Tornado";
                } else {
                    desc = "Funnel clouds";
                }
                return desc;
            }
        };

        createWxSymbol("FU", R.drawable.fu, "Smoke");

        createWxSymbol("GS", R.drawable.l_gs, R.drawable.gs, "Small hail", MINUS_SIGN);

        createWxSymbol("GR", R.drawable.l_gr, R.drawable.gr, "Hail", MINUS_SIGN);

        createWxSymbol("HZ", R.drawable.hz, "Haze");

        createWxSymbol("IC", R.drawable.ic, "Ice crystals");

        createWxSymbol("UP", R.drawable.up, "Unknown precipitation");

        createWxSymbol("PL", R.drawable.pl, "Ice pellets");

        createWxSymbol("PO", R.drawable.po, "Dust/sand whirls");

        createWxSymbol("RA", R.drawable.h_ra, R.drawable.l_ra, R.drawable.ra, "Rain");

        createWxSymbol("SN", R.drawable.h_sn, R.drawable.l_sn, R.drawable.sn, "Snow");

        createWxSymbol("SG", R.drawable.sg, "Snow grains");

        createWxSymbol("SQ", R.drawable.sq, "Squalls");

        createWxSymbol("SA", R.drawable.sa, "Sand");

        createWxSymbol("SS", R.drawable.h_ss, R.drawable.ss, "Sandstorm", PLUS_SIGN);

        createWxSymbol("SH", R.drawable.l_sh, R.drawable.sh, "Showers", MINUS_SIGN);

        createWxSymbol("TS", R.drawable.ts, "Thunderstorm");

        createWxSymbol("VA", R.drawable.fu, "Volcanic ash");

        createWxSymbol("NSW", 0, "No significant weather");
    }

    private static void createWxSymbol(String code, final int drawable, final String description) {
        new WxSymbol(code) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                return drawable;
            }

            @Override
            protected String getDescription() {
                return description;
            }
        };
    }

    private static void createWxSymbol(String code, final int drawable1, final int drawable2, final String description, final String sign) {
        new WxSymbol(code) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if (mIntensity.equals(sign) ) {
                    return drawable1;
                } else {
                    return drawable2;
                }
            }

            @Override
            protected String getDescription() {
                return description;
            }
        };
    }

    private static void createWxSymbol(String code, final int drawable1, final int drawable2, final int drawable3, final String description) {
        new WxSymbol(code) {

            private static final long serialVersionUID = 1L;

            @Override
            public int getDrawable() {
                if ( mIntensity.equals(PLUS_SIGN) ) {
                    return drawable1;
                } else if ( mIntensity.equals(MINUS_SIGN) ) {
                    return drawable2;
                } else {
                    return drawable3;
                }
            }

            @Override
            protected String getDescription() {
                return description;
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
