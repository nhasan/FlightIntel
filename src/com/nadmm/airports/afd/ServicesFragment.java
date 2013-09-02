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

import java.util.ArrayList;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.notams.AirportNotamActivity;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;

public final class ServicesFragment extends FragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.services_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        Bundle args = getArguments();
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        setBackgroundTask( new ServicesTask() ).execute( siteNumber );

        super.onActivityCreated( savedInstanceState );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        showAirportTitle( apt );
        showAirportServices( result );
        showFaaServices( result );
        showFssServices( result );

        setContentShown( true );
    }

    protected void showAirportServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        ArrayList<String> services = new ArrayList<String>();

        String other =  apt.getString( apt.getColumnIndex( Airports.OTHER_SERVICES ) );
        if ( other.length() > 0 ) {
            services.addAll( DataUtils.decodeServices( other ) );
        }
        String storage = apt.getString( apt.getColumnIndex( Airports.STORAGE_FACILITY ) );
        if ( storage.length() > 0 ) {
            services.addAll( DataUtils.decodeStorage( storage ) );
        }
        String bottOxygen = apt.getString( apt.getColumnIndex( Airports.BOTTLED_O2_AVAILABLE ) );
        if ( bottOxygen.equals( "Y" ) ) {
            services.add( "Bottled Oxygen" );
        }
        String bulkOxygen = apt.getString( apt.getColumnIndex( Airports.BULK_O2_AVAILABLE ) );
        if ( bulkOxygen.equals( "Y" ) ) {
            services.add( "Bulk Oxygen" );
        }
        if ( !services.isEmpty() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.airport_services_layout );
            for ( String service : services ) {
                addBulletedRow( layout, service );
            }
        } else {
            TextView tv = (TextView) findViewById( R.id.airport_services_label );
            tv.setVisibility( View.GONE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.airport_services_layout );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showFaaServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.faa_services_layout );
        String faaRegion = apt.getString( apt.getColumnIndex( Airports.REGION_CODE ) );
        if ( faaRegion.length() > 0 ) {
            addRow( layout, "FAA region", DataUtils.decodeFaaRegion( faaRegion ) );
        }
        String artccId = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_ID ) );
        String artccName = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_NAME ) );
        addRow( layout, "ARTCC", artccId+" ("+artccName+")" );
        String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
        String notamFacility = apt.getString( apt.getColumnIndex( Airports.NOTAM_FACILITY_ID ) );
        Intent intent = new Intent( getActivity(), AirportNotamActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addClickableRow( layout, "NOTAM facility", notamFacility, intent );
        String notamD = apt.getString( apt.getColumnIndex( Airports.NOTAM_D_AVAILABLE ) );
        addRow( layout, "NOTAM D available", notamD.equals( "Y" )? "Yes" : "No" );
        setRowBackgroundResource( layout );
    }

    protected void showFssServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.fss_services_layout );
        String fssId = apt.getString( apt.getColumnIndex( Airports.FSS_ID ) );
        String fssName = apt.getString( apt.getColumnIndex( Airports.FSS_NAME ) );
        addRow( layout, "Flight service", fssId+" ("+fssName+")" );
        String fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_LOCAL_PHONE ) );
        if ( fssPhone.length() == 0 ) {
            fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_TOLLFREE_PHONE ) );
        }
        addPhoneRow( layout, "FSS phone", fssPhone );
        String state = apt.getString( apt.getColumnIndex( Airports.ASSOC_STATE ) );
        if ( !state.equals( "AK" ) ) {
            addPhoneRow( layout, "TIBS", "1-877-4TIBS-WX" );
            addPhoneRow( layout, "Clearance delivery", "1-888-766-8287" );
            String faaRegion = apt.getString( apt.getColumnIndex( Airports.REGION_CODE ) );
            if ( faaRegion.equals( "AEA" ) ) {
                addPhoneRow( layout, "DC SFRA & FRZ", "1-866-225-7410" );
            }
            addPhoneRow( layout, "Lifeguard flights", "1-877-LIF-GRD3" );
        }
    }

    private final class ServicesTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 1 ];
            cursors[ 0 ] = getAirportDetails( siteNumber );
            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

}
