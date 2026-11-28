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
package com.nadmm.airports.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.OverScroller
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * An image view that fits its image, then lets the user pinch to zoom,
 * drag and fling to pan, and double-tap to zoom in or back out. The image
 * always fills the view along any side where it is larger, and is centered
 * along any side where it is smaller.
 */
class ImageZoomView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    // Fits the image in the view; the user's zoom and pan go on top of it
    private val baseMatrix = Matrix()
    private val zoomMatrix = Matrix()
    private val drawMatrix = Matrix()
    private val displayRect = RectF()
    private val matrixValues = FloatArray(9)

    private val scroller = OverScroller(context)
    private var lastFlingX = 0
    private var lastFlingY = 0
    private var zoomAnimator: ValueAnimator? = null

    // False while the superclass constructor runs, before the fields above exist
    private var ready = false

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val zoom = currentZoom
                val factor = detector.scaleFactor.coerceIn(MIN_ZOOM / zoom, MAX_ZOOM / zoom)
                zoomMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                applyMatrix()
                return true
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                stopMotion()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
            ): Boolean {
                zoomMatrix.postTranslate(-distanceX, -distanceY)
                applyMatrix()
                return true
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                fling(velocityX, velocityY)
                return true
            }

            // Zoom when the second tap lifts: its press also calls onDown, which
            // would cancel an animation started on the press
            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                if (e.actionMasked == MotionEvent.ACTION_UP) {
                    val target = if (currentZoom > MIN_ZOOM + 0.01f) MIN_ZOOM else DOUBLE_TAP_ZOOM
                    animateZoomTo(target, e.x, e.y)
                }
                return true
            }
        })

    private val flingStep = object : Runnable {
        override fun run() {
            if (!scroller.computeScrollOffset()) return
            val x = scroller.currX
            val y = scroller.currY
            zoomMatrix.postTranslate((lastFlingX - x).toFloat(), (lastFlingY - y).toFloat())
            lastFlingX = x
            lastFlingY = y
            applyMatrix()
            postOnAnimation(this)
        }
    }

    init {
        scaleType = ScaleType.MATRIX
        ready = true
    }

    /** The user's zoom, where 1 is the image fitted to the view. */
    private val currentZoom: Float
        get() {
            zoomMatrix.getValues(matrixValues)
            return matrixValues[Matrix.MSCALE_X]
        }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (ready) resetZoom()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetZoom()
    }

    override fun onDetachedFromWindow() {
        stopMotion()
        super.onDetachedFromWindow()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    /** Fits the image to the view, dropping any zoom and pan. */
    private fun resetZoom() {
        stopMotion()
        val d = drawable ?: return
        if (width == 0 || height == 0 || d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) return
        baseMatrix.setRectToRect(
            RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat()),
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            Matrix.ScaleToFit.CENTER)
        zoomMatrix.reset()
        applyMatrix()
    }

    /** Keeps the image filling or centered in the view, then shows it. */
    private fun applyMatrix() {
        if (!updateDisplayRect()) return
        val dx = edgeCorrection(displayRect.left, displayRect.right, width.toFloat())
        val dy = edgeCorrection(displayRect.top, displayRect.bottom, height.toFloat())
        if (dx != 0f || dy != 0f) {
            zoomMatrix.postTranslate(dx, dy)
            drawMatrix.postTranslate(dx, dy)
        }
        imageMatrix = drawMatrix
    }

    /** Sets drawMatrix and displayRect, where the image now lands in the view. */
    private fun updateDisplayRect(): Boolean {
        val d = drawable ?: return false
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(zoomMatrix)
        displayRect.set(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        drawMatrix.mapRect(displayRect)
        return true
    }

    /** How far to move the image along one axis so it fills, or is centered on, that side. */
    private fun edgeCorrection(start: Float, end: Float, size: Float): Float {
        val length = end - start
        return when {
            length <= size -> (size - length) / 2 - start
            start > 0 -> -start
            end < size -> size - end
            else -> 0f
        }
    }

    private fun fling(velocityX: Float, velocityY: Float) {
        if (!updateDisplayRect()) return
        // Scroll position is how far the image's left and top edges are past the view's
        val startX = (-displayRect.left).roundToInt()
        val startY = (-displayRect.top).roundToInt()
        val maxX = max(0, (displayRect.width() - width).roundToInt())
        val maxY = max(0, (displayRect.height() - height).roundToInt())
        if (maxX == 0 && maxY == 0) return
        lastFlingX = startX
        lastFlingY = startY
        scroller.fling(startX, startY, -velocityX.roundToInt(), -velocityY.roundToInt(),
            0, maxX, 0, maxY)
        postOnAnimation(flingStep)
    }

    private fun animateZoomTo(target: Float, focusX: Float, focusY: Float) {
        stopMotion()
        zoomAnimator = ValueAnimator.ofFloat(currentZoom, target).apply {
            duration = ZOOM_ANIMATION_MS
            addUpdateListener {
                val factor = (it.animatedValue as Float) / currentZoom
                zoomMatrix.postScale(factor, factor, focusX, focusY)
                applyMatrix()
            }
            start()
        }
    }

    private fun stopMotion() {
        scroller.forceFinished(true)
        removeCallbacks(flingStep)
        zoomAnimator?.cancel()
        zoomAnimator = null
    }

    private companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 8f
        const val DOUBLE_TAP_ZOOM = 3f
        const val ZOOM_ANIMATION_MS = 250L
    }
}
