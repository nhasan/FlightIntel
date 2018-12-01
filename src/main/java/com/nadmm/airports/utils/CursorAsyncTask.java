/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor;
import android.os.AsyncTask;

import com.nadmm.airports.FragmentBase;

import java.lang.ref.WeakReference;

public abstract class CursorAsyncTask<T extends FragmentBase> extends AsyncTask<String, Void, Cursor[]> {
    private WeakReference<T> mFragment;

    public CursorAsyncTask( T fragment ) {
        mFragment = new WeakReference<>( fragment );
    }

    public T getFragment() {
        return mFragment.get();
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Cursor[] doInBackground( String... params ) {
        T fragment = mFragment.get();
        if ( fragment != null && fragment.getActivity() != null ) {
            return onExecute( fragment, params );
        }

        return null;
    }

    @Override
    protected final void onPostExecute( Cursor[] result ) {
        T fragment = mFragment.get();
        boolean close = true;
        if ( result != null ) {
            if ( fragment != null && fragment.getActivity() != null ) {
                close = onResult( fragment, result );
            }

            if ( close ) {
                for ( Cursor c : result ) {
                    if ( c != null ) {
                        c.close();
                    }
                }
            }
        }
    }

    protected abstract Cursor[] onExecute( T fragment, String... params );
    protected abstract boolean onResult( T fragment, Cursor[] result );

}
