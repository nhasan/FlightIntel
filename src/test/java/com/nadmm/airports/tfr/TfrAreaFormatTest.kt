package com.nadmm.airports.tfr

import com.nadmm.airports.tfr.TfrList.Altitude
import com.nadmm.airports.tfr.TfrList.AltitudeType
import com.nadmm.airports.tfr.TfrList.TfrFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TfrAreaFormatTest {
    @Test
    fun formatsCoordinatesAsDegreesMinutesSeconds() {
        assertEquals("39°02'55\"N 084°40'04\"W", TfrAreaFormat.formatCoordinates(39.04861, -84.66778))
        assertEquals("33°52'00\"S 151°12'30\"E", TfrAreaFormat.formatCoordinates(-33.86667, 151.20833))
        // 59.9" rounds up into the next minute, not to 60"
        assertEquals("10°01'00\"N 020°00'00\"E", TfrAreaFormat.formatCoordinates(10.0166528, 20.0))
    }

    @Test
    fun formatsFixFromNavaid() {
        assertEquals(
            "Falmouth VOR/DME (FLM) 329° radial at 29.2 NM",
            TfrAreaFormat.formatFix(TfrFix("Falmouth", "VOR/DME", "FLM", 329, 29.2))
        )
        assertEquals("FLM 4° radial at 7 NM", TfrAreaFormat.formatFix(TfrFix(null, null, "FLM", 4, 7.0)))
    }

    @Test
    fun formatsNauticalMiles() {
        assertEquals("5 NM", TfrAreaFormat.formatNm(5.0))
        assertEquals("4.5 NM", TfrAreaFormat.formatNm(4.5))
        assertEquals("2.7 NM", TfrAreaFormat.formatNm(2.6998))
    }

    @Test
    fun formatsAltitudeRange() {
        val surface = Altitude(0, AltitudeType.AGL)
        assertEquals(
            "Surface up to 4,999 ft AGL",
            TfrAreaFormat.formatAltitudeRange(surface, Altitude(4999, AltitudeType.AGL))
        )
        assertEquals(
            "1,500 ft AGL up to unlimited",
            TfrAreaFormat.formatAltitudeRange(Altitude(1500, AltitudeType.AGL), Altitude(99999, AltitudeType.MSL))
        )
        assertEquals("Up to 18,000 ft MSL", TfrAreaFormat.formatAltitudeRange(null, Altitude(18000, AltitudeType.MSL)))
        assertNull(TfrAreaFormat.formatAltitudeRange(null, null))
    }
}
