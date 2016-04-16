/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2016 Nadeem Hasan <nhasan@nadmm.com>
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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.LruCache;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.R;

import java.util.Locale;

public class UiUtils {

    private static final LruCache<String, Drawable> sDrawableCache = new LruCache<>( 100 );

    private final static Handler sHandler;
    private final static Paint sPaint = new Paint( Paint.FILTER_BITMAP_FLAG );

    static {
        // Make sure to associate with the Looper in the main (Gui) thread
        sHandler = new Handler( Looper.getMainLooper() );
    }

    public static Drawable getDrawableFromCache( String key ){
        return sDrawableCache.get( key );
    }

    public static void putDrawableIntoCache( String key, Drawable d ) {
        if ( sDrawableCache.get( key ) == null ) {
            sDrawableCache.put( key, d );
        }
    }

    public static int convertDpToPx( Context context, float dp ) {
        return (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
                dp, context.getResources().getDisplayMetrics() );
    }

    public static void showToast( final Context context, final String msg ) {
        showToast( context, msg, Toast.LENGTH_LONG );
    }

    public static void showToast( final Context context, final String msg, final int duration ) {
        if ( msg == null ) {
            return;
        }
        sHandler.post( new Runnable() {
            @Override
            public void run() {
                Toast.makeText( context.getApplicationContext(), msg, duration ).show();
            }
        } );
    }

    public static Drawable combineDrawables( Context context, Drawable d1, Drawable d2,
            int paddingDp ) {
        // Assumes both d1 & d2 are same size and square shaped
        int w = d1.getIntrinsicWidth();
        int h = d1.getIntrinsicHeight();
        int paddingPx = convertDpToPx( context, paddingDp );
        Bitmap result = Bitmap.createBitmap( w+( d2!=null? w+paddingPx : 0 ), h,
                Bitmap.Config.ARGB_8888 );

        Canvas canvas = new Canvas( result );
        canvas.setDensity( Bitmap.DENSITY_NONE );
        d1.setBounds( 0, 0, w-1, h-1 );
        d1.draw( canvas );
        if ( d2 != null ) {
            canvas.translate( w+paddingPx, 0 );
            d2.setBounds( 0, 0, w-1, h-1 );
            d2.draw( canvas );
        }

        return new BitmapDrawable( context.getResources(), result );
    }

    public static Drawable getRotatedDrawable( Context context, int resid, float rotation ) {
        String key = String.format( Locale.US, "%d:%d", resid, (int) rotation );
        Drawable d = getDrawableFromCache( key );
        if ( d == null ) {
            Resources res = context.getResources();
            Bitmap bmp = BitmapFactory.decodeResource( res, resid );
            Bitmap rotated = Bitmap.createBitmap( bmp.getWidth(), bmp.getHeight(),
                    Bitmap.Config.ARGB_8888 );
            Canvas canvas = new Canvas( rotated );
            canvas.setDensity( Bitmap.DENSITY_NONE );
            canvas.rotate( rotation, bmp.getWidth()/2, bmp.getHeight()/2 );
            canvas.drawBitmap( bmp, 0, 0, sPaint );
            d = new BitmapDrawable( res, rotated );
            putDrawableIntoCache( key, d );
        }
        return d;
    }

    static public void setTextViewDrawable( TextView tv, int resid ) {
        String key = String.format( Locale.US, "%d", resid );
        Drawable d = getDrawableFromCache( key );
        if ( d == null ) {
            Resources res = tv.getResources();
            d = ResourcesCompat.getDrawable( res, resid, null );
            putDrawableIntoCache( key, d );
        }
        setTextViewDrawable( tv, d.mutate() );
    }

