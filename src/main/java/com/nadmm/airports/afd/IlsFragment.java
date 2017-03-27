/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Ils1;
import com.nadmm.airports.data.DatabaseManager.Ils2;
import com.nadmm.airports.data.DatabaseManager.Ils3;
import com.nadmm.airports.data.DatabaseManager.Ils4;
import com.nadmm.airports.data.DatabaseManager.Ils5;
import com.nadmm.airports.data.DatabaseManager.Ils6;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;

public final class IlsFragment extends FragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.ils_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( "ILS details", "" );

        Bundle args = getArguments();
        String siteNumber = args.getString( Ils1.SITE_NUMBER );
        String runwayId = args.getString( Ils1.RUNWAY_ID );
        String ilsType = args.getString( Ils1.ILS_TYPE );
        String icaoCode = args.getString( Airports.ICAO_CODE );

        getActivityBase().faLogViewItem( "ils", icaoCode, runwayId );

        setBackgroundTask( new IlsTask() ).execute( siteNumber, runwayId, ilsType );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        showAirportTitle( apt );

        Cursor ils1 = result[ 1 ];
        if ( ils1.moveToFirst() ) {
            showIlsDetails( result );
            showLocalizerDetails( result );
            showGlideslopeDetails( result );
            showInnerMarkerDetails( result );
            showMiddleMarkerDetails( result );
            showOuterMarkerDetails( result );
            showIlsRemarks( result );
        } else {
            setContentMsg( "ILS details not found" );
        }

        setFragmentContentShown( true );
    }

    protected void showIlsDetails( Cursor[] result ) {
        Cursor ils1 = result[ 1 ];
        TextView tv = (TextView) findViewById( R.id.rwy_ils_label );
        String rwyId = ils1.getString( ils1.getColumnIndex( Ils1.RUNWAY_ID ) );
        tv.setText( "Runway "+rwyId );
        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_ils_details );
        String ilsType = ils1.getString( ils1.getColumnIndex( Ils1.ILS_TYPE ) );
        addRow( layout, "Type", ilsType );
        String locId = ils1.getString( ils1.getColumnIndex( Ils1.ILS_ID ) );
        addRow( layout, "Id", locId );
        String category = ils1.getString( ils1.getColumnIndex( Ils1.ILS_CATEGORY ) );
        if ( category.length() > 0 ) {
            addRow( layout, "Category", category );
        }
        String bearing = ils1.getString( ils1.getColumnIndex( Ils1.ILS_MAGNETIC_BEARING ) );
        addRow( layout, "Magnetic bearing", bearing+"\u00B0" );
    }

    protected void showLocalizerDetails( Cursor[] result ) {
        Cursor ils2 = result[ 2 ];
        if ( ils2.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_loc_details );
            String locFreq = ils2.getString( ils2.getColumnIndex( Ils2.LOCALIZER_FREQUENCY ) );
            addRow( layout, "Frequency", locFreq );
            float locWidth = ils2.getFloat( ils2.getColumnIndex( Ils2.LOCALIZER_COURSE_WIDTH ) );
            addRow( layout, "Course width", FormatUtils.formatDegrees( locWidth ) );
            String back = ils2.getString( ils2.getColumnIndex( Ils2.LOCALIZER_BACK_COURSE_STATUS ) );
            if ( back.length() > 0 ) {
                addRow( layout, "Back course", back );
            }
            String status = ils2.getString( ils2.getColumnIndex( Ils2.OPERATIONAL_STATUS ) );
            String date = ils2.getString( ils2.getColumnIndex( Ils2.OPERATIONAL_EFFECTIVE_DATE ) );
            addRow( layout, "Status", status, date );
        } else {
            TextView tv = (TextView) findViewById( R.id.rwy_loc_label );
            tv.setVisibility( View.GONE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_loc_details );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showGlideslopeDetails( Cursor[] result ) {
        Cursor ils3 = result[ 3 ];
        if ( ils3.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_gs_details );
            String gsType = ils3.getString( ils3.getColumnIndex( Ils3.GLIDE_SLOPE_TYPE ) );
            addRow( layout, "Type", gsType );
            float gsAngle = ils3.getFloat( ils3.getColumnIndex( Ils3.GLIDE_SLOPE_ANGLE ) );
            addRow( layout, "Glide angle", FormatUtils.formatDegrees( gsAngle ) );
            String gsFreq = ils3.getString( ils3.getColumnIndex( Ils3.GLIDE_SLOPE_FREQUENCY ) );
            addRow( layout, "Frequency", gsFreq );
            String status = ils3.getString( ils3.getColumnIndex( Ils2.OPERATIONAL_STATUS ) );
            String date = ils3.getString( ils3.getColumnIndex( Ils2.OPERATIONAL_EFFECTIVE_DATE ) );
            addRow( layout, "Status", status, date );
        } else {
            TextView tv = (TextView) findViewById( R.id.rwy_gs_label );
            tv.setVisibility( View.GONE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_gs_details );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showInnerMarkerDetails( Cursor[] result ) {
        Cursor ils5 = result[ 5 ];
        if ( ils5.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_im_details );
            String imType = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_TYPE ) );
            addRow( layout, "Type", imType );
            int distance = ils5.getInt( ils5.getColumnIndex( Ils5.MARKER_DISTANCE ) );
            addRow( layout, "Distance", FormatUtils.formatFeet( distance ) );
            String status = ils5.getString( ils5.getColumnIndex( Ils2.OPERATIONAL_STATUS ) );
            String date = ils5.getString( ils5.getColumnIndex( Ils2.OPERATIONAL_EFFECTIVE_DATE ) );
            addRow( layout, "Status", status, date );
        } else {
            TextView tv = (TextView) findViewById( R.id.rwy_im_label );
            tv.setVisibility( View.GONE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_im_details );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showMiddleMarkerDetails( Cursor[] result ) {
        Cursor ils5 = result[ 6 ];
        if ( ils5.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_mm_details );
            String mmType = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_TYPE ) );
            addRow( layout, "Type", mmType );
            String mmId = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_BEACON_ID ) );
            if ( mmId.length() > 0 ) {
                addRow( layout, "Id", mmId );
            }
            String mmName = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_BEACON_NAME ) );
            if ( mmName.length() > 0 ) {
                addRow( layout, "Name", mmName );
            }
            String mmFreq = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_BEACON_FREQUENCY ) );
            if ( mmFreq.length() > 0 ) {
                addRow( layout, "Frequency", mmFreq );
            }
            int mmDistance = ils5.getInt( ils5.getColumnIndex( Ils5.MARKER_DISTANCE ) );
            addRow( layout, "Distance", FormatUtils.formatFeet( mmDistance ) );
            String status = ils5.getString( ils5.getColumnIndex( Ils2.OPERATIONAL_STATUS ) );
            String date = ils5.getString( ils5.getColumnIndex( Ils2.OPERATIONAL_EFFECTIVE_DATE ) );
            addRow( layout, "Status", status, date );
        } else {
            TextView tv = (TextView) findViewById( R.id.rwy_mm_label );
            tv.setVisibility( View.GONE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_mm_details );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showOuterMarkerDetails( Cursor[] result ) {
        Cursor ils5 = result[ 7 ];
        if ( ils5.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_om_details );
            String omType = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_TYPE ) );
            addRow( layout, "Type", omType );
            String omId = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_BEACON_ID ) );
            if ( omId.length() > 0 ) {
                addRow( layout, "Id", omId );
            }
            String omName = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_BEACON_NAME ) );
            if ( omName.length() > 0 ) {
                addRow( layout, "Name", omName );
            }
            String omFreq = ils5.getString( ils5.getColumnIndex( Ils5.MARKER_BEACON_FREQUENCY ) );
            if ( omFreq.length() > 0 ) {
                addRow( layout, "Frequency", omFreq );
            }
            int omDistance = ils5.getInt( ils5.getColumnIndex( Ils5.MARKER_DISTANCE ) );
            addRow( layout, "Distance", FormatUtils.formatFeet( omDistance ) );
            String status = ils5.getString( ils5.getColumnIndex( Ils2.OPERATIONAL_STATUS ) );
            String date = ils5.getString( ils5.getColumnIndex( Ils2.OPERATIONAL_EFFECTIVE_DATE ) );
            addRow( layout, "Status", status, date );
        } else {
            TextView tv = (TextView) findViewById( R.id.rwy_om_label );
            tv.setVisibility( View.GONE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_om_details );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showIlsRemarks( Cursor[] result ) {
        Cursor ils6 = result[ 8 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_ils_remarks );
        if ( ils6.moveToFirst() ) {
            do {
                String remark = ils6.getString( ils6.getColumnIndex( Ils6.ILS_REMARKS ) );
                addBulletedRow( layout, remark );
            } while ( ils6.moveToNext() );
        } else {
            layout.setVisibility( View.GONE );
        }
    }

    private final class IlsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            String runwayId = params[ 1 ];
            String ilsType = params[ 2 ];

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 9 ];

            Cursor apt = getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Ils1.TABLE_NAME );
            cursors[ 1 ] = builder.query( db, new String[] { "*" },
                    Ils1.SITE_NUMBER+"=? AND "+Ils1.RUNWAY_ID+"=? AND "+Ils1.ILS_TYPE+"=?",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Ils2.TABLE_NAME );
            cursors[ 2 ] = builder.query( db, new String[] { "*" },
                    Ils2.SITE_NUMBER+"=? AND "+Ils2.RUNWAY_ID+"=? AND "+Ils2.ILS_TYPE+"=?",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Ils3.TABLE_NAME );
            cursors[ 3 ] = builder.query( db, new String[] { "*" },
                    Ils3.SITE_NUMBER+"=? AND "+Ils3.RUNWAY_ID+"=? AND "+Ils3.ILS_TYPE+"=?",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Ils4.TABLE_NAME );
            cursors[ 4 ] = builder.query( db, new String[] { "*" },
                    Ils4.SITE_NUMBER+"=? AND "+Ils4.RUNWAY_ID+"=? AND "+Ils4.ILS_TYPE+"=?",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Ils5.TABLE_NAME );
            cursors[ 5 ] = builder.query( db, new String[] { "*" },
                    Ils5.SITE_NUMBER+"=? AND "+Ils5.RUNWAY_ID+"=? AND "+Ils5.ILS_TYPE+"=?"
                    +" AND "+Ils5.MARKER_TYPE+"='IM'",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Ils5.TABLE_NAME );
            cursors[ 6 ] = builder.query( db, new String[] { "*" },
                    Ils5.SITE_NUMBER+"=? AND "+Ils5.RUNWAY_ID+"=? AND "+Ils5.ILS_TYPE+"=?"
                    +" AND "+Ils5.MARKER_TYPE+"='MM'",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Ils5.TABLE_NAME );
            cursors[ 7 ] = builder.query( db, new String[] { "*" },
                    Ils5.SITE_NUMBER+"=? AND "+Ils5.RUNWAY_ID+"=? AND "+Ils5.ILS_TYPE+"=?"
                    +" AND "+Ils5.MARKER_TYPE+"='OM'",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Ils6.TABLE_NAME );
            cursors[ 8 ] = builder.query( db, new String[] { "*" },
                    Ils6.SITE_NUMBER+"=? AND "+Ils6.RUNWAY_ID+"=? AND "+Ils6.ILS_TYPE+"=?",
                    new String[] { siteNumber, runwayId, ilsType }, null, null, null, null );

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

}
