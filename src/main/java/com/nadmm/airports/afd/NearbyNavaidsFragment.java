/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.LocationColumns;
import com.nadmm.airports.data.DatabaseManager.Nav1;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.GeoUtils;

import java.util.Arrays;
import java.util.Locale;

public final class NearbyNavaidsFragment extends FragmentBase {

    private final String[] mNavColumns = new String[] {
            Nav1.NAVAID_ID,
            Nav1.NAVAID_TYPE,
            Nav1.NAVAID_NAME,
            Nav1.NAVAID_FREQUENCY,
            Nav1.TACAN_CHANNEL,
            LocationColumns.RADIAL,
            LocationColumns.DISTANCE
    };
    int mRadius;

    private final class NavaidData implements Comparable<NavaidData> {
        private String NAVAID_ID;
        private String NAVAID_TYPE;
        private String NAVAID_NAME;
        private String NAVAID_FREQ;
        private String TACAN_CHANNEL;
        private int RADIAL;
        private float DISTANCE;

        private void setFromCursor( Cursor c, Location location ) {
            // Calculate the distance and bearing to this navaid from this airport
            NAVAID_ID = c.getString( c.getColumnIndex( Nav1.NAVAID_ID ) );
            NAVAID_TYPE= c.getString( c.getColumnIndex( Nav1.NAVAID_TYPE ) );
            NAVAID_NAME = c.getString( c.getColumnIndex( Nav1.NAVAID_NAME ) );
            NAVAID_FREQ = c.getString( c.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
            TACAN_CHANNEL = c.getString( c.getColumnIndex( Nav1.TACAN_CHANNEL ) );

            int var = c.getInt( c.getColumnIndex( Nav1.MAGNETIC_VARIATION_DEGREES ) );
            String dir = c.getString( c.getColumnIndex( Nav1.MAGNETIC_VARIATION_DIRECTION ) );
            if ( dir.equals( "E" ) ) {
                var *= -1;
            }

            float[] results = new float[ 2 ];
            Location.distanceBetween(
                    location.getLatitude(),
                    location.getLongitude(),
                    c.getDouble( c.getColumnIndex( Nav1.REF_LATTITUDE_DEGREES ) ),
                    c.getDouble( c.getColumnIndex( Nav1.REF_LONGITUDE_DEGREES ) ),
                    results );
            DISTANCE = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
            RADIAL = DataUtils.calculateRadial( results[ 1 ], var );
        }

        @Override
        public int compareTo( NavaidData another ) {
            if ( this.DISTANCE > another.DISTANCE ) {
                return 1;
            } else if ( this.DISTANCE < another.DISTANCE ) {
                return -1;
            }
            return 0;
        }
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.airport_navaids_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( "Nearby Navaids", "" );

        mRadius = getActivityBase().getPrefNearbyRadius();

        Bundle args = getArguments();
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        setBackgroundTask( new NavaidDetailsTask() ).execute( siteNumber );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        Cursor vor = result[ 1 ];
        Cursor ndb = result[ 2 ];
        if ( vor != null || ndb != null ) {
            showAirportTitle( apt );
            showNavaidDetails( result );
        } else {
            setContentMsg( String.format( Locale.US, "No navaids found within %d NM radius",
                    mRadius ) );
        }

        setFragmentContentShown( true );
    }

    protected void showNavaidDetails( Cursor[] result ) {
        Cursor vor = result[ 1 ];
        if ( vor != null && vor.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_navaids_vor_layout );
            do {
                String navaidId = vor.getString( vor.getColumnIndex( Nav1.NAVAID_ID ) );
                String freq = vor.getString( vor.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
                if ( freq.length() == 0 ) {
                    freq = vor.getString( vor.getColumnIndex( Nav1.TACAN_CHANNEL ) );
                }
                String name = vor.getString( vor.getColumnIndex( Nav1.NAVAID_NAME ) );
                String type = vor.getString( vor.getColumnIndex( Nav1.NAVAID_TYPE ) );
                int radial = vor.getInt( vor.getColumnIndex( "RADIAL" ) );
                float distance = vor.getFloat( vor.getColumnIndex( "DISTANCE" ) );
                addDirectionalNavaidRow( layout, navaidId, name, type, freq, radial,
                        distance );
            } while ( vor.moveToNext() );
        } else {
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_navaids_vor_layout );
            layout.setVisibility( View.GONE );
            TextView tv = (TextView) findViewById( R.id.detail_navaids_vor_label );
            tv.setVisibility( View.GONE );
        }

