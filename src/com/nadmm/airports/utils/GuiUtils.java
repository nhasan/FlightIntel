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
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class GuiUtils {

    private final static Handler mHandler;

    static {
        // Make sure to associate with the Looper in the main (Gui) thread
        mHandler = new Handler( Looper.getMainLooper() );
    }

    public static void showToast( final Context context, final String msg ) {
        if ( msg == null ) {
            return;
        }
        mHandler.post( new Runnable () {
            @Override
            public void run() {
                Toast.makeText( context.getApplicationContext(), msg, Toast.LENGTH_LONG ).show();
            }
        } );
    }

}
