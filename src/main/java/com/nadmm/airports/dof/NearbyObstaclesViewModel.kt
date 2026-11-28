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
 * Shares the nearby obstacles with the Map and List tabs: the screen finds
 * them, and the tabs show them. Until it can, [message] says why, such as no
 * location permission.
 */
class NearbyObstaclesViewModel : ViewModel() {
    private val _nearby = MutableStateFlow<NearbyObstacles?>(null)
    val nearby: StateFlow<NearbyObstacles?> = _nearby.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun publish(nearby: NearbyObstacles) {
        _message.value = null
        _nearby.value = nearby
    }

    fun publishMessage(message: String) {
        _message.value = message
    }
}
