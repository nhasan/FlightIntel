/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.os.Parcelable
import com.nadmm.airports.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
class WxSymbol(
    val symbol: String,
    val drawable: Int = 0,
    val description: String
) : Parcelable, Cloneable {
    init {

        sWxSymbolsMap.put(symbol, this)

        // Do not add the prefixed variants of the main symbol
        val firstChar = symbol.get(0)
        if (firstChar != PLUS_SIGN && firstChar != MINUS_SIGN) {
            sSymbolNames.add(symbol)
        }
    }

    override fun toString(): String {
        return description
    }

    override fun clone() = WxSymbol(symbol, drawable, description)

    companion object {
        private const val MINUS_SIGN = '-'
        private const val PLUS_SIGN = '+'

        private val sWxSymbolsMap = mutableMapOf<String, WxSymbol>()
        private val sSymbolNames = mutableListOf<String>()

        fun get(name: String, intensity: String): WxSymbol? {
            var symbol: WxSymbol? = null
            try {
                val key = intensity + name
                if (sWxSymbolsMap.containsKey(key)) {
                    symbol = sWxSymbolsMap[key]?.clone() as WxSymbol
                } else if (sWxSymbolsMap.containsKey(name)) {
                    symbol = sWxSymbolsMap[name]?.clone() as WxSymbol
                } else {
                    symbol = WxSymbol(key, 0, key)
                }
            } catch (_: CloneNotSupportedException) {
            }
            return symbol
        }

        init {
            WxSymbol("SHRA", R.drawable.sh, "Rainshowers")
            WxSymbol("+SHRA", R.drawable.sh, "Heavy rainshowers")
            WxSymbol("-SHRA", R.drawable.l_sh, "Light rainshowers")

            WxSymbol("SHGS", R.drawable.gs, "Small hail showers")
            WxSymbol("+SHGS", R.drawable.gs, "Heavy small hail showers")
            WxSymbol("-SHGS", R.drawable.l_gs, "Light small hail showers")

            WxSymbol("SHSN", R.drawable.shsn, "Snowshowers")
            WxSymbol("+SHSN", R.drawable.shsn, "Heavy snowshowers")
            WxSymbol("-SHSN", R.drawable.l_shsn, "Light snowshowers")

            WxSymbol("SHGR", R.drawable.gr, "Hail showers")
            WxSymbol("+SHGR", R.drawable.gr, "Heavy hail showers")
            WxSymbol("-SHGR", R.drawable.l_gr, "Light hail showers")

            WxSymbol("SHPL", R.drawable.pl, "Ice pellet showers")
            WxSymbol("SHPE", R.drawable.pl, "Ice pellet showers")

            WxSymbol("SH", R.drawable.sh, "Showers")
            WxSymbol("+SH", R.drawable.sh, "Heavy showers")
            WxSymbol("-SH", R.drawable.l_sh, "Light showers")

            WxSymbol("RA", R.drawable.ra, "Rain")
            WxSymbol("+RA", R.drawable.h_ra, "Heavy rain")
            WxSymbol("-RA", R.drawable.l_ra, "Light rain")

            WxSymbol("SN", R.drawable.sn, "Snow")
            WxSymbol("+SN", R.drawable.h_sn, "Heavy snow")
            WxSymbol("-SN", R.drawable.l_sn, "Light snow")

            WxSymbol("DZ", R.drawable.dz, "Drizzle")
            WxSymbol("+DZ", R.drawable.h_dz, "Heavy drizzle")
            WxSymbol("-DZ", R.drawable.l_dz, "Light drizzle")

            WxSymbol("TSRA", R.drawable.tsra, "Thunderstorm with rain")
            WxSymbol("+TSRA", R.drawable.h_tsra, "Heavy thunderstorm with rain")

            WxSymbol("TSPL", R.drawable.tsra, "Thunderstorm with ice pellets")
            WxSymbol("+TSPL", R.drawable.h_tsra, "Heavy thunderstorm with ice pellets")

            WxSymbol("TSSN", R.drawable.tsra, "Thunderstorm with snow")
            WxSymbol("+TSSN", R.drawable.h_tsra, "Heavy thunderstorm with snow")

            WxSymbol("TSGS", R.drawable.tsgr, "Thunderstorm with small hail")
            WxSymbol("+TSGS", R.drawable.h_tsgr, "Heavy thunderstorm with small hail")

            WxSymbol("TSGR", R.drawable.tsgr, "Thunderstorm with hail")
            WxSymbol("+TSGR", R.drawable.h_tsgr, "Heavy thunderstorm with hail")

            WxSymbol("FZDZ", R.drawable.fzdz, "Freezing drizzle")
            WxSymbol("+FZDZ", R.drawable.fzdz, "Heavy freezing drizzle")
            WxSymbol("-FZDZ", R.drawable.l_fzdz, "Light freezing drizzle")

            WxSymbol("FZRA", R.drawable.fzra, "Freezing rain")
            WxSymbol("+FZRA", R.drawable.fzra, "Heavy freezing rain")
            WxSymbol("-FZRA", R.drawable.l_fzra, "Light freezing rain")

            WxSymbol("PL", R.drawable.pl, "Ice pellets")
            WxSymbol("PE", R.drawable.pl, "Ice pellets")

            WxSymbol("GS", R.drawable.gs, "Small hail")
            WxSymbol("+GS", R.drawable.gs, "Heavy small hail")
            WxSymbol("-GS", R.drawable.l_gs, "Light small hail")

            WxSymbol("GR", R.drawable.gr, "Hail")
            WxSymbol("+GR", R.drawable.gr, "Heavy hail")
            WxSymbol("-GR", R.drawable.l_gr, "Light hail")

            WxSymbol("DS", R.drawable.ss, "Duststorm")
            WxSymbol("+DS", R.drawable.h_ss, "Heavy duststorm")

            WxSymbol("SS", R.drawable.ss, "Sandstorm")
            WxSymbol("+SS", R.drawable.h_ss, "Heavy sandstorm")

            WxSymbol("FC", R.drawable.fc, "Funnel clouds")
            WxSymbol("+FC", R.drawable.fc, "Tornado")

            WxSymbol("BCFG", R.drawable.bcfg, "Patches of fog")

            WxSymbol("PRFG", R.drawable.prfg, "Partial fog")

            WxSymbol("MIFG", R.drawable.mifg, "Shallow fog")

            WxSymbol("BLDU", R.drawable.sa, "Blowing dust")

            WxSymbol("BLSA", R.drawable.sa, "Blowing sand")

            WxSymbol("BLSN", R.drawable.blsn, "Blowing snow")

            WxSymbol("BLPY", R.drawable.sa, "Blowing spray")

            WxSymbol("VCBR", R.drawable.vcbr, "Mist in the vicinity")

            WxSymbol("VCTS", R.drawable.vcts, "Thunderstorm in the vicinity")

            WxSymbol("DRDU", R.drawable.ss, "Low drifting dust")

            WxSymbol("DRSA", R.drawable.ss, "Low drifting sand")

            WxSymbol("DRSN", R.drawable.drsn, "Low drifting snow")

            WxSymbol("FZFG", R.drawable.fzfg, "Freezing fog")

            WxSymbol("VCFG", R.drawable.vcfg, "Fog in the vicinity")

            WxSymbol("VCFC", R.drawable.fc, "Funel clouds in the vicinity")

            WxSymbol("VCSS", R.drawable.vcss, "Sandstorm in the vicinity")

            WxSymbol("VCDS", R.drawable.vcss, "Duststorm in the vicinity")

            WxSymbol("VCSH", R.drawable.vcsh, "Showers in the vicinity")

            WxSymbol("VCPO", R.drawable.po, "Dust/sand whirls in the vicinity")

            WxSymbol("VCBLDU", R.drawable.sa, "Blowing dust in the vicinity")

            WxSymbol("VCBLSA", R.drawable.sa, "Blowing sand in the vicinity")

            WxSymbol("VCBLSN", R.drawable.blsn, "Blowing snow in the vicinity")

            WxSymbol("BR", R.drawable.br, "Mist")

            WxSymbol("DU", R.drawable.du, "Widespread dust")

            WxSymbol("FG", R.drawable.fg, "Fog")

            WxSymbol("FU", R.drawable.fu, "Smoke")

            WxSymbol("HZ", R.drawable.hz, "Haze")

            WxSymbol("IC", R.drawable.ic, "Ice crystals")

            WxSymbol("UP", R.drawable.up, "Unknown precipitation")

            WxSymbol("PO", R.drawable.po, "Dust/sand whirls")

            WxSymbol("SG", R.drawable.sg, "Snow grains")

            WxSymbol("SQ", R.drawable.sq, "Squalls")

            WxSymbol("SA", R.drawable.sa, "Sand")

            WxSymbol("TS", R.drawable.ts, "Thunderstorm")

            WxSymbol("VA", R.drawable.fu, "Volcanic ash")

            WxSymbol("NSW", 0, "No significant weather")
        }

        fun parseWxSymbols(wxList: MutableList<WxSymbol>, wxString: String) {
            val groups = wxString.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (group in groups) {
                var offset = 0
                var intensity = ""
                val firstChar = group.get(offset)
                if (firstChar == PLUS_SIGN || firstChar == MINUS_SIGN) {
                    intensity = firstChar.toString()
                    ++offset
                }
                while (offset < group.length) {
                    var wx: WxSymbol? = null
                    for (name in sSymbolNames) {
                        if (group.substring(offset).startsWith(name)) {
                            wx = get(name, intensity)
                            if (wx != null) {
                                wxList.add(wx)
                            }
                            intensity = ""
                            offset += name.length
                            break
                        }
                    }

                    if (wx == null) {
                        // No match found, skip to next character and try again
                        ++offset
                    }
                }
            }
        }
    }
}
