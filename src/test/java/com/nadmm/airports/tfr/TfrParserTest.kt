package com.nadmm.airports.tfr

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TfrParserTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun parse(tfrBody: String): TfrList {
        val file = folder.newFile("tfr_list.xml")
        file.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
            <TFRList count="1" timestamp="2026-11-27T02:25:03Z">
              <TFR><NID>6/9862</NID><NAME>Test</NAME>$tfrBody</TFR>
            </TFRList>""".trimIndent()
        )
        return TfrList().also { TfrParser().parse(file, it) }
    }

    @Test
    fun parsesEachShapeAsAPolygon() {
        val list = parse(
            "<SHAPE>28.85444,-80.70528 29.125,-80.5 29.09912,-80.4551 28.85444,-80.70528</SHAPE>" +
                "<SHAPE>38.1,-77.1 38.2,-77.0 38.0,-76.9</SHAPE>"
        )
        val shapes = list.entries.single().shapes
        assertEquals(2, shapes.size)
        assertArrayEquals(doubleArrayOf(28.85444, 29.125, 29.09912, 28.85444), shapes[0].latitudes, 0.0)
        assertArrayEquals(doubleArrayOf(-80.70528, -80.5, -80.4551, -80.70528), shapes[0].longitudes, 0.0)
        assertEquals(3, shapes[1].latitudes.size)
    }

    @Test
    fun skipsMalformedPointsAndShapesWithTooFewPoints() {
        val list = parse(
            "<SHAPE>38.1,-77.1 bad 38.2 95.0,-77.0 38.2,-77.0 38.0,-76.9</SHAPE>" +
                "<SHAPE>38.1,-77.1 38.2,-77.0</SHAPE>"
        )
        val shapes = list.entries.single().shapes
        assertEquals(1, shapes.size)
        assertArrayEquals(doubleArrayOf(38.1, 38.2, 38.0), shapes[0].latitudes, 0.0)
    }

    @Test
    fun tfrWithoutShapesHasNone() {
        assertTrue(parse("").entries.single().shapes.isEmpty())
    }

    @Test
    fun parsesShapeAltitudes() {
        val shapes = parse(
            "<SHAPE minalt=\"0A\" maxalt=\"18000M\">38.1,-77.1 38.2,-77.0 38.0,-76.9</SHAPE>" +
                "<SHAPE minalt=\"bad\">38.1,-77.1 38.2,-77.0 38.0,-76.9</SHAPE>"
        ).entries.single().shapes
        assertEquals(0, shapes[0].minAltitude!!.feet)
        assertEquals(TfrList.AltitudeType.AGL, shapes[0].minAltitude!!.type)
        assertEquals(18000, shapes[0].maxAltitude!!.feet)
        assertEquals(TfrList.AltitudeType.MSL, shapes[0].maxAltitude!!.type)
        assertNull(shapes[1].minAltitude)
        assertNull(shapes[1].maxAltitude)
    }

    @Test
    fun parsesShapeTimes() {
        val shape = parse(
            "<SHAPE active=\"2026-11-24T21:45:00Z\" expires=\"2026-11-24T23:15:00Z\" " +
                "dailystart=\"bad\">38.1,-77.1 38.2,-77.0 38.0,-76.9</SHAPE>"
        ).entries.single().shapes.single()
        assertEquals(1795556700000L, shape.activeTime)
        assertEquals(1795562100000L, shape.expireTime)
        assertNull(shape.dailyStart)
        assertNull(shape.dailyEnd)
    }
}
