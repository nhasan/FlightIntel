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
package com.nadmm.airports.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.collection.LruCache
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.nadmm.airports.R
import java.util.*

object UiUtils {
    private val sDrawableCache = LruCache<String, Drawable?>(100)
    private var sHandler: Handler? = null
    private val sPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    @JvmStatic
    fun getDrawableFromCache(key: String): Drawable? {
        return sDrawableCache[key]
    }

    @JvmStatic
    fun clearDrawableCache() {
        sDrawableCache.evictAll()
    }

    @JvmStatic
    fun putDrawableIntoCache(key: String, d: Drawable?) {
        if (sDrawableCache[key] == null) {
            sDrawableCache.put(key, d!!)
        }
    }

    @JvmStatic
    fun convertDpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp, context.resources.displayMetrics
        ).toInt()
    }

    @JvmStatic
    fun showToast(context: Context, msg: String?) {
        showToast(context, msg, Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun showToast(context: Context, msg: String?, duration: Int) {
        msg?.let {
            sHandler?.post { Toast.makeText(context.applicationContext, it, duration).show() }
        }
    }

    @JvmStatic
    fun combineDrawables(
        context: Context, d1: Drawable, d2: Drawable?,
        paddingDp: Int
    ): Drawable {
        // Assumes both d1 & d2 are same size and square shaped
        val w = d1.intrinsicWidth
        val h = d1.intrinsicHeight
        val paddingPx = convertDpToPx(context, paddingDp.toFloat())
        val result = Bitmap.createBitmap(
            w + if (d2 != null) w + paddingPx else 0, h,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.density = Bitmap.DENSITY_NONE
        d1.setBounds(0, 0, w - 1, h - 1)
        d1.draw(canvas)
        if (d2 != null) {
            canvas.translate((w + paddingPx).toFloat(), 0f)
            d2.setBounds(0, 0, w - 1, h - 1)
            d2.draw(canvas)
        }
        return BitmapDrawable(context.resources, result)
    }

    @JvmStatic
    fun getRotatedDrawable(context: Context, resid: Int, rotation: Float): Drawable {
        val rotate = RotateDrawable()
        rotate.drawable = getDefaultTintedDrawable(context, resid)
        rotate.fromDegrees = rotation
        rotate.toDegrees = rotation
        rotate.pivotX = .5f
        rotate.pivotY = .5f
        rotate.level = 1
        return rotate
    }

    fun getBitmap(context: Context?, drawable: VectorDrawable): Bitmap {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return bitmap
    }

    @JvmStatic
    fun setTextViewDrawable(tv: TextView, resid: Int) {
        val key = String.format(Locale.US, "%d", resid)
        var d = getDrawableFromCache(key)
        if (d == null) {
            d = getDefaultTintedDrawable(tv.context, resid)
            putDrawableIntoCache(key, d)
        }
        setTextViewDrawable(tv, d?.mutate())
    }

    @JvmStatic
    fun setTextViewDrawable(tv: TextView, d: Drawable?) {
        tv.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null)
        tv.compoundDrawablePadding = convertDpToPx(tv.context, 6f)
    }

    @JvmStatic
    fun removeTextViewDrawable(tv: TextView) {
        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }

    @JvmStatic
    fun getTintedDrawable(context: Context?, resid: Int, color: Int): Drawable {
        // Get a mutable copy of the drawable so each can be set to a different color
        val key = String.format(Locale.US, "%d:%d", resid, color)
        var d = getDrawableFromCache(key)
        if (d == null) {
            d = AppCompatResources.getDrawable(context!!, resid)!!.mutate()
            d.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                color, BlendModeCompat.SRC_ATOP)
            putDrawableIntoCache(key, d)
        }
        return d
    }

    @JvmStatic
    fun getTintedDrawable(context: Context?, resid: Int, tintList: ColorStateList?): Drawable? {
        /* Get a mutable copy of the drawable so each can be set to a different color */
        val key = String.format(Locale.US, "%d:%d", resid, tintList?.defaultColor)
        var d = getDrawableFromCache(key)
        if (d == null) {
            d = AppCompatResources.getDrawable(context!!, resid)?.mutate()
            d?.let {
                DrawableCompat.setTintList(it, tintList)
                putDrawableIntoCache(key, it)
            }
        }
        return d
    }

    @JvmStatic
    fun getDefaultTintedDrawable(context: Context, resid: Int): Drawable? {
        return getTintedDrawable(
            context, resid,
            getColorStateList(context, android.R.attr.colorControlNormal)
        )
    }

    private fun getColorStateList(context: Context, resid: Int): ColorStateList? {
        val value = TypedValue()
        var tintList: ColorStateList? = null
        if (context.theme.resolveAttribute(resid, value, true)) {
            tintList = AppCompatResources.getColorStateList(context, value.resourceId)
        }
        return tintList
    }

    @JvmStatic
    fun setTintedTextViewDrawable(tv: TextView, resid: Int, color: Int) {
        val d = getTintedDrawable(tv.context, resid, color)
        setTextViewDrawable(tv, d)
    }

    @JvmStatic
    fun setDefaultTintedTextViewDrawable(tv: TextView, resid: Int) {
        val d = getDefaultTintedDrawable(tv.context, resid)
        setTextViewDrawable(tv, d)
    }

    @JvmStatic
    fun setTintedTextViewDrawable(tv: TextView, resid: Int, tintList: ColorStateList?) {
        val d = getTintedDrawable(tv.context, resid, tintList)
        setTextViewDrawable(tv, d)
    }

    @JvmStatic
    fun setRunwayDrawable(
        context: Context, tv: TextView, runwayId: String,
        length: Int, heading: Int
    ) {
        val resid: Int = if (runwayId.startsWith("H")) {
            R.drawable.helipad_24
        } else {
            if (length > 10000) {
                R.drawable.runway9_24
            } else if (length > 9000) {
                R.drawable.runway8_24
            } else if (length > 8000) {
                R.drawable.runway7_24
            } else if (length > 7000) {
                R.drawable.runway6_24
            } else if (length > 6000) {
                R.drawable.runway5_24
            } else if (length > 5000) {
                R.drawable.runway4_24
            } else if (length > 4000) {
                R.drawable.runway3_24
            } else if (length > 3000) {
                R.drawable.runway2_24
            } else if (length > 2000) {
                R.drawable.runway1_24
            } else {
                R.drawable.runway0_24
            }
        }
        val d = getRotatedDrawable(context, resid, heading.toFloat())
        tv.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null)
        tv.compoundDrawablePadding = convertDpToPx(context, 5f)
    }

    @JvmStatic
    fun getSelectableItemBackgroundResource(context: Context): Int {
        val attrs = intArrayOf(androidx.appcompat.R.attr.selectableItemBackground)
        val typedArray = context.obtainStyledAttributes(attrs)
        val res = typedArray.getResourceId(0, 0)
        typedArray.recycle()
        return res
    }

    init {
        // Make sure to associate with the Looper in the main (Gui) thread
        sHandler = Handler(Looper.getMainLooper())
    }
}