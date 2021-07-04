/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2021 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.e6b

import android.database.Cursor
import android.provider.BaseColumns
import android.view.View
import android.widget.ListView
import androidx.collection.LongSparseArray
import com.nadmm.airports.Application
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R
import com.nadmm.airports.utils.UiUtils

class E6bMenuFragment : ListMenuFragment() {
    companion object {
        private val mDispatchMap: LongSparseArray<Class<*>> = LongSparseArray(32)

        init {
            mDispatchMap.put(R.id.E6B_WIND_CALCS.toLong(), E6bMenuFragment::class.java)
            mDispatchMap.put(R.id.E6B_CROSS_WIND.toLong(), CrossWindFragment::class.java)
            mDispatchMap.put(R.id.E6B_WIND_TRIANGLE_WIND.toLong(), WindTriangleFragment::class.java)
            mDispatchMap.put(
                R.id.E6B_WIND_TRIANGLE_HDG_GS.toLong(),
                WindTriangleFragment::class.java
            )
            mDispatchMap.put(
                R.id.E6B_WIND_TRIANGLE_CRS_GS.toLong(),
                WindTriangleFragment::class.java
            )
            mDispatchMap.put(R.id.E6B_ALTIMETRY.toLong(), E6bMenuFragment::class.java)
            mDispatchMap.put(R.id.E6B_ALTIMETRY_ISA.toLong(), IsaFragment::class.java)
            mDispatchMap.put(R.id.E6B_ALTIMETRY_ALTITUDES.toLong(), AltitudesFragment::class.java)
            mDispatchMap.put(R.id.E6B_ALTIMETRY_TAS.toLong(), TrueAirspeedFragment::class.java)
            mDispatchMap.put(
                R.id.E6B_ALTIMETRY_OAT.toLong(),
                OutsideAirTemperatureFragment::class.java
            )
            mDispatchMap.put(R.id.E6B_ALTIMETRY_MACH.toLong(), MachNumberFragment::class.java)
            mDispatchMap.put(R.id.E6B_ALTIMETRY_TA.toLong(), TrueAltitudeFragment::class.java)
            mDispatchMap.put(R.id.E6B_TIME_SPEED_DISTANCE.toLong(), E6bMenuFragment::class.java)
            mDispatchMap.put(R.id.E6B_TSD_TIME.toLong(), TimeSpeedDistanceFragment::class.java)
            mDispatchMap.put(R.id.E6B_TSD_SPEED.toLong(), TimeSpeedDistanceFragment::class.java)
            mDispatchMap.put(R.id.E6B_TSD_DISTANCE.toLong(), TimeSpeedDistanceFragment::class.java)
            mDispatchMap.put(R.id.E6B_FUEL_CALCS.toLong(), E6bMenuFragment::class.java)
            mDispatchMap.put(R.id.E6B_FUEL_ENDURANCE.toLong(), FuelCalcsFragment::class.java)
            mDispatchMap.put(R.id.E6B_FUEL_BURN_RATE.toLong(), FuelCalcsFragment::class.java)
            mDispatchMap.put(R.id.E6B_FUEL_TOTAL_BURNED.toLong(), FuelCalcsFragment::class.java)
            mDispatchMap.put(
                R.id.E6B_FUEL_SPECIFIC_RANGE.toLong(),
                SpecificRangeFragment::class.java
            )
            mDispatchMap.put(R.id.E6B_FUEL_WEIGHTS.toLong(), FuelWeightFragment::class.java)
            mDispatchMap.put(R.id.E6B_CLIMB_DESCENT.toLong(), E6bMenuFragment::class.java)
            mDispatchMap.put(
                R.id.E6B_CLIMB_DESCENT_REQCLIMB.toLong(),
                ClimbRateFragment::class.java
            )
            mDispatchMap.put(
                R.id.E6B_CLIMB_DESCENT_REQDSCNT.toLong(),
                DescentRateFragment::class.java
            )
            mDispatchMap.put(
                R.id.E6B_CLIMB_DESCENT_TOPDSCNT.toLong(),
                TopOfDescentFragment::class.java
            )
            mDispatchMap.put(R.id.E6B_UNIT_CONVERSIONS.toLong(), UnitConvertFragment::class.java)
        }
    }

    override fun getItemFragmentClass(id: Long): Class<*> {
        return mDispatchMap[id]!!
    }

    override fun getMenuCursor(id: Long): Cursor {
        return E6bMenuCursor(id)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        val c = listAdapter!!.getItem(position) as Cursor
        val id = c.getLong(c.getColumnIndex(BaseColumns._ID))
        val clss = getItemFragmentClass(id)
        if (clss == E6bMenuFragment::class.java || Application.sDonationDone) {
            super.onListItemClick(l, v, position)
        } else {
            UiUtils.showToast(activity, "This function is only available after a donation")
        }
    }

