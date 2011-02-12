package com.nadmm.airports;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public abstract class SectionedCursorAdapter extends SimpleCursorAdapter {
    private int mSectionId;
    private HashMap<Integer, String> mSectionNames;

    public SectionedCursorAdapter( Context context, int layout, Cursor c,
            String[] from, int[] to, int sectionId ) {
        super( context, layout, c, from, to );
        mSectionId = sectionId;
        mSectionNames = new HashMap<Integer, String>( c.getCount() );
    }

    /**
     * Called by newView() to get the section key. If the section key is different than
     * previous section key, we need to show the section header. Cursor is already pointing
     * to the correct row
     * @return Key that uniquely identifies a section
     */
    public abstract String getSectionName();

    @Override
    public View newView( Context context, Cursor cursor, ViewGroup parent ) {
        View view = super.newView( context, cursor, parent );

        TextView section = (TextView) view.findViewById( mSectionId );
        section.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick( View v ) {
                // Ignore clicks to the section title
            }
        } );

        int position = cursor.getPosition();
        String curSectionName = mSectionNames.get( position );
        if ( curSectionName == null ) {
            curSectionName = getSectionName();
            mSectionNames.put( position, curSectionName );
        }
        if ( position > 0 ) {
            // Need to check if this position starts a new section
            cursor.moveToPrevious();
            String prevSectionName = mSectionNames.get( position-1 );
            if ( prevSectionName == null ) {
                prevSectionName = getSectionName();
                mSectionNames.put( position-1, curSectionName );
            }

            // Restore cursor position
            cursor.moveToNext();

            if ( !curSectionName.equals( prevSectionName ) ) {
                // A new section begins at this position
                section.setVisibility( View.VISIBLE );
                section.setText( curSectionName );
            } else {
                section.setVisibility( View.GONE );
            }
        } else {
            // First item always starts a new section
            section.setVisibility( View.VISIBLE );
            section.setText( curSectionName );
        }

        return view;
    }

    @Override
    public void changeCursor( Cursor c ) {
        super.changeCursor( c );
        // Reset section name cache
        mSectionNames.clear();
    }

    @Override
    public void changeCursorAndColumns( Cursor c, String from[], int[] to ) {
        super.changeCursorAndColumns( c, from, to );
        // Reset section name cache
        mSectionNames.clear();
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        // Reset section name cache
        mSectionNames.clear();
    }
}
