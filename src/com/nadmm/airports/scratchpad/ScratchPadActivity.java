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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.utils.SystemUtils;

public class ScratchPadActivity extends ActivityBase {

    private static final String DIR_NAME = "scratchpad";
    private static final String FILE_NAME = "scratchpad.png";

    private FreeHandDrawView mView;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mView = new FreeHandDrawView( this );
        setContentView( mView );
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveBitmap();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadBitmap();
    }

    private void saveBitmap() {
        try {
            File file = SystemUtils.getExternalFile( DIR_NAME, FILE_NAME );
            FileOutputStream stream = new FileOutputStream( file );
            Bitmap bitmap = mView.getBitmap();
            bitmap.compress( CompressFormat.PNG, 0, stream );
            bitmap.recycle();
        } catch ( FileNotFoundException e ) {
        }
    }

    private void loadBitmap() {
        try {
            File file = SystemUtils.getExternalFile( DIR_NAME, FILE_NAME );
            if ( file.exists() ) {
                FileInputStream stream = new FileInputStream( file );
                Bitmap bitmap = BitmapFactory.decodeStream( stream );
                mView.setBitmap( bitmap );
            }
        } catch ( FileNotFoundException e ) {
        }
    }

}
