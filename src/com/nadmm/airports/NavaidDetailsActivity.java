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
import android.widget.Toast;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Nav1;
import com.nadmm.airports.DatabaseManager.Nav2;
import com.nadmm.airports.DatabaseManager.States;

public class NavaidDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String navaidId = intent.getStringExtra( Nav1.NAVAID_ID );
        String type = intent.getStringExtra( Nav1.NAVAID_TYPE );

        NavaidDetailsTask task = new NavaidDetailsTask();
        task.execute( navaidId, type );
    }

    private final class NavaidDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String navaidId = params[ 0 ];
            String type = params[ 1 ];

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 2 ];

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Nav1.TABLE_NAME+" a LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Nav1.ASSOC_STATE+"=s."+States.STATE_CODE );
            Cursor c = builder.query( db, new String[] { "*" },
                    Nav1.NAVAID_ID+"=? AND "+Nav1.NAVAID_TYPE+"=?",
                    new String[] { navaidId, type }, null, null, null, null );
            if ( !c.moveToFirst() ) {
                return null;
            }

            cursors[ 0 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Nav2.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Nav1.NAVAID_ID+"=? AND "+Nav1.NAVAID_TYPE+"=?",
                    new String[] { navaidId, type }, null, null, null, null );
            cursors[ 1 ] = c;

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );

            if ( result == null ) {
                Toast.makeText( getApplicationContext(), "Navaid not found", Toast.LENGTH_LONG );
                NavaidDetailsActivity.this.finish();
                return;
            }

            View view = mInflater.inflate( R.layout.navaid_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.navaid_top_layout );

            // Title
            Cursor nav1 = result[ 0 ];
            showNavaidTitle( mMainLayout, nav1 );

            showNavaidDetails( result );
            showNavaidRemarks( result );

            // Cleanup cursors
            for ( Cursor c : result ) {
                if ( c != null ) {
                    c.close();
                }
            }
        }

    }

    protected void showNavaidDetails( Cursor[] result ) {
        Cursor nav1 = result[ 0 ];
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.navaid_details );
        String navaidClass = nav1.getString( nav1.getColumnIndex( Nav1.NAVAID_CLASS ) );
        addRow( layout, "Class", navaidClass );
        double freq = nav1.getDouble( nav1.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
        String tacan = nav1.getString( nav1.getColumnIndex( Nav1.TACAN_CHANNEL ) );
        if ( freq == 0 ) {
            freq = DataUtils.getTacanChannelFrequency( tacan );
        }
        if ( freq > 0 ) {
            addSeparator( layout );
            if ( ( freq%1.0 ) > 0 ) {
                addRow( layout, "Frequency", String.format( "%.2f", freq ) );
            } else {
                addRow( layout, "Frequency", String.format( "%.0f", freq ) );
            }
        }
        String power = nav1.getString( nav1.getColumnIndex( Nav1.POWER_OUTPUT ) );
        if ( power.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Power output", power+" Watts" );
        }
        if ( tacan.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Tacan channel", tacan );
        }
        String magVar = nav1.getString( nav1.getColumnIndex( Nav1.MAGNETIC_VARIATION_DEGREES ) );
        if ( magVar.length() > 0 ) {
            String magDir = nav1.getString( nav1.getColumnIndex( 
                    Nav1.MAGNETIC_VARIATION_DIRECTION ) );
            String magYear = nav1.getString( nav1.getColumnIndex( 
                    Nav1.MAGNETIC_VARIATION_YEAR ) );
            addSeparator( layout );
            addRow( layout, "Magnetic variation", String.format( "%d\u00B0%s (%s)",
                    Integer.valueOf( magVar ), magDir, magYear ) );
        }
        String alt = nav1.getString( nav1.getColumnIndex( Nav1.PROTECTED_FREQUENCY_ALTITUDE ) );
        if ( alt.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Service volume", DataUtils.decodeNavProtectedAltitude( alt ) );
        }
        String hours = nav1.getString( nav1.getColumnIndex( Nav1.OPERATING_HOURS ) );
        addSeparator( layout );
        addRow( layout, "Operating hours", hours );
        String voiceFeature = nav1.getString( nav1.getColumnIndex( Nav1.VOICE_FEATURE ) );
        addSeparator( layout );
        addRow( layout, "Voice feature", voiceFeature.equals( "Y" )? "Yes" : "No" );
        String voiceIdent = nav1.getString( nav1.getColumnIndex( Nav1.AUTOMATIC_VOICE_IDENT ) );
        addSeparator( layout );
        addRow( layout, "Voice ident", voiceIdent.equals( "Y" )? "Yes" : "No" );
    }

    protected void showNavaidRemarks( Cursor[] result ) {
        Cursor nav2 = result[ 1 ];
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById( R.id.navaid_remarks );

        if ( nav2.moveToFirst() ) {
            do {
                String remark = nav2.getString( nav2.getColumnIndex( Nav2.REMARK_TEXT ) );
                addBulletedRow( layout, remark );
            } while ( nav2.moveToNext() );
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
