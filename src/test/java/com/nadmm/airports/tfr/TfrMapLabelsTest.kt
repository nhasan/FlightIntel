package com.nadmm.airports.tfr

import com.nadmm.airports.tfr.TfrList.Altitude
import com.nadmm.airports.tfr.TfrList.AltitudeType.AGL
import com.nadmm.airports.tfr.TfrList.AltitudeType.MSL
import com.nadmm.airports.tfr.TfrList.TfrShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class TfrMapLabelsTest {
    private val lat0 = 38.85
    private val lon0 = -77.04

    /** A circle in degrees of latitude, like the FAA's 72-point circles. */
    private fun circle(
        radius: Double, min: Altitude? = null, max: Altitude? = null,
        lat: Double = lat0, lon: Double = lon0
    ): TfrShape {
        val scale = cos(lat * PI / 180)
        val angles = (0 until 72).map { it * 2 * PI / 72 }
        return TfrShape(
            angles.map { lat + radius * sin(it) }.toDoubleArray(),
            angles.map { lon + radius * cos(it) / scale }.toDoubleArray(),
            min, max
        )
    }

    /** Distance from the circles' center in degrees of latitude. */
    private fun radiusOf(label: TfrMapLabels.Label) = hypot(
        label.latitude - lat0, (label.longitude - lon0) * cos(lat0 * PI / 180)
    )

    @Test
    fun labelsConcentricCirclesInTheirOwnRing() {
        val inner = circle(0.25, Altitude(0, AGL), Altitude(17999, MSL))
        val outer = circle(0.5, Altitude(2400, MSL), Altitude(18000, MSL))
        val labels = TfrMapLabels.place(listOf(inner, outer))
        assertEquals(2, labels.size)

        val outerLabel = labels.single { it.shapes.single() === outer }
        val innerLabel = labels.single { it.shapes.single() === inner }
        // Midway across the ring, not over the inner circle
        assertEquals(0.375, radiusOf(outerLabel), 0.03)
        assertEquals(0.0, radiusOf(innerLabel), 0.03)
    }

    @Test
    fun areasWithTheSameOutlineShareALabel() {
        val low = circle(0.3, Altitude(0, AGL), Altitude(2400, MSL))
        val high = circle(0.3, Altitude(2400, MSL), Altitude(18000, MSL))
        val label = TfrMapLabels.place(listOf(low, high)).single()
        assertEquals(setOf(low, high), label.shapes.toSet())
        assertEquals(0.0, radiusOf(label), 0.03)
    }

    @Test
    fun separateAreasGetSeparateLabels() {
        val labels = TfrMapLabels.place(
            listOf(circle(0.1), circle(0.2, lon = lon0 + 2))
        )
        assertEquals(2, labels.size)
        assertEquals(1, labels.count { it.longitude > lon0 + 1 })
    }

    @Test
    fun formatsAltitudeRanges() {
        fun range(min: Altitude?, max: Altitude?) =
            TfrMapLabels.formatAltitudeRange(TfrShape(DoubleArray(0), DoubleArray(0), min, max))

        assertEquals("SFC–17,999 ft MSL", range(Altitude(0, AGL), Altitude(17999, MSL)))
        assertEquals("2,100–2,600 ft MSL", range(Altitude(2100, MSL), Altitude(2600, MSL)))
        assertEquals("1,500 ft AGL–FL430", range(Altitude(1500, AGL), Altitude(43000, MSL)))
        assertEquals("FL320–UNL", range(Altitude(32000, MSL), Altitude(99999, MSL)))
        assertEquals("Up to 4,999 ft AGL", range(null, Altitude(4999, AGL)))
        assertNull(range(null, null))
    }

    @Test
    fun enclosesOnlySmallerAreasInside() {
        val inner = circle(0.25)
        val outer = circle(0.5)
        val apart = circle(0.1, lon = lon0 + 2)
        assertTrue(TfrMapLabels.encloses(outer, inner))
        assertFalse(TfrMapLabels.encloses(inner, outer))
        assertFalse(TfrMapLabels.encloses(outer, apart))
        assertFalse(TfrMapLabels.encloses(outer, outer))
    }
}
