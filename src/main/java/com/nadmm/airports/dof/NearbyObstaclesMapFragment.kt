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

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.OsmMaps
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * The Map tab of the Obstacles screen: the tallest obstacles in its view.
 * It opens on the location, and gives its view to the screen each time it
 * settles after a move or zoom, for the screen to find the obstacles in it.
 */
class NearbyObstaclesMapFragment : FragmentBase() {
    private val viewModel: NearbyObstaclesViewModel by activityViewModels()
    private var mapView: MapView? = null
    private var status: TextView? = null
    private var details: TextView? = null
    private var hintView: TextView? = null
    // The hint goes once the user has tapped an obstacle, or after it has
    // been on screen for HINT_TIMEOUT_MS
    private var hintDismissed = false
    // A job, not a post to the view: the pager detaches the page before
    // pausing it, and a detached view can no longer cancel its posts
    private var hintTimeout: Job? = null
    private var obstacleOverlay: ObstacleOverlay? = null
    // Zoomed to the location once; after that the view is the user's
    private var zoomed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        OsmMaps.configure(requireContext())
        val view = inflater.inflate(R.layout.dof_map_view, container, false)
        mapView = view.findViewById<MapView>(R.id.dof_map).also { OsmMaps.setUp(it) }
        status = view.findViewById(R.id.dof_map_status)
        // Only the backgrounds are see-through, so the text stays crisp
        details = view.findViewById<TextView>(R.id.dof_map_details).apply {
            background = background.mutate().apply { alpha = DETAILS_BACKGROUND_ALPHA }
        }
        hintView = view.findViewById<TextView>(R.id.dof_map_hint).apply {
            background = background.mutate().apply { alpha = DETAILS_BACKGROUND_ALPHA }
        }
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val map = mapView ?: return
        // Zoomed out further, the area around the view gets too big to hold
        map.minZoomLevel = MIN_ZOOM

        obstacleOverlay = ObstacleOverlay(requireContext()) { showDetails(it) }.apply {
            centerLabel = arguments?.getString(NearbyObstaclesActivity.EXTRA_PLACE_NAME)
        }
        map.overlays.add(obstacleOverlay)
        OsmMaps.addScaleAndAttribution(map)
        OsmMaps.runUnderSystemBars(map)
        OsmMaps.showWhenOffline(map, viewLifecycleOwner)

        status?.text = WAITING_TEXT
        setFragmentContentShown(true)

        // Once the map settles after a move or zoom; not before it first
        // zooms to the location, as until then it shows the whole world
        map.addMapListener(DelayedMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?) = reportView()
            override fun onZoom(event: ZoomEvent?) = reportView()
        }, VIEW_SETTLE_MS))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.nearby.collect { nearby -> nearby?.let { show(it) } }
                }
                launch {
                    viewModel.recenter.collect { recenter() }
                }
                // Until there are obstacles, why not, such as no location permission
                viewModel.message.collect { message ->
                    if (viewModel.nearby.value == null) status?.text = message ?: WAITING_TEXT
                }
            }
        }
    }

    private fun reportView(): Boolean {
        val map = mapView ?: return true
        if (zoomed && map.width > 0) viewModel.setViewport(map.boundingBox)
        return true
    }

    private fun show(nearby: NearbyObstacles) {
        val map = mapView ?: return
        val center = GeoPoint(nearby.location.latitude, nearby.location.longitude)
        obstacleOverlay?.center = center
        obstacleOverlay?.setObstacles(nearby.obstacles)

        map.visibility = View.VISIBLE
        status?.apply {
            visibility = if (nearby.obstacles.isEmpty()) View.VISIBLE else View.GONE
            text = if (nearby.inView > 0) "No obstacles match the filters"
                else "No obstacles in view"
            // Over the map now, so at the top, clear of the location
            updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
        }
        hintView?.visibility =
            if (nearby.obstacles.isNotEmpty() && !hintDismissed) View.VISIBLE else View.GONE
        startHintTimeout()

        if (!zoomed) {
            zoomed = true
            val box = NearbyObstaclesFragment.startBox(nearby.location)
            val padding = (ZOOM_PADDING_DP * resources.displayMetrics.density).toInt()
            // Zooming needs the map's size, which is known only after layout
            if (map.width > 0 && map.height > 0) {
                map.zoomToBoundingBox(box, false, padding)
            } else {
                map.addOnFirstLayoutListener { _, _, _, _, _ ->
                    map.zoomToBoundingBox(box, false, padding)
                }
            }
        }
        map.invalidate()
    }

    /** Goes back to the view the map opens on, around the latest location. */
    private fun recenter() {
        val map = mapView ?: return
        val location = viewModel.nearby.value?.location ?: return
        val padding = (ZOOM_PADDING_DP * resources.displayMetrics.density).toInt()
        map.zoomToBoundingBox(NearbyObstaclesFragment.startBox(location), true, padding)
    }

    private fun showDetails(obstacle: DofListDataModel?) {
        details?.apply {
            if (obstacle == null) {
                visibility = View.GONE
                return
            }
            hintDismissed = true
            hintView?.visibility = View.GONE
            text = listOf(
                obstacle.obstacleType,
                FormatUtils.formatFeetMsl(obstacle.mslHeight.toFloat()) + " · " +
                        FormatUtils.formatFeetAgl(obstacle.aglHeight.toFloat()),
                obstacle.lightingType,
                obstacle.markingType,
                obstacle.location
            ).joinToString("\n")
            visibility = View.VISIBLE
        }
    }

    /**
     * Counts down the hint's time on screen, once it shows and the map is the
     * page in view; location updates while it counts don't restart it.
     */
    private fun startHintTimeout() {
        if (hintTimeout?.isActive == true || hintView?.visibility != View.VISIBLE || !isResumed) return
        hintTimeout = viewLifecycleOwner.lifecycleScope.launch {
            delay(HINT_TIMEOUT_MS)
            hintDismissed = true
            hintView?.animate()?.alpha(0f)?.withEndAction {
                hintView?.visibility = View.GONE
                hintView?.alpha = 1f
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        startHintTimeout()
    }

    override fun onPause() {
        // The count starts again when the map is back in view
        hintTimeout?.cancel()
        hintTimeout = null
        mapView?.onPause()
        super.onPause()
    }

    // The map takes every drag, so it never scrolls the toolbar away
    override val scrollsAppBar: Boolean
        get() = false

    override fun onDestroyView() {
        mapView?.onDetach()
        mapView = null
        status = null
        details = null
        hintTimeout = null
        hintView = null
        super.onDestroyView()
    }

    companion object {
        private const val HINT_TIMEOUT_MS = 10_000L
        private const val WAITING_TEXT =
            "Obstacles appear here once your location is known."
        private const val ZOOM_PADDING_DP = 16f
        // About 60 NM across a phone; the area held around the view is three
        // times that each way
        private const val MIN_ZOOM = 9.0
        private const val VIEW_SETTLE_MS = 300L
        // 20% translucent, so the map shows through the details panel
        private const val DETAILS_BACKGROUND_ALPHA = 204
    }
}
