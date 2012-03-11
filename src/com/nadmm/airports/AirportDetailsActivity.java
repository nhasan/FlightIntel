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
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Aff3;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.Dtpp;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.Tower1;
import com.nadmm.airports.DatabaseManager.Tower3;
import com.nadmm.airports.DatabaseManager.Tower6;
import com.nadmm.airports.DatabaseManager.Tower7;
import com.nadmm.airports.DatabaseManager.Tower8;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.dtpp.DtppActivity;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.wx.Metar;
import com.nadmm.airports.wx.MetarService;
import com.nadmm.airports.wx.NoaaService;
import com.nadmm.airports.wx.WxDetailActivity;
import com.nadmm.airports.wx.WxUtils;

public class AirportDetailsActivity extends ActivityBase {

    protected Location mLocation;
    protected float mDeclination;
    protected String mSiteNumber;

    private final static String DISTANCE = "DISTANCE";
    private final static String BEARING = "BEARING";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();
        mSiteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        setContentView( createContentView( R.layout.airport_activity_layout ) );

        addFragment( AirportDetailsFragment.class );
    }

    protected AirportDetailsTask startTask() {
        AirportDetailsTask task = new AirportDetailsTask();
        task.execute( mSiteNumber );
        return task;
    }

    private final class AwosData implements Comparable<AwosData> {

            public String ICAO_CODE;
            public String SENSOR_IDENT;
            public String SENSOR_TYPE;
            public double LATITUDE;
            public double LONGITUDE;
            public String FREQUENCY;
            public String FREQUENCY2;
            public String PHONE;
            public String NAME;
            public float DISTANCE;
            public float BEARING;

            public AwosData( String icaoCode, String id, String type, double lat, double lon,
                    String freq, String freq2, String phone, String name ) {
                ICAO_CODE = icaoCode;
                SENSOR_IDENT = id;
                SENSOR_TYPE = type;
                if ( SENSOR_TYPE == null || SENSOR_TYPE.length() == 0 ) {
                    SENSOR_TYPE = "ASOS/AWOS";
                }
                LATITUDE = lat;
                LONGITUDE = lon;
                FREQUENCY = freq;
                FREQUENCY2 = freq2;
                PHONE = phone;
                NAME = name;

                // Now calculate the distance to this wx station
                float[] results = new float[ 2 ];
                Location.distanceBetween( mLocation.getLatitude(), mLocation.getLongitude(), 
                        LATITUDE, LONGITUDE, results );
                DISTANCE = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
                BEARING = GeoUtils.applyDeclination( results[ 1 ], mDeclination );
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

            DatabaseManager dbManager = getDbManager();
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 10 ];

            Cursor apt = getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            int elev_msl = apt.getInt( apt.getColumnIndex( Airports.ELEVATION_MSL ) );

            mLocation = new Location( "" );
            mLocation.setLatitude( lat );
            mLocation.setLongitude( lon );
            mLocation.setAltitude( elev_msl );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { Runways.SITE_NUMBER, Runways.RUNWAY_ID,
                    Runways.RUNWAY_LENGTH, Runways.RUNWAY_WIDTH, Runways.SURFACE_TYPE,
                    Runways.BASE_END_HEADING, Runways.BASE_END_ID, Runways.RECIPROCAL_END_ID },
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
                builder = new SQLiteQueryBuilder();
                builder.setTables( Tower6.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Tower3.FACILITY_ID+"=?",
                        new String[] { faaCode }, null, null, Tower6.ELEMENT_NUMBER, null );
                cursors[ 5 ] = c;
            }

            // Get the magnetic declination at this location
            mDeclination = GeoUtils.getMagneticDeclination( mLocation );

            // Get the bounding box first to do a quick query as a first cut
            double[] box = GeoUtils.getBoundingBox( mLocation, 20 );
            double radLatMin = box[ 0 ];
            double radLatMax = box[ 1 ];
            double radLonMin = box[ 2 ];
            double radLonMax = box[ 3 ];

            ArrayList<AwosData> awosList = new ArrayList<AwosData>();

            // Check if 180th Meridian lies within the bounding Box
            boolean isCrossingMeridian180 = ( radLonMin > radLonMax );
            String[] selectionArgs = {
                    String.valueOf( Math.toDegrees( radLatMin ) ), 
                    String.valueOf( Math.toDegrees( radLatMax ) ),
                    String.valueOf( Math.toDegrees( radLonMin ) ),
                    String.valueOf( Math.toDegrees( radLonMax ) )
                    };

            String[] wxColumns = new String[] {
                    Wxs.STATION_ID,
                    Wxs.STATION_NAME,
                    Wxs.STATION_ELEVATOIN_METER,
                    "x."+Wxs.STATION_LATITUDE_DEGREES,
                    "x."+Wxs.STATION_LONGITUDE_DEGREES,
                    Wxs.STATION_STATE,
                    Awos.WX_SENSOR_IDENT,
                    Awos.WX_SENSOR_TYPE,
                    Awos.STATION_FREQUENCY,
                    Awos.SECOND_STATION_FREQUENCY,
                    Awos.STATION_PHONE_NUMBER,
                    Awos.COMMISSIONING_STATUS
            };

            String selection = "("
                    +"x."+Wxs.STATION_LATITUDE_DEGREES+">=? AND "
                    +"x."+Wxs.STATION_LATITUDE_DEGREES+"<=?"
                    +") AND (x."+Wxs.STATION_LONGITUDE_DEGREES+">=? "
                    +(isCrossingMeridian180? "OR " : "AND ")
                    +"x."+Wxs.STATION_LONGITUDE_DEGREES+"<=?)";
            builder = new SQLiteQueryBuilder();
            builder.setTables( Wxs.TABLE_NAME+" x"
                    +" LEFT JOIN "+Airports.TABLE_NAME+" a"
                    +" ON x."+Wxs.STATION_ID+" = a."+Airports.ICAO_CODE
                    +" LEFT JOIN "+Awos.TABLE_NAME+" w"
                    +" ON w."+Awos.WX_SENSOR_IDENT+" = a."+Airports.FAA_CODE );
            c = builder.query( db, wxColumns, selection, selectionArgs,
                    null, null, null, null );
            if ( c.moveToFirst() ) {
                do {
                    String status = c.getString( c.getColumnIndex( Awos.COMMISSIONING_STATUS ) );
                    if ( status != null && status.equals( "N" ) ) {
                        // Skip the inactive station
                        continue;
                    }
                    String icaoCode = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
                    String name = c.getString( c.getColumnIndex( Wxs.STATION_NAME ) );
                    lat = c.getDouble( c.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
                    lon = c.getDouble( c.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
                    String id = c.getString( c.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
                    String type = c.getString( c.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
                    String freq = c.getString( c.getColumnIndex( Awos.STATION_FREQUENCY ) );
                    String freq2 = c.getString( c.getColumnIndex( Awos.SECOND_STATION_FREQUENCY ) );
                    String phone = c.getString( c.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
                    AwosData awos = new AwosData( icaoCode, id, type, lat, lon,
                            freq, freq2, phone, name );
                    awosList.add( awos );
                } while ( c.moveToNext() );
            }
            c.close();

            // Sort the airport list by distance from current location
            Object[] awosSortedList = awosList.toArray();
            Arrays.sort( awosSortedList );

            // Build a cursor out of the sorted wx station list
            String[] columns = new String[] {
                    BaseColumns._ID,
                    Airports.ICAO_CODE,
                    Awos.WX_SENSOR_IDENT,
                    Awos.WX_SENSOR_TYPE,
                    Awos.STATION_FREQUENCY,
                    Awos.SECOND_STATION_FREQUENCY,
                    Awos.STATION_PHONE_NUMBER,
                    Airports.FACILITY_NAME,
                    DISTANCE,
                    BEARING
            };
            MatrixCursor matrix = new MatrixCursor( columns );

            for ( Object o : awosSortedList ) {
                AwosData awos = (AwosData) o;
                if ( awos.DISTANCE <= 20 ) {
                    MatrixCursor.RowBuilder row = matrix.newRow();
                    row.add( matrix.getPosition() )
                        .add( awos.ICAO_CODE )
                        .add( awos.SENSOR_IDENT )
                        .add( awos.SENSOR_TYPE )
                        .add( awos.FREQUENCY )
                        .add( awos.FREQUENCY2 )
                        .add( awos.PHONE )
                        .add( awos.NAME )
                        .add( awos.DISTANCE )
                        .add( awos.BEARING );
                }
            }

            cursors[ 6 ] = matrix;

            String faa_code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Aff3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" }, Aff3.IFR_FACILITY_ID+"=?",
                    new String[] { faa_code }, null, null, null, null );
            cursors[ 7 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower8.TABLE_NAME );
            c = builder.query( db, new String[] { "*" }, Tower8.FACILITY_ID+"=? ",
                    new String[] { faaCode }, null, null, null, null );
            cursors[ 8 ] = c;

            db = dbManager.getDatabase( DatabaseManager.DB_DTPP );
            if ( db != null ) {
                builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                c = builder.query( db, new String[] { "*" }, Dtpp.FAA_CODE+"=? ",
                        new String[] { faaCode }, null, null, null, null );
                cursors[ 9 ] = c;
            }

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            AirportDetailsFragment f = (AirportDetailsFragment) getFragment(
                    AirportDetailsFragment.class );
            if ( f != null ) {
                f.onResult( result );
            }
        }

    }

    public static class AirportDetailsFragment extends FragmentBase {

        private final HashSet<TextView> mAwosViews = new HashSet<TextView>();
        private final HashSet<TextView> mRunwayViews = new HashSet<TextView>();
        private BroadcastReceiver mReceiver;
        private Bundle mExtras;
        private Location mLocation;
        private float mDeclination;
        private String mIcaoCode;
        AirportDetailsTask mTask;
        int mWxUpdates = 0;

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );
            mReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive( Context context, Intent intent ) {
                    Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
                    showWxInfo( metar );

                    ++mWxUpdates;
                    if ( mWxUpdates == mAwosViews.size() ) {
                        // We have all the wx updates, stop the refresh animation
                        mWxUpdates = 0;
                        ( (AirportDetailsActivity) getActivityBase() ).stopRefreshAnimation();
                    }
                }

            };
        }

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View view = inflater.inflate( R.layout.airport_detail_view, container, false );
            return view;
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            super.onActivityCreated( savedInstanceState );
            AirportDetailsActivity activity = (AirportDetailsActivity) getActivityBase();
            mTask = activity.startTask();
        }

        @Override
        public void onResume() {
            super.onResume();
            IntentFilter filter = new IntentFilter();
            filter.addAction( NoaaService.ACTION_GET_METAR );
            getActivityBase().registerReceiver( mReceiver, filter );
            requestMetars( false );
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivityBase().unregisterReceiver( mReceiver );
            if ( mTask != null ) {
                mTask.cancel( true );
            }
        }

        public void onResult( Cursor[] result ) {
            mTask = null;
            showDetails( result );
        }

        protected void showDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];

            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            int elev_msl = apt.getInt( apt.getColumnIndex( Airports.ELEVATION_MSL ) );
            mLocation = new Location( "" );
            mLocation.setLatitude( lat );
            mLocation.setLongitude( lon );
            mLocation.setAltitude( elev_msl );

            // Get the magnetic declination at this location
            mDeclination = GeoUtils.getMagneticDeclination( mLocation );

            mIcaoCode = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( mIcaoCode == null || mIcaoCode.length() == 0 ) {
                mIcaoCode = "K"+apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            }

            // Extras bundle for "Nearby" activity
            mExtras = new Bundle();
            mExtras.putParcelable( NearbyActivity.APT_LOCATION, mLocation );

            getActivityBase().setActionBarTitle( apt );
            getActivityBase().showAirportTitle( apt );

            showCommunicationsDetails( result );
            showRunwayDetails( result );
            showAwosDetails( result );
            showOperationsDetails( result );
            showTppDetails( result );
            showRemarks( result );
            showServicesDetails( result );
            showOtherDetails( result );

            TextView tv = (TextView) findViewById( R.id.effective_date );
            tv.setText( "Effective date: "
                    +apt.getString( apt.getColumnIndex( Airports.EFFECTIVE_DATE ) ) );

            requestMetars( false );

            getActivityBase().setContentShown( true );
        }

        protected void showCommunicationsDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );

            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_comm_layout );
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

            if ( row > 0 ) {
                addSeparator( layout );
            }
            Intent intent = new Intent( getActivity(), CommDetailsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            ++row;
            int resId = getSelectorResourceForRow( row-1, row+1 );
            addClickableRow( layout, "ATC", intent, resId );

            if ( row > 0 ) {
                addSeparator( layout );
            }

            ++row;
            Intent fss = new Intent( getActivity(), FssCommActivity.class );
            fss.putExtra( Airports.SITE_NUMBER, siteNumber );
            resId = getSelectorResourceForRow( row, row+2 );
            addClickableRow( layout, "FSS outlets", fss, resId );

            addSeparator( layout );

            ++row;
            Intent navaids = new Intent( getActivity(), NavaidsActivity.class );
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
            LinearLayout rwyLayout = (LinearLayout) findViewById( R.id.detail_rwy_layout );
            LinearLayout heliLayout = (LinearLayout) findViewById( R.id.detail_heli_layout );
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
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_awos_layout );
            Cursor awos = result[ 6 ];

            if ( awos.moveToFirst() ) {
                do {
                    String icaoCode = awos.getString( awos.getColumnIndex( Airports.ICAO_CODE ) );
                    String sensorId = awos.getString( awos.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
                    if ( icaoCode == null || icaoCode.length() == 0 ) {
                        icaoCode = "K"+sensorId;
                    }
                    String type = awos.getString( awos.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
                    String freq = awos.getString( awos.getColumnIndex( Awos.STATION_FREQUENCY ) );
                    if ( freq == null || freq.length() == 0 ) {
                        freq = awos.getString( awos.getColumnIndex( Awos.SECOND_STATION_FREQUENCY ) );
                    }
                    String phone = awos.getString( awos.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
                    String name = awos.getString( awos.getColumnIndex( Airports.FACILITY_NAME ) );
                    float distance = awos.getFloat( awos.getColumnIndex( "DISTANCE" ) );
                    float bearing = awos.getFloat( awos.getColumnIndex( "BEARING" ) );
                    if ( awos.getPosition() > 0 ) {
                        addSeparator( layout );
                    }
                    Intent intent = new Intent( getActivity(), WxDetailActivity.class );
                    Bundle args = new Bundle();
                    args.putString( NoaaService.STATION_ID, icaoCode );
                    args.putString( Awos.WX_SENSOR_IDENT, sensorId );
                    intent.putExtras( args );
                    int resid = getSelectorResourceForRow( awos.getPosition(), awos.getCount() );
                    addAwosRow( layout, icaoCode, name, type, freq, phone, distance,
                            bearing, intent, resid );
                } while ( awos.moveToNext() );
            } else {
                label.setVisibility( View.GONE );
                layout.setVisibility( View.GONE );
            }
        }

        protected void showOperationsDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_operations_layout );
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
            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            addSeparator( layout );
            addRow( layout, "FAA code", faaCode );
            Cursor twr8 = result[ 8 ];
            if ( twr8.moveToFirst() ) {
                String airspace = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_TYPES ) );
                String value = DataUtils.decodeAirspace( airspace );
                String hours = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_HOURS ) );
                addSeparator( layout );
                addRow( layout, "Airspace", value, hours );
            }
            addSeparator( layout );
            String tower = apt.getString( apt.getColumnIndex( Airports.TOWER_ON_SITE ) );
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
            Intent intent = new Intent( getActivity(), AlmanacActivity.class );
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Sunrise and sunset", intent, R.drawable.row_selector_middle );
            addSeparator( layout );
            intent = new Intent( getActivity(), AirportNotamActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "NOTAMs", intent, R.drawable.row_selector_bottom );
        }

        protected void showTppDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
            Cursor dtpp = result[ 9 ];
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_ifr_layout );
            if ( dtpp != null && dtpp.moveToFirst() ) {
                Intent intent = new Intent( getActivity(), DtppActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                addClickableRow( layout, "Instrument procedures", intent,
                        R.drawable.row_selector );
            } else {
                addRow( layout, "No instrument procedures found" );
            }
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
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_services_layout );
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
            Intent intent = new Intent( getActivity(), ServicesDetailsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER,
                    apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) ) );
            addClickableRow( layout, "Other services", intent, R.drawable.row_selector_bottom );
        }

        protected void showOtherDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_other_layout );
            Intent intent = new Intent( getActivity(), OwnershipDetailsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Ownership and contact", intent, R.drawable.row_selector_top );
            intent = new Intent( getActivity(), RemarkDetailsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addSeparator( layout );
            addClickableRow( layout, "Additional remarks", intent, R.drawable.row_selector_middle );
            intent = new Intent( getActivity(), AttendanceDetailsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addSeparator( layout );
            addClickableRow( layout, "Attendance", intent, R.drawable.row_selector_middle );
            addSeparator( layout );
            intent = new Intent( getActivity(), NearbyActivity.class );
            intent.putExtras( mExtras );
            addClickableRow( layout, "Nearby airports", intent, R.drawable.row_selector_bottom );
        }

        protected void addAwosRow( LinearLayout layout, String id, String name, String type, 
                String freq, String phone, float distance, float bearing,
                final Intent intent, int resid ) {
            StringBuilder sb = new StringBuilder();
            sb.append( id );
            if ( name != null && name.length() > 0 ) {
                sb.append( " - " );
                sb.append( name );
            }
            String label1 = sb.toString();

            sb.setLength( 0 );
            if ( freq != null && freq.length() > 0 ) {
                try {
                    sb.append( String.format( "%.3f", Double.valueOf( freq ) ) );
                } catch ( NumberFormatException e ) {
                }
            }
            String value1 = sb.toString();

            sb.setLength( 0 );
            sb.append( type );
            if ( distance >= 2.5 ) {
                sb.append( String.format( ", %.0fNM %s", distance,
                        GeoUtils.getCardinalDirection( bearing ) ) );
            } else {
                sb.append( ", On-site" );
            }
            String label2 = sb.toString();
            String value2 = phone;

            View row = addClickableRow( layout, label1, value1, label2, value2, intent, resid );

            TextView tv = (TextView) row.findViewById( R.id.item_label );
            tv.setTag( id );
            mAwosViews.add( tv );
            // Make phone number clickable
            tv = (TextView) row.findViewById( R.id.item_extra_value );
            if ( tv.getText().length() > 0 ) {
                UiUtils.makeClickToCall( getActivity(), tv );
            }
        }

        protected void addRunwayRow( LinearLayout table, Cursor c, int resid ) {
            String siteNumber = c.getString( c.getColumnIndex( Runways.SITE_NUMBER ) );
            String runwayId = c.getString( c.getColumnIndex( Runways.RUNWAY_ID ) );
            int length = c.getInt( c.getColumnIndex( Runways.RUNWAY_LENGTH ) );
            int width = c.getInt( c.getColumnIndex( Runways.RUNWAY_WIDTH ) );
            String surfaceType = c.getString( c.getColumnIndex( Runways.SURFACE_TYPE ) );
            String baseId = c.getString( c.getColumnIndex( Runways.BASE_END_ID ) );
            String reciprocalId = c.getString( c.getColumnIndex( Runways.RECIPROCAL_END_ID ) );

            int heading = c.getInt( c.getColumnIndex( Runways.BASE_END_HEADING ) );
            if ( heading > 0 ) {
                heading = (int) GeoUtils.applyDeclination( heading, mDeclination );
            } else {
                // Actual heading is not available, try to deduce it from runway id
                heading = getRunwayHeading( runwayId );
            }

            LinearLayout row = (LinearLayout) inflate( R.layout.runway_detail_item );
            row.setBackgroundResource( resid );

            TextView tv = (TextView) row.findViewById( R.id.runway_id );
            tv.setText( runwayId );
            setRunwayDrawable( tv, runwayId, length, heading );

            tv = (TextView) row.findViewById( R.id.runway_size );
            tv.setText( String.format( "%s x %s",
                    FormatUtils.formatFeet( length ), FormatUtils.formatFeet( width ) ) );

            tv = (TextView) row.findViewById( R.id.runway_surface );
            tv.setText( DataUtils.decodeSurfaceType( surfaceType ) );

            if ( !runwayId.startsWith( "H" ) ) {
                // Save the textview and runway info for later use
                tv = (TextView) row.findViewById( R.id.runway_wind_info );
                Bundle tag = new Bundle();
                tag.putString( Runways.BASE_END_ID, baseId );
                tag.putString( Runways.RECIPROCAL_END_ID, reciprocalId );
                tag.putInt( Runways.BASE_END_HEADING, heading );
                tv.setTag( tag );
                mRunwayViews.add( tv );
            }

            final Bundle bundle = new Bundle();
            bundle.putString( Runways.SITE_NUMBER, siteNumber );
            bundle.putString( Runways.RUNWAY_ID, runwayId );
            row.setOnClickListener( new OnClickListener() {

                @Override
                public void onClick( View v ) {
                    Intent intent = new Intent( getActivityBase(), RunwayDetailsActivity.class );
                    intent.putExtras( bundle );
                    startActivity( intent );
                }

            } );

            table.addView( row, new LinearLayout.LayoutParams(
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
            }

            Drawable rwy = UiUtils.getRotatedDrawable( getActivity(), resid, heading );
            tv.setCompoundDrawablesWithIntrinsicBounds( rwy, null, null, null );
            tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( getActivity(), 5 ) );
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

        protected void requestMetars( boolean force ) {
            if ( mAwosViews.size() == 0 ) {
                return;
            }

            // Now get the METAR if already in the cache
            boolean cacheOnly = NetworkUtils.useCacheContentOnly( getActivity() );
            if ( force || !cacheOnly ) {
                getActivityBase().startRefreshAnimation();
            }

            for ( TextView tv : mAwosViews ) {
                String stationId = (String) tv.getTag();
                Intent service = new Intent( getActivityBase(), MetarService.class );
                service.setAction( NoaaService.ACTION_GET_METAR );
                service.putExtra( NoaaService.STATION_ID, stationId );
                if ( force ) {
                    service.putExtra( NoaaService.FORCE_REFRESH, true );
                } else if ( cacheOnly ) {
                    service.putExtra( NoaaService.CACHE_ONLY, true );
                }
                getActivityBase().startService( service );
            }
        }

        protected void showWxInfo( Metar metar ) {
            if ( metar.stationId == null ) {
                return;
            }

            if ( metar.isValid
                    && mIcaoCode != null
                    && mIcaoCode.equals( metar.stationId )
                    && WxUtils.isWindAvailable( metar ) ) {
                showRunwayWindInfo( metar );
            }

            for ( TextView tv : mAwosViews ) {
                String icaoCode = (String) tv.getTag();
                if ( icaoCode.equals( metar.stationId ) ) {
                    WxUtils.setColorizedWxDrawable( tv, metar, mDeclination );
                    break;
                }
            }
        }

        protected void showRunwayWindInfo( Metar metar ) {
            for ( TextView tv : mRunwayViews ) {
                Bundle tag = (Bundle) tv.getTag();
                String id = tag.getString( Runways.BASE_END_ID );
                long rwyHeading = tag.getInt( Runways.BASE_END_HEADING );
                long windDir = GeoUtils.applyDeclination( metar.windDirDegrees, mDeclination );
                long headWind = WxUtils.getHeadWindComponent( metar.windSpeedKnots,
                        windDir, rwyHeading );

                if ( headWind < 0 ) {
                    // If this is a tail wind, use the other end
                    id = tag.getString( Runways.RECIPROCAL_END_ID );
                    rwyHeading = ( rwyHeading+180 )%360;
                }
                long crossWind = WxUtils.getCrossWindComponent( metar.windSpeedKnots,
                        windDir, rwyHeading );
                String side = "right";
                if ( crossWind < 0 ) {
                    side = "left";
                    crossWind = Math.abs( crossWind );
                }
                StringBuilder windInfo = new StringBuilder();
                if ( crossWind > 0 ) {
                    boolean gusting = ( metar.windGustKnots < Integer.MAX_VALUE );
                    windInfo.append( String.format( "%d %s%s x-wind from %s",
                            crossWind, crossWind > 1? "knots" : "knot",
                            gusting? " gusting" : "" , side ) );
                } else {
                    windInfo.append( "no x-wind" );
                    if ( metar.windGustKnots < Integer.MAX_VALUE ) {
                        long gustFactor = ( metar.windGustKnots-metar.windSpeedKnots )/2;
                        windInfo.append( String.format( ", %d knots gust factor", gustFactor ) );
                    }
                }
                tv.setText( String.format( "Rwy %s: %s", id, windInfo.toString() ) );
                tv.setVisibility( View.VISIBLE );
            }
        }

    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        setRefreshItemVisible( true );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_refresh:
            AirportDetailsFragment f = (AirportDetailsFragment) getFragment(
                    AirportDetailsFragment.class );
            f.requestMetars( true );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
