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

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;

public class NearbyActivity extends Activity {

    private static final String TAG = SearchActivity.class.getSimpleName();
    private static final int MSEC_PER_SEC = 1000;

    private TextView mTextView;
    private ListView mListView;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation;
    private SharedPreferences mPrefs;
    private AirportsCursorAdapter mListAdapter;
    private ArrayList<String> mFavorites;

    private final String[] mQueryColumns = new String[] {
        BaseColumns._ID,
        Airports.SITE_NUMBER,
        Airports.ICAO_CODE,
        Airports.FAA_CODE,
        Airports.FACILITY_NAME,
        Airports.ASSOC_CITY,
        Airports.ASSOC_STATE,
        Airports.FACILITY_TYPE,
        Airports.FUEL_TYPES,
        Airports.UNICOM_FREQS,
        Airports.ELEVATION_MSL,
        Airports.STATUS_CODE,
        States.STATE_NAME,
        Airports.REF_LATTITUDE_DEGREES,
        Airports.REF_LONGITUDE_DEGREES
     };

    private final String[] mDisplayColumns = new String[] {
            BaseColumns._ID,
            Airports.SITE_NUMBER,
            Airports.ICAO_CODE,
            Airports.FAA_CODE,
            Airports.FACILITY_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Airports.FACILITY_TYPE,
            Airports.FUEL_TYPES,
            Airports.UNICOM_FREQS,
            Airports.ELEVATION_MSL,
            Airports.STATUS_CODE,
            States.STATE_NAME,
            Airports.DISTANCE,
            Airports.BEARING
         };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setTitle( "Nearby Airports" );
        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        setContentView( R.layout.airport_list_view );
        mTextView = (TextView) findViewById( R.id.message );
        mListView = (ListView) findViewById( R.id.list_view );

        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        mLocationListener = new LocationListener() {

            @Override
            public void onLocationChanged( Location location ) {
                Log.i( "LOCATION", String.valueOf( location.getLatitude() ) );
                if ( mLastLocation != null ) {
                    float distance = location.distanceTo( mLastLocation );
                    if ( distance < GeoUtils.METERS_PER_STATUTE_MILE ) {
                        // We have not moved enough to recalculate nearby airports
                        return;
                    }
                }

                // We have moved atleast a mile from the location of last calculation
                mLastLocation = location;
                NearbyTask task = new NearbyTask();
                task.execute( (Void[]) null );
            }

            @Override
            public void onProviderDisabled( String provider ) {
            }

            @Override
            public void onProviderEnabled( String provider ) {
            }

            @Override
            public void onStatusChanged( String provider, int status, Bundle extras ) {
            }
            
        };

        mFavorites = null;

        Cursor c = new MatrixCursor( mDisplayColumns );
        mListAdapter = new AirportsCursorAdapter( NearbyActivity.this, c );
        mListView.setAdapter( mListAdapter );

