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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
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

    private static final File EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY
        = new File ( new File( Environment.getExternalStorageDirectory(), "Android" ), "data" );

    final class DataInfo {
        public String type;
        public String desc;
        public int version;
        public String fileName;
        public String dbName;
        public int size;
        public Date start;
        public Date end;

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

    private DownloadListAdapter mAdapter;
    private DatabaseManager mDbManager;
    private DownloadTask mDownloadTask;
    private AtomicBoolean mStop;

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        mAdapter = new DownloadListAdapter( this );
        mDbManager = new DatabaseManager( this );
        mDownloadTask = null;
        mStop = new AtomicBoolean( false );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        setTitle( "Airports - "+getTitle() );

        LayoutInflater inflater = getLayoutInflater();
        View header = inflater.inflate( R.layout.download_header, null );
        ListView lv = getListView();
        lv.addHeaderView( header, null, false );
        Button btnUpdate = (Button) findViewById( R.id.btnUpdate );
        btnUpdate.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkNetwork();
                    }
                }
        );

        checkData();
    }

    private void checkData() {
        CheckDataTask task = new CheckDataTask( this );
        task.execute();
    }

    private void checkNetwork() {
        ConnectivityManager connMan = (ConnectivityManager) getSystemService( 
                Context.CONNECTIVITY_SERVICE );
        NetworkInfo network = connMan.getActiveNetworkInfo();
        if ( network == null || !network.isConnected() ) {
            showError( "Network connectivity is not available" );
            return;
        }
            
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
                            finish();
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
            showError( "External storage is not writable" );
            return;
        } else if ( !Environment.MEDIA_MOUNTED.equals( state ) ) { 
            showError( "External storage is not available" );
            return;
        }

        mDownloadTask = new DownloadTask( this );
        mDownloadTask.execute();
    }

    private final class ItemData {
        public String type;
        public TextView statusText;
        public ProgressBar progressBar;

        public ItemData( String t, TextView s, ProgressBar p ) {
            type = t;
            statusText = s;
            progressBar = p;
        }

        public void setMaxSize( int max ) {
            progressBar.setMax( max );
            setProgress( 0 );
            progressBar.setVisibility( View.VISIBLE );
        }

        public void setProgress( int progress ) {
            progressBar.setProgress( progress );
            statusText.setText( Formatter.formatShortFileSize( DownloadActivity.this, progress )
                    +" of "
                    +Formatter.formatShortFileSize( DownloadActivity.this, progressBar.getMax() ) );
        }
    }

    private final class DownloadListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Context mContext;
        private final int VIEW_TYPE_SECTION_HEADER = 0;
        private final int VIEW_TYPE_DATA_FILE = 1;

        public DownloadListAdapter( Context context ) {
            mInflater = LayoutInflater.from( context );
            mContext = context;
        }

        private int getInstalledCount() {
            return mInstalledData.isEmpty()? 1: mInstalledData.size();
        }

        private int getAvailableCount() {
            return mAvailableData.isEmpty()? 1: mAvailableData.size();
        }

        private int getInstalledHeaderPos() {
            return 0;
        }

        private int getAvailableHeaderPos() {
            return getInstalledCount()+1;
        }

        private int getInstalledItemOffset() {
            return getInstalledHeaderPos()+1;
        }

        private int getAvailableItemOffset() {
            return getAvailableHeaderPos()+1;
        }

        @Override
        public int getCount() {
            // Account for 2 section headers
            return 2+getInstalledCount()+getAvailableCount();
        }

        @Override
        public Object getItem( int position ) {
            return position;
        }

        @Override
        public long getItemId( int position ) {
            return position;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled( int position ) {
            return false;
        }

        @Override
        public int getViewTypeCount() {
            // We have 2 view types: section and data
            return 2;
        }

        @Override public int getItemViewType( int position ) {
            return ( position == getInstalledHeaderPos() || position == getAvailableHeaderPos() )?
                    VIEW_TYPE_SECTION_HEADER : VIEW_TYPE_DATA_FILE;
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            int viewType = getItemViewType( position );

            if ( viewType == VIEW_TYPE_SECTION_HEADER ) {
                if ( convertView == null ) {
                    convertView = mInflater.inflate( R.layout.download_list_section, null );
                }

                TextView section = (TextView) convertView.findViewById( R.id.download_section );
                if (position == 0) {
                    section.setText( R.string.download_installed );
                }
                else {
                    section.setText( R.string.download_available );
                }
            }
            else {
                if ( position>=getInstalledHeaderPos() && position<getAvailableHeaderPos() ) {
                    if ( mInstalledData.isEmpty() ) {
                        if ( convertView == null ) {
                            convertView = mInflater.inflate( R.layout.download_list_nodata, null );
                        }
                        TextView msg = (TextView) convertView.findViewById( R.id.download_msg );
                        msg.setText( R.string.download_noinstall );
                    }
                    else {
                        if ( convertView == null ) {
                            convertView = mInflater.inflate( R.layout.download_list_item, null );
                        }
                        DataInfo info = mInstalledData.get( position-getInstalledItemOffset() );
                        TextView desc = (TextView) convertView.findViewById( R.id.download_desc );
                        desc.setText( info.desc );
                        TextView dates = (TextView) convertView.findViewById( R.id.download_dates );
                        dates.setText("Effective "+DateUtils.formatDateRange( mContext, 
                                info.start.getTime(), info.end.getTime(), 
                                DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_ABBREV_ALL ) );
                        TextView size = (TextView) convertView.findViewById( R.id.download_status );
                        size.setText( "Current" );
                    }
                }
                else {
                    if ( mAvailableData.isEmpty() ) {
                        if ( convertView == null ) {
                            convertView = mInflater.inflate( R.layout.download_list_nodata, null );
                        }
                        TextView msg = (TextView) convertView.findViewById( R.id.download_msg );
                        msg.setText( R.string.download_noupdate );
                    } else {
                        if ( convertView == null ) {
                            convertView = mInflater.inflate( R.layout.download_list_item, null );
                        }                        
                        DataInfo data = mAvailableData.get( position-getAvailableItemOffset() );
                        TextView desc = (TextView) convertView.findViewById( R.id.download_desc );
                        desc.setText( data.desc );
                        TextView dates = (TextView) convertView.findViewById( R.id.download_dates );
                        dates.setText("Effective "+DateUtils.formatDateRange( mContext, 
                                data.start.getTime(), data.end.getTime(), 
                                DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_ABBREV_ALL ) );
                        TextView status = (TextView) convertView.findViewById( 
                                R.id.download_status );
                        status.setText( Formatter.formatShortFileSize( mContext, data.size )
                                +"   ("+DateUtils.formatElapsedTime( data.size/(200*1024/8) )
                                +" @ 200kbps)" );
                        ProgressBar progress = (ProgressBar) convertView.findViewById( 
                                R.id.download_progress );
                        convertView.setTag( new ItemData( data.type, status, progress ) );
                    }
                }
            }

            return convertView;
        }        
    }

    private final class CheckDataTask extends AsyncTask<Void, Void, Integer> {
        private DownloadActivity mActivity;

        public CheckDataTask( DownloadActivity activity ) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
            Button btnUpdate = (Button) findViewById( R.id.btnUpdate );
            btnUpdate.setEnabled( false );
            mActivity.getListView().setVisibility( View.INVISIBLE );
            mInstalledData.clear();
            mAvailableData.clear();
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            int result;

            result = getInstalled();
            if ( result != 0 )
            {
                return result;
            }

            result = downloadManifest();
            if ( result != 0 )
            {
                return result;
            }

            result = parseManifest();
            if ( result != 0 )
            {
                return result;
            }

            processManifest();

            return 0;
        }

        @Override
        protected void onPostExecute( Integer result ) {
            if ( result != 0 ) {
                setProgressBarIndeterminateVisibility( false );
                showError( "There was an error while checking for updates" );
                return;
            }

            setProgressBarIndeterminateVisibility( false );

            ListView lv = mActivity.getListView();
            lv.setVisibility( View.VISIBLE );

            Button btnUpdate = (Button)lv.findViewById( R.id.btnUpdate );
            btnUpdate.setEnabled( !mAvailableData.isEmpty() );

            mActivity.setListAdapter( mAdapter );
            mAdapter.notifyDataSetInvalidated();
        }

        private int getInstalled() {
            int result = 0;
            final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

            SQLiteDatabase catalogDb = mDbManager.getCatalogDatabase();
            Cursor cursor = catalogDb.rawQuery( "SELECT * FROM "+Catalog.TABLE_NAME
                    +" GROUP BY "+Catalog.TYPE, null );
                    //+" HAVING MAX("+Catalog.VERSION+")", null );

            if ( cursor.moveToFirst() ) {
                do {
                    DataInfo info = new DataInfo();
                    info.type = cursor.getString( cursor.getColumnIndex( Catalog.TYPE ) );
                    info.desc = cursor.getString( cursor.getColumnIndex( Catalog.DESCRIPTION ) );
                    info.version = cursor.getInt( cursor.getColumnIndex( Catalog.VERSION ) );
                    try {
                        String start = cursor.getString( cursor.getColumnIndex( Catalog.START_DATE ) );
                        info.start = dateFormat.parse( start );
                        String end = cursor.getString( cursor.getColumnIndex( Catalog.END_DATE ) );
                        info.end = dateFormat.parse( end );
                        mInstalledData.add( info );
                    } catch ( ParseException e ) {
                        Log.e( TAG, "Error parsing dates: "+e.getMessage() );
                        return -1;
                    }
                } while ( cursor.moveToNext() );
            }

            cursor.close();
            catalogDb.close();

            return result;
        }

        private int downloadManifest() {
            try {
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
                showError( "Unable to read manifest file" );
                return -1;
            }

            final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
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
                                info.start = dateFormat.parse( body );
                            } catch ( ParseException e ) {
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
                                info.end = dateFormat.parse( body );
                            } catch ( ParseException e ) {
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
                Log.i( TAG, "start="+dataFile.start.toLocaleString() );
                Log.i( TAG, "end="+dataFile.end.toLocaleString() );
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
        private Handler mHandler;
        private File mCacheDir;
        private File mDatabaseDir;
        private ItemData mCurrentItem;

        public DownloadTask( DownloadActivity activity )
        {
            mHandler = new Handler();
            mCacheDir = new File( EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY, 
                    getClass().getPackage().getName()+"/cache" );
            mDatabaseDir = new File( EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY, 
                    getClass().getPackage().getName()+"/databases" );
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground( Void... params ) {
                Iterator<DataInfo> it = mAvailableData.iterator();
                while ( it.hasNext() ) {
                    final DataInfo data = it.next();

                    mCurrentItem = getItemForType( data.type );

                    int result = downloadFile( data);
                    if ( result != 0 ) {
                        return result;
                    }

                    result = installData( data );
                    if ( result != 0 ) {
                        return result;
                    }
                }

            return 0;
        }

        @Override
        protected void onProgressUpdate( Integer... progress ) {
            if ( mCurrentItem != null ) {
                mCurrentItem.setProgress( progress[ 0 ] );
            }
        }

        @Override
        protected void onPostExecute( Integer result ) {
            if ( result == 0 ) {
                showMessage( "Download", "The data was downloaded and installed successfully" );
            }
            else if ( result == -1 ) {
                showError( "There was an error while downloading data" );
            }
            else if ( result == -2 ) {
                showError( "Data file was not found on the server" );
            }
            else if ( result == -3 ) {
                showError( "Airports data download was interrupted" );
            }

            mStop.set( false );

            // Re-check for data to reflect the current downloads
            checkData();
        }

        protected ItemData getItemForType( String type ) {
            ListView lv = getListView();
            int count = lv.getChildCount();
            for ( int i=0; i<count; ++i) {
                ItemData data = (ItemData) lv.getChildAt( i ).getTag();
                if ( data != null && type.equals( data.type ) ) {
                    return data;
                }
            }

            return null;
        }

        protected int downloadFile( final DataInfo data ) {
            mHandler.post( new Runnable() {
                @Override
                public void run() {
                    if ( mCurrentItem != null ) {
                        mCurrentItem.setMaxSize( data.size );
                    }
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

                if ( !mCacheDir.exists() ) {
                    if ( !mCacheDir.mkdirs() ) {
                        Log.e( TAG, "Unable to create cache dir on external storage" );
                        return -3;
                    }
                }

                File zipFile = new File( mCacheDir, data.fileName );
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
                    // The download was stopped by the user
                    return -3;
                }

                return 0;
            } catch ( URISyntaxException e ) {
                Log.e( TAG, "URISyntaxException: "+e.getMessage() );
                return -1;
            } catch ( ClientProtocolException e ) {
                Log.e( TAG, "ClientProtocolException: "+e.getMessage() );
                return -1;
            } catch ( FileNotFoundException e ) {
                Log.e( TAG, "FileNotFoundException: "+e.getMessage() );
                return -1;
            } catch ( IOException e ) {
                Log.e( TAG, "IOException: "+e.getMessage() );
                return -1;
            } catch ( IllegalStateException e ) {
                Log.e( TAG, "IllegalStateException: "+e.getMessage() );
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
            int result=0;

            File cacheFile = new File( mCacheDir, data.fileName );
            ZipFile zipFile;
            try {
                zipFile = new ZipFile( cacheFile );
            } catch ( ZipException e ) {
                Log.e( TAG, "ZipException: "+e.getMessage() );
                return -1;
            } catch ( IOException e ) {
                Log.e( TAG, "IOException: "+e.getMessage() );
                return -1;
            }

            final ZipEntry entry = (ZipEntry) zipFile.entries().nextElement();

            mHandler.post( new Runnable() {
                @Override
                public void run() {
                    // Set the new progress details
                    if ( mCurrentItem != null ) {
                        mCurrentItem.setMaxSize( (int) entry.getSize() );
                    }
                }
            } );

            // Reset the progress
            publishProgress( 0 );

            if ( !mDatabaseDir.exists() ) {
                if ( !mDatabaseDir.mkdirs() ) {
                    Log.e( TAG, "Unable to create database dir on external storage" );
                    return -3;
                }
            }

            data.dbName = entry.getName();

            InputStream in = null;
            FileOutputStream out = null;

            try {
                in = zipFile.getInputStream( entry );
                File dbFile = new File( mDatabaseDir, data.dbName );
                out = new FileOutputStream( dbFile );

                byte[] buffer = new byte[ 64*1024 ];
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
                    // The download was stopped by the user
                    return -3;
                }

                result = (int) insertCatalogEntry( data );
                if ( result != 0 ) {
                    return result;
                }
     
                return 0;
            } catch ( FileNotFoundException e ) {
                Log.e( TAG, "FileNotFoundException: "+e.getMessage() );
                return -1;
            } catch ( IOException e ) {
                Log.e( TAG, "IOException: "+e.getMessage() );
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

        long insertCatalogEntry( DataInfo data ) {
            ContentValues values = new ContentValues();
            values.put( Catalog.TYPE, data.type );
            values.put( Catalog.DESCRIPTION, data.desc );
            values.put( Catalog.VERSION, data.version );
            values.put( Catalog.START_DATE, DateFormat.format( "yyyy-MM-dd", data.start ).toString() );
            values.put( Catalog.END_DATE, DateFormat.format( "yyyy-MM-dd", data.end ).toString() );
            values.put( Catalog.DB_NAME, data.dbName );

            Log.i( TAG, "Inserting catalog: type="+data.type
                    +", version="+data.version+", db="+data.dbName );
            return mDbManager.insertCatalogEntry( values );
        }
    }

    protected void showMessage( String title, String msg ) {
        Toast.makeText( getApplicationContext(), title+": "+msg, Toast.LENGTH_LONG ).show();
    }

    protected void showError( String msg ) {
        Toast.makeText( getApplicationContext(), "Download Failed: "+msg, 
                Toast.LENGTH_LONG ).show();
    }

    @Override
    public void onPause() {
        super.onPause();
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
}
