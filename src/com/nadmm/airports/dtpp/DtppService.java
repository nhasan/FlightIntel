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

package com.nadmm.airports.dtpp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.nadmm.airports.AirportsMain;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Dtpp;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.UiUtils;

public class DtppService extends IntentService {

    public static final String ACTION_GET_CHART = "flightintel.intent.action.GET_CHART";
    public static final String ACTION_CHECK_CHART = "flightintel.intent.action.CHECK_CHART";

    public static final String OP_MODE = "OP_MODE";
    public static final String FAA_CODE = "FAA_CODE";
    public static final String TPP_VOLUME = "TPP_VOLUME";
    public static final String TPP_CYCLE = "TPP_CYCLE";
    public static final String PDF_NAME = "PDF_NAME";
    public static final String DOWNLOAD_IF_MISSING = "DOWNLOAD_IF_MISSING";
    public static final String PDF_PATH = "PDF_PATH";
    public static final String RESULT = "RESULT";

    public static final String OP_MODE_SINGLE = "OP_MODE_SINGLE";
    public static final String OP_MODE_APT = "OP_MODE_APT";
    public static final String OP_MODE_VOLUME = "OP_MODE_VOLUME";

    private final String DTPP_HOST = "aeronav.faa.gov";
    private final File DTPP_DIR = new File(
            AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/dtpp" );

    private final HttpClient mHttpClient;
    private final HttpHost mTarget;
    private final DatabaseManager mDbManager;

    public DtppService() {
        super( "DtppService" );
        mHttpClient = NetworkUtils.getHttpClient();
        mTarget = new HttpHost( DTPP_HOST, 80 );
        mDbManager= DatabaseManager.instance( this );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if ( !DTPP_DIR.exists() ) {
            DTPP_DIR.mkdirs();
        }
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        File cycleDir = new File( DTPP_DIR, tppCycle );
        if ( !cycleDir.exists() ) {
            // A new cycle has begun, cleanup any old cycles
            cleanupOldCycles();
            // Create the directory for the new cycle
            cycleDir.mkdir();
        }

        String action = intent.getAction();
        String opMode = OP_MODE_SINGLE;
        if ( action.equals( ACTION_GET_CHART ) ) {
            if ( opMode.equals( OP_MODE_SINGLE ) ) {
                getSingleChart( intent );
            } else if ( opMode.equals( OP_MODE_APT ) ) {
                getAllChartsForAirport( intent );
            } else if ( opMode.equals( OP_MODE_VOLUME ) ) {
                getAllChartsForVolume( intent );
            }
        } else if ( action.equals( ACTION_CHECK_CHART ) ) {
            if ( opMode.equals( OP_MODE_SINGLE ) ) {
                checkSingleChart( intent );
            } else if ( opMode.equals( OP_MODE_APT ) ) {
            } else if ( opMode.equals( OP_MODE_VOLUME ) ) {
            }
        }
    }

    protected void getSingleChart( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        String pdfName = intent.getStringExtra( PDF_NAME );

        File cycleDir = new File( DTPP_DIR, tppCycle );
        File pdfFile = new File( cycleDir, pdfName );
        if ( !pdfFile.exists() ) {
            downloadChart( tppCycle, pdfFile );
        }

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_GET_CHART );
        result.putExtra( OP_MODE, OP_MODE_SINGLE );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( PDF_NAME, pdfName );
        result.putExtra( RESULT, pdfFile.exists() );
        if ( pdfFile.exists() ) {
            result.putExtra( PDF_PATH, pdfFile.getAbsolutePath() );
        }
        sendBroadcast( result );
    }

