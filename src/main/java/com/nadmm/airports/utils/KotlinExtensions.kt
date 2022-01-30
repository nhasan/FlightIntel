package com.nadmm.airports.utils

import android.database.Cursor

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
