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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.States;

public class AirportDetailsActivity extends Activity {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        AirportDetailsTask task = new AirportDetailsTask();
        task.execute( siteNumber );
    }

    private final class AirportDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            DatabaseManager dbManager = DatabaseManager.instance( getApplicationContext() );
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 2 ];

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME+" a INNER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            Cursor c = builder.query( db, new String[] { "*" }, Airports.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            if ( !c.moveToFirst() ) {
                return null;
            }
            cursors[ 0 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            c = builder.query( db, new String[] { "*"  }, Airports.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 1 ] = c;

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );
            if ( result == null ) {
                // TODO: Show an error here
                return;
            }

            View view = mInflater.inflate( R.layout.airport_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.airport_detail_layout );

            Cursor apt = result[ 0 ];
            TextView tv;

            // Title
            GuiUtils.showAirportTitle( mMainLayout, apt );

            // Airport Communications section
            TableLayout commLayout = (TableLayout) mMainLayout.findViewById( 
                    R.id.detail_comm_layout );
            String ctaf = apt.getString( apt.getColumnIndex( Airports.CTAF_FREQ ) );
            String unicom = apt.getString( apt.getColumnIndex( Airports.UNICOM_FREQS ) );
            if ( ctaf.length() > 0 || unicom.length() > 0 ) {
                if ( ctaf.length() > 0 ) {
                    addRow( commLayout, "CTAF", ctaf );
                }
                if ( ctaf.length() > 0 && unicom.length() > 0 ) {
                    addSeparator( commLayout );
                }
                if ( unicom.length() > 0 ) {
                    addRow( commLayout, "Unicom", DataUtils.decodeUnicomFreq( unicom ) );
                }
            } else {
                commLayout.setVisibility( View.GONE );
            }

            // Runway section
            TableLayout rwyLayout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_rwy_layout );
            TableLayout heliLayout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_heli_layout );
            Cursor rwy = result[ 1 ];
            if ( rwy.moveToFirst() ) {
                int rwyNum = 0;
                int heliNum = 0;
                do {
                    String rwyId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
                    if ( rwyId.startsWith( "H" ) ) {
                        // This is a helipad
                        if ( heliNum > 0 ) {
                            addSeparator( heliLayout );
                        }
                        addRunwayRow( heliLayout, rwy );
                        ++heliNum;
                    } else {
                        // This is a runway
                        if ( rwyNum > 0 ) {
                            addSeparator( rwyLayout );
                        }
                        addRunwayRow( rwyLayout, rwy );
                        ++rwyNum;
                    }
                } while ( rwy.moveToNext() );

                if ( rwyNum == 0 ) {
                    // No runways so remove the section
                    tv = (TextView) mMainLayout.findViewById( R.id.detail_rwy_label );
                    tv.setVisibility( View.GONE );
                    rwyLayout.setVisibility( View.GONE );
                }
                if ( heliNum == 0 ) {
                    // No helipads so remove the section
                    tv = (TextView) mMainLayout.findViewById( R.id.detail_heli_label );
                    tv.setVisibility( View.GONE );
                    heliLayout.setVisibility( View.GONE );
                }
            } else {
                // No runway records so remove the sections
                tv = (TextView) mMainLayout.findViewById( R.id.detail_rwy_label );
                tv.setVisibility( View.GONE );
                rwyLayout.setVisibility( View.GONE );
                tv = (TextView) mMainLayout.findViewById( R.id.detail_heli_label );
                tv.setVisibility( View.GONE );
                heliLayout.setVisibility( View.GONE );
            }

            // Airport Operations section
            TableLayout opsLayout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_operations_layout );
            String use = apt.getString( apt.getColumnIndex( Airports.FACILITY_USE ) );
            addRow( opsLayout, "Airport use", DataUtils.decodeFacilityUse( use ) );
            addSeparator( opsLayout );
            String activation = apt.getString( apt.getColumnIndex( Airports.ACTIVATION_DATE ) );
            addRow( opsLayout, "Activation date", activation );
            addSeparator( opsLayout );
            String windIndicator = apt.getString( apt.getColumnIndex( Airports.WIND_INDICATOR ) );
            addRow( opsLayout, "Wind indicator", DataUtils.decodeWindIndicator( windIndicator ) );
            addSeparator( opsLayout );
            String circle = apt.getString( apt.getColumnIndex( Airports.SEGMENTED_CIRCLE ) );
            addRow( opsLayout, "Segmented circle", circle.equals( "Y" )? "Yes" : "No" );
            addSeparator( opsLayout );
            String beacon = apt.getString( apt.getColumnIndex( Airports.BEACON_COLOR ) );
            addRow( opsLayout, "Beacon", DataUtils.decodeBeacon( beacon ) );
            addSeparator( opsLayout );
            String landingFee = apt.getString( apt.getColumnIndex( Airports.LANDING_FEE ) );
            addRow( opsLayout, "Landing fee", landingFee.equals( "Y" )? "Yes" : "No" );
            addSeparator( opsLayout );
            String varDegrees = apt.getString( apt.getColumnIndex(
                    Airports.MAGNETIC_VARIATION_DEGREES ) );
            String varYear = apt.getString( apt.getColumnIndex(
                    Airports.MAGNETIC_VARIATION_YEAR ) );
            addRow( opsLayout, "Magnetic variation", varDegrees+" ("+varYear+")" );

            // Airport Services section
            TableLayout servicesLayout = (TableLayout) mMainLayout.findViewById(
                    R.id.detail_services_layout );
            String fuelTypes = DataUtils.decodeFuelTypes( 
                    apt.getString( apt.getColumnIndex( Airports.FUEL_TYPES ) ) );
            if ( fuelTypes.length() == 0 ) {
                fuelTypes = "No";
            }
            addRow( servicesLayout, "Fuel available", fuelTypes );
            addSeparator( servicesLayout );
            String repair;
            repair = apt.getString( apt.getColumnIndex( Airports.AIRFRAME_REPAIR_SERVICE ) );
            if ( repair.length() == 0 ) {
                repair = "No";
            }
            addRow( servicesLayout, "Airframe repair", repair );
            addSeparator( servicesLayout );
            repair = apt.getString( apt.getColumnIndex( Airports.POWER_PLANT_REPAIR_SERVICE ) );
            if ( repair.length() == 0 ) {
                repair = "No";
            }
            addRow( servicesLayout, "Powerplant repair", repair );
            String storage = DataUtils.decodeStorage( 
                    apt.getString( apt.getColumnIndex( Airports.STORAGE_FACILITY ) ) );
            if ( storage.length() > 0 ) {
                addSeparator( servicesLayout );
                addRow( servicesLayout, "Storage facility", storage );
            }
            String other = DataUtils.decodeServices(
                    apt.getString( apt.getColumnIndex( Airports.OTHER_SERVICES ) ) );
            if ( other.length() > 0 ) {
                addSeparator( servicesLayout );
                addRow( servicesLayout, "Other services", other );
            }

            // Cleanup cursors
            for ( Cursor c : result ) {
                c.close();
            }
        }

    }

    protected void addRow( TableLayout table, String label, String value ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tvLabel = new TextView( this );
        tvLabel.setText( label );
        tvLabel.setSingleLine();
        tvLabel.setGravity( Gravity.LEFT );
        tvLabel.setPadding( 4, 4, 2, 4 );
        row.addView( tvLabel, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        TextView tvValue = new TextView( this );
        tvValue.setText( value );
        tvValue.setMarqueeRepeatLimit( -1 );
        tvValue.setGravity( Gravity.RIGHT );
        tvLabel.setPadding( 4, 4, 2, 4 );
        row.addView( tvValue, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRunwayRow( TableLayout table, Cursor c ) {
        String siteNumber = c.getString( c.getColumnIndex( Runways.SITE_NUMBER ) );
        String runwayId = c.getString( c.getColumnIndex( Runways.RUNWAY_ID ) );
        String length = c.getString( c.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        String width = c.getString( c.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        String surfaceType = c.getString( c.getColumnIndex( Runways.SURFACE_TYPE ) );

        TableRow row = new TableRow( this );
        row.setPadding( 8, 8, 8, 8 );
        row.setLayoutParams( new TableLayout.LayoutParams( 
                TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT ) );

        TextView tv = new TextView( this );
        tv.setText( runwayId );
        tv.setGravity( Gravity.CENTER_VERTICAL );
        tv.setPadding( 4, 4, 4, 4 );
        row.addView( tv, new TableRow.LayoutParams( 
                TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.FILL_PARENT, 1f ) );

        LinearLayout layout = new LinearLayout( this );
        layout.setOrientation( LinearLayout.VERTICAL );
        tv = new TextView( this );
        tv.setText( length+"' x "+width+"'" );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 4, 4, 4, 0 );
        layout.addView( tv, new LinearLayout.LayoutParams( 
                TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT ) );
        tv = new TextView( this );
        tv.setText( DataUtils.decodeSurfaceType( surfaceType ) );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 4, 0, 4, 4 );
        layout.addView( tv, new LinearLayout.LayoutParams( 
                TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT ) );
        row.addView( layout, new TableRow.LayoutParams( 
                TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.FILL_PARENT, 0f ) );

        Bundle bundle = new Bundle();
        bundle.putString( Runways.SITE_NUMBER, siteNumber );
        bundle.putString( Runways.RUNWAY_ID, runwayId );
        row.setTag( bundle );
        row.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                Intent intent = new Intent( AirportDetailsActivity.this, 
                        RunwayDetailsActivity.class );
                intent.putExtras( (Bundle) v.getTag() );
                startActivity( intent );
            }
        } );

        table.addView( row );
    }

    protected void addSeparator( TableLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, 
                new TableLayout.LayoutParams( TableLayout.LayoutParams.FILL_PARENT, 1 ) );
    }

}
