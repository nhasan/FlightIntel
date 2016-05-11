/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2016 Nadeem Hasan <nhasan@nadmm.com>
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

import com.nadmm.airports.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class WxSymbol implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private static final String MINUS_SIGN = "-";
    private static final String PLUS_SIGN = "+";

    private static HashMap<String, WxSymbol> sWxSymbolsMap = new HashMap<>();
    private static ArrayList<String> sSymbolNames = new ArrayList<>();

    protected String mSymbol;
    protected int mDrawable;
    protected String mDesc;

    private WxSymbol() {}

    private WxSymbol( String symbol, int drawable, String desc )
    {
        mSymbol = symbol;
        mDrawable = drawable;
        mDesc = desc;

        sWxSymbolsMap.put( symbol, this );

        // Do not add the prefixed variants of the main symbol
        if ( !symbol.startsWith( PLUS_SIGN ) && !symbol.startsWith( MINUS_SIGN ) ) {
            sSymbolNames.add( symbol );
        }
    }

    public String getSymbol() {
        return mSymbol;
    }

    public int getDrawable() {
        return mDrawable;
    }

    @Override
    public String toString() {
        return mDesc;
    }

    public static WxSymbol get( String name, String intensity ) {
        WxSymbol symbol = null;
        try {
            String key = intensity+name;
            if ( sWxSymbolsMap.containsKey( key ) ) {
                symbol = (WxSymbol) sWxSymbolsMap.get( key ).clone();
            } else if ( sWxSymbolsMap.containsKey( name ) ) {
                symbol = (WxSymbol) sWxSymbolsMap.get( name ).clone();
            } else {
                symbol = new WxSymbol( key, 0, key );
            }
        } catch ( CloneNotSupportedException ignored ) {
        }
        return symbol;
    }

    static {
        new WxSymbol( "SHRA", R.drawable.sh, "Rainshowers" );
        new WxSymbol( "+SHRA", R.drawable.sh, "Heavy rainshowers" );
        new WxSymbol( "-SHRA", R.drawable.l_sh, "Light rainshowers" );

        new WxSymbol( "SHGS", R.drawable.gs, "Small hail showers" );
        new WxSymbol( "+SHGS", R.drawable.gs, "Heavy small hail showers" );
        new WxSymbol( "-SHGS", R.drawable.l_gs, "Light small hail showers" );

        new WxSymbol( "SHSN", R.drawable.shsn, "Snowshowers" );
        new WxSymbol( "+SHSN", R.drawable.shsn, "Heavy snowshowers" );
        new WxSymbol( "-SHSN", R.drawable.l_shsn, "Light snowshowers" );

        new WxSymbol( "SHGR", R.drawable.gr, "Hail showers" );
        new WxSymbol( "+SHGR", R.drawable.gr, "Heavy hail showers" );
        new WxSymbol( "-SHGR", R.drawable.l_gr, "Light hail showers" );

        new WxSymbol( "SHPL", R.drawable.pl, "Ice pellet showers" );
        new WxSymbol( "SHPE", R.drawable.pl, "Ice pellet showers" );

        new WxSymbol( "SH", R.drawable.sh, "Showers" );
        new WxSymbol( "+SH", R.drawable.sh, "Heavy showers" );
        new WxSymbol( "-SH", R.drawable.l_sh, "Light showers" );

        new WxSymbol( "RA", R.drawable.ra, "Rain" );
        new WxSymbol( "+RA", R.drawable.h_ra, "Heavy rain" );
        new WxSymbol( "-RA", R.drawable.l_ra, "Light rain" );

        new WxSymbol( "SN", R.drawable.sn, "Snow" );
        new WxSymbol( "+SN", R.drawable.h_sn, "Heavy snow" );
        new WxSymbol( "-SN", R.drawable.l_sn, "Light snow" );

        new WxSymbol( "DZ", R.drawable.dz, "Drizzle" );
        new WxSymbol( "+DZ", R.drawable.h_dz, "Heavy drizzle" );
        new WxSymbol( "-DZ", R.drawable.l_dz, "Light drizzle" );

        new WxSymbol( "TSRA", R.drawable.tsra, "Thunderstorm with rain" );
        new WxSymbol( "+TSRA", R.drawable.h_tsra, "Heavy thunderstorm with rain" );

        new WxSymbol( "TSPL", R.drawable.tsra, "Thunderstorm with ice pellets" );
        new WxSymbol( "+TSPL", R.drawable.h_tsra, "Heavy thunderstorm with ice pellets" );

        new WxSymbol( "TSSN", R.drawable.tsra, "Thunderstorm with snow" );
        new WxSymbol( "+TSSN", R.drawable.h_tsra, "Heavy thunderstorm with snow" );

        new WxSymbol( "TSGS", R.drawable.tsgr, "Thunderstorm with small hail" );
        new WxSymbol( "+TSGS", R.drawable.h_tsgr, "Heavy thunderstorm with small hail" );

        new WxSymbol( "TSGR", R.drawable.tsgr, "Thunderstorm with hail" );
        new WxSymbol( "+TSGR", R.drawable.h_tsgr, "Heavy thunderstorm with hail" );

        new WxSymbol( "FZDZ", R.drawable.fzdz, "Freezing drizzle" );
        new WxSymbol( "+FZDZ", R.drawable.fzdz, "Heavy freezing drizzle" );
        new WxSymbol( "-FZDZ", R.drawable.l_fzdz, "Light freezing drizzle" );

        new WxSymbol( "FZRA", R.drawable.fzra, "Freezing rain" );
        new WxSymbol( "+FZRA", R.drawable.fzra, "Heavy freezing rain" );
        new WxSymbol( "-FZRA", R.drawable.l_fzra, "Light freezing rain" );

        new WxSymbol( "PL", R.drawable.pl, "Ice pellets" );
        new WxSymbol( "PE", R.drawable.pl, "Ice pellets" );

        new WxSymbol( "GS", R.drawable.gs, "Small hail" );
        new WxSymbol( "+GS", R.drawable.gs, "Heavy small hail" );
        new WxSymbol( "-GS", R.drawable.l_gs, "Light small hail" );

        new WxSymbol( "GR", R.drawable.gr, "Hail" );
        new WxSymbol( "+GR", R.drawable.gr, "Heavy hail" );
        new WxSymbol( "-GR", R.drawable.l_gr, "Light hail" );

        new WxSymbol( "DS", R.drawable.ss, "Duststorm" );
        new WxSymbol( "+DS", R.drawable.h_ss, "Heavy duststorm" );

        new WxSymbol( "SS", R.drawable.ss, "Sandstorm" );
        new WxSymbol( "+SS", R.drawable.h_ss, "Heavy sandstorm" );

        new WxSymbol( "FC", R.drawable.fc, "Funnel clouds" );
        new WxSymbol( "+FC", R.drawable.fc, "Tornado" );

        new WxSymbol( "BCFG", R.drawable.bcfg, "Patches of fog" );

        new WxSymbol( "PRFG", R.drawable.prfg, "Partial fog" );

        new WxSymbol( "MIFG", R.drawable.mifg, "Shallow fog" );

        new WxSymbol( "BLDU", R.drawable.sa, "Blowing dust" );

        new WxSymbol( "BLSA", R.drawable.sa, "Blowing sand" );

        new WxSymbol( "BLSN", R.drawable.blsn, "Blowing snow" );

        new WxSymbol( "BLPY", R.drawable.sa, "Blowing spray" );

        new WxSymbol( "VCBR", R.drawable.vcbr, "Mist in the vicinity" );

        new WxSymbol( "VCTS", R.drawable.vcts, "Thunderstorm in the vicinity" );

        new WxSymbol( "DRDU", R.drawable.ss, "Low drifting dust" );

        new WxSymbol( "DRSA", R.drawable.ss, "Low drifting sand" );

        new WxSymbol( "DRSN", R.drawable.drsn, "Low drifting snow" );

        new WxSymbol( "FZFG", R.drawable.fzfg, "Freezing fog" );

        new WxSymbol( "VCFG", R.drawable.vcfg, "Fog in the vicinity" );

        new WxSymbol( "VCFC", R.drawable.fc, "Funel clouds in the vicinity" );

        new WxSymbol( "VCSS", R.drawable.vcss, "Sandstorm in the vicinity" );

        new WxSymbol( "VCDS", R.drawable.vcss, "Duststorm in the vicinity" );

        new WxSymbol( "VCSH", R.drawable.vcsh, "Showers in the vicinity" );

        new WxSymbol( "VCPO", R.drawable.po, "Dust/sand whirls in the vicinity" );

        new WxSymbol( "VCBLDU", R.drawable.sa, "Blowing dust in the vicinity" );

        new WxSymbol( "VCBLSA", R.drawable.sa, "Blowing sand in the vicinity" );

        new WxSymbol( "VCBLSN", R.drawable.blsn, "Blowing snow in the vicinity" );

        new WxSymbol( "BR", R.drawable.br, "Mist" );

        new WxSymbol( "DU", R.drawable.du, "Widespread dust" );

        new WxSymbol( "FG", R.drawable.fg, "Fog" );

        new WxSymbol( "FU", R.drawable.fu, "Smoke" );

        new WxSymbol( "HZ", R.drawable.hz, "Haze" );

        new WxSymbol( "IC", R.drawable.ic, "Ice crystals" );

        new WxSymbol( "UP", R.drawable.up, "Unknown precipitation" );

        new WxSymbol( "PO", R.drawable.po, "Dust/sand whirls" );

        new WxSymbol( "SG", R.drawable.sg, "Snow grains" );

        new WxSymbol( "SQ", R.drawable.sq, "Squalls" );

        new WxSymbol( "SA", R.drawable.sa, "Sand" );

        new WxSymbol( "TS", R.drawable.ts, "Thunderstorm" );

        new WxSymbol( "VA", R.drawable.fu, "Volcanic ash" );

        new WxSymbol( "NSW", 0, "No significant weather" );
    }

    public static void parseWxSymbols( ArrayList<WxSymbol> wxList, String wxString ) {
        String[] groups = wxString.split( "\\s+" );
        for ( String group : groups ) {
            int offset = 0;
            String intensity = "";
            String check = group.substring( offset, offset+1 );
            if ( check.equals( PLUS_SIGN ) || check.equals( MINUS_SIGN ) ) {
                intensity = check;
                ++offset;
            }
            while ( offset < group.length() ) {
                WxSymbol wx = null;
                for  ( String name : sSymbolNames  ) {
                    if ( group.substring( offset ).startsWith( name ) ) {
                        wx = WxSymbol.get( name, intensity );
                        intensity = "";
                        wxList.add( wx );
                        offset += name.length();
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
