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

package com.nadmm.airports.afd;

import java.util.Arrays;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.LocationColumns;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.AirportsCursorAdapter;
import com.nadmm.airports.utils.AirportsCursorHelper;
import com.nadmm.airports.utils.GeoUtils;

public class NearbyAirportsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.fragment_activity_layout ) );

        Bundle args = getIntent().getExtras();
        addFragment( NearbyAirportsFragment.class, args );
    }

    public static class NearbyAirportsFragment extends FragmentBase implements LocationListener {

        Location mLocation;
        private CursorAdapter mListAdapter;

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
                LocationColumns.DISTANCE,
                LocationColumns.BEARING
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
                        mLocation.getLatitude(),
                        mLocation.getLongitude(), 
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

        private final class NearbyAirportsTask extends AsyncTask<Location, Void, Cursor> {

            @Override
            protected Cursor doInBackground( Location... params ) {
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences( getActivity() );
                int radius = Integer.valueOf( prefs.getString(
                        PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );

                mLocation = params[ 0 ];

                // Get the bounding box first to do a quick query as a first cut
                double[] box = GeoUtils.getBoundingBox( mLocation, radius );

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

                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
                Cursor c = AirportsCursorHelper.query( db, selection, selectionArgs,
                        null, null, null, null );
                if ( !c.moveToFirst() ) {
                    c.close();
                    return null;
                }

                // Now find the magnetic declination at this location
                float declination = GeoUtils.getMagneticDeclination( mLocation );

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

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View view = inflater.inflate( R.layout.list_view_layout, container, false );
            return createContentView( view );
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            Bundle args = getArguments();
            Location location = (Location) args.getParcelable( LocationColumns.LOCATION );
            onLocationChanged( location );

            super.onActivityCreated( savedInstanceState );
        }

        @SuppressWarnings("deprecation")
        protected void showDetails( Cursor c ) {
            ListView listView = (ListView) findViewById( R.id.list_view );
            registerForContextMenu( listView );
            listView.setOnItemClickListener( new OnItemClickListener() {

                @Override
                public void onItemClick( AdapterView<?> parent, View view,
                        int position, long id ) {
                    Cursor c = mListAdapter.getCursor();
                    c.moveToPosition( position );
                    String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                    Intent intent = new Intent( getActivity(), AirportDetailsActivity.class );
                    intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                    startActivity( intent );
                }

            } );

            int position = listView.getFirstVisiblePosition();
            if ( mListAdapter == null ) {
                mListAdapter = new AirportsCursorAdapter( getActivity(), c );
            } else {
                mListAdapter.changeCursor( c );
            }
            listView.setAdapter( mListAdapter );
            listView.setSelection( position );
            getActivityBase().startManagingCursor( c );

            setFragmentContentShown( true );
        }

        @Override
        public void onLocationChanged( Location location ) {
            new NearbyAirportsTask().execute( location );
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
    }

}
