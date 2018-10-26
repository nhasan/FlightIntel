/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2016 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public abstract class ListFragmentBase extends FragmentBase {

    private static final String LISTVIEW_STATE = "LISTVIEW_STATE";

    private ListView mListView;
    private String mEmptyText;
    private Parcelable mListViewState;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        if ( savedInstanceState != null && savedInstanceState.containsKey( LISTVIEW_STATE ) ) {
            mListViewState = savedInstanceState.getParcelable( LISTVIEW_STATE );
        }
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.list_view_layout, container, false );
        mListView = view.findViewById( android.R.id.list );
        mListView.setOnItemClickListener( ( parent, view1, position, id )
                -> onListItemClick( mListView, view1, position ) );
        mListView.setCacheColorHint( ContextCompat.getColor(
                getActivity(), android.R.color.white ) );

        return createContentView( view );
    }

    @Override
    public void onDestroy() {
        if ( mListView != null ) {
            ListAdapter adapter = mListView.getAdapter();
            if ( adapter instanceof CursorAdapter ) {
                Cursor c = ( (CursorAdapter)adapter ).getCursor();
                c.close();
            }
        }

        super.onDestroy();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );
        setFragmentContentShownNoAnimation( false );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        if ( mListView != null ) {
            mListViewState = mListView.onSaveInstanceState();
            outState.putParcelable( LISTVIEW_STATE, mListViewState );
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return mListView.canScrollVertically( -1 );
    }

    protected void setCursor( Cursor c ) {
        if ( getActivity() == null ) {
            // We may get called here after activity has detached
            c.close();
            return;
        }

        CursorAdapter adapter = (CursorAdapter) mListView.getAdapter();
        if ( adapter == null ) {
            adapter = newListAdapter( getActivity(), c );
            setAdapter( adapter );
        } else {
            adapter.changeCursor( c );
            setListShown( c.getCount() > 0 );
        }
    }

    public void setAdapter( ListAdapter adapter ) {
        mListView.setAdapter( adapter );

        int count = adapter == null? 0 : adapter.getCount();
        setListShown( count > 0 );

        if ( mListViewState != null ) {
            mListView.onRestoreInstanceState( mListViewState );
            mListViewState = null;
        }

        setFragmentContentShown( true );
    }

    public void setEmptyText( String text ) {
        mEmptyText = text;
    }

    public void setListShown( boolean show ) {
        TextView tv = (TextView) findViewById( android.R.id.empty );
        if ( show ) {
            tv.setVisibility( View.GONE );
            mListView.setVisibility( View.VISIBLE );
        } else {
            tv.setText( mEmptyText );
            tv.setVisibility( View.VISIBLE );
            mListView.setVisibility( View.GONE );
        }
    }

    public ListAdapter getListAdapter() {
        return mListView != null? mListView.getAdapter() : null;
    }

    public ListView getListView() {
        return mListView;
    }

    protected CursorAdapter newListAdapter( Context context, Cursor c )
    {
        return null;
    }

    abstract protected void onListItemClick( ListView l, View v, int position );

}
