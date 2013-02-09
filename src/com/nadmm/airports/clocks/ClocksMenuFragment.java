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

package com.nadmm.airports.clocks;

import java.util.HashMap;

import android.database.Cursor;

import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

public class ClocksMenuFragment extends ListMenuFragment {

    private static final HashMap<Long, Class<?>> mDispatchMap;
    static {
        mDispatchMap = new HashMap<Long, Class<?>>();
        mDispatchMap.put( (long)R.id.TIME_CLOCKS, ClockFragment.class );
        mDispatchMap.put( (long)R.id.TIME_STOPWATCH, StopWatchFragment.class );
        mDispatchMap.put( (long)R.id.TIME_COUNTDOWN, CountDownFragment.class );
    }

    private static final HashMap<Long, String> mTitleMap;
    static {
        mTitleMap = new HashMap<Long, String>();
        mTitleMap.put( (long)R.id.CATEGORY_MAIN, "Clocks for Instrument Flying" );
        mTitleMap.put( (long)R.id.TIME_CLOCKS, "Current Time" );
        mTitleMap.put( (long)R.id.TIME_STOPWATCH, "Stop Watch" );
        mTitleMap.put( (long)R.id.TIME_COUNTDOWN, "Countdown Timer" );
    }

    @Override
    protected String getItemTitle( long itemId ) {
        return mTitleMap.get( itemId );
    }

    @Override
    protected Class<?> getItemFragmentClass( long itemId ) {
        return mDispatchMap.get( itemId );
    }

    @Override
    protected Cursor getMenuCursor() {
        return new ClocksMenuCursor();
    }

    public class ClocksMenuCursor extends ListMenuCursor {

        public ClocksMenuCursor() {
            super();
        }

        @Override
        protected void populateMenuItems() {
            newRow().add( R.id.TIME_CLOCKS )
                .add( R.drawable.clock )
                .add( getItemTitle( R.id.TIME_CLOCKS ) )
                .add( "Display UTC clock, local clock" );
            newRow().add( R.id.TIME_STOPWATCH )
                .add( R.drawable.stopwatch )
                .add( getItemTitle( R.id.TIME_STOPWATCH ) )
                .add( "Stop watch for timing legs and approaches" );
            newRow().add( R.id.TIME_COUNTDOWN )
                .add( R.drawable.countdown )
                .add( getItemTitle( R.id.TIME_COUNTDOWN ) )
                .add( "Countdown timer for timing approaches and holds" );
        }
    }

}
