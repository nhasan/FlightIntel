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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Ils1;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.GuiUtils;

public class RunwayDetailsActivity extends ActivityBase {

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        String siteNumber = args.getString( Runways.SITE_NUMBER );
        String runwayId = args.getString( Runways.RUNWAY_ID );
        RunwayDetailsTask task = new RunwayDetailsTask();
        task.execute( siteNumber, runwayId );
    }

    private final class RunwayDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            String runwayId = params[ 1 ];
            Cursor[] cursors = new Cursor[ 3 ];

            cursors[ 0 ] = mDbManager.getAirportDetails( siteNumber );

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
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
                    +"AND ( substr("+Remarks.REMARK_NAME+", 1, 2) in ('A2', 'A3', 'A4', 'A5', 'A6')"
                    +"OR "+Remarks.REMARK_NAME+" in ('A81', 'A81 1') )",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 2 ] = c;

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            setContentView( R.layout.runway_detail_view );

            Cursor apt = result[ 0 ];
            showAirportTitle( apt );

            Cursor rwy = result[ 1 ];
            if ( !rwy.moveToFirst() ) {
                GuiUtils.showToast( getApplicationContext(), "Unable to get runway information" );
                finish();
                return;
            }

            String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
            boolean isHelipad = runwayId.startsWith( "H" );

            if ( isHelipad ) {
                showHelipadInformation( result );
            } else {
                showCommonInformation( result );
                showBaseEndInformation( result );
                showReciprocalEndInformation( result );
            }
        }

    }

    protected void showCommonInformation( Cursor[] result ) {
        // Common runway information
        Cursor rwy = result[ 1 ];
        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
        TextView tv = (TextView) findViewById( R.id.rwy_common_label );
        tv.setText( "Runway "+runwayId );
        TableLayout layout = (TableLayout) findViewById( R.id.rwy_common_details );
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
        LinearLayout rmkLayout = (LinearLayout) findViewById( R.id.rwy_common_remarks );
        showCommonRemarks( rmkLayout, result, runwayId );
    }

    protected void showBaseEndInformation( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        Cursor rwy = result[ 1 ];

        boolean showExtra = mPrefs.getBoolean( PreferencesActivity.KEY_SHOW_EXTRA_RUNWAY_DATA,
                false );

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ID ) );
        TextView tv = (TextView) findViewById( R.id.rwy_base_end_label );
        tv.setText( "Runway "+runwayId );

        TableLayout layout = (TableLayout) findViewById( R.id.rwy_base_end_details );
        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            String siteNumber = apt.getString( apt.getColumnIndex(
                    Airports.SITE_NUMBER ) );
            Bundle args = new Bundle();
            args.putString( Ils1.SITE_NUMBER, siteNumber );
            args.putString( Ils1.RUNWAY_ID, runwayId );
            args.putString( Ils1.ILS_TYPE, ilsType );
            Intent intent = new Intent( this, IlsDetailsActivity.class );
            intent.putExtras( args );
            addClickableRow( layout, "Instrument approach", ilsType, intent, 
                    R.drawable.row_selector_top );
            addSeparator( layout );
        }
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_HEADING ) );
        if ( heading > 0 ) {
            int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
            String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
            if ( dir.equals( "E" ) ) {
                variation *= -1;
            }
            addRow( layout, "Magnetic heading", String.format( "%03d\u00B0",
                    DataUtils.calculateMagneticHeading( heading, variation ) ) );
            addSeparator( layout );
        }
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
        String reil = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_REIL_AVAILABLE ) );
        addSeparator( layout );
        addRow( layout, "REIL", reil.equals( "Y" )? "Yes" : "No" );
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.BASE_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addSeparator( layout );
            addRow( layout, "Displaced threshold",
                    String.format( "%d'", displacedThreshold ) );
        }

        if ( showExtra ) {
            String centerline = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_CENTERLINE_LIGHTS_AVAILABLE ) );
            addSeparator( layout );
            addRow( layout, "Centerline lights", centerline.equals( "Y" )? "Yes" : "No" );
            String touchdown = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE ) );
            addSeparator( layout );
            addRow( layout, "Touchdown lights", touchdown.equals( "Y" )? "Yes" : "No" );
            int tora = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TORA ) );
            if ( tora > 0 ) {
                addSeparator( layout );
                addRow( layout, "TORA", String.format( "%d'", tora ) );
            }
            int toda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TODA ) );
            if ( toda > 0 ) {
                addSeparator( layout );
                addRow( layout, "TODA", String.format( "%d'", toda ) );
            }
            int lda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_LDA ) );
            if ( lda > 0 ) {
                addSeparator( layout );
                addRow( layout, "LDA", String.format( "%d'", lda ) );
            }
            int asda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_ASDA ) );
            if ( asda > 0 ) {
                addSeparator( layout );
                addRow( layout, "ASDA", String.format( "%d'", asda ) );
            }
            int tch = rwy.getInt( rwy.getColumnIndex(
                    Runways.BASE_END_THRESHOLD_CROSSING_HEIGHT ) );
            if ( tch > 0 ) {
                addSeparator( layout );
                addRow( layout, "TCH", String.format( "%d'", tch ) );
            }
            int tdz = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TDZ_ELEVATION ) );
            if ( tdz > 0 ) {
                addSeparator( layout );
                addRow( layout, "TDZ elevation", String.format( "%d'", tdz ) );
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
        }

        // Show remarks
        showBaseEndRemarks( result );
    }

    protected void showReciprocalEndInformation( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        Cursor rwy = result[ 1 ];

        boolean showExtra = mPrefs.getBoolean( PreferencesActivity.KEY_SHOW_EXTRA_RUNWAY_DATA,
                false );

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ID ) );
        TextView tv = (TextView) findViewById( R.id.rwy_reciprocal_end_label );
        tv.setText( "Runway "+runwayId );

        TableLayout layout = (TableLayout) findViewById( R.id.rwy_reciprocal_end_details );
        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            String siteNumber = apt.getString( apt.getColumnIndex(
                    Airports.SITE_NUMBER ) );
            Bundle args = new Bundle();
            args.putString( Ils1.SITE_NUMBER, siteNumber );
            args.putString( Ils1.RUNWAY_ID, runwayId );
            args.putString( Ils1.ILS_TYPE, ilsType );
            Intent intent = new Intent( this, IlsDetailsActivity.class );
            intent.putExtras( args );
            addClickableRow( layout, "Instrument approach", ilsType, intent, 
                    R.drawable.row_selector_top );
            addSeparator( layout );
        }
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_HEADING ) );
        if ( heading > 0 ) {
            int variation = apt.getInt( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DEGREES ) );
            String dir = apt.getString( apt.getColumnIndex( Airports.MAGNETIC_VARIATION_DIRECTION ) );
            if ( dir.equals( "E" ) ) {
                variation *= -1;
            }
            addRow( layout, "Magnetic heading", String.format( "%03d\u00B0",
                    DataUtils.calculateMagneticHeading( heading, variation ) ) );
            addSeparator( layout );
        }
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
        String reil = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_REIL_AVAILABLE ) );
        addSeparator( layout );
        addRow( layout, "REIL", reil.equals( "Y" )? "Yes" : "No" );
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addSeparator( layout );
            addRow( layout, "Displaced threshold",
                    String.format( "%d'", displacedThreshold ) );
        }

        if ( showExtra ) {
            String centerline = rwy.getString( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE ) );
            addSeparator( layout );
            addRow( layout, "Centerline lights", centerline.equals( "Y" )? "Yes" : "No" );
            String touchdown = rwy.getString( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE ) );
            addSeparator( layout );
            addRow( layout, "Touchdown lights", touchdown.equals( "Y" )? "Yes" : "No" );
            int tora = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TORA ) );
            if ( tora > 0 ) {
                addSeparator( layout );
                addRow( layout, "TORA", String.format( "%d'", tora ) );
            }
            int toda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TODA ) );
            if ( toda > 0 ) {
                addSeparator( layout );
                addRow( layout, "TODA", String.format( "%d'", toda ) );
            }
            int lda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_LDA ) );
            if ( lda > 0 ) {
                addSeparator( layout );
                addRow( layout, "LDA", String.format( "%d'", lda ) );
            }
            int asda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_ASDA ) );
            if ( asda > 0 ) {
                addSeparator( layout );
                addRow( layout, "ASDA", String.format( "%d'", asda ) );
            }
            int tch = rwy.getInt( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT ) );
            if ( tch > 0 ) {
                addSeparator( layout );
                addRow( layout, "TCH", String.format( "%d'", tch ) );
            }
            int tdz = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TDZ_ELEVATION ) );
            if ( tdz > 0 ) {
                addSeparator( layout );
                addRow( layout, "TDZ elevation", String.format( "%d'", tdz ) );
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
        }

        // Show remarks
        showReciprocalEndRemarks( result );
    }

    protected void showHelipadInformation( Cursor[] result ) {
        // Hide the runway sections
        TextView tv = (TextView) findViewById( R.id.rwy_base_end_label );
        tv.setVisibility( View.GONE );
        TableLayout layout = (TableLayout) findViewById( R.id.rwy_base_end_details );
        layout.setVisibility( View.GONE );
        tv = (TextView) findViewById( R.id.rwy_reciprocal_end_label );
        tv.setVisibility( View.GONE );
        layout = (TableLayout) findViewById( R.id.rwy_reciprocal_end_details );
        layout.setVisibility( View.GONE );

        Cursor rwy = result[ 1 ];
        String helipadId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
        tv = (TextView) findViewById( R.id.rwy_common_label );
        tv.setText( "Helipad "+helipadId );

        layout = (TableLayout) findViewById( R.id.rwy_common_details );
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
        LinearLayout rmkLayout = (LinearLayout) findViewById( R.id.rwy_base_end_remarks );
        showRemarks( rmkLayout, result, helipadId );
    }

    protected void showCommonRemarks( LinearLayout layout, Cursor[] result, String runwayId ) {
        int count = 0;
        Cursor rmk = result[ 2 ];
        if ( rmk.moveToFirst() ) {
            do {
                String rmkName = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_NAME ) );
                if ( rmkName.startsWith( "A81" ) ) {
                    ++count;
                    String rmkText = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                    addRemarkRow( layout, rmkText );
                }
            } while ( rmk.moveToNext() );
        }
        count += showRemarks( layout, result, runwayId );
        if ( count > 0 ) {
            layout.setVisibility( View.VISIBLE );
        }
    }

    protected void showBaseEndRemarks( Cursor[] result ) {
        int count = 0;
        Cursor rwy = result[ 1 ];
        LinearLayout layout = (LinearLayout) findViewById(R.id.rwy_base_end_remarks );
        String als = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_APCH_LIGHT_SYSTEM ) );
        if ( als.length() > 0 ) {
            String apchLights = DataUtils.getApproachLightSystemDescription( als );
            if ( apchLights.length() > 0 ) {
                addBulletedRow( layout, apchLights );
                ++count;
            }
        }

        // Show RVR information
        String rvr = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_RVR_LOCATIONS ) );
        if ( rvr.length() > 0 ) {
            addBulletedRow( layout, "RVR equipment located at "
                    +DataUtils.decodeRVRLocations( rvr ) );
            ++count;
        }

        // Show obstructions
        count += showBaseEndObstructions( layout, result );

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ID ) );
        count += showRemarks( layout, result, runwayId );

        if ( count > 0 ) {
            layout.setVisibility( View.VISIBLE );
        }
    }

    protected void showReciprocalEndRemarks( Cursor[] result ) {
        int count = 0;
        Cursor rwy = result[ 1 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_reciprocal_end_remarks );
        String als = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_APCH_LIGHT_SYSTEM ) );
        if ( als.length() > 0 ) {
            String apchLights = DataUtils.getApproachLightSystemDescription( als );
            if ( apchLights.length() > 0 ) {
                addBulletedRow( layout, apchLights );
                ++count;
            }
        }

        // Show RVR information
        String rvr = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_RVR_LOCATIONS ) );
        if ( rvr.length() > 0 ) {
            addBulletedRow( layout, "RVR equipment located at "
                    +DataUtils.decodeRVRLocations( rvr ) );
            ++count;
        }

        // Show obstructions
        count += showReciprocalEndObstructions( layout, result );

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ID ) );
        showRemarks( layout, result, runwayId );

        if ( count > 0 ) {
            layout.setVisibility( View.VISIBLE );
        }
    }

    protected int showRemarks( LinearLayout layout, Cursor[] result, String runwayId ) {
        int count = 0;
        Cursor rmk = result[ 2 ];
        if ( rmk.moveToFirst() ) {
            do {
                String rmkName = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_NAME ) );
                if ( rmkName.endsWith( "-"+runwayId ) ) {
                    ++count;
                    String rmkText = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                    addRemarkRow( layout, rmkText );
                }
            } while ( rmk.moveToNext() );
        }
        return count;
    }

    protected int showBaseEndObstructions( LinearLayout layout, Cursor[] result ) {
        int count = 0;
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
    
                text = String.format( "%d' %s, ", height, object.toLowerCase() );
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
    
            addBulletedRow( layout, text );
            ++count;
        }

        return count;
    }

    protected int showReciprocalEndObstructions( LinearLayout layout, Cursor[] result ) {
        int count = 0;
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

            String text = String.format( "%d' %s, ", height, object.toLowerCase() );
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

            addBulletedRow( layout, text );
            ++count;
        }

        return count;
    }

    protected void addRemarkRow( LinearLayout layout, String remark ) {
        int index = remark.indexOf( ' ' );
        if ( index != -1 ) {
            while ( remark.charAt( index ) == ' ' ) {
                ++index;
            }
            remark = remark.substring( index );
        }
        addBulletedRow( layout, remark );
    }

}
