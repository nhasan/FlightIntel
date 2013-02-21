/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public abstract class ListMenuFragment extends ListFragmentBase {

    public static final String MENU_ID = "MENU_ID";
    public static final String SUBTITLE_TEXT = "SUBTITLE_TEXT";

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        long id = args.getLong( MENU_ID );

        getSupportActionBar().setSubtitle( getItemTitle( id ) );

        Cursor c = getMenuCursor( id );
        setCursor( c );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return new ListMenuAdapter( context, c );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        long id = getListAdapter().getItemId( position );
        Class<?> clss = getItemFragmentClass( id );
        if ( clss != null ) {
            Bundle args = new Bundle();
            args.putLong( MENU_ID, id );
            args.putString( SUBTITLE_TEXT, getItemTitle( id ) );
            getActivityBase().replaceFragment( clss, args );
        }
    }

    protected abstract String getItemTitle( long id );
    protected abstract Class<?> getItemFragmentClass( long id );
    protected abstract Cursor getMenuCursor( long id );

    private class ListMenuAdapter extends ResourceCursorAdapter {

        public ListMenuAdapter( Context context, Cursor c ) {
            super( context, R.layout.list_menu_item, c, 0 );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            int icon = c.getInt( c.getColumnIndex( ListMenuCursor.ITEM_ICON ) );
            String title = c.getString( c.getColumnIndex( ListMenuCursor.ITEM_TITLE ) );
            String summary = c.getString( c.getColumnIndex( ListMenuCursor.ITEM_SUMMARY ) );

            ImageView iv = (ImageView) view.findViewById( R.id.item_icon );
            iv.setImageResource( icon );
            TextView tv = (TextView) view.findViewById( R.id.item_title );
            tv.setText( title );
            tv = (TextView) view.findViewById( R.id.item_summary );
            tv.setText( summary );
        }

    }

    public abstract static class ListMenuCursor extends MatrixCursor {

        public static final String ITEM_ICON = "ITEM_ICON";
        public static final String ITEM_TITLE = "ITEM_TITLE";
        public static final String ITEM_SUMMARY = "ITEM_SUMMARY";

        private final static String[] sColumnNames = new String[]
                { BaseColumns._ID, ITEM_ICON, ITEM_TITLE, ITEM_SUMMARY };

        public ListMenuCursor( long id ) {
            super( sColumnNames );
            populateMenuItems( id );
        }

        protected abstract void populateMenuItems( long id );
    }

}
