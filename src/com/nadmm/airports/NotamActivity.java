/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.nadmm.airports.DatabaseManager.Airports;

public class NotamActivity extends ActivityBase {

    HashMap<String, ArrayList<NotamRec>> mNotams;
    LinearLayout mMainLayout;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mNotams = new HashMap<String, ArrayList<NotamRec>>();

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        NotamTask task = new NotamTask();
        task.execute( siteNumber );
    }

    private final class NotamRec {
        public String mSourceId;
        public String mNotamId;
        public Date mCreateTime;
        public String mQCode;
        public String mNotamReport;

        public final void clear() {
            mSourceId = "";
            mNotamId = "";
            mCreateTime.setTime( 0 );
            mQCode = "";
            mNotamReport = "";
        }
    }

    private void parseNotam( FileInputStream in ) {

        final NotamRec node = new NotamRec();
        RootElement root = new RootElement( "notam-query-result-set" );
        Element rec = root.getChild( "notam-rec" );

        rec.setEndElementListener( new EndElementListener() {
            
            @Override
            public void end() {
                Log.d( "SOURCE ID", node.mSourceId );
                Log.d( "NOTAM_ID", node.mNotamId );
                Log.d( "Q CODE", node.mQCode );
                Log.d( "NOTAM REPORT", node.mNotamReport );
                String type;
                try {
                    type = node.mQCode.substring( 1, 2 );
                } catch ( Exception e ) {
                    type = "Z";
                }
                ArrayList<NotamRec> list = mNotams.get( type );
                if ( list == null ) {
                    list = new ArrayList<NotamRec>();
                    mNotams.put( type, list );
                }
                list.add( node );
                node.clear();
            }

        } );

        rec.getChild( "source_id" ).setEndTextElementListener( new EndTextElementListener() {
            
            @Override
            public void end( String body ) {
                node.mSourceId = body;
            }

        } );

        rec.getChild( "notam_id" ).setEndTextElementListener( new EndTextElementListener() {
            
            @Override
            public void end( String body ) {
                node.mNotamId = body;
            }

        } );

        rec.getChild( "notam_qcode" ).setEndTextElementListener( new EndTextElementListener() {
            
            @Override
            public void end( String body ) {
                node.mQCode = body;
            }

        } );

        rec.getChild( "notam_report" ).setEndTextElementListener( new EndTextElementListener() {
            
            @Override
            public void end( String body ) {
                node.mNotamReport = body;
            }

        } );

        rec.getChild( "notam_lastmod_dtg" ).setEndTextElementListener( new EndTextElementListener() {
            
            @Override
            public void end( String body ) {
                SimpleDateFormat format = new SimpleDateFormat( "yyyyMMddHHmm" );
                try {
                    node.mCreateTime = format.parse( body );
                } catch ( ParseException e ) {
                    e.printStackTrace();
                }
            }

        } );

        try {
            Xml.parse( in, Xml.Encoding.UTF_8, root.getContentHandler() );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private final class NotamTask extends AsyncTask<String, Void, Cursor> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            Activity activity = NotamActivity.this;

            FileInputStream in = null;
            try {
                in = activity.openFileInput( "notam.xml" );
                parseNotam( in );
                in.close();
            } catch ( Exception e ) {
                Log.d( "EXCEPTION", e.getMessage() );
            }

            Cursor c = mDbManager.getAirportDetails( siteNumber );
            return c;
        }
        
        @Override
        protected void onPostExecute( Cursor result ) {
            setProgressBarIndeterminateVisibility( false );

            View view = inflate( R.layout.notam_detail_view );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.notam_top_layout );

            // Title
            showAirportTitle( mMainLayout, result );

            // Cleanup cursor
            result.close();
        }

    }

}
