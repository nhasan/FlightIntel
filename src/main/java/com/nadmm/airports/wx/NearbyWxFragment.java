/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2017 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.wx;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.nadmm.airports.data.DatabaseManager;

public class NearbyWxFragment extends WxListFragmentBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setEmptyText( "No wx stations found nearby." );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );
    }

    @Override
    protected LocationTask newLocationTask() {
        return new NearbyWxTask();
    }

    private final class NearbyWxTask extends LocationTask {

        @Override
        protected Cursor doInBackground( Void... params ) {
            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            return new NearbyWxCursor( db, getLastLocation(), getNearbyRadius() );
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            setCursor( c );
        }
    }

}
