package com.nadmm.airports.dof

import org.junit.Assert.assertEquals
import org.junit.Test

class DofListDataModelTest {
    @Test
    fun labelsGroupsWithTheirCount() {
        assertEquals("Tower", DofListDataModel.obstacleLabel("TOWER", 1))
        assertEquals("Tower (4 obstacles)", DofListDataModel.obstacleLabel("TOWER", 4))
        assertEquals("Building", DofListDataModel.obstacleLabel("BLDG", 0))
        assertEquals("T-L Tower (2 obstacles)", DofListDataModel.obstacleLabel("T-L TWR", 2))
    }

    @Test
    fun writesOutAbbreviatedTypesInTitleCase() {
        assertEquals("Utility Pole", DofListDataModel.obstacleLabel("UTILITY POLE", 1))
        assertEquals("Building-Tower", DofListDataModel.obstacleLabel("BLDG-TWR", 1))
        assertEquals("Vertical Structure", DofListDataModel.obstacleLabel("VERTICAL STRUCTUR", 1))
        assertEquals("Meteorological Tower", DofListDataModel.obstacleLabel("MET", 1))
        assertEquals("Control Tower", DofListDataModel.obstacleLabel("CTRL TWR", 1))
    }

    @Test
    fun knowsWhichCodesAreLighted() {
        for (code in listOf("R", "D", "H", "M", "S", "F", "C", "W", "L")) {
            assertEquals(code, true, DofListDataModel.isLighted(code))
        }
        for (code in listOf("N", "U", "", "RD", null)) {
            assertEquals(code.toString(), false, DofListDataModel.isLighted(code))
        }
    }
}
