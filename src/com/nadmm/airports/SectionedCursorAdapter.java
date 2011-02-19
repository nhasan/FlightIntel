/*
 * Airports for Android
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public abstract class SectionedCursorAdapter extends ResourceCursorAdapter {

    /**
     * Id of the view that represents the section header in list item layout
     */
    private int mSectionId;
    /**
     * Map to cache the section names for each row in the cursor
     */
    private HashMap<Integer, String> mSectionNames;

    public SectionedCursorAdapter( Context context, int layout, Cursor c, int sectionId ) {
        super( context, layout, c );
        mSectionId = sectionId;
        mSectionNames = new HashMap<Integer, String>( c.getCount() );
    }

    /**
     * Called by newView() to get the section name. If the section name is different than
     * previous section name, a new section has begun and the section header is made visible.
     * Cursor is already pointing to the correct row
     * @return Key that uniquely identifies a section
     */
    public abstract String getSectionName();

    @Override
    public View newView( Context context, Cursor cursor, ViewGroup parent ) {
        View view = super.newView( context, cursor, parent );

        // Get the view that represents the section header
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
            // Section name is not in the cache, ask the implementation for it
            curSectionName = getSectionName();
            mSectionNames.put( position, curSectionName );
        }

        if ( position == 0 ) {
            // First item always starts a new section
            section.setVisibility( View.VISIBLE );
            section.setText( curSectionName );
        } else {
            // Need to check if this position starts a new section by comparing the section
            // name of this row with the section name of the previous row
            cursor.moveToPrevious();
            String prevSectionName = mSectionNames.get( position-1 );
            if ( prevSectionName == null ) {
                prevSectionName = getSectionName();
                mSectionNames.put( position-1, curSectionName );
            }

            // Restore cursor position
            cursor.moveToNext();

            if ( !curSectionName.equals( prevSectionName ) ) {
                // A new section begins at this position, show the section header
                section.setVisibility( View.VISIBLE );
                section.setText( curSectionName );
            } else {
                // Same section as previous row, hide the section header if visible
                section.setVisibility( View.GONE );
            }
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
    protected void onContentChanged() {
        super.onContentChanged();
        // Reset section name cache
        mSectionNames.clear();
    }
}
