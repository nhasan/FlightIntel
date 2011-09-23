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
import java.util.HashMap;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Aff3;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.Tower1;
import com.nadmm.airports.DatabaseManager.Tower3;
import com.nadmm.airports.DatabaseManager.Tower6;
import com.nadmm.airports.DatabaseManager.Tower7;

public class AirportDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;
    private Bundle mExtras;

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

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 8 ];

            Cursor apt = mDbManager.getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { Runways.SITE_NUMBER, Runways.RUNWAY_ID,
                    Runways.RUNWAY_LENGTH, Runways.RUNWAY_WIDTH, Runways.SURFACE_TYPE },
                    Runways.SITE_NUMBER+"=? AND "+Runways.RUNWAY_LENGTH+" > 0",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 1 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            c = builder.query( db, new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=?"
                    +"AND "+Remarks.REMARK_NAME+" in ('E147', 'A3', 'A24', 'A70', 'A82')",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 2 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower1.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower1.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 3 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower7.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower7.SATELLITE_AIRPORT_SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 4 ] = c;

            if ( !c.moveToFirst() ) {
                String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
                builder = new SQLiteQueryBuilder();
                builder.setTables( Tower6.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Tower3.FACILITY_ID+"=?",
                        new String[] { faaCode }, null, null, Tower6.ELEMENT_NUMBER, null );
                cursors[ 5 ] = c;
            }

            builder = new SQLiteQueryBuilder();
            builder.setTables( Awos.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Awos.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 6 ] = c;

            String faa_code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Aff3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Aff3.IFR_FACILITY_ID+"=?",
                    new String[] { faa_code }, null, null, null, null );
            cursors[ 7 ] = c;

            // Extras bundle for "Nearby" activity
            mExtras = new Bundle();
            String code = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( code == null  || code.length() == 0 ) {
                code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            }
            mExtras.putString( NearbyActivity.APT_CODE, code );
            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            int elev_msl = apt.getInt( apt.getColumnIndex( Airports.ELEVATION_MSL ) );
            Location location = new Location( "" );
            location.setLatitude( lat );
            location.setLongitude( lon );
            location.setAltitude( elev_msl );
            mExtras.putParcelable( NearbyActivity.APT_LOCATION, location );

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );

            View view = mInflater.inflate( R.layout.airport_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.airport_detail_layout );

            // Title
            Cursor apt = result[ 0 ];
            showAirportTitle( mMainLayout, apt );

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

            TextView tv = (TextView) mMainLayout.findViewById( R.id.effective_date );
            tv.setText( "Effective date: "
                    +apt.getString( apt.getColumnIndex( Airports.EFFECTIVE_DATE ) ) );

            // Cleanup cursors
            for ( Cursor c : result ) {
                if ( c != null ) {
                    c.close();
                }
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

        Cursor awos = result[ 6 ];
        if ( awos.moveToFirst() ) {
            String freq = awos.getString( awos.getColumnIndex( Awos.STATION_FREQUENCY ) );
            if ( freq.length() == 0 ) {
                freq = "N/A";
            } else {
                freq = String.format( "%.3f", Double.valueOf( freq ) );
            }
            if ( row > 0 ) {
                addSeparator( layout );
            }
            String type = awos.getString( awos.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
            String phone = awos.getString( awos.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
            addAwosRow( layout, type, freq, phone );
            ++row;
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
                ++row;
                int resId = getSelectorResourceForRow( row-1, row+1 );
                addClickableRow( layout, "ATC frequencies", intent, resId );
            } else {
                String apchRadioCall =  twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_APCH ) );
                String depRadioCall =  twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_DEP ) );
                Cursor twr7 = result[ 4 ];
                if ( twr7.moveToFirst() ) {
                    HashMap<String, ArrayList<Pair<String, String>>> map =
                        new HashMap<String, ArrayList<Pair<String, String>>>();
                    do {
                        String freq = twr7.getString( twr7.getColumnIndex(
                                Tower7.SATELLITE_AIRPORT_FREQ ) );
                        String extra = "";
                        String freqUse = twr7.getString( twr7.getColumnIndex(
                                Tower7.SATELLITE_AIRPORT_FREQ_USE ) );
                        int i = 0;
                        while ( i < freq.length() ) {
                            char c = freq.charAt( i );
                            if ( ( c >= '0' && c <= '9' ) || c == '.' ) {
                                ++i;
                                continue;
                            }
                            extra = freq.substring( i );
                            freq = freq.substring( 0, i );
                            break;
                        }
                        if ( freqUse.contains( "APCH/" ) ) {
                            addFrequencyToMap( map, apchRadioCall+" Approach", freq, extra );
                        } else if ( freqUse.contains( "DEP/" ) ) {
                            addFrequencyToMap( map, depRadioCall+" Departure", freq, extra );
                        }
                        if ( freqUse.contains( "CD" ) || freqUse.contains( "CLNC DEL" ) ) {
                            addFrequencyToMap( map, "Clearance Delivery", freq, extra );
                        }
                    } while ( twr7.moveToNext() );

                    for ( String key : map.keySet() ) {
                        for ( Pair<String, String> pair : map.get( key ) ) {
                            if ( row > 0 ) {
                                addSeparator( layout );
                            }
                            addFrequencyRow( layout, key, pair );
                            ++row;
                        }
                    }
                }

                Cursor aff3 = result[ 7 ];
                if ( aff3.moveToFirst() ) {
                    do {
                        String artcc = aff3.getString( aff3.getColumnIndex( Aff3.ARTCC_ID ) );
                        Double freq = aff3.getDouble( aff3.getColumnIndex( Aff3.SITE_FREQUENCY ) );
                        String alt = aff3.getString( aff3.getColumnIndex( Aff3.FREQ_ALTITUDE ) );
                        if ( row > 0 ) {
                            addSeparator( layout );
                        }
                        addFrequencyRow( layout, DataUtils.decodeArtcc( artcc ),
                                Pair.create( String.format( "%.3f", freq ), "("+alt+")" ) );
                        ++row;
                    } while ( aff3.moveToNext() );
                }
            }
        }

        if ( row > 0 ) {
            addSeparator( layout );
        }
        Intent intent = new Intent( this, NavaidsActivity.class );
        String siteNumber = apt.getString( apt.getColumnIndex(
                Airports.SITE_NUMBER ) );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        ++row;
        int resId = getSelectorResourceForRow( row-1, row );
        addClickableRow( layout, "Navaids", intent, resId );
    }

    protected void addFrequencyToMap( HashMap<String, ArrayList<Pair<String, String>>> map,
            String freqUse, String freq, String extra ) {
        ArrayList<Pair<String, String>> list = map.get( freqUse );
        if ( list == null ) {
            list = new ArrayList<Pair<String, String>>();
        }
        list.add( Pair.create( String.format( "%.3f", Double.valueOf( freq ) ), extra.trim() ) );
        map.put( freqUse, list );
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
            int rwyTot = 0;
            int heliTot = 0;
            do {
                String rwyId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
                if ( rwyId.startsWith( "H" ) ) {
                    ++heliTot;
                } else {
                    ++rwyTot;
                }
            } while ( rwy.moveToNext() );

            rwy.moveToFirst();
            do {
                String rwyId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
                if ( rwyId.startsWith( "H" ) ) {
                    // This is a helipad
                    if ( heliNum > 0 ) {
                        addSeparator( heliLayout );
                    }
                    int resId = getSelectorResourceForRow( heliNum, heliTot );
                    addRunwayRow( heliLayout, rwy, resId );
                    ++heliNum;
                } else {
                    // This is a runway
                    if ( rwyNum > 0 ) {
                        addSeparator( rwyLayout );
                    }
                    int resId = getSelectorResourceForRow( rwyNum, rwyTot );
                    addRunwayRow( rwyLayout, rwy, resId );
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
        String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
        if ( dir.length() > 0 ) {
            int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
            String year = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_YEAR ) );
            addSeparator( layout );
            if ( year.length() > 0 ) {
                addRow( layout, "Magnetic variation", 
                        String.format( "%d\u00B0 %s (%s)", variation, dir, year ) );
            } else {
                addRow( layout, "Magnetic variation", 
                        String.format( "%d\u00B0 %s", variation, dir ) );
            }
        } else {
            Location location = (Location) mExtras.get( NearbyActivity.APT_LOCATION );
            int variation = Math.round( GeoUtils.getMagneticDeclination( location ) );
            dir = ( variation >= 0 )? "W" : "E";
            addSeparator( layout );
            addRow( layout, "Magnetic variation", 
                    String.format( "%d\u00B0 %s (actual)", Math.abs( variation ), dir ) );
        }
        addSeparator( layout );
        String sectional = apt.getString( apt.getColumnIndex( Airports.SECTIONAL_CHART ) );
        if ( sectional.length() > 0 ) {
            addRow( layout, "Sectional chart", sectional );
        }
    }

    protected void showRemarks( Cursor[] result ) {
        int row = 0;
        TextView label = (TextView) mMainLayout.findViewById( R.id.detail_remarks_label );
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.detail_remarks_layout );
        Cursor rmk = result[ 2 ];
        if ( rmk.moveToFirst() ) {
            do {
                String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addRemarkRow( layout, remark );
                ++row;
            } while ( rmk.moveToNext() );
        }

        Cursor twr1 = result[ 3 ];
        Cursor twr7 = result[ 4 ];
        if ( twr1.moveToFirst() ) {
            String facilityType = twr1.getString( twr1.getColumnIndex( Tower1.FACILITY_TYPE ) );
            if ( facilityType.equals( "NON-ATCT" ) && twr7.getCount() == 0 ) {
                // Show remarks, if any, since there are no frequencies listed
                Cursor twr6 = result[ 5 ];
                if ( twr6.moveToFirst() ) {
                    do {
                        String remark = twr6.getString( twr6.getColumnIndex( Tower6.REMARK_TEXT ) );
                        addBulletedRow( layout, remark );
                        ++row;
                    } while ( twr6.moveToNext() );
                }
            }
        }

        if ( row == 0 ) {
            label.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }
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
        addClickableRow( layout, "Other services", intent, R.drawable.row_selector_bottom );
    }

    protected void showOtherDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.detail_other_layout );
        Intent intent = new Intent( this, OwnershipDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addClickableRow( layout, "Ownership and contact", intent, R.drawable.row_selector_top );
        intent = new Intent( this, RemarkDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addSeparator( layout );
        addClickableRow( layout, "Additional remarks", intent, R.drawable.row_selector_middle );
        intent = new Intent( this, AttendanceDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addSeparator( layout );
        addClickableRow( layout, "Attendance", intent, R.drawable.row_selector_bottom );
    }

    protected void addRow( TableLayout table, String label, String value ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tv = new TextView( this );
        tv.setText( label );
        tv.setSingleLine();
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 2, 2, 2 );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setText( value );
        tv.setMarqueeRepeatLimit( -1 );
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 2, 4, 2 );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addFrequencyRow( TableLayout table, String freqUse,
            Pair<String, String> data ) {
        RelativeLayout layout = (RelativeLayout) mInflater.inflate(
                R.layout.comm_detail_item, null );
        TextView tv = (TextView) layout.findViewById( R.id.comm_freq_use );
        tv.setText( freqUse );
        tv.setPadding( 4, 2, 2, 2 );
        tv = (TextView) layout.findViewById( R.id.comm_freq_value );
        tv.setText( data.first );
        tv.setPadding( 2, 2, 4, 2 );
        TextView tvExtra = (TextView) layout.findViewById( R.id.comm_freq_extra );
        if ( data.second.length() > 0 ) {
            tvExtra.setText( data.second );
        } else {
            tvExtra.setVisibility( View.GONE );
        }
        tvExtra.setPadding( 2, 2, 4, 2 );
        table.addView( layout, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addAwosRow( TableLayout table, String type, String freq, String phone ) {
        RelativeLayout layout = (RelativeLayout) mInflater.inflate(
                R.layout.comm_detail_item, null );
        TextView tv = (TextView) layout.findViewById( R.id.comm_freq_use );
        tv.setText( type );
        tv = (TextView) layout.findViewById( R.id.comm_freq_value );
        tv.setText( freq );
        tv = (TextView) layout.findViewById( R.id.comm_freq_extra );
        if ( phone.length() > 0 ) {
            tv.setText( phone );
            makeClickToCall( tv );
        } else {
            tv.setVisibility( View.GONE );
        }
        table.addView( layout, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addClickableRow( TableLayout table, String label,
            final Intent intent, int resid ) {
        LinearLayout row = (LinearLayout) mInflater.inflate( R.layout.simple_detail_item, null );
        row.setBackgroundResource( resid );
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

    protected void addRunwayRow( TableLayout table, Cursor c, int resid ) {
        String siteNumber = c.getString( c.getColumnIndex( Runways.SITE_NUMBER ) );
        String runwayId = c.getString( c.getColumnIndex( Runways.RUNWAY_ID ) );
        int length = c.getInt( c.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        int width = c.getInt( c.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        String surfaceType = c.getString( c.getColumnIndex( Runways.SURFACE_TYPE ) );

        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        row.setBackgroundResource( resid );

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
        addBulletedRow( layout, remark );
    }

    protected void addPhoneRemarkRow( LinearLayout layout, String remark, final String phone ) {
        TextView tv = addBulletedRow( layout, remark+": "+phone );
        makeClickToCall( tv );
    }

    protected TextView addBulletedRow( LinearLayout layout, String text ) {
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
        tv.setText( text );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
        return tv;
    }

    protected void addSeparator( LinearLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.FILL_PARENT, 1 ) );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.airport_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_nearby:
            Intent intent = new Intent( this, NearbyActivity.class );
            intent.putExtras( mExtras );
            startActivity( intent );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
