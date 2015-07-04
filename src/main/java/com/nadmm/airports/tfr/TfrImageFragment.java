/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.tfr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar.LayoutParams;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.ImageZoomView;

public class TfrImageFragment extends FragmentBase {

    private Tfr mTfr;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private ImageZoomView mImageView;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mReceiver = new TfrReceiver();
        mFilter = new IntentFilter();
        mFilter.addAction( TfrImageService.ACTION_GET_TFR_IMAGE );

        Bundle args = getArguments();
        mTfr = (Tfr) args.getSerializable( TfrImageService.TFR_ENTRY );

        Intent service = new Intent( getActivity(), TfrImageService.class );
        service.setAction( TfrImageService.ACTION_GET_TFR_IMAGE );
        service.putExtra( TfrImageService.TFR_ENTRY, mTfr );
        getActivity().startService( service );
    }

    @Override
    public void onResume() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.registerReceiver( mReceiver, mFilter );

        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.unregisterReceiver( mReceiver );

        super.onPause();
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        mImageView = new ImageZoomView( getActivity(), null );
        mImageView.setId( R.id.main_content );
        mImageView.setLayoutParams( new ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );

        return createContentView( mImageView );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        setActionBarTitle( mTfr.name );
        setActionBarSubtitle( "TFR Graphic" );

        super.onActivityCreated( savedInstanceState );
    }

    private final class TfrReceiver extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            String path = (String) intent.getSerializableExtra( TfrImageService.TFR_IMAGE_PATH );
            Bitmap bitmap = null;
            if ( path != null ) {
                bitmap = BitmapFactory.decodeFile( path );
            }
            if ( bitmap != null ) {
                mImageView.setImage( bitmap );
                setFragmentContentShown( true );
            } else {
                UiUtils.showToast( getActivity(), "Unable to show image" );
            }
        }
    }

}