        registerForContextMenu( mListView );
    }

    
    @Override
    protected void onResume() {
        super.onResume();

        // Get the new favorites list
        mFavorites = DatabaseManager.instance().getFavorites();

        boolean useGps = mPrefs.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
        String provider = useGps? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;

        mLocationManager.requestLocationUpdates( provider, 60*MSEC_PER_SEC,
                GeoUtils.METERS_PER_STATUTE_MILE, mLocationListener );

        Location location = mLocationManager.getLastKnownLocation( provider );
        if ( System.currentTimeMillis()-location.getTime() <= 5*60*MSEC_PER_SEC ) {
            // Use the last known location if it is no more than 5 minutes old
            mLastLocation = location;
            NearbyTask task = new NearbyTask();
            task.execute( (Void[]) null );
        } else {
            setProgressBarIndeterminateVisibility( true );
            mTextView.setText( "Acquiring location..." );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates( mLocationListener );
    }

    // This data class allows us to sort the airport list based in distance
    class AirportData implements Comparable<AirportData> {

        public String SITE_NUMBER;
        public String ICAO_CODE;
        public String FAA_CODE;
        public String FACILITY_NAME;
        public String ASSOC_CITY;
        public String ASSOC_STATE;
        public String FACILITY_TYPE;
        public String FUEL_TYPES;
        public String UNICOM_FREQ;
        public String ELEVATION_MSL;
        public String STATUS_CODE;
        public String STATE_NAME;
        public float DISTANCE;
        public float BEARING;

        public void setFromCursor( Cursor c ) {
            SITE_NUMBER = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            FACILITY_NAME = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
            ICAO_CODE = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
            FAA_CODE = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
            ASSOC_CITY = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
            ASSOC_STATE = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
            FACILITY_TYPE = c.getString( c.getColumnIndex( Airports.FACILITY_TYPE ) );
            FUEL_TYPES = c.getString( c.getColumnIndex( Airports.FUEL_TYPES ) );
            ELEVATION_MSL = c.getString( c.getColumnIndex( Airports.ELEVATION_MSL ) );
            UNICOM_FREQ = c.getString( c.getColumnIndex( Airports.UNICOM_FREQS ) );
            STATUS_CODE = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
            STATE_NAME = c.getString( c.getColumnIndex( States.STATE_NAME ) );

            // Now calculate the distance to this airport
            float[] results = new float[ 2 ];
            Location.distanceBetween(
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude(), 
                    c.getDouble( c.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) ),
                    c.getDouble( c.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) ),
                    results );
            DISTANCE = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
            BEARING = ( results[ 1 ]+360 )%360;
        }

        @Override
        public int compareTo( AirportData another ) {
            if ( this.DISTANCE > another.DISTANCE ) {
                return 1;
            } else if ( this.DISTANCE < another.DISTANCE ) {
                return -1;
            }

            return 0;
        }

    }

    private final class NearbyTask extends AsyncTask<Void, Void, Cursor> {

        private int mRadius;

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor doInBackground( Void... params ) {
            mRadius = Integer.valueOf( mPrefs.getString( 
                    PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "20" ) );

            // Get the bounding box first to do a quick query as a first cut
            double[] box = GeoUtils.getBoundingBox( mLastLocation, mRadius );

            double radLatMin = box[ 0 ];
            double radLatMax = box[ 1 ];
            double radLonMin = box[ 2 ];
            double radLonMax = box[ 3 ];

            // Check if 180th Meridian lies within the bounding Box
            boolean isCrossingMeridian180 = ( radLonMin > radLonMax );

            String[] dbColumnNames = new String[ mQueryColumns.length-1 ];
            System.arraycopy( mQueryColumns, 0, dbColumnNames, 0, dbColumnNames.length );

            DatabaseManager dbManager = DatabaseManager.instance();
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME+" a INNER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            String selection = "("
                +Airports.REF_LATTITUDE_DEGREES+">=? AND "+Airports.REF_LONGITUDE_DEGREES+"<=?"
                +") AND ("+Airports.REF_LONGITUDE_DEGREES+">=? "
                +(isCrossingMeridian180? "OR " : "AND ")+Airports.REF_LONGITUDE_DEGREES+"<=?)";
            String[] selectionArgs = {
                    String.valueOf( Math.toDegrees( radLatMin ) ), 
                    String.valueOf( Math.toDegrees( radLatMax ) ),
                    String.valueOf( Math.toDegrees( radLonMin ) ),
                    String.valueOf( Math.toDegrees( radLonMax ) )
                    };
            Cursor c = builder.query( db, mQueryColumns, selection, selectionArgs, 
                    null, null, null );
            if ( !c.moveToFirst() ) {
                Log.i( TAG, "No airports found within the query radius" );
                Toast.makeText( NearbyActivity.this, "No airports found within the query radius", 
                        Toast.LENGTH_LONG ).show();
                c.close();
                return null;
            }

            AirportData[] airports = new AirportData[ c.getCount() ];
            do {
                AirportData airport = new AirportData();
                airport.setFromCursor( c );
                airports[ c.getPosition() ] = airport;
            } while ( c.moveToNext() );

            c.close();

            // Sort the airport list by distance from current location
            Arrays.sort( airports );

            // Build a cursor out of the sorted airport list
            MatrixCursor matrix = new MatrixCursor( mDisplayColumns );
            for ( AirportData airport : airports ) {
                if ( airport.DISTANCE <= mRadius ) {
                    MatrixCursor.RowBuilder row = matrix.newRow();
                    row.add( matrix.getPosition() )
                        .add( airport.SITE_NUMBER )
                        .add( airport.ICAO_CODE)
                        .add( airport.FAA_CODE )
                        .add( airport.FACILITY_NAME )
                        .add( airport.ASSOC_CITY )
                        .add( airport.ASSOC_STATE )
                        .add( airport.FACILITY_TYPE )
                        .add( airport.FUEL_TYPES )
                        .add( airport.UNICOM_FREQ )
                        .add( airport.ELEVATION_MSL )
                        .add( airport.STATUS_CODE )
                        .add( airport.STATE_NAME )
                        .add( airport.DISTANCE )
                        .add( airport.BEARING );
                }
            }

            return matrix;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            mListAdapter.changeCursor( c );
            mListAdapter.notifyDataSetChanged();
            mTextView.setText( String.valueOf( c.getCount() )+" found within "
                    +String.valueOf( mRadius )+" NM radius" );
            setProgressBarIndeterminateVisibility( false );
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        MenuItem browse = menu.findItem( R.id.menu_nearby );
        browse.setEnabled( false );
        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_search:
            onSearchRequested();
            return true;
        case R.id.menu_browse:
            try {
                Intent browse = new Intent( this, BrowseActivity.class );
                browse.putExtra( BrowseActivity.EXTRA_BUNDLE, new Bundle() );
                startActivity( browse );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        case R.id.menu_nearby:
            try {
                Intent nearby = new Intent( this, NearbyActivity.class );
                startActivity( nearby );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        case R.id.menu_favorites:
            try {
                Intent favorites = new Intent( this, FavoritesActivity.class );
                startActivity( favorites );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        case R.id.menu_download:
            try {
                Intent download = new Intent( this, DownloadActivity.class );
                startActivity( download );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        case R.id.menu_settings:
            try {
                Intent settings = new Intent( this, PreferencesActivity.class  );
                startActivity( settings );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = mListAdapter.getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String facilityName = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        c.moveToPosition( pos );

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.airport_list_context_menu, menu );
        menu.setHeaderTitle( facilityName );

        // Show either "Add" or "Remove" entry depending on the context
        if ( mFavorites.contains( siteNumber ) ) {
            menu.removeItem( R.id.menu_add_favorites );
        } else {
            menu.removeItem( R.id.menu_remove_favorites );
        }
    }

    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = mListAdapter.getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        c.moveToPosition( pos );

        switch ( item.getItemId() ) {
            case R.id.menu_add_favorites:
                DatabaseManager.instance().addToFavorites( siteNumber );
                mFavorites.add( siteNumber );
                break;
            case R.id.menu_remove_favorites:
                DatabaseManager.instance().removeFromFavorites( siteNumber );
                mFavorites.remove( siteNumber );
                break;
            case R.id.menu_view_details:
                break;
            default:
        }
        return super.onContextItemSelected( item );
    }

}
