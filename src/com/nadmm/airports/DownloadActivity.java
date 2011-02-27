/*
 * Airports for Android
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.BaseColumns;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.DatabaseManager.Catalog;

public final class DownloadActivity extends ListActivity {
    private static final String TAG = DownloadActivity.class.getName();
    private static final String HOST = "10.0.2.2";
    //private static final String HOST = "192.168.1.116";
    private static final Integer PORT = 80;
    private static final String PATH = "/~nhasan/fadds";
    private static final String MANIFEST = "manifest.xml";

    final class DataInfo {
        public String type;
        public String desc;
        public int version;
        public String fileName;
        public String dbName;
        public int size;
        public Time start;
        public Time end;

        public DataInfo() {
        }

        public DataInfo( DataInfo info ) {
            type = info.type;
            desc = info.desc;
            version = info.version;
            fileName = info.fileName;
            size = info.size;
            start = info.start;
            end = info.end;
        }
    }

    private final ArrayList<DataInfo> mInstalledData = new ArrayList<DataInfo>();
    private final ArrayList<DataInfo> mAvailableData = new ArrayList<DataInfo>();

    private DatabaseManager mDbManager;
    private DownloadTask mDownloadTask;
    private AtomicBoolean mStop;
    private Handler mHandler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        mDbManager = DatabaseManager.instance();
        mDownloadTask = null;
        mStop = new AtomicBoolean( false );
        mHandler = new Handler();

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        setTitle( "Airports - "+getTitle() );

        setContentView( R.layout.download_list_view );
        
        // Add the footer view
        View footer = getLayoutInflater().inflate( R.layout.download_footer, null );
        getListView().addFooterView( footer );

        Button btnDownload = (Button) findViewById( R.id.btnDownload );
        btnDownload.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        warnIfNoWifi();
                    }
                }
        );

        Button btnDelete = (Button) findViewById( R.id.btnDelete );
        btnDelete.setOnClickListener( 
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkDelete();
                    }
                }
        );

        Intent intent = getIntent();
        if ( intent.hasExtra( "MSG" ) ) {
            String msg = intent.getStringExtra( "MSG" );
            Toast.makeText( this, msg, Toast.LENGTH_LONG ).show();
        }

        checkData();
    }

    private void checkData() {
        CheckDataTask task = new CheckDataTask( this );
        task.execute();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connMan = (ConnectivityManager) getSystemService( 
                Context.CONNECTIVITY_SERVICE );
        NetworkInfo network = connMan.getActiveNetworkInfo();
        if ( network == null || !network.isConnected() ) {
            showMessage( "Network connectivity is not available" );
            return false;
        }

        return true;
    }

    private void warnIfNoWifi() {
        if ( isNetworkAvailable() == false ) {
            return;
        }

        ConnectivityManager connMan = (ConnectivityManager) getSystemService( 
                Context.CONNECTIVITY_SERVICE );
        NetworkInfo network = connMan.getActiveNetworkInfo();
        if ( network.getType() != ConnectivityManager.TYPE_WIFI ) {
            AlertDialog.Builder builder = new AlertDialog.Builder( this );
            builder.setMessage( "You are not connected to a wifi network.\n"
                    +"Continue download?" )
                   .setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int id ) {
                            download();
                        }
                   } )
                   .setNegativeButton( "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int id ) {
                        }
                   } );
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
        else {
            download();
        }
    }

    private void download() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            showMessage( "External storage is not writable" );
            return;
        } else if ( !Environment.MEDIA_MOUNTED.equals( state ) ) {
            showMessage( "External storage is not available" );
            return;
        }

        mDownloadTask = new DownloadTask( this );
        mDownloadTask.execute();
    }

    private final class ProgressTracker {
        public String type;
        public TextView msgText;
        public TextView statusText;
        public ProgressBar progressBar;

        public ProgressTracker( String t, View convertView ) {
            type = t;
            msgText = (TextView) convertView.findViewById( R.id.download_msg );
            statusText = (TextView) convertView.findViewById( R.id.download_status );
            progressBar = (ProgressBar) convertView.findViewById( R.id.download_progress );
            statusText.setVisibility( View.VISIBLE );
        }

        public void initProgress( int resid, int max ) {
            progressBar.setMax( max );
            msgText.setText( resid );
            setProgress( 0 );
            progressBar.setVisibility( View.VISIBLE );
            statusText.setVisibility( View.VISIBLE );
            msgText.setVisibility( View.VISIBLE );
        }

        public void setProgress( int progress ) {
            progressBar.setProgress( progress );
            if ( progress < progressBar.getMax() ) {
                statusText.setText( Formatter.formatShortFileSize( DownloadActivity.this, progress )
                    +" of "
                    +Formatter.formatShortFileSize( DownloadActivity.this, progressBar.getMax() ) );
            } else {
                msgText.setText( R.string.install_done );
                statusText.setVisibility( View.GONE );
            }
        }
    }

    private final class DownloadCursor extends MatrixCursor {
        private static final String SECTION = "SECTION";
        private static final String TYPE = "TYPE";
        private static final String DESC = "DESC";
        private static final String DATES = "DATES";
        private static final String MSG = "MSG";

        private int mId = 0;

        public DownloadCursor() {
            super( new String[] { BaseColumns._ID, SECTION, TYPE, DESC, DATES, MSG } );
        }

        public void addRow( int section, DataInfo info ) {
            RowBuilder builder = newRow();
            builder.add( mId++ );
            builder.add( section );
            builder.add( info.type );
            builder.add( info.desc );
            builder.add( "Effective "+DateUtils.formatDateRange( DownloadActivity.this, 
                    info.start.toMillis( false ), info.end.toMillis( false )+1000, 
                    DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_ABBREV_ALL ) );
            builder.add( Formatter.formatShortFileSize( DownloadActivity.this, info.size )
                    +"   ("+DateUtils.formatElapsedTime( info.size/(200*1024/8) )
                    +" @ 200kbps)" );
        }
    }

    private final class DownloadListAdapter extends SectionedCursorAdapter {

        public DownloadListAdapter( Context context, Cursor c ) {
            super( context, R.layout.download_list_item, c, R.id.download_section );
        }

        @Override
        public String getSectionName() {
            Cursor c = getCursor();
            int section = c.getInt( c.getColumnIndex( DownloadCursor.SECTION ) );
            return getResources().getString( section );
        }

        @Override
        public View newView( Context context, Cursor c, ViewGroup parent ) {
            View view = super.newView( context, c, parent );
            int section = c.getInt( c.getColumnIndex( DownloadCursor.SECTION ) );
            if ( section == R.string.download_available ) {
                String type = c.getString( c.getColumnIndex( DownloadCursor.TYPE ) );
                view.setTag( new ProgressTracker( type, view ) );
            } else {
                View msg = view.findViewById( R.id.download_msg );
                msg.setVisibility( View.GONE );
                View status = view.findViewById( R.id.download_status );
                status.setVisibility( View.GONE );
            }
            return view;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled( int position ) {
            return false;
        }

        @Override
        public void bindView( View view, Context context, Cursor cursor ) {
            TextView tv;
            tv = (TextView) view.findViewById( R.id.download_desc );
            tv.setText( cursor.getString( cursor.getColumnIndex( DownloadCursor.DESC ) ) );
            tv = (TextView) view.findViewById( R.id.download_dates );
            tv.setText( cursor.getString( cursor.getColumnIndex( DownloadCursor.DATES ) ) );
            tv = (TextView) view.findViewById( R.id.download_msg );
            tv.setText( cursor.getString( cursor.getColumnIndex( DownloadCursor.MSG ) ) );
        }

    }

    private Cursor createCursor() {
        DownloadCursor c = new DownloadCursor();
        for ( DataInfo info : mInstalledData ) {
            c.addRow( R.string.download_installed, info );
        }
        for ( DataInfo info : mAvailableData ) {
            c.addRow( R.string.download_available, info );
        }
        return c;
    }

    private final class CheckDataTask extends AsyncTask<Void, Void, Integer> {
        private DownloadActivity mActivity;

        public CheckDataTask( DownloadActivity activity ) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
            Button btnDownload = (Button) findViewById( R.id.btnDownload );
            btnDownload.setEnabled( false );
            mInstalledData.clear();
            mAvailableData.clear();
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            int result;

            result = getInstalled();
            if ( result != 0 )
            {
                showMessage( "There was an error while reading catalog" );
                return result;
            }

            result = downloadManifest();
            if ( result != 0 )
            {
                showMessage( "There was an error while downloading manifest" );
                return result;
            }

            result = parseManifest();
            if ( result != 0 )
            {
                showMessage( "There was an error while processing manifest" );
                return result;
            }

            processManifest();

            return 0;
        }

        @Override
        protected void onPostExecute( Integer result ) {
            setProgressBarIndeterminateVisibility( false );

            if ( result != 0 ) {
                TextView empty = (TextView) findViewById( android.R.id.empty );
                empty.setText( R.string.download_error );
                return;
            }

            Cursor c = createCursor();
            DownloadListAdapter adapter = new DownloadListAdapter( DownloadActivity.this, c );
            mActivity.setListAdapter( adapter );
            adapter.notifyDataSetInvalidated();

            Button btnDelete = (Button) findViewById( R.id.btnDelete );
            if ( !mInstalledData.isEmpty() ) {
                btnDelete.setVisibility( View.VISIBLE );
                btnDelete.setEnabled( true );
            } else {
                btnDelete.setVisibility( View.GONE );
            }

            Button btnDownload = (Button) findViewById( R.id.btnDownload );
            if ( !mAvailableData.isEmpty() ) {
                btnDownload.setVisibility( View.VISIBLE );
                btnDownload.setEnabled( true );
            } else {
                btnDownload.setVisibility( View.GONE );
            }
        }

        private int getInstalled() {
            int result = 0;

            Cursor c = mDbManager.getLatestFromCatalog();
            if ( c.moveToFirst() ) {
                do {
                    DataInfo info = new DataInfo();
                    info.type = c.getString( c.getColumnIndex( Catalog.TYPE ) );
                    info.desc = c.getString( c.getColumnIndex( Catalog.DESCRIPTION ) );
                    info.version = c.getInt( c.getColumnIndex( Catalog.VERSION ) );
                    String start = c.getString( c.getColumnIndex( Catalog.START_DATE ) );
                    String end = c.getString( c.getColumnIndex( Catalog.END_DATE ) );
                    Log.i( TAG, info.type+","+start+","+end );
                    try {
                        info.start = new Time();
                        info.start.parse3339( start );
                        info.start.normalize( false );
                        info.end = new Time();
                        info.end.parse3339( end );
                        info.end.normalize( false );
                        mInstalledData.add( info );
                    } catch ( TimeFormatException e ) {
                        Log.e( TAG, "Error parsing dates: "+e.getMessage() );
                        return -1;
                    }
                } while ( c.moveToNext() );
                c.close();
            }

            return result;
        }

        private int downloadManifest() {
            try {
                if ( isNetworkAvailable() == false ) {
                    return -1;
                }

                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpHost target = new HttpHost( HOST, PORT );
                URI uri = new URI( PATH+"/"+MANIFEST );
                HttpGet get = new HttpGet( uri );

                HttpResponse response = httpClient.execute( target, get );
                if ( response.getStatusLine().getStatusCode() != 200 ) {
                    Log.e( TAG, response.getStatusLine().getReasonPhrase() );
                    return -1;
                }

                HttpEntity entity = response.getEntity();
                InputStream in = entity.getContent();
                FileOutputStream out = mActivity.openFileOutput( MANIFEST, MODE_PRIVATE );

                byte[] buffer = new byte[ 8*1024 ];
                int len = buffer.length;
                int count = 0;

                while ( ( count = in.read( buffer, 0, len ) ) != -1 ) {
                    out.write( buffer, 0, count );
                }

                in.close();
                out.close();
            }
            catch ( IOException e ) {
                Log.e( TAG, "IOException: "+e.getMessage() );
                return -1;
            }
            catch ( URISyntaxException e ) {
                Log.e( TAG, "URISyntaxException: "+e.getMessage() );
                return -1;
            }
            
            return 0;
        }

        private int parseManifest() {
            FileInputStream in;
            try {
                in = mActivity.openFileInput( MANIFEST );
            } catch ( FileNotFoundException e ) {
                Log.e( TAG, e.getMessage() );
                setProgressBarIndeterminateVisibility( false );
                showMessage( "Unable to read manifest file" );
                return -1;
            }

            final DataInfo info = new DataInfo();
            RootElement root = new RootElement( "manifest" );
            Element datafile = root.getChild( "datafile" );
            datafile.setEndElementListener( new EndElementListener() {
                @Override
                public void end() {
                    mAvailableData.add( new DataInfo( info ) );
                }
            } );
            datafile.getChild( "type" ).setEndTextElementListener( 
                    new EndTextElementListener() {
                        @Override
                        public void end( String body ) {
                            info.type = body;
                        }
                    }
            );
            datafile.getChild( "desc" ).setEndTextElementListener(
                    new EndTextElementListener() {
                        @Override
                        public void end( String body ) {
                            info.desc = body;
                        }
                    }
            );
            datafile.getChild( "version" ).setEndTextElementListener(
                    new EndTextElementListener() {
                        @Override
                        public void end( String body ) {
                            info.version = Integer.parseInt( body );
                        }
                    }
            );
            datafile.getChild( "filename" ).setEndTextElementListener(
                    new EndTextElementListener() {
                        @Override
                        public void end( String body ) {
                            info.fileName = body;
                        }
                    }
            );
            datafile.getChild( "size" ).setEndTextElementListener( 
                    new EndTextElementListener() {
                        @Override
                        public void end( String body ) {
                            info.size = Integer.parseInt( body );
                        }
                    }
            );
            datafile.getChild( "start" ).setEndTextElementListener(
                    new EndTextElementListener() {
                        @Override
                        public void end( String body ) {
                            try {
                                info.start = new Time();
                                info.start.parse3339( body );
                                info.start.normalize( false );
                            } catch ( TimeFormatException e ) {
                                Log.e( TAG, "Error parsing start date: "+e.getMessage() );
                            }
                        }
                    }
            );
            datafile.getChild( "end" ).setEndTextElementListener(
                    new EndTextElementListener() {
                        @Override
                        public void end(String body) {
                            try {
                                info.end = new Time();
                                info.end.parse3339( body );
                                info.end.normalize( false );
                            } catch ( TimeFormatException e ) {
                                Log.e( TAG, "Error parsing end date: "+e.getMessage() );
                            }
                        }
                    }
            );

            try {
                Xml.parse( in, Xml.Encoding.UTF_8, root.getContentHandler() );
            }
            catch ( Exception e ) {
                Log.e( TAG, "Error parsing manifest: "+e.getMessage() );
                return -1;
            }

            Iterator<DataInfo> it = mAvailableData.iterator();
            while ( it.hasNext() )
            {
                DataInfo dataFile = it.next();
                Log.i( TAG, "==== Data File Info ====" );
                Log.i( TAG, "type="+dataFile.type );
                Log.i( TAG, "desc="+dataFile.desc );
                Log.i( TAG, "version="+dataFile.version );
                Log.i( TAG, "filename="+dataFile.fileName );
                Log.i( TAG, "size="+dataFile.size );
                Log.i( TAG, "start="+dataFile.start.format3339( true ) );
                Log.i( TAG, "end="+dataFile.end.format3339( true ) );
            }

            try {
                in.close();
            } catch ( IOException e ) {
                Log.e( TAG, "Error closing manifest file: "+e.getMessage() );
                return -1;
            }

            return 0;
        }

        private void processManifest() {
            Iterator<DataInfo> it = mAvailableData.iterator();
            while ( it.hasNext() )
            {
                // Find out which of these available entries are not installed
                DataInfo available = it.next();
                Iterator<DataInfo> it2 = mInstalledData.iterator();
                while ( it2.hasNext() ) {
                    DataInfo installed = it2.next();
                    if ( available.type.equals( installed.type )
                            && available.version <= installed.version ) {
                        // This update is already installed
                        Log.i( TAG, "Removing "+available.type+" version "+available.version );
                        it.remove();
                    }
                }
            }
        }
    }

    private final class DownloadTask extends AsyncTask<Void, Integer, Integer> {
        private ProgressTracker mTracker;

        public DownloadTask( DownloadActivity activity )
        {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            Iterator<DataInfo> it = mAvailableData.iterator();
            while ( it.hasNext() ) {
                final DataInfo data = it.next();
    
                mTracker = getTrackerForType( data.type );
    
                int result = downloadData( data );
                if ( result < 0 ) {
                    return result;
                }
                Log.i( TAG, "Download done" );
    
                result = installData( data );
                if ( result < 0 ) {
                    return result;
                }
                Log.i( TAG, "Install done" );
    
                result = updateCatalog( data );
                if ( result < 0 ) {
                    return result;
                }         
                Log.i( TAG, "Catalog updated" );
            }
    
            cleanupExpiredData();

            return 0;
        }

        @Override
        protected void onProgressUpdate( Integer... progress ) {
             mTracker.setProgress( progress[ 0 ] );
        }

        @Override
        protected void onPostExecute( Integer result ) {
            if ( result == 0 ) {
                showMessage( "The data was downloaded and installed successfully" );
            }
            else {
                showMessage( "There was an error while downloading data" );
            }

            mStop.set( false );

            // Re-check for data to reflect the current downloads
            checkData();
        }

        protected ProgressTracker getTrackerForType( String type ) {
            ListView lv = getListView();
            int count = lv.getChildCount();
            for ( int i=0; i<count; ++i) {
                ProgressTracker tracker = (ProgressTracker) lv.getChildAt( i ).getTag();
                if ( tracker != null && type.equals( tracker.type ) ) {
                    return tracker;
                }
            }

            return null;
        }

        protected int downloadData( final DataInfo data ) {
            mHandler.post( new Runnable() {
                @Override
                public void run() {
                    mTracker.initProgress( R.string.downloading, data.size );
                }
            } );

            InputStream in = null;
            FileOutputStream out = null;

            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpHost target = new HttpHost( HOST, PORT );
                URI uri = new URI( PATH+"/"+data.fileName );
                HttpGet get = new HttpGet( uri );

                HttpResponse response = httpClient.execute( target, get );
                if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                    Log.e( TAG, response.getStatusLine().getReasonPhrase() );
                    return -2;
                }

                if ( !DatabaseManager.CACHE_DIR.exists() ) {
                    if ( !DatabaseManager.CACHE_DIR.mkdirs() ) {
                        showMessage( "Unable to create cache dir on external storage" );
                        return -3;
                    }
                }

                File zipFile = new File( DatabaseManager.CACHE_DIR, data.fileName );
                out = new FileOutputStream( zipFile );

                HttpEntity entity = response.getEntity();
                in = entity.getContent();

                Log.i( TAG, "Opened file "+zipFile.getCanonicalPath()+" for writing" );

                byte[] buffer = new byte[ 64*1024 ];
                int len = buffer.length;
                int count;
                int total = 0;

                while ( mStop.get() == false && ( count = in.read( buffer, 0, len ) ) != -1 ) {
                    out.write( buffer, 0, count );
                    total += count;
                    publishProgress( total );
                }

                if ( mStop.get() == true ) {
                    // Process was stopped by the user
                    return -3;
                }

                return 0;
            } catch ( URISyntaxException e ) {
                showMessage( e.getMessage() );
                return -1;
            } catch ( ClientProtocolException e ) {
                showMessage( e.getMessage() );
                return -1;
            } catch ( FileNotFoundException e ) {
                showMessage( e.getMessage() );
                return -1;
            } catch ( IOException e ) {
                showMessage( e.getMessage() );
                return -1;
            } catch ( IllegalStateException e ) {
                showMessage( e.getMessage() );
                return -1;
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

        protected int installData( final DataInfo data ) {
            File cacheFile = new File( DatabaseManager.CACHE_DIR, data.fileName );
            ZipFile zipFile;
            try {
                zipFile = new ZipFile( cacheFile );
            } catch ( ZipException e ) {
                showMessage( e.getMessage() );
                return -1;
            } catch ( IOException e ) {
                showMessage( e.getMessage() );
                return -1;
            }

            final ZipEntry entry = (ZipEntry) zipFile.entries().nextElement();

            mHandler.post( new Runnable() {
                @Override
                public void run() {
                    mTracker.initProgress( R.string.installing, (int) entry.getSize() );
                }
            } );

            if ( !DatabaseManager.DATABASE_DIR.exists() ) {
                if ( !DatabaseManager.DATABASE_DIR.mkdirs() ) {
                    showMessage( "Unable to create database dir on external storage" );
                    return -3;
                }
            }

            data.dbName = entry.getName();

            InputStream in = null;
            FileOutputStream out = null;

            try {
                in = zipFile.getInputStream( entry );
                File dbFile = new File( DatabaseManager.DATABASE_DIR, data.dbName );
                out = new FileOutputStream( dbFile );

                byte[] buffer = new byte[ 4*1024 ];
                int len = buffer.length;
                int count;
                int total = 0;

                // Try to read the type of record first
                while ( mStop.get() == false && ( count = in.read( buffer, 0, len ) ) != -1 )  {
                    out.write( buffer, 0, count );
                    total += count;
                    publishProgress( total );
                }

                if ( mStop.get() == true ) {
                    // Process was stopped by the user
                    return -3;
                }

                return 0;
            } catch ( FileNotFoundException e ) {
                showMessage( e.getMessage() );
                return -1;
            } catch ( IOException e ) {
                showMessage( e.getMessage() );
               return -1;
            } finally {
                cacheFile.delete();

                try {
                    if ( in != null ) {
                        in.close();
                    }
                } catch ( IOException e ) {
                }
                try {
                    if ( out != null ) {
                        out.close();
                    }
                } catch ( IOException e ) {
                }
                try {
                    zipFile.close();
                } catch ( IOException e ) {
                }
            }
        }

        protected int updateCatalog( DataInfo data ) {
            ContentValues values = new ContentValues();
            values.put( Catalog.TYPE, data.type );
            values.put( Catalog.DESCRIPTION, data.desc );
            values.put( Catalog.VERSION, data.version );
            values.put( Catalog.START_DATE, data.start.format3339( true ) );
            values.put( Catalog.END_DATE, data.end.format3339( true ) );
            values.put( Catalog.DB_NAME, data.dbName );

            Log.i( TAG, "Inserting catalog: type="+data.type
                    +", version="+data.version+", db="+data.dbName );
            int rc = mDbManager.insertCatalogEntry( values );
            if ( rc < 0 )
            {
                showMessage( "Failed to update catalog database" );
            }

            return rc;
        }
    }

    private void checkDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( "Are you sure you want to delete all installed data?" )
               .setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                        DeleteDataTask deleteTask = new DeleteDataTask();
                        deleteTask.execute( (Void)null );
                    }
               } )
               .setNegativeButton( "No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                    }
               } );
        AlertDialog alert = builder.create();
        alert.show();
        return;
        
    }

    private final class DeleteDataTask extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show( DownloadActivity.this, 
                    "", "Delete installed data. Please wait...", true );
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            int result = 0;

            // Make sure all the databases we want to delete are closed
            mDbManager.closeDatabases();

            // Get all the catalog entries
            SQLiteDatabase catalogDb = mDbManager.getCatalogDatabase();
            Cursor cursor = catalogDb.query( Catalog.TABLE_NAME, null, null, null, 
                    null, null, null );
            if ( cursor.moveToFirst() ) {
                do {
                    int _id = cursor.getInt( cursor.getColumnIndex( Catalog._ID ) );
                    String type = cursor.getString( cursor.getColumnIndex( Catalog.TYPE ) );
                    String dbName = cursor.getString( cursor.getColumnIndex( Catalog.DB_NAME ) );

                    // Delete the db file on the external device
                    Log.i( TAG, "Deleting _id="+_id+" type="+type+" dbName="+dbName );
                    File file = new File( DatabaseManager.DATABASE_DIR, dbName );
                    if ( file.delete() ) {
                        // Now delete the catalog entry for the file
                        int rows = catalogDb.delete( Catalog.TABLE_NAME, "_id=?", 
                                new String[] { Integer.toString( _id ) } );
                        if ( rows != 1 ) {
                            // If we could not delete the row, remember the error
                            result = -1;
                        }
                    } else {
                        result = -1;
                    }
                } while ( cursor.moveToNext() );
            }

            cursor.close();

            return result;
        }

        @Override
        protected void onPostExecute( Integer result )
        {
            mProgressDialog.dismiss();
            if ( result != 0 )
            {
                // Some or all data files were not deleted
                Toast.makeText( getApplicationContext(), 
                        "There was an error while deleting installed data", 
                        Toast.LENGTH_LONG ).show();                
            }

            // Refresh the download list view
            checkData();
        }
    }

    protected void cleanupExpiredData() {
        SQLiteDatabase catalogDb = mDbManager.getCatalogDatabase();

        Time now = new Time();
        now.setToNow();
        String today = now.format3339( true );

        Cursor c = mDbManager.getLatestFromCatalog();
        if ( c != null ) {
            do {
                // For each type that we have valid data, delete the expired data
                String end = c.getString( c.getColumnIndex( Catalog.END_DATE ) );
                if ( end.compareTo( today ) >= 0 ) {
                    // This data is valid today, cleanup any previous expired data
                    String type = c.getString( c.getColumnIndex( Catalog.TYPE ) );
                    int version = c.getInt( c.getColumnIndex( Catalog.VERSION ) );
                    Cursor expired = catalogDb.query( Catalog.TABLE_NAME, 
                            new String[] { Catalog._ID, Catalog.DB_NAME },
                            Catalog.TYPE+"=? and "+Catalog.VERSION+"<?",
                            new String[] { type, Integer.toString( version ) },
                            null, null, null );
                    if ( expired.moveToFirst() ) {
                        do {
                            Integer _id = expired.getInt( c.getColumnIndex( Catalog._ID ) );
                            String dbName = expired.getString( 
                                    expired.getColumnIndex( Catalog.DB_NAME ) );
                            Log.i( TAG, "Deleting _id="+_id+" type="+type+" dbName="+dbName );
                            File file = new File( DatabaseManager.DATABASE_DIR, dbName );
                            if ( file.delete() ) {
                                // Now delete the catalog entry for the file
                                catalogDb.delete( Catalog.TABLE_NAME, Catalog._ID+"=?", 
                                        new String[] { Integer.toString( _id ) } );
                            }
                        } while ( expired.moveToNext() );
                    }

                    expired.close();
                }
            } while ( c.moveToNext() );

            c.close();
        }
    }

    protected void showMessage( final String msg ) {
        Log.i( TAG, msg );
        mHandler.post( new Runnable () {
            @Override
            public void run() {
                Toast.makeText( getApplicationContext(), msg, Toast.LENGTH_LONG ).show();
            }
        } );
    }

    @Override
    public void onPause() {
        super.onPause();
        mDbManager.close();
        Log.i( TAG, "onPause() called" );
    }

    public void onResume() {
        super.onResume();
        Log.i( TAG, "onResume called" );
    }

    public void onStop() {
        super.onStop();
        Log.i( TAG, "onStop called" );

        if ( mDownloadTask != null ) {
            Log.i( TAG, "Stopping download thread" );
            mStop.set( true );
        }
    }


    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        MenuItem settings = menu.findItem( R.id.menu_download );
        settings.setEnabled( false );
        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_search:
            onSearchRequested();
            return true;
        case R.id.menu_browse:
            try {
                Intent browse = new Intent( this, BrowseActivity.class );
                browse.putExtra( BrowseActivity.EXTRA_BUNDLE, new Bundle() );
                startActivity( browse );
            } catch ( ActivityNotFoundException e ) {
                showErrorMessage( e.getMessage() );
            }
            return true;
        case R.id.menu_download:
            try {
                Intent download = new Intent( this, DownloadActivity.class );
                startActivity( download );
            } catch ( ActivityNotFoundException e ) {
                showErrorMessage( e.getMessage() );
            }
            return true;
        case R.id.menu_settings:
            try {
                Intent settings = new Intent( this, PreferencesActivity.class  );
                startActivity( settings );
            } catch ( ActivityNotFoundException e ) {
                showErrorMessage( e.getMessage() );
            }
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

    protected void showErrorMessage( String msg )
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( msg )
            .setTitle( "Download Error" )
            .setPositiveButton( "Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            } );
        AlertDialog alert = builder.create();
        alert.show();
    }

}
