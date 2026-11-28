/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nadmm.airports.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nadmm.airports.R

/**
 * A SwipeRefreshLayout that draws an optional foreground drawable and lets
 * a callback decide whether its content can still scroll up.
 */
class MultiSwipeRefreshLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    fun interface CanChildScrollUpCallback {
        fun canSwipeRefreshChildScrollUp(): Boolean
    }

    var canChildScrollUpCallback: CanChildScrollUpCallback? = null

    private var foregroundDrawable: Drawable? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.MultiSwipeRefreshLayout) {
            foregroundDrawable = getDrawable(R.styleable.MultiSwipeRefreshLayout_foreground)?.also {
                it.callback = this@MultiSwipeRefreshLayout
                setWillNotDraw(false)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        foregroundDrawable?.setBounds(0, 0, w, h)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        foregroundDrawable?.draw(canvas)
    }

    override fun canChildScrollUp(): Boolean =
        canChildScrollUpCallback?.canSwipeRefreshChildScrollUp() ?: super.canChildScrollUp()
}
