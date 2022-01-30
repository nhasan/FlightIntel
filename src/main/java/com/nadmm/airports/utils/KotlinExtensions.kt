package com.nadmm.airports.utils

import android.database.Cursor

fun Cursor?.forEach(runForEachRow: () -> Unit) {
    this?.let {
        if (moveToFirst()) {
            do {
                runForEachRow()
            } while (moveToNext())
        }
        close()
    }
}
