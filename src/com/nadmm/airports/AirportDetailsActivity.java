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
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Runways;

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

            cursors[ 0 ] = dbManager.getAirportDetails( siteNumber );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { Runways.SITE_NUMBER, Runways.RUNWAY_ID,
                    Runways.RUNWAY_LENGTH, Runways.RUNWAY_WIDTH, Runways.SURFACE_TYPE },
                    Airports.SITE_NUMBER+"=?", new String[] { siteNumber },
                    null, null, null, null );
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

            // Title
            Cursor apt = result[ 0 ];
            GuiUtils.showAirportTitle( mMainLayout, apt );

            // Airport Communications section
            showCommunicationsDetails( result );
            // Runway section
            showRunwayDetails( result );
            // Airport Operations section
            showOperationsDetails( result );
            // Airport Services section
            showServicesDetails( result );
            // Other details
            showOtherDetails( result );

            // Cleanup cursors
            for ( Cursor c : result ) {
                c.close();
            }
        }

    }

    protected void showCommunicationsDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.detail_comm_layout );
        String ctaf = apt.getString( apt.getColumnIndex( Airports.CTAF_FREQ ) );
        String unicom = apt.getString( apt.getColumnIndex( Airports.UNICOM_FREQS ) );
        if ( ctaf.length() > 0 || unicom.length() > 0 ) {
            if ( ctaf.length() > 0 ) {
                addRow( layout, "CTAF", ctaf );
            }
            if ( ctaf.length() > 0 && unicom.length() > 0 ) {
                addSeparator( layout );
            }
            if ( unicom.length() > 0 ) {
                addRow( layout, "Unicom", DataUtils.decodeUnicomFreq( unicom ) );
            }
        } else {
            layout.setVisibility( View.GONE );
        }
    }

    protected void showRunwayDetails( Cursor[] result ) {
        TableLayout rwyLayout = (TableLayout) mMainLayout.findViewById(
                R.id.detail_rwy_layout );
        TableLayout heliLayout = (TableLayout) mMainLayout.findViewById(
                R.id.detail_heli_layout );
        TextView tv;
        int rwyNum = 0;
        int heliNum = 0;

        Cursor rwy = result[ 1 ];
        if ( rwy.moveToFirst() ) {
            do {
                String rwyId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
                int length = rwy.getInt( rwy.getColumnIndex( Runways.RUNWAY_LENGTH ) );

                if ( length == 0 ) {
                    continue;
                }

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
        }

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
    }

    protected void showOperationsDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById(
                R.id.detail_operations_layout );
        String use = apt.getString( apt.getColumnIndex( Airports.FACILITY_USE ) );
        addRow( layout, "Airport use", DataUtils.decodeFacilityUse( use ) );
        String activation = apt.getString( apt.getColumnIndex( Airports.ACTIVATION_DATE ) );
        addSeparator( layout );
        addRow( layout, "Activation date", activation );
        String windIndicator = apt.getString( apt.getColumnIndex( Airports.WIND_INDICATOR ) );
        addSeparator( layout );
        addRow( layout, "Wind indicator", DataUtils.decodeWindIndicator( windIndicator ) );
        String circle = apt.getString( apt.getColumnIndex( Airports.SEGMENTED_CIRCLE ) );
        addSeparator( layout );
        addRow( layout, "Segmented circle", circle.equals( "Y" )? "Yes" : "No" );
        String beacon = apt.getString( apt.getColumnIndex( Airports.BEACON_COLOR ) );
        addSeparator( layout );
        addRow( layout, "Beacon", DataUtils.decodeBeacon( beacon ) );
        String landingFee = apt.getString( apt.getColumnIndex( Airports.LANDING_FEE ) );
        addSeparator( layout );
        addRow( layout, "Landing fee", landingFee.equals( "Y" )? "Yes" : "No" );
        int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
        String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
        String varYear = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_YEAR ) );
        addSeparator( layout );
        addRow( layout, "Magnetic variation", 
                String.format( "%d\u00B0 %s (%s)", variation, dir, varYear ) );
    }

    protected void showServicesDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.detail_services_layout );
        String fuelTypes = DataUtils.decodeFuelTypes( 
                apt.getString( apt.getColumnIndex( Airports.FUEL_TYPES ) ) );
        if ( fuelTypes.length() == 0 ) {
            fuelTypes = "No";
        }
        addRow( layout, "Fuel available", fuelTypes );
        String repair;
        repair = apt.getString( apt.getColumnIndex( Airports.AIRFRAME_REPAIR_SERVICE ) );
        if ( repair.length() == 0 ) {
            repair = "No";
        }
        addSeparator( layout );
        addRow( layout, "Airframe repair", repair );
        repair = apt.getString( apt.getColumnIndex( Airports.POWER_PLANT_REPAIR_SERVICE ) );
        if ( repair.length() == 0 ) {
            repair = "No";
        }
        addSeparator( layout );
        addRow( layout, "Powerplant repair", repair );
        addSeparator( layout );
        Intent intent = new Intent( this, ServicesDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER,
                apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) ) );
        addClickableRow( layout, "Other services", intent );
    }

    protected void showOtherDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.detail_other_layout );
        Intent intent = new Intent( this, OwnershipDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addClickableRow( layout, "Ownership and contact", intent );
        intent = new Intent( this, AirportRemarksActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addSeparator( layout );
        addClickableRow( layout, "Remarks", intent );
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

    protected void addClickableRow( TableLayout table, String label, final Intent intent ) {
        LinearLayout row = (LinearLayout) mInflater.inflate( R.layout.simple_detail_item, null );
        TextView tv = new TextView( this );
        tv.setText( label );
        tv.setGravity( Gravity.CENTER_VERTICAL );
        tv.setPadding( 4, 4, 2, 4 );
        row.addView( tv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        ImageView iv = new ImageView( this );
        iv.setImageResource( R.drawable.arrow );
        iv.setPadding( 0, 0, 4, 0 );
        iv.setScaleType( ScaleType.CENTER );
        row.addView( iv, new LinearLayout.LayoutParams( 
                LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 0f ) );
        row.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                startActivity( intent );
            }

        } );

        table.addView( row, new TableLayout.LayoutParams( 
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRunwayRow( TableLayout table, Cursor c ) {
        String siteNumber = c.getString( c.getColumnIndex( Runways.SITE_NUMBER ) );
        String runwayId = c.getString( c.getColumnIndex( Runways.RUNWAY_ID ) );
        int length = c.getInt( c.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        int width = c.getInt( c.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        String surfaceType = c.getString( c.getColumnIndex( Runways.SURFACE_TYPE ) );

        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tv = new TextView( this );
        tv.setText( runwayId );
        tv.setGravity( Gravity.CENTER_VERTICAL );
        tv.setPadding( 4, 0, 4, 0 );
        row.addView( tv, new TableRow.LayoutParams( 
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.FILL_PARENT, 1f ) );
        LinearLayout layout = new LinearLayout( this );
        layout.setOrientation( LinearLayout.VERTICAL );
        tv = new TextView( this );
        tv.setText( String.valueOf( length )+"' x "+String.valueOf( width )+"'" );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 4, 0, 4, 0 );
        layout.addView( tv, new LinearLayout.LayoutParams( 
                TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT ) );
        tv = new TextView( this );
        tv.setText( DataUtils.decodeSurfaceType( surfaceType ) );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 4, 0, 4, 0 );
        layout.addView( tv, new LinearLayout.LayoutParams( 
                TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT ) );
        row.addView( layout, new TableRow.LayoutParams( 
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.FILL_PARENT, 0f ) );
        ImageView iv = new ImageView( this );
        iv.setImageResource( R.drawable.arrow );
        iv.setPadding( 6, 0, 4, 0 );
        iv.setScaleType( ScaleType.CENTER );
        row.addView( iv, new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.FILL_PARENT, 0f ) );

        final Bundle bundle = new Bundle();
        bundle.putString( Runways.SITE_NUMBER, siteNumber );
        bundle.putString( Runways.RUNWAY_ID, runwayId );
        row.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                Intent intent = new Intent( AirportDetailsActivity.this, 
                        RunwayDetailsActivity.class );
                intent.putExtras( bundle );
                startActivity( intent );
            }

        } );

        table.addView( row, new TableLayout.LayoutParams( 
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( TableLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.FILL_PARENT, 1 ) );
    }

}
