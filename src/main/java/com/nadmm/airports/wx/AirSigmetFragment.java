/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2018 Nadeem Hasan <nhasan@nadmm.com>
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
import com.nadmm.airports.data.DatabaseManager.Wxs;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.wx.AirSigmet.AirSigmetEntry;

import java.util.Locale;

public class AirSigmetFragment extends WxFragmentBase {

    private final String mAction = NoaaService.ACTION_GET_AIRSIGMET;

    private final int AIRSIGMET_RADIUS_NM = 50;
    private final int AIRSIGMET_HOURS_BEFORE = 3;

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
        View view = inflater.inflate( R.layout.airsigmet_detail_view, container, false );
        Button btnGraphic = view.findViewById( R.id.btnViewGraphic );
        btnGraphic.setOnClickListener( v -> {
            Intent intent = new Intent( getActivity(), AirSigmetMapActivity.class );
            startActivity( intent );
        } );

        return createContentView( view );
    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle args = getArguments();
        String stationId = args.getString( NoaaService.STATION_ID );
        setBackgroundTask( new AirSigmetTask( this ) ).execute( stationId );
    }

    @Override
    protected void handleBroadcast( Intent intent ) {
        if ( mLocation == null ) {
            // This was probably intended for wx list view
            return;
        }

        String type = intent.getStringExtra( NoaaService.TYPE );
        if ( type.equals( NoaaService.TYPE_TEXT ) ) {
            showAirSigmetText( intent );
            setRefreshing( false );
        }
    }

    @Override
    protected String getProduct() {
        return "airsigmet";
    }

    @Override
    public boolean isRefreshable() {
        return true;
    }

    @Override
    public void requestDataRefresh() {
        requestAirSigmetText( true );
    }

    private Cursor[] doQuery( String stationId ) {
        SQLiteDatabase db = getDbManager().getDatabase( DatabaseManager.DB_FADDS );

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Wxs.TABLE_NAME );
        String selection = Wxs.STATION_ID+"=?";
        Cursor c = builder.query( db, new String[]{ "*" }, selection,
                new String[] { stationId }, null, null, null, null );
        return new Cursor[] { c };
    }

    private void setCursor( Cursor c ) {
        if ( c.moveToFirst() ) {
            mStationId = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
            mLocation = new Location( "" );
            float lat = c.getFloat( c.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
            float lon = c.getFloat( c.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
            mLocation.setLatitude( lat );
            mLocation.setLongitude( lon );
            // Now request the airmet/sigmet
            requestAirSigmetText( false );
        }
    }

    private static class AirSigmetTask extends CursorAsyncTask<AirSigmetFragment> {

        private AirSigmetTask( AirSigmetFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( AirSigmetFragment fragment, String... params ) {
            String stationId = params[ 0 ];
            return fragment.doQuery( stationId );
        }

        @Override
        protected boolean onResult( AirSigmetFragment fragment, Cursor[] result ) {
            fragment.setCursor( result[ 0 ] );
            return true;
        }

    }

    private void requestAirSigmetText( boolean refresh ) {
        double[] box = GeoUtils.getBoundingBoxDegrees( mLocation, AIRSIGMET_RADIUS_NM );
        Intent service = new Intent( getActivity(), AirSigmetService.class );
        service.setAction( mAction );
        service.putExtra( NoaaService.STATION_ID, mStationId );
        service.putExtra( NoaaService.TYPE, NoaaService.TYPE_TEXT );
        service.putExtra( NoaaService.COORDS_BOX, box );
        service.putExtra( NoaaService.HOURS_BEFORE, AIRSIGMET_HOURS_BEFORE );
        service.putExtra( NoaaService.FORCE_REFRESH, refresh );
        getActivity().startService( service );
    }

    private void showAirSigmetText( Intent intent ) {
        if ( getActivity() == null ) {
            return;
        }

        LinearLayout layout = findViewById( R.id.airsigmet_entries_layout );
        if ( layout == null ) {
            return;
        }
        layout.removeAllViews();

        AirSigmet airSigmet = (AirSigmet) intent.getSerializableExtra(NoaaService.RESULT );

        TextView tv = findViewById( R.id.airsigmet_title_msg );
        if ( !airSigmet.entries.isEmpty() ) {
            tv.setText( String.format( Locale.US, "%d AIR/SIGMETs reported within %d NM of %s in"
                    +" last %d hours", airSigmet.entries.size(), AIRSIGMET_RADIUS_NM,
                    mStationId, AIRSIGMET_HOURS_BEFORE ) );
            for ( AirSigmetEntry entry : airSigmet.entries ) {
                showAirSigmetEntry( layout, entry );
            }
        } else {
            tv.setText( String.format( Locale.US, "No AIR/SIGMETs reported within %d NM of %s in"
                    +" last %d hours",
                    AIRSIGMET_RADIUS_NM, mStationId, AIRSIGMET_HOURS_BEFORE ) );
        }

        tv = findViewById( R.id.wx_fetch_time );
        tv.setText( String.format( Locale.US, "Fetched on %s",
                TimeUtils.formatDateTime( getActivityBase(), airSigmet.fetchTime ) ) );
        tv.setVisibility( View.VISIBLE );

        setFragmentContentShown( true );
    }

    private void showAirSigmetEntry( LinearLayout layout, AirSigmetEntry entry ) {
        RelativeLayout item = (RelativeLayout) inflate( R.layout.airsigmet_detail_item );
        TextView tv = item.findViewById( R.id.airsigmet_type );
        tv.setText( entry.type );
        tv = item.findViewById( R.id.wx_raw_airsigmet );
        tv.setText( entry.rawText );
        tv = item.findViewById( R.id.airsigmet_time );
        tv.setText( TimeUtils.formatDateRange( getActivityBase(), entry.fromTime, entry.toTime ) );

        LinearLayout details = item.findViewById( R.id.airsigmet_details );

        if ( entry.hazardType != null && entry.hazardType.length() > 0 ) {
            addRow( details, "Type", entry.hazardType );
        }

        if ( entry.hazardSeverity != null && entry.hazardSeverity.length() > 0 ) {
            addRow( details, "Severity", entry.hazardSeverity );
        }

        if ( entry.minAltitudeFeet < Integer.MAX_VALUE
                || entry.maxAltitudeFeet < Integer.MAX_VALUE ) {
            addRow( details, "Altitude", FormatUtils.formatFeetRangeMsl(
                    entry.minAltitudeFeet, entry.maxAltitudeFeet ) );
        }

        if ( entry.movementDirDegrees < Integer.MAX_VALUE ) {
            StringBuilder sb = new StringBuilder();
            sb.append( String.format( "%s (%s)",
                    FormatUtils.formatDegrees( entry.movementDirDegrees ),
                    GeoUtils.getCardinalDirection( entry.movementDirDegrees ) ) );
            if ( entry.movementSpeedKnots < Integer.MAX_VALUE ) {
                sb.append( String.format( Locale.US, " at %d knots", entry.movementSpeedKnots ) );
            }
            addRow( details, "Movement", sb.toString() );
        }

        layout.addView( item, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );
    }

}
