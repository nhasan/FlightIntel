/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.dof

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The obstacles found near a location, within [radiusNm]. */
class NearbyObstacles(
    val location: Location,
    val radiusNm: Int,
    val obstacles: List<DofListDataModel>
)

/**
 * Shares the nearby obstacles between the List and Map tabs: the list finds
 * them and the map draws the same ones.
 */
class NearbyObstaclesViewModel : ViewModel() {
    private val _nearby = MutableStateFlow<NearbyObstacles?>(null)
    val nearby: StateFlow<NearbyObstacles?> = _nearby.asStateFlow()

    fun publish(nearby: NearbyObstacles) {
        _nearby.value = nearby
    }
}
