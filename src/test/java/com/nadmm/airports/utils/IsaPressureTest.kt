package com.nadmm.airports.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class IsaPressureTest {
    @Test
    fun seaLevelIsStandardPressure() {
        assertEquals(29.9212, WxUtils.isaPressureInHg(0.0), 0.0001)
    }

    @Test
    fun tropopauseMatchesStandardAtmosphere() {
        // 11 km is 36,089 ft. Standard pressure there is 226.32 hPa, about 6.683 inHg.
        assertEquals(6.683, WxUtils.isaPressureInHg(WxUtils.TROPOPAUSE_FT.toDouble()), 0.01)
    }

    @Test
    fun flightLevel450StaysInTheStratosphere() {
        // The old 10e-5 literal made this about 0.09 inHg. Standard atmosphere is about 4.35.
        assertEquals(4.355, WxUtils.isaPressureInHg(45000.0), 0.02)
    }

    @Test
    fun twentyKilometersMatchesStandardAtmosphere() {
        // 20 km is 65,617 ft. Standard pressure there is 54.75 hPa, about 1.617 inHg.
        assertEquals(1.617, WxUtils.isaPressureInHg(65617.0), 0.01)
    }
}
