/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.nadmm.airports.*;
import com.nadmm.airports.DatabaseManager.*;
import com.nadmm.airports.aeronav.DafdService;
import com.nadmm.airports.aeronav.DtppActivity;
import com.nadmm.airports.donate.DonateActivity;
import com.nadmm.airports.notams.AirportNotamActivity;
import com.nadmm.airports.tfr.TfrListActivity;
import com.nadmm.airports.utils.*;
import com.nadmm.airports.wx.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

public final class AirportDetailsFragment extends FragmentBase {

    private final HashSet<TextView> mAwosViews = new HashSet<>();
    private final HashSet<TextView> mRunwayViews = new HashSet<>();
    private final int MAX_WX_STATIONS = 5;

    private BroadcastReceiver mMetarReceiver;
    private BroadcastReceiver mDafdReceiver;
    private IntentFilter mMetarFilter;
    private IntentFilter mDafdFilter;
    private Location mLocation;
    private float mDeclination;
    private String mIcaoCode;
    private int mRadius;
    private int mWxUpdates = 0;
    private String mHome;
    private String mSiteNumber;

    int mScrollPos = -1;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mMetarFilter = new IntentFilter();
        mMetarFilter.addAction( NoaaService.ACTION_GET_METAR );
        mMetarReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
                if ( metar != null ) {
                    showWxInfo( metar );

                    ++mWxUpdates;
                    if ( mWxUpdates == mAwosViews.size() ) {
                        // We have all the wx updates, stop the refresh animation
                        mWxUpdates = 0;
                        stopRefreshAnimation();
                    }
                }
            }

        };

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( getActivity() );
        mRadius = Integer.valueOf( prefs.getString(
                PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );
        mHome = prefs.getString( PreferencesActivity.KEY_HOME_AIRPORT, "" );

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
        return createContentView( view );
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
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.registerReceiver( mMetarReceiver, mMetarFilter );
        bm.registerReceiver( mDafdReceiver, mDafdFilter );
        requestMetars( false );

        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
        bm.unregisterReceiver( mMetarReceiver );
        bm.unregisterReceiver( mDafdReceiver );

        ScrollView view = (ScrollView) findViewById( R.id.scroll_view );
        mScrollPos = view.getScrollY();

        super.onPause();
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        setRefreshItemVisible( !getActivityBase().isNavDrawerOpen() );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_refresh:
            requestMetars( true );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        showAirportTitle( apt );

        showCommunicationsDetails( result );
        showRunwayDetails( result );
        showRemarks( result );
        showAwosDetails( result );
        showHomeDistance( result );
        showNearbyFacilities( result );
        showNotamAndTfr();
        showCharts( result );
        showOperationsDetails( result );
        showAeroNavDetails( result );
        showServicesDetails( result );
        showOtherDetails( result );

        requestMetars( false );

        if ( mScrollPos > -1 ) {
            new Handler().post( new Runnable() {

                @Override
                public void run() {
                    ScrollView view = (ScrollView) findViewById( R.id.scroll_view );
                    view.scrollTo( 0, mScrollPos );
                }
            } );
        }

        setContentShown( true );
    }

    protected void showCommunicationsDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_comm_layout );

        String ctaf = apt.getString( apt.getColumnIndex( Airports.CTAF_FREQ ) );
        addRow( layout, "CTAF", ctaf.length() > 0? ctaf : "None" );
        String unicom = apt.getString( apt.getColumnIndex( Airports.UNICOM_FREQS ) );
        if ( unicom.length() > 0 ) {
            addRow( layout, "Unicom", unicom );
        }

        Cursor twr3 = result[ 4 ];
        if ( twr3.moveToFirst() ) {
            HashMap<String, ArrayList<Float>> freqMap = new HashMap<>();
            do {
                String freqUse = twr3.getString(
                        twr3.getColumnIndex( Tower3.MASTER_AIRPORT_FREQ_USE ) );
                String value = twr3.getString(
                        twr3.getColumnIndex( Tower3.MASTER_AIRPORT_FREQ ) );
                if ( freqUse.contains( "LCL" ) || freqUse.contains( "LC/P" ) ) {
                    addFrequencyToMap( freqMap, "Tower", value );
                } else if ( freqUse.contains( "GND" ) ) {
                    addFrequencyToMap( freqMap, "Ground", value );
                } else if ( freqUse.contains( "ATIS" ) ) {
                    addFrequencyToMap( freqMap, "ATIS", value );
                }
            } while ( twr3.moveToNext() );

            if ( freqMap.size() > 0 ) {
                for ( String key : freqMap.keySet() ) {
                    ArrayList<Float> freqs = freqMap.get( key );
                    // Do not show here if multiple frequencies are listed
                    if ( freqs.size() == 1 ) {
                        addRow( layout, key, FormatUtils.formatFreq( freqs.get( 0 ) ) );
                    }
                }
            }
        }

        addClickableRow( layout, "More...", CommunicationsFragment.class, getArguments() );
    }

    protected void addFrequencyToMap( HashMap<String, ArrayList<Float>> freqMap,
            String key, String value ) {
        ArrayList<Float> freqs = freqMap.get( key );
        if ( freqs == null ) {
            freqs = new ArrayList<>();
        }
        int i = 0;
        while ( i < value.length() ) {
            char c = value.charAt( i );
            if ( ( c >= '0' && c <= '9' ) || c == '.' ) {
                ++i;
                continue;
            }
            value = value.substring( 0, i );
            break;
        }
        Float freq = Float.valueOf( value );
        if ( freq <= 136 && !freqs.contains( freq ) ) {
            // Add VHF frequencies only
            freqs.add( freq );
        }
        freqMap.put( key, freqs );
    }

    protected void showRunwayDetails( Cursor[] result ) {
        LinearLayout rwyLayout = (LinearLayout) findViewById( R.id.detail_rwy_layout );
        LinearLayout heliLayout = (LinearLayout) findViewById( R.id.detail_heli_layout );
        TextView tv;
        int rwyNum = 0;
        int heliNum = 0;

        Cursor rwy = result[ 1 ];
        if ( rwy.moveToFirst() ) {
            do {
                String rwyId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
                if ( rwyId.startsWith( "H" ) ) {
                    // This is a helipad
                    addRunwayRow( heliLayout, rwy );
                    ++heliNum;
                } else {
                    // This is a runway
                    addRunwayRow( rwyLayout, rwy );
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
        Cursor twr7 = result[ 5 ];
        if ( twr1.moveToFirst() ) {
            String facilityType = twr1.getString( twr1.getColumnIndex( Tower1.FACILITY_TYPE ) );
            if ( facilityType.equals( "NON-ATCT" ) && twr7.getCount() == 0 ) {
                // Show remarks, if any, since there are no frequencies listed
                Cursor twr6 = result[ 6 ];
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
        Cursor awos1 = result[ 7 ];

        if ( awos1.moveToFirst() ) {
            do {
                if ( awos1.getPosition() == MAX_WX_STATIONS ) {
                    break;
                }
                String icaoCode = awos1.getString(
                        awos1.getColumnIndex( Wxs.STATION_ID ) );
                String sensorId = awos1.getString(
                        awos1.getColumnIndex( Awos1.WX_SENSOR_IDENT ) );
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

                final Bundle extras = new Bundle();
                extras.putString( NoaaService.STATION_ID, icaoCode );
                extras.putString( Awos1.WX_SENSOR_IDENT, sensorId );

                Runnable runnable = new Runnable() {

                    @Override
                    public void run() {
                        cacheMetars();
                        Intent intent = new Intent( getActivity(), WxDetailActivity.class );
                        intent.putExtras( extras );
                        startActivity( intent );
                    }
                };
                addAwosRow( layout, icaoCode, name, type, freq, phone, distance,
                        bearing, runnable );
            } while ( awos1.moveToNext() );

            if ( !awos1.isAfterLast() ) {
                Intent intent = new Intent( getActivity(), NearbyWxActivity.class );
                intent.putExtra( LocationColumns.LOCATION, mLocation );
                intent.putExtra( LocationColumns.RADIUS, mRadius );
                addClickableRow( layout, "More...", intent );
            }
        } else {
            label.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showHomeDistance( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_home_layout );
        Cursor home = result[ 14 ];
        if ( home == null ) {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    Intent prefs = new Intent( getActivity(), PreferencesActivity.class );
                    startActivity( prefs );
                    getActivity().finish();
                }
            };
            addClickableRow( layout, "Tap here to set home airport", runnable );
        } else if ( home.moveToFirst() ) {
            String siteNumber = home.getString( home.getColumnIndex( Airports.SITE_NUMBER ) );
            if ( siteNumber.equals( mSiteNumber ) ) {
                addRow( layout, mHome+" is your home airport" );
            } else {
                double lat = home.getDouble( home.getColumnIndex(
                        Airports.REF_LATTITUDE_DEGREES ) );
                double lon = home.getDouble( home.getColumnIndex(
                        Airports.REF_LONGITUDE_DEGREES ) );
                float[] results = new float[ 3 ];
                Location.distanceBetween( lat, lon,
                        mLocation.getLatitude(), mLocation.getLongitude(), results );
                float distance = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
                if ( distance >= 100 ) {
                    distance = Math.round( distance );
                }
                int initialBearing = Math.round( ( results[ 1 ]+mDeclination+360 )%360 );
                int finalBearing = Math.round( ( results[ 2 ]+mDeclination+360 )%360 );

                addRow( layout, "Distance from "+mHome, String.format( "%s %s",
                        FormatUtils.formatNauticalMiles( distance ),
                        GeoUtils.getCardinalDirection( initialBearing ) ) );
                addRow( layout, "Initial bearing",
                        FormatUtils.formatDegrees( initialBearing )+" M" );
                if ( Math.abs( finalBearing-initialBearing ) >= 10 ) {
                    addRow( layout, "Final bearing",
                            FormatUtils.formatDegrees( finalBearing )+" M" );
                }
            }
        } else {
            addRow( layout, "Home airport '"+mHome+"' not found" );
        }
    }

    protected void showNearbyFacilities( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_nearby_layout );

        Bundle args = new Bundle();
        args.putParcelable( LocationColumns.LOCATION, mLocation );
        args.putInt( LocationColumns.RADIUS, mRadius );
        args.putString( Airports.ICAO_CODE, mIcaoCode );
        addClickableRow( layout, "Airports", NearbyAirportsFragment.class, args );
        addClickableRow( layout, "FSS outlets", FssCommFragment.class, getArguments() );
        addClickableRow( layout, "Navaids", NearbyNavaidsFragment.class, getArguments() );
    }

    private void showNotamAndTfr() {
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_notam_faa_layout );
        Intent intent = new Intent( getActivity(), AirportNotamActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, mSiteNumber );
        addClickableRow( layout, "View NOTAMs", intent );
        intent = new Intent( getActivity(), TfrListActivity.class );
        addClickableRow( layout, "View TFRs", intent );
    }

    private void showCharts( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_charts_layout );
        String sectional = apt.getString( apt.getColumnIndex( Airports.SECTIONAL_CHART ) );
        if ( sectional == null || sectional.length() == 0 ) {
            sectional = "N/A";
        }
        String lat = apt.getString( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
        String lon = apt.getString( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
        if ( lat.length() > 0 && lon.length() > 0 ) {
            // Link to the sectional at VFRMAP if location is available
            Uri uri = Uri.parse( String.format(
                    "http://vfrmap.com/?type=vfrc&lat=%s&lon=%s&zoom=12", lat, lon ) );
            Intent intent = new Intent( Intent.ACTION_VIEW, uri );
            addClickableRow( layout, "Sectional VFR", sectional, intent );
            uri = Uri.parse( String.format(
                    "http://vfrmap.com/?type=ifrlc&lat=%s&lon=%s&zoom=10", lat, lon ) );
            intent = new Intent( Intent.ACTION_VIEW, uri );
            addClickableRow( layout, "Low-altitude IFR", intent );
            uri = Uri.parse( String.format(
                    "http://vfrmap.com/?type=ehc&lat=%s&lon=%s&zoom=10", lat, lon ) );
            intent = new Intent( Intent.ACTION_VIEW, uri );
            addClickableRow( layout, "High-altitude IFR", intent );
            View row = addRow( layout, "Charts require internet connection" );
            TextView tv = (TextView) row.findViewById( R.id.item_label );
            tv.setTextAppearance( getActivity(), R.style.TextSmall_Light );
        } else {
            addRow( layout, "Sectional chart", sectional );
        }
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
            addRow( layout, "Local time zone", TimeUtils.getTimeZoneAsString( tz ) );
        }
        String activation = apt.getString( apt.getColumnIndex( Airports.ACTIVATION_DATE ) );
        if ( activation.length() > 0 ) {
            addRow( layout, "Activation date", activation );
        }
        Cursor twr8 = result[ 9 ];
        if ( twr8.moveToFirst() ) {
            String airspace = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_TYPES ) );
            String value = DataUtils.decodeAirspace( airspace );
            String hours = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_HOURS ) );
            addRow( layout, "Airspace", value, hours );
        }
        String tower = apt.getString( apt.getColumnIndex( Airports.TOWER_ON_SITE ) );
        String towerValue;
        if ( tower.equals( "Y" ) ) {
            Cursor att = result[ 10 ];
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
            towerValue = "No";
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
        } catch ( Exception ignored ) {
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
    }

    protected void showAeroNavDetails( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_aeronav_layout );
        if ( Application.sDonationDone ) {
            Cursor apt = result[ 0 ];
            String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
            Cursor cycle = result[ 12 ];
            if ( cycle != null && cycle.moveToFirst() ) {
                String afdCycle = cycle.getString( cycle.getColumnIndex( DafdCycle.AFD_CYCLE ) );
                Cursor dafd = result[ 13 ];
                if ( dafd.moveToFirst() ) {
                    String pdfName = dafd.getString( dafd.getColumnIndex( Dafd.PDF_NAME ) );
                    View row = addClickableRow( layout, "A/FD page", null );
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
            Cursor dtpp = result[ 11 ];
            if ( dtpp != null ) {
                if ( dtpp.moveToFirst() ) {
                    Intent intent = new Intent( getActivity(), DtppActivity.class );
                    intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                    addClickableRow( layout, "Instrument procedures", intent );
                } else {
                    addRow( layout, "No instrument procedures available" );
                }
            } else {
                addRow( layout, "d-TPP data not found" );
            }
        } else {
            Intent intent = new Intent( getActivity(), DonateActivity.class );
            addClickableRow( layout, "Please donate to enable this section", intent );
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
            if ( path != null ) {
                SystemUtils.startPDFViewer( getActivity(), path );
            }
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
        addClickableRow( layout, "Other services", ServicesFragment.class, getArguments() );
    }

    protected void showOtherDetails( Cursor[] result ) {
        Bundle args = getArguments();
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_other_layout );
        addClickableRow( layout, "Ownership and contact", OwnershipFragment.class, args );
        addClickableRow( layout, "Aircraft operations", AircraftOpsFragment.class, args );
        addClickableRow( layout, "Additional remarks", RemarksFragment.class, args );
        addClickableRow( layout, "Attendance", AttendanceFragment.class, args );
        addClickableRow( layout, "Sunrise and sunset", AlmanacFragment.class, args );
    }

    protected void addAwosRow( LinearLayout layout, String id, String name, String type,
            String freq, String phone, float distance, float bearing,
            final Runnable runnable ) {
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
            } catch ( NumberFormatException ignored ) {
            }
        }
        String value1 = sb.toString();

        sb.setLength( 0 );
        sb.append( type );
        if ( mIcaoCode.equals( id ) ) {
            sb.append( ", On-site" );
        } else {
            sb.append( String.format( ", %.1f NM %s", distance,
                    GeoUtils.getCardinalDirection( bearing ) ) );
        }
        String label2 = sb.toString();

        View row = addClickableRow( layout, label1, value1, label2, phone, runnable );

        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setTag( id );
        mAwosViews.add( tv );
        // Make phone number clickable
        tv = (TextView) row.findViewById( R.id.item_extra_value );
        if ( tv.getText().length() > 0 ) {
            makeClickToCall( tv );
        }
    }

    protected void addRunwayRow( LinearLayout layout, Cursor c ) {
        String siteNumber = c.getString( c.getColumnIndex( Runways.SITE_NUMBER ) );
        String runwayId = c.getString( c.getColumnIndex( Runways.RUNWAY_ID ) );
        int length = c.getInt( c.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        int width = c.getInt( c.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        String surfaceType = c.getString( c.getColumnIndex( Runways.SURFACE_TYPE ) );
        String baseId = c.getString( c.getColumnIndex( Runways.BASE_END_ID ) );
        String reciprocalId = c.getString( c.getColumnIndex( Runways.RECIPROCAL_END_ID ) );
        String baseRP = c.getString( c.getColumnIndex( Runways.BASE_END_RIGHT_TRAFFIC ) );
        String reciprocalRP = c.getString(
                c.getColumnIndex( Runways.RECIPROCAL_END_RIGHT_TRAFFIC ) );

        String rp = null;
        if ( baseRP.equals( "Y" ) && reciprocalRP.equals( "Y" ) ) {
            rp = " (RP)";
        } else if ( baseRP.equals( "Y" ) ) {
            rp = " (RP "+baseId+")";
        } else if ( reciprocalRP.equals( "Y" ) ) {
            rp = " (RP "+reciprocalId+")";
        }

        int heading = c.getInt( c.getColumnIndex( Runways.BASE_END_HEADING ) );
        if ( heading > 0 ) {
            heading = (int) GeoUtils.applyDeclination( heading, mDeclination );
        } else {
            // Actual heading is not available, try to deduce it from runway id
            heading = DataUtils.getRunwayHeading( runwayId );
        }

        RelativeLayout row = (RelativeLayout) inflate( R.layout.runway_detail_item );

        TextView tv = (TextView) row.findViewById( R.id.runway_id );
        tv.setText( runwayId );
        UiUtils.setRunwayDrawable( getActivity(), tv, runwayId, length, heading );

        if ( rp != null ) {
            tv = (TextView) row.findViewById( R.id.runway_rp );
            tv.setText( rp );
            tv.setVisibility( View.VISIBLE );
        }

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

        Bundle args = new Bundle();
        args.putString( Runways.SITE_NUMBER, siteNumber );
        args.putString( Runways.RUNWAY_ID, runwayId );
        addClickableRow( layout, row, RunwaysFragment.class, args );
    }

    protected void cacheMetars() {
        requestMetars( NoaaService.ACTION_CACHE_METAR, false, false );
    }

    protected void requestMetars( boolean force ) {
        boolean cacheOnly = !NetworkUtils.canDownloadData( getActivity() );
        requestMetars( NoaaService.ACTION_GET_METAR, force, cacheOnly );
    }

    protected void requestMetars( String action, boolean force, boolean cacheOnly ) {
        if ( mAwosViews.size() == 0 ) {
            return;
        }

        // Now get the METAR if already in the cache
        if ( force || !cacheOnly ) {
            getActivityBase().startRefreshAnimation();
        }

        ArrayList<String> stationIds = new ArrayList<>();
        for ( TextView tv : mAwosViews ) {
            String stationId = (String) tv.getTag();
            stationIds.add( stationId );
        }
        Intent service = new Intent( getActivity(), MetarService.class );
        service.setAction( action );
        service.putExtra( NoaaService.STATION_IDS, stationIds );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_TEXT );
        if ( force ) {
            service.putExtra( NoaaService.FORCE_REFRESH, true );
        } else if ( cacheOnly ) {
            service.putExtra( NoaaService.CACHE_ONLY, true );
        }
        getActivity().startService( service );
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
            String side = ( crossWind >= 0 )? "right" : "left";
            crossWind = Math.abs( crossWind );
            StringBuilder windInfo = new StringBuilder();
            if ( crossWind > 0 ) {
                windInfo.append( String.format( "%d %s %s x-wind",
                        crossWind, crossWind > 1? "knots" : "knot", side ) );
            } else {
                windInfo.append( "no x-wind" );
            }
            if ( metar.windGustKnots < Integer.MAX_VALUE ) {
                double gustFactor = (metar.windGustKnots-metar.windSpeedKnots)/2;
                windInfo.append( String.format( ", %d knots gust factor",
                        Math.round( gustFactor ) ) );
            }
            tv.setText( String.format( "Rwy %s: %s", id, windInfo.toString() ) );
            tv.setVisibility( View.VISIBLE );
        }
    }

    private final class AirportDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            mSiteNumber = params[ 0 ];

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 15 ];

            Cursor apt = getAirportDetails( mSiteNumber );
            cursors[ 0 ] = apt;

            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
            int elev_msl = apt.getInt( apt.getColumnIndex( Airports.ELEVATION_MSL ) );
            mIcaoCode = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( mIcaoCode == null || mIcaoCode.length() == 0 ) {
                mIcaoCode = "K"+apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            }

            mLocation = new Location( "" );
            mLocation.setLatitude( lat );
            mLocation.setLongitude( lon );
            mLocation.setAltitude( elev_msl );

            mDeclination = GeoUtils.getMagneticDeclination( mLocation );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { Runways.SITE_NUMBER, Runways.RUNWAY_ID,
                    Runways.RUNWAY_LENGTH, Runways.RUNWAY_WIDTH, Runways.SURFACE_TYPE,
                    Runways.BASE_END_HEADING, Runways.BASE_END_ID, Runways.RECIPROCAL_END_ID,
                    Runways.BASE_END_RIGHT_TRAFFIC, Runways.RECIPROCAL_END_RIGHT_TRAFFIC },
                    Runways.SITE_NUMBER+"=? AND "+Runways.RUNWAY_LENGTH+" > 0",
                    new String[] { mSiteNumber }, null, null, null, null );
            cursors[ 1 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            c = builder.query( db, new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=?"
                    +"AND "+Remarks.REMARK_NAME+" in ('E147', 'A3', 'A24', 'A70', 'A75', 'A82')",
                    new String[] { mSiteNumber }, null, null, null, null );
            cursors[ 2 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower1.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower1.SITE_NUMBER+"=?",
                    new String[] { mSiteNumber }, null, null, null, null );
            cursors[ 3 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower3.FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, null, null );
            cursors[ 4 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower7.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower7.SATELLITE_AIRPORT_SITE_NUMBER+"=?",
                    new String[] { mSiteNumber }, null, null, null, null );
            cursors[ 5 ] = c;

            if ( !c.moveToFirst() ) {
                builder = new SQLiteQueryBuilder();
                builder.setTables( Tower6.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Tower6.FACILITY_ID+"=?",
                        new String[] { faaCode }, null, null, Tower6.ELEMENT_NUMBER, null );
                cursors[ 6 ] = c;
            }

            cursors[ 7 ] = new NearbyWxCursor( db, mLocation, mRadius );

            String faa_code = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Aff3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" }, Aff3.IFR_FACILITY_ID+"=?",
                    new String[] { faa_code }, null, null, null, null );
            cursors[ 8 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower8.TABLE_NAME );
            c = builder.query( db, new String[] { "*" }, Tower8.FACILITY_ID+"=? ",
                    new String[] { faaCode }, null, null, null, null );
            cursors[ 9 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Attendance.TABLE_NAME );
            c = builder.query( db,
                    new String[] { Attendance.ATTENDANCE_SCHEDULE },
                    Attendance.SITE_NUMBER+"=?", new String[] { mSiteNumber },
                    null, null, Attendance.SEQUENCE_NUMBER, null );
            cursors[ 10 ] = c;

            db = getDatabase( DatabaseManager.DB_DTPP );
            if ( db != null ) {
                builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                c = builder.query( db, new String[] { "*" }, Dtpp.FAA_CODE+"=? ",
                        new String[] { faaCode }, null, null, null, null );
                cursors[ 11 ] = c;
            }

            db = getDatabase( DatabaseManager.DB_DAFD );
            if ( db != null ) {
                builder = new SQLiteQueryBuilder();
                builder.setTables( DafdCycle.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        null, null, null, null, null, null );
                cursors[ 12 ] = c;

                builder = new SQLiteQueryBuilder();
                builder.setTables( Dafd.TABLE_NAME );
                c = builder.query( db, new String[] { "*" }, Dafd.FAA_CODE+"=? ",
                        new String[] { faaCode }, null, null, null, null );
                cursors[ 13 ] = c;
            }

            if ( mHome.length() > 0 ) {
                db = getDatabase( DatabaseManager.DB_FADDS );
                builder = new SQLiteQueryBuilder();
                builder.setTables( Airports.TABLE_NAME );
                c = builder.query( db,
                        new String[] {
                            Airports.SITE_NUMBER,
                            Airports.REF_LATTITUDE_DEGREES,
                            Airports.REF_LONGITUDE_DEGREES
                        },
                        Airports.FAA_CODE+"=? OR "+Airports.ICAO_CODE+"=?",
                        new String[] { mHome, mHome }, null, null, null, null );
                cursors[ 14 ] = c;
            }

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

}
