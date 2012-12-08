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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

public class FreeHandDrawView extends View {

    private static final int STROKE_WIDTH = 4;

    private Bitmap mBitmap;
    private Bitmap mInitialBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mFingerPaint;
    private Paint mBitmapPaint;
    private float mLastX;
    private float mLastY;

    public FreeHandDrawView( Context context ) {
        super( context );

        mPath = new Path();
        mBitmapPaint = new Paint( Paint.DITHER_FLAG );

        mFingerPaint = new Paint();
        mFingerPaint.setAntiAlias( true );
        mFingerPaint.setDither( true );
        mFingerPaint.setColor( 0xFF000000 );
        mFingerPaint.setStyle( Paint.Style.STROKE );
        mFingerPaint.setStrokeJoin( Paint.Join.ROUND );
        mFingerPaint.setStrokeCap( Paint.Cap.ROUND );
        mFingerPaint.setStrokeWidth( STROKE_WIDTH );
    }

    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
        super.onSizeChanged( w, h, oldw, oldh );
        if ( mBitmap != null ) {
            mBitmap.recycle();
        }
        mBitmap = Bitmap.createBitmap( w, h, Bitmap.Config.ARGB_8888 );
        mCanvas = new Canvas( mBitmap );
        if ( mInitialBitmap != null ) {
            mCanvas.drawBitmap( mInitialBitmap, 0, 0, mBitmapPaint );
            mInitialBitmap.recycle();
            mInitialBitmap = null;
        }
    }

    @Override
    protected void onDraw( Canvas canvas ) {
        canvas.drawColor( 0xFFAAAAAA );
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
        mInitialBitmap = bitmap;
    }

}
