/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.notams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public class NotamActivityBase extends ActivityBase {

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.dtpp_detail_view ) );

        mFilter = new IntentFilter();
        mFilter.addAction( NotamService.ACTION_GET_NOTAM );

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive( Context context, Intent intent ) {
                handleNotamBroadcast( intent );
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver( mReceiver, mFilter );
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver( mReceiver );
    }

    protected void handleNotamBroadcast( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( NotamService.ACTION_GET_NOTAM ) ) {
            String path = intent.getStringExtra( NotamService.NOTAM_PATH );
            if ( path != null ) {
                File notamFile = new File( path );
                showNotams( notamFile );
            }
        }
    }

    protected void getNotams( String icaoCode ) {
        Intent service = new Intent( this, NotamService.class );
        service.setAction( NotamService.ACTION_GET_NOTAM );
        service.putExtra( NotamService.ICAO_CODE, icaoCode );
        startService( service );
    }

    protected void showNotams( File notamFile ) {
        setContentShown( true );

        LinearLayout content = (LinearLayout) findViewById( R.id.notam_content_layout );

        HashMap<String, ArrayList<String>> notams = parseNotams( notamFile );
        if ( notams == null ) {
            // NOTAM cache file is missing. Must be an issue with fetching from FAA
            TextView title1 = (TextView) findViewById( R.id.notam_title1 );
            title1.setText( "Unable to show NOTAMs at this moment" );
            return;
        }

        int count = 0;
        // Get subjects in specific order
        String[] subjects = DataUtils.getNotamSubjects();
        for ( String subject : subjects ) {
            if ( !notams.containsKey( subject ) ) {
                continue;
            }
            TextView label = new TextView( this );
            label.setPadding(
                    UiUtils.convertDpToPx( this, 6 ),
                    UiUtils.convertDpToPx( this, 12 ),
                    UiUtils.convertDpToPx( this, 6 ),
                    0 );
            label.setTextAppearance( this, R.style.TextSmall_Bold );
            label.setText( subject );
            content.addView( label, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
            addSeparator( content );
            ArrayList<String> list = notams.get( subject );
            for ( String notam : list ) {
                addBulletedRow( content, notam );
                addSeparator( content );
                ++count;
            }
        }

        TextView title1 = (TextView) findViewById( R.id.notam_title1 );
        title1.setText( getResources().getQuantityString( R.plurals.notams_found, count, count ) );
        TextView title2 = (TextView) findViewById( R.id.notam_title2 );
        Date lastModified = new Date( notamFile.lastModified() );
        title2.setText( "Last updated at "+ TimeUtils.formatDateTime(
                this, lastModified.getTime() ) );
    }

    private HashMap<String, ArrayList<String>> parseNotams( File notamFile ) {
        // Remeber the ids we have seen to remove duplicates
        HashMap<String, ArrayList<String>> notams = new HashMap<String, ArrayList<String>>();
        BufferedReader in = null;

        // Build the NOTAM list group by the subject
        try {
            Set<String> notamIDs = new HashSet<String>();
            in = new BufferedReader( new FileReader( notamFile ) );
            String notam;
            while ( ( notam = in.readLine() ) != null ) {
                String parts[] = notam.split( " ", 5 );
                String notamID = parts[ 1 ];
                if ( !notamIDs.contains( notamID ) ) {
                    String keyword = parts[0].equals( "!FDC" )? "FDC" : parts[ 3 ];
                    String subject = DataUtils.getNotamSubjectFromKeyword( keyword );
                    ArrayList<String> list = notams.get( subject );
                    if ( list == null ) {
                        list = new ArrayList<String>();
                    }
                    list.add( notam );
                    notams.put( subject, list );
                    notamIDs.add( notamID );
                }
            }
        } catch ( IOException e ) {
        } finally {
            try {
                if ( in != null ) {
                    in.close();
                }
            } catch ( IOException e ) {
            }
        }

        return notams;
    }

}
