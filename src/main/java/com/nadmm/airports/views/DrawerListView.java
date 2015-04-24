/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.views;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.BaseColumns;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.R;

public final class DrawerListView extends ListView {

    public static final int ITEM_ID_AFD = 0;
    public static final int ITEM_ID_WX = 1;
    public static final int ITEM_ID_TFR = 2;
    public static final int ITEM_ID_LIBRARY = 3;
    public static final int ITEM_ID_SCRATCHPAD = 4;
    public static final int ITEM_ID_CLOCKS = 5;
    public static final int ITEM_ID_E6B = 6;
    public static final int ITEM_ID_CHARTS = 7;

    public DrawerListView( Context context, AttributeSet attrs ) {
        super( context, attrs );
        init( context );
    }

    public DrawerListView( Context context ) {
        super( context );
        init( context );
    }

    private void init( Context context ) {
        setAdapter( new DrawerAdapter( context, new DrawerCursor( context ) ) );
    }

    protected class DrawerAdapter extends ResourceCursorAdapter {

        public DrawerAdapter( Context context, Cursor c ) {
            super( context, R.layout.navdrawer_item, c, 0 );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            String text = c.getString( c.getColumnIndex( DrawerCursor.ITEM_TEXT ) );
            int icon = c.getInt( c.getColumnIndex( DrawerCursor.ITEM_ICON ) );
            TextView tv = (TextView) view.findViewById( R.id.item_title );
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

        public DrawerCursor( Context context ) {
            super( sColumnNames );
            Resources res = context.getResources();
            newRow().add( ITEM_ID_AFD )
                    .add( res.getString( R.string.afd ) )
                    .add( R.drawable.ic_navdrawer_afd );
            newRow().add( ITEM_ID_WX )
                    .add( res.getString( R.string.weather ) )
                    .add( R.drawable.ic_navdrawer_wx );
            newRow().add( ITEM_ID_TFR )
                    .add( res.getString( R.string.tfrs ) )
                    .add( R.drawable.ic_navdrawer_tfr );
            newRow().add( ITEM_ID_LIBRARY )
                    .add( res.getString( R.string.library ) )
                    .add( R.drawable.ic_navdrawer_library );
            newRow().add( ITEM_ID_SCRATCHPAD )
                    .add( res.getString( R.string.scratch_pad ) )
                    .add( R.drawable.ic_navdrawer_scratchpad );
            newRow().add( ITEM_ID_CLOCKS )
                    .add( res.getString( R.string.clocks ) )
                    .add( R.drawable.ic_navdrawer_clocks );
            newRow().add( ITEM_ID_E6B )
                    .add( res.getString( R.string.e6b ) )
                    .add( R.drawable.ic_navdrawer_e6b );
            newRow().add( ITEM_ID_CHARTS )
                    .add( res.getString( R.string.charts ) )
                    .add( R.drawable.ic_navdrawer_charts );
        }

    }

}
