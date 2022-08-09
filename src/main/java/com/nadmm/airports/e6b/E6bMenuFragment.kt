/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R
import java.util.*

class E6bMenuFragment : ListMenuFragment() {
    companion object {
        private val mDispatchMap: HashMap<Int, Class<*>> = HashMap<Int, Class<*>>()

        init {
            mDispatchMap[R.id.E6B_WIND_CALCS] = E6bMenuFragment::class.java
            mDispatchMap[R.id.E6B_CROSS_WIND] = CrossWindFragment::class.java
            mDispatchMap[R.id.E6B_WIND_TRIANGLE_WIND] = WindTriangleFragment::class.java
            mDispatchMap[R.id.E6B_WIND_TRIANGLE_HDG_GS] = WindTriangleFragment::class.java
            mDispatchMap[R.id.E6B_WIND_TRIANGLE_CRS_GS] = WindTriangleFragment::class.java
            mDispatchMap[R.id.E6B_ALTIMETRY] = E6bMenuFragment::class.java
            mDispatchMap[R.id.E6B_ALTIMETRY_ISA] = IsaFragment::class.java
            mDispatchMap[R.id.E6B_ALTIMETRY_ALTITUDES] = AltitudesFragment::class.java
            mDispatchMap[R.id.E6B_ALTIMETRY_TAS] = TrueAirspeedFragment::class.java
            mDispatchMap[R.id.E6B_ALTIMETRY_OAT] = OutsideAirTemperatureFragment::class.java
            mDispatchMap[R.id.E6B_ALTIMETRY_MACH] = MachNumberFragment::class.java
            mDispatchMap[R.id.E6B_ALTIMETRY_TA] = TrueAltitudeFragment::class.java
            mDispatchMap[R.id.E6B_TIME_SPEED_DISTANCE] = E6bMenuFragment::class.java
            mDispatchMap[R.id.E6B_TSD_TIME] = TimeSpeedDistanceFragment::class.java
            mDispatchMap[R.id.E6B_TSD_SPEED] = TimeSpeedDistanceFragment::class.java
            mDispatchMap[R.id.E6B_TSD_DISTANCE] = TimeSpeedDistanceFragment::class.java
            mDispatchMap[R.id.E6B_FUEL_CALCS] = E6bMenuFragment::class.java
            mDispatchMap[R.id.E6B_FUEL_ENDURANCE] = FuelCalcsFragment::class.java
            mDispatchMap[R.id.E6B_FUEL_BURN_RATE] = FuelCalcsFragment::class.java
            mDispatchMap[R.id.E6B_FUEL_TOTAL_BURNED] = FuelCalcsFragment::class.java
            mDispatchMap[R.id.E6B_FUEL_SPECIFIC_RANGE] = SpecificRangeFragment::class.java
            mDispatchMap[R.id.E6B_FUEL_WEIGHTS] = FuelWeightFragment::class.java
            mDispatchMap[R.id.E6B_CLIMB_DESCENT] = E6bMenuFragment::class.java
            mDispatchMap[R.id.E6B_CLIMB_DESCENT_REQCLIMB] = ClimbRateFragment::class.java
            mDispatchMap[R.id.E6B_CLIMB_DESCENT_REQDSCNT] = DescentRateFragment::class.java
            mDispatchMap[R.id.E6B_CLIMB_DESCENT_TOPDSCNT] = TopOfDescentFragment::class.java
            mDispatchMap[R.id.E6B_UNIT_CONVERSIONS] = UnitConvertFragment::class.java
        }
    }

    override fun getItemFragmentClass(id: Int): Class<*>? {
        return mDispatchMap[id]
    }

    override fun getMenuCursor(id: Int): Cursor {
        return E6bMenuCursor(id)
    }