    protected void checkSingleChart( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        String pdfName = intent.getStringExtra( PDF_NAME );
        boolean download = intent.getBooleanExtra( DOWNLOAD_IF_MISSING, false );

        File cycleDir = new File( DTPP_DIR, tppCycle );
        File pdfFile = new File( cycleDir, pdfName );
        if ( download && !pdfFile.exists() ) {
            downloadChart( tppCycle, pdfFile );
        }

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_CHECK_CHART );
        result.putExtra( OP_MODE, OP_MODE_SINGLE );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( PDF_NAME, pdfName );
        result.putExtra( RESULT, pdfFile.exists() );
        if ( pdfFile.exists() ) {
            result.putExtra( PDF_PATH, pdfFile.getAbsolutePath() );
        }
        sendBroadcast( result );
    }

    protected void getAllChartsForAirport( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        String faaCode = intent.getStringExtra( FAA_CODE );

        SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_DTPP );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Dtpp.TABLE_NAME );
        Cursor c = builder.query( db, new String[] { Dtpp.PDF_NAME },
                Dtpp.FAA_CODE+"=?", new String[] { faaCode }, null, null, null, null );


        boolean ok = downloadCharts( tppCycle, c );

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_GET_CHART );
        result.putExtra( OP_MODE, OP_MODE_APT );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( RESULT, ok );
        sendBroadcast( result );
    }

    protected void checkAllChartsForAirport( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        String faaCode = intent.getStringExtra( FAA_CODE );

        SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_DTPP );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Dtpp.TABLE_NAME );
        Cursor c = builder.query( db, new String[] { Dtpp.PDF_NAME },
                Dtpp.FAA_CODE+"=?", new String[] { faaCode }, null, null, null, null );


        boolean ok = checkCharts( tppCycle, c );

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_CHECK_CHART );
        result.putExtra( OP_MODE, OP_MODE_APT );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( RESULT, ok );
        sendBroadcast( result );
    }

    protected void getAllChartsForVolume( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        String tppVolume = intent.getStringExtra( TPP_VOLUME );

        SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_DTPP );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Dtpp.TABLE_NAME );
        Cursor c = builder.query( db, new String[] { Dtpp.PDF_NAME },
                Dtpp.TPP_VOLUME+"=?", new String[] { tppVolume }, null, null, null, null );

        boolean ok = downloadCharts( tppCycle, c );

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_GET_CHART );
        result.putExtra( OP_MODE, OP_MODE_VOLUME );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( RESULT, ok );
        sendBroadcast( result );
    }

    protected void checkAllChartsForVolume( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        String tppVolume = intent.getStringExtra( TPP_VOLUME );

        SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_DTPP );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Dtpp.TABLE_NAME );
        Cursor c = builder.query( db, new String[] { Dtpp.PDF_NAME },
                Dtpp.TPP_VOLUME+"=?", new String[] { tppVolume }, null, null, null, null );

        boolean ok = checkCharts( tppCycle, c );

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_CHECK_CHART );
        result.putExtra( OP_MODE, OP_MODE_VOLUME );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( RESULT, ok );
        sendBroadcast( result );
    }

    protected boolean downloadCharts( String tppCycle, Cursor c )
    {
        boolean ok = true;
        if ( c.moveToFirst() ) {
            do {
                String pdfName = c.getString( c.getColumnIndex( Dtpp.PDF_NAME ) );
                File cycleDir = new File( DTPP_DIR, tppCycle );
                File pdfFile = new File( cycleDir, pdfName );
                if ( !pdfFile.exists() ) {
                    downloadChart( tppCycle, pdfFile );
                }
                if ( !pdfFile.exists() ) {
                    ok = false;
                    break;
                }
            } while ( c.moveToNext() );
        }
        return ok;
    }

    protected boolean checkCharts( String tppCycle, Cursor c )
    {
        boolean ok = true;
        File cycleDir = new File( DTPP_DIR, tppCycle );
        if ( c.moveToFirst() ) {
            do {
                String pdfName = c.getString( c.getColumnIndex( Dtpp.PDF_NAME ) );
                File pdfFile = new File( cycleDir, pdfName );
                if ( !pdfFile.exists() ) {
                    ok = false;
                    break;
                }
            } while ( c.moveToNext() );
        }
        return ok;
    }

    protected void downloadChart( String tppCycle, File pdfFile ) {
        InputStream in = null;
        FileOutputStream out = null;

        try {
            String path;
            if ( pdfFile.getName().equals( "legendAD.pdf" ) ) {
                path = "/content/aeronav/online/pdf_files/legendAD.pdf";
            } else {
                path = String.format( "/d-tpp/%s/%s", tppCycle, pdfFile.getName() );
            }
            URI uri = new URI( path );
            HttpGet get = new HttpGet( uri );

            HttpResponse response = mHttpClient.execute( mTarget, get );
            if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                return;
            }

            out = new FileOutputStream( pdfFile );

            HttpEntity entity = response.getEntity();
            in = entity.getContent();

            byte[] buffer = new byte[ 16*1024 ];
            int count;

            while ( ( count = in.read( buffer, 0, buffer.length ) ) != -1 ) {
                out.write( buffer, 0, count );
            }
        } catch ( Exception e ) {
            UiUtils.showToast( getApplicationContext(), e.getMessage() );
        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch ( IOException e ) {
                }
            }
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                }
            }
        }
    }

    protected void cleanupOldCycles() {
        File[] cycles = DTPP_DIR.listFiles();
        for ( File cycle : cycles ) {
            // First delete all the charts within the cycle directory
            File[] charts = cycle.listFiles();
            for ( File chart : charts ) {
                chart.delete();
            }
            // Now delete the cycle directory itself
            cycle.delete();
        }
    }
}
