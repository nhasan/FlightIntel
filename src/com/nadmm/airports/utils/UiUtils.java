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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.R;

public class UiUtils {

    private final static Handler sHandler;
    private final static Paint sPaint = new Paint( Paint.FILTER_BITMAP_FLAG );

    static {
        // Make sure to associate with the Looper in the main (Gui) thread
        sHandler = new Handler( Looper.getMainLooper() );
    }

    public static int convertDpToPx( Context context, int dp ) {
        return (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
                dp, context.getResources().getDisplayMetrics() );
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

    public static Drawable combineDrawables( Context context, Drawable d1, Drawable d2 ) {
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

        return new BitmapDrawable( context.getResources(), result );
    }

    public static Drawable getRotatedDrawable( Context context, int resid, float rotation ) {
        Resources res = context.getResources();
        Bitmap bmp = BitmapFactory.decodeResource( res, resid );
        Bitmap rotated = Bitmap.createBitmap( bmp.getWidth(), bmp.getHeight(),
                Bitmap.Config.ARGB_8888 );
        Canvas canvas = new Canvas( rotated );
        canvas.setDensity( Bitmap.DENSITY_NONE );
        canvas.rotate( rotation, bmp.getWidth()/2, bmp.getHeight()/2 );
        canvas.drawBitmap( bmp, 0, 0, sPaint );
        return new BitmapDrawable( res, rotated );
    }

    static public void setTextViewDrawable( TextView tv, int resid ) {
        Resources res = tv.getResources();
        Drawable d = res.getDrawable( resid ).mutate();
        setTextViewDrawable( tv, d );
    }

    static public void setTextViewDrawable( TextView tv, Drawable d ) {
        DisplayMetrics dm = tv.getResources().getDisplayMetrics();
        tv.setCompoundDrawablesWithIntrinsicBounds( d, null, null, null );
        tv.setCompoundDrawablePadding( (int) ( dm.density*6+0.5 ) );
    }

    static public Drawable getColorizedDrawable( Resources res, int resid, int color ) {
        Drawable d = res.getDrawable( resid ).mutate();
        d.setColorFilter( color, PorterDuff.Mode.SRC_ATOP );
        return d;
    }

    static public void setColorizedTextViewDrawable( TextView tv, int resid, int color ) {
        Resources res = tv.getResources();
        Drawable d = getColorizedDrawable( res, resid, color );
        setTextViewDrawable( tv, d );
    }

    static public int getRowSelectorForCursor( Cursor c ) {
        int resid;
        if ( c.getCount() == 1 ) {
            resid = R.drawable.row_selector;
        } else if ( c.isFirst() ) {
            resid = R.drawable.row_selector_top;
        } else if ( c.isLast() ) {
            resid = R.drawable.row_selector_bottom;
        } else {
            resid = R.drawable.row_selector_middle;
        }
        return resid;
    }

    static public int getRowSelector( int row, int count ) {
        int resid;
        if ( count == 1 ) {
            resid = R.drawable.row_selector;
        } else if ( row == 0 ) {
            resid = R.drawable.row_selector_top;
        } else if ( row == count-1 ) {
            resid = R.drawable.row_selector_bottom;
        } else {
            resid = R.drawable.row_selector_middle;
        }
        return resid;
    }

}
