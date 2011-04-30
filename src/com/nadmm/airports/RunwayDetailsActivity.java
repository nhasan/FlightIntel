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
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;

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
            Cursor[] cursors = new Cursor[ 3 ];
            
            DatabaseManager dbManager = DatabaseManager.instance( getApplicationContext() );
            cursors[ 0 ] = dbManager.getAirportDetails( siteNumber );

            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "*" },
                    Runways.SITE_NUMBER+"=? AND "+Runways.RUNWAY_ID+"=?",
                    new String[] { siteNumber, runwayId }, null, null, null, null );
            cursors[ 1 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            c = builder.query( db, new String[] { Remarks.REMARK_NAME, Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 2) in ('A3', 'A4', 'A5', 'A6')",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 2 ] = c;

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
            mMainLayout = (LinearLayout) view.findViewById( R.id.rwy_top_layout );

            Cursor apt = result[ 0 ];
            // Title
            GuiUtils.showAirportTitle( mMainLayout, apt );

            Cursor rwy = result[ 1 ];
            if ( !rwy.moveToFirst() ) {
                return;
            }

            String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
            boolean isHelipad = runwayId.startsWith( "H" );

            if ( isHelipad ) {
                // Helipad information
                showHelipadInformation( result );
            } else {
                // Common information
                showCommonInformation( result );
                // Base end information
                showBaseEndInformation( result );
                // Reciprocal end information
                showReciprocalEndInformation( result );
            }

            // Cleanup cursors
            for ( Cursor c : result ) {
                c.close();
            }
        }

    }

    protected void showCommonInformation( Cursor[] result ) {
        // Common runway information
        Cursor rwy = result[ 1 ];
        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
        TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_common_label );
        tv.setText( "Runway "+runwayId );
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_common_details );
        String length = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        String width = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        addRow( layout, "Dimensions", length+"' x "+width+"'" );
        addSeparator( layout );
        String surfaceType = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TYPE ) );
        addRow( layout, "Surface type", DataUtils.decodeSurfaceType( surfaceType ) );
        addSeparator( layout );
        String surfaceTreat = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TREATMENT ) );
        addRow( layout, "Surface treatment", DataUtils.decodeSurfaceTreatment( surfaceTreat ) );
        addSeparator( layout );
        String edgeLights = rwy.getString( rwy.getColumnIndex( Runways.EDGE_LIGHTS_INTENSITY ) );
        addRow( layout, "Edge lights", DataUtils.decodeRunwayEdgeLights( edgeLights ) );

        // Show remarks
        showRemarks( R.id.rwy_common_remarks, result, runwayId );
    }

    protected void showBaseEndInformation( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        Cursor rwy = result[ 1 ];

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ID ) );
        TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_base_end_label );
        tv.setText( "Runway "+runwayId );

        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_base_end_details );
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_HEADING ) );
        int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
        String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
        if ( dir.equals( "E" ) ) {
            variation *= -1;
        }
        addRow( layout, "Magnetic heading", String.format( "%03d\u00B0",
                DataUtils.calculateMagneticHeading( heading, variation ) ) );
        addSeparator( layout );
        String rhPattern = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_RIGHT_TRAFFIC ) );
        addRow( layout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );
        double gradient = rwy.getDouble( rwy.getColumnIndex( Runways.BASE_END_GRADIENT ) );
        if ( gradient > 0 ) {
            String gradientString = String.format( "%.1f%%", gradient );
            String gradientDir = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_GRADIENT_DIRECTION ) );
            if ( gradientDir.length() > 0 ) {
                gradientString += " "+gradientDir;
            }
            addSeparator( layout );
            addRow( layout, "Gradient", gradientString );
        }
        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "ILS type", ilsType );
        }
        String arrestingDevice = rwy.getString( rwy.getColumnIndex( 
                Runways.BASE_END_ARRESTING_DEVICE_TYPE ) );
        if ( arrestingDevice.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Arresting device", arrestingDevice );
        }
        String apchLights = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_APCH_LIGHT_SYSTEM ) );
        if ( apchLights.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Approach lights", apchLights );
        }
        String markings = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_MARKING_TYPE ) );
        String condition = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_MARKING_CONDITION ) );
        if ( markings.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Markings", DataUtils.decodeRunwayMarking( markings )
                    +", "+DataUtils.decodeRunwayMarkingCondition( condition ) );
        }
        String glideSlope = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_VISUAL_GLIDE_SLOPE ) );
        if ( glideSlope.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Glideslope", DataUtils.decodeGlideSlope( glideSlope ) );
        }
        double glideAngle = rwy.getDouble( rwy.getColumnIndex(
                Runways.BASE_END_GLIDE_ANGLE ) );
        if ( glideAngle > 0 ) {
            addSeparator( layout );
            addRow( layout, "Glide angle",
                    String.format( "%.02f\u00B0", glideAngle ) );
        }
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.BASE_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addSeparator( layout );
            addRow( layout, "Displaced threshold",
                    String.format( "%d'", displacedThreshold ) );
        }
        int tora = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TORA ) );
        int toda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TODA ) );
        if ( tora > 0 ) {
            addSeparator( layout );
            addRow( layout, "TORA/TODA", String.format( "%d'/%d'", tora, toda ) );
        }
        int lda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_LDA ) );
        int asda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_ASDA ) );
        if ( lda > 0 ) {
            addSeparator( layout );
            addRow( layout, "LDA/ASDA", String.format( "%d'/%d'", lda, asda ) );
        }
        int lahso = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_LAHSO_DISTANCE ) );
        String lahsoRunway = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_LAHSO_RUNWAY ) );
        if ( lahso > 0 ) {
            addSeparator( layout );
            if ( lahsoRunway.length() > 0 ) {
                addRow( layout, "LAHSO distance",
                        String.format( "%d' to %s", lahso, lahsoRunway ) );
            } else {
                addRow( layout, "LAHSO distance", String.format( "%d'", lahso ) );
            }
        }

        // Show remarks
        showRemarks( R.id.rwy_base_end_remarks, result, runwayId );
        showBaseEndObstructions( R.id.rwy_base_end_remarks, result );
    }

    protected void showReciprocalEndInformation( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        Cursor rwy = result[ 1 ];

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ID ) );
        TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_reciprocal_end_label );
        tv.setText( "Runway "+runwayId );

        TableLayout layout = (TableLayout) mMainLayout.findViewById(
                R.id.rwy_reciprocal_end_details );
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_HEADING ) );
        int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
        String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
        if ( dir.equals( "E" ) ) {
            variation *= -1;
        }
        addRow( layout, "Magnetic heading", String.format( "%03d\u00B0",
                DataUtils.calculateMagneticHeading( heading, variation ) ) );
        addSeparator( layout );
        String rhPattern = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_RIGHT_TRAFFIC ) );
        addRow( layout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );
        double gradient = rwy.getDouble( rwy.getColumnIndex( Runways.RECIPROCAL_END_GRADIENT ) );
        if ( gradient > 0 ) {
            String gradientString = String.format( "%.1f%%", gradient );
            String gradientDir = rwy.getString( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_GRADIENT_DIRECTION ) );
            if ( gradientDir.length() > 0 ) {
                gradientString += " "+gradientDir;
            }
            addSeparator( layout );
            addRow( layout, "Gradient", gradientString );
        }
        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "ILS type", ilsType );
        }
        String arrestingDevice = rwy.getString( rwy.getColumnIndex( 
                Runways.RECIPROCAL_END_ARRESTING_DEVICE_TYPE ) );
        if ( arrestingDevice.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Arresting device", arrestingDevice );
        }
        String apchLights = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_APCH_LIGHT_SYSTEM ) );
        if ( apchLights.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Approach lights", apchLights );
        }
        String markings = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_MARKING_TYPE ) );
        String condition = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_MARKING_CONDITION ) );
        if ( markings.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Markings", DataUtils.decodeRunwayMarking( markings )
                    +", "+DataUtils.decodeRunwayMarkingCondition( condition ) );
        }
        String glideSlope = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_VISUAL_GLIDE_SLOPE ) );
        if ( glideSlope.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Glideslope", DataUtils.decodeGlideSlope( glideSlope ) );
        }
        double glideAngle = rwy.getDouble( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_GLIDE_ANGLE ) );
        if ( glideAngle > 0 ) {
            addSeparator( layout );
            addRow( layout, "Glide angle",
                    String.format( "%.02f\u00B0", glideAngle ) );
        }
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addSeparator( layout );
            addRow( layout, "Displaced threshold",
                    String.format( "%d'", displacedThreshold ) );
        }
        int tora = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TORA ) );
        int toda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TODA ) );
        if ( tora > 0 ) {
            addSeparator( layout );
            addRow( layout, "TORA/TODA", String.format( "%d'/%d'", tora, toda ) );
        }
        int lda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_LDA ) );
        int asda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_ASDA ) );
        if ( lda > 0 ) {
            addSeparator( layout );
            addRow( layout, "LDA/ASDA", String.format( "%d'/%d'", lda, asda ) );
        }
        int lahso = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_LAHSO_DISTANCE ) );
        String lahsoRunway = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_LAHSO_RUNWAY ) );
        if ( lahso > 0 ) {
            addSeparator( layout );
            if ( lahsoRunway.length() > 0 ) {
                addRow( layout, "LAHSO distance",
                        String.format( "%d' to %s", lahso, lahsoRunway ) );
            } else {
                addRow( layout, "LAHSO distance", String.format( "%d'", lahso ) );
            }
        }

        // Show remarks
        showRemarks( R.id.rwy_reciprocal_end_remarks, result, runwayId );
        showReciprocalEndObstructions( R.id.rwy_reciprocal_end_remarks, result );
    }

    protected void showHelipadInformation( Cursor[] result ) {
        // Hide the runway sections
        TextView tv = (TextView) mMainLayout.findViewById( R.id.rwy_base_end_label );
        tv.setVisibility( View.GONE );
        TableLayout layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_base_end_details );
        layout.setVisibility( View.GONE );
        tv = (TextView) mMainLayout.findViewById( R.id.rwy_reciprocal_end_label );
        tv.setVisibility( View.GONE );
        layout = (TableLayout) mMainLayout.findViewById( R.id.rwy_reciprocal_end_details );
        layout.setVisibility( View.GONE );

        Cursor rwy = result[ 1 ];
        String helipadId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
        tv = (TextView) mMainLayout.findViewById( R.id.rwy_common_label );
        tv.setText( "Helipad "+helipadId );

        layout = (TableLayout) mMainLayout.findViewById(
                R.id.rwy_common_details );
        String length = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        String width = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        addRow( layout, "Dimensions", length+"' x "+width+"'" );
        addSeparator( layout );
        String surfaceType = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TYPE ) );
        addRow( layout, "Surface type", DataUtils.decodeSurfaceType( surfaceType ) );
        addSeparator( layout );
        String surfaceTreat = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TREATMENT ) );
        addRow( layout, "Surface treatment", DataUtils.decodeSurfaceTreatment( surfaceTreat ) );
        String markings = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_MARKING_TYPE ) );
        String condition = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_MARKING_CONDITION ) );
        if ( markings.length() > 0 ) {
            addSeparator( layout );
            addRow( layout, "Markings", DataUtils.decodeRunwayMarking( markings )
                    +", "+DataUtils.decodeRunwayMarkingCondition( condition ) );
        }
        addSeparator( layout );
        String edgeLights = rwy.getString( rwy.getColumnIndex( Runways.EDGE_LIGHTS_INTENSITY ) );
        addRow( layout, "Edge lights", DataUtils.decodeRunwayEdgeLights( edgeLights ) );
        addSeparator( layout );
        String rhPattern = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_RIGHT_TRAFFIC ) );
        addRow( layout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );

        // Show remarks
        showRemarks( R.id.rwy_base_end_remarks, result, helipadId );
    }

    protected void showRemarks( int resid, Cursor[] result, String runwayId ) {
        Cursor rmk = result[ 2 ];
        if ( rmk.moveToFirst() ) {
            int rmkNum = 0;
            LinearLayout layout = (LinearLayout) mMainLayout.findViewById( resid );
            do {
                String rmkName = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_NAME ) );
                if ( rmkName.endsWith( "-"+runwayId ) ) {
                    ++rmkNum;
                    String rmkText = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                    addRemarkRow( layout, rmkText );
                }
            } while ( rmk.moveToNext() );
            if ( rmkNum > 0 ) {
                layout.setVisibility( View.VISIBLE );
            }
        }
    }

    protected void showBaseEndObstructions( int resid, Cursor[] result ) {
        Cursor rwy = result[ 1 ];
        String text;
        String object = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_CONTROLLING_OBJECT ) );
        if ( object.length() > 0 ) {
            int height = rwy.getInt( rwy.getColumnIndex( 
                    Runways.BASE_END_CONTROLLING_OBJECT_HEIGHT ) );
            if ( height > 0 ) {
                int distance = rwy.getInt( rwy.getColumnIndex( 
                        Runways.BASE_END_CONTROLLING_OBJECT_DISTANCE ) );
                String lighted = rwy.getString( rwy.getColumnIndex( 
                        Runways.BASE_END_CONTROLLING_OBJECT_LIGHTED ) );
                String offset = rwy.getString( rwy.getColumnIndex( 
                        Runways.BASE_END_CONTROLLING_OBJECT_OFFSET ) );
                int slope = rwy.getInt( rwy.getColumnIndex(
                        Runways.BASE_END_CONTROLLING_OBJECT_SLOPE ) );
    
                text = String.format( " %d' %s, ", height, object );
                if ( lighted.length() > 0 ) {
                    text += DataUtils.decodeControllingObjectLighted( lighted )+", ";
                }
                text += String.format( "%d' from runway end", distance );
                if ( offset.length() > 0 ) {
                    text += String.format( ", %d' %s of centerline",
                            DataUtils.decodeControllingObjectOffset( offset ),
                            DataUtils.decodeControllingObjectOffsetDirection( offset ) );
                }
                if ( slope > 0 ) {
                    text += String.format( ", %d:1 slope to clear", slope );
                }
            } else {
                text = object;
            }
    
            LinearLayout layout = (LinearLayout) mMainLayout.findViewById( resid );
            addRemarkRow( layout, text );
            layout.setVisibility( View.VISIBLE );
        }
    }

    protected void showReciprocalEndObstructions( int resid, Cursor[] result ) {
        Cursor rwy = result[ 1 ];
        String object = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_CONTROLLING_OBJECT ) );
        if ( object.length() > 0 ) {
            String lighted = rwy.getString( rwy.getColumnIndex( 
                    Runways.RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED ) );
            int height = rwy.getInt( rwy.getColumnIndex( 
                    Runways.RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT ) );
            int distance = rwy.getInt( rwy.getColumnIndex( 
                    Runways.RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE ) );
            String offset = rwy.getString( rwy.getColumnIndex( 
                    Runways.RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET ) );
            int slope = rwy.getInt( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE ) );

            String text = String.format( "%d' %s, ", height, object );
            if ( lighted.length() > 0 ) {
                text += DataUtils.decodeControllingObjectLighted( lighted )+", ";
            }
            text += String.format( "%d' from runway end, ", distance );
            text += String.format( "%d' %s of centerline",
                    DataUtils.decodeControllingObjectOffset( offset ),
                    DataUtils.decodeControllingObjectOffsetDirection( offset ) );
            if ( slope > 0 ) {
                text += String.format( ", %d:1 slope to clear", slope );
            }

            LinearLayout layout = (LinearLayout) mMainLayout.findViewById( resid );
            addRemarkRow( layout, text );
            layout.setVisibility( View.VISIBLE );
        }
    }

    protected void addRow( TableLayout table, String label, String text ) {
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
        tvValue.setText( text );
        tvValue.setGravity( Gravity.RIGHT );
        tvValue.setPadding( 4, 4, 2, 4 );
        row.addView( tvValue, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRemarkRow( LinearLayout layout, String remark ) {
        int index = remark.indexOf( ' ' );
        if ( index != -1 ) {
            while ( remark.charAt( index ) == ' ' ) {
                ++index;
            }
            remark = remark.substring( index );
        }
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 12, 1, 2, 1 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 1, 12, 1 );
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
