package com.nadmm.airports.dof

import android.database.Cursor
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.data.DatabaseManager.DOF
import com.nadmm.airports.utils.GeoUtils

data class DofListDataModel(
    val obstacleType: String,
    val mslHeight: Int,
    val aglHeight: Int,
    val markingType: String,
    val lightingType: String,
    val location: String
) {
    companion object {
        fun fromCursor(cursor: Cursor): DofListDataModel {
            val obstacleType = decodeObstacle(cursor.getString(cursor.getColumnIndexOrThrow(DOF.OBSTACLE_TYPE)))
            val count = cursor.getInt(cursor.getColumnIndexOrThrow(DOF.COUNT))
            if (count > 1) {
               obstacleType.plus(" ($count count)")
            }
            val mslHeight = cursor.getInt(cursor.getColumnIndexOrThrow(DOF.HEIGHT_MSL))
            val aglHeight = cursor.getInt(cursor.getColumnIndexOrThrow(DOF.HEIGHT_AGL))
            val markingType = decodeMarking(cursor.getString(cursor.getColumnIndexOrThrow(DOF.MARKING_TYPE)))
            val lightingType = decodeLighting(cursor.getString(cursor.getColumnIndexOrThrow(DOF.LIGHTING_TYPE)))
            val location = buildString {
                if (cursor.getColumnIndexOrThrow(LocationColumns.DISTANCE) >= 0
                    && cursor.getColumnIndexOrThrow(LocationColumns.BEARING) >= 0
                ) {
                    val distance = cursor.getFloat(cursor.getColumnIndexOrThrow(LocationColumns.DISTANCE))
                    val bearing = cursor.getFloat(cursor.getColumnIndexOrThrow(LocationColumns.BEARING))
                    append(
                        "%.1f NM %s, heading %.0f\u00B0 M".format(
                            distance, GeoUtils.getCardinalDirection(bearing), bearing
                        )
                    )
                } else {
                    append("Location unknown")
                }
            }
            return DofListDataModel(
                obstacleType,
                mslHeight,
                aglHeight,
                markingType,
                lightingType,
                location
            )
        }

        private fun decodeObstacle(type: String): String {
            return type.replace("TWR", "TOWER")
                .replace("BLDG", "BUILDING")
        }

        private fun decodeMarking(code: String?): String {
            return when (code) {
                "P" -> "Orange/White paint marker"
                "W" -> "White paint marker"
                "M" -> "Marked"
                "F" -> "Flag marker"
                "S" -> "Spherical marker"
                "N" -> "Not marked"
                else -> "Unknown marking"
            }
        }

        private fun decodeLighting(type: String?): String {
            return when (type) {
                "R" -> "Red lighting"
                "D" -> "Medium intensity White Strobe & Red lighting"
                "H" -> "High intensity White Strobe & Red lighting"
                "M" -> "Medium intensity White Strobe lighting"
                "S" -> "High intensity White Strobe lighting"
                "F" -> "Flood lighting"
                "C" -> "Dual medium catenary lighting"
                "W" -> "Synchronized Red lighting"
                "L" -> "Lighted"
                "N" -> "Not lighted"
                else -> "Unknown lighting"
            }
        }
    }
}