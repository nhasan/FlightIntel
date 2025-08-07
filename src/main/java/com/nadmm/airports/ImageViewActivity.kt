/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import com.nadmm.airports.utils.UiUtils
import com.nadmm.airports.views.ImageZoomView

open class ImageViewActivity : FragmentActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.let { args ->
            args.getString(IMAGE_TITLE)?.let { title ->
                setActionBarTitle(title)
            }
            args.getString(IMAGE_SUBTITLE)?.let { subtitle ->
                setActionBarSubtitle(subtitle)
            }
            addFragment(ImageViewFragment::class.java, args)
        }
    }

    class ImageViewFragment : FragmentBase() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = ImageZoomView(activity, null)
            view.setId(R.id.main_content)
            view.setLayoutParams(
                ViewGroup.LayoutParams(
                    ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT
                )
            )
            arguments?.let { args ->
                val path = args.getString(IMAGE_PATH)
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    view.setImage(bitmap)
                } else {
                    UiUtils.showToast(requireActivity(), "Unable to show image")
                }
            }
            return view
        }
    }

    companion object {
        const val IMAGE_TITLE: String = "IMAGE_TITLE"
        const val IMAGE_SUBTITLE: String = "IMAGE_SUBTITLE"
        const val IMAGE_PATH: String = "IMAGE_PATH"
    }
}
