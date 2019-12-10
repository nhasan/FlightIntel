/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2018 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.data;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.BaseColumns;
import android.sax.Element;
import android.sax.RootElement;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TimeFormatException;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.ExternalStorageActivity;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SectionedCursorAdapter;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class DownloadFragment extends FragmentBase {

    private static final String TAG = DownloadActivity.class.getName();
    private static final String HOST = "commondatastorage.googleapis.com";
    private static final String PATH = "/flightintel/database";
    private static final String MANIFEST = "manifest.xml";

    private DatabaseManager mDbManager;
    private DownloadTask mDownloadTask;
    private Handler mHandler;
    private ListView mListView;

    private Map<String, ProgressTracker> mTrackers = new HashMap<>();

    private ArrayList<DataInfo> mInstalledData = new ArrayList<>();
    private ArrayList<DataInfo> mAvailableData = new ArrayList<>();

    private final class DataInfo implements Comparable<DataInfo> {

        private String type;
        private String desc;
        private int version;
        private String fileName;
        private int size;
        private Date start;
        private Date end;

        private DataInfo() {
        }

        private DataInfo( DataInfo info ) {
            type = info.type;
            desc = info.desc;
            version = info.version;
            fileName = info.fileName;
            size = info.size;
            start = info.start;
            end = info.end;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DataInfo ) {
                DataInfo info = (DataInfo) o;
                return type.equals( info.type ) && version == info.version;
            }
            return false;
        }

        @Override
        public int compareTo( DataInfo info ) {
            if ( !type.equals( info.type ) ) {
                return type.compareTo( info.type );
            }
            return start.compareTo( info.start );
        }

    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mDbManager = DatabaseManager.instance( getActivity() );
        mDownloadTask = null;
        mHandler = new Handler();
    }

    @Nullable
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.download_list_view, container, false );

        // Add the footer view
        View footer = inflate( R.layout.download_footer );
        mListView = view.findViewById( android.R.id.list );
        mListView.addFooterView( footer );
        mListView.setFooterDividersEnabled( true );

        Button btnDownload = view.findViewById( R.id.btnDownload );
        btnDownload.setOnClickListener(
                v -> checkNetworkAndDownload()
        );

        Button btnDelete = view.findViewById( R.id.btnDelete );
        btnDelete.setOnClickListener(
                v -> checkDelete()
        );

        return createContentView( view );
    }

    @Override
    public void onActivityCreated( @Nullable Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        checkData();
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        Cursor c = mDbManager.getCurrentFromCatalog();
        boolean visible = c.moveToFirst();
        c.close();
        menu.findItem( R.id.menu_search ).setVisible( visible );
    }

    private void checkNetworkAndDownload() {
        NetworkUtils.checkNetworkAndDownload( getActivity(), this::download );
    }

    public void download() {
        if ( SystemUtils.isExternalStorageAvailable() ) {
            mHandler.post( () -> {
                mDownloadTask = new DownloadTask( this );
                mDownloadTask.execute();
            } );
        } else {
            Intent intent = new Intent( getActivity(), ExternalStorageActivity.class );
            startActivity( intent );
            getActivity().finish();
        }
    }

    private void checkDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setMessage( "Are you sure you want to delete all installed data?" )
                .setPositiveButton( "Yes", ( dialog, id ) -> {
                    DeleteDataTask deleteTask = new DeleteDataTask( this );
                    deleteTask.execute( (Void) null );
                } )
                .setNegativeButton( "No", ( dialog, id ) -> {
                } );
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void checkData() {
        CheckDataTask task = new CheckDataTask( this );
        task.execute();
    }

    private void cleanupExpiredData() {
        SQLiteDatabase catalogDb = mDbManager.getCatalogDb();

        Cursor c = mDbManager.getAllFromCatalog();
        if ( c.moveToFirst() ) {
            Date now = new Date();

            do {
                // Check and delete all the expired databases
                String s = c.getString( c.getColumnIndex( DatabaseManager.Catalog.END_DATE ) );
                Date end = TimeUtils.parse3339( s );
                if ( end == null || !end.after( now ) ) {
                    // This database has expired, remove it
                    Integer _id = c.getInt( c.getColumnIndex( DatabaseManager.Catalog._ID ) );
                    String dbName = c.getString( c.getColumnIndex( DatabaseManager.Catalog.DB_NAME ) );
                    File file = mDbManager.getDatabaseFile( dbName );
                    if ( catalogDb.isOpen() && file.delete() ) {
                        // Now delete the catalog entry for the file
                        catalogDb.delete( DatabaseManager.Catalog.TABLE_NAME,
                                DatabaseManager.Catalog._ID + "=?",
                                new String[]{ Integer.toString( _id ) } );
                    }
                }
            } while ( c.moveToNext() );
        }
        c.close();
    }

    private Cursor createCursor() {
        DownloadCursor c = new DownloadCursor();
        for ( DataInfo info : mAvailableData ) {
            c.addRow( R.string.download_available, info );
        }
        for ( DataInfo info : mInstalledData ) {
            c.addRow( R.string.download_installed, info );
        }
        return c;
    }

    private void updateDownloadList() {
        Cursor c = createCursor();
        DownloadListAdapter adapter = new DownloadListAdapter( getActivity(), c );
        mListView.setAdapter( adapter );
        setContentShown( true );

        if ( c.getCount() == 0 ) {
            TextView empty = findViewById( android.R.id.empty );
            empty.setText( R.string.download_error );
            return;
        }

        Button btnDelete = findViewById( R.id.btnDelete );
        if ( !mInstalledData.isEmpty() ) {
            btnDelete.setVisibility( View.VISIBLE );
            btnDelete.setEnabled( true );
        } else {
            btnDelete.setVisibility( View.GONE );
        }

        Button btnDownload = findViewById( R.id.btnDownload );
        if ( !mAvailableData.isEmpty() ) {
            btnDownload.setVisibility( View.VISIBLE );
            btnDownload.setEnabled( true );
        } else {
            btnDownload.setVisibility( View.GONE );
        }
    }

    private int getInstalled() {
        mInstalledData.clear();
        Date now = new Date();

        Cursor c = mDbManager.getAllFromCatalog();
        if ( c.moveToFirst() ) {
            do {
                DataInfo info = new DataInfo();
                info.type = c.getString( c.getColumnIndex( DatabaseManager.Catalog.TYPE ) );
                info.desc = c.getString( c.getColumnIndex( DatabaseManager.Catalog.DESCRIPTION ) );
                info.version = c.getInt( c.getColumnIndex( DatabaseManager.Catalog.VERSION ) );
                String start = c.getString( c.getColumnIndex( DatabaseManager.Catalog.START_DATE ) );
                String end = c.getString( c.getColumnIndex( DatabaseManager.Catalog.END_DATE ) );
                Log.i( TAG, info.type + "," + start + "," + end );
                try {
                    info.start = TimeUtils.parse3339( start );
                    info.end = TimeUtils.parse3339( end );
                    if ( info.end != null && now.before( info.end ) ) {
                        mInstalledData.add( info );
                    }
                } catch ( TimeFormatException e ) {
                    UiUtils.showToast( getContext(), e.getMessage() );
                    return -1;
                }
            } while ( c.moveToNext() );
        }
        c.close();

        return 0;
    }

    private int downloadManifest() {
        try {
            if ( !NetworkUtils.isNetworkAvailable( getContext() ) ) {
                UiUtils.showToast( getContext(), "Please check your network connection" );
                return -1;
            }

            File manifest = new File( getActivity().getCacheDir(), MANIFEST );

            boolean fetch = true;
            if ( manifest.exists() ) {
                Date now = new Date();
                long age = now.getTime() - manifest.lastModified();
                if ( age < 10 * DateUtils.MINUTE_IN_MILLIS ) {
                    fetch = false;
                }
            }

            if ( fetch ) {
                NetworkUtils.doHttpsGet( getContext(), HOST, PATH + "/" + MANIFEST, manifest );
            }
        } catch ( Exception e ) {
            UiUtils.showToast( getContext(), e.getMessage() );
            return -1;
        }

        return 0;
    }

    private int parseManifest() {
        FileInputStream in;
        try {
            File manifest = new File( getActivity().getCacheDir(), MANIFEST );
            in = new FileInputStream( manifest );

            final DataInfo info = new DataInfo();
            RootElement root = new RootElement( "manifest" );
            Element datafile = root.getChild( "datafile" );
            datafile.setEndElementListener( () -> mAvailableData.add( new DataInfo( info ) ) );
            datafile.getChild( "type" ).setEndTextElementListener(
                    body -> info.type = body
            );
            datafile.getChild( "desc" ).setEndTextElementListener(
                    body -> info.desc = body
            );
            datafile.getChild( "version" ).setEndTextElementListener(
                    body -> info.version = Integer.parseInt( body )
            );
            datafile.getChild( "filename" ).setEndTextElementListener(
                    body -> info.fileName = body
            );
            datafile.getChild( "size" ).setEndTextElementListener(
                    body -> info.size = Integer.parseInt( body )
            );
            datafile.getChild( "start" ).setEndTextElementListener(
                    body -> info.start = TimeUtils.parse3339( body )
            );
            datafile.getChild( "end" ).setEndTextElementListener(
                    body -> info.end = TimeUtils.parse3339( body )
            );

            Xml.parse( in, Xml.Encoding.UTF_8, root.getContentHandler() );

            Collections.sort( mAvailableData );

            in.close();
        } catch ( Exception e ) {
            UiUtils.showToast( getContext(), e.getMessage() );
            return -1;
        }

        return 0;
    }

    private void processManifest() {
        Date now = new Date();
        Iterator<DataInfo> it = mAvailableData.iterator();
        while ( it.hasNext() ) {
            DataInfo available = it.next();
            if ( now.after( available.end ) ) {
                // Expired
                Log.i( TAG, "Removing expired " + available.type + ":" + available.version );
                it.remove();
                continue;
            }
            if ( isInstalled( available ) ) {
                // Already installed
                Log.i( TAG, "Removing installed " + available.type + ":" + available.version );
                it.remove();
            }
        }
    }

    private boolean isInstalled( DataInfo available ) {
        for( DataInfo installed : mInstalledData ) {
            if ( available.equals( installed ) ) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<DataInfo> getAvailableData() {
        return mAvailableData;
    }

    private void clearData() {
        mInstalledData.clear();
        mAvailableData.clear();
    }

    private void addInstalledData( DataInfo data ) {
        mInstalledData.add( data );
        mAvailableData.remove( data );

    }

    private void reopenDatabases() {
        mDbManager.closeDatabases();
        mDbManager.openDatabases();
    }

    private ProgressTracker getTrackerForType( String type ) {
        return mTrackers.get( type );
    }

    public Handler getHandler() {
        return mHandler;
    }

    private static class DownloadTask extends AsyncTask<Void, Integer, Integer> {
        private WeakReference<DownloadFragment> mFragment;
        private ProgressTracker mTracker;

        private DownloadTask( DownloadFragment fragment ) {
            mFragment = new WeakReference<>( fragment );
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return 1;
            }

            Iterator<DataInfo> it = fragment.getAvailableData().iterator();
            if ( it.hasNext() ) {
                final DataInfo data = it.next();

                mTracker = fragment.getTrackerForType( data.type );

                int result = downloadData( data );
                if ( result < 0 ) {
                    return result;
                }

                result = updateCatalog( data );
                if ( result < 0 ) {
                    return result;
                }

                // Update the displayed list to reflect the installed data
                fragment.addInstalledData( data );
            } else {
                // No more downloads left, cleanup any expired data
                fragment.cleanupExpiredData();
                fragment.reopenDatabases();
                UiUtils.showToast( fragment.getContext(), "Data installation completed successfully" );
                return 1;
            }

            return 0;
        }

        @Override
        protected void onProgressUpdate( Integer... progress ) {
            mTracker.setProgress( progress[ 0 ] );
        }

        @Override
        protected void onPostExecute( Integer result ) {
            if ( mTracker != null ) {
                mTracker.hideProgress();
            }

            DownloadFragment fragment = mFragment.get();
            if ( fragment == null || fragment.getActivity() == null ) {
                return;
            }

            // Update the displayed list to reflect the recently installed data
            fragment.updateDownloadList();

            if ( result == 0 ) {
                // Start the download of the next data file
                fragment.download();
            }
        }

        private int downloadData( final DataInfo data ) {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return 1;
            }

            fragment.getHandler().post( () -> mTracker.initProgress( data.size ) );

            try {
                File dbFile = fragment.getDbManager().getDatabaseFile( data.fileName );

                ResultReceiver receiver = new ResultReceiver( fragment.getHandler() ) {
                    protected void onReceiveResult( int resultCode, Bundle resultData ) {
                        long progress = resultData.getLong( NetworkUtils.CONTENT_PROGRESS );
                        publishProgress( (int) progress );
                    }
                };

                Bundle result = new Bundle();
                NetworkUtils.doHttpsGet( fragment.getContext(), HOST,
                        PATH + "/" +     data.fileName + ".gz", null,
                        dbFile, receiver, result, GZIPInputStream.class );
            } catch ( Exception e ) {
                UiUtils.showToast( fragment.getContext(), e.getMessage() );
                return -1;
            }

            return 0;
        }

        private int updateCatalog( DataInfo data ) {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return 1;
            }

            Date now = new Date();
            ContentValues values = new ContentValues();
            values.put( DatabaseManager.Catalog.TYPE, data.type );
            values.put( DatabaseManager.Catalog.DESCRIPTION, data.desc );
            values.put( DatabaseManager.Catalog.VERSION, data.version );
            values.put( DatabaseManager.Catalog.START_DATE, TimeUtils.format3339( data.start ) );
            values.put( DatabaseManager.Catalog.END_DATE, TimeUtils.format3339( data.end ) );
            values.put( DatabaseManager.Catalog.DB_NAME, data.fileName );
            values.put( DatabaseManager.Catalog.INSTALL_DATE, TimeUtils.format3339( now ) );

            Log.i( TAG, "Inserting catalog: type=" + data.type
                    + ", version=" + data.version + ", db=" + data.fileName );
            int rc = fragment.getDbManager().insertCatalogEntry( values );
            if ( rc < 0 ) {
                UiUtils.showToast( fragment.getContext(), "Failed to update catalog database" );
            }

            return rc;
        }
    }

    private static class DeleteDataTask extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog mProgressDialog;
        private WeakReference<DownloadFragment> mFragment;

        private DeleteDataTask( DownloadFragment fragment ) {
            mFragment = new WeakReference<>( fragment );
        }

        @Override
        protected void onPreExecute() {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return;
            }

            mProgressDialog = ProgressDialog.show( fragment.getContext(),
                    "", "Deleting installed data. Please wait...", true );
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return 1;
            }

            int result = 0;
            DatabaseManager dbManager = fragment.getDbManager();

            // Make sure all the databases we want to delete are closed
            dbManager.closeDatabases();

            // Get all the catalog entries
            SQLiteDatabase catalogDb = dbManager.getCatalogDb();
            Cursor cursor = catalogDb.query( DatabaseManager.Catalog.TABLE_NAME, null, null, null,
                    null, null, null );
            if ( cursor.moveToFirst() ) {
                do {
                    int _id = cursor.getInt( cursor.getColumnIndex( DatabaseManager.Catalog._ID ) );
                    String dbName = cursor.getString( cursor.getColumnIndex( DatabaseManager.Catalog.DB_NAME ) );

                    // Delete the db file on the external device
                    File file = dbManager.getDatabaseFile( dbName );
                    // Now delete the catalog entry for the file
                    int rows = catalogDb.delete( DatabaseManager.Catalog.TABLE_NAME, "_id=?",
                            new String[]{ Integer.toString( _id ) } );
                    if ( rows != 1 ) {
                        // If we could not delete the row, remember the error
                        result = -1;
                    }
                    if ( file.exists() ) {
                        file.delete();
                    }
                } while ( cursor.moveToNext() );
            }

            cursor.close();
            return result;
        }

        @Override
        protected void onPostExecute( Integer result ) {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return;
            }

            mProgressDialog.dismiss();
            if ( result != 0 ) {
                // Some or all data files were not deleted
                Toast.makeText( fragment.getContext(), "There was an error while deleting installed data",
                        Toast.LENGTH_LONG ).show();
            }

            // Refresh the download list view
            fragment.getHandler().postDelayed( fragment::checkData, 100 );
        }
    }

    private static class CheckDataTask extends AsyncTask<Boolean, Void, Integer> {
        private WeakReference<DownloadFragment> mFragment;

        private CheckDataTask( DownloadFragment fragment ) {
            mFragment = new WeakReference<>( fragment );
        }

        @Override
        protected void onPreExecute() {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return;
            }

            Button btnDownload = fragment.findViewById( R.id.btnDownload );
            btnDownload.setEnabled( false );
            fragment.clearData();
        }

        @Override
        protected Integer doInBackground( Boolean... params ) {
            int result = 1;

            DownloadFragment fragment = mFragment.get();
            if ( fragment == null ) {
                return result;
            }

            result = fragment.getInstalled();
            if ( result != 0 ) {
                return result;
            }

            result = fragment.downloadManifest();
            if ( result != 0 ) {
                return result;
            }

            result = fragment.parseManifest();
            if ( result != 0 ) {
                return result;
            }

            fragment.processManifest();

            return 0;
        }

        @Override
        protected void onPostExecute( Integer result ) {
            DownloadFragment fragment = mFragment.get();
            if ( fragment == null || fragment.getActivity() == null ) {
                return;
            }

            if ( result != 0 ) {
                TextView empty = fragment.findViewById( android.R.id.empty );
                empty.setText( R.string.download_error );
                return;
            }

            fragment.updateDownloadList();
        }

    }

    private final class DownloadCursor extends MatrixCursor {

        private static final String SECTION = "SECTION";
        private static final String TYPE = "TYPE";
        private static final String DESC = "DESC";
        private static final String DATES = "DATES";
        private static final String MSG = "MSG";
        private static final String EXPIRED = "EXPIRED";

        private int mId = 0;
        private final Date mNow = new Date();
        private final long mSpeed = 500;

        private DownloadCursor() {
            super( new String[]{ BaseColumns._ID, SECTION, TYPE, DESC, DATES, MSG, EXPIRED } );
        }

        public void addRow( int section, DataInfo info ) {
            RowBuilder builder = newRow();
            builder.add( mId++ )
                    .add( section )
                    .add( info.type )
                    .add( info.desc )
                    .add( "Effective " + DateUtils.formatDateRange( getActivity(),
                            info.start.getTime(), info.end.getTime() + 1000,
                            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_ALL ) )
                    .add( String.format( Locale.US, "%s (%s @ %dkbps)",
                            Formatter.formatShortFileSize( getActivity(), info.size ),
                            DateUtils.formatElapsedTime( info.size/( mSpeed*1024/8 ) ), mSpeed ) )
                    .add( !mNow.before( info.end )? "Y" : "N" );
        }

    }

    private final class DownloadListAdapter extends SectionedCursorAdapter {

        private DownloadListAdapter( Context context, Cursor c ) {
            super( context, R.layout.download_list_item, c, R.layout.list_item_header );
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
                mTrackers.put( type, new ProgressTracker( view ) );
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
            tv = view.findViewById( R.id.download_desc );
            tv.setText( cursor.getString( cursor.getColumnIndex( DownloadCursor.DESC ) ) );
            tv = view.findViewById( R.id.download_dates );
            String expired = cursor.getString( cursor.getColumnIndex( DownloadCursor.EXPIRED ) );
            tv.setText( cursor.getString( cursor.getColumnIndex( DownloadCursor.DATES ) ) );
            if ( expired.equals( "Y" ) ) {
                tv.setText( tv.getText() + " (Expired)" );
                tv.setTextColor( Color.RED );
            }
            tv = view.findViewById( R.id.download_msg );
            tv.setText( cursor.getString( cursor.getColumnIndex( DownloadCursor.MSG ) ) );
        }

    }

    private final class ProgressTracker {
        private TextView msgText;
        private TextView statusText;
        private ProgressBar progressBar;

        private ProgressTracker( View v ) {
            msgText = v.findViewById( R.id.download_msg );
            statusText = v.findViewById( R.id.download_status );
            progressBar = v.findViewById( R.id.download_progress );
        }

        private void initProgress( int max ) {
            progressBar.setMax( max );
            msgText.setText( R.string.installing );
            setProgress( 0 );
            showProgress();
        }

        private void showProgress() {
            progressBar.setVisibility( View.VISIBLE );
            statusText.setVisibility( View.VISIBLE );
            msgText.setVisibility( View.VISIBLE );
        }

        private void hideProgress() {
            progressBar.setVisibility( View.GONE );
            statusText.setVisibility( View.GONE );
            msgText.setVisibility( View.GONE );
        }

        public void setProgress( int progress ) {
            progressBar.setProgress( progress );
            if ( progress < progressBar.getMax() ) {
                Context context = getActivity();
                statusText.setText( String.format( "%s of %s",
                        Formatter.formatShortFileSize( context, progress ),
                        Formatter.formatShortFileSize( context, progressBar.getMax() ) ) );
            } else {
                msgText.setText( R.string.install_done );
                statusText.setVisibility( View.GONE );
            }
        }
    }

}
