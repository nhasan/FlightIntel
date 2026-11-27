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
package com.nadmm.airports.tfr

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.withTranslation
import androidx.core.os.BundleCompat
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.tfr.TfrList.TfrShape
import com.nadmm.airports.utils.OsmMaps
import com.nadmm.airports.utils.TimeUtils
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import java.util.Locale
import kotlin.math.ceil

/** Draws the areas of a TFR on an OpenStreetMap map. */
class TfrMapFragment : FragmentBase() {
    private var mapView: MapView? = null
    private var tfr: Tfr? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        OsmMaps.configure(requireContext())
        val view = inflater.inflate(R.layout.tfr_map_view, container, false)
        mapView = view.findViewById<MapView>(R.id.tfr_map).also { OsmMaps.setUp(it) }
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val map = mapView ?: return
        val tfr = BundleCompat.getSerializable(
            requireArguments(), TfrListActivity.EXTRA_TFR, Tfr::class.java
        )
        this.tfr = tfr

        val shapes = tfr?.shapes.orEmpty()
        val points = shapes.map { shape ->
            shape.latitudes.indices.map { GeoPoint(shape.latitudes[it], shape.longitudes[it]) }
        }
        // The innermost area that starts at the surface gets a thicker outline
        // when another area encloses it, drawn last so the thinner outlines of
        // other areas do not cross it
        val surface = shapes.indices
            .filter { shapes[it].minAltitude?.feet == 0 }
            .minByOrNull { TfrMapLabels.area(shapes[it]) }
            ?.takeIf { inner -> shapes.any { TfrMapLabels.encloses(it, shapes[inner]) } }
            ?: -1
        for (i in shapes.indices.sortedBy { it == surface }) {
            val outlineWidth = if (i == surface) SURFACE_OUTLINE_WIDTH_DP else OUTLINE_WIDTH_DP
            map.overlays.add(Polygon(map).apply {
                setPoints(points[i])
                outlinePaint.color = TFR_COLOR
                outlinePaint.strokeWidth = outlineWidth * resources.displayMetrics.density
                fillPaint.color = Color.argb(FILL_ALPHA, Color.red(TFR_COLOR),
                    Color.green(TFR_COLOR), Color.blue(TFR_COLOR))
                infoWindow = null
            })
        }
        // The NOTAM id and name stay at the top center; each area is labeled
        // with its altitudes, and its times when they differ from the TFR's
        val metrics = resources.displayMetrics
        if (tfr != null) {
            for (label in TfrMapLabels.place(tfr.shapes)) {
                val text = areaText(label) { formatAreaTimes(it, tfr) } ?: continue
                map.overlays.add(TfrLabelOverlay(
                    text, metrics, LABEL_COLOR, GeoPoint(label.latitude, label.longitude)))
            }
        }
        val title = tfr?.let { titleText(it, it.formatTimeRange(activityBase)) }
            ?.let { TfrLabelOverlay(it, metrics, TITLE_COLOR) }
        title?.let { map.overlays.add(it) }
        OsmMaps.addScaleAndAttribution(map)

