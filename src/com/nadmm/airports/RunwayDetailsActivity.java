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

import android.app.Activity;
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

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.States;

public class RunwayDetailsActivity extends Activity {

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
        RunwayDetailsTask task = new RunwayDetailsTask();
        task.execute( args );
    }

    private final class RunwayDetailsTask extends AsyncTask<Bundle, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( Bundle... params ) {
            Bundle args = params[ 0 ];
            String siteNumber = args.getString( Runways.SITE_NUMBER );
            String runwayId = args.getString( Runways.RUNWAY_ID );
            
            DatabaseManager dbManager = DatabaseManager.instance( getApplicationContext() );
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 2 ];

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME+" a INNER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            Cursor c = builder.query( db, new String[] { "*" }, Airports.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            if ( !c.moveToFirst() ) {
                return null;
            }
            cursors[ 0 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Runways.SITE_NUMBER+"=? AND "+Runways.RUNWAY_ID+"=?",
                    new String[] { siteNumber, runwayId }, null, null, null, null );
            if ( !c.moveToFirst() ) {
                return null;
            }
            cursors[ 1 ] = c;

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            setProgressBarIndeterminateVisibility( false );
            if ( result == null ) {
                // TODO: Show an error here
                return;
            }

            View view = mInflater.inflate( R.layout.runway_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.runway_detail_layout);

            Cursor apt = result[ 0 ];
            Cursor rwy = result[ 1 ];

            // Title
            GuiUtils.showAirportTitle( mMainLayout, apt );
            // General information
            showGeneralInformation( rwy );
            // Base end information
            showBaseEndInformation( apt, rwy );
            // Reciprocal end information
            showReciprocalEndInformation( apt, rwy );

            // Cleanup cursors
            for ( Cursor c : result ) {
                c.close();
            }
        }

    }

    protected void showGeneralInformation( Cursor c ) {
        String runwayId = c.getString( c.getColumnIndex( Runways.RUNWAY_ID ) );
        TextView tv = (TextView) mMainLayout.findViewById( R.id.runway_general_label );
        tv.setText( "Runway "+runwayId );

        TableLayout generalLayout = (TableLayout) mMainLayout.findViewById(
                R.id.runway_general_layout );
        String length = c.getString( c.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        String width = c.getString( c.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        addRow( generalLayout, "Dimensions", length+"' x "+width+"'" );
        addSeparator( generalLayout );
        String surfaceType = c.getString( c.getColumnIndex( Runways.SURFACE_TYPE ) );
        addRow( generalLayout, "Surface type", DataUtils.decodeSurfaceType( surfaceType ) );
        addSeparator( generalLayout );
        String surfaceTreat = c.getString( c.getColumnIndex( Runways.SURFACE_TREATMENT ) );
        addRow( generalLayout, "Surface treatment",
                DataUtils.decodeSurfaceTreatment( surfaceTreat ) );
        addSeparator( generalLayout );
        String edgeLights = c.getString( c.getColumnIndex( Runways.EDGE_LIGHTS_INTENSITY ) );
        addRow( generalLayout, "Edge lights", DataUtils.decodeRunwayEdgeLights( edgeLights ) );
    }

    protected void showBaseEndInformation( Cursor apt, Cursor rwy ) {
        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ID ) );
        TextView tv = (TextView) mMainLayout.findViewById( R.id.runway_base_end_label );
        tv.setText( "Runway "+runwayId );

        TableLayout baseEndLayout = (TableLayout) mMainLayout.findViewById(
                R.id.runway_base_end_layout );
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_HEADING ) );
        String variation = apt.getString( apt.getColumnIndex(
                Airports.MAGNETIC_VARIATION_DEGREES ) );
        int magneticHeading = heading+DataUtils.getMagneticVariation( variation );
        addRow( baseEndLayout, "Magnetic heading", String.format( "%03d\u00B0",
                magneticHeading ) );
        addSeparator( baseEndLayout );
        String rhPattern = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_RIGHT_TRAFFIC ) );
        addRow( baseEndLayout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );
        double gradient = rwy.getDouble( rwy.getColumnIndex( Runways.BASE_END_GRADIENT ) );
        if ( gradient > 0 ) {
            String gradientString = String.format( "%.1f%%", gradient );
            String gradientDir = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_GRADIENT_DIRECTION ) );
            if ( gradientDir.length() > 0 ) {
                gradientString += " "+gradientDir;
            }
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "Gradient", gradientString );
        }
        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "ILS type", ilsType );
        }
        String arrestingDevice = rwy.getString( rwy.getColumnIndex( 
                Runways.BASE_END_ARRESTING_DEVICE_TYPE ) );
        if ( arrestingDevice.length() > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "Arresting device", arrestingDevice );
        }
        String apchLights = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_APCH_LIGHT_SYSTEM ) );
        if ( apchLights.length() > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "Approach lights", apchLights );
        }
        String marking = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_MARKING_TYPE ) );
        String condition = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_MARKING_CONDITION ) );
        if ( marking.length() > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "Runway marking", DataUtils.decodeRunwayMarking( marking )
                    +", "+DataUtils.decodeRunwayMarkingCondition( condition ) );
        }
        String glideSlope = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_VISUAL_GLIDE_SLOPE ) );
        if ( glideSlope.length() > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "Glideslope", DataUtils.decodeGlideSlope( glideSlope ) );
        }
        double glideAngle = rwy.getDouble( rwy.getColumnIndex(
                Runways.BASE_END_GLIDE_ANGLE ) );
        if ( glideAngle > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "Glide angle",
                    String.format( "%.02f\u00B0", glideAngle ) );
        }
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.BASE_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "Displaced threshold",
                    String.format( "%d'", displacedThreshold ) );
        }
        int tora = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TORA ) );
        int toda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TODA ) );
        if ( tora > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "TORA/TODA", String.format( "%d'/%d'", tora, toda ) );
        }
        int lda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_LDA ) );
        int asda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_ASDA ) );
        if ( lda > 0 ) {
            addSeparator( baseEndLayout );
            addRow( baseEndLayout, "LDA/ASDA", String.format( "%d'/%d'", lda, asda ) );
        }
        int lahso = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_LAHSO_DISTANCE ) );
        String lahsoRunway = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_LAHSO_RUNWAY ) );
        if ( lahso > 0 ) {
            addSeparator( baseEndLayout );
            if ( lahsoRunway.length() > 0 ) {
                addRow( baseEndLayout, "LAHSO distance",
                        String.format( "%d' to %s", lahso, lahsoRunway ) );
            } else {
                addRow( baseEndLayout, "LAHSO distance", String.format( "%d'", lahso ) );
            }
        }
    }

    protected void showReciprocalEndInformation( Cursor apt, Cursor rwy ) {
        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ID ) );
        TextView tv = (TextView) mMainLayout.findViewById( R.id.runway_reciprocal_end_label );
        tv.setText( "Runway "+runwayId );

        TableLayout reciprocalEndLayout = (TableLayout) mMainLayout.findViewById(
                R.id.runway_reciprocal_end_layout );
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_HEADING ) );
        String variation = apt.getString( apt.getColumnIndex(
                Airports.MAGNETIC_VARIATION_DEGREES ) );
        int magneticHeading = heading+DataUtils.getMagneticVariation( variation );
        addRow( reciprocalEndLayout, "Magnetic heading", String.format( "%03d\u00B0",
                magneticHeading ) );
        addSeparator( reciprocalEndLayout );
        String rhPattern = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_RIGHT_TRAFFIC ) );
        addRow( reciprocalEndLayout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );
        double gradient = rwy.getDouble( rwy.getColumnIndex( Runways.BASE_END_GRADIENT ) );
        if ( gradient > 0 ) {
            String gradientString = String.format( "%.1f%%", gradient );
            String gradientDir = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_GRADIENT_DIRECTION ) );
            if ( gradientDir.length() > 0 ) {
                gradientString += " "+gradientDir;
            }
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "Gradient", gradientString );
        }
        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "ILS type", ilsType );
        }
        String arrestingDevice = rwy.getString( rwy.getColumnIndex( 
                Runways.RECIPROCAL_END_ARRESTING_DEVICE_TYPE ) );
        if ( arrestingDevice.length() > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "Arresting device", arrestingDevice );
        }
        String apchLights = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_APCH_LIGHT_SYSTEM ) );
        if ( apchLights.length() > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "Approach lights", apchLights );
        }
        String marking = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_MARKING_TYPE ) );
        String condition = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_MARKING_CONDITION ) );
        if ( marking.length() > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "Runway marking", DataUtils.decodeRunwayMarking( marking )
                    +", "+DataUtils.decodeRunwayMarkingCondition( condition ) );
        }
        String glideSlope = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_VISUAL_GLIDE_SLOPE ) );
        if ( glideSlope.length() > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "Glideslope", DataUtils.decodeGlideSlope( glideSlope ) );
        }
        double glideAngle = rwy.getDouble( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_GLIDE_ANGLE ) );
        if ( glideAngle > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "Glide angle",
                    String.format( "%.02f\u00B0", glideAngle ) );
        }
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "Displaced threshold",
                    String.format( "%d'", displacedThreshold ) );
        }
        int tora = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TORA ) );
        int toda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TODA ) );
        if ( tora > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "TORA/TODA", String.format( "%d'/%d'", tora, toda ) );
        }
        int lda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_LDA ) );
        int asda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_ASDA ) );
        if ( lda > 0 ) {
            addSeparator( reciprocalEndLayout );
            addRow( reciprocalEndLayout, "LDA/ASDA", String.format( "%d'/%d'", lda, asda ) );
        }
        int lahso = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_LAHSO_DISTANCE ) );
        String lahsoRunway = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_LAHSO_RUNWAY ) );
        if ( lahso > 0 ) {
            addSeparator( reciprocalEndLayout );
            if ( lahsoRunway.length() > 0 ) {
                addRow( reciprocalEndLayout, "LAHSO distance",
                        String.format( "%d' to %s", lahso, lahsoRunway ) );
            } else {
                addRow( reciprocalEndLayout, "LAHSO distance", String.format( "%d'", lahso ) );
            }
        }
    }

    protected void addRow( TableLayout table, String label, String value ) {
        TableRow row = new TableRow( this );
        row.setPadding( 8, 8, 8, 8 );
        TextView tvLabel = new TextView( this );
        tvLabel.setText( label );
        tvLabel.setSingleLine();
        tvLabel.setGravity( Gravity.LEFT );
        tvLabel.setPadding( 4, 4, 2, 4 );
        row.addView( tvLabel, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        TextView tvValue = new TextView( this );
        tvValue.setText( value );
        tvValue.setMarqueeRepeatLimit( -1 );
        tvValue.setGravity( Gravity.RIGHT );
        tvLabel.setPadding( 4, 4, 2, 4 );
        row.addView( tvValue, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( TableLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, 
                new TableLayout.LayoutParams( TableLayout.LayoutParams.FILL_PARENT, 1 ) );
    }

}
