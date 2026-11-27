package com.nadmm.airports.wx

import com.nadmm.airports.wx.WindsAloftBuilder.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WindsAloftBuilderTest {
    private fun level(heightFt: Double, dir: Double, speed: Double, temp: Double): Level {
        val (u, v) = WindsAloftBuilder.components(dir, speed)
        return Level(heightFt, u, v, temp)
    }

    @Test
    fun convertsBetweenDirectionAndComponents() {
        for (dir in listOf(10.0, 90.0, 180.0, 270.0, 359.0)) {
            val (u, v) = WindsAloftBuilder.components(dir, 20.0)
            assertEquals(dir.toInt(), WindsAloftBuilder.direction(u, v))
        }
        // North is 360, as in forecasts, not 0
        val (u, v) = WindsAloftBuilder.components(0.0, 20.0)
        assertEquals(360, WindsAloftBuilder.direction(u, v))
    }

    @Test
    fun interpolatesBetweenLevels() {
        val levels = listOf(level(2000.0, 270.0, 20.0, 10.0), level(4000.0, 270.0, 40.0, 0.0))
        val mid = WindsAloftBuilder.interpolate(levels, 3000.0)!!
        assertEquals(5.0, mid.temperatureC, 1e-9)
        assertEquals(270, WindsAloftBuilder.direction(mid.u, mid.v))
        assertEquals(30.0, kotlin.math.hypot(mid.u, mid.v), 1e-9)
        // Outside the levels there is nothing to go on
        assertNull(WindsAloftBuilder.interpolate(levels, 1000.0))
        assertNull(WindsAloftBuilder.interpolate(levels, 5000.0))
    }

    @Test
    fun averagesDirectionsAcrossNorth() {
        val levels = listOf(level(2000.0, 350.0, 30.0, 0.0), level(4000.0, 10.0, 30.0, 0.0))
        val mid = WindsAloftBuilder.interpolate(levels, 3000.0)!!
        assertEquals(360, WindsAloftBuilder.direction(mid.u, mid.v))
    }

    @Test
    fun followsTheFbConventions() {
        val levels = listOf(
            level(0.0, 200.0, 3.0, 15.0),
            level(7000.0, 200.0, 3.0, 1.0),
            level(8000.0, 250.0, 30.0, -1.0),
            level(10000.0, 250.0, 30.0, -5.0),
            level(42000.0, 260.0, 90.0, -56.0)
        )
        // A station at 2,000 ft: no wind at 3,000 ft (within 1,500 ft)
        val rows = WindsAloftBuilder.rows(levels, 2000)
        assertEquals(WindsAloftBuilder.ALTITUDES_FT, rows.map { it.altitudeFt })
        assertFalse(rows[0].hasWind)
        assertNull(rows[0].temperatureC)
        // 6,000 ft: under 5 kt is light and variable, with a temperature
        assertTrue(rows[1].lightAndVariable)
        assertNull(rows[1].directionDeg)
        assertEquals(3, rows[1].temperatureC)
        // 9,000 ft
        assertEquals(30, rows[2].speedKt)
        assertEquals(250, rows[2].directionDeg)
        assertEquals(-3, rows[2].temperatureC)
        // A station at 5,000 ft: no temperature at 6,000 ft (within 2,500 ft)
        val high = WindsAloftBuilder.rows(levels, 5000)
        assertFalse(high[1].hasWind)
        assertNull(high[1].temperatureC)
        assertTrue(high[2].hasWind)
    }

    @Test
    fun buildsForecastsFromOpenMeteo() {
        // Two hours, with every pressure level the same apart from height
        val hpa = WindsAloftBuilder.PRESSURE_LEVELS_HPA
        val vars = hpa.withIndex().joinToString(",") { (i, p) ->
            val height = 100.0 + i * 700.0
            """
            "geopotential_height_${p}hPa":[$height,$height],
            "wind_speed_${p}hPa":[25.0,40.0],
            "wind_direction_${p}hPa":[270.0,300.0],
            "temperature_${p}hPa":[${-2.0 * i},${-2.0 * i}]
            """.trimIndent()
        }
        val json = """{"hourly":{"time":["2026-11-27T22:00","2026-11-28T04:00"],$vars}}"""
        val now = Instant.parse("2026-11-27T22:20:00Z").toEpochMilli()
        val winds = WindsAloftBuilder.build(json, "KTTN", 200, now, listOf(0, 6))

        assertEquals("KTTN", winds.stationId)
        assertEquals(2, winds.forecasts.size)
        val first = winds.forecasts[0]
        assertEquals(Instant.parse("2026-11-27T22:00:00Z").toEpochMilli(), first.timeMillis)
        assertEquals(270, first.rows[0].directionDeg)
        assertEquals(25, first.rows[0].speedKt)
        val second = winds.forecasts[1]
        assertEquals(6, second.hoursAhead)
        assertEquals(300, second.rows[0].directionDeg)
        assertEquals(40, second.rows[0].speedKt)
    }
}