        val all = points.flatten()
        if (all.isNotEmpty()) {
            val box = BoundingBox.fromGeoPointsSafe(all)
            // The padding applies to all sides, so make it clear the title
            val titleBottom = title?.let { TITLE_MARGIN_DP * metrics.density + it.height } ?: 0f
            val padding = maxOf(ZOOM_PADDING_DP * metrics.density,
                titleBottom + TITLE_MARGIN_DP * metrics.density).toInt()
            // Zooming needs the map's size, which is known only after layout
            map.addOnFirstLayoutListener { _, _, _, _, _ ->
                map.zoomToBoundingBox(box, false, padding)
            }
        }
        setFragmentContentShown(true)
    }

    /**
     * An area's effective dates when they differ from the TFR's, which the
     * title already shows, plus its daily window if it has one.
     */
    private fun formatAreaTimes(shape: TfrShape, tfr: Tfr): List<String> {
        val activity = activityBase
        val active = shape.activeTime
        val expire = shape.expireTime
        val lines = ArrayList<String>()
        if (TfrMapLabels.hasOwnDates(shape, tfr)) when {
            active != null && expire != null ->
                lines.add(TimeUtils.formatDateRange(activity, active, expire))
            active != null -> lines.add("From " + TimeUtils.formatDateTimeYear(activity, active))
            expire != null -> lines.add("Until " + TimeUtils.formatDateTimeYear(activity, expire))
        }
        TfrMapLabels.dailyWindow(shape)?.let { (start, end) ->
            lines.add("Daily " + TfrAreaFormat.formatTimeOfDayRange(activity, start, end))
        }
        return lines
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        // The name is on the map, so the action bar shows where the TFR is
        tfr?.let { setActionBarSubtitle(it.formatLocation()) }
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        mapView?.onDetach()
        mapView = null
        super.onDestroyView()
    }

    companion object {
        private val TFR_COLOR = Color.rgb(0xD3, 0x2F, 0x2F)
        private const val FILL_ALPHA = 0x40
        private const val OUTLINE_WIDTH_DP = 2f
        private const val SURFACE_OUTLINE_WIDTH_DP = 4f
        private const val ZOOM_PADDING_DP = 48f
        private val LABEL_COLOR = Color.rgb(0xB7, 0x1C, 0x1C)
        private const val TITLE_COLOR = Color.BLACK
        private const val LABEL_TEXT_SP = 14f
        private const val LABEL_HALO_DP = 3f
        private const val TITLE_MARGIN_DP = 12f
        // Name, type, altitudes and times, smaller than the NOTAM id
        private const val SMALL_SCALE = 0.85f

        private fun titleText(tfr: Tfr, time: String): CharSequence? {
            val id = tfr.notamId.orEmpty().trim()
            val name = tfr.name.orEmpty().trim()
            val text = SpannableStringBuilder()
            if (id.isNotEmpty()) text.appendLine(id, StyleSpan(Typeface.BOLD))
            if (name.isNotEmpty() && !name.equals(id, ignoreCase = true)) {
                text.appendLine(name, RelativeSizeSpan(SMALL_SCALE))
            }
            val type = titleCase(tfr.type.orEmpty().trim())
            if (type.isNotEmpty() && !type.equals(name, ignoreCase = true)) {
                text.appendLine(type, RelativeSizeSpan(SMALL_SCALE))
            }
            if (time.isNotEmpty()) text.appendLine(time, RelativeSizeSpan(SMALL_SCALE))
            return text.takeIf { it.isNotEmpty() }
        }

        /**
         * The altitudes of the areas under a label, each followed by the times
         * [times] gives for its areas. Areas sharing an outline are stacked, so
         * the highest come first, and areas at the same altitudes but different
         * times share the altitude line.
         */
        private fun areaText(
            label: TfrMapLabels.Label, times: (TfrShape) -> List<String>
        ): CharSequence? {
            val text = SpannableStringBuilder()
            label.shapes
                .sortedWith(compareByDescending<TfrShape> {
                    it.maxAltitude?.feet ?: it.minAltitude?.feet ?: 0
                }.thenBy { it.activeTime ?: 0L })
                .groupBy { TfrMapLabels.formatAltitudeRange(it) }
                .forEach { (altitude, shapes) ->
                    altitude?.let { text.appendLine(it, RelativeSizeSpan(SMALL_SCALE)) }
                    shapes.flatMap(times).distinct().forEach {
                        text.appendLine(it, RelativeSizeSpan(SMALL_SCALE))
                    }
                }
            return text.takeIf { it.isNotEmpty() }
        }

        // Kept in capitals when title casing a TFR type
        private val ACRONYMS = setOf("VIP", "UAS")

        /** "SPACE OPERATIONS" as "Space Operations", keeping acronyms such as VIP. */
        private fun titleCase(text: String): String =
            text.split(Regex("(?<=[\\s/-])")).joinToString("") { word ->
                if (word.trim(' ', '/', '-').uppercase() in ACRONYMS) word.uppercase()
                else word.lowercase().replaceFirstChar { it.titlecase(Locale.US) }
            }

        private fun SpannableStringBuilder.appendLine(line: String, vararg spans: Any) {
            if (isNotEmpty()) append("\n")
            val start = length
            append(line)
            for (span in spans) setSpan(span, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * Draws text with a halo for contrast with the tiles: centered on [position]
     * when given, so it moves with the map, or else fixed at the top center.
     */
    private class TfrLabelOverlay(
        text: CharSequence,
        private val metrics: DisplayMetrics,
        textColor: Int,
        private val position: GeoPoint? = null
    ) : Overlay() {
        private val fillLayout: StaticLayout
        private val haloLayout: StaticLayout
        private val point = Point()

        val height get() = fillLayout.height

        init {
            val fill = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, LABEL_TEXT_SP, metrics)
            }
            val halo = TextPaint(fill).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeWidth = LABEL_HALO_DP * metrics.density
            }
            // As wide as the screen allows inside the zoom padding, so text
            // wraps only when it must
            val maxWidth = metrics.widthPixels - (2 * ZOOM_PADDING_DP * metrics.density).toInt()
            val width = minOf(maxWidth, ceil(Layout.getDesiredWidth(text, fill)).toInt())
            fillLayout = layout(text, fill, width)
            haloLayout = layout(text, halo, width)
        }

        private fun layout(text: CharSequence, paint: TextPaint, width: Int) =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()

        override fun draw(canvas: Canvas, projection: Projection) {
            val x: Float
            val y: Float
            if (position != null) {
                projection.toPixels(position, point)
                x = point.x - fillLayout.width / 2f
                y = point.y - fillLayout.height / 2f
            } else {
                x = (projection.width - fillLayout.width) / 2f
                y = TITLE_MARGIN_DP * metrics.density
            }
            canvas.withTranslation(x, y) {
                haloLayout.draw(this)
                fillLayout.draw(this)
            }
        }
    }
}
