/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2021 Nadeem Hasan <nhasan@nadmm.com>
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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TimeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotamFragmentBase extends FragmentBase {

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mFilter = new IntentFilter( NotamService.ACTION_GET_NOTAM );

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( NotamService.ACTION_GET_NOTAM.equals( action ) ) {
                    String path = intent.getStringExtra( NotamService.NOTAM_PATH );
                    if ( path != null ) {
                        String location = intent.getStringExtra( NotamService.LOCATION );
                        File notamFile = new File( path );
                        showNotams( location, notamFile );
                        setRefreshing( false );
                    }
                }
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

    protected void getNotams( String location, boolean force ) {
        Intent service = new Intent( getActivity(), NotamService.class );
        service.setAction( NotamService.ACTION_GET_NOTAM );
        service.putExtra( NotamService.LOCATION, location );
        service.putExtra( NotamService.FORCE_REFRESH, force );
        getActivity().startService( service );
    }

    @SuppressLint( "SetTextI18n" )
    private void showNotams( String location, File notamFile ) {
        ArrayList<Notam> notams = parseNotams( notamFile );

        TextView title1 = findViewById( R.id.notam_title1 );
        title1.setText( getResources().getQuantityString( R.plurals.notams_found,
                notams.size(), notams.size() ) );
        TextView title2 = findViewById( R.id.notam_title2 );
        Date lastModified = new Date( notamFile.lastModified() );
        title2.setText( String.format( Locale.US, "Updated %s",
                TimeUtils.formatElapsedTime( lastModified.getTime() ) ) );

        LinearLayout content = findViewById( R.id.notam_content_layout );
        content.removeAllViews();

        for (Notam notam : notams)
        {
            if ( !notam.classification.equals( "FDC" ) ) {
                addRow( content, formatNotam( location, notam ) );
            }
        }

        for (Notam notam : notams)
        {
            if ( notam.classification.equals( "FDC" ) ) {
                addRow( content, formatNotam( location, notam ) );
            }
        }

        setFragmentContentShown( true );
    }

    private String formatNotam( String location, Notam notam ) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern( "dd MMM HH:mm yyyy", Locale.US );
        StringBuilder sb = new StringBuilder();

        if (notam.classification.equals( "FDC" )) {
            sb.append( "FDC " );
        }
        sb.append( notam.notamID );
        if ( notam.xovernotamID != null && !notam.xovernotamID.trim().isEmpty() ) {
            sb.append( " (" );
            sb.append( notam.xovernotamID );
            sb.append( ")" );
        }
        sb.append( " - " );
        if ( !notam.location.equals( location ) ) {
            sb.append( notam.location );
            sb.append( " " );
        }
        sb.append( notam.text );
        if ( !notam.text.endsWith( "." ) ) {
            sb.append( "." );
        }
        sb.append( " " );
        sb.append( dtf.format( notam.effectiveStart ).toUpperCase() );
        sb.append( " UNTIL " );
        if ( notam.effectiveEnd != null ) {
            sb.append( dtf.format( notam.effectiveEnd ).toUpperCase() );
            if ( notam.estimatedEnd.equals( "Y" ) ) {
                sb.append( " ESTIMATED" );
            }
        } else {
            sb.append( "PERM" );
        }
        sb.append( ". CREATED: " );
        sb.append( dtf.format( notam.issued ).toUpperCase() );
        if ( notam.lastUpdated.isAfter( notam.issued ) ) {
            sb.append( " UPDATED: " );
            sb.append( dtf.format( notam.lastUpdated ).toUpperCase() );
        }
        return sb.toString();
    }

    private ArrayList<Notam> parseNotams( File notamFile ) {
        FileInputStream in = null;
        ArrayList<Notam> notams = new ArrayList<>();
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter( int.class, new IntTypeAdapter() )
                    .registerTypeAdapter( OffsetDateTime.class, new DateTypeAdapter() )
                    .create();
            in = new FileInputStream( notamFile );
            JsonReader reader = new JsonReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) );
            reader.beginArray();
            while ( reader.hasNext() ) {
                Notam notam = gson.fromJson( reader, Notam.class );
                notams.add( notam );
            }
            reader.endArray();
            reader.close();
        } catch ( IOException e ) {
            e.printStackTrace();
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

    public static class IntTypeAdapter extends TypeAdapter<Integer> {

        @Override
        public Integer read( JsonReader in ) throws IOException {
            // Allows to parse empty strings
            if ( in.peek() == JsonToken.NULL ) {
                in.nextNull();
                return null;
            }
            String stringValue = in.nextString();
            try {
                return Integer.parseInt( stringValue );
            } catch ( NumberFormatException e ) {
                return 0;
            }
        }

        @Override
        public void write( JsonWriter out, Integer value ) {

        }
    }

    public static class DateTypeAdapter extends TypeAdapter<OffsetDateTime> {

        @Override
        public OffsetDateTime read( JsonReader in ) throws IOException {
            // Allows to parse empty strings
            if ( in.peek() == JsonToken.NULL ) {
                in.nextNull();
                return null;
            }
            String stringValue = in.nextString();
            try {
                return OffsetDateTime.parse( stringValue );
            } catch ( DateTimeParseException e ) {
                return null;
            }
        }

        @Override
        public void write( JsonWriter out, OffsetDateTime value ) {
        }
    }

}
