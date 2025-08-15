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
import com.nadmm.airports.utils.FormatUtils.formatFeetAgl
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
class SkyCondition(
    val skyCover: String,
    val cloudBaseAGL: Int,
    val drawable: Int,
    val description: String
) : Parcelable {

    override fun toString(): String {
        return description.plus(formatCloudBase())
    }

    protected fun formatCloudBase(): String {
        return if (cloudBaseAGL > 0) {
            " at ${formatFeetAgl(cloudBaseAGL.toFloat())}"
        } else {
            ""
        }
    }

    companion object {

        fun of(skyCover: String, cloudBaseAGL: Int = 0): SkyCondition {
            return when (skyCover) {
                "CLR" -> SkyCondition(skyCover, 0, R.drawable.clr, "Sky clear below 12,000 ft AGL")
                "SKC" -> SkyCondition(skyCover, 0, R.drawable.skc, "Sky clear")
                "FEW" -> SkyCondition(skyCover, cloudBaseAGL, R.drawable.few, "Few clouds")
                "SCT" -> SkyCondition(skyCover, cloudBaseAGL, R.drawable.sct, "Scattered clouds")
                "BKN" -> SkyCondition(skyCover, cloudBaseAGL, R.drawable.bkn, "Broken clouds")
                "OVC" -> SkyCondition(skyCover, cloudBaseAGL, R.drawable.ovc, "Overcast clouds")
                "OVX" -> SkyCondition(skyCover, 0, R.drawable.ovx, "Indefinite ceiling")
                "NSC" -> SkyCondition(skyCover, 0, R.drawable.skm, "No significant clouds")
                else -> SkyCondition(skyCover, 0, R.drawable.skm, "Sky condition is missing")
            }
        }
    }
}
