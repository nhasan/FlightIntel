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
package com.nadmm.airports.tfr

import com.nadmm.airports.tfr.TfrList.Altitude
import com.nadmm.airports.tfr.TfrList.AltitudeType
import com.nadmm.airports.tfr.TfrList.TfrShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Decides where to label the areas of a TFR on a map, and what to write. */
object TfrMapLabels {
    /**
     * A label position. [shapes] are the areas it describes: more than one when
     * areas share an outline and differ only in altitude.
     */
    class Label(val latitude: Double, val longitude: Double, val shapes: List<TfrShape>)

    private const val GRID_SIZE = 24
    private const val REFINE_STEPS = 3

    fun place(shapes: List<TfrShape>): List<Label> {
        if (shapes.isEmpty()) return emptyList()
        // Small enough areas to treat lat/lon as planar, once longitude is
        // scaled to the same length as latitude
        val scale = cos(shapes.flatMap { it.latitudes.asList() }.average() * PI / 180)
        val polygons = shapes.map { Polygon(it, scale) }.filter { it.area > 0 }

        // Areas with the same outline share a label
        val groups = ArrayList<MutableList<Polygon>>()
        for (polygon in polygons.sortedByDescending { it.area }) {
            groups.firstOrNull { it[0].sameOutline(polygon) }?.add(polygon)
                ?: groups.add(mutableListOf(polygon))
        }

        return groups.map { group ->
            val outline = group[0]
            // Smaller areas inside this one cover part of it, as with
            // concentric circles, so label the part that is left
            val holes = groups.map { it[0] }.filter { it !== outline && outline.encloses(it) }
            val (x, y) = labelPoint(outline, holes)
            Label(y, x / scale, group.map { it.shape })
        }
    }

    /** The size of an area, for comparing areas with each other. */
    fun area(shape: TfrShape): Double =
        Polygon(shape, cos(shape.latitudes.average() * PI / 180)).area

    /**
     * True when [inner] lies inside [outer]: smaller, with its center inside.
     * The same test decides which areas a label must keep clear of.
     */
    fun encloses(outer: TfrShape, inner: TfrShape): Boolean {
        if (outer === inner) return false
        val scale = cos((outer.latitudes.average() + inner.latitudes.average()) / 2 * PI / 180)
        val o = Polygon(outer, scale)
        val i = Polygon(inner, scale)
        return o.encloses(i)
    }

    /**
     * The point of [outline], outside [holes], farthest from any of their edges:
     * the roomiest spot for a label. A coarse grid search, refined around the
     * best point found.
     */
    private fun labelPoint(outline: Polygon, holes: List<Polygon>): Pair<Double, Double> {
        var minX = outline.minX
        var maxX = outline.maxX
        var minY = outline.minY
        var maxY = outline.maxY
        var bestX = outline.centroidX
        var bestY = outline.centroidY
        var bestDistance = -1.0
        repeat(REFINE_STEPS) {
            val stepX = (maxX - minX) / GRID_SIZE
            val stepY = (maxY - minY) / GRID_SIZE
            // North first, so a tie on a ring puts the label at the top
            for (row in GRID_SIZE - 1 downTo 0) {
                val y = minY + (row + 0.5) * stepY
                for (col in 0 until GRID_SIZE) {
                    val x = minX + (col + 0.5) * stepX
                    if (!outline.contains(x, y) || holes.any { it.contains(x, y) }) continue
                    var distance = outline.distance(x, y)
                    for (hole in holes) distance = min(distance, hole.distance(x, y))
                    if (distance > bestDistance) {
                        bestDistance = distance
                        bestX = x
                        bestY = y
                    }
                }
            }
            if (bestDistance < 0) return bestX to bestY
            minX = bestX - stepX
            maxX = bestX + stepX
            minY = bestY - stepY
            maxY = bestY + stepY
        }
        return bestX to bestY
    }

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /** The daily window of an area active only part of each day, else null. */
    fun dailyWindow(shape: TfrShape): Pair<Long, Long>? = dailyWindow(shape.dailyStart, shape.dailyEnd)

    /** The daily window from [start] to [end] on the first day, else null. */
    fun dailyWindow(start: Long?, end: Long?): Pair<Long, Long>? {
        if (start == null || end == null) return null
        return (start to end).takeIf { end > start && end - start < DAY_MILLIS }
    }