    class E6bMenuCursor(id: Long) : ListMenuCursor(id) {
        override fun populateMenuItems(id: Long) {
            when (id) {
                R.id.CATEGORY_MAIN.toLong() -> {
                    addRow(
                        R.id.E6B_WIND_CALCS.toLong(),
                        "Wind Calculations",
                        "Cross wind, head wind and wind triangle"
                    )
                    addRow(
                        R.id.E6B_TIME_SPEED_DISTANCE.toLong(),
                        "Time, Speed and Distance",
                        "Solve for time, speed and distance"
                    )
                    addRow(
                        R.id.E6B_FUEL_CALCS.toLong(),
                        "Fuel Calculations",
                        "Find endurance, burn rate and fuel required"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY.toLong(),
                        "Altimetry",
                        "Altimeter, altitude and the standard atmosphere"
                    )
                    addRow(
                        R.id.E6B_CLIMB_DESCENT.toLong(),
                        "Climb and Descent",
                        "Climb and descent rates"
                    )
                    addRow(
                        R.id.E6B_UNIT_CONVERSIONS.toLong(),
                        "Unit Conversions",
                        "Convert between units of measurement"
                    )
                }
                R.id.E6B_WIND_CALCS.toLong() -> {
                    addRow(
                        R.id.E6B_CROSS_WIND.toLong(),
                        "Crosswind and Headwind",
                        "Cross wind and head wind for a runway"
                    )
                    addRow(
                        R.id.E6B_WIND_TRIANGLE_WIND.toLong(),
                        "Find Wind Speed and Direction",
                        "WS and WDIR using Wind Triangle"
                    )
                    addRow(
                        R.id.E6B_WIND_TRIANGLE_HDG_GS.toLong(),
                        "Find Heading and Ground Speed",
                        "HDG and GS using Wind Triangle"
                    )
                    addRow(
                        R.id.E6B_WIND_TRIANGLE_CRS_GS.toLong(),
                        "Find Course and Ground Speed",
                        "CRS and GS using Wind Triangle"
                    )
                }
                R.id.E6B_ALTIMETRY.toLong() -> {
                    addRow(
                        R.id.E6B_ALTIMETRY_ISA.toLong(),
                        "Standard Atmosphere",
                        "International Standard Atmosphere (ISA 1976 model)"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_ALTITUDES.toLong(),
                        "Pressure & Density Altitude",
                        "Altimeter and temperature"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_TAS.toLong(),
                        "True Airspeed",
                        "True airspeed at a given altitude and temperature"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_MACH.toLong(),
                        "Mach Number",
                        "Supersonic or subsonic"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_OAT.toLong(),
                        "Outside Air Temperature",
                        "Also known as True Air Temperature"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_TA.toLong(),
                        "True Altitude",
                        "True altitude at a given altitude and temperature"
                    )
                }
                R.id.E6B_TIME_SPEED_DISTANCE.toLong() -> {
                    addRow(
                        R.id.E6B_TSD_TIME.toLong(),
                        "Find Flight Time",
                        "Find flight time based on speed and distance"
                    )
                    addRow(
                        R.id.E6B_TSD_SPEED.toLong(),
                        "Find Ground Speed",
                        "Find ground speed based on time and distance"
                    )
                    addRow(
                        R.id.E6B_TSD_DISTANCE.toLong(),
                        "Find Distance Flown",
                        "Find distance flown based on time and speed"
                    )
                }
                R.id.E6B_FUEL_CALCS.toLong() -> {
                    addRow(
                        R.id.E6B_FUEL_ENDURANCE.toLong(),
                        "Endurance",
                        "Find endurance in flight"
                    )
                    addRow(
                        R.id.E6B_FUEL_TOTAL_BURNED.toLong(),
                        "Total Fuel Burn",
                        "Find total fuel burned during the flight"
                    )
                    addRow(
                        R.id.E6B_FUEL_BURN_RATE.toLong(),
                        "Fuel Burn Rate",
                        "Find rate of fuel burn during the flight"
                    )
                    addRow(
                        R.id.E6B_FUEL_SPECIFIC_RANGE.toLong(),
                        "Specific Range",
                        "Find range based on ground speed and fuel burn"
                    )
                    addRow(
                        R.id.E6B_FUEL_WEIGHTS.toLong(),
                        "Fuel Weight",
                        "Find weight in Pounds from Gallons"
                    )
                }
                R.id.E6B_CLIMB_DESCENT.toLong() -> {
                    addRow(
                        R.id.E6B_CLIMB_DESCENT_REQCLIMB.toLong(),
                        "Required Rate of Climb",
                        "Minimum rate of climb on a departure procedures"
                    )
                    addRow(
                        R.id.E6B_CLIMB_DESCENT_REQDSCNT.toLong(),
                        "Required Rate of Descent",
                        "Rate of descent or climb to cross a fix"
                    )
                    addRow(
                        R.id.E6B_CLIMB_DESCENT_TOPDSCNT.toLong(),
                        "Top of Descent",
                        "Calculate how far out to begin the descent"
                    )
                }
            }
        }

        private fun addRow(id: Long, title: String, subtitle: String) {
            newRow().add(id)
                .add(0)
                .add(title)
                .add(subtitle)
        }
    }
}