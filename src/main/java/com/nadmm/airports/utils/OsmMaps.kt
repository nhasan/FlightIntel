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
package com.nadmm.airports.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import java.io.File
import java.util.Locale

/** Shared setup for the app's OpenStreetMap maps. */
object OsmMaps {
    private const val SCALE_BAR_OFFSET_DP = 12f
    // osmdroid's own offset for the attribution, from the bottom left
    private const val COPYRIGHT_OFFSET_PX = 10

    /**
     * Configures osmdroid. The OSM tile usage policy requires an identifying
     * user agent, and the tile cache stays in app storage so no storage
     * permission is needed. Call before inflating a MapView.
     */
    fun configure(context: Context) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
    }

    /**
     * Sets up a map shown as a page of a ViewPager2. Call [MapView.onDetach]
     * from the fragment's onDestroyView.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setUp(map: MapView) {
        map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            // The pager detaches the page from the window when switching tabs
            // and attaches it again later, so the map must outlive that
            setDestroyMode(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            // Drags pan the map rather than swipe to the next page or pull to
            // refresh; the tabs still switch pages
            setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    /**
     * Adds the scale bar in nautical miles at the bottom right, and the tile
     * attribution at the bottom left. Add them after the map's own overlays.
     */
    fun addScaleAndAttribution(map: MapView) {
        val density = map.resources.displayMetrics.density
        map.overlays.add(NmScaleBarOverlay(map).apply {
            setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.nautical)
            setAlignBottom(true)
            setAlignRight(true)
            val offset = (SCALE_BAR_OFFSET_DP * density).toInt()
            setScaleBarOffset(offset, offset)
        })
        map.overlays.add(CopyrightOverlay(map.context))
    }

    /**
     * Runs [map] on under the navigation bar and any side bars, while its
     * parent's other views, and the map's scale bar, attribution and zoom
     * buttons, stay clear of them. The parent keeps the padding the system
     * bars need; the map reaches out past it. Call after
     * [addScaleAndAttribution], from onViewCreated after super's.
     */
    fun runUnderSystemBars(map: MapView) {
        val container = map.parent as? ViewGroup ?: return
        val offset = (SCALE_BAR_OFFSET_DP * map.resources.displayMetrics.density).toInt()
        container.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            map.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(-bars.left, 0, -bars.right, -bars.bottom)
            }
            for (overlay in map.overlays) {
                when (overlay) {
                    is ScaleBarOverlay ->
                        overlay.setScaleBarOffset(offset + bars.right, offset + bars.bottom)
                    is CopyrightOverlay ->
                        overlay.setOffset(COPYRIGHT_OFFSET_PX + bars.left, COPYRIGHT_OFFSET_PX + bars.bottom)
                }
            }
            map.zoomController.display.setAdditionalPixelMargins(
                bars.left.toFloat(), 0f, bars.right.toFloat(), bars.bottom.toFloat())
            map.invalidate()
            insets
        }
        ViewCompat.requestApplyInsets(container)
    }

    /**
     * The scale bar labeled to the nearest 0.1 NM. osmdroid rounds to whole
     * NM from 5 NM up; below 0.1 NM it keeps osmdroid's label in feet.
     */
    private class NmScaleBarOverlay(map: MapView) : ScaleBarOverlay(map) {
        override fun scaleBarLengthText(meters: Double): String {
            val nm = meters / GeoUtils.METERS_PER_NAUTICAL_MILE
            return if (nm >= 0.1) String.format(Locale.getDefault(), "%.1f nm", nm)
            else super.scaleBarLengthText(meters)
        }
    }
}
