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

package com.nadmm.airports.library;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nadmm.airports.Application;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Library;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

public class LibraryFragment extends FragmentBase {

    private boolean mIsOk;
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private OnClickListener mOnClickListener;
    private boolean mPending;
    private String mCategory;
    private HashMap<String, View> mBookRowMap = new HashMap<String, View>();

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        setRetainInstance( true );

        mFilter = new IntentFilter();
        mFilter.setPriority( 10 );
        mFilter.addAction( LibraryService.ACTION_CHECK_BOOKS );
        mFilter.addAction( LibraryService.ACTION_GET_BOOK );
        mFilter.addAction( LibraryService.ACTION_DOWNLOAD_PROGRESS );

        Bundle args = getArguments();
        mCategory = args.getString( Library.CATEGORY_CODE );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action.equals( LibraryService.ACTION_DOWNLOAD_PROGRESS ) ) {
                    handleProgress( intent );
                } else {
                    handleBook( intent );
                }
            }
        };

        mOnClickListener = new OnClickListener() {

            @Override
            public void onClick( View v ) {
                String path = (String) v.getTag( R.id.LIBRARY_PDF_PATH );
                if ( path == null ) {
                    if ( !mPending ) {
                        mPending = true;
                        ProgressBar progressBar = (ProgressBar) v.findViewById( R.id.progress );
                        progressBar.setIndeterminate( true );
                        progressBar.setVisibility( View.VISIBLE );
                        String name = (String) v.getTag( R.id.LIBRARY_PDF_NAME );
                        getBook( name );
                    }
                } else {
                    SystemUtils.startPDFViewer( getActivity(), path );
                }
            }
        };

        super.onCreate( savedInstanceState );
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver( mReceiver, mFilter );

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver( mReceiver );

        super.onPause();
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View v = inflate( R.layout.library_detail_view, container );
        return v;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        LibraryTask task = new LibraryTask();
        task.execute( mCategory );

        super.onActivityCreated( savedInstanceState );
    }

    private class LibraryTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String category = params[ 0 ];
            SQLiteDatabase db = getDatabase( DatabaseManager.DB_LIBRARY );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Library.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "DISTINCT "+Library.BOOK_DESC },
                    Library.CATEGORY_CODE+"=?", new String[] { category },
                    null, null, null );

            Cursor[] result = new Cursor[ c.getCount() ];

            if ( c.getCount() > 0 ) {
                builder = new SQLiteQueryBuilder();
                builder.setTables( Library.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Library.CATEGORY_CODE+"=?", new String[] { category },
                        null, null, Library._ID );
                c.moveToFirst();
                int i =0;
                String prevDesc = "";
                MatrixCursor matrix = null;
                do {
                    String name = c.getString( c.getColumnIndex( Library.BOOK_NAME ) );
                    String desc = c.getString( c.getColumnIndex( Library.BOOK_DESC ) );
                    String edition = c.getString( c.getColumnIndex( Library.EDITION ) );
                    String author = c.getString( c.getColumnIndex( Library.AUTHOR ) );
                    long size = c.getLong( c.getColumnIndex( Library.DOWNLOAD_SIZE ) );

                    if ( !desc.equals( prevDesc ) ) {
                        matrix = new MatrixCursor( c.getColumnNames() );
                        result[ i++ ] = matrix;
                    }
                    prevDesc = desc;

                    MatrixCursor.RowBuilder row = matrix.newRow();
                    row.add( 0 )
                        .add( category )
                        .add( name )
                        .add( desc )
                        .add( edition )
                        .add( author )
                        .add( size );
                } while ( c.moveToNext() );
            }

            return result;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showBooks( result );
            return true;
        }
    }

    private void showBooks( Cursor[] result ) {
        String msg;
        if ( !Application.sDonationDone ) {
            msg = "This function is only available after a donation";
            mIsOk = false;
        } else if ( !NetworkUtils.isNetworkAvailable( getActivity() ) ) {
            msg = "Not connected to the internet";
            mIsOk = false;
        } else if ( NetworkUtils.isConnectedToMeteredNetwork( getActivity() ) ) {
            msg = "Connected to a metered network";
            mIsOk = false;
        } else {
            msg = "Connected to an unmetered network";
            mIsOk = true;
        }
        TextView tv = (TextView) findViewById( R.id.msg_txt );
        tv.setText( msg );
        tv.setCompoundDrawablesWithIntrinsicBounds(
                mIsOk? R.drawable.check : R.drawable.delete, 0, 0, 0 );
        tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( getActivity(), 4 ) );

        LinearLayout topLayout = (LinearLayout) findViewById( R.id.parent_layout );
        for ( Cursor c : result ) {
            if ( c.moveToFirst() ) {
                LinearLayout layout = (LinearLayout) inflate( R.layout.library_detail_section,
                        topLayout );
                topLayout.addView( layout );
                do {
                    String name = c.getString( c.getColumnIndex( Library.BOOK_NAME ) );
                    String desc = c.getString( c.getColumnIndex( Library.BOOK_DESC ) );
                    String edition = c.getString( c.getColumnIndex( Library.EDITION ) );
                    String author = c.getString( c.getColumnIndex( Library.AUTHOR ) );
                    long size = c.getLong( c.getColumnIndex( Library.DOWNLOAD_SIZE ) );
                    int resid = UiUtils.getRowSelectorForCursor( c );
                    addLibraryRow( layout, name, desc, edition, author, size, resid );
                } while ( c.moveToNext() );
            }
        }

        checkBooks();
    }

    private View addLibraryRow( LinearLayout layout, String name, String desc, String edition,
            String author, long size, int resid ) {
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }
        RelativeLayout row = (RelativeLayout) inflate( R.layout.library_row_item );
        TextView tv = (TextView) row.findViewById( R.id.book_desc );
        tv.setText( desc );
        tv = (TextView) row.findViewById( R.id.book_edition );
        tv.setText( edition );
        tv = (TextView) row.findViewById( R.id.book_author );
        tv.setText( author );
        tv = (TextView) row.findViewById( R.id.book_size );
        tv.setText( Formatter.formatShortFileSize( getActivity(), size ) );
        row.setTag( R.id.LIBRARY_PDF_NAME, name );
        row.setOnClickListener( mOnClickListener );
        row.setBackgroundResource( resid );
        showStatus( row, false );
        mBookRowMap.put( name, row );
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected void handleBook( Intent intent ) {
        String pdfName = intent.getStringExtra( LibraryService.BOOK_NAME );
        View row = mBookRowMap.get( pdfName );
        if ( row == null ) {
            // Maybe for a different category
            return;
        }

        String path = intent.getStringExtra( LibraryService.PDF_PATH );
        if ( path != null ) {
            showStatus( row, true );
            row.setTag( R.id.LIBRARY_PDF_PATH, path );
            String action = intent.getAction();
            if ( action.equals( LibraryService.ACTION_GET_BOOK ) ) {
                SystemUtils.startPDFViewer( getActivity(), path );
            }
        } else {
            showStatus( row, false );
        }
    }

    protected void handleProgress( Intent intent ) {
        String name = intent.getStringExtra( NetworkUtils.CONTENT_NAME );
        View row = mBookRowMap.get( name );
        if ( row != null ) {
            ProgressBar progressBar = (ProgressBar) row.findViewById( R.id.progress );
            long length = intent.getLongExtra( NetworkUtils.CONTENT_LENGTH, 0 );
            if ( !progressBar.isShown() ) {
                progressBar.setVisibility( View.VISIBLE );
            }
            progressBar.setMax( (int) length );
            progressBar.setIndeterminate( false );
            long progress = intent.getLongExtra( NetworkUtils.CONTENT_PROGRESS, 0 );
            progressBar.setProgress( (int) progress );
            if ( progress >= length ) {
                progressBar.setVisibility( View.GONE );
                mPending = false;
            }
        }
    }

    protected void showStatus( View row, boolean isAvailable ) {
        TextView tv = (TextView) row.findViewById( R.id.book_desc );
        if ( isAvailable ) {
            tv.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.btn_check_on_holo_light, 0, 0, 0 );
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.btn_check_off_holo_light, 0, 0, 0 );
        }
    }

    protected void getBook( String name ) {
        Intent service = makeServiceIntent( LibraryService.ACTION_GET_BOOK );
        service.putExtra( LibraryService.BOOK_NAME, name );
        getActivity().startService( service );
    }

    protected void checkBooks() {
        Intent service = makeServiceIntent( LibraryService.ACTION_CHECK_BOOKS );
        ArrayList<String> books = new ArrayList<String>( mBookRowMap.keySet() );
        service.putExtra( LibraryService.BOOK_NAMES, books );
        getActivity().startService( service );
    }

    protected Intent makeServiceIntent( String action ) {
        Intent service = new Intent( getActivity(), LibraryService.class );
        service.setAction( action );
        service.putExtra( LibraryService.CATEGORY, mCategory );
        return service;
    }

}
