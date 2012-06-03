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

import java.util.HashSet;
import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.Application;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Aff3;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Attendance;
import com.nadmm.airports.DatabaseManager.Awos1;
import com.nadmm.airports.DatabaseManager.Dafd;
import com.nadmm.airports.DatabaseManager.DafdCycle;
import com.nadmm.airports.DatabaseManager.Dtpp;
import com.nadmm.airports.DatabaseManager.LocationColumns;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.Tower1;
import com.nadmm.airports.DatabaseManager.Tower3;
import com.nadmm.airports.DatabaseManager.Tower6;
import com.nadmm.airports.DatabaseManager.Tower7;
import com.nadmm.airports.DatabaseManager.Tower8;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.aeronav.DafdService;
import com.nadmm.airports.aeronav.DtppActivity;
import com.nadmm.airports.donate.DonateActivity;
import com.nadmm.airports.notams.AirportNotamActivity;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.wx.Metar;
import com.nadmm.airports.wx.MetarService;
import com.nadmm.airports.wx.NearbyWxCursor;
import com.nadmm.airports.wx.NoaaService;
import com.nadmm.airports.wx.WxDetailActivity;
import com.nadmm.airports.wx.WxUtils;

public class AirportDetailsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.airport_activity_layout ) );

        Bundle args = getIntent().getExtras();
        addFragment( AirportDetailsFragment.class, args );
    }

    public static class AirportDetailsFragment extends FragmentBase {

        private final HashSet<TextView> mAwosViews = new HashSet<TextView>();
        private final HashSet<TextView> mRunwayViews = new HashSet<TextView>();
        private BroadcastReceiver mMetarReceiver;
        private BroadcastReceiver mDafdReceiver;
        private IntentFilter mMetarFilter;
        private IntentFilter mDafdFilter;
        private Location mLocation;
        private float mDeclination;
        private String mIcaoCode;
        int mWxUpdates = 0;

        private final class AirportDetailsTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                String siteNumber = params[ 0 ];

                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
                Cursor[] cursors = new Cursor[ 13 ];

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

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences( getActivity() );
                int radius = Integer.valueOf( prefs.getString(
                        PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );

                cursors[ 6 ] = new NearbyWxCursor( db, mLocation, radius );

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

                builder = new SQLiteQueryBuilder();
                builder.setTables( Attendance.TABLE_NAME );
                c = builder.query( db,
                        new String[] { Attendance.ATTENDANCE_SCHEDULE },
                        Attendance.SITE_NUMBER+"=?", new String[] { siteNumber },
                        null, null, Attendance.SEQUENCE_NUMBER, null );
                cursors[ 9 ] = c;

                db = getDatabase( DatabaseManager.DB_DTPP );
                if ( db != null ) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables( Dtpp.TABLE_NAME );
                    c = builder.query( db, new String[] { "*" }, Dtpp.FAA_CODE+"=? ",
                            new String[] { faaCode }, null, null, null, null );
                    cursors[ 10 ] = c;
                }

                db = getDatabase( DatabaseManager.DB_DAFD );
                if ( db != null ) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables( DafdCycle.TABLE_NAME );
                    c = builder.query( db, new String[] { "*" },
                            null, null, null, null, null, null );
                    cursors[ 11 ] = c;

                    builder = new SQLiteQueryBuilder();
                    builder.setTables( Dafd.TABLE_NAME );
                    c = builder.query( db, new String[] { "*" }, Dafd.FAA_CODE+"=? ",
                            new String[] { faaCode }, null, null, null, null );
                    cursors[ 12 ] = c;
                }

                return cursors;
            }

            @Override
            protected boolean onResult( Cursor[] result ) {
                showDetails( result );
                return true;
            }

        }

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );

            mMetarFilter = new IntentFilter();
            mMetarFilter.addAction( NoaaService.ACTION_GET_METAR );
            mMetarReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive( Context context, Intent intent ) {
                    Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
                    showWxInfo( metar );

                    ++mWxUpdates;
                    if ( mWxUpdates == mAwosViews.size() ) {
                        // We have all the wx updates, stop the refresh animation
                        mWxUpdates = 0;
                        stopRefreshAnimation();
                    }
                }

            };

            mDafdFilter = new IntentFilter();
            mDafdFilter.addAction( DafdService.ACTION_GET_AFD );
            mDafdReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive( Context context, Intent intent ) {
                    handleDafdBroadcast( intent );
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
            Bundle args = getArguments();
            String siteNumber = args.getString( Airports.SITE_NUMBER );
            setBackgroundTask( new AirportDetailsTask() ).execute( siteNumber );

            super.onActivityCreated( savedInstanceState );
        }

        @Override
        public void onResume() {
            super.onResume();
            getActivityBase().registerReceiver( mMetarReceiver, mMetarFilter );
            getActivityBase().registerReceiver( mDafdReceiver, mDafdFilter );
            requestMetars( false );
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivityBase().unregisterReceiver( mMetarReceiver );
            getActivityBase().unregisterReceiver( mDafdReceiver );
        }

        protected void showDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];

            mIcaoCode = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( mIcaoCode == null || mIcaoCode.length() == 0 ) {
                mIcaoCode = "K"+apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            }

            showAirportTitle( apt );

            showCommunicationsDetails( result );
            showRunwayDetails( result );
            showRemarks( result );
            showAwosDetails( result );
            showNearbyFacilities( result );
            showOperationsDetails( result );
            showAeroNavDetails( result );
            showServicesDetails( result );
            showOtherDetails( result );

            requestMetars( false );

            getActivityBase().setContentShown( true );
        }

        protected void showCommunicationsDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );

            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_comm_layout );

            String ctaf = apt.getString( apt.getColumnIndex( Airports.CTAF_FREQ ) );
            addRow( layout, "CTAF", ctaf.length() > 0? ctaf : "None" );
            String unicom = apt.getString( apt.getColumnIndex( Airports.UNICOM_FREQS ) );
            addRow( layout, "Unicom", unicom.length() > 0? unicom : "None" );
            Intent intent = new Intent( getActivity(), CommunicationsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "ATC", intent, R.drawable.row_selector_bottom );
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
                        int resId = getSelectorResourceForRow( heliNum, heliTot );
                        addRunwayRow( heliLayout, rwy, resId );
                    } else {
                        // This is a runway
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

        protected void showRemarks( Cursor[] result ) {
            int row = 0;
            TextView label = (TextView) findViewById( R.id.detail_remarks_label );
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_remarks_layout );
            Cursor rmk = result[ 2 ];
            if ( rmk.moveToFirst() ) {
                do {
                    String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                    addBulletedRow( layout, remark );
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

        protected void showAwosDetails( Cursor[] result ) {
            TextView label = (TextView) findViewById( R.id.detail_awos_label );
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_awos_layout );
            Cursor awos1 = result[ 6 ];

            if ( awos1.moveToFirst() ) {
                do {
                    String icaoCode = awos1.getString( awos1.getColumnIndex( Wxs.STATION_ID ) );
                    String sensorId = awos1.getString( awos1.getColumnIndex( Awos1.WX_SENSOR_IDENT ) );
                    if ( icaoCode == null || icaoCode.length() == 0 ) {
                        icaoCode = "K"+sensorId;
                    }
                    String type = awos1.getString( awos1.getColumnIndex( Awos1.WX_SENSOR_TYPE ) );
                    String freq = awos1.getString( awos1.getColumnIndex( Awos1.STATION_FREQUENCY ) );
                    if ( freq == null || freq.length() == 0 ) {
                        freq = awos1.getString( awos1.getColumnIndex( Awos1.SECOND_STATION_FREQUENCY ) );
                    }
                    String phone = awos1.getString( awos1.getColumnIndex( Awos1.STATION_PHONE_NUMBER ) );
                    String name = awos1.getString( awos1.getColumnIndex( Wxs.STATION_NAME ) );
                    float distance = awos1.getFloat( awos1.getColumnIndex( "DISTANCE" ) );
                    float bearing = awos1.getFloat( awos1.getColumnIndex( "BEARING" ) );
                    Intent intent = new Intent( getActivity(), WxDetailActivity.class );
                    Bundle args = new Bundle();
                    args.putString( NoaaService.STATION_ID, icaoCode );
                    args.putString( Awos1.WX_SENSOR_IDENT, sensorId );
                    intent.putExtras( args );
                    int resid = getSelectorResourceForRow( awos1.getPosition(), awos1.getCount() );
                    addAwosRow( layout, icaoCode, name, type, freq, phone, distance,
                            bearing, intent, resid );
                } while ( awos1.moveToNext() );
            } else {
                label.setVisibility( View.GONE );
                layout.setVisibility( View.GONE );
            }
        }

        protected void showNearbyFacilities( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );

            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_nearby_layout );

            Intent airport = new Intent( getActivity(), NearbyAirportsActivity.class );
            Bundle extras = new Bundle();
            extras.putParcelable( LocationColumns.LOCATION, mLocation );
            airport.putExtras( extras );
            addClickableRow( layout, "Airports", airport, R.drawable.row_selector_top );
            Intent fss = new Intent( getActivity(), FssCommActivity.class );
            fss.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "FSS outlets", fss, R.drawable.row_selector_middle );
            Intent navaids = new Intent( getActivity(), NearbyNavaidsActivity.class );
            navaids.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Navaids", navaids, R.drawable.row_selector_bottom );
        }

        protected void showOperationsDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_operations_layout );
            String use = apt.getString( apt.getColumnIndex( Airports.FACILITY_USE ) );
            addRow( layout, "Operation", DataUtils.decodeFacilityUse( use ) );
            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            addRow( layout, "FAA code", faaCode );
            String timezoneId = apt.getString( apt.getColumnIndex( Airports.TIMEZONE_ID ) );
            if ( timezoneId.length() > 0 ) {
                TimeZone tz = TimeZone.getTimeZone( timezoneId );
                addRow( layout, "Local time zone", DataUtils.getTimeZoneAsString( tz ) );
            }
            String activation = apt.getString( apt.getColumnIndex( Airports.ACTIVATION_DATE ) );
            if ( activation.length() > 0 ) {
                addRow( layout, "Activation date", activation );
            }
            Cursor twr8 = result[ 8 ];
            if ( twr8.moveToFirst() ) {
                String airspace = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_TYPES ) );
                String value = DataUtils.decodeAirspace( airspace );
                String hours = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_HOURS ) );
                addRow( layout, "Airspace", value, hours );
            }
            String tower = apt.getString( apt.getColumnIndex( Airports.TOWER_ON_SITE ) );
            String towerValue;
            if ( tower.equals( "Y" ) ) {
                Cursor att = result[ 9 ];
                String hours = null;
                if ( att.moveToFirst() && att.getCount() == 1 ) {
                    String schedule = att.getString( att.getColumnIndex(
                            Attendance.ATTENDANCE_SCHEDULE ) );
                    String[] parts = schedule.split( "/" );
                    if ( parts.length == 3 ) {
                        hours = parts[ 2 ];
                        if ( hours.equals( "ALL" ) ) {
                            hours = "24 Hours";
                        }
                    }
                }
                if ( hours == null || hours.length() == 0 ) {
                    towerValue = "Yes";
                } else {
                    towerValue = String.format( "Yes (%s)", hours );
                }
            } else {
                towerValue = String.format( "No" );
            }
            addRow( layout, "Control tower", towerValue );
            String windIndicator = apt.getString( apt.getColumnIndex( Airports.WIND_INDICATOR ) );
            addRow( layout, "Wind indicator", DataUtils.decodeWindIndicator( windIndicator ) );
            String circle = apt.getString( apt.getColumnIndex( Airports.SEGMENTED_CIRCLE ) );
            addRow( layout, "Segmented circle", circle.equals( "Y" )? "Yes" : "No" );
            String beacon = apt.getString( apt.getColumnIndex( Airports.BEACON_COLOR ) );
            addRow( layout, "Beacon", DataUtils.decodeBeacon( beacon ) );
            String lighting;
            lighting = apt.getString( apt.getColumnIndex( Airports.LIGHTING_SCHEDULE ) );
            if ( lighting.length() > 0 ) {
                addRow( layout, "Airport lighting", lighting );
            }
            try {
                lighting = apt.getString( apt.getColumnIndex( Airports.BEACON_LIGHTING_SCHEDULE ) );
                if ( lighting.length() > 0 ) {
                    addRow( layout, "Beacon lighting", lighting );
                }
            } catch ( Exception e ) {
            }
            String landingFee = apt.getString( apt.getColumnIndex( Airports.LANDING_FEE ) );
            addRow( layout, "Landing fee", landingFee.equals( "Y" )? "Yes" : "No" );
            String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
            if ( dir.length() > 0 ) {
                int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
                String year = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_YEAR ) );
                if ( year.length() > 0 ) {
                    addRow( layout, "Magnetic variation", 
                            String.format( "%d\u00B0 %s (%s)", variation, dir, year ) );
                } else {
                    addRow( layout, "Magnetic variation", 
                            String.format( "%d\u00B0 %s", variation, dir ) );
                }
            } else {
                int variation = Math.round( GeoUtils.getMagneticDeclination( mLocation ) );
                dir = ( variation >= 0 )? "W" : "E";
                addRow( layout, "Magnetic variation", 
                        String.format( "%d\u00B0 %s (actual)", Math.abs( variation ), dir ) );
            }
            String intlEntry = apt.getString( apt.getColumnIndex( Airports.INTL_ENTRY_AIRPORT ) );
            if ( intlEntry != null && intlEntry.equals( "Y" ) ) {
                addRow( layout, "International entry", "Yes" );
            }
            String customs = apt.getString( apt.getColumnIndex(
                    Airports.CUSTOMS_LANDING_RIGHTS_AIRPORT ) );
            if ( customs != null && customs.equals( "Y" ) ) {
                addRow( layout, "Customs landing rights", "Yes" );
            }
            String jointUse = apt.getString( apt.getColumnIndex(
                    Airports.CIVIL_MILITARY_JOINT_USE ) );
            if ( jointUse != null && jointUse.equals( "Y" ) ) {
                addRow( layout, "Civil/military joint use", "Yes" );
            }
            String militaryRights = apt.getString( apt.getColumnIndex(
                    Airports.MILITARY_LANDING_RIGHTS ) );
            if ( militaryRights != null && militaryRights.equals( "Y" ) ) {
                addRow( layout, "Military landing rights", "Yes" );
            }
            String medical = apt.getString( apt.getColumnIndex( Airports.MEDICAL_USE ) );
            if ( medical != null && medical.equals( "Y" ) ) {
                addRow( layout, "Medical use", "Yes" );
            }
            String sectional = apt.getString( apt.getColumnIndex( Airports.SECTIONAL_CHART ) );
            if ( sectional.length() > 0 ) {
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
            Intent intent = new Intent( getActivity(), AlmanacActivity.class );
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Sunrise and sunset", intent, R.drawable.row_selector_middle );
            intent = new Intent( getActivity(), AirportNotamActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "NOTAMs", intent, R.drawable.row_selector_bottom );
        }

        protected void showAeroNavDetails( Cursor[] result ) {
            if ( Application.sDonationDone ) {
                Cursor apt = result[ 0 ];
                String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
                Cursor dtpp = result[ 10 ];
                LinearLayout layout = (LinearLayout) findViewById( R.id.detail_ifr_layout );
                if ( dtpp != null ) {
                    if ( dtpp.moveToFirst() ) {
                        Intent intent = new Intent( getActivity(), DtppActivity.class );
                        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                        addClickableRow( layout, "Instrument procedures", intent,
                                R.drawable.row_selector_top );
                    } else {
                        addRow( layout, "No instrument procedures available" );
                    }
                } else {
                    addRow( layout, "d-TPP data not found" );
                }
                Cursor cycle = result[ 11 ];
                if ( cycle != null && cycle.moveToFirst() ) {
                    String afdCycle = cycle.getString( cycle.getColumnIndex( DafdCycle.AFD_CYCLE ) );
                    Cursor dafd = result[ 12 ];
                    if ( dafd.moveToFirst() ) {
                        String pdfName = dafd.getString( dafd.getColumnIndex( Dafd.PDF_NAME ) );
                        View row = addClickableRow( layout, "A/FD airport info", null,
                                R.drawable.row_selector_bottom );
                        row.setTag( R.id.DAFD_CYCLE, afdCycle );
                        row.setTag( R.id.DAFD_PDF_NAME, pdfName );
                        row.setOnClickListener( new OnClickListener() {

                            @Override
                            public void onClick( View v ) {
                                String afdCycle = (String) v.getTag( R.id.DAFD_CYCLE );
                                String pdfName = (String) v.getTag( R.id.DAFD_PDF_NAME );
                                getAfdPage( afdCycle, pdfName );
                            }
                        } );
                    } else {
                        addRow( layout, "A/FD page is not available for this airport" );
                    }
                } else {
                    addRow( layout, "d-A/FD data not found" );
                }
            } else {
                Intent intent = new Intent( getActivity(), DonateActivity.class );
                LinearLayout layout = (LinearLayout) findViewById( R.id.detail_ifr_layout );
                addClickableRow( layout, "Please donate to enable this section",
                        intent, R.drawable.row_selector );
            }
        }

        protected void getAfdPage( String afdCycle, String pdfName ) {
            setRefreshItemVisible( true );
            startRefreshAnimation();
            Intent service = new Intent( getActivity(), DafdService.class );
            service.setAction( DafdService.ACTION_GET_AFD );
            service.putExtra( DafdService.CYCLE_NAME, afdCycle );
            service.putExtra( DafdService.PDF_NAME, pdfName );
            getActivity().startService( service );
        }

        protected void handleDafdBroadcast( Intent intent ) {
            stopRefreshAnimation();
            setRefreshItemVisible( false );
            String action = intent.getAction();
            if ( action.equals( DafdService.ACTION_GET_AFD ) ) {
                String path = intent.getStringExtra( DafdService.PDF_PATH );
                SystemUtils.startPDFViewer( getActivity(), path );
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
            String repair = apt.getString( apt.getColumnIndex( Airports.AIRFRAME_REPAIR_SERVICE ) );
            if ( repair.length() == 0 ) {
                repair = "No";
            }
            addRow( layout, "Airframe repair", repair );
            repair = apt.getString( apt.getColumnIndex( Airports.POWER_PLANT_REPAIR_SERVICE ) );
            if ( repair.length() == 0 ) {
                repair = "No";
            }
            addRow( layout, "Powerplant repair", repair );
            Intent intent = new Intent( getActivity(), ServicesActivity.class );
            intent.putExtra( Airports.SITE_NUMBER,
                    apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) ) );
            addClickableRow( layout, "Other services", intent, R.drawable.row_selector_bottom );
        }

        protected void showOtherDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
            LinearLayout layout = (LinearLayout) findViewById( R.id.detail_other_layout );
            Intent intent = new Intent( getActivity(), OwnershipActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Ownership and contact", intent, R.drawable.row_selector_top );
            intent = new Intent( getActivity(), AircraftOpsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Aircraft operations", intent, R.drawable.row_selector_middle );
            intent = new Intent( getActivity(), RemarksActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Additional remarks", intent, R.drawable.row_selector_middle );
            intent = new Intent( getActivity(), AttendanceActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            addClickableRow( layout, "Attendance", intent, R.drawable.row_selector_bottom );
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
                    sb.append( FormatUtils.formatFreq( Float.valueOf( freq ) ) );
                } catch ( NumberFormatException e ) {
                }
            }
            String value1 = sb.toString();

            sb.setLength( 0 );
            sb.append( type );
            if ( distance >= 2.5 ) {
                sb.append( String.format( ", %.0f NM %s", distance,
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
                makeClickToCall( tv );
            }
        }

        protected void addRunwayRow( LinearLayout layout, Cursor c, int resid ) {
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

            Bundle bundle = new Bundle();
            bundle.putString( Runways.SITE_NUMBER, siteNumber );
            bundle.putString( Runways.RUNWAY_ID, runwayId );
            Intent intent = new Intent( getActivityBase(), RunwaysActivity.class );
            intent.putExtras( bundle );

            addClickableRow( layout, row, intent, resid );
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
