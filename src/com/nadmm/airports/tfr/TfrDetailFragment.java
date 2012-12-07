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

package com.nadmm.airports.tfr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ImageViewActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.TimeUtils;

public class TfrDetailFragment extends FragmentBase {

    private Tfr mTfr;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        mReceiver = new TfrReceiver();
        mFilter = new IntentFilter();
        mFilter.addAction( TfrImageService.ACTION_GET_TFR_IMAGE );

        super.onCreate( savedInstanceState );
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
        Context context = getActivity();
        View view = inflater.inflate( R.layout.tfr_detail_view, container, false );

        Button btnGraphic = (Button) view.findViewById( R.id.btnViewGraphic );
        btnGraphic.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                requestTfrGraphic();
            }
        } );

        Bundle args = getArguments();
        mTfr = (Tfr) args.getSerializable( TfrActivity.EXTRA_TFR );

        LinearLayout layout = (LinearLayout) view.findViewById( R.id.tfr_header_layout );
        addRow( layout, "Name", mTfr.name );
        addRow( layout, "NOTAM", mTfr.notamId );
        addRow( layout, "Type", mTfr.type );
        addRow( layout, "Status", mTfr.isExpired()? "Expired"
                : mTfr.isActive()? "Active" : "Inactive" );
        addRow( layout, "Time", mTfr.formatTimeRange( context ) );
        addRow( layout, "Altitudes", mTfr.formatAltitudeRange() );

        layout = (LinearLayout) view.findViewById( R.id.tfr_time_layout );
        if ( mTfr.createTime < Long.MAX_VALUE ) {
            addRow( layout, "Created", TimeUtils.formatDateTimeYear( context, mTfr.createTime ) );
        }
        if ( mTfr.modifyTime < Long.MAX_VALUE && mTfr.modifyTime > mTfr.createTime ) {
            addRow( layout, "Modified", TimeUtils.formatDateTimeYear( context, mTfr.modifyTime ) );
        }

        TextView tv = (TextView) view.findViewById( R.id.tfr_text_view );
        tv.setText( mTfr.comment.replace( "\\n", "\n" ) );

        tv = (TextView) view.findViewById( R.id.tfr_warning_text );
        tv.setText( "Depicted TFR data may not be a complete listing. Pilots should not use "
        		+ "the information for flight planning purposes. For the latest information, "
        		+ "call your local Flight Service Station at 1-800-WX-BRIEF." );

        setContentShown( true );

        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility( false );
        setActionBarTitle( "TFR Details" );
        setActionBarSubtitle( mTfr.name );

        super.onActivityCreated( savedInstanceState );
    }

    private void requestTfrGraphic() {
        Button btnGraphic = (Button) findViewById( R.id.btnViewGraphic );
        btnGraphic.setEnabled( false );
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility( true );

        Intent service = new Intent( getActivity(), TfrImageService.class );
        service.setAction( TfrImageService.ACTION_GET_TFR_IMAGE );
        service.putExtra( TfrImageService.TFR_ENTRY, mTfr );
        getActivity().startService( service );
    }

    private final class TfrReceiver extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            String path = (String) intent.getSerializableExtra( TfrImageService.TFR_IMAGE_PATH );
            if ( path != null ) {
                Intent activity = new Intent( getActivity(), ImageViewActivity.class );
                activity.putExtra( ImageViewActivity.IMAGE_PATH, path );
                activity.putExtra( ImageViewActivity.IMAGE_TITLE, "TFR Graphic" );
                activity.putExtra( ImageViewActivity.IMAGE_SUBTITLE, mTfr.name );
                startActivity( activity );
            }

            getSherlockActivity().setSupportProgressBarIndeterminateVisibility( false );
            Button btnGraphic = (Button) findViewById( R.id.btnViewGraphic );
            btnGraphic.setEnabled( true );
        }
    }

}
