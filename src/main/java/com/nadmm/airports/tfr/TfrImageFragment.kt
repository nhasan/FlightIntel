/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2026 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.CoroutineIntentService
import com.nadmm.airports.utils.UiUtils.showToast
import com.nadmm.airports.views.ImageZoomView
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

class TfrImageFragment : FragmentBase() {
    private var mTfr: Tfr? = null
    private var mImageView: ImageZoomView? = null
    private var mImageShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTfr = BundleCompat.getSerializable(
            requireArguments(),
            TfrListActivity.EXTRA_TFR,
            Tfr::class.java
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mImageView = ImageZoomView(activity, null)
        mImageView!!.id = R.id.main_content
        mImageView!!.layoutParams = ViewGroup.LayoutParams(
            ActionBar.LayoutParams.MATCH_PARENT,
            ActionBar.LayoutParams.MATCH_PARENT
        )
        return createContentView(mImageView!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionBarTitle(mTfr!!.name!!)
        setActionBarSubtitle("TFR Graphic")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Events has no replay, so request the image only once subscribed,
                // or a cached image could be posted before anyone is listening.
                CoroutineIntentService.Events.events
                    .onSubscription { if (!mImageShown) requestTfrImage() }
                    .filterIsInstance<Bundle>()
                    .collect { bundle ->
                        if (bundle.getString("ACTION") == TfrImageService.ACTION_GET_TFR_IMAGE) {
                            showTfrImage(bundle.getString(TfrImageService.TFR_IMAGE_PATH))
                        }
                    }
            }
        }
    }

    private fun requestTfrImage() {
        val service = Intent(activity, TfrImageService::class.java)
        service.action = TfrImageService.ACTION_GET_TFR_IMAGE
        service.putExtra(TfrImageService.TFR_ENTRY, mTfr)
        requireActivity().startService(service)
    }

    private fun showTfrImage(path: String?) {
        val bitmap = path?.let { BitmapFactory.decodeFile(it) }
        if (bitmap != null) {
            mImageView!!.setImage(bitmap)
            mImageShown = true
            setFragmentContentShown(true)
        } else {
            showToast(requireActivity(), "Unable to show image")
        }
    }
}