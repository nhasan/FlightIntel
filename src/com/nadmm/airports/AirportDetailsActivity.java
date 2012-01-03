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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Aff3;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.Tower1;
import com.nadmm.airports.DatabaseManager.Tower3;
import com.nadmm.airports.DatabaseManager.Tower6;
import com.nadmm.airports.DatabaseManager.Tower7;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.GuiUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.WxUtils;
import com.nadmm.airports.wx.Metar;
import com.nadmm.airports.wx.MetarService;

public class AirportDetailsActivity extends ActivityBase {

    private Bundle mExtras;
    private Location mLocation;
    private BroadcastReceiver mReceiver;
    private HashMap<String, TextView> mAwosMap;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mAwosMap =new HashMap<String, TextView>();
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                Metar metar = (Metar) intent.getSerializableExtra( MetarService.RESULT );
                showWxInfo( metar );
            }

        };

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        AirportDetailsTask task = new AirportDetailsTask();
        task.execute( siteNumber );
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction( MetarService.ACTION_GET_METAR );
        registerReceiver( mReceiver, filter );
        //Request metar from cache in case user navigates back to this activity
        requestMetars();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver( mReceiver );
    }

    private final class AwosData implements Comparable<AwosData> {

            public String ICAO_CODE;
            public String SENSOR_IDENT;
            public String SENSOR_TYPE;
            public double LATITUDE;
            public double LONGITUDE;
            public String FREQUENCY;
            public String PHONE;
            public String NAME;
            public float DISTANCE;
            public float BEARING;

            public AwosData( Cursor c, float declination ) {
                ICAO_CODE = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
                SENSOR_IDENT = c.getString( c.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
                SENSOR_TYPE = c.getString( c.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
                LATITUDE = c.getDouble( c.getColumnIndex( Awos.STATION_LATTITUDE_DEGREES ) );
                LONGITUDE = c.getDouble( c.getColumnIndex( Awos.STATION_LONGITUDE_DEGREES ) );
                FREQUENCY = c.getString( c.getColumnIndex( Awos.STATION_FREQUENCY ) );
                PHONE = c.getString( c.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
                NAME = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );

                // Now calculate the distance to this wx station
                float[] results = new float[ 2 ];
                Location.distanceBetween( mLocation.getLatitude(), mLocation.getLongitude(), 
                        LATITUDE, LONGITUDE, results );
                DISTANCE = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
                BEARING = ( results[ 1 ]+declination+360 )%360;
            }

            @Override
            public int compareTo( AwosData another ) {
                if ( this.DISTANCE > another.DISTANCE ) {
                    return 1;
                } else if ( this.DISTANCE < another.DISTANCE ) {
                    return -1;
                }
                return 0;
            }
    }

    private final class AirportDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 8 ];

            Cursor apt = getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { Runways.SITE_NUMBER, Runways.RUNWAY_ID,
                    Runways.RUNWAY_LENGTH, Runways.RUNWAY_WIDTH, Runways.SURFACE_TYPE,
                    Runways.BASE_END_HEADING },
                    Runways.SITE_NUMBER+"=? AND "+Runways.RUNWAY_LENGTH+" > 0",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 1 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            c = builder.query( db, new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=?"
                    +"AND "+Remarks.REMARK_NAME+" in ('E147', 'A3', 'A24', 'A70', 'A75', 'A82')",
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

            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            int elev_msl = apt.getInt( apt.getColumnIndex( Airports.ELEVATION_MSL ) );
            mLocation = new Location( "" );
            mLocation.setLatitude( lat );
            mLocation.setLongitude( lon );
            mLocation.setAltitude( elev_msl );

            // Get the bounding box first to do a quick query as a first cut
            double[] box = GeoUtils.getBoundingBox( mLocation, 20 );

            double radLatMin = box[ 0 ];
            double radLatMax = box[ 1 ];
            double radLonMin = box[ 2 ];
            double radLonMax = box[ 3 ];

            // Check if 180th Meridian lies within the bounding Box
            boolean isCrossingMeridian180 = ( radLonMin > radLonMax );
            String selection = "("
                +Awos.STATION_LATTITUDE_DEGREES+">=? AND "+Awos.STATION_LATTITUDE_DEGREES+"<=?"
                +") AND ("+Awos.STATION_LONGITUDE_DEGREES+">=? "
                +(isCrossingMeridian180? "OR " : "AND ")+Awos.STATION_LONGITUDE_DEGREES+"<=?)"
                +" AND "+Awos.COMMISSIONING_STATUS+"='Y'";
            String[] selectionArgs = {
                    String.valueOf( Math.toDegrees( radLatMin ) ), 
                    String.valueOf( Math.toDegrees( radLatMax ) ),
                    String.valueOf( Math.toDegrees( radLonMin ) ),
                    String.valueOf( Math.toDegrees( radLonMax ) )
                    };

            builder = new SQLiteQueryBuilder();
            builder.setTables( Awos.TABLE_NAME+" w LEFT OUTER JOIN "+Airports.TABLE_NAME+" a"
                    +" ON w."+Awos.SITE_NUMBER+" = a."+Airports.SITE_NUMBER );
            c = builder.query( db, new String[] { "w.*, a."+Airports.ICAO_CODE,
                    "a."+Airports.FACILITY_NAME }, selection, selectionArgs,
                    null, null, null, null );

            String[] columns = new String[] {
                    BaseColumns._ID,
                    Airports.ICAO_CODE,
                    Awos.WX_SENSOR_IDENT,
                    Awos.WX_SENSOR_TYPE,
                    Awos.STATION_FREQUENCY,
                    Awos.STATION_PHONE_NUMBER,
                    Airports.FACILITY_NAME,
                    "DISTANCE",
                    "BEARING"
            };
            MatrixCursor matrix = new MatrixCursor( columns );

            // Now find the magnetic declination at this location
            float declination = GeoUtils.getMagneticDeclination( mLocation );

            if ( c.moveToFirst() ) {
                AwosData[] awosList = new AwosData[ c.getCount() ];
                do {
                    AwosData awos = new AwosData( c, declination );
                    awosList[ c.getPosition() ] = awos;
                } while ( c.moveToNext() );

                // Sort the airport list by distance from current location
                Arrays.sort( awosList );

                // Build a cursor out of the sorted wx station list
                for ( AwosData awos : awosList ) {
                    if ( awos.DISTANCE <= 20 ) {
                        MatrixCursor.RowBuilder row = matrix.newRow();
                        row.add( matrix.getPosition() )
                            .add( awos.ICAO_CODE )
                            .add( awos.SENSOR_IDENT )
                            .add( awos.SENSOR_TYPE )
                            .add( awos.FREQUENCY )
                            .add( awos.PHONE )
                            .add( awos.NAME )
                            .add( awos.DISTANCE )
                            .add( awos.BEARING );
                    }
                }
            }

            c.close();
            cursors[ 6 ] = matrix;

            String faa_code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Aff3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" }, Aff3.IFR_FACILITY_ID+"=?",
                    new String[] { faa_code }, null, null, null, null );
            cursors[ 7 ] = c;

            // Extras bundle for "Nearby" activity
            mExtras = new Bundle();
            String code = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( code == null  || code.length() == 0 ) {
                code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            }
            mExtras.putString( NearbyActivity.APT_CODE, code );
            mExtras.putParcelable( NearbyActivity.APT_LOCATION, mLocation );

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            setContentView( R.layout.airport_detail_view );

            Cursor apt = result[ 0 ];
            showAirportTitle( apt );
            showCommunicationsDetails( result );
            showRunwayDetails( result );
            showAwosDetails( result );
            showOperationsDetails( result );
            showRemarks( result );
            showServicesDetails( result );
            showOtherDetails( result );

            TextView tv = (TextView) findViewById( R.id.effective_date );
            tv.setText( "Effective date: "
                    +apt.getString( apt.getColumnIndex( Airports.EFFECTIVE_DATE ) ) );
        }

    }

    protected void requestMetars() {
        // Now get the METAR if already in the cache
        String[] icaoCodes = new String[ mAwosMap.size() ];
        mAwosMap.keySet().toArray( icaoCodes );
        for ( String icaoCode : icaoCodes ) {
            Intent service = new Intent( AirportDetailsActivity.this, MetarService.class );
            service.setAction( MetarService.ACTION_GET_METAR );
            service.putExtra( MetarService.STATION_ID, icaoCode );
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
            boolean alwaysAutoFetch = prefs.getBoolean(
                    PreferencesActivity.ALWAYS_AUTO_FETCH_WEATHER, false );
            if ( !alwaysAutoFetch && !NetworkUtils.isConnectedToWifi( this ) ) {
                service.putExtra( MetarService.CACHE_ONLY, true );
            }
            startService( service );
        }
    }

    protected void showCommunicationsDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );

        TableLayout layout = (TableLayout) findViewById( R.id.detail_comm_layout );
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
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                ++row;
                int resId = getSelectorResourceForRow( row-1, row+1 );
                addClickableRow( layout, "ATC", intent, resId );
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
                            addRow( layout, key, pair );
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
                        String extra = "("+alt+" altitude)";
                        String type = aff3.getString( aff3.getColumnIndex( Aff3.FACILITY_TYPE ) );
                        if ( !type.equals( "ARTCC" ) ) {
                            extra = aff3.getString( aff3.getColumnIndex( Aff3.SITE_LOCATION ) )
                                    +" "+type+" "+extra;
                        }
                        if ( row > 0 ) {
                            addSeparator( layout );
                        }
                        addRow( layout, DataUtils.decodeArtcc( artcc ),
                                Pair.create( String.format( "%.3f", freq ), extra ) );
                        ++row;
                    } while ( aff3.moveToNext() );
                }
            }
        }

        if ( row > 0 ) {
            addSeparator( layout );
        }

        ++row;
        Intent fss = new Intent( this, FssCommActivity.class );
        fss.putExtra( Airports.SITE_NUMBER, siteNumber );
        int resId = getSelectorResourceForRow( row, row+2 );
        addClickableRow( layout, "FSS outlets", fss, resId );

        addSeparator( layout );

        ++row;
        Intent navaids = new Intent( this, NavaidsActivity.class );
        navaids.putExtra( Airports.SITE_NUMBER, siteNumber );
        resId = getSelectorResourceForRow( row, row+1 );
        addClickableRow( layout, "Navaids", navaids, resId );
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
        TableLayout rwyLayout = (TableLayout) findViewById( R.id.detail_rwy_layout );
        TableLayout heliLayout = (TableLayout) findViewById( R.id.detail_heli_layout );
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
            tv = (TextView) findViewById( R.id.detail_rwy_label );
            tv.setVisibility( View.GONE );
            rwyLayout.setVisibility( View.GONE );
        }
        if ( heliNum == 0 ) {
            // No helipads so remove the section
            tv = (TextView) findViewById( R.id.detail_heli_label );
            tv.setVisibility( View.GONE );
            heliLayout.setVisibility( View.GONE );
        }
    }

    protected void showAwosDetails( Cursor[] result ) {
        TextView label = (TextView) findViewById( R.id.detail_awos_label );
        TableLayout layout = (TableLayout) findViewById( R.id.detail_awos_layout );
        Cursor awos = result[ 6 ];

        if ( awos.moveToFirst() ) {
            do {
                String icaoCode = awos.getString( awos.getColumnIndex( Airports.ICAO_CODE ) );
                String id = awos.getString( awos.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
                if ( icaoCode == null || icaoCode.length() == 0 ) {
                    icaoCode = "K"+id;
                }
                String type = awos.getString( awos.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
                String freq = awos.getString( awos.getColumnIndex( Awos.STATION_FREQUENCY ) );
                String phone = awos.getString( awos.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
                String name = awos.getString( awos.getColumnIndex( Airports.FACILITY_NAME ) );
                float distance = awos.getFloat( awos.getColumnIndex( "DISTANCE" ) );
                float bearing = awos.getFloat( awos.getColumnIndex( "BEARING" ) );
                if ( awos.getPosition() > 0 ) {
                    addSeparator( layout );
                }
                Intent intent = new Intent( this, WxDetailActivity.class );
                intent.putExtra( Awos.WX_SENSOR_IDENT, id );
                intent.putExtra( MetarService.STATION_ID, icaoCode );
                int resid = getSelectorResourceForRow( awos.getPosition(), awos.getCount() );
                addAwosRow( layout, icaoCode, name, type, freq, phone, distance,
                        bearing, intent, resid );
            } while ( awos.moveToNext() );

            // Request the metar from cache
            requestMetars();
        } else {
            label.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showOperationsDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        TableLayout layout = (TableLayout) findViewById( R.id.detail_operations_layout );
        String use = apt.getString( apt.getColumnIndex( Airports.FACILITY_USE ) );
        addRow( layout, "Airport use", DataUtils.decodeFacilityUse( use ) );
        String timezoneId = apt.getString( apt.getColumnIndex( Airports.TIMEZONE_ID ) );
        if ( timezoneId.length() > 0 ) {
            addSeparator( layout );
            TimeZone tz = TimeZone.getTimeZone( timezoneId );
            addRow( layout, "Local time zone", DataUtils.getTimeZoneAsString( tz ) );
        }
        String activation = apt.getString( apt.getColumnIndex( Airports.ACTIVATION_DATE ) );
        if ( activation.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Activation date", activation );
        }
        String tower = apt.getString( apt.getColumnIndex( Airports.TOWER_ON_SITE ) );
        addSeparator( layout );
        addRow( layout, "Control tower", tower.equals( "Y" )? "Yes" : "No" );
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
        String sectional = apt.getString( apt.getColumnIndex( Airports.SECTIONAL_CHART ) );
        if ( sectional.length() > 0 ) {
            addSeparator( layout );
            String lat = apt.getString( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            String lon = apt.getString( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            if ( lat.length() > 0 && lon.length() > 0 ) {
                // Link to the sectional at SkyVector if location is available
                Uri uri = Uri.parse( String.format(
                        "http://skyvector.com/?ll=%s,%s&zoom=1", lat, lon ) );
                Intent intent = new Intent( Intent.ACTION_VIEW, uri );
                addClickableRow( layout, "Sectional chart", sectional, intent, 
                        R.drawable.row_selector_middle );
            } else {
                addRow( layout, "Sectional chart", sectional );
            }
        }
        addSeparator( layout );
        Intent intent = new Intent( this, AlmanacActivity.class );
        String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addClickableRow( layout, "Sunset/Sunrise", intent, R.drawable.row_selector_middle );
        addSeparator( layout );
        intent = new Intent( this, AirportNotamActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addClickableRow( layout, "NOTAMs", intent, R.drawable.row_selector_bottom );
    }

    protected void showRemarks( Cursor[] result ) {
        int row = 0;
        TextView label = (TextView) findViewById( R.id.detail_remarks_label );
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_remarks_layout );
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
        TableLayout layout = (TableLayout) findViewById( R.id.detail_services_layout );
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
        TableLayout layout = (TableLayout) findViewById( R.id.detail_other_layout );
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

    protected void addAwosRow( TableLayout table, String id, String name, String type, 
            String freq, String phone, float distance, float bearing,
            Intent intent, int resid ) {
        LinearLayout layout = (LinearLayout) inflate( R.layout.awos_detail_item );
        layout.setBackgroundResource( resid );
        layout.setTag( intent );

        TextView tv = (TextView) layout.findViewById( R.id.awos_station_name );
        mAwosMap.put( id, tv );
        if ( name != null && name.length() > 0 ) {
            tv.setText( id+" - "+name );
        } else {
            tv.setText( id );
        }

        if ( freq != null && freq.length() > 0 ) {
            try {
                tv = (TextView) layout.findViewById( R.id.awos_freq );
                tv.setText( String.format( "%.3f", Double.valueOf( freq ) ) );
            } catch ( NumberFormatException e ) {
            }
        }

        if ( phone != null && phone.length() > 0 ) {
            tv = (TextView) layout.findViewById( R.id.awos_phone );
            tv.setText( phone );
            makeClickToCall( tv );
        }

        tv = (TextView) layout.findViewById( R.id.awos_info );
        if ( distance > 1 ) {
            tv.setText( String.format( "%s, %.0fNM %s", type, distance,
                    GeoUtils.getCardinalDirection( bearing ) ) );
        } else {
            tv.setText( type+", On-site" );
        }

        layout.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                Intent intent = (Intent) v.getTag();
                startActivity( intent );
            }

        } );

        table.addView( layout, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRunwayRow( TableLayout table, Cursor c, int resid ) {
        String siteNumber = c.getString( c.getColumnIndex( Runways.SITE_NUMBER ) );
        String runwayId = c.getString( c.getColumnIndex( Runways.RUNWAY_ID ) );
        int length = c.getInt( c.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        int width = c.getInt( c.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        String surfaceType = c.getString( c.getColumnIndex( Runways.SURFACE_TYPE ) );

        int heading = c.getInt( c.getColumnIndex( Runways.BASE_END_HEADING ) );
        if ( heading > 0 ) {
            heading += GeoUtils.getMagneticDeclination( mLocation );
        }

        TableRow row = (TableRow) inflate( R.layout.runway_detail_item );
        row.setBackgroundResource( resid );

        TextView tv = (TextView) row.findViewById( R.id.runway_id );
        tv.setText( runwayId );
        setRunwayDrawable( tv, runwayId, length, heading );

        tv = (TextView) row.findViewById( R.id.runway_size );
        tv.setText( String.valueOf( length )+"' x "+String.valueOf( width )+"'" );

        tv = (TextView) row.findViewById( R.id.runway_surface );
        tv.setText( DataUtils.decodeSurfaceType( surfaceType ) );

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

    protected void setRunwayDrawable( TextView tv, String runwayId, int length, int heading ) {
        int resid = 0;
        if ( runwayId.startsWith( "H" ) ) {
            resid = R.drawable.helipad;
        } else {
            if ( length > 10000 ) {
                resid = R.drawable.runway9;
            } else if ( length > 9000 ) {
                resid = R.drawable.runway8;
            } else if ( length > 8000 ) {
                resid = R.drawable.runway7;
            } else if ( length > 7000 ) {
                resid = R.drawable.runway6;
            } else if ( length > 6000 ) {
                resid = R.drawable.runway5;
            } else if ( length > 5000 ) {
                resid = R.drawable.runway4;
            } else if ( length > 4000 ) {
                resid = R.drawable.runway3;
            } else if ( length > 3000 ) {
                resid = R.drawable.runway2;
            } else if ( length > 2000 ) {
                resid = R.drawable.runway1;
            } else {
                resid = R.drawable.runway0;
            }

            if ( heading == 0 ) {
                // Actual heading is not available, try to deduce it from runway id
                heading = getRunwayHeading( runwayId );
            }
        }

        Drawable rwy = GuiUtils.getRotatedDrawable( this, resid, heading );
        tv.setCompoundDrawablesWithIntrinsicBounds( rwy, null, null, null );
        tv.setCompoundDrawablePadding( convertDpToPx( 5 ) );
    }

    protected int getRunwayHeading( String runwayId ) {
        int index = 0;
        while ( index < runwayId.length() ) {
            if ( !Character.isDigit( runwayId.charAt( index ) ) ) {
                break;
            }
            ++index;
        }

        int heading = 0;
        try {
            heading = Integer.valueOf( runwayId.substring( 0, index ) )*10;
        } catch ( Exception e ) {
        }

        return heading;
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

    protected void showWxInfo( Metar metar ) {
        TextView tv = mAwosMap.get( metar.stationId );
        if ( tv != null ) {
            if ( metar.isValid ) {
                WxUtils.setColorizedWxDrawable( tv, metar );
            } else {
                GuiUtils.setTextViewDrawable( tv, R.drawable.error );
            }
        }
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
