package com.nadmm.airports.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AircraftTypeDesignatorsTest {
    @Test
    fun namesCommonTypes() {
        assertEquals("Cessna, 172", AircraftTypeDesignators.getAircraftName("C172"))
        assertEquals("Cessna, 182", AircraftTypeDesignators.getAircraftName("C182"))
        assertEquals("Boeing, 737-800", AircraftTypeDesignators.getAircraftName("B738"))
        assertEquals("Beech, 200 Super King Air", AircraftTypeDesignators.getAircraftName("BE20"))
        assertEquals("AgustaWestland, AW-139", AircraftTypeDesignators.getAircraftName("A139"))
    }

    @Test
    fun keepsDesignatorsIcaoListsApart() {
        assertEquals("Unknown", AircraftTypeDesignators.getAircraftName("UNKN"))
        assertEquals("Piper, PA-28 Cherokee", AircraftTypeDesignators.getAircraftName("PA28"))
    }

    @Test
    fun ignoresCaseAndSpacesAndUnknownCodes() {
        assertEquals("Cessna, 172", AircraftTypeDesignators.getAircraftName(" c172 "))
        assertEquals("", AircraftTypeDesignators.getAircraftName("XXXX"))
    }
}
