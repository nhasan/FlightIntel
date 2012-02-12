package com.nadmm.airports.utils;

import android.database.Cursor;
import android.os.AsyncTask;

public abstract class CursorAsyncTask extends AsyncTask<String, Void, Cursor[]> {

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected final void onPostExecute( Cursor[] result ) {
        onResult( result );
        for ( Cursor c : result ) {
            if ( c != null ) {
                c.close();
            }
        }
    }

    protected abstract void onResult( Cursor[] result );

}