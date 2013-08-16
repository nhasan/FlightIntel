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

import com.nadmm.airports.afd.FavoritesFragment;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ActivityBase {

    public static final int ITEM_ID_AFD = 0;
    public static final int ITEM_ID_TFR = 1;
    public static final int ITEM_ID_LIBRARY = 2;
    public static final int ITEM_ID_SCRATCHPAD = 3;
    public static final int ITEM_ID_CLOCKS = 4;
    public static final int ITEM_ID_E6B = 5;
    public static final int ITEM_ID_CHARTS = 6;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        mTitle = mDrawerTitle = getTitle();

        mDrawerList = (ListView) findViewById( R.id.left_drawer );
        mDrawerList.setAdapter( new DrawerAdapter( this, new DrawerCursor() ) );
        mDrawerList.setOnItemClickListener( new DrawerItemClickListener() );

        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout );
        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed( View view ) {
                getSupportActionBar().setTitle( mTitle );
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened( View drawerView ) {
                getSupportActionBar().setTitle( mDrawerTitle );
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener( mDrawerToggle );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
        selectItem( ITEM_ID_AFD );
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged( newConfig );
        mDrawerToggle.onConfigurationChanged( newConfig );
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen( mDrawerList );
        menu.findItem( R.id.menu_refresh ).setVisible( !drawerOpen );
        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if ( mDrawerToggle.onOptionsItemSelected( item ) ) {
          return true;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    public void setTitle( CharSequence title ) {
        mTitle = title;
        getSupportActionBar().setTitle( mTitle );
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @SuppressWarnings("rawtypes")
        @Override
        public void onItemClick( AdapterView parent, View view, int position, long id ) {
            selectItem( position );
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem( int position ) {
        // Create a new fragment and specify the planet to show based on position
        Class<?> clss = null;
        long id = mDrawerList.getItemIdAtPosition( position );
        if ( id == ITEM_ID_AFD ) {
            clss = FavoritesFragment.class;
        } else if ( id == ITEM_ID_TFR ) {
        } else if ( id == ITEM_ID_LIBRARY ) {
        } else if ( id == ITEM_ID_SCRATCHPAD ) {
        } else if ( id == ITEM_ID_CHARTS ) {
        } else if ( id == ITEM_ID_CLOCKS ) {
        } else if ( id == ITEM_ID_E6B ) {
        }

        if ( clss != null ) {
            replaceFragment( clss, null );
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked( position, true );
        mDrawerLayout.closeDrawer( mDrawerList );
    }

    @Override
    public Fragment replaceFragment( Class<?> clss, Bundle args ) {
        return replaceFragment( clss, args, R.id.content_frame );
    }

    protected class DrawerAdapter extends ResourceCursorAdapter {

        public DrawerAdapter( Context context, Cursor c ) {
            super( context, R.layout.sliding_menu_item, c, 0 );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            String text = c.getString( c.getColumnIndex( DrawerCursor.ITEM_TEXT ) );
            int icon = c.getInt( c.getColumnIndex( DrawerCursor.ITEM_ICON ) );
            TextView tv = (TextView) view.findViewById( R.id.item_text );
            tv.setText( text );
            ImageView iv = (ImageView) view.findViewById( R.id.item_icon );
            iv.setImageResource( icon );
        }

    }

    protected static class DrawerCursor extends MatrixCursor {

        private static final String ITEM_TEXT = "ITEM_TEXT";
        private static final String ITEM_ICON = "ITEM_ICON";

        private final static String[] sColumnNames = new String[]
                { BaseColumns._ID, ITEM_TEXT, ITEM_ICON };

        public DrawerCursor() {
            super( sColumnNames );
            newRow().add( ITEM_ID_AFD ).add( "A/FD" ).add( R.drawable.airport );
            newRow().add( ITEM_ID_TFR ).add( "TFRs" ).add( R.drawable.stop );
            newRow().add( ITEM_ID_LIBRARY ).add( "Library" ).add( R.drawable.book );
            newRow().add( ITEM_ID_SCRATCHPAD ).add( "Scratch Pad" ).add( R.drawable.notepad );
            newRow().add( ITEM_ID_CLOCKS ).add( "Clocks" ).add( R.drawable.clock );
            newRow().add( ITEM_ID_E6B ).add( "E6B" ).add( R.drawable.e6b );
            newRow().add( ITEM_ID_CHARTS ).add( "Manage Charts" ).add( R.drawable.folder );
        }

    }

}
