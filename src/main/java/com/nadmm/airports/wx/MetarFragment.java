/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Awos1;
import com.nadmm.airports.data.DatabaseManager.Awos2;
import com.nadmm.airports.data.DatabaseManager.Wxs;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.wx.Metar.Flags;

import java.text.NumberFormat;
import java.util.ArrayList;

public class MetarFragment extends WxFragmentBase {

    private final String mAction = NoaaService.ACTION_GET_METAR;

    private Location mLocation;
    private ArrayList<String> mRemarks;

    private final int METAR_HOURS_BEFORE = 3;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mRemarks = new ArrayList<>();
        setupBroadcastFilter( mAction );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.metar_detail_view, container, false );
        Button btnGraphic = (Button) view.findViewById( R.id.btnViewGraphic );
        btnGraphic.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                Intent intent = new Intent( getActivity(), MetarMapActivity.class );
                startActivity( intent );
            }
        } );
        return createContentView( view );
    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle args = getArguments();
        String stationId = args.getString( NoaaService.STATION_ID );
        setBackgroundTask( new MetarTask() ).execute( stationId );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        getActivityBase().onFragmentStarted( this );
    }

    @Override
    protected void handleBroadcast( Intent intent ) {
        if ( mLocation == null ) {
            // This was probably intended for wx list view
            return;
        }

        String type = intent.getStringExtra( NoaaService.TYPE );
        if ( type.equals( NoaaService.TYPE_TEXT ) ) {
            showMetar( intent );
            setRefreshing( false );
        }
    }

    @Override
    public boolean isRefreshable() {
        return true;
    }

    @Override
    public void requestDataRefresh() {
        requestMetar( true );
    }

    private final class MetarTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String stationId = params[ 0 ];

            Cursor[] cursors = new Cursor[ 3 ];

            SQLiteDatabase db = getActivityBase().getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Wxs.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "*" }, Wxs.STATION_ID+"=?",
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
            String selection = "a."+Airports.ICAO_CODE+"=? AND w."+Awos1.COMMISSIONING_STATUS+"='Y'";
            c = builder.query( db, wxColumns, selection, new String[] { stationId },
                    null, null, null );
            cursors[ 1 ] = c;

            if ( c.moveToFirst() ) {
                String sensorId = c.getString( c.getColumnIndex( Awos1.WX_SENSOR_IDENT ) );
                String sensorType = c.getString( c.getColumnIndex( Awos1.WX_SENSOR_TYPE ) );
                builder = new SQLiteQueryBuilder();
                builder.setTables( Awos2.TABLE_NAME );
                selection = String.format( "%s=? AND %s=?",
                        Awos2.WX_SENSOR_IDENT, Awos2.WX_SENSOR_TYPE );
                c = builder.query( db, new String[] { Awos2.WX_STATION_REMARKS },
                        selection, new String[] { sensorId, sensorType }, null, null, null );
                cursors[ 2 ] = c;
            }

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            Cursor wxs = result[ 0 ];
            if ( wxs.moveToFirst() ) {
                mLocation = new Location( "" );
                float lat = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
                float lon = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
                mLocation.setLatitude( lat );
                mLocation.setLongitude( lon );

                Cursor rmk = result[ 2 ];
                if ( rmk != null && rmk.moveToFirst() ) {
                    mRemarks.clear();
                    do {
                        String remark = rmk.getString(
                                rmk.getColumnIndex( Awos2.WX_STATION_REMARKS ) );
                        mRemarks.add( remark );
                    } while ( rmk.moveToNext() );
                }

                showWxTitle( result );
                requestMetar( false );
            } else {
                UiUtils.showToast( getActivity().getApplicationContext(),
                        "Unable to get weather station info" );
                getActivity().finish();
            }
            return true;
        }

    }

    protected void requestMetar( boolean refresh ) {
        if ( getActivity() == null ) {
            // Not ready to do this yet
            return;
        }

        Bundle args = getArguments();
        String stationId = args.getString( NoaaService.STATION_ID );
        Intent service = new Intent( getActivity(), MetarService.class );
        service.setAction( mAction );
        ArrayList<String> stationIds = new ArrayList<>();
        stationIds.add( stationId );
        service.putExtra( NoaaService.STATION_IDS, stationIds );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_TEXT );
        service.putExtra( NoaaService.HOURS_BEFORE, METAR_HOURS_BEFORE );
        service.putExtra( NoaaService.FORCE_REFRESH, refresh );
        getActivity().startService( service );
    }

    protected void showMetar( Intent intent ) {
        if ( getActivity() == null ) {
            // Not ready to do this yet
            return;
        }

        Metar metar = (Metar) intent.getSerializableExtra( NoaaService.RESULT );
        if ( metar == null ) {
            return;
        }

        View detail = findViewById( R.id.wx_detail_layout );
        LinearLayout layout = (LinearLayout) findViewById( R.id.wx_status_layout );
        layout.removeAllViews();
        TextView tv =(TextView) findViewById( R.id.status_msg );
        if ( !metar.isValid ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );
            tv.setText( "Unable to get METAR for this location" );
            addRow( layout, "This could be due to the following reasons:" );
            addBulletedRow( layout, "Network connection is not available" );
            addBulletedRow( layout, "ADDS does not publish METAR for this station" );
            addBulletedRow( layout, "Station is currently out of service" );
            addBulletedRow( layout, "Station has not updated the METAR for more than 3 hours" );
            detail.setVisibility( View.GONE );
            setFragmentContentShown( true );
            return;
        } else {
            tv.setText( "" );
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
            detail.setVisibility( View.VISIBLE );
        }

        tv = (TextView) findViewById( R.id.wx_station_info2 );
        WxUtils.setFlightCategoryDrawable( tv, metar.flightCategory );

        tv = (TextView) findViewById( R.id.wx_age );
        tv.setText( TimeUtils.formatElapsedTime( metar.observationTime ) );

        // Raw Text
        tv = (TextView) findViewById( R.id.wx_raw_metar );
        tv.setText( metar.rawText );

        // Winds
        tv = (TextView) findViewById( R.id.wx_wind_label );
        layout = (LinearLayout) findViewById( R.id.wx_wind_layout );
        layout.removeAllViews();
        int visibility = View.GONE;
        if ( metar.windSpeedKnots < Integer.MAX_VALUE ) {
            showWindInfo( layout, metar );
            visibility = View.VISIBLE;
        }
        tv.setVisibility( visibility );
        layout.setVisibility( visibility );

        // Visibility
        tv = (TextView) findViewById( R.id.wx_vis_label );
        layout = (LinearLayout) findViewById( R.id.wx_vis_layout );
        layout.removeAllViews();
        visibility = View.GONE;
        if ( metar.visibilitySM < Float.MAX_VALUE ) {
            if ( metar.flags.contains( Flags.AutoReport ) && metar.visibilitySM == 10 ) {
                addRow( layout, "10+ statute miles horizontal" );
            } else {
                NumberFormat decimal2 = NumberFormat.getNumberInstance();
                decimal2.setMaximumFractionDigits( 2 );
                decimal2.setMinimumFractionDigits( 0 );
                addRow( layout, String.format( "%s statute miles horizontal",
                        FormatUtils.formatNumber( metar.visibilitySM ) ) );
            }
            if ( metar.vertVisibilityFeet < Integer.MAX_VALUE ) {
                addRow( layout, String.format( "%s vertical",
                        FormatUtils.formatFeetAgl( metar.vertVisibilityFeet ) ) );
            }
            visibility = View.VISIBLE;
        }
        tv.setVisibility( visibility );
        layout.setVisibility( visibility );

        // Weather
        layout = (LinearLayout) findViewById( R.id.wx_weather_layout );
        layout.removeAllViews();
        for ( WxSymbol wx : metar.wxList ) {
            addWeatherRow( layout, wx, metar.flightCategory );
        }

        // Sky Conditions
        tv = (TextView) findViewById( R.id.wx_sky_cond_label );
        layout = (LinearLayout) findViewById( R.id.wx_sky_cond_layout );
        layout.removeAllViews();
        visibility = View.GONE;
        if ( !metar.skyConditions.isEmpty() ) {
            for ( SkyCondition sky : metar.skyConditions ) {
                addSkyConditionRow( layout, sky, metar.flightCategory );
            }
            visibility = View.VISIBLE;
        }
        tv.setVisibility( visibility );
        layout.setVisibility( visibility );

        // Temperature
        tv = (TextView) findViewById( R.id.wx_temp_label );
        layout = (LinearLayout) findViewById( R.id.wx_temp_layout );
        layout.removeAllViews();
        visibility = View.GONE;
        if ( metar.tempCelsius < Float.MAX_VALUE && metar.dewpointCelsius < Float.MAX_VALUE ) {
            addRow( layout, "Temperature",
                    FormatUtils.formatTemperature( metar.tempCelsius ) );
            if ( metar.dewpointCelsius < Float.MAX_VALUE ) {
                addRow( layout, "Dew point",
                        FormatUtils.formatTemperature( metar.dewpointCelsius ) );
                addRow( layout,"Relative humidity", String.format( "%.0f%%",
                        WxUtils.getRelativeHumidity( metar ) ) );

                long denAlt = WxUtils.getDensityAltitude( metar );
                addRow( layout, "Density altitude", FormatUtils.formatFeet( denAlt ) );
            } else {
                addRow( layout, "Dew point", "n/a" );
            }

            if ( metar.maxTemp6HrCentigrade < Float.MAX_VALUE ) {
                addRow( layout, "6-hour maximum",
                        FormatUtils.formatTemperature( metar.maxTemp6HrCentigrade ) );
            }
            if ( metar.minTemp6HrCentigrade < Float.MAX_VALUE ) {
                addRow( layout, "6-hour minimum",
                        FormatUtils.formatTemperature( metar.minTemp6HrCentigrade ) );
            }
            if ( metar.maxTemp24HrCentigrade < Float.MAX_VALUE ) {
                addRow( layout, "24-hour maximum",
                        FormatUtils.formatTemperature( metar.maxTemp24HrCentigrade ) );
            }
            if ( metar.minTemp24HrCentigrade < Float.MAX_VALUE ) {
                addRow( layout, "24-hour minimum",
                        FormatUtils.formatTemperature( metar.minTemp24HrCentigrade ) );
            }
            visibility = View.VISIBLE;
        }
        tv.setVisibility( visibility );
        layout.setVisibility( visibility );

        // Pressure
        tv = (TextView) findViewById( R.id.wx_pressure_label );
        layout = (LinearLayout) findViewById( R.id.wx_pressure_layout );
        layout.removeAllViews();
        visibility = View.GONE;
        if ( metar.altimeterHg < Float.MAX_VALUE ) {
            addRow( layout, "Altimeter",
                    FormatUtils.formatAltimeter( metar.altimeterHg ) );
            if ( metar.seaLevelPressureMb < Float.MAX_VALUE ) {
                addRow( layout, "Sea level pressure", String.format( "%s mb",
                        FormatUtils.formatNumber( metar.seaLevelPressureMb ) ) );
            }
            long presAlt = WxUtils.getPressureAltitude( metar );
            addRow( layout, "Pressure altitude", FormatUtils.formatFeet( presAlt ) );
            if ( metar.pressureTend3HrMb < Float.MAX_VALUE ) {
                addRow( layout, "3-hour tendency", String.format( "%+.2f mb",
                        metar.pressureTend3HrMb ) );
            }
            if ( metar.presfr ) {
                addRow( layout, "Pressure falling rapidly" );
            } if ( metar.presrr ) {
                addRow( layout, "Pressure rising rapidly" );
            }
            visibility = View.VISIBLE;
        }
        tv.setVisibility( visibility );
        layout.setVisibility( visibility );

        // Precipitation
        tv = (TextView) findViewById( R.id.wx_precip_label );
        layout = (LinearLayout) findViewById( R.id.wx_precip_layout );
        layout.removeAllViews();
        if ( metar.precipInches < Float.MAX_VALUE ) {
            addRow( layout, "1-hour precipitation",
                    String.format( "%.2f\"", metar.precipInches ) );
        }
        if ( metar.precip3HrInches < Float.MAX_VALUE ) {
            addRow( layout, "3-hour precipitation",
                    String.format( "%.2f\"", metar.precip3HrInches ) );
        }
        if ( metar.precip6HrInches < Float.MAX_VALUE ) {
            addRow( layout, "6-hour precipitation",
                    String.format( "%.2f\"", metar.precip6HrInches ) );
        }
        if ( metar.precip24HrInches < Float.MAX_VALUE ) {
            addRow( layout, "24-hour precipitation",
                    String.format( "%.2f\"", metar.precip24HrInches ) );
        }
        if ( metar.snowInches < Float.MAX_VALUE ) {
            addRow( layout, "Snow depth", String.format( "%.0f\"", metar.snowInches ) );
        }
        if ( metar.snincr ) {
            addRow( layout, "Snow is increasing rapidly" );
        }
        visibility = layout.getChildCount() > 0? View.VISIBLE : View.GONE;
        tv.setVisibility( visibility );
        layout.setVisibility( visibility );

        // Remarks
        tv = (TextView) findViewById( R.id.wx_remarks_label );
        layout = (LinearLayout) findViewById( R.id.wx_remarks_layout );
        layout.removeAllViews();
        for ( Flags flag : metar.flags ) {
            addBulletedRow( layout, flag.toString() );
        }
        for ( String remark : mRemarks ) {
            addBulletedRow( layout, remark );
        }
        visibility = layout.getChildCount() > 0? View.VISIBLE : View.GONE;
        tv.setVisibility( visibility );
        layout.setVisibility( visibility );

        // Fetch time
        tv = (TextView) findViewById( R.id.wx_fetch_time );
        tv.setText( "Fetched on " + TimeUtils.formatDateTime( getActivity(), metar.fetchTime ) );
        tv.setVisibility( View.VISIBLE );

        setFragmentContentShown( true );
    }

    protected String getWindsDescription( Metar metar ) {
        StringBuilder s = new StringBuilder();
        if ( metar.windDirDegrees == 0 && metar.windSpeedKnots == 0 ) {
            s.append( "Winds are calm" );
        } else if ( metar.windDirDegrees == 0 ) {
            s.append( String.format( "Winds variable at %d knots", metar.windSpeedKnots ) );
        } else {
            s.append( String.format( "From %s (%s true) at %d knots",
                    GeoUtils.getCardinalDirection( metar.windDirDegrees ),
                    FormatUtils.formatDegrees( metar.windDirDegrees ), metar.windSpeedKnots ) );
            if ( metar.windGustKnots < Integer.MAX_VALUE ) {
                s.append( String.format( " gusting to %d knots", metar.windGustKnots ) );
            }
            if ( metar.windPeakKnots < Integer.MAX_VALUE
                    && metar.windPeakKnots != metar.windGustKnots ) {
                s.append( String.format( ", peak at %d knots", metar.windPeakKnots ) );
            }
        }
        return s.toString();
    }

    protected void showWindInfo( LinearLayout layout, Metar metar ) {
        View row = addRow( layout, getWindsDescription( metar ) );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        if ( metar.windDirDegrees > 0 ) {
            float declination = GeoUtils.getMagneticDeclination( mLocation );
            Drawable wind= WxUtils.getWindBarbDrawable( tv.getContext(), metar, declination );
            tv.setCompoundDrawablesWithIntrinsicBounds( wind, null, null, null );
            tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( getActivity(), 6 ) );
        }
        if ( metar.windGustKnots < Integer.MAX_VALUE ) {
            double gustFactor = metar.windGustKnots - metar.windSpeedKnots;
            addRow( layout, String.format( "Add %d knots to your normal approach speed",
                    Math.round( gustFactor/2 ) ) );
        }
        if ( metar.wshft ) {
            StringBuilder sb = new StringBuilder();
            sb.append( "Wind shift of 45\u00B0 or more detected during past hour" );
            if ( metar.fropa ) {
                sb.append( " due to frontal passage" );
            }
            addRow( layout, sb.toString() );
        }
    }

    protected void addSkyConditionRow( LinearLayout layout, SkyCondition sky,
            String flightCategory ) {
        View row = addRow( layout, sky.toString() );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        WxUtils.showColorizedDrawable( tv, flightCategory, sky.getDrawable() );
    }

    protected void addWeatherRow( LinearLayout layout, WxSymbol wx, String flightCategory ) {
        View row = addRow( layout, wx.toString() );
        if ( wx.getDrawable() != 0 ) {
            TextView tv = (TextView) row.findViewById( R.id.item_label );
            WxUtils.showColorizedDrawable( tv, flightCategory, wx.getDrawable() );
        }
    }

}
