package com.nadmm.airports.utils

import android.database.Cursor

fun Cursor?.forEach(runForEachRow: () -> Unit) {
    this?.let {
        if (this.moveToFirst()) {
            do {
                runForEachRow();
            } while (this.moveToNext())
            this.close()
        }
    }
}
