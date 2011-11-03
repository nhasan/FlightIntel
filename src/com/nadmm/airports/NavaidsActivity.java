/*
 * FlightIntel for Pilots
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

import java.util.Arrays;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Nav1;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.GeoUtils;

public class NavaidsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    private final String[] mNavColumns = new String[] {
            Nav1.NAVAID_ID,
            Nav1.NAVAID_TYPE,
            Nav1.NAVAID_NAME,
            Nav1.NAVAID_FREQUENCY,
            "RADIAL",
            "DISTANCE"
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        NavaidDetailsTask task = new NavaidDetailsTask();
        task.execute( siteNumber );
    }

    private final class NavaidData implements Comparable<NavaidData> {
        public String NAVAID_ID;
        public String NAVAID_TYPE;
        public String NAVAID_NAME;
        public String NAVAID_FREQ;
        public int RADIAL;
        public float DISTANCE;
        public int RANGE;

        public void setFromCursor( Cursor c, Location location ) {
            // Calculate the distance and bearing to this navaid from this airport
            NAVAID_ID = c.getString( c.getColumnIndex( Nav1.NAVAID_ID ) );
            NAVAID_TYPE= c.getString( c.getColumnIndex( Nav1.NAVAID_TYPE ) );
            NAVAID_NAME = c.getString( c.getColumnIndex( Nav1.NAVAID_NAME ) );
            NAVAID_FREQ = c.getString( c.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );

            int var = c.getInt( c.getColumnIndex( Nav1.MAGNETIC_VARIATION_DEGREES ) );
            String dir = c.getString( c.getColumnIndex(
                    Nav1.MAGNETIC_VARIATION_DIRECTION ) );
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
            RADIAL = (int) Math.round( results[ 1 ]+360 )%360;
            RADIAL = DataUtils.calculateMagneticHeading( RADIAL, var );
            RADIAL = DataUtils.calculateReciprocalHeading( RADIAL );
            String alt = c.getString( c.getColumnIndex( Nav1.PROTECTED_FREQUENCY_ALTITUDE ) );
            RANGE = ( alt != null && alt.equals( "T" ) )? 25 : 40;
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

    private final class NavaidDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 3 ];

            Cursor apt = mDbManager.getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            Location location = new Location( "" );
            location.setLatitude( lat );
            location.setLongitude( lon );

            // Get the navaid within 40nm radius
            // Get the bounding box first to do a quick query as a first cut
            double[] box = GeoUtils.getBoundingBox( location, 40 );

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

                MatrixCursor vor = new MatrixCursor( mNavColumns );
                MatrixCursor ndb = new MatrixCursor( mNavColumns );
                for ( NavaidData navaid : navaids ) {
                    if ( navaid.DISTANCE <= navaid.RANGE ) {
                        if ( DataUtils.isDirectionalNavaid( navaid.NAVAID_TYPE ) ) {
                            MatrixCursor.RowBuilder row = vor.newRow();
                            row.add( navaid.NAVAID_ID )
                                    .add( navaid.NAVAID_TYPE )
                                    .add( navaid.NAVAID_NAME )
                                    .add( navaid.NAVAID_FREQ )
                                    .add( navaid.RADIAL )
                                    .add( navaid.DISTANCE );
                        } else {
                            MatrixCursor.RowBuilder row = ndb.newRow();
                            row.add( navaid.NAVAID_ID )
                                    .add( navaid.NAVAID_TYPE )
                                    .add( navaid.NAVAID_NAME )
                                    .add( navaid.NAVAID_FREQ )
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
       protected void onPostExecute( Cursor[] result ) {
           setProgressBarIndeterminateVisibility( false );

           Cursor vor = result[ 1 ];
           Cursor ndb = result[ 2 ];
           if ( vor == null && ndb == null ) {
               Toast.makeText( NavaidsActivity.this, "No navaids found in the vicinity",
                       Toast.LENGTH_LONG ).show();
               NavaidsActivity.this.finish();
           }

           View view = mInflater.inflate( R.layout.airport_navaids_view, null );
           setContentView( view );
           mMainLayout = (LinearLayout) view.findViewById( R.id.navaids_detail_layout );

           // Title
           Cursor apt = result[ 0 ];
           showAirportTitle( mMainLayout, apt );

           // Navaids
           showNavaidDetails( result );

           // Cleanup cursors
           for ( Cursor c : result ) {
               if ( c != null ) {
                   c.close();
               }
           }
       }

    }

    protected void showNavaidDetails( Cursor[] result ) {
        Cursor vor = result[ 1 ];
        if ( vor != null && vor.moveToFirst() ) {
            TableLayout layout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_navaids_vor_layout );
            do {
                if ( vor.getPosition() > 0 ) {
                    addSeparator( layout );
                }
                String navaidId = vor.getString( vor.getColumnIndex( Nav1.NAVAID_ID ) );
                String freq = vor.getString( vor.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
                String name = vor.getString( vor.getColumnIndex( Nav1.NAVAID_NAME ) );
                String type = vor.getString( vor.getColumnIndex( Nav1.NAVAID_TYPE ) );
                int radial = vor.getInt( vor.getColumnIndex( "RADIAL" ) );
                float distance = vor.getFloat( vor.getColumnIndex( "DISTANCE" ) );
                int resid = getSelectorResourceForRow( vor.getPosition(), vor.getCount() );
                addDirectionalNavaidRow( layout, navaidId, name, type, freq, radial,
                        distance, resid );
            } while ( vor.moveToNext() );
        } else {
            TableLayout layout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_navaids_vor_layout );
            layout.setVisibility( View.GONE );
            TextView tv = (TextView) mMainLayout.findViewById( R.id.detail_navaids_vor_label );
            tv.setVisibility( View.GONE );
        }

        Cursor ndb = result[ 2 ];
        if ( ndb != null && ndb.moveToFirst() ) {
            TableLayout layout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_navaids_ndb_layout );
            do {
                if ( ndb.getPosition() > 0 ) {
                    addSeparator( layout );
                }
                String navaidId = ndb.getString( vor.getColumnIndex( Nav1.NAVAID_ID ) );
                String freq = ndb.getString( ndb.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
                String name = ndb.getString( ndb.getColumnIndex( Nav1.NAVAID_NAME ) );
                String type = ndb.getString( ndb.getColumnIndex( Nav1.NAVAID_TYPE ) );
                int heading = ndb.getInt( ndb.getColumnIndex( "RADIAL" ) );
                float distance = ndb.getFloat( ndb.getColumnIndex( "DISTANCE" ) );
                int resid = getSelectorResourceForRow( ndb.getPosition(), ndb.getCount() );
                addNonDirectionalNavaidRow( layout, navaidId, name, type, freq, heading,
                        distance, resid );
            } while ( ndb.moveToNext() );
        } else {
            TableLayout layout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_navaids_ndb_layout );
            layout.setVisibility( View.GONE );
            TextView tv = (TextView) mMainLayout.findViewById( R.id.detail_navaids_ndb_label );
            tv.setVisibility( View.GONE );
        }
    }

    protected void addDirectionalNavaidRow( TableLayout table, final String navaidId,
            String name, final String type, String freq, int radial, float 
            distance, int resid ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        row.setBackgroundResource( resid );

        LinearLayout layout2 = new LinearLayout( this );
        layout2.setOrientation( LinearLayout.VERTICAL );
        LinearLayout layout3 = new LinearLayout( this );
        layout3.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setText( navaidId+"      "+DataUtils.getMorseCode( navaidId ));
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 0, 2, 0 );
        layout3.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setText( freq );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 0, 4, 0 );
        layout3.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1f ) );
        layout2.addView( layout3, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
        LinearLayout layout4 = new LinearLayout( this );
        layout4.setOrientation( LinearLayout.HORIZONTAL );
        tv = new TextView( this );
        tv.setText( name+" "+type );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 0, 2, 0 );
        layout4.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setText( String.format( "r%03d/%.1fNM", radial, distance ) );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 0, 4, 0 );
        layout4.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout2.addView( layout4, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
        row.addView( layout2, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        ImageView iv = new ImageView( this );
        iv.setImageResource( R.drawable.arrow );
        iv.setPadding( 6, 0, 4, 0 );
        iv.setScaleType( ScaleType.CENTER );
        row.addView( iv, new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.FILL_PARENT, 0f ) );

        row.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                Intent intent = new Intent( NavaidsActivity.this, NavaidDetailsActivity.class );
                intent.putExtra( Nav1.NAVAID_ID, navaidId );
                intent.putExtra( Nav1.NAVAID_TYPE, type );
                startActivity( intent );
            }

        } );

        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addNonDirectionalNavaidRow( TableLayout table, final String navaidId,
            String name, final String type, String freq, int heading,
            float distance, int resid ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        row.setBackgroundResource( resid );

        LinearLayout layout2 = new LinearLayout( this );
        layout2.setOrientation( LinearLayout.VERTICAL );
        LinearLayout layout3 = new LinearLayout( this );
        layout3.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setText( navaidId+"      "+DataUtils.getMorseCode( navaidId ) );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 0, 2, 0 );
        layout3.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setText( freq );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 0, 4, 0 );
        layout3.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1f ) );
        layout2.addView( layout3, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
        LinearLayout layout4 = new LinearLayout( this );
        layout4.setOrientation( LinearLayout.HORIZONTAL );
        tv = new TextView( this );
        tv.setText( name+" "+type );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 0, 2, 0 );
        layout4.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setText( String.format( "%03d\u00B0M/%.1fNM", heading, distance ) );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 0, 4, 0 );
        layout4.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout2.addView( layout4, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
        row.addView( layout2, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        ImageView iv = new ImageView( this );
        iv.setImageResource( R.drawable.arrow );
        iv.setPadding( 6, 0, 4, 0 );
        iv.setScaleType( ScaleType.CENTER );
        row.addView( iv, new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.FILL_PARENT, 0f ) );

        row.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                Intent intent = new Intent( NavaidsActivity.this, NavaidDetailsActivity.class );
                intent.putExtra( Nav1.NAVAID_ID, navaidId );
                intent.putExtra( Nav1.NAVAID_TYPE, type );
                startActivity( intent );
            }

        } );

        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( LinearLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.FILL_PARENT, 1 ) );
    }

}
