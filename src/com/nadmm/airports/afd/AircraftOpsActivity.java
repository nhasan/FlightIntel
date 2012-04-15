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

package com.nadmm.airports.afd;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;

public class AircraftOpsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.aircraft_ops_view ) );

        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        setBackgroundTask( new OpsDetailsTask() ).execute( siteNumber );
    }

    private final class OpsDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            Cursor[] cursors = new Cursor[ 1 ];

            Cursor apt = getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        showAirportTitle( apt );
        showBasedAircraft( apt );
        showAnnualOps( apt );

        setContentShown( true );
    }

    protected void showBasedAircraft( Cursor apt ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.based_aircraft_layout );
        int count = apt.getInt( apt.getColumnIndex( Airports.SINGLE_ENGINE_COUNT ) );
        addRow( layout, "Single engine", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.MULTI_ENGINE_COUNT ) );
        addRow( layout, "Multi engine", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.JET_ENGINE_COUNT ) );
        addRow( layout, "Jet engine", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.HELI_COUNT ) );
        addRow( layout, "Helicopters", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.GLIDERS_COUNT ) );
        addRow( layout, "Gliders", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.ULTRA_LIGHT_COUNT ) );
        addRow( layout, "Ultra light", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.MILITARY_COUNT ) );
        addRow( layout, "Military", FormatUtils.formatNumber( count ) );
    }

    protected void showAnnualOps( Cursor apt ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.aircraft_ops_layout );
        int count = apt.getInt( apt.getColumnIndex( Airports.ANNUAL_COMMERCIAL_OPS ) );
        addRow( layout, "Commercial", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.ANNUAL_COMMUTER_OPS ) );
        addRow( layout, "Commuter", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.ANNUAL_AIRTAXI_OPS ) );
        addRow( layout, "Air taxi", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.ANNUAL_GA_LOCAL_OPS ) );
        addRow( layout, "GA local", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.ANNUAL_GA_ININERANT_OPS ) );
        addRow( layout, "GA other", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        count = apt.getInt( apt.getColumnIndex( Airports.ANNUAL_MILITARY_OPS ) );
        addRow( layout, "Military", FormatUtils.formatNumber( count ) );
        addSeparator( layout );
        String date = apt.getString( apt.getColumnIndex( Airports.OPS_REFERENCE_DATE ) );
        addRow( layout, "As-of", date );
    }

}
