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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.Tower1;
import com.nadmm.airports.DatabaseManager.Tower3;
import com.nadmm.airports.DatabaseManager.Tower7;

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
            Cursor[] cursors = new Cursor[ 5 ];

            Cursor apt = dbManager.getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { Runways.SITE_NUMBER, Runways.RUNWAY_ID,
                    Runways.RUNWAY_LENGTH, Runways.RUNWAY_WIDTH, Runways.SURFACE_TYPE },
                    Airports.SITE_NUMBER+"=?", new String[] { siteNumber },
                    null, null, null, null );
            cursors[ 1 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            c = builder.query( db, new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND "+Remarks.REMARK_NAME+" in ('E147', 'A3', 'A24', 'A70', 'A82')",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 2 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower1.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower1.SITE_NUMBER+"=? ",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 3 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower7.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower7.SATELLITE_AIRPORT_SITE_NUMBER+"=? ",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 4 ] = c;

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
            // Airport Remarks section
            showRemarks( result );
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
        int row = 0;

        String ctaf = apt.getString( apt.getColumnIndex( Airports.CTAF_FREQ ) );
        if ( ctaf.length() > 0 ) {
            ++row;
            addRow( layout, "CTAF", ctaf );
        }

        String unicom = apt.getString( apt.getColumnIndex( Airports.UNICOM_FREQS ) );
        if ( unicom.length() > 0 ) {
            if ( row > 0 ) {
                addSeparator( layout );
            }
            ++row;
            addRow( layout, "Unicom", DataUtils.decodeUnicomFreq( unicom ) );
        }

        Cursor twr1 = result[ 3 ];
        if ( twr1.moveToFirst() ) {
            String facilityType = twr1.getString( twr1.getColumnIndex( Tower1.FACILITY_TYPE ) );
            if ( !facilityType.equals( "NON-ATCT" ) ) {
                if ( row > 0 ) {
                    addSeparator( layout );
                }
                Intent intent = new Intent( this, CommDetailsActivity.class );
                String siteNumber = apt.getString( apt.getColumnIndex(
                        Airports.SITE_NUMBER ) );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                addClickableRow( layout, "Other frequencies", intent );
            } else {
                String apchRadioCall =  twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_APCH ) );
                String depRadioCall =  twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_DEP ) );
                Cursor twr7 = result[ 4 ];
                if ( twr7.moveToFirst() ) {
                    String apchFreqs = "";
                    String depFreqs = "";
                    do {
                        String freq = twr7.getString( twr7.getColumnIndex(
                                Tower7.SATELLITE_AIRPORT_FREQ ) );
                        String freqUse = twr7.getString( twr7.getColumnIndex(
                                Tower7.SATELLITE_AIRPORT_FREQ_USE ) );
                        if ( freqUse.contains( "APCH/" ) ) {
                            if ( apchFreqs.length() > 0 ) {
                                apchFreqs += ", ";
                            }
                            apchFreqs += freq;
                        }
                        if ( freqUse.contains( "DEP/" ) ) {
                            if ( depFreqs.length() > 0 ) {
                                depFreqs += ", ";
                            }
                            depFreqs += freq;
                        }
                    } while ( twr7.moveToNext() );
                    if ( apchFreqs.length() > 0 ) {
                        if ( row > 0 ) {
                            addSeparator( layout );
                        }
                        ++row;
                        if ( !apchRadioCall.endsWith( "ARTCC" ) ) {
                            apchRadioCall += " Approach";
                        }
                        addRow( layout, apchRadioCall, apchFreqs );
                    }
                    if ( depFreqs.length() > 0 ) {
                        if ( row > 0 ) {
                            addSeparator( layout );
                        }
                        ++row;
                        if ( !depRadioCall.endsWith( "ARTCC" ) ) {
                            depRadioCall += " Departure";
                        }
                        addRow( layout, depRadioCall, depFreqs );
                    }
                }
            }
        }

        if ( row == 0 ) {
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
        if ( activation.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Activation date", activation );
        }
        String windIndicator = apt.getString( apt.getColumnIndex( Airports.WIND_INDICATOR ) );
        addSeparator( layout );
        addRow( layout, "Wind indicator", DataUtils.decodeWindIndicator( windIndicator ) );
        String circle = apt.getString( apt.getColumnIndex( Airports.SEGMENTED_CIRCLE ) );
        addSeparator( layout );
        addRow( layout, "Segmented circle", circle.equals( "Y" )? "Yes" : "No" );
        String beacon = apt.getString( apt.getColumnIndex( Airports.BEACON_COLOR ) );
        addSeparator( layout );
        addRow( layout, "Beacon", DataUtils.decodeBeacon( beacon ) );
        String lighting = apt.getString( apt.getColumnIndex( Airports.LIGHTING_SCHEDULE ) );
        if ( lighting.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Lighting schedule", lighting );
        }
        String landingFee = apt.getString( apt.getColumnIndex( Airports.LANDING_FEE ) );
        addSeparator( layout );
        addRow( layout, "Landing fee", landingFee.equals( "Y" )? "Yes" : "No" );
        int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
        String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
        String varYear = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_YEAR ) );
        addSeparator( layout );
        addRow( layout, "Magnetic variation", 
                String.format( "%d\u00B0 %s (%s)", variation, dir, varYear ) );
        addSeparator( layout );
        String sectional = apt.getString( apt.getColumnIndex( Airports.SECTIONAL_CHART ) );
        addRow( layout, "Sectional chart", sectional );
    }

    protected void showRemarks( Cursor[] result ) {
        Cursor rmk = result[ 2 ];
        if ( !rmk.moveToFirst() ) {
            return;
        }
        TextView label = (TextView) mMainLayout.findViewById( R.id.detail_remarks_label );
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.detail_remarks_layout );
        label.setVisibility( View.VISIBLE );
        layout.setVisibility( View.VISIBLE );
        do {
            String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
            addRemarkRow( layout, remark );
        } while ( rmk.moveToNext() );
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
        intent = new Intent( this, RemarkDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addSeparator( layout );
        addClickableRow( layout, "Additional remarks", intent );
        intent = new Intent( this, AttendanceDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addSeparator( layout );
        addClickableRow( layout, "Attendance", intent );
    }

    protected void addRow( TableLayout table, String label, String value ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tvLabel = new TextView( this );
        tvLabel.setText( label );
        tvLabel.setSingleLine();
        tvLabel.setGravity( Gravity.LEFT );
        tvLabel.setPadding( 4, 2, 2, 2 );
        row.addView( tvLabel, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        TextView tvValue = new TextView( this );
        tvValue.setText( value );
        tvValue.setMarqueeRepeatLimit( -1 );
        tvValue.setGravity( Gravity.RIGHT );
        tvLabel.setPadding( 2, 2, 4, 2 );
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
        tv.setPadding( 4, 2, 2, 2 );
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

    protected void addRemarkRow( LinearLayout layout, String remark ) {
        int index = remark.indexOf( ' ' );
        if ( index != -1 ) {
            while ( remark.charAt( index ) == ' ' ) {
                ++index;
            }
            remark = remark.substring( index );
        }
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( remark );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( TableLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.FILL_PARENT, 1 ) );
    }

}