    class E6bMenuCursor(id: Int) : ListMenuCursor(id) {
        override fun populateMenuItems(id: Int) {
            when (id) {
                R.id.CATEGORY_MAIN -> {
                    addRow(
                        R.id.E6B_WIND_CALCS,
                        R.drawable.ic_outline_air_40,
                        "Wind Calculations",
                        "Cross wind, head wind and wind triangle"
                    )
                    addRow(
                        R.id.E6B_TIME_SPEED_DISTANCE,
                        R.drawable.ic_outline_access_time_40,
                        "Time, Speed and Distance",
                        "Solve for time, speed and distance"
                    )
                    addRow(
                        R.id.E6B_FUEL_CALCS,
                        R.drawable.ic_outline_local_gas_station_40,
                        "Fuel Calculations",
                        "Find endurance, burn rate and fuel required"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY,
                        R.drawable.ic_outline_speed_40,
                        "Altimetry",
                        "Altimeter, altitude and the standard atmosphere"
                    )
                    addRow(
                        R.id.E6B_CLIMB_DESCENT,
                        R.drawable.ic_outline_flight_takeoff_40,
                        "Climb and Descent",
                        "Climb and descent rates"
                    )
                    addRow(
                        R.id.E6B_UNIT_CONVERSIONS,
                        R.drawable.ic_outline_measure_40,
                        "Unit Conversions",
                        "Convert between units of measurement"
                    )
                }
                R.id.E6B_WIND_CALCS -> {
                    addRow(
                        R.id.E6B_CROSS_WIND,
                        "Crosswind and Headwind",
                        "Cross wind and head wind for a runway"
                    )
                    addRow(
                        R.id.E6B_WIND_TRIANGLE_WIND,
                        "Find Wind Speed and Direction",
                        "WS and WDIR using Wind Triangle"
                    )
                    addRow(
                        R.id.E6B_WIND_TRIANGLE_HDG_GS,
                        "Find Heading and Ground Speed",
                        "HDG and GS using Wind Triangle"
                    )
                    addRow(
                        R.id.E6B_WIND_TRIANGLE_CRS_GS,
                        "Find Course and Ground Speed",
                        "CRS and GS using Wind Triangle"
                    )
                }
                R.id.E6B_ALTIMETRY -> {
                    addRow(
                        R.id.E6B_ALTIMETRY_ISA,
                        "Standard Atmosphere",
                        "ISA 1976 model"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_ALTITUDES,
                        "Pressure & Density Altitude",
                        "Altimeter and temperature"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_TAS,
                        "True Airspeed",
                        "True airspeed at a given altitude and temperature"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_MACH,
                        "Mach Number",
                        "Supersonic or subsonic"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_OAT,
                        "Outside Air Temperature",
                        "Also known as True Air Temperature"
                    )
                    addRow(
                        R.id.E6B_ALTIMETRY_TA,
                        "True Altitude",
                        "True altitude at a given altitude and temperature"
                    )
                }
                R.id.E6B_TIME_SPEED_DISTANCE -> {
                    addRow(
                        R.id.E6B_TSD_TIME,
                        "Find Flight Time",
                        "Find flight time based on speed and distance"
                    )
                    addRow(
                        R.id.E6B_TSD_SPEED,
                        "Find Ground Speed",
                        "Find ground speed based on time and distance"
                    )
                    addRow(
                        R.id.E6B_TSD_DISTANCE,
                        "Find Distance Flown",
                        "Find distance flown based on time and speed"
                    )
                }
                R.id.E6B_FUEL_CALCS -> {
                    addRow(
                        R.id.E6B_FUEL_ENDURANCE,
                        "Endurance",
                        "Find endurance in flight"
                    )
                    addRow(
                        R.id.E6B_FUEL_TOTAL_BURNED,
                        "Total Fuel Burn",
                        "Find total fuel burned during the flight"
                    )
                    addRow(
                        R.id.E6B_FUEL_BURN_RATE,
                        "Fuel Burn Rate",
                        "Find rate of fuel burn during the flight"
                    )
                    addRow(
                        R.id.E6B_FUEL_SPECIFIC_RANGE,
                        "Specific Range",
                        "Find range based on ground speed and fuel burn"
                    )
                    addRow(
                        R.id.E6B_FUEL_WEIGHTS,
                        "Fuel Weight",
                        "Find weight in Pounds from Gallons"
                    )
                }
                R.id.E6B_CLIMB_DESCENT -> {
                    addRow(
                        R.id.E6B_CLIMB_DESCENT_REQCLIMB,
                        "Required Rate of Climb",
                        "Minimum rate of climb on a departure procedures"
                    )
                    addRow(
                        R.id.E6B_CLIMB_DESCENT_REQDSCNT,
                        "Required Rate of Descent",
                        "Rate of descent or climb to cross a fix"
                    )
                    addRow(
                        R.id.E6B_CLIMB_DESCENT_TOPDSCNT,
                        "Top of Descent",
                        "Calculate how far out to begin the descent"
                    )
                }
            }
        }

        private fun addRow(id: Int, icon: Int, title: String, subtitle: String) {
            newRow().add(id)
                .add(icon)
                .add(title)
                .add(subtitle)
        }

        private fun addRow(id: Int, title: String, subtitle: String) {
            addRow(id, 0, title, subtitle)
        }
    }
}