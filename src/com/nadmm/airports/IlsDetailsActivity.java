/*
 * Airports for Android
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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Ils1;
import com.nadmm.airports.DatabaseManager.Ils2;

public class IlsDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        ILSDetailsTask task = new ILSDetailsTask();
        task.execute( args );
    }

    private final class ILSDetailsTask extends AsyncTask<Bundle, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( Bundle... params ) {
            Bundle args = params[ 0 ];
            String siteNumber = args.getString( Ils1.SITE_NUMBER );
            String runwayId = args.getString( Ils1.RUNWAY_ID );
            String ilsType = args.getString( Ils1.ILS_TYPE );

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 3 ];

            Cursor apt = mDbManager.getAirportDetails( siteNumber );
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

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );

            View view = mInflater.inflate( R.layout.ils_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.ils_top_layout );

            Cursor apt = result[ 0 ];
            // Title
            showAirportTitle( mMainLayout, apt );

            Cursor ils1 = result[ 1 ];
            if ( !ils1.moveToFirst() ) {
                return;
            }

            showIlsDetails( result );
            showLocalizerDetails( result );
            showGlideslopeDetails( result );
            showInnerMarkerDetails( result );
            showMiddleMarkerDetails( result );
            showOuterMarkerDetails( result );
            showIlsRemarks( result );

            // Cleanup cursors
            for ( Cursor c : result ) {
                if ( c != null ) {
                    c.close();
                }
            }
        }

    }

    protected void showIlsDetails( Cursor[] result ) {
        Cursor ils1 = result[ 1 ];
        TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_ils_label );
        String rwyId = ils1.getString( ils1.getColumnIndex( Ils1.RUNWAY_ID ) );
        tv.setText( "Runway "+rwyId );
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_ils_details );
        String ilsType = ils1.getString( ils1.getColumnIndex( Ils1.ILS_TYPE ) );
        addRow( layout, "Type", ilsType );
        addSeparator( layout );
        String category = ils1.getString( ils1.getColumnIndex( Ils1.ILS_CATEGORY ) );
        if ( category.length() > 0 ) {
            addRow( layout, "Category", category );
            addSeparator( layout );
        }
        String bearing = ils1.getString( ils1.getColumnIndex( Ils1.ILS_MAGNETIC_BEARING ) );
        addRow( layout, "Magnetic bearing", bearing+"\u00B0" );
    }

    protected void showLocalizerDetails( Cursor result[] ) {
        Cursor ils1 = result[ 1 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_loc_details );
        String locType = ils1.getString( ils1.getColumnIndex( Ils1.LOCALIZER_TYPE ) );
        addRow( layout, "Type", locType );
        addSeparator( layout );
        String locId = ils1.getString( ils1.getColumnIndex( Ils1.LOCALIZER_ID ) );
        addRow( layout, "Id", locId );
        addSeparator( layout );
        String locFreq = ils1.getString( ils1.getColumnIndex( Ils1.LOCALIZER_FREQUENCY ) );
        addRow( layout, "Frequency", locFreq );
        addSeparator( layout );
        Double locWidth = ils1.getDouble( ils1.getColumnIndex( Ils1.LOCALIZER_COURSE_WIDTH ) );
        addRow( layout, "Course width", String.format( "%.02f\u00B0", locWidth ) );
    }

    protected void showGlideslopeDetails( Cursor result[] ) {
        Cursor ils1 = result[ 1 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_gs_details );
        String gsType = ils1.getString( ils1.getColumnIndex( Ils1.GLIDE_SLOPE_TYPE ) );
        if ( gsType.length() > 0 ) {
            addRow( layout, "Type", gsType );
            addSeparator( layout );
            Double gsAngle = ils1.getDouble( ils1.getColumnIndex( Ils1.GLIDE_SLOPE_ANGLE ) );
            addRow( layout, "Angle", String.format( "%.02f\u00B0", gsAngle ) );
            addSeparator( layout );
            String gsFreq = ils1.getString( ils1.getColumnIndex( Ils1.GLIDE_SLOPE_FREQUENCY ) );
            addRow( layout, "Frequency", gsFreq );
        } else {
            TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_gs_label );
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showInnerMarkerDetails( Cursor result[] ) {
        Cursor ils1 = result[ 1 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_im_details );
        String imType = ils1.getString( ils1.getColumnIndex( Ils1.INNER_MARKER_TYPE ) );
        if ( imType.length() > 0 ) {
            addRow( layout, "Type", imType );
            addSeparator( layout );
            int imDistance = ils1.getInt( ils1.getColumnIndex( Ils1.INNER_MARKER_DISTANCE ) );
            addRow( layout, "Distance", String.format( "%d'", imDistance ) );
        } else {
            TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_im_label );
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showMiddleMarkerDetails( Cursor result[] ) {
        Cursor ils1 = result[ 1 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_mm_details );
        String mmType = ils1.getString( ils1.getColumnIndex( Ils1.MIDDLE_MARKER_TYPE ) );
        if ( mmType.length() > 0 ) {
            addRow( layout, "Type", mmType );
            addSeparator( layout );
            String mmId = ils1.getString( ils1.getColumnIndex( Ils1.MIDDLE_MARKER_ID ) );
            if ( mmId.length() > 0 ) {
                addRow( layout, "Id", mmId );
                addSeparator( layout );
            }
            String mmName = ils1.getString( ils1.getColumnIndex( Ils1.MIDDLE_MARKER_NAME ) );
            if ( mmName.length() > 0 ) {
                addRow( layout, "Name", mmName );
                addSeparator( layout );
            }
            String mmFreq = ils1.getString( ils1.getColumnIndex( Ils1.MIDDLE_MARKER_FREQUENCY ) );
            if ( mmFreq.length() > 0 ) {
                addRow( layout, "Frequency", mmFreq );
                addSeparator( layout );
            }
            int mmDistance = ils1.getInt( ils1.getColumnIndex( Ils1.MIDDLE_MARKER_DISTANCE ) );
            addRow( layout, "Distance", String.format( "%d'", mmDistance ) );
        } else {
            TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_mm_label );
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showOuterMarkerDetails( Cursor result[] ) {
        Cursor ils1 = result[ 1 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_om_details );
        String omType = ils1.getString( ils1.getColumnIndex( Ils1.OUTER_MARKER_TYPE ) );
        if ( omType.length() > 0 ) {
            addRow( layout, "Type", omType );
            addSeparator( layout );
            String omId = ils1.getString( ils1.getColumnIndex( Ils1.OUTER_MARKER_ID ) );
            if ( omId.length() > 0 ) {
                addRow( layout, "Id", omId );
                addSeparator( layout );
            }
            String omName = ils1.getString( ils1.getColumnIndex( Ils1.OUTER_MARKER_NAME ) );
            if ( omName.length() > 0 ) {
                addRow( layout, "Name", omName );
                addSeparator( layout );
            }
            String omFreq = ils1.getString( ils1.getColumnIndex( Ils1.OUTER_MARKER_FREQUENCY ) );
            if ( omFreq.length() > 0 ) {
                addRow( layout, "Frequency", omFreq );
                addSeparator( layout );
            }
            int omDistance = ils1.getInt( ils1.getColumnIndex( Ils1.OUTER_MARKER_DISTANCE ) );
            addRow( layout, "Distance", String.format( "%d'", omDistance ) );
        } else {
            TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_om_label );
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }
    }

    protected void showIlsRemarks( Cursor result[] ) {
        Cursor ils2 = result[ 2 ];
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.rwy_ils_remarks );
        if ( ils2.moveToFirst() ) {
            do {
                String remark = ils2.getString( ils2.getColumnIndex( Ils2.ILS_REMARKS ) );
                addBulletedRow( layout, remark );
            } while ( ils2.moveToNext() );
        } else {
            layout.setVisibility( View.GONE );
        }
    }

    protected void addRow( TableLayout table, String label, String text ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tvLabel = new TextView( this );
        tvLabel.setText( label );
        tvLabel.setSingleLine();
        tvLabel.setGravity( Gravity.LEFT );
        tvLabel.setPadding( 4, 4, 2, 4 );
        row.addView( tvLabel, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        TextView tvValue = new TextView( this );
        tvValue.setText( text );
        tvValue.setGravity( Gravity.RIGHT );
        tvValue.setPadding( 4, 4, 2, 4 );
        row.addView( tvValue, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addBulletedRow( LinearLayout layout, String remark ) {
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( remark );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( TableLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, 
                new TableLayout.LayoutParams( TableLayout.LayoutParams.FILL_PARENT, 1 ) );
    }

}
