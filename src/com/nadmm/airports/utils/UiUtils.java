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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;

public class UiUtils {

    private final static Handler sHandler;
    private final static Paint sPaint = new Paint( Paint.FILTER_BITMAP_FLAG );

    static {
        // Make sure to associate with the Looper in the main (Gui) thread
        sHandler = new Handler( Looper.getMainLooper() );
    }

    protected static String getPhoneNumber( TextView tv ) {
        return DataUtils.decodePhoneNumber( tv.getText().toString() );
    }

    public static void makeClickToCall( final Context context, TextView tv ) {
        PackageManager pm = context.getPackageManager();
        boolean hasTelephony = pm.hasSystemFeature( PackageManager.FEATURE_TELEPHONY );
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        String tapAction = prefs.getString( PreferencesActivity.KEY_PHONE_TAP_ACTION, "dial" );
        if ( hasTelephony && !tapAction.equals( "ignore" ) ) {
            if ( tv.getText().length() > 0 ) {
                final String action =
                        tapAction.equals( "call" )? Intent.ACTION_CALL : Intent.ACTION_DIAL;
                tv.setCompoundDrawablesWithIntrinsicBounds( R.drawable.phone, 0, 0, 0 );
                tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( context, 3 ) );
                tv.setOnClickListener( new OnClickListener() {
    
                    @Override
                    public void onClick( View v ) {
                        TextView tv = (TextView) v;
                        Intent intent = new Intent( action,
                                Uri.parse( "tel:"+getPhoneNumber( tv ) ) );
                        context.startActivity( intent );
                    }

                } );
            } else {
                tv.setCompoundDrawablesWithIntrinsicBounds( 0, 0, 0, 0 );
                tv.setOnClickListener( null );
            }
        }
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

    static public int getRowSelectorForCursor( Cursor c ) {
        int resid;
        if ( c.getCount() == 1 ) {
            resid = R.drawable.row_selector;
        } else if ( c.getPosition() == 0 ) {
            resid = R.drawable.row_selector_top;
        } else if ( c.getPosition() == c.getCount()-1 ) {
            resid = R.drawable.row_selector_bottom;
        } else {
            resid = R.drawable.row_selector_middle;
        }
        return resid;
    }

}