        Cursor ndb = result[ 2 ];
        if ( ndb != null && ndb.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_navaids_ndb_layout );
            do {
                String navaidId = ndb.getString( ndb.getColumnIndex( Nav1.NAVAID_ID ) );
                String freq = ndb.getString( ndb.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
                String name = ndb.getString( ndb.getColumnIndex( Nav1.NAVAID_NAME ) );
                String type = ndb.getString( ndb.getColumnIndex( Nav1.NAVAID_TYPE ) );
                int heading = ndb.getInt( ndb.getColumnIndex( "RADIAL" ) );
                float distance = ndb.getFloat( ndb.getColumnIndex( "DISTANCE" ) );
                addNonDirectionalNavaidRow( layout, navaidId, name, type, freq, heading,
                        distance );
            } while ( ndb.moveToNext() );
        } else {
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_navaids_ndb_layout );
            layout.setVisibility( View.GONE );
            TextView tv = (TextView) findViewById( R.id.detail_navaids_ndb_label );
            tv.setVisibility( View.GONE );
        }
    }

    protected void addDirectionalNavaidRow( LinearLayout table, String navaidId,
            String name, String type, String freq, int radial, float distance ) {
        String label1 = navaidId+"      "+DataUtils.getMorseCode( navaidId );
        String label2 = name+" "+type;
        String value2 = String.format( Locale.US, "r%03d/%.1fNM", radial, distance );

        Bundle args = new Bundle();
        args.putString( Nav1.NAVAID_ID, navaidId );
        args.putString( Nav1.NAVAID_TYPE, type );
        addClickableRow( table, label1, freq, label2, value2, NavaidDetailsFragment.class, args );
    }

    protected void addNonDirectionalNavaidRow( LinearLayout table, String navaidId,
            String name, String type, String freq, int heading, float distance ) {
        String label1 = navaidId+"      "+DataUtils.getMorseCode( navaidId );
        String label2 = name+" "+type;
        String value2 = String.format( Locale.US, "%03d\u00B0M/%.1fNM", heading, distance );

        Bundle args = new Bundle();
        args.putString( Nav1.NAVAID_ID, navaidId );
        args.putString( Nav1.NAVAID_TYPE, type );
        addClickableRow( table, label1, freq, label2, value2, NavaidDetailsFragment.class, args );
    }

    private final class NavaidDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 3 ];

            Cursor apt = getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            Location location = new Location( "" );
            location.setLatitude( lat );
            location.setLongitude( lon );

            // Get the bounding box first to do a quick query as a first cut
            double[] box = GeoUtils.getBoundingBoxRadians( location, mRadius );

            double radLatMin = box[ 0 ];
            double radLatMax = box[ 1 ];
            double radLonMin = box[ 2 ];
            double radLonMax = box[ 3 ];

            // Check if 180th Meridian lies within the bounding Box
            boolean isCrossingMeridian180 = ( radLonMin > radLonMax );

            String selection = "("
                +Nav1.REF_LATTITUDE_DEGREES+">=? AND "+Nav1.REF_LATTITUDE_DEGREES+"<=?"
                +") AND ("+Nav1.REF_LONGITUDE_DEGREES+">=? "
                +(isCrossingMeridian180? "OR " : "AND ")+Nav1.REF_LONGITUDE_DEGREES+"<=?)";
            String[] selectionArgs = {
                    String.valueOf( Math.toDegrees( radLatMin ) ),
                    String.valueOf( Math.toDegrees( radLatMax ) ),
                    String.valueOf( Math.toDegrees( radLonMin ) ),
                    String.valueOf( Math.toDegrees( radLonMax ) )
                    };
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Nav1.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "*" }, selection, selectionArgs,
                    null, null, null, null );
            if ( c.moveToFirst() ) {
                NavaidData[] navaids = new NavaidData[ c.getCount() ];
                do {
                    NavaidData navaid = new NavaidData();
                    navaid.setFromCursor( c, location );
                    navaids[ c.getPosition() ] = navaid;
                } while ( c.moveToNext() );

                // Sort the navaids list by distance from current location
                Arrays.sort( navaids );

                @SuppressWarnings("resource")
                MatrixCursor vor = new MatrixCursor( mNavColumns );
                @SuppressWarnings("resource")
                MatrixCursor ndb = new MatrixCursor( mNavColumns );
                for ( NavaidData navaid : navaids ) {
                    if ( navaid.DISTANCE <= mRadius ) {
                        if ( DataUtils.isDirectionalNavaid( navaid.NAVAID_TYPE ) ) {
                            MatrixCursor.RowBuilder row = vor.newRow();
                            row.add( navaid.NAVAID_ID )
                                    .add( navaid.NAVAID_TYPE )
                                    .add( navaid.NAVAID_NAME )
                                    .add( navaid.NAVAID_FREQ )
                                    .add( navaid.TACAN_CHANNEL )
                                    .add( navaid.RADIAL )
                                    .add( navaid.DISTANCE );
                        } else {
                            MatrixCursor.RowBuilder row = ndb.newRow();
                            row.add( navaid.NAVAID_ID )
                                    .add( navaid.NAVAID_TYPE )
                                    .add( navaid.NAVAID_NAME )
                                    .add( navaid.NAVAID_FREQ )
                                    .add( navaid.TACAN_CHANNEL )
                                    .add( navaid.RADIAL )
                                    .add( navaid.DISTANCE );
                        }
                    }
                }
                cursors[ 1 ] = vor;
                cursors[ 2 ] = ndb;
            }

            c.close();
            return cursors;
       }

       @Override
       protected boolean onResult( Cursor[] result ) {
           showDetails( result );
           return true;
       }

    }

}