    static public void setTextViewDrawable( TextView tv, Drawable d ) {
        tv.setCompoundDrawablesWithIntrinsicBounds( d, null, null, null );
        tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( tv.getContext(), 6 ) );
    }

    static public void removeTextViewDrawable( TextView tv ) {
        tv.setCompoundDrawablesWithIntrinsicBounds( 0, 0, 0, 0 );
    }

    static public Drawable getTintedDrawable( Context context, int resid, int color ) {
        // Get a mutable copy of the drawable so each can be set to a different color
        String key = String.format( Locale.US, "%d:%d", resid, color );
        Drawable d = getDrawableFromCache( key );
        if ( d == null ) {
            d = ResourcesCompat.getDrawable( context.getResources(), resid, null ).mutate();
            DrawableCompat.setTint( d, color );
            putDrawableIntoCache( key, d );
        }
        return d;
    }

    static public Drawable getDefaultTintedDrawable( Context context, int resid ) {
        TypedValue value = new TypedValue();
        ColorStateList tintList = null;
        if ( context.getTheme().resolveAttribute(
                android.R.attr.textColorSecondary, value, true ) ) {
            tintList = context.getResources().getColorStateList( value.resourceId );
        }

        return getTintedDrawable( context, resid, tintList );
    }

    static public Drawable getTintedDrawable( Context context, int resid, ColorStateList tintList ) {
        // Get a mutable copy of the drawable so each can be set to a different color
        String key = String.format( Locale.US, "%d:%d", resid, tintList.getDefaultColor() );
        Drawable d = getDrawableFromCache( key );
        if ( d == null ) {
            d = ResourcesCompat.getDrawable( context.getResources(), resid, null ).mutate();
            DrawableCompat.setTintList( d, tintList );
            putDrawableIntoCache( key, d );
        }
        return d;
    }

    static public void setTintedTextViewDrawable( TextView tv, int resid, int color ) {
        Drawable d = getTintedDrawable( tv.getContext(), resid, color );
        setTextViewDrawable( tv, d );
    }

    static public void setDefaultTintedTextViewDrawable( TextView tv, int resid ) {
        Drawable d = getDefaultTintedDrawable( tv.getContext(), resid );
        setTextViewDrawable( tv, d );
    }

    static public void setTintedTextViewDrawable( TextView tv, int resid, ColorStateList tintList ) {
        Drawable d = getTintedDrawable( tv.getContext(), resid, tintList );
        setTextViewDrawable( tv, d );
    }

    public static void setRunwayDrawable( Context context, TextView tv, String runwayId,
            int length, int heading ) {
        int resid;
        if ( runwayId.startsWith( "H" ) ) {
            resid = R.drawable.helipad;
        } else {
            if ( length > 10000 ) {
                resid = R.drawable.runway9;
            } else if ( length > 9000 ) {
                resid = R.drawable.runway8;
            } else if ( length > 8000 ) {
                resid = R.drawable.runway7;
            } else if ( length > 7000 ) {
                resid = R.drawable.runway6;
            } else if ( length > 6000 ) {
                resid = R.drawable.runway5;
            } else if ( length > 5000 ) {
                resid = R.drawable.runway4;
            } else if ( length > 4000 ) {
                resid = R.drawable.runway3;
            } else if ( length > 3000 ) {
                resid = R.drawable.runway2;
            } else if ( length > 2000 ) {
                resid = R.drawable.runway1;
            } else {
                resid = R.drawable.runway0;
            }
        }

        Drawable d = UiUtils.getRotatedDrawable( context, resid, heading );
        tv.setCompoundDrawablesWithIntrinsicBounds( d, null, null, null );
        tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( context, 5 ) );
    }

    private static final int[] RES_IDS_ACTION_BAR_SIZE = { R.attr.actionBarSize };

    /** Calculates the Action Bar height in pixels. */
    public static int calculateActionBarSize( Context context ) {
        if ( context == null ) {
            return 0;
        }

        Resources.Theme curTheme = context.getTheme();
        if ( curTheme == null ) {
            return 0;
        }

        TypedArray att = curTheme.obtainStyledAttributes( RES_IDS_ACTION_BAR_SIZE );
        if ( att == null ) {
            return 0;
        }

        float size = att.getDimension( 0, 0 );
        att.recycle();
        return (int) size;
    }

    public static int getSelectableItemBackgroundResource( Context context ) {
        int[] attrs = new int[]{ R.attr.selectableItemBackground };
        TypedArray typedArray = context.obtainStyledAttributes( attrs );
        int res = typedArray.getResourceId( 0, 0 );
        typedArray.recycle();
        return res;
    }

}