    /**
     * True when the area has effective dates of its own, other than those of
     * the whole TFR, so its label needs to show them.
     */
    fun hasOwnDates(shape: TfrShape, tfr: TfrList.Tfr): Boolean {
        if (shape.activeTime == null && shape.expireTime == null) return false
        val tfrActive = tfr.activeTime.takeIf { it < Long.MAX_VALUE }
        val tfrExpire = tfr.expireTime.takeIf { it < Long.MAX_VALUE }
        return shape.activeTime != tfrActive || shape.expireTime != tfrExpire
    }

    /** The vertical limits of an area, such as "SFC–17,999 ft MSL", or null if unknown. */
    fun formatAltitudeRange(shape: TfrShape): String? {
        var lower = shape.minAltitude?.let { formatAltitude(it, false) }
        val upper = shape.maxAltitude?.let { formatAltitude(it, true) }
        return when {
            lower != null && upper != null -> {
                // "2,100–2,600 ft MSL" rather than "2,100 ft MSL–2,600 ft MSL"
                for (type in AltitudeType.entries) {
                    val suffix = " ft $type"
                    if (lower!!.endsWith(suffix) && upper.endsWith(suffix)) {
                        lower = lower.removeSuffix(suffix)
                    }
                }
                "$lower–$upper"
            }
            upper != null -> "Up to $upper"
            lower != null -> "$lower and above"
            else -> null
        }
    }

    private fun formatAltitude(altitude: Altitude, upper: Boolean): String = when {
        altitude.feet == 0 -> "SFC"
        upper && altitude.feet >= 91000 -> "UNL"
        altitude.type == AltitudeType.MSL && altitude.feet >= 18000 -> "FL${altitude.feet / 100}"
        else -> "%,d ft %s".format(altitude.feet, altitude.type)
    }

    /** An area in planar coordinates: x is scaled longitude, y is latitude. */
    private class Polygon(val shape: TfrShape, scale: Double) {
        val x = DoubleArray(shape.longitudes.size) { shape.longitudes[it] * scale }
        val y = shape.latitudes
        val area: Double
        val centroidX: Double
        val centroidY: Double
        val minX = x.min()
        val maxX = x.max()
        val minY = y.min()
        val maxY = y.max()

        init {
            var a = 0.0
            var cx = 0.0
            var cy = 0.0
            for (i in x.indices) {
                val j = (i + 1) % x.size
                val cross = x[i] * y[j] - x[j] * y[i]
                a += cross
                cx += (x[i] + x[j]) * cross
                cy += (y[i] + y[j]) * cross
            }
            a /= 2
            area = abs(a)
            centroidX = if (area > 0) cx / (6 * a) else x.average()
            centroidY = if (area > 0) cy / (6 * a) else y.average()
        }

        fun encloses(other: Polygon) =
            other.area < area && contains(other.centroidX, other.centroidY)

        /** Even-odd ray casting. */
        fun contains(px: Double, py: Double): Boolean {
            var inside = false
            var j = x.size - 1
            for (i in x.indices) {
                if ((y[i] > py) != (y[j] > py)
                    && px < (x[j] - x[i]) * (py - y[i]) / (y[j] - y[i]) + x[i]
                ) {
                    inside = !inside
                }
                j = i
            }
            return inside
        }

        /** Distance from a point to the nearest edge. */
        fun distance(px: Double, py: Double): Double {
            var best = Double.MAX_VALUE
            var j = x.size - 1
            for (i in x.indices) {
                best = min(best, segmentDistance(px, py, x[j], y[j], x[i], y[i]))
                j = i
            }
            return best
        }

        /** Same size and place, within a small tolerance. */
        fun sameOutline(other: Polygon): Boolean {
            val size = sqrt(max(area, other.area))
            return abs(area - other.area) <= 0.01 * max(area, other.area)
                && hypot(centroidX - other.centroidX, centroidY - other.centroidY) <= 0.02 * size
        }

        private fun segmentDistance(
            px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double
        ): Double {
            val dx = bx - ax
            val dy = by - ay
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared == 0.0) 0.0
            else ((px - ax) * dx + (py - ay) * dy) / lengthSquared
            val clamped = t.coerceIn(0.0, 1.0)
            return hypot(px - (ax + clamped * dx), py - (ay + clamped * dy))
        }
    }
}
