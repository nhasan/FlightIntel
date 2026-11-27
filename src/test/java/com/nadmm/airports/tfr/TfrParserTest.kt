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

    @Test
    fun parsesAreaDefinitions() {
        val tfr = parse(
            """
            <AREA name="Area A" minalt="0A" maxalt="4999A" active="2026-11-24T21:45:00Z">
              <PART op="BASE">
                <CIRCLE lat="39.04861" lon="-84.66778" radius="5" navaid="Falmouth"
                    navtype="VOR/DME" ident="FLM" bearing="329" distance="29.2"/>
              </PART>
              <PART op="SUBTR">
                <POINT lat="64.23611" lon="-149.23361"/>
                <ARC dir="CCW" lat="64.28194" lon="-149.27806" radius="3"/>
                <POINT lat="bad" lon="-149.3"/>
              </PART>
            </AREA>
            <AREA name="Empty"><PART op="BASE"/></AREA>
            <REASON>Temporary flight restrictions for VIP Movement</REASON>
            <AUTHORITY>Title 14 CFR section 91.141</AUTHORITY>
            <FACILITYNAME>Indianapolis Center</FACILITYNAME>
            <REPLACED reason="Due to change in times">6/9446</REPLACED>
            <RESTRICTION>No pilots may operate an aircraft</RESTRICTION>
            <INSTRUCTION>EXC THE FLT OPS LISTED BLW:</INSTRUCTION>
            <INSTRUCTION>1. ACFT MAY NOT LOITER.</INSTRUCTION>
            """
        ).entries.single()
        assertEquals("Temporary flight restrictions for VIP Movement", tfr.reason)
        assertEquals("Title 14 CFR section 91.141", tfr.authority)
        assertEquals("Indianapolis Center", tfr.facilityName)
        assertEquals("6/9446", tfr.replacedNotamId)
        assertEquals("Due to change in times", tfr.replacedReason)
        assertEquals("No pilots may operate an aircraft", tfr.restriction)
        assertEquals(listOf("EXC THE FLT OPS LISTED BLW:", "1. ACFT MAY NOT LOITER."), tfr.instructions)

        // An area with nothing to define it is dropped
        val area = tfr.areas.single()
        assertEquals("Area A", area.name)
        assertEquals(4999, area.maxAltitude?.feet)
        assertEquals(1795556700000L, area.activeTime)
        assertNull(area.expireTime)
        assertEquals(2, area.parts.size)

        val circle = area.parts[0].vertices.single()
        assertEquals(false, area.parts[0].excluded)
        assertEquals(TfrList.TfrVertexType.CIRCLE, circle.type)
        assertEquals(5.0, circle.radius!!, 0.0)
        val fix = circle.fix!!
        assertEquals("Falmouth", fix.navaid)
        assertEquals("VOR/DME", fix.navaidType)
        assertEquals("FLM", fix.ident)
        assertEquals(329, fix.bearing)
        assertEquals(29.2, fix.distance, 0.0)

        // Cut out of the area, and the bad point is skipped
        val excluded = area.parts[1]
        assertTrue(excluded.excluded)
        assertEquals(
            listOf(TfrList.TfrVertexType.POINT, TfrList.TfrVertexType.ARC_CCW),
            excluded.vertices.map { it.type }
        )
        assertNull(excluded.vertices[0].fix)
    }
}
