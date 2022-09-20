/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.UiUtils.showToast
import com.nadmm.airports.views.ImageZoomView

class TfrImageFragment : FragmentBase() {
    private var mTfr: Tfr? = null
    private var mReceiver: BroadcastReceiver? = null
    private var mFilter: IntentFilter? = null
    private var mImageView: ImageZoomView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mReceiver = TfrReceiver()
        mFilter = IntentFilter()
        mFilter!!.addAction(TfrImageService.ACTION_GET_TFR_IMAGE)
        val args = arguments
        mTfr = args!!.getSerializable(TfrListActivity.EXTRA_TFR) as Tfr?
        val service = Intent(activity, TfrImageService::class.java)
        service.action = TfrImageService.ACTION_GET_TFR_IMAGE
        service.putExtra(TfrImageService.TFR_ENTRY, mTfr)
        requireActivity().startService(service)
    }

    override fun onResume() {
        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.registerReceiver(mReceiver!!, mFilter!!)
        super.onResume()
    }

    override fun onPause() {
        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.unregisterReceiver(mReceiver!!)
        super.onPause()
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle(mTfr!!.name!!)
        setActionBarSubtitle("TFR Graphic")
    }

    private inner class TfrReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val path = intent.getSerializableExtra(TfrImageService.TFR_IMAGE_PATH) as String?
            var bitmap: Bitmap? = null
            if (path != null) {
                bitmap = BitmapFactory.decodeFile(path)
            }
            if (bitmap != null) {
                mImageView!!.setImage(bitmap)
                setFragmentContentShown(true)
            } else {
                showToast(activity!!, "Unable to show image")
            }
        }
    }
}