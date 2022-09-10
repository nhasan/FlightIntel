/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2022 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.scratchpad

import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import androidx.core.content.FileProvider
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.UiUtils.showToast
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

class ScratchPadFragment : FragmentBase(), FreeHandDrawView.EventListener {
    private var mDrawView: FreeHandDrawView? = null
    private var mToolbar: View? = null
    private lateinit var mFadeIn: Animation
    private lateinit var mFadeOut: Animation
    private lateinit var mImgFile: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
        mFadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
        mImgFile = SystemUtils.getExternalFile(activity, DIR_NAME, FILE_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.scratchpad_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mToolbar = findViewById(R.id.toolbar)
        mDrawView = findViewById(R.id.drawing)
        mDrawView!!.setEventListener(this)
        val draw = findViewById<ImageButton>(R.id.action_draw)
        draw!!.setOnClickListener { v: View? -> mDrawView!!.setDrawMode() }
        val erase = findViewById<ImageButton>(R.id.action_erase)
        erase!!.setOnClickListener { v: View? -> mDrawView!!.setEraseMode() }
        val discard = findViewById<ImageButton>(R.id.action_discard)
        discard!!.setOnClickListener { v: View? ->
            mDrawView!!.discardBitmap()
            mImgFile.delete()
        }
        val share = findViewById<ImageButton>(R.id.action_share)
        share!!.setOnClickListener { v: View? ->
            saveBitmap()
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.type = "image/*"
            val uri = FileProvider.getUriForFile(requireContext(),
                "com.nadmm.airports.fileprovider", mImgFile)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(intent, "Share Scratchpad"))
        }
    }

    override fun onPause() {
        super.onPause()
        saveBitmap()
    }

    override fun onResume() {
        super.onResume()
        loadBitmap()
    }

    override fun onDestroy() {
        super.onDestroy()
        val bitmap = mDrawView!!.bitmap
        bitmap!!.recycle()
    }

    override fun actionDown() {
        mToolbar?.run {
            startAnimation(mFadeOut)
            visibility = View.INVISIBLE
        }
    }

    override fun actionUp() {
        mToolbar?.run {
            startAnimation(mFadeIn)
            visibility = View.VISIBLE
        }
    }

    private fun saveBitmap() {
        try {
            val stream = FileOutputStream(mImgFile)
            val bitmap = mDrawView!!.bitmap
            bitmap!!.compress(CompressFormat.PNG, 0, stream)
        } catch (e: FileNotFoundException) {
            showToast(requireActivity(), "Unable to save scratchpad data")
        }
    }

    private fun loadBitmap() {
        if (mImgFile.exists()) {
            try {
                val stream = FileInputStream(mImgFile)
                val bitmap = BitmapFactory.decodeStream(stream)
                mDrawView!!.bitmap = bitmap
                bitmap.recycle()
            } catch (e: FileNotFoundException) {
                mImgFile.delete()
                showToast(requireActivity(), "Unable to restore scratchpad data")
            }
        }
    }

    companion object {
        private const val DIR_NAME = "tmp"
        private const val FILE_NAME = "scratchpad.png"
    }
}