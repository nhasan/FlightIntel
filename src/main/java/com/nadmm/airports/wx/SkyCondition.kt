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

import com.nadmm.airports.R
import com.nadmm.airports.utils.FormatUtils.formatFeetAgl
import java.io.Serializable

abstract class SkyCondition(val skyCover: String, val cloudBaseAGL: Int) : Serializable {

    abstract val drawable: Int

    companion object {

        fun formatCloudBase(cloudBaseAGL: Int): String {
            return if (cloudBaseAGL > 0) {
                " at ${formatFeetAgl(cloudBaseAGL.toFloat())}"
            } else {
                ""
            }
        }

        fun create(skyCover: String, cloudBaseAGL: Int = 0): SkyCondition {
            var sky: SkyCondition?

            when (skyCover) {
                "CLR" -> {
                    sky =  object : SkyCondition(skyCover, 0) {

                        override fun toString(): String {
                            return "Sky clear below 12,000 ft AGL"
                        }

                        override val drawable: Int
                            get() = R.drawable.clr
                    }
                }
                "SKC" -> {
                    sky = object : SkyCondition(skyCover, 0) {

                        override fun toString(): String {
                            return "Sky clear"
                        }

                        override val drawable: Int
                            get() = R.drawable.skc
                    }
                }
                "FEW" -> {
                    sky = object : SkyCondition(skyCover, cloudBaseAGL) {

                        override fun toString(): String {
                            return "Few clouds".plus(formatCloudBase(cloudBaseAGL))
                        }

                        override val drawable: Int
                            get() = R.drawable.few
                    }
                }
                "SCT" -> {
                    sky = object : SkyCondition(skyCover, cloudBaseAGL) {

                        override fun toString(): String {
                            return "Scattered clouds".plus(formatCloudBase(cloudBaseAGL))
                        }

                        override val drawable: Int
                            get() = R.drawable.sct
                    }
                }
                "BKN" -> {
                    sky = object : SkyCondition(skyCover, cloudBaseAGL) {

                        override fun toString(): String {
                            return "Broken clouds".plus(formatCloudBase(cloudBaseAGL))
                        }

                        override val drawable: Int
                            get() = R.drawable.bkn
                    }
                }
                "OVC" -> {
                    sky = object : SkyCondition(skyCover, cloudBaseAGL) {

                        override fun toString(): String {
                            return "Overcast clouds".plus(formatCloudBase(cloudBaseAGL))
                        }

                        override val drawable: Int
                            get() = R.drawable.ovc
                    }
                }
                "OVX" -> {
                    sky = object : SkyCondition(skyCover, 0) {

                        override fun toString(): String {
                            return "Indefinite ceiling"
                        }

                        override val drawable: Int
                            get() = R.drawable.ovx
                    }
                }
                "NSC" -> {
                    sky = object : SkyCondition(skyCover, 0) {

                        override fun toString(): String {
                            return "No significant clouds"
                        }

                        override val drawable: Int
                            get() = R.drawable.skm
                    }
                }
                else -> {
                    sky = object : SkyCondition(skyCover, 0) {

                        override fun toString(): String {
                            return "Sky condition is missing"
                        }

                        override val drawable: Int
                            get() = R.drawable.skm
                    }
                }
            }

            return sky
        }
    }
}
