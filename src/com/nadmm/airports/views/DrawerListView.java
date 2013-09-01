package com.nadmm.airports.views;

import android.content.Context;
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
    public static final int ITEM_ID_TFR = 1;
    public static final int ITEM_ID_LIBRARY = 2;
    public static final int ITEM_ID_SCRATCHPAD = 3;
    public static final int ITEM_ID_CLOCKS = 4;
    public static final int ITEM_ID_E6B = 5;
    public static final int ITEM_ID_CHARTS = 6;

    public DrawerListView( Context context, AttributeSet attrs ) {
        super( context, attrs );
        init( context );
    }

    public DrawerListView( Context context ) {
        super( context );
        init( context );
    }

    private void init( Context context ) {
        setAdapter( new DrawerAdapter( context, new DrawerCursor() ) );
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
