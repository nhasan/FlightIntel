/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.afd.AfdMainActivity;

public abstract class ListFragmentBase extends FragmentBase
        implements LocationListener, IRefreshable {

    public static final String FRAGMENT_ID = "FRAGMENT_ID";

    private ListView mListView;
    private String mEmptyText;
    private int mFragmentId;
    private int mContentTopClearance;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Bundle args = getArguments();
        if ( args.containsKey( FRAGMENT_ID ) ) {
            mFragmentId = args.getInt( FRAGMENT_ID );
        } else {
            mFragmentId = getId();
        }
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.list_view_layout, container, false );
        mListView = (ListView) view.findViewById( android.R.id.list );
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                onListItemClick( mListView, view, position );
            }
        } );
        mListView.setCacheColorHint( getResources().getColor( android.R.color.white ) );

        return createContentView( view );
    }

    @Override
    public void onDestroy() {
        if ( mListView != null ) {
            CursorAdapter adapter = (CursorAdapter) mListView.getAdapter();
            if ( adapter != null ) {
                Cursor c = adapter.getCursor();
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
    public boolean isRefreshable() {
        return false;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return ViewCompat.canScrollVertically( mListView, -1 );
    }

    @Override
    public void requestDataRefresh() {
    }

    public void setContentTopClearance( int clearance ) {
        if ( mContentTopClearance != clearance ) {
            mContentTopClearance = clearance;
            mListView.setPadding( mListView.getPaddingLeft(), mContentTopClearance,
                    mListView.getPaddingRight(), mListView.getPaddingBottom() );
            CursorAdapter adapter = (CursorAdapter) mListView.getAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    protected void setCursor( Cursor c ) {
        if ( getActivity() == null ) {
            // We may get called here after activity has detached
            c.close();
            return;
        }

        setListShown( c.getCount() > 0 );

        CursorAdapter adapter = (CursorAdapter) mListView.getAdapter();
        if ( adapter == null ) {
            adapter = newListAdapter( getActivity(), c );
            mListView.setAdapter( adapter );
        } else {
            adapter.changeCursor( c );
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
        return mListView.getAdapter();
    }

    public ListView getListView() {
        return mListView;
    }

    abstract protected CursorAdapter newListAdapter( Context context, Cursor c );

    abstract protected void onListItemClick( ListView l, View v, int position );

    @Override
    public void onLocationChanged( Location location ) {
    }

    @Override
    public void onProviderDisabled( String provider ) {
    }

    @Override
    public void onProviderEnabled( String provider ) {
    }

    @Override
    public void onStatusChanged( String provider, int status, Bundle extras ) {
    }

}
