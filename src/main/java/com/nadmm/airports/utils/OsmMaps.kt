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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.color.MaterialColors
import com.nadmm.airports.R
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
    private const val OFFLINE_PADDING_DP = 8f
    private const val OFFLINE_TEXT = "Offline: Map detail may be missing"

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
            // The top margin belongs to the offline notice, when it shows
            map.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = -bars.left
                rightMargin = -bars.right
                bottomMargin = -bars.bottom
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
     * While the app has no network, shows a strip above [map] saying so: the
     * map then shows only tiles kept in the cache, and areas never seen at
     * the zoom shown appear blurred, scaled up from a lower zoom. The
     * map moves down below the strip rather than hide under it. When the
     * network comes back, the strip goes and the map redraws, fetching the
     * tiles it is missing. Watches only while [owner] is started.
     */
    fun showWhenOffline(map: MapView, owner: LifecycleOwner) {
        val container = map.parent as? ViewGroup ?: return
        val context = map.context
        val padding = (OFFLINE_PADDING_DP * map.resources.displayMetrics.density).toInt()
        val notice = TextView(context).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextSmall)
            text = OFFLINE_TEXT
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerHighest))
            setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant))
            visibility = View.GONE
        }
        container.addView(notice, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP))
        // The map starts below the strip while it shows
        notice.addOnLayoutChangeListener { v, _, top, _, bottom, _, _, _, _ ->
            val height = if (v.visibility == View.VISIBLE) bottom - top else 0
            if (map.marginTop != height) {
                map.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = height }
            }
        }

        val cm = context.getSystemService(ConnectivityManager::class.java)
        fun show(online: Boolean) {
            if (online == (notice.visibility == View.GONE)) return
            notice.visibility = if (online) View.GONE else View.VISIBLE
            if (online) {
                map.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = 0 }
                // Missing tiles are requested again as the map draws
                map.invalidate()
            }
        }
        // Networks with the internet, and those blocked for this app, as the
        // callback reports them on a binder thread
        val internet = mutableSetOf<Network>()
        val blocked = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun update() {
                val online = synchronized(internet) { (internet - blocked).isNotEmpty() }
                map.post { show(online) }
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                synchronized(internet) {
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        internet.add(network)
                    } else {
                        internet.remove(network)
                    }
                }
                update()
            }

            // Such as by Data Saver, or a setting that turns off the app's data
            override fun onBlockedStatusChanged(network: Network, isBlocked: Boolean) {
                synchronized(internet) {
                    if (isBlocked) blocked.add(network) else blocked.remove(network)
                }
                update()
            }

            override fun onLost(network: Network) {
                synchronized(internet) {
                    internet.remove(network)
                    blocked.remove(network)
                }
                update()
            }
        }

        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Start from what holds now, so the strip doesn't flash while the
                // first calls come; with no network at all, none come. The
                // active network is null while it is blocked for this app.
                synchronized(internet) {
                    internet.clear()
                    blocked.clear()
                }
                val online = cm.getNetworkCapabilities(cm.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                show(online)
                cm.registerDefaultNetworkCallback(callback)
            }

            override fun onStop(owner: LifecycleOwner) {
                cm.unregisterNetworkCallback(callback)
            }
        })
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
