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

package com.nadmm.airports.wx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.nadmm.airports.ImageViewActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.ArrayAdapterBase;

public abstract class WxMapListActivityBase extends SherlockListActivity {

    private String mAction;
    private String mTitle;
    private BroadcastReceiver mReceiver;
    private ArrayAdapterBase mAdapter;

    public WxMapListActivityBase( String action, String title ) {
        mAction = action;
        mTitle = title;
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.list_view_layout );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action.equals( mAction ) ) {
                    String type = intent.getStringExtra( NoaaService.TYPE );
                    if ( type.equals( NoaaService.TYPE_IMAGE ) ) {
                        showWxMap( intent );
                        mAdapter.clearPendingPos();
                    }
                }
            }
        };

        mAdapter = getMapListAdapter();
        setListAdapter( mAdapter );
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( mAction );
        registerReceiver( mReceiver, filter );

        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver( mReceiver );
        super.onPause();
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position, long id ) {
        if ( mAdapter.getPendingPos() == -1 ) {
            mAdapter.setPendingPos( position );
            String code = (String) mAdapter.getItem( position );
            requestWxMap( code );
        }
    }

    protected void requestWxMap( String code ) {
        Intent service = getServiceIntent();
        service.setAction( mAction );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_IMAGE );
        service.putExtra( NoaaService.IMAGE_CODE, code );
        startService( service );
    }

    protected void showWxMap( Intent intent ) {
        String path = intent.getStringExtra( NoaaService.RESULT );
        if ( path != null ) {
            Intent view = new Intent( this, ImageViewActivity.class );
            view.putExtra( ImageViewActivity.IMAGE_PATH, path );
            view.putExtra( ImageViewActivity.IMAGE_TITLE, mTitle );
            String code = intent.getStringExtra( NoaaService.IMAGE_CODE );
            String name = mAdapter.getDisplayText( code );
            if ( name != null ) {
                view.putExtra( ImageViewActivity.IMAGE_SUBTITLE, name );
            }
            startActivity( view );
        }
    }

    protected abstract ArrayAdapterBase getMapListAdapter();

    protected abstract Intent getServiceIntent();

}
