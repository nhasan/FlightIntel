/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.wx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos1;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.wx.Taf.Forecast;
import com.nadmm.airports.wx.Taf.IcingCondition;
import com.nadmm.airports.wx.Taf.TurbulenceCondition;

public class TafFragment extends FragmentBase {

    private final int TAF_RADIUS = 25;

    protected Location mLocation;
    protected BroadcastReceiver mReceiver;
    protected String mStationId;
    protected Forecast mLastForecast;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                showTaf( intent );
            }

        };
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( NoaaService.ACTION_GET_TAF );
        getActivity().registerReceiver( mReceiver, filter );

        Bundle args = getArguments();
        String stationId = args.getString( NoaaService.STATION_ID );
        setBackgroundTask( new TafTask() ).execute( stationId );
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver( mReceiver );
        super.onPause();
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.taf_detail_view, container, false );
        return createContentView( view );
    }

    private final class TafTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String stationId = params[ 0 ];

            Cursor[] cursors = new Cursor[ 2 ];
            SQLiteDatabase db = getDbManager().getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Wxs.TABLE_NAME );
            String selection = Wxs.STATION_ID+"=?";
            Cursor c = builder.query( db, new String[] { "*" }, selection,
                    new String[] { stationId }, null, null, null, null );
            c.moveToFirst();
            String siteTypes = c.getString( c.getColumnIndex( Wxs.STATION_SITE_TYPES ) );
            if ( !siteTypes.contains( "TAF" ) ) {
                // There is no TAF available at this station, search for the nearest
                double lat = c.getDouble( c.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
                double lon = c.getDouble( c.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
                Location location = new Location( "" );
                location.setLatitude( lat );
                location.setLongitude( lon );
                c.close();

                // Get the bounding box first to do a quick query as a first cut
                double[] box = GeoUtils.getBoundingBoxRadians( location, TAF_RADIUS );
                double radLatMin = box[ 0 ];
                double radLatMax = box[ 1 ];
                double radLonMin = box[ 2 ];
                double radLonMax = box[ 3 ];
                // Check if 180th Meridian lies within the bounding Box
                boolean isCrossingMeridian180 = ( radLonMin > radLonMax );

                selection = "("
                    +Wxs.STATION_LATITUDE_DEGREES+">=? AND "+Wxs.STATION_LATITUDE_DEGREES+"<=?"
                    +") AND ("+Wxs.STATION_LONGITUDE_DEGREES+">=? "
                    +(isCrossingMeridian180? "OR " : "AND ")+Wxs.STATION_LONGITUDE_DEGREES+"<=?)";
                String[] selectionArgs = {
                        String.valueOf( Math.toDegrees( radLatMin ) ), 
                        String.valueOf( Math.toDegrees( radLatMax ) ),
                        String.valueOf( Math.toDegrees( radLonMin ) ),
                        String.valueOf( Math.toDegrees( radLonMax ) )
                        };
                c = builder.query( db, new String[] { "*" }, selection, selectionArgs,
                        null, null, null, null );
                stationId = "";
                if ( c.moveToFirst() ) {
                    float distance = Float.MAX_VALUE;
                    do {
                        siteTypes = c.getString( c.getColumnIndex( Wxs.STATION_SITE_TYPES ) );
                        if ( !siteTypes.contains( "TAF" ) ) {
                            continue;
                        }
                        // Get the location of this station
                        float[] results = new float[ 2 ];
                        Location.distanceBetween(
                                location.getLatitude(),
                                location.getLongitude(), 
                                c.getDouble( c.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) ),
                                c.getDouble( c.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) ),
                                results );
                        results[ 0 ] /= GeoUtils.METERS_PER_NAUTICAL_MILE;
                        if ( results[ 0 ] <= TAF_RADIUS && results[ 0 ] < distance ) {
                            stationId = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
                            distance = results[ 0 ];
                        }
                    } while ( c.moveToNext() );
                }
            }
            c.close();

            if ( stationId.length() > 0 ) {
                // We have the station with TAF
                builder = new SQLiteQueryBuilder();
                builder.setTables( Wxs.TABLE_NAME );
                selection = Wxs.STATION_ID+"=?";
                c = builder.query( db, new String[] { "*" }, selection,
                        new String[] { stationId }, null, null, null, null );
                cursors[ 0 ] = c;

                String[] wxColumns = new String[] {
                        Awos1.WX_SENSOR_IDENT,
                        Awos1.WX_SENSOR_TYPE,
                        Awos1.STATION_FREQUENCY,
                        Awos1.SECOND_STATION_FREQUENCY,
                        Awos1.STATION_PHONE_NUMBER,
                        Airports.ASSOC_CITY,
                        Airports.ASSOC_STATE
                };
                builder = new SQLiteQueryBuilder();
                builder.setTables( Airports.TABLE_NAME+" a"
                        +" LEFT JOIN "+Awos1.TABLE_NAME+" w"
                        +" ON a."+Airports.FAA_CODE+" = w."+Awos1.WX_SENSOR_IDENT );
                selection = "a."+Airports.ICAO_CODE+"=?";
                c = builder.query( db, wxColumns, selection, new String[] { stationId },
                        null, null, null, null );
                cursors[ 1 ] = c;
            }

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            Cursor wxs = result[ 0 ];
            if ( wxs == null || !wxs.moveToFirst() ) {
                // No station with TAF was found nearby
                Bundle args = getArguments();
                String stationId = args.getString( NoaaService.STATION_ID );

                View detail = findViewById( R.id.wx_detail_layout );
                detail.setVisibility( View.GONE );
                LinearLayout layout = (LinearLayout) findViewById( R.id.wx_status_layout );
                layout.removeAllViews();
                layout.setVisibility( View.GONE );
                TextView tv =(TextView) findViewById( R.id.status_msg );
                tv.setVisibility( View.VISIBLE );
                tv.setText( String.format( "No wx station with TAF was found near %s"
                        +" within %dNM radius", stationId, TAF_RADIUS ) );
                View title = findViewById( R.id.wx_title_layout );
                title.setVisibility( View.GONE );
                stopRefreshAnimation();
                setContentShown( true );
            } else {
                mLocation = new Location( "" );
                float lat = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
                float lon = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
                mLocation.setLatitude( lat );
                mLocation.setLongitude( lon );
    
                // Show the weather station info
                showWxTitle( result );
    
                // Now request the weather
                mStationId = wxs.getString( wxs.getColumnIndex( Wxs.STATION_ID ) );
                requestTaf( mStationId, false );
            }
            return true;
        }

    }

    protected void requestTaf( String stationId, boolean refresh ) {
        Intent service = new Intent( getActivity(), TafService.class );
        service.setAction( NoaaService.ACTION_GET_TAF );
        service.putExtra( NoaaService.STATION_ID, stationId );
        service.putExtra( NoaaService.FORCE_REFRESH, refresh );
        getActivity().startService( service );
    }

    protected void showTaf( Intent intent ) {
        if ( getActivity() == null ) {
            // Not ready to do this yet
            return;
        }

        Taf taf = (Taf) intent.getSerializableExtra( NoaaService.RESULT );

        View detail = findViewById( R.id.wx_detail_layout );
        LinearLayout layout = (LinearLayout) findViewById( R.id.wx_status_layout );
        TextView tv =(TextView) findViewById( R.id.status_msg );
        layout.removeAllViews();
        if ( !taf.isValid ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );
            tv.setText( "Unable to get TAF for this location." );
            addRow( layout, "This could be due to the following reasons:" );
            addBulletedRow( layout, "Network connection is not available" );
            addBulletedRow( layout, "ADDS does not publish TAF for this station" );
            addBulletedRow( layout, "Station is currently out of service" );
            addBulletedRow( layout, "Station has not updated the TAF for more than 12 hours" );
            detail.setVisibility( View.GONE );
            stopRefreshAnimation();
            setContentShown( true );
            return;
        } else {
            tv.setText( "" );
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
            detail.setVisibility( View.VISIBLE );
        }

        tv = (TextView) findViewById( R.id.wx_age );
        tv.setText( TimeUtils.formatElapsedTime( taf.issueTime ) );

        // Raw Text
        tv = (TextView) findViewById( R.id.wx_raw_taf );
        tv.setText( taf.rawText.replaceAll( "(FM|BECMG|TEMPO)", "\n    $1" ) );

        layout = (LinearLayout) findViewById( R.id.taf_summary_layout );
        layout.removeAllViews();
        String fcstType;
        if ( taf.rawText.startsWith( "TAF AMD " ) ) {
            fcstType = "Amendment";
        } else if ( taf.rawText.startsWith( "TAF COR " ) ) {
            fcstType = "Correction";
        } else {
            fcstType = "Normal";
        }
        addRow( layout, "Forecast type", fcstType );
        addRow( layout, "Issued at", TimeUtils.formatDateTime( getActivity(), taf.issueTime ) );
        addRow( layout, "Valid from", TimeUtils.formatDateTime( getActivity(), taf.validTimeFrom ) );
        addRow( layout, "Valid to", TimeUtils.formatDateTime( getActivity(), taf.validTimeTo ) );
        if ( taf.remarks != null && taf.remarks.length() > 0 && !taf.remarks.equals( "AMD" ) ) {
            addRow( layout, "\u2022 "+taf.remarks );
        }

        LinearLayout topLayout = (LinearLayout) findViewById( R.id.taf_forecasts_layout );
        topLayout.removeAllViews();

        StringBuilder sb = new StringBuilder();
        for ( Forecast forecast : taf.forecasts ) {
            RelativeLayout grp_layout = (RelativeLayout) inflate( R.layout.grouped_detail_item );

            // Keep track of forecast conditions across all change groups
            if ( mLastForecast == null || forecast.changeIndicator == null
                    || forecast.changeIndicator.equals( "FM" ) ) {
                mLastForecast = forecast;
            } else {
                if ( forecast.visibilitySM < Float.MAX_VALUE ) {
                    mLastForecast.visibilitySM = forecast.visibilitySM;
                }
                if ( forecast.skyConditions.size() > 0 ) {
                    mLastForecast.skyConditions = forecast.skyConditions;
                }
            }

            sb.setLength( 0 );
            if ( forecast.changeIndicator != null ) {
                sb.append( forecast.changeIndicator );
                sb.append( " " );
            }
            sb.append( TimeUtils.formatDateRange(
                    getActivity(), forecast.timeFrom, forecast.timeTo ) );
            tv = (TextView) grp_layout.findViewById( R.id.group_name );
            tv.setText( sb.toString() );
            String flightCategory = WxUtils.computeFlightCategory(
                    mLastForecast.skyConditions, mLastForecast.visibilitySM );
            WxUtils.setFlightCategoryDrawable( tv, flightCategory );

            LinearLayout fcst_layout = (LinearLayout) grp_layout.findViewById( R.id.group_details );

            if ( forecast.probability < Integer.MAX_VALUE ) {
                addRow( fcst_layout, "Probability", String.format( "%d%%", forecast.probability ) );
            }

            if ( forecast.changeIndicator != null
                    && forecast.changeIndicator.equals( "BECMG" ) ) {
                addRow( fcst_layout, "Becoming at", TimeUtils.formatDateTime(
                        getActivity(), forecast.timeBecoming ) );
            }

            if ( forecast.windSpeedKnots < Integer.MAX_VALUE ) {
                String wind;
                if ( forecast.windDirDegrees == 0 && forecast.windSpeedKnots == 0 ) {
                    wind = "Calm";
                } else if ( forecast.windDirDegrees == 0 ) {
                    wind = String.format( "Variable at %d knots", forecast.windSpeedKnots );
                } else {
                    wind = String.format( "%s (%03d\u00B0 true) at %d knots",
                            GeoUtils.getCardinalDirection( forecast.windDirDegrees ),
                            forecast.windDirDegrees, forecast.windSpeedKnots );
                }
                String gust = "";
                if ( forecast.windGustKnots < Integer.MAX_VALUE ) {
                    gust = String.format( "Gusting to %d knots", forecast.windGustKnots );
                }
                addRow( fcst_layout, "Winds", wind, gust );
            }

            if ( forecast.visibilitySM < Float.MAX_VALUE ) {
                String value = forecast.visibilitySM > 6? "6+ SM"
                        : FormatUtils.formatVisibility( forecast.visibilitySM );
                addRow( fcst_layout, "Visibility", value );
            }

            if ( forecast.vertVisibilityFeet < Integer.MAX_VALUE ) {
                addRow( fcst_layout, "Visibility",
                        FormatUtils.formatFeetAgl( forecast.vertVisibilityFeet ) );
            }

            for ( WxSymbol wx : forecast.wxList ) {
                addRow( fcst_layout, "Weather", wx.toString() );
            }

            for ( SkyCondition sky : forecast.skyConditions ) {
                addRow( fcst_layout, "Clouds", sky.toString() );
            }

            if ( forecast.windShearSpeedKnots < Integer.MAX_VALUE ) {
                String shear = String.format( "%s (%03d\u00B0 true) at %d knots",
                        GeoUtils.getCardinalDirection( forecast.windShearDirDegrees ),
                        forecast.windShearDirDegrees, forecast.windShearSpeedKnots );
                String height = FormatUtils.formatFeetAgl( forecast.windShearHeightFeetAGL );
                addRow( fcst_layout, "Wind shear", shear, height );
            }

            if ( forecast.altimeterHg < Float.MAX_VALUE ) {
                addRow( fcst_layout, "Altimeter",
                        FormatUtils.formatAltimeter( forecast.altimeterHg ) );
            }

            for ( TurbulenceCondition turbulence : forecast.turbulenceConditions ) {
                String value = WxUtils.decodeTurbulenceIntensity( turbulence.intensity );
                String height = FormatUtils.formatFeetRangeAgl(
                        turbulence.minAltitudeFeetAGL, turbulence.maxAltitudeFeetAGL );
                addRow( fcst_layout, "Turbulence", value, height );
            }

            for ( IcingCondition icing : forecast.icingConditions ) {
                String value = WxUtils.decodeIcingIntensity( icing.intensity );
                String height = FormatUtils.formatFeetRangeAgl(
                        icing.minAltitudeFeetAGL, icing.maxAltitudeFeetAGL );
                addRow( fcst_layout, "Icing", value, height );
            }

            topLayout.addView( grp_layout, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );
        }

        tv = (TextView) findViewById( R.id.wx_fetch_time );
        tv.setText( "Fetched on "+TimeUtils.formatDateTime( getActivity(), taf.fetchTime )  );
        tv.setVisibility( View.VISIBLE );

        stopRefreshAnimation();
        setFragmentContentShown( true );
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        setRefreshItemVisible( true );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_refresh:
            startRefreshAnimation();
            requestTaf( mStationId, true );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
