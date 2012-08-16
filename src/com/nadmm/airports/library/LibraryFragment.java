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
    private HashMap<String, View> mBookRowMap = new HashMap<String, View>();

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        setRetainInstance( true );

        mFilter = new IntentFilter();
        mFilter.addAction( LibraryService.ACTION_CHECK_BOOKS );
        mFilter.addAction( LibraryService.ACTION_GET_BOOK );

        mReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive( Context context, Intent intent ) {
                handleBroadcast( intent );
            }
        };

        mOnClickListener = new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                String name = (String) v.getTag();
                getBook( name );
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
        Bundle args = getArguments();
        String category = args.getString( Library.CATEGORY_CODE );
        LibraryTask task = new LibraryTask();
        task.execute( category );

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
                        null, null, null );
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
        row.setTag( name );
        row.setOnClickListener( mOnClickListener );
        row.setBackgroundResource( resid );
        showStatus( row, false );
        mBookRowMap.put( name, row );
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected void handleBroadcast( Intent intent ) {
        String action = intent.getAction();
        String pdfName = intent.getStringExtra( LibraryService.BOOK_NAME );
        String path = intent.getStringExtra( LibraryService.PDF_PATH );

        View row = mBookRowMap.get( pdfName );
        if ( row == null ) {
            return;
        }

        if ( path != null ) {
            showStatus( row, true );
            if ( action.equals( LibraryService.ACTION_GET_BOOK ) ) {
                SystemUtils.startPDFViewer( getActivity(), path );
            }
        } else {
            showStatus( row, false );
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
        Bundle args = getArguments();
        String category = args.getString( Library.CATEGORY_CODE );
        service.putExtra( LibraryService.CATEGORY, category );
        return service;
    }

}
