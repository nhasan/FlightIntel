package com.nadmm.airports.dof

import android.database.Cursor
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.data.DatabaseManager.DOF
import com.nadmm.airports.utils.GeoUtils
import java.util.Locale

data class DofListDataModel(
    val obstacleType: String,
    val mslHeight: Int,
    val aglHeight: Int,
    val markingType: String,
    val lightingType: String,
    val location: String,
    // Where the obstacle is, and how far from the search position in NM,
    // when the cursor has them
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceNm: Float? = null
) {
    companion object {
        fun fromCursor(cursor: Cursor): DofListDataModel {
            val obstacleType = obstacleLabel(
                cursor.getString(cursor.getColumnIndexOrThrow(DOF.OBSTACLE_TYPE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DOF.COUNT))
            )
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
            val latitude = cursor.getColumnIndex(DOF.LATITUDE_DEGREES)
                .takeIf { it >= 0 }?.let { cursor.getDouble(it) }
            val longitude = cursor.getColumnIndex(DOF.LONGITUDE_DEGREES)
                .takeIf { it >= 0 }?.let { cursor.getDouble(it) }
            val distanceNm = cursor.getColumnIndex(LocationColumns.DISTANCE)
                .takeIf { it >= 0 }?.let { cursor.getFloat(it) }
            return DofListDataModel(
                obstacleType,
                mslHeight,
                aglHeight,
                markingType,
                lightingType,
                location,
                latitude,
                longitude,
                distanceNm
            )
        }

        /**
         * The obstacle type, such as "TOWER", with the number of obstacles
         * when the entry stands for a group of them, such as a tower farm.
         */
        fun obstacleLabel(type: String, count: Int): String {
            val name = decodeObstacle(type)
            return if (count > 1) "$name ($count obstacles)" else name
        }

        /**
         * The obstacle type in title case, such as "Utility Pole", with the
         * FAA's abbreviated and cut-off names written out in full.
         */
        private fun decodeObstacle(type: String): String {
            val name = TYPE_NAMES[type.trim()] ?: type.replace("TWR", "TOWER")
                .replace("BLDG", "BUILDING")
            return titleCase(name)
        }

        // Words capitalized at each space or hyphen, so "T-L TOWER" stays "T-L Tower"
        private fun titleCase(text: String): String =
            text.lowercase(Locale.US).split(Regex("(?<=[ -])")).joinToString("") { word ->
                word.replaceFirstChar { it.titlecase(Locale.US) }
            }

        // Types that are abbreviated, or cut off by the field length
        private val TYPE_NAMES = mapOf(
            "VERTICAL STRUCTUR" to "VERTICAL STRUCTURE",
            "NATURAL GAS SYSTE" to "NATURAL GAS SYSTEM",
            "LGTHOUSE" to "LIGHTHOUSE",
            "GEN UTIL" to "GENERAL UTILITY",
            "ELEC SYS" to "ELECTRICAL SYSTEM",
            "CTRL TWR" to "CONTROL TOWER",
            "COOL TWR" to "COOLING TOWER",
            "AG EQUIP" to "AGRICULTURAL EQUIPMENT",
            "MET" to "METEOROLOGICAL TOWER",
        )

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