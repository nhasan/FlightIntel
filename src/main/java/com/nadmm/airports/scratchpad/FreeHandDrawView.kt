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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.MotionEvent
import com.nadmm.airports.utils.UiUtils
import android.graphics.*
import com.nadmm.airports.R
import kotlin.math.abs

class FreeHandDrawView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    interface EventListener {
        fun actionDown()
        fun actionUp()
    }

    private val mPaperColor: Int
    private val mPenColor: Int
    private val mBitmap: Bitmap
    private val mCanvas: Canvas
    private val mPath: Path
    private val mFingerPaint: Paint
    private val mBitmapPaint: Paint
    private var mLastX = 0f
    private var mLastY = 0f
    private val mBlurFilter: MaskFilter
    private var mEventListener: EventListener? = null
    private val mStrokeWidth: Int
    private val mEraseWidth: Int
    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(mBitmap, 0f, 0f, mBitmapPaint)
        canvas.drawPath(mPath, mFingerPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }
        return true
    }

    private fun touchStart(x: Float, y: Float) {
        mEventListener?.actionDown()
        mPath.reset()
        mPath.moveTo(x, y)
        mLastX = x
        mLastY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mLastX)
        val dy = abs(y - mLastY)
        if (dx >= mStrokeWidth || dy >= mStrokeWidth) {
            mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2)
            mLastX = x
            mLastY = y
        }
    }

    private fun touchUp() {
        mEventListener?.actionUp()
        if (mPath.isEmpty) {
            // If this was just a touch, make sure to draw a point
            mPath.addCircle(mLastX, mLastY, (mStrokeWidth / 2).toFloat(), Path.Direction.CW)
        } else {
            // Finish up the path
            mPath.lineTo(mLastX, mLastY)
        }
        mCanvas.drawPath(mPath, mFingerPaint)
        mPath.reset()
    }

    var bitmap: Bitmap?
        get() = mBitmap
        set(bitmap) {
            mCanvas.drawBitmap(bitmap!!, 0f, 0f, mBitmapPaint)
        }

    fun discardBitmap() {
        mBitmap.eraseColor(mPaperColor)
        invalidate()
    }

    fun setDrawMode() {
        mFingerPaint.color = mPenColor
        mFingerPaint.maskFilter = null
        mFingerPaint.strokeWidth = mStrokeWidth.toFloat()
    }

    fun setEraseMode() {
        mFingerPaint.color = mPaperColor
        mFingerPaint.maskFilter = mBlurFilter
        mFingerPaint.strokeWidth = mEraseWidth.toFloat()
    }

    fun setEventListener(listener: EventListener?) {
        mEventListener = listener
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FreeHandDrawView)
        mPenColor = ta.getColor(R.styleable.FreeHandDrawView_penColor, Color.WHITE)
        mPaperColor = ta.getColor(R.styleable.FreeHandDrawView_paperColor, Color.DKGRAY)
        ta.recycle()
        mPath = Path()
        mBitmapPaint = Paint(Paint.DITHER_FLAG)
        mBlurFilter = BlurMaskFilter(8F, BlurMaskFilter.Blur.SOLID)
        mStrokeWidth = UiUtils.convertDpToPx(context, 2f)
        mEraseWidth = 10 * mStrokeWidth
        mFingerPaint = Paint()
        mFingerPaint.isAntiAlias = true
        mFingerPaint.isDither = true
        mFingerPaint.style = Paint.Style.STROKE
        mFingerPaint.strokeJoin = Paint.Join.ROUND
        mFingerPaint.strokeCap = Paint.Cap.ROUND
        setDrawMode()
        val res = context.resources
        val dm = res.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val size = w.coerceAtLeast(h)
        mBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        mBitmap.eraseColor(mPaperColor)
        mCanvas = Canvas(mBitmap)
    }
}