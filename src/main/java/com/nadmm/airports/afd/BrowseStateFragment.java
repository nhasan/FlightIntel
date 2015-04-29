package com.nadmm.airports.afd;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DownloadActivity;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.SectionedCursorAdapter;

import java.util.HashMap;
import java.util.Locale;

public class BrowseStateFragment extends AirportListFragment {

    // Projection map for queries
    static private final HashMap<String, String> sStateMap;
    static {
        sStateMap = new HashMap<>();
        sStateMap.put(BaseColumns._ID, "max(" + BaseColumns._ID + ") AS " + BaseColumns._ID);
        sStateMap.put(DatabaseManager.Airports.ASSOC_STATE, DatabaseManager.Airports.ASSOC_STATE);
        sStateMap.put(DatabaseManager.States.STATE_NAME,
                "IFNULL(" + DatabaseManager.States.STATE_NAME + ", " + DatabaseManager.Airports.ASSOC_COUNTY + ")"
                        + " AS " + DatabaseManager.States.STATE_NAME);
        sStateMap.put(BaseColumns._COUNT, "count(*) AS " + BaseColumns._COUNT);
    }

    private SectionedCursorAdapter mAdapter;

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        setBackgroundTask( new BrowseTask() ).execute();

        super.onActivityCreated( savedInstanceState );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        mAdapter = new StateCursorAdapter( getActivity(), R.layout.browse_all_item, c );
        return mAdapter;
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        position = mAdapter.sectionedPositionToPosition( position );
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        Cursor c = adapter.getCursor();
        c.moveToPosition( position );
        Bundle args = new Bundle();
        String state_code = c.getString( c.getColumnIndex( DatabaseManager.Airports.ASSOC_STATE ) );
        String state_name = c.getString( c.getColumnIndex( DatabaseManager.States.STATE_NAME ) );
        args.putString( DatabaseManager.States.STATE_CODE, state_code );
        args.putString( DatabaseManager.States.STATE_NAME, state_name );
        getActivityBase().replaceFragment( BrowseAirportsFragment.class, args );
    }

    private final class StateCursorAdapter extends SectionedCursorAdapter {

        public static final int STATE_MODE = 0;
        public static final int CITY_MODE = 1;

        public StateCursorAdapter( Context context, int layout, Cursor c ) {
            super( context, layout, c, R.layout.list_item_header );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            // Browsing all states
            String state_name = c.getString( c.getColumnIndex( DatabaseManager.States.STATE_NAME ) );
            int count = c.getInt( c.getColumnIndex( BaseColumns._COUNT ) );
            TextView tv = (TextView) view.findViewById( R.id.browse_state_name );
            tv.setText( String.format( Locale.US, "%s (%d)", state_name, count ) );
        }

        @Override
        public String getSectionName() {
            Cursor c = getCursor();
            return c.getString( c.getColumnIndex( DatabaseManager.States.STATE_NAME ) ).substring( 0, 1 );
        }
    }

    private final class BrowseTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            if ( getActivity() == null ) {
                cancel( false );
                return null;
            }

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            if ( db == null ) {
                return null;
            }

            // Show all the states grouped by first letter
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( DatabaseManager.Airports.TABLE_NAME+" a LEFT OUTER JOIN "
                    + DatabaseManager.States.TABLE_NAME+" s"+" ON a."+ DatabaseManager.Airports.ASSOC_STATE
                    +"=s."+ DatabaseManager.States.STATE_CODE );
            builder.setProjectionMap( sStateMap );
            Cursor c = builder.query( db,
                    // String[] projectionIn
                    new String[] { BaseColumns._ID,
                            DatabaseManager.Airports.ASSOC_STATE,
                            DatabaseManager.States.STATE_NAME,
                            BaseColumns._COUNT },
                    // String selection
                    null,
                    // String[] selectionArgs
                    null,
                    // String groupBy
                    DatabaseManager.States.STATE_NAME,
                    // String having
                    null,
                    // String sortOrder
                    DatabaseManager.States.STATE_NAME );

            return new Cursor[] { c };
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            if ( result != null ) {
                setCursor( result[ 0 ] );
            } else {
                Intent intent = new Intent( getActivity(), DownloadActivity.class );
                intent.putExtra( "MSG", "Please install the data before using the app" );
                startActivity( intent );
            }
            return false;
        }

    }

}
