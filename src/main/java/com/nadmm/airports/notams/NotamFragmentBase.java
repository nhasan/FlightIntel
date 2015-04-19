/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.TimeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class NotamFragmentBase extends FragmentBase {

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

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
        Intent service = new Intent( getActivity(), NotamService.class );
        service.setAction( NotamService.ACTION_GET_NOTAM );
        service.putExtra( NotamService.ICAO_CODE, icaoCode );
        getActivity().startService( service );
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

            LinearLayout item = (LinearLayout) inflate( R.layout.notam_detail_item );
            TextView tv = (TextView) item.findViewById( R.id.notam_subject );
            tv.setText( subject );

            LinearLayout details = (LinearLayout) item.findViewById( R.id.notam_details );
            ArrayList<String> list = notams.get( subject );
            count += list.size();
            for ( String notam : list ) {
                addBulletedRow( details, notam );
            }

            content.addView( item, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        }

        TextView title1 = (TextView) findViewById( R.id.notam_title1 );
        title1.setText( getResources().getQuantityString( R.plurals.notams_found, count, count ) );
        TextView title2 = (TextView) findViewById( R.id.notam_title2 );
        Date lastModified = new Date( notamFile.lastModified() );
        title2.setText( "Updated "+ TimeUtils.formatElapsedTime( lastModified.getTime() ) );
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
        } catch ( IOException ignored ) {
        } finally {
            try {
                if ( in != null ) {
                    in.close();
                }
            } catch ( IOException ignored ) {
            }
        }

        return notams;
    }

}
