/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2019 Nadeem Hasan <nhasan@nadmm.com>
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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Awos1;
import com.nadmm.airports.data.DatabaseManager.Wxs;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.wx.Pirep.Flags;
import com.nadmm.airports.wx.Pirep.IcingCondition;
import com.nadmm.airports.wx.Pirep.PirepEntry;
import com.nadmm.airports.wx.Pirep.SkyCondition;
import com.nadmm.airports.wx.Pirep.TurbulenceCondition;

import java.util.Locale;

public class PirepFragment extends WxFragmentBase {

    private final String mAction = NoaaService.ACTION_GET_PIREP;
    private final int PIREP_RADIUS_NM = 50;
    private final int PIREP_HOURS_BEFORE = 3;

    private Location mLocation;
    private String mStationId;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setupBroadcastFilter( mAction );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.pirep_detail_view, container, false );
        Button btnGraphic = view.findViewById( R.id.btnViewGraphic );
        btnGraphic.setOnClickListener( v -> {
            Intent intent = new Intent( getActivity(), PirepMapActivity.class );
            startActivity( intent );
        } );
        btnGraphic.setVisibility( View.GONE );
        return createContentView( view );
    }

    @Override
    public void onResume() {
        super.onResume();

        if ( getArguments() != null ) {
            String stationId = getArguments().getString( NoaaService.STATION_ID );
            setBackgroundTask( new PirepDetailTask( this ) ).execute( stationId );
        }
    }

    @Override
    protected void handleBroadcast( Intent intent ) {
        String type = intent.getStringExtra( NoaaService.TYPE );
        if ( NoaaService.TYPE_TEXT.equals( type ) ) {
            showPirep( intent );
            setRefreshing( false );
        }
    }

    @Override
    protected String getProduct() {
        return "pirep";
    }

    @Override
    public boolean isRefreshable() {
        return true;
    }

    @Override
    public void requestDataRefresh() {
        requestPirep( true );
    }

    private Cursor[] doQuery( String stationId ) {
        Cursor[] cursors = new Cursor[ 2 ];
        SQLiteDatabase db = getDbManager().getDatabase( DatabaseManager.DB_FADDS );

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
        String selection = "a."+Airports.ICAO_CODE+"=?";
        c = builder.query( db, wxColumns, selection, new String[] { stationId },
                null, null, null, null );
        cursors[ 1 ] = c;

        return cursors;
    }

    private void setCursor( Cursor c ) {
        if ( c == null || !c.moveToFirst() ) {
            if ( getArguments() != null ) {
                String stationId = getArguments().getString( NoaaService.STATION_ID );
                String error = String.format( "Unable to get station info for %s", stationId );
                showError( error );
                setRefreshing( false );
            }
        } else {
            mLocation = new Location( "" );
            float lat = c.getFloat( c.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
            float lon = c.getFloat( c.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
            mLocation.setLatitude( lat );
            mLocation.setLongitude( lon );

            // Now request the weather
            mStationId = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
            requestPirep( false );
        }
    }

    private static class PirepDetailTask extends CursorAsyncTask<PirepFragment> {

        private PirepDetailTask( PirepFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( PirepFragment fragment, String... params ) {
            String stationId = params[ 0 ];
            return fragment.doQuery( stationId );
        }

        @Override
        protected boolean onResult( PirepFragment fragment, Cursor[] result ) {
            fragment.setCursor( result[ 0 ] );
            return true;
        }

    }

    private void requestPirep( boolean refresh ) {
        if ( getActivity() != null ) {
            Intent service = new Intent( getActivity(), PirepService.class );
            service.setAction( mAction );
            service.putExtra( NoaaService.STATION_ID, mStationId );
            service.putExtra( NoaaService.TYPE, NoaaService.TYPE_TEXT );
            service.putExtra( PirepService.RADIUS_NM, PIREP_RADIUS_NM );
            service.putExtra( PirepService.HOURS_BEFORE, PIREP_HOURS_BEFORE );
            service.putExtra( PirepService.LOCATION, mLocation );
            service.putExtra( NoaaService.FORCE_REFRESH, refresh );
            getActivity().startService( service );
        }
    }

    private void showPirep( Intent intent ) {
        Pirep pirep = (Pirep) intent.getSerializableExtra( NoaaService.RESULT );
        if ( pirep == null ) {
            return;
        }

        LinearLayout layout = findViewById( R.id.pirep_entries_layout );
        layout.removeAllViews();

        TextView tv = findViewById( R.id.pirep_title_msg );
        if ( !pirep.entries.isEmpty() ) {
            tv.setText( String.format( Locale.US,
                    "%d PIREPs reported within %d NM of %s during last %d hours",
                    pirep.entries.size(), PIREP_RADIUS_NM, mStationId, PIREP_HOURS_BEFORE ) );
            for ( PirepEntry entry : pirep.entries ) {
                showPirepEntry( layout, entry );
            }
        } else {
            tv.setText( String.format( Locale.US,
                    "No PIREPs reported within %d NM of %s in last %d hours",
                    PIREP_RADIUS_NM, mStationId, PIREP_HOURS_BEFORE ) );
        }

        tv = findViewById( R.id.wx_fetch_time );
        tv.setText( String.format( Locale.US, "Fetched on %s",
                TimeUtils.formatDateTime( getActivityBase(), pirep.fetchTime ) ) );
        tv.setVisibility( View.VISIBLE );

        setFragmentContentShown( true );
    }

    @SuppressLint( "SetTextI18n" )
    private void showPirepEntry( LinearLayout layout, PirepEntry entry ) {
        RelativeLayout item = inflate( R.layout.pirep_detail_item );

        TextView tv = item.findViewById( R.id.pirep_title );
        if ( entry.flags.contains( Flags.BadLocation ) ) {
            String dir = GeoUtils.getCardinalDirection( entry.bearing );
            tv.setText( String.format( Locale.US, "%.0f NM %s approx.", entry.distanceNM, dir ) );
        } else if ( entry.distanceNM > 0 ) {
            String dir = GeoUtils.getCardinalDirection( entry.bearing );
            tv.setText( String.format( Locale.US, "%.0f NM %s", entry.distanceNM, dir ) );
        } else {
            tv.setText( "On Site" );
        }

        tv = item.findViewById( R.id.pirep_title_extra );
        tv.setText( TimeUtils.formatElapsedTime( entry.observationTime ) );

        tv = item.findViewById( R.id.wx_raw_pirep );
        tv.setText( entry.rawText );

        LinearLayout details = item.findViewById( R.id.pirep_details );

        addRow( details, "Type", entry.reportType );
        addRow( details, "Aircraft", entry.aircraftRef );

        String time = TimeUtils.formatDateTime( getActivityBase(), entry.observationTime );
        addRow( details, "Time", time );

        if ( entry.altitudeFeetMSL < Integer.MAX_VALUE ) {
            if ( entry.flags.contains( Flags.NoFlightLevel ) ) {
                addRow( details, "Altitude",
                        FormatUtils.formatFeetMsl( entry.altitudeFeetMSL ), "(Approximate)" );
            } else {
                addRow( details, "Altitude",
                        FormatUtils.formatFeetMsl( entry.altitudeFeetMSL ) );
            }
        }

        if ( entry.visibilitySM < Integer.MAX_VALUE ) {
            addRow( details, "Visibility", FormatUtils.formatStatuteMiles( entry.visibilitySM ) );
        }

        if ( entry.tempCelsius < Integer.MAX_VALUE ) {
            addRow( details, "Temperature", FormatUtils.formatTemperature( entry.tempCelsius ) );
        }

        if ( entry.windSpeedKnots < Integer.MAX_VALUE ) {
            addRow( details, "Winds", String.format( Locale.US, "%s (true) at %d knots",
                    FormatUtils.formatDegrees( entry.windDirDegrees ), entry.windSpeedKnots ) );
        }

        if ( !entry.wxList.isEmpty() ) {
            StringBuilder sb = new StringBuilder();
            for ( WxSymbol wx : entry.wxList ) {
                if ( sb.length() > 0 ) {
                    sb.append( ", " );
                }
                sb.append( wx.toString() );
            }
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

        layout.addView( item, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );
    }

    private void addSkyConditionRow( LinearLayout details, SkyCondition sky ) {
        String extra = FormatUtils.formatFeetRangeMsl( sky.baseFeetMSL, sky.topFeetMSL );
        addRow( details, "Sky cover", sky.skyCover, extra );
    }

    private void addTurbulenceRow( LinearLayout details, TurbulenceCondition turbulence ) {
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
        sb.append( FormatUtils.formatFeetRangeMsl( turbulence.baseFeetMSL, turbulence.topFeetMSL ) );
        String extra = sb.toString();
        addRow( details, "Turbulence", value, extra );
    }

    private void addIcingRow( LinearLayout details, IcingCondition icing ) {
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
        addRow( details, "Icing", value, extra );
    }

    private void showError( String error ) {
        View detail = findViewById( R.id.wx_detail_layout );
        detail.setVisibility( View.GONE );
        LinearLayout layout = findViewById( R.id.wx_status_layout );
        layout.removeAllViews();
        layout.setVisibility( View.GONE );
        TextView tv =findViewById( R.id.status_msg );
        tv.setVisibility( View.VISIBLE );
        tv.setText( error );
        View title = findViewById( R.id.wx_title_layout );
        title.setVisibility( View.GONE );
        setContentShown( true );
    }

}
