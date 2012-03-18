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

package com.nadmm.airports;

import java.util.ArrayList;
import java.util.Arrays;

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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.utils.AirportsCursorAdapter;
import com.nadmm.airports.utils.AirportsCursorHelper;
import com.nadmm.airports.utils.GeoUtils;

public class NearbyActivity extends ActivityBase {

    public static final String APT_LOCATION = "APT_LOCATION";
    public static final String APT_CODE = "APT_CODE";

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation;
    private SharedPreferences mPrefs;
    private ArrayList<String> mFavorites;
    private AirportsCursorAdapter mListAdapter;

    private final String[] mDisplayColumns = new String[] {
            BaseColumns._ID,
            Airports.SITE_NUMBER,
            Airports.ICAO_CODE,
            Airports.FAA_CODE,
            Airports.FACILITY_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Airports.FUEL_TYPES,
            Airports.CTAF_FREQ,
            Airports.UNICOM_FREQS,
            Airports.ELEVATION_MSL,
            Airports.STATUS_CODE,
            Airports.FACILITY_USE,
            States.STATE_NAME,
            Airports.DISTANCE,
            Airports.BEARING
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.airport_list_view ) );

        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );

        // Check if an airport location was passed
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if ( bundle != null ) {
            mLastLocation = (Location) bundle.get( APT_LOCATION );
        }

        if ( mLastLocation == null ) {
            // No location was passed, initialize the location service to get a fix
            mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
            mLocationListener = new AirportsLocationListener();

            // Get the last known location to use a starting point
            mLastLocation = mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
            if ( mLastLocation == null ) {
                // Try to get last location from network provider
                mLastLocation = mLocationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER );
            }
        }

        if ( mLastLocation != null ) {
            // We have some location to use
            NearbyTask task = new NearbyTask();
            task.execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ( mLocationManager != null ) {
            mLocationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER,
                    30*DateUtils.SECOND_IN_MILLIS, 0.5f*GeoUtils.METERS_PER_STATUTE_MILE,
                    mLocationListener );
            boolean useGps = mPrefs.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
            if ( useGps ) {
                mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                        30*DateUtils.SECOND_IN_MILLIS, 0.5f*GeoUtils.METERS_PER_STATUTE_MILE,
                        mLocationListener );
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ( mLocationManager != null ) {
            mLocationManager.removeUpdates( mLocationListener );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( mListAdapter != null ) {
            Cursor c = mListAdapter.getCursor();
            c.close();
        }
    }

    private final class AirportsLocationListener implements LocationListener {

        @Override
        public void onLocationChanged( Location location ) {
            if ( GeoUtils.isBetterLocation( location, mLastLocation ) ) {
                mLastLocation = location;
                NearbyTask task = new NearbyTask();
                task.execute();
            }
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
    private final class AirportData implements Comparable<AirportData> {

        public String SITE_NUMBER;
        public String ICAO_CODE;
        public String FAA_CODE;
        public String FACILITY_NAME;
        public String ASSOC_CITY;
        public String ASSOC_STATE;
        public String FUEL_TYPES;
        public String UNICOM_FREQ;
        public String CTAF_FREQ;
        public String ELEVATION_MSL;
        public String STATUS_CODE;
        public String FACILITY_USE;
        public String STATE_NAME;
        public float DISTANCE;
        public float BEARING;

        public void setFromCursor( Cursor c, float declination ) {
            SITE_NUMBER = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            FACILITY_NAME = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
            ICAO_CODE = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
            FAA_CODE = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
            ASSOC_CITY = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
            ASSOC_STATE = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
            FUEL_TYPES = c.getString( c.getColumnIndex( Airports.FUEL_TYPES ) );
            ELEVATION_MSL = c.getString( c.getColumnIndex( Airports.ELEVATION_MSL ) );
            UNICOM_FREQ = c.getString( c.getColumnIndex( Airports.UNICOM_FREQS ) );
            CTAF_FREQ = c.getString( c.getColumnIndex( Airports.CTAF_FREQ ) );
            STATUS_CODE = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
            FACILITY_USE = c.getString( c.getColumnIndex( Airports.FACILITY_USE ) );
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
            BEARING = ( results[ 1 ]+declination+360 )%360;
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

        @Override
        protected Cursor doInBackground( Void... params ) {
            int radius = Integer.valueOf( mPrefs.getString( 
                    PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "20" ) );

            // Favorites may have changed, get the new list
            mFavorites = mDbManager.getAptFavorites();

            // Get the bounding box first to do a quick query as a first cut
            double[] box = GeoUtils.getBoundingBox( mLastLocation, radius );

            double radLatMin = box[ 0 ];
            double radLatMax = box[ 1 ];
            double radLonMin = box[ 2 ];
            double radLonMax = box[ 3 ];

            // Check if 180th Meridian lies within the bounding Box
            boolean isCrossingMeridian180 = ( radLonMin > radLonMax );
            String selection = "("
                +Airports.REF_LATTITUDE_DEGREES+">=? AND "+Airports.REF_LATTITUDE_DEGREES+"<=?"
                +") AND ("+Airports.REF_LONGITUDE_DEGREES+">=? "
                +(isCrossingMeridian180? "OR " : "AND ")+Airports.REF_LONGITUDE_DEGREES+"<=?)";
            String[] selectionArgs = {
                    String.valueOf( Math.toDegrees( radLatMin ) ), 
                    String.valueOf( Math.toDegrees( radLatMax ) ),
                    String.valueOf( Math.toDegrees( radLonMin ) ),
                    String.valueOf( Math.toDegrees( radLonMax ) )
                    };
            Cursor c = AirportsCursorHelper.query( NearbyActivity.this, selection, selectionArgs,
                    null, null, null, null );
            if ( !c.moveToFirst() ) {
                c.close();
                return null;
            }

            // Now find the magnetic declination at this location
            float declination = GeoUtils.getMagneticDeclination( mLastLocation );

            AirportData[] airports = new AirportData[ c.getCount() ];
            do {
                AirportData airport = new AirportData();
                airport.setFromCursor( c, declination );
                airports[ c.getPosition() ] = airport;
            } while ( c.moveToNext() );

            c.close();

            // Sort the airport list by distance from current location
            Arrays.sort( airports );

            // Build a cursor out of the sorted airport list
            MatrixCursor matrix = new MatrixCursor( mDisplayColumns );
            for ( AirportData airport : airports ) {
                if ( airport.DISTANCE > 0 && airport.DISTANCE <= radius ) {
                    MatrixCursor.RowBuilder row = matrix.newRow();
                    row.add( matrix.getPosition() )
                        .add( airport.SITE_NUMBER )
                        .add( airport.ICAO_CODE)
                        .add( airport.FAA_CODE )
                        .add( airport.FACILITY_NAME )
                        .add( airport.ASSOC_CITY )
                        .add( airport.ASSOC_STATE )
                        .add( airport.FUEL_TYPES )
                        .add( airport.CTAF_FREQ )
                        .add( airport.UNICOM_FREQ )
                        .add( airport.ELEVATION_MSL )
                        .add( airport.STATUS_CODE )
                        .add( airport.FACILITY_USE )
                        .add( airport.STATE_NAME )
                        .add( airport.DISTANCE )
                        .add( airport.BEARING );
                }
            }

            return matrix;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            showDetails( c );
        }

    }

    protected void showDetails( Cursor c ) {
        setContentShown( true );

        TextView title = (TextView) findViewById( R.id.airport_list_title );
        title.setVisibility( View.VISIBLE );

        int radius = Integer.valueOf( mPrefs.getString( 
                PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "20" ) );
        int count = ( c==null )? 0 : c.getCount();

        String msg = String.format( "%d airports found within %d NM radius", count, radius );
        title.setText( msg );

        if ( count == 0 ) {
            return;
        }

        ListView listView = (ListView) findViewById( R.id.list_view );
        registerForContextMenu( listView );
        listView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                Cursor c = mListAdapter.getCursor();
                c.moveToPosition( position );
                String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                Intent intent = new Intent( NearbyActivity.this, AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
            }

        } );

        if ( mListAdapter == null ) {
            // No adapter is set yet
            mListAdapter = new AirportsCursorAdapter( this, c );
            listView.setAdapter( mListAdapter );
        } else {
            int position = listView.getFirstVisiblePosition();
            mListAdapter.changeCursor( c );
            listView.setSelection( position );
        }
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = mListAdapter.getCursor();
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );            
        }
        String facilityName = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );

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
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );

        switch ( item.getItemId() ) {
            case R.id.menu_add_favorites:
                mDbManager.addToFavoriteAirports( siteNumber );
                mFavorites.add( siteNumber );
                break;
            case R.id.menu_remove_favorites:
                mDbManager.removeFromFavoriteAirports( siteNumber );
                mFavorites.remove( siteNumber );
                break;
            case R.id.menu_view_details:
                Intent intent = new Intent( this, AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
                break;
            default:
        }
        return super.onContextItemSelected( item );
    }

}
