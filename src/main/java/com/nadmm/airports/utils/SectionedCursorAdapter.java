/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
 * Copyright 2012 Google Inc
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

package com.nadmm.airports.utils;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;


public abstract class SectionedCursorAdapter extends ResourceCursorAdapter {

    private int mSectionResourceId;
    private LayoutInflater mLayoutInflater;
    private SparseArray<Section> mSections = new SparseArray<Section>();

    public static class Section {
        int firstPosition;
        int sectionedPosition;
        CharSequence title;

        public Section(int firstPosition, CharSequence title) {
            this.firstPosition = firstPosition;
            this.title = title;
        }

        public CharSequence getTitle() {
            return title;
        }
    }

    /**
     * Id of the view that represents the section header in list item layout
     */
    private int mSectionId;
    /**
     * Map to cache the section names for each row in the cursor
     */
    private HashMap<Integer, String> mSectionNames;

    public SectionedCursorAdapter( Context context, int layout, Cursor c, int sectionResourceId ) {
        super( context, layout, c, 0 );
        mSectionResourceId = sectionResourceId;
        mLayoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        setSections();
    }

    private void setSections() {
        Cursor c = getCursor();
        if ( c.moveToFirst() ) {
            ArrayList<Section> sections = new ArrayList<Section>();
            String last = "";
            int offset = 0; // offset positions for the headers we're adding
            do {
                String cur = getSectionName();
                if ( !cur.contentEquals( last ) ) {
                    Section section = new Section( c.getPosition(), cur );
                    section.sectionedPosition = section.firstPosition + offset;
                    mSections.append( section.sectionedPosition, section );
                    ++offset;
                    last = cur;
                }
            } while ( c.moveToNext() );
        }
    }

    public abstract String getSectionName();

    public int positionToSectionedPosition( int position ) {
        int offset = 0;
        for ( int i = 0; i < mSections.size(); i++ ) {
            if ( mSections.valueAt( i ).firstPosition > position ) {
                break;
            }
            ++offset;
        }
        return position + offset;
    }

    public int sectionedPositionToPosition( int sectionedPosition ) {
        if ( isSectionHeaderPosition( sectionedPosition ) ) {
            return ListView.INVALID_POSITION;
        }

        int offset = 0;
        for ( int i = 0; i < mSections.size(); i++ ) {
            if ( mSections.valueAt( i ).sectionedPosition > sectionedPosition ) {
                break;
            }
            --offset;
        }
        return sectionedPosition + offset;
    }

    public boolean isSectionHeaderPosition( int position ) {
        return mSections.get( position ) != null;
    }

    @Override
    public int getCount() {
        return ( getCursor().getCount() > 0? getCursor().getCount() + mSections.size() : 0 );
    }

    @Override
    public Object getItem( int position ) {
        return isSectionHeaderPosition( position )
                ? mSections.get( position )
                : super.getItem( sectionedPositionToPosition( position ) );
    }

    @Override
    public long getItemId( int position ) {
        return isSectionHeaderPosition( position )
                ? Integer.MAX_VALUE - mSections.indexOfKey( position )
                : sectionedPositionToPosition(position);
    }

    @Override
    public int getItemViewType( int position ) {
        return isSectionHeaderPosition( position )? 0 : 1;
    }

    @Override
    public boolean isEnabled( int position ) {
        return isSectionHeaderPosition( position )? false : true;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        if ( isSectionHeaderPosition( position ) ) {
            if ( convertView == null ) {
                convertView = mLayoutInflater.inflate( mSectionResourceId, parent, false );
            }
            TextView tv = (TextView) convertView;
            tv.setText( mSections.get( position ).title );
        } else {
            convertView = super.getView( sectionedPositionToPosition( position ), convertView, parent );
        }

        return convertView;
    }

    @Override
    public void changeCursor( Cursor c ) {
        super.changeCursor(c);
        setSections();
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        Cursor c = getCursor();
        setSections();
    }

}
