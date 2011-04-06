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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;

public class NearbyActivity extends Activity {

    private static final String TAG = SearchActivity.class.getSimpleName();

    private TextView mHeader;
    private TextView mEmpty;
    private ListView mListView;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation;
    private SharedPreferences mPrefs;
    private ArrayList<String> mFavorites;

    private AirportsCursorAdapter mListAdapter = null;

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
        mHeader = (TextView) getLayoutInflater().inflate( R.layout.list_header, null );
        mEmpty = (TextView) findViewById( android.R.id.empty );
        mEmpty.setText( R.string.waiting_location );

        mListView = (ListView) findViewById( R.id.list_view );
        mListView.addHeaderView( mHeader );
        registerForContextMenu( mListView );
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                Cursor c = mListAdapter.getCursor();
                c.moveToPosition( position-1 );
                String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                Intent intent = new Intent( NearbyActivity.this, AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
            }
        } );

        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        mLocationListener = new AirportsLocationListener();
        mFavorites = null;

        boolean useGps = mPrefs.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
        String provider = useGps? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;

        Location location = mLocationManager.getLastKnownLocation( provider );
        if ( location != null && 
              System.currentTimeMillis()-location.getTime() <= 5*60*DateUtils.SECOND_IN_MILLIS ) {
            // Use the last known location if it is no more than 5 minutes old
            mLastLocation = location;
            NearbyTask task = new NearbyTask();
            task.execute( (Void[]) null );
        } else {
            setProgressBarIndeterminateVisibility( true );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean useGps = mPrefs.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
        String provider = useGps? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        mLocationManager.requestLocationUpdates( provider, 60*DateUtils.SECOND_IN_MILLIS,
                GeoUtils.METERS_PER_STATUTE_MILE, mLocationListener );
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates( mLocationListener );
    }

    private final class AirportsLocationListener implements LocationListener {

        @Override
        public void onLocationChanged( Location location ) {
            Log.i( "LOCATION", String.valueOf( location.toString() ) );
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

            // Favorites may have changed, get the new list
            mFavorites = DatabaseManager.instance().getFavorites();

            // Get the bounding box first to do a quick query as a first cut
            double[] box = GeoUtils.getBoundingBox( mLastLocation, mRadius );

            double radLatMin = box[ 0 ];
            double radLatMax = box[ 1 ];
            double radLonMin = box[ 2 ];
            double radLonMax = box[ 3 ];

            // Check if 180th Meridian lies within the bounding Box
            boolean isCrossingMeridian180 = ( radLonMin > radLonMax );
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
            Cursor c = AirportsCursorHelper.query( selection, selectionArgs,
                    null, null, null, null );
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
            mListAdapter = new AirportsCursorAdapter( NearbyActivity.this, c );
            mListView.setAdapter( mListAdapter );
            mHeader.setText( String.valueOf( c.getCount() )+" found within "
                    +String.valueOf( mRadius )+" NM radius" );
            setProgressBarIndeterminateVisibility( false );
            mEmpty.setVisibility( View.GONE );
            mListView.setVisibility( View.VISIBLE );
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
            Intent browse = new Intent( this, BrowseActivity.class );
            browse.putExtra( BrowseActivity.EXTRA_BUNDLE, new Bundle() );
            startActivity( browse );
            return true;
        case R.id.menu_favorites:
            Intent favorites = new Intent( this, FavoritesActivity.class );
            startActivity( favorites );
            return true;
        case R.id.menu_download:
            Intent download = new Intent( this, DownloadActivity.class );
            startActivity( download );
            return true;
        case R.id.menu_settings:
            Intent settings = new Intent( this, PreferencesActivity.class  );
            startActivity( settings );
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
        // Subtract 1 to account for header item
        c.moveToPosition( info.position-1 );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );            
        }
        String facilityName = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        c.moveToPosition( pos );

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.airport_list_context_menu, menu );
        menu.setHeaderTitle( code+" - "+facilityName );

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
