/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2017 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class ScratchPadFragment extends FragmentBase implements FreeHandDrawView.EventListener {

    private static final String DIR_NAME = "scratchpad";
    private static final String FILE_NAME = "scratchpad.png";

    private FreeHandDrawView mDrawView;
    private View mToolbar;
    private Animation mFadeIn, mFadeOut;
    private File mImgFile;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mFadeIn = AnimationUtils.loadAnimation( getActivity(), R.anim.fade_in );
        mFadeOut = AnimationUtils.loadAnimation( getActivity(), R.anim.fade_out );
        mImgFile = SystemUtils.getExternalFile( getActivity(), DIR_NAME, FILE_NAME );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflater.inflate( R.layout.scratchpad_view, container, false );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mToolbar = findViewById( R.id.toolbar );
        mDrawView = findViewById( R.id.drawing );
        mDrawView.setEventListener( this );

        ImageButton draw = findViewById( R.id.action_draw );
        draw.setOnClickListener( v -> mDrawView.setDrawMode() );

        ImageButton erase = findViewById( R.id.action_erase );
        erase.setOnClickListener( v -> mDrawView.setEraseMode() );

        ImageButton discard = findViewById( R.id.action_discard );
        discard.setOnClickListener( v -> {
            mDrawView.discardBitmap();
            mImgFile.delete();
        } );

        ImageButton share = findViewById( R.id.action_share );
        share.setOnClickListener( v -> {
            saveBitmap();
            Intent intent = new Intent( Intent.ACTION_SEND );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            intent.setType( "image/*" );
            Uri uri = Uri.fromFile( mImgFile );
            intent.putExtra( Intent.EXTRA_STREAM, uri );
            startActivity( Intent.createChooser( intent, "Share Scratchpad" ) );
        } );
    }

    @Override
    public void onPause() {
        super.onPause();

        saveBitmap();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadBitmap();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Bitmap bitmap = mDrawView.getBitmap();
        bitmap.recycle();
    }

    @Override
    public void actionDown() {
        mToolbar.startAnimation( mFadeOut );
        mToolbar.setVisibility( View.INVISIBLE );
    }

    @Override
    public void actionUp() {
        mToolbar.startAnimation( mFadeIn );
        mToolbar.setVisibility( View.VISIBLE );
    }

    private void saveBitmap() {
        try {
            FileOutputStream stream = new FileOutputStream( mImgFile );
            Bitmap bitmap = mDrawView.getBitmap();
            bitmap.compress( CompressFormat.PNG, 0, stream );
        } catch ( FileNotFoundException e ) {
            UiUtils.showToast( getActivity(), "Unable to save scratchpad data" );
        }
    }

    private void loadBitmap() {
        if ( mImgFile.exists() ) {
            try {
                FileInputStream stream = new FileInputStream( mImgFile );
                Bitmap bitmap = BitmapFactory.decodeStream( stream );
                mDrawView.setBitmap( bitmap );
                bitmap.recycle();
            } catch ( FileNotFoundException e ) {
                mImgFile.delete();
                UiUtils.showToast( getActivity(), "Unable to restore scratchpad data" );
            }
        }
    }
}
