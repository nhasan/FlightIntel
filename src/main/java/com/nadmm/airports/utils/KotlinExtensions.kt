/*
 * FlightIntel for Pilots
 *
 * Copyright 2023 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.utils

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
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

fun LinearLayout.addSeparator(context: Context) {
    View(context).let { v ->
        v.setBackgroundColor(ContextCompat.getColor(context,
            com.google.android.material.R.color.material_on_surface_stroke))
        addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
    }
}

fun LinearLayout.addRow(context: Context, row: View) : View {
    if (childCount > 0) {
        addSeparator(context)
    }
    addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
    )
    return row
}
