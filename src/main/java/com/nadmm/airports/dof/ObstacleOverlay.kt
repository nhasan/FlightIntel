/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.dof

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Typeface
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import com.nadmm.airports.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Draws obstacles, towers with a tower icon and others as triangles, larger
 * for those 1,000 ft AGL or taller, and the search position as a dot. The
 * ten tallest over 100 ft AGL, and any 100 ft AGL or taller within 1 NM of
 * the search position, stand out in red with their heights, MSL with AGL in
 * parentheses as on a sectional chart. A name for the search position, such
 * as an airport's, can be drawn above it. One overlay draws them all, as a
 * search can find over a thousand. Tapping near an obstacle selects it.
 */
class ObstacleOverlay(
    private val context: Context,
    private val onSelect: (DofListDataModel?) -> Unit
) : Overlay() {
    private class Item(val obstacle: DofListDataModel, val point: GeoPoint) {
        val isTower = obstacle.obstacleType.contains("Tower")
        val isTall = obstacle.aglHeight >= TALL_AGL_FT
    }

    private val density = context.resources.displayMetrics.density
    private var items: List<Item> = emptyList()
    private var highlighted: List<Item> = emptyList()
    private var selected: Item? = null
    var center: GeoPoint? = null
    /** Drawn above the center dot, such as the airport searched around. */
    var centerLabel: String? = null

    private val screen = Point()
    private val path = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = OUTLINE_DP * density
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val labelText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, LABEL_SP, context.resources.displayMetrics)
        typeface = Typeface.DEFAULT_BOLD
        color = HIGHLIGHT_COLOR
    }
    private val labelHalo = Paint(labelText).apply {
        style = Paint.Style.STROKE
        strokeWidth = LABEL_HALO_DP * density
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val centerText = Paint(labelText).apply { color = POSITION_LABEL_COLOR }
    private val centerHalo = Paint(labelHalo)
    // Tower icons drawn once per color and size, as drawing the vector for
    // each of a thousand obstacles on every frame would be slow
    private val towerIcons = HashMap<Pair<Int, Int>, Bitmap>()

    /** Replaces the obstacles, keeping the selection if it is still among them. */
    fun setObstacles(obstacles: List<DofListDataModel>) {
        items = obstacles.mapNotNull { o ->
            val lat = o.latitude ?: return@mapNotNull null
            val lon = o.longitude ?: return@mapNotNull null
            Item(o, GeoPoint(lat, lon))
        }
        val tallest = items.filter { it.obstacle.aglHeight > TALLEST_MIN_AGL_FT }
            .sortedByDescending { it.obstacle.aglHeight }
            .take(TALLEST_COUNT)
        val near = items.filter { isHighlightedNear(it.obstacle) }
        highlighted = (tallest + near).distinct()
        selected = selected?.let { old -> items.firstOrNull { it.obstacle == old.obstacle } }
        if (selected == null) onSelect(null)
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        for (item in items) {
            if (item !== selected && item !in highlighted) drawObstacle(canvas, projection, item, OBSTACLE_COLOR, 1f)
        }
        // The highlighted ones and the selection on top, so they stay visible where
        // obstacles crowd
        for (item in highlighted) {
            if (item !== selected) drawObstacle(canvas, projection, item, HIGHLIGHT_COLOR, HIGHLIGHT_SCALE)
        }
        selected?.let {
            drawObstacle(canvas, projection, it, SELECTED_COLOR,
                if (it in highlighted) HIGHLIGHT_SCALE else SELECTED_SCALE)
        }
        for (item in highlighted) drawHeightLabel(canvas, projection, item)
        center?.let {
            projection.toPixels(it, screen)
            fill.color = POSITION_COLOR
            val r = POSITION_DP * density
            canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), r, fill)
            canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), r, outline)
            centerLabel?.let { label ->
                val x = screen.x - centerText.measureText(label) / 2
                val y = screen.y - r - CENTER_LABEL_GAP_DP * density - centerText.descent()
                canvas.drawText(label, x, y, centerHalo)
                canvas.drawText(label, x, y, centerText)
            }
        }
    }

    private fun drawObstacle(canvas: Canvas, projection: Projection, item: Item, color: Int, scale: Float) {
        projection.toPixels(item.point, screen)
        val x = screen.x.toFloat()
        val y = screen.y.toFloat()
        val half = (if (item.isTall) TALL_DP else SIZE_DP) * density * scale
        if (item.isTower) {
            val icon = towerIcon(color, (half * TOWER_ICON_SCALE * 2).roundToInt())
            canvas.drawBitmap(icon, x - icon.width / 2f, y - icon.height / 2f, null)
            return
        }
        path.reset()
        path.moveTo(x, y - half)
        path.lineTo(x + half, y + half * 0.8f)
        path.lineTo(x - half, y + half * 0.8f)
        path.close()
        fill.color = color
        canvas.drawPath(path, fill)
        canvas.drawPath(path, outline)
    }

    /** MSL with AGL in parentheses, to the right of the obstacle. */
    private fun drawHeightLabel(canvas: Canvas, projection: Projection, item: Item) {
        projection.toPixels(item.point, screen)
        val text = String.format(Locale.US, "%,d' (%,d')",
            item.obstacle.mslHeight, item.obstacle.aglHeight)
        val x = screen.x + (TALL_DP * HIGHLIGHT_SCALE * TOWER_ICON_SCALE + LABEL_GAP_DP) * density
        val y = screen.y - (labelText.ascent() + labelText.descent()) / 2
        canvas.drawText(text, x, y, labelHalo)
        canvas.drawText(text, x, y, labelText)
    }

    /** The tower icon in [color] on a white disc, [size] pixels across. */
    private fun towerIcon(color: Int, size: Int): Bitmap = towerIcons.getOrPut(color to size) {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val r = size / 2f
        fill.color = Color.WHITE
        canvas.drawCircle(r, r, r, fill)
        AppCompatResources.getDrawable(context, R.drawable.ic_outline_cell_tower_24)?.mutate()?.let {
            it.setTint(color)
            val inset = (size * ICON_INSET).roundToInt()
            it.setBounds(inset, inset, size - inset, size - inset)
            it.draw(canvas)
        }
        bitmap
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val projection = mapView.projection
        var nearest: Item? = null
        var nearestDistance = TAP_SLOP_DP * density
        for (item in items) {
            projection.toPixels(item.point, screen)
            val d = hypot(screen.x - e.x, screen.y - e.y)
            if (d <= nearestDistance) {
                nearest = item
                nearestDistance = d
            }
        }
        if (nearest == null && selected == null) return false
        selected = nearest
        onSelect(nearest?.obstacle)
        mapView.invalidate()
        return nearest != null
    }

    companion object {
        private const val OBSTACLE_COLOR = 0xFFE65100.toInt()
        private const val HIGHLIGHT_COLOR = 0xFFC62828.toInt()
        private const val SELECTED_COLOR = 0xFF1565C0.toInt()
        private const val POSITION_COLOR = 0xFF1E88E5.toInt()
        private const val POSITION_LABEL_COLOR = 0xFF1565C0.toInt()
        private const val CENTER_LABEL_GAP_DP = 4f
        // The tallest stand out, but only those tall enough to matter
        private const val TALLEST_COUNT = 10
        private const val TALLEST_MIN_AGL_FT = 100
        // So do those close in, where they matter most near an airport
        const val NEAR_RADIUS_NM = 1f
        private const val NEAR_MIN_AGL_FT = 100

        /** True for an obstacle highlighted for being tall and close in. */
        fun isHighlightedNear(obstacle: DofListDataModel) =
            obstacle.aglHeight >= NEAR_MIN_AGL_FT
                    && (obstacle.distanceNm ?: Float.MAX_VALUE) <= NEAR_RADIUS_NM
        private const val TALL_AGL_FT = 1000
        private const val SIZE_DP = 6f
        private const val TALL_DP = 9f
        private const val SELECTED_SCALE = 1.4f
        private const val HIGHLIGHT_SCALE = 1.5f
        // Tower icons are a little larger than triangles to read as icons
        private const val TOWER_ICON_SCALE = 1.4f
        private const val ICON_INSET = 0.12f
        private const val OUTLINE_DP = 1.5f
        private const val POSITION_DP = 6f
        private const val TAP_SLOP_DP = 24f
        private const val LABEL_SP = 12f
        private const val LABEL_HALO_DP = 3f
        private const val LABEL_GAP_DP = 2f
    }
}
