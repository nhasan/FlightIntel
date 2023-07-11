package com.nadmm.airports.utils

import android.database.Cursor
import android.os.Bundle
import com.nadmm.airports.data.DatabaseManager.Airports

fun Cursor?.forEach(runForEachRow: (Cursor) -> Unit) {
    this?.let {
        if (moveToFirst()) {
            do {
                runForEachRow(this)
            } while (moveToNext())
        }
        close()
    }
}

fun Cursor.makeAirportBundle(): Bundle {
    val siteNumber = getString(getColumnIndexOrThrow(Airports.SITE_NUMBER))
    val faaCode = getString(getColumnIndexOrThrow(Airports.FAA_CODE))
    val icaoCode = getString(getColumnIndexOrThrow(Airports.ICAO_CODE))
    return Bundle().apply {
        putString(Airports.SITE_NUMBER, siteNumber)
        putString(Airports.FAA_CODE, faaCode)
        putString(Airports.ICAO_CODE, icaoCode)
    }
}