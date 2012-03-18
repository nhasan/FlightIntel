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
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.wx.Pirep.Flags;
import com.nadmm.airports.wx.Pirep.IcingCondition;
import com.nadmm.airports.wx.Pirep.PirepEntry;
import com.nadmm.airports.wx.Pirep.SkyCondition;
import com.nadmm.airports.wx.Pirep.TurbulenceCondition;

public class PirepFragment extends FragmentBase {

    private final int PIREP_RADIUS_NM = 50;
    private final int PIREP_HOURS_BEFORE = 3;

    protected Location mLocation;
    protected CursorAsyncTask mTask;
    protected BroadcastReceiver mReceiver;
    protected String mStationId;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                Pirep pirep = (Pirep) intent.getSerializableExtra( NoaaService.RESULT );
                showPirep( pirep );
            }

        };
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( NoaaService.ACTION_GET_PIREP );
        getActivity().registerReceiver( mReceiver, filter );

        Bundle args = getArguments();
        String stationId = args.getString( NoaaService.STATION_ID );
        mTask = new PirepDetailTask();
        mTask.execute( stationId );
        super.onResume();
    }

    @Override
    public void onPause() {
        mTask.cancel( true );
        getActivity().unregisterReceiver( mReceiver );
        super.onPause();
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.pirep_detail_view, container, false );
        return createContentView( view );
    }

    private final class PirepDetailTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String stationId = params[ 0 ];

            Cursor[] cursors = new Cursor[ 2 ];
            SQLiteDatabase db = getDbManager().getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Wxs.TABLE_NAME );
            String selection = Wxs.STATION_ID+"=?";
            Cursor c = builder.query( db, new String[] { "*" }, Wxs.STATION_ID+"=?",
                    new String[] { stationId }, null, null, null, null );
            cursors[ 0 ] = c;

            String[] wxColumns = new String[] {
                    Awos.WX_SENSOR_IDENT,
                    Awos.WX_SENSOR_TYPE,
                    Awos.STATION_FREQUENCY,
                    Awos.SECOND_STATION_FREQUENCY,
                    Awos.STATION_PHONE_NUMBER,
                    Airports.ASSOC_CITY,
                    Airports.ASSOC_STATE
            };
            builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME+" a"
                    +" LEFT JOIN "+Awos.TABLE_NAME+" w"
                    +" ON a."+Airports.FAA_CODE+" = w."+Awos.WX_SENSOR_IDENT );
            selection = "a."+Airports.ICAO_CODE+"=?";
            c = builder.query( db, wxColumns, selection, new String[] { stationId },
                    null, null, null, null );
            cursors[ 1 ] = c;

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            Cursor wxs = result[ 0 ];
            if ( wxs == null || !wxs.moveToFirst() ) {
                Bundle args = getArguments();
                String stationId = args.getString( NoaaService.STATION_ID );
                String error = String.format( "Unable to get station info for %s", stationId );
                showError( error );
                return;
            }

            mLocation = new Location( "" );
            float lat = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
            float lon = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
            mLocation.setLatitude( lat );
            mLocation.setLongitude( lon );

            // Now request the weather
            mStationId = wxs.getString( wxs.getColumnIndex( Wxs.STATION_ID ) );
            requestPirep( false );
        }

    }

    protected void requestPirep( boolean refresh ) {
        Intent service = new Intent( getActivity(), PirepService.class );
        service.setAction( NoaaService.ACTION_GET_PIREP );
        service.putExtra( NoaaService.STATION_ID, mStationId );
        service.putExtra( PirepService.PIREP_RADIUS_NM, PIREP_RADIUS_NM );
        service.putExtra( PirepService.PIREP_HOURS_BEFORE, PIREP_HOURS_BEFORE );
        service.putExtra( PirepService.PIREP_LOCATION, mLocation );
        service.putExtra( NoaaService.FORCE_REFRESH, refresh );
        getActivity().startService( service );
    }

    protected void showPirep( Pirep pirep ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.pirep_entries_layout );
        layout.removeAllViews();

        if ( !pirep.entries.isEmpty() ) {
            TextView tv = (TextView) findViewById( R.id.pirep_title_msg );
            tv.setText( String.format( "%d PIREPs reported within %d NM of %s during last %d hours",
                    pirep.entries.size(), PIREP_RADIUS_NM, mStationId, PIREP_HOURS_BEFORE ) );
            for ( PirepEntry entry : pirep.entries ) {
                showPirepEntry( layout, entry );
            }
        } else {
            TextView tv = (TextView) findViewById( R.id.pirep_title_msg );
            tv.setText( String.format( "No PIREPs reported within %d NM of %s in last %d hours",
                    PIREP_RADIUS_NM, mStationId, PIREP_HOURS_BEFORE ) );
        }

        TextView tv = (TextView) findViewById( R.id.wx_fetch_time );
        tv.setText( "Fetched on "+TimeUtils.formatLongDateTime( pirep.fetchTime )  );
        tv.setVisibility( View.VISIBLE );

        stopRefreshAnimation();
        setContentShown( true );
    }

    protected void showPirepEntry( LinearLayout layout, PirepEntry entry ) {
        RelativeLayout item = (RelativeLayout) inflate( R.layout.pirep_detail_item );
        TextView tv = (TextView) item.findViewById( R.id.pirep_name );
        String time = TimeUtils.formatDateTime( getActivity(), entry.observationTime );
        if ( entry.flags.contains( Flags.BadLocation ) ) {
            String dir = GeoUtils.getCardinalDirection( entry.bearing );
            tv.setText( String.format( "%s (%.0f NM %s approx.)", time, entry.distanceNM, dir ) );
        } else if ( entry.distanceNM > 0 ) {
            String dir = GeoUtils.getCardinalDirection( entry.bearing );
            tv.setText( String.format( "%s (%.0f NM %s)", time, entry.distanceNM, dir ) );
        } else {
            tv.setText( String.format( "%s (0 NM)", time ) );
        }
        tv = (TextView) item.findViewById( R.id.pirep_age );
        tv.setText( TimeUtils.formatElapsedTime( entry.observationTime ) );

        tv = (TextView) item.findViewById( R.id.wx_raw_pirep );
        tv.setText( entry.rawText );

        LinearLayout details = (LinearLayout) item.findViewById( R.id.pirep_details );

        addRow( details, "Type", entry.reportType );
        addSeparator( details );
        addRow( details, "Aircraft", entry.aircraftRef );

        if ( entry.altitudeFeetMSL < Integer.MAX_VALUE ) {
            addSeparator( details );
            if ( entry.flags.contains( Flags.NoFlightLevel ) ) {
                addRow( details, "Altitude",
                        FormatUtils.formatFeetMsl( entry.altitudeFeetMSL ), "(Approximate)" );
            } else {
                addRow( details, "Altitude",
                        FormatUtils.formatFeetMsl( entry.altitudeFeetMSL ) );
            }
        }

        if ( entry.visibilitySM < Integer.MAX_VALUE ) {
            addSeparator( details );
            addRow( details, "Visibility", FormatUtils.formatVisibility( entry.visibilitySM ) );
        }

        if ( entry.tempCelsius < Integer.MAX_VALUE ) {
            addSeparator( details );
            addRow( details, "Temperature", FormatUtils.formatTemperature( entry.tempCelsius ) );
        }

        if ( entry.windSpeedKnots < Integer.MAX_VALUE ) {
            addSeparator( details );
            addRow( details, "Winds", String.format( "%d\u00B0 (true) at %d knots",
                    entry.windDirDegrees, entry.windSpeedKnots ) );
        }

        if ( !entry.wxList.isEmpty() ) {
            StringBuilder sb = new StringBuilder();
            for ( WxSymbol wx : entry.wxList ) {
                if ( sb.length() > 0 ) {
                    sb.append( ", " );
                }
                sb.append( wx.toString() );
            }
            addSeparator( details );
            addRow( details, "Weather", sb.toString() );
        }

        for ( SkyCondition sky : entry.skyConditions ) {
            addSkyConditionRow( details, sky );
        }

        for ( TurbulenceCondition turbulence : entry.turbulenceConditions ) {
            addTurbulenceRow( details, turbulence );
        }

        for ( IcingCondition icing : entry.icingConditions ) {
            addIcingRow( details, icing );
        }

        layout.addView( item, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT );
    }

    protected void addSkyConditionRow( LinearLayout details, SkyCondition sky ) {
        StringBuilder sb = new StringBuilder();
        sb.append( FormatUtils.formatFeetRangeMsl( sky.baseFeetMSL, sky.topFeetMSL ) );
        String extra = sb.toString();
        addSeparator( details );
        addRow( details, "Sky cover", sky.skyCover, extra );
    }

    protected void addTurbulenceRow( LinearLayout details, TurbulenceCondition turbulence ) {
        StringBuilder sb = new StringBuilder();
        if ( turbulence.frequency != null ) {
            sb.append( turbulence.frequency );
        }
        if ( turbulence.intensity != null ) {
            if ( sb.length() > 0 ) {
                sb.append( " " );
            }
            sb.append( turbulence.intensity );
        }
        if ( turbulence.type != null ) {
            if ( sb.length() > 0 ) {
                sb.append( " " );
            }
            sb.append( turbulence.type );
        }
        String value = sb.toString();
        sb.setLength( 0 );
        sb.append( FormatUtils.formatFeetRangeMsl(
                turbulence.baseFeetMSL, turbulence.topFeetMSL ) );
        String extra = sb.toString();
        addSeparator( details );
        addRow( details, "Turbulence", value, extra );
    }

    protected void addIcingRow( LinearLayout details, IcingCondition icing ) {
        StringBuilder sb = new StringBuilder();
        if ( icing.intensity != null ) {
            sb.append( icing.intensity );
        }
        if ( icing.type != null ) {
            if ( sb.length() > 0 ) {
                sb.append( " " );
            }
            sb.append( icing.type );
        }
        String value = sb.toString();
        sb.setLength( 0 );
        sb.append( FormatUtils.formatFeetRangeMsl( icing.baseFeetMSL, icing.topFeetMSL ) );
        String extra = sb.toString();
        addSeparator( details );
        addRow( details, "Icing", value, extra );
    }

    protected void showError( String error ) {
        View detail = findViewById( R.id.wx_detail_layout );
        detail.setVisibility( View.GONE );
        LinearLayout layout = (LinearLayout) findViewById( R.id.wx_status_layout );
        layout.removeAllViews();
        layout.setVisibility( View.GONE );
        TextView tv =(TextView) findViewById( R.id.status_msg );
        tv.setVisibility( View.VISIBLE );
        tv.setText( error );
        View title = findViewById( R.id.wx_title_layout );
        title.setVisibility( View.GONE );
        stopRefreshAnimation();
        setContentShown( true );
        return;
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
            requestPirep( true );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
