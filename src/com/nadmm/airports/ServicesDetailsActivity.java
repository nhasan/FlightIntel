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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;

public class ServicesDetailsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.services_detail_view ) );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        ServicesDetailsTask task = new ServicesDetailsTask();
        task.execute( siteNumber );
    }

    private final class ServicesDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 1 ];
            cursors[ 0 ] = getAirportDetails( siteNumber );
            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            showDetails( result );
        }

    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        setActionBarTitle( apt );
        showAirportTitle( apt );
        showAirportServices( result );
        showFaaServices( result );
        showFssServices( result );

        setContentShown( true );
    }

    protected void showAirportServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_airport_services_layout );
        String otherServices = DataUtils.decodeServices(
                apt.getString( apt.getColumnIndex( Airports.OTHER_SERVICES ) ) );
        String storage = DataUtils.decodeStorage( 
                apt.getString( apt.getColumnIndex( Airports.STORAGE_FACILITY ) ) );
        if ( storage.length() > 0 ) {
            otherServices += ","+storage;
        }
        String bottOxygen = apt.getString( apt.getColumnIndex( Airports.BOTTLED_O2_AVAILABLE ) );
        if ( bottOxygen.equals( "Y" ) ) {
            if  ( otherServices.length() > 0 ) {
                otherServices += ",";
            }
            otherServices += "Bottled Oxygen";
        }
        String bulkOxygen = apt.getString( apt.getColumnIndex( Airports.BULK_O2_AVAILABLE ) );
        if ( bulkOxygen.equals( "Y" ) ) {
            if  ( otherServices.length() > 0 ) {
                otherServices += ",";
            }
            otherServices += "Bulk Oxygen";
        }
        String[] services = otherServices.split( ",\\s*" );
        for ( String service : services ) {
            if ( service.length() > 0 ) {
                addBulletedRow( layout, service );
            }
        }
    }

    protected void showFaaServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_faa_services_layout );
        String faaRegion = apt.getString( apt.getColumnIndex( Airports.REGION_CODE ) );
        if ( faaRegion.length() > 0 ) {
            addRow( layout, "FAA region", DataUtils.decodeFaaRegion( faaRegion ) );
            addSeparator( layout );
        }
        String artccId = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_ID ) );
        String artccName = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_NAME ) );
        addRow( layout, "ARTCC", artccId+" ("+artccName+")" );
        addSeparator( layout );
        String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
        String notamFacility = apt.getString( apt.getColumnIndex( Airports.NOTAM_FACILITY_ID ) );
        Intent intent = new Intent( this, AirportNotamActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addClickableRow( layout, "NOTAM facility", notamFacility, intent,
                R.drawable.row_selector_middle );
        addSeparator( layout );
        String notamD = apt.getString( apt.getColumnIndex( Airports.NOTAM_D_AVAILABLE ) );
        addRow( layout, "NOTAM D available", notamD.equals( "Y" )? "Yes" : "No" );
    }

    protected void showFssServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_fss_services_layout );
        String fssId = apt.getString( apt.getColumnIndex( Airports.FSS_ID ) );
        String fssName = apt.getString( apt.getColumnIndex( Airports.FSS_NAME ) );
        addRow( layout, "Flight service", fssId+" ("+fssName+")" );
        addSeparator( layout );
        String fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_LOCAL_PHONE ) );
        if ( fssPhone.length() == 0 ) {
            fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_TOLLFREE_PHONE ) );
        }
        addPhoneRow( layout, "FSS phone", fssPhone );
        String state = apt.getString( apt.getColumnIndex( Airports.ASSOC_STATE ) );
        if ( !state.equals( "AK" ) ) {
            addSeparator( layout );
            addPhoneRow( layout, "TIBS", "1-877-4TIBS-WX" );
            addSeparator( layout );
            addPhoneRow( layout, "Clearance delivery", "1-888-766-8287" );
            String faaRegion = apt.getString( apt.getColumnIndex( Airports.REGION_CODE ) );
            if ( faaRegion.equals( "AEA" ) ) {
                addSeparator( layout );
                addPhoneRow( layout, "DC SFRA & FRZ", "1-866-225-7410" );
            }
            addSeparator( layout );
            addPhoneRow( layout, "Lifeguard flights", "1-877-LIF-GRD3" );
        }
    }

}
