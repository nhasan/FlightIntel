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

package com.nadmm.airports.utils;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FlightIntel;
import com.nadmm.airports.R;

public class ExternalStorageActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.external_storage_view );

        TextView tv = (TextView) findViewById( R.id.storage_desc_text );
        tv.setText( "This application uses external SD card for storing it's databases. " +
                "As a result, it will not function if the external SD card is not available." +
                "\n\n" +
                "The SD card is normally unavailable if the device is connected via USB to a " +
                "computer and mounted as a storage device." );
        Button btnTryNow = (Button) findViewById( R.id.btn_trynow );
        btnTryNow.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                tryAgain();
            }
        } );
    }

    @Override
    protected void onResume() {
        super.onResume();
        externalStorageStatusChanged();
    }

    @Override
    protected void externalStorageStatusChanged() {
        if ( SystemUtils.isExternalStorageAvailable() ) {
            TextView tv = (TextView) findViewById( R.id.storage_status_text );
            tv.setText( "External SD card is available for use" );
            tv = (TextView) findViewById( R.id.storage_desc_text2 );
            tv.setText( "You should be able to use the application at this time." );
            tv.setTextColor( Color.GREEN );
            Button btnTryNow = (Button) findViewById( R.id.btn_trynow );
            btnTryNow.setVisibility( View.VISIBLE );
        } else {
            TextView tv = (TextView) findViewById( R.id.storage_status_text );
            tv.setText( "External SD card is not available for use" );
            tv = (TextView) findViewById( R.id.storage_desc_text2 );
            tv.setText( "Please disconnect or unmount the device from the computer." );
            tv.setTextColor( Color.RED );
            Button btnTryNow = (Button) findViewById( R.id.btn_trynow );
            btnTryNow.setVisibility( View.GONE );
        }
    }

    protected void tryAgain() {
        Intent intent = new Intent( this, FlightIntel.class );
        startActivity( intent );
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        return false;
    }

}
