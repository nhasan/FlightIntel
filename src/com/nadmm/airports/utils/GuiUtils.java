/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class GuiUtils {

    private final static Handler sHandler;
    private final static Paint sPaint = new Paint( Paint.FILTER_BITMAP_FLAG );

    static {
        // Make sure to associate with the Looper in the main (Gui) thread
        sHandler = new Handler( Looper.getMainLooper() );
    }

    public static void showToast( final Context context, final String msg ) {
        if ( msg == null ) {
            return;
        }
        sHandler.post( new Runnable () {
            @Override
            public void run() {
                Toast.makeText( context.getApplicationContext(), msg, Toast.LENGTH_LONG ).show();
            }
        } );
    }

    public static Drawable combineDrawables( Drawable d1, Drawable d2 ) {
        // Assumes both d1 & d2 are same size and square shaped
        int w = d1.getIntrinsicWidth();
        int h = d1.getIntrinsicHeight();
        Bitmap result = Bitmap.createBitmap( w+( d2!=null? w : 0 ), h, Bitmap.Config.ARGB_8888 );

        Canvas canvas = new Canvas( result );
        canvas.setDensity( Bitmap.DENSITY_NONE );
        d1.setBounds( 0, 0, w-1, h-1 );
        d1.draw( canvas );
        if ( d2 != null ) {
            canvas.translate( w, 0 );
            d2.setBounds( 0, 0, w-1, h-1 );
            d2.draw( canvas );
        }

        return new BitmapDrawable( result );
    }

    public static Drawable getRotatedDrawable( Context context, int resid, float rotation ) {
        Bitmap bmp = BitmapFactory.decodeResource( context.getResources(), resid );
        Bitmap rotated = Bitmap.createBitmap( bmp.getWidth(), bmp.getHeight(),
                Bitmap.Config.ARGB_8888 );
        Canvas canvas = new Canvas( rotated );
        canvas.setDensity( Bitmap.DENSITY_NONE );
        canvas.rotate( rotation, bmp.getWidth()/2, bmp.getHeight()/2 );
        canvas.drawBitmap( bmp, 0, 0, sPaint );
        return new BitmapDrawable( rotated );
    }

}
