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

package com.nadmm.airports.e6b;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;

public class E6bMenuFragment extends ListFragmentBase {

    private static final String MENU_ID = "MENU_ID";

    private static final HashMap<Long, Class<?>> mDispatchMap = new HashMap<Long, Class<?>>();
    static {
        mDispatchMap.put( (long)R.id.CATEGORY_MAIN, E6bMenuFragment.class );
        mDispatchMap.put( (long)R.id.CATEGORY_TIME, E6bMenuFragment.class );
        mDispatchMap.put( (long)R.id.TIME_CLOCKS, ClockFragment.class );
    }

    private static final HashMap<Long, String> mTitleMap = new HashMap<Long, String>();
    static {
        mTitleMap.put( (long)R.id.CATEGORY_MAIN, "Main Menu" );
        mTitleMap.put( (long)R.id.CATEGORY_TIME, "Time" );
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        long menuId = args != null? args.getLong( MENU_ID ) : R.id.CATEGORY_MAIN;

        getSupportActionBar().setSubtitle( mTitleMap.get( menuId ) );

        Cursor c = new E6bMenuCursor( menuId );
        setCursor( c );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return new E6bMenuAdapter( context, c );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        long id = getListAdapter().getItemId( position );
        Class<?> clss = mDispatchMap.get( id );
        if ( clss != null ) {
            Bundle args = new Bundle();
            args.putLong( MENU_ID, id );
            getActivityBase().replaceFragment( clss, args );
        }
    }

    private class E6bMenuAdapter extends ResourceCursorAdapter {

        public E6bMenuAdapter( Context context, Cursor c ) {
            super( context, R.layout.e6b_menu_item, c, 0 );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            int icon = c.getInt( c.getColumnIndex( E6bMenuCursor.ITEM_ICON ) );
            String title = c.getString( c.getColumnIndex( E6bMenuCursor.ITEM_TITLE ) );
            String summary = c.getString( c.getColumnIndex( E6bMenuCursor.ITEM_SUMMARY ) );

            ImageView iv = (ImageView) view.findViewById( R.id.item_icon );
            iv.setImageResource( icon );
            TextView tv = (TextView) view.findViewById( R.id.item_title );
            tv.setText( title );
            tv = (TextView) view.findViewById( R.id.item_summary );
            tv.setText( summary );
        }

    }

}
