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

package com.nadmm.airports.afd;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Com;
import com.nadmm.airports.DatabaseManager.Nav1;
import com.nadmm.airports.DatabaseManager.Nav2;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.notams.NavaidNotamActivity;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.UiUtils;

public class NavaidDetailsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.navaid_activity_layout ) );

        Bundle args = getIntent().getExtras();
        addFragment( NavaidDetailsFragment.class, args );
    }

    public static class NavaidDetailsFragment extends FragmentBase {

        private final class NavaidDetailsTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                String navaidId = params[ 0 ];
                String navaidType = params[ 1 ];

                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
                Cursor[] cursors = new Cursor[ 3 ];

                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables( Nav1.TABLE_NAME+" a LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                        +" ON a."+Nav1.ASSOC_STATE+"=s."+States.STATE_CODE );
                Cursor c = builder.query( db, new String[] { "*" },
                        Nav1.NAVAID_ID+"=? AND "+Nav1.NAVAID_TYPE+"=?",
                        new String[] { navaidId, navaidType }, null, null, null, null );
                if ( !c.moveToFirst() ) {
                    return null;
                }

                cursors[ 0 ] = c;

                builder = new SQLiteQueryBuilder();
                builder.setTables( Nav2.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Nav1.NAVAID_ID+"=? AND "+Nav1.NAVAID_TYPE+"=?",
                        new String[] { navaidId, navaidType }, null, null, null, null );
                cursors[ 1 ] = c;

                if ( !navaidType.equals( "VOT" ) ) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables( Com.TABLE_NAME );
                    c = builder.query( db, new String[] { "*" },
                            Com.ASSOC_NAVAID_ID+"=?", new String[] { navaidId },
                            null, null, null, null );
                    cursors[ 2 ] = c;
                } else {
                    cursors[ 2 ] = null;
                }

                return cursors;
            }

            @Override
            protected boolean onResult( Cursor[] result ) {
                showDetails( result );
                return true;
            }

        }

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View view = inflater.inflate( R.layout.navaid_detail_view, container, false );
            return view;
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            Bundle args = getArguments();
            String navaidId = args.getString( Nav1.NAVAID_ID );
            String navaidType = args.getString( Nav1.NAVAID_TYPE );
            setBackgroundTask( new NavaidDetailsTask() ).execute( navaidId, navaidType );

            super.onActivityCreated( savedInstanceState );
        }

        protected void showDetails( Cursor[] result ) {
            if ( result == null ) {
                UiUtils.showToast( getActivity(), "Navaid not found" );
                getActivity().finish();
                return;
            }

            Cursor nav1 = result[ 0 ];

            String id = nav1.getString( nav1.getColumnIndex( Nav1.NAVAID_ID ) );
            setActionBarTitle( id );
            showNavaidTitle( nav1 );
            showNavaidDetails( result );
            showNavaidRemarks( result );

            setContentShown( true );
        }

        protected void showNavaidDetails( Cursor[] result ) {
            Cursor nav1 = result[ 0 ];
            LinearLayout layout = (LinearLayout) findViewById( R.id.navaid_details );
            String navaidClass = nav1.getString( nav1.getColumnIndex( Nav1.NAVAID_CLASS ) );
            String navaidType = nav1.getString( nav1.getColumnIndex( Nav1.NAVAID_TYPE ) );
            addRow( layout, "Class", navaidClass );
            double freq = nav1.getDouble( nav1.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
            String tacan = nav1.getString( nav1.getColumnIndex( Nav1.TACAN_CHANNEL ) );
            if ( freq == 0 ) {
                freq = DataUtils.getTacanChannelFrequency( tacan );
            }
            if ( freq > 0 ) {
                if ( !DataUtils.isDirectionalNavaid( navaidType ) && ( freq%1.0 ) == 0 ) {
                    addRow( layout, "Frequency", String.format( "%.0f", freq ) );
                } else {
                    addRow( layout, "Frequency", String.format( "%.2f", freq ) );
                }
            }
            String power = nav1.getString( nav1.getColumnIndex( Nav1.POWER_OUTPUT ) );
            if ( power.length() > 0 ) {
                addRow( layout, "Power output", power+" Watts" );
            }
            if ( tacan.length() > 0 ) {
                addRow( layout, "Tacan channel", tacan );
            }
            String magVar = nav1.getString( nav1.getColumnIndex( Nav1.MAGNETIC_VARIATION_DEGREES ) );
            if ( magVar.length() > 0 ) {
                String magDir = nav1.getString( nav1.getColumnIndex( 
                        Nav1.MAGNETIC_VARIATION_DIRECTION ) );
                String magYear = nav1.getString( nav1.getColumnIndex( 
                        Nav1.MAGNETIC_VARIATION_YEAR ) );
                addRow( layout, "Magnetic variation", String.format( "%d\u00B0%s (%s)",
                        Integer.valueOf( magVar ), magDir, magYear ) );
            }
            String alt = nav1.getString( nav1.getColumnIndex( Nav1.PROTECTED_FREQUENCY_ALTITUDE ) );
            if ( alt.length() > 0 ) {
                addRow( layout, "Service volume", DataUtils.decodeNavProtectedAltitude( alt ) );
            }
            String hours = nav1.getString( nav1.getColumnIndex( Nav1.OPERATING_HOURS ) );
            addRow( layout, "Operating hours", hours );
            String type = nav1.getString( nav1.getColumnIndex( Nav1.FANMARKER_TYPE ) );
            if ( type.length() > 0 ) {
                addRow( layout, "Fan marker type", type );
            }
            String voiceFeature = nav1.getString( nav1.getColumnIndex( Nav1.VOICE_FEATURE ) );
            addRow( layout, "Voice feature", voiceFeature.equals( "Y" )? "Yes" : "No" );
            String voiceIdent = nav1.getString( nav1.getColumnIndex( Nav1.AUTOMATIC_VOICE_IDENT ) );
            addRow( layout, "Voice ident", voiceIdent.equals( "Y" )? "Yes" : "No" );
            Cursor com = result[ 2 ];
            if ( com != null && com.moveToFirst() ) {
                do {
                    String outletType = com.getString( com.getColumnIndex( Com.COMM_OUTLET_TYPE ) );
                    String fssName = com.getString( com.getColumnIndex( Com.FSS_NAME ) );
                    String freqs = com.getString( com.getColumnIndex( Com.COMM_OUTLET_FREQS ) );
                    String outletName = fssName+" Radio ("+outletType+")";
                    int i =0;
                    while ( i < freqs.length() ) {
                        int end = Math.min( i+9, freqs.length() );
                        String fssFreq = freqs.substring( i, end ).trim();
                        addRow( layout, outletName, fssFreq );
                        i = end;
                    }
                } while ( com.moveToNext() );
            }
            String navaidId = nav1.getString( nav1.getColumnIndex( Nav1.NAVAID_ID ) );
            Intent intent = new Intent( getActivity(), NavaidNotamActivity.class );
            intent.putExtra( Nav1.NAVAID_ID, navaidId );
            intent.putExtra( Nav1.NAVAID_TYPE, navaidType );
            addClickableRow( layout, "NOTAMs", intent, R.drawable.row_selector_bottom );
        }

        protected void showNavaidRemarks( Cursor[] result ) {
            Cursor nav2 = result[ 1 ];
            LinearLayout layout = (LinearLayout) findViewById( R.id.navaid_remarks );

            if ( nav2.moveToFirst() ) {
                do {
                    String remark = nav2.getString( nav2.getColumnIndex( Nav2.REMARK_TEXT ) );
                    addBulletedRow( layout, remark );
                } while ( nav2.moveToNext() );
            } else {
                layout.setVisibility( View.GONE );
            }
        }
    }

}
