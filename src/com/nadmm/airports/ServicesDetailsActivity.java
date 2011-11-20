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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.utils.DataUtils;

public class ServicesDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        ServicesDetailsTask task = new ServicesDetailsTask();
        task.execute( siteNumber );
    }

    private final class ServicesDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 1 ];
            cursors[ 0 ] = mDbManager.getAirportDetails( siteNumber );
            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );
            View view = inflate( R.layout.services_detail_view );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.services_top_layout );

            // Title
            Cursor apt = result[ 0 ];
            showAirportTitle( mMainLayout, apt );

            showAirportServices( result );
            showFaaServices( result );
        }

    }

    protected void showAirportServices( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                R.id.detail_airport_services_layout );
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
        TableLayout layout = (TableLayout) mMainLayout.findViewById(
                R.id.detail_faa_services_layout );
        String region = apt.getString( apt.getColumnIndex( Airports.REGION_CODE ) );
        if ( region.length() > 0 ) {
            addRow( layout, "FAA region", DataUtils.decodeFaaRegion( region ) );
            addSeparator( layout );
        }
        String artccId = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_ID ) );
        String artccName = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_NAME ) );
        addRow( layout, "ARTCC", artccId+" ("+artccName+")" );
        addSeparator( layout );
        String fssId = apt.getString( apt.getColumnIndex( Airports.FSS_ID ) );
        String fssName = apt.getString( apt.getColumnIndex( Airports.FSS_NAME ) );
        addRow( layout, "Flight service", fssId+" ("+fssName+")" );
        addSeparator( layout );
        String fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_LOCAL_PHONE ) );
        if ( fssPhone.length() == 0 ) {
            fssPhone = apt.getString( apt.getColumnIndex( Airports.FSS_TOLLFREE_PHONE ) );
        }
        addPhoneRow( layout, "FSS phone", fssPhone );
        addSeparator( layout );
        String siteNumber = apt.getString( apt.getColumnIndex( Airports.SITE_NUMBER ) );
        String notamFacility = apt.getString( apt.getColumnIndex( Airports.NOTAM_FACILITY_ID ) );
        Intent intent = new Intent( this, NotamActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        addClickableRow( layout, "NOTAM facility", notamFacility, intent,
                R.drawable.row_selector_middle );
        addSeparator( layout );
        String notamD = apt.getString( apt.getColumnIndex( Airports.NOTAM_D_AVAILABLE ) );
        addRow( layout, "NOTAM D available", notamD.equals( "Y" )? "Yes" : "No" );
    }

    protected void addPhoneRow( TableLayout table, String label, final String phone ) {
        RelativeLayout row = (RelativeLayout) inflate( R.layout.airport_detail_item );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label );
        tv = (TextView) row.findViewById( R.id.item_value );
        tv.setText( phone );
        makeClickToCall( tv );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

}
