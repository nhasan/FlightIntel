/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.scratchpad;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class FreeHandDrawView extends View {

    public interface EventListener {
        public void actionDown();
        public void actionUp();
    }

    private static final int STROKE_WIDTH = 3;
    private static final int ERASE_WIDTH = 5*STROKE_WIDTH;
    private static final int PAPER_COLOR = 0xffe0e0e0;
    private static final int PEN_COLOR = 0xff000000;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mFingerPaint;
    private Paint mBitmapPaint;
    private float mLastX;
    private float mLastY;
    private MaskFilter mBlurFilter;
    private EventListener mEventListener;

    public FreeHandDrawView( Context context, AttributeSet attrs ) {
        super( context, attrs );

        mPath = new Path();
        mBitmapPaint = new Paint( Paint.DITHER_FLAG );
        mBlurFilter = new BlurMaskFilter( 8, BlurMaskFilter.Blur.SOLID );

        mFingerPaint = new Paint();
        mFingerPaint.setAntiAlias( true );
        mFingerPaint.setDither( true );
        mFingerPaint.setStyle( Paint.Style.STROKE );
        mFingerPaint.setStrokeJoin( Paint.Join.ROUND );
        mFingerPaint.setStrokeCap( Paint.Cap.ROUND );

        setDrawMode();

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;
        int size = Math.max( w, h );
        mBitmap = Bitmap.createBitmap( size, size, Bitmap.Config.ARGB_8888 );
        mBitmap.eraseColor( PAPER_COLOR );
        mCanvas = new Canvas( mBitmap );
    }

    @Override
    protected void onDraw( Canvas canvas ) {
        canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint );
        canvas.drawPath( mPath, mFingerPaint );
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        float x = event.getX();
        float y = event.getY();

        switch ( event.getAction() ) {
            case MotionEvent.ACTION_DOWN:
                touch_start( x, y );
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move( x, y );
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    private void touch_start( float x, float y ) {
        if ( mEventListener != null ) {
            mEventListener.actionDown();
        }

        mPath.reset();
        mPath.moveTo( x, y );
        mLastX = x;
        mLastY = y;
    }

    private void touch_move( float x, float y ) {
        float dx = Math.abs( x - mLastX );
        float dy = Math.abs( y - mLastY );
        if ( dx >= STROKE_WIDTH || dy >= STROKE_WIDTH ) {
            mPath.quadTo( mLastX, mLastY, (x + mLastX)/2, (y + mLastY)/2 );
            mLastX = x;
            mLastY = y;
        }
    }

    private void touch_up() {
        if ( mEventListener != null ) {
            mEventListener.actionUp();
        }

        if ( mPath.isEmpty() ) {
            // If this was just a touch, make sure to draw a point
            mPath.addCircle( mLastX, mLastY, STROKE_WIDTH/2, Path.Direction.CW );
        } else {
            // Finish up the path
            mPath.lineTo( mLastX, mLastY );
        }
        mCanvas.drawPath( mPath, mFingerPaint );
        mPath.reset();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap( Bitmap bitmap ) {
        mCanvas.drawBitmap( bitmap, 0, 0, mBitmapPaint );
    }

    public void discardBitmap() {
        mBitmap.eraseColor( PAPER_COLOR );
        invalidate();
    }

    public void setDrawMode() {
        mFingerPaint.setColor( PEN_COLOR );
        mFingerPaint.setMaskFilter( null );
        mFingerPaint.setStrokeWidth( STROKE_WIDTH );
    }

    public void setEraseMode() {
        mFingerPaint.setColor( PAPER_COLOR );
        mFingerPaint.setMaskFilter( mBlurFilter );
        mFingerPaint.setStrokeWidth( ERASE_WIDTH );
    }

    public void setEventListener( EventListener listener ) {
        mEventListener = listener;
    }

}
