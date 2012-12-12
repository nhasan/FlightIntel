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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.SystemUtils;

public class ScratchPadActivity extends ActivityBase {

    private static final String DIR_NAME = "scratchpad";
    private static final String FILE_NAME = "scratchpad.png";

    private FreeHandDrawView mView;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.scratchpad_view );
        mView = (FreeHandDrawView) findViewById( R.id.drawing );

        ImageButton draw = (ImageButton) findViewById( R.id.action_draw );
        draw.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                mView.setDrawMode();
            }
        } );

        ImageButton erase = (ImageButton) findViewById( R.id.action_erase );
        erase.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                mView.setEraseMode();
            }
        } );

        ImageButton discard = (ImageButton) findViewById( R.id.action_discard );
        discard.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                mView.discardBitmap();
            }
        } );

        ImageButton share = (ImageButton) findViewById( R.id.action_share );
        share.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                saveBitmap();
                Intent intent = new Intent( Intent.ACTION_SEND );
                intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET );
                intent.setType( "image/*" );
                Uri uri = Uri.fromFile( SystemUtils.getExternalFile( DIR_NAME, FILE_NAME ) );
                intent.putExtra( Intent.EXTRA_STREAM, uri );
                startActivity( Intent.createChooser( intent, "Share Scratchpad" ) );
            }
        } );
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Bitmap bitmap = mView.getBitmap();
        bitmap.recycle();
    }

    private void saveBitmap() {
        try {
            File file = SystemUtils.getExternalFile( DIR_NAME, FILE_NAME );
            FileOutputStream stream = new FileOutputStream( file );
            Bitmap bitmap = mView.getBitmap();
            bitmap.compress( CompressFormat.PNG, 0, stream );
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
                bitmap.recycle();
            }
        } catch ( FileNotFoundException e ) {
        }
    }

}
