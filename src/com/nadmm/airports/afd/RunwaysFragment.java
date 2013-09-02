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

import java.util.Locale;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Ars;
import com.nadmm.airports.DatabaseManager.Ils1;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.UiUtils;

public final class RunwaysFragment extends FragmentBase {

    private float mDeclination;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.runway_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        Bundle args = getArguments();
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        String runwayId = args.getString( Runways.RUNWAY_ID );
        setBackgroundTask( new RunwaysTask() ).execute( siteNumber, runwayId );

        super.onActivityCreated( savedInstanceState );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        showAirportTitle( apt );

        Cursor rwy = result[ 1 ];

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
        boolean isHelipad = runwayId.startsWith( "H" );

        if ( isHelipad ) {
            setActionBarTitle( apt, "Helipad "+runwayId );
            showHelipadInformation( result );
            showCommonRemarks( result, runwayId );
        } else {
            setActionBarTitle( apt, "Runway "+runwayId );
            showCommonInformation( result );
            showCommonRemarks( result, runwayId );
            showBaseEndInformation( result );
            showReciprocalEndInformation( result );
        }

        setContentShown( true );
    }

    protected void showCommonInformation( Cursor[] result ) {
        // Common runway information
        Cursor rwy = result[ 1 ];
        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
        int length = rwy.getInt( rwy.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_HEADING ) );
        if ( heading > 0 ) {
            heading = (int) DataUtils.calculateMagneticHeading( heading,
                    Math.round( mDeclination ) );
        } else {
            // Actual heading is not available, try to deduce it from runway id
            heading = DataUtils.getRunwayHeading( runwayId );
        }
        TextView tv = (TextView) findViewById( R.id.rwy_common_label );
        tv.setText( "Runway "+runwayId );
        UiUtils.setRunwayDrawable( getActivity(), tv, runwayId, length, heading );

        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_common_details );
        int width = rwy.getInt( rwy.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        addRow( layout, "Dimensions", String.format( "%s x %s",
                FormatUtils.formatFeet( length ), FormatUtils.formatFeet( width ) ) );
        String surfaceType = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TYPE ) );
        addRow( layout, "Surface type", DataUtils.decodeSurfaceType( surfaceType ) );
        String surfaceTreat = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TREATMENT ) );
        addRow( layout, "Surface treatment", DataUtils.decodeSurfaceTreatment( surfaceTreat ) );
        String edgeLights = rwy.getString( rwy.getColumnIndex( Runways.EDGE_LIGHTS_INTENSITY ) );
        addRow( layout, "Edge lights", DataUtils.decodeRunwayEdgeLights( edgeLights ) );
    }

    protected void showBaseEndInformation( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        Cursor rwy = result[ 1 ];

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( getActivity() );
        boolean showExtra = prefs.getBoolean( PreferencesActivity.KEY_SHOW_EXTRA_RUNWAY_DATA,
                false );
        StringBuilder sb = new StringBuilder();

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ID ) );
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_HEADING ) );
        if ( heading > 0 ) {
            heading = (int) DataUtils.calculateMagneticHeading( heading,
                    Math.round( mDeclination ) );
        } else {
            // Actual heading is not available, try to deduce it from runway id
            heading = DataUtils.getRunwayHeading( runwayId );
        }
        TextView tv = (TextView) findViewById( R.id.rwy_base_end_label );
        tv.setText( "Runway "+runwayId );

        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_base_end_details );
        addRow( layout, "Magnetic heading", FormatUtils.formatDegrees( heading ) );

        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            String siteNumber = apt.getString( apt.getColumnIndex(
                    Airports.SITE_NUMBER ) );
            Bundle args = new Bundle();
            args.putString( Ils1.SITE_NUMBER, siteNumber );
            args.putString( Ils1.RUNWAY_ID, runwayId );
            args.putString( Ils1.ILS_TYPE, ilsType );
            addClickableRow( layout, "Instrument approach", ilsType,
                    IlsFragment.class, args );
        }

        Float elevation = rwy.getFloat( rwy.getColumnIndex( Runways.BASE_END_RUNWAY_ELEVATION ) );
        if ( elevation != null && elevation > 0 ) {
            addRow( layout, "Elevation", FormatUtils.formatFeet( elevation ) );
        }

        String rhPattern = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_RIGHT_TRAFFIC ) );
        addRow( layout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );
        double gradient = rwy.getDouble( rwy.getColumnIndex( Runways.BASE_END_GRADIENT ) );
        if ( gradient > 0 ) {
            sb.setLength( 0 );
            sb.append( String.format( "%.1f%%", gradient ) );
            String gradientDir = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_GRADIENT_DIRECTION ) );
            if ( gradientDir.length() > 0 ) {
                sb.append( " " );
                sb.append( gradientDir );
            }
            addRow( layout, "Gradient", sb.toString() );
        }
        Cursor ars = result[ 2 ];
        if ( ars != null && ars.moveToFirst() ) {
            do {
                String arrestingDevice = ars.getString( ars.getColumnIndex(
                        Ars.ARRESTING_DEVICE ) );
                if ( arrestingDevice.length() > 0 ) {
                    addRow( layout, "Arresting device", arrestingDevice );
                }
            } while ( ars.moveToNext() );
        }
        String apchLights = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_APCH_LIGHT_SYSTEM ) );
        if ( apchLights.length() > 0 ) {
            addRow( layout, "Approach lights", apchLights );
        }
        String markings = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_MARKING_TYPE ) );
        if ( markings.length() > 0 ) {
            sb.setLength( 0 );
            sb.append( DataUtils.decodeRunwayMarking( markings ) );
            String condition = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_MARKING_CONDITION ) );
            if ( condition.length() > 0 ) {
                sb.append( ", " );
                sb.append( DataUtils.decodeRunwayMarkingCondition( condition ) );
            }
            addRow( layout, "Markings", sb.toString() );
        }
        String glideSlope = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_VISUAL_GLIDE_SLOPE ) );
        if ( glideSlope.length() > 0 ) {
            addRow( layout, "Glideslope", DataUtils.decodeGlideSlope( glideSlope ) );
        }
        float glideAngle = rwy.getFloat( rwy.getColumnIndex( Runways.BASE_END_GLIDE_ANGLE ) );
        if ( glideAngle > 0 ) {
            addRow( layout, "Glide angle", FormatUtils.formatDegrees( glideAngle ) );
        }
        String reil = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_REIL_AVAILABLE ) );
        addRow( layout, "REIL", reil.equals( "Y" )? "Yes" : "No" );
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.BASE_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addRow( layout, "Displaced threshold", FormatUtils.formatFeet( displacedThreshold ) );
        }

        if ( showExtra ) {
            String centerline = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_CENTERLINE_LIGHTS_AVAILABLE ) );
            addRow( layout, "Centerline lights", centerline.equals( "Y" )? "Yes" : "No" );
            String touchdown = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE ) );
            addRow( layout, "Touchdown lights", touchdown.equals( "Y" )? "Yes" : "No" );
            int tora = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TORA ) );
            if ( tora > 0 ) {
                addRow( layout, "TORA", FormatUtils.formatFeet( tora ) );
            }
            int toda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TODA ) );
            if ( toda > 0 ) {
                addRow( layout, "TODA", FormatUtils.formatFeet( toda ) );
            }
            int lda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_LDA ) );
            if ( lda > 0 ) {
                addRow( layout, "LDA", FormatUtils.formatFeet( lda ) );
            }
            int asda = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_ASDA ) );
            if ( asda > 0 ) {
                addRow( layout, "ASDA", FormatUtils.formatFeet( asda ) );
            }
            int tch = rwy.getInt( rwy.getColumnIndex(
                    Runways.BASE_END_THRESHOLD_CROSSING_HEIGHT ) );
            if ( tch > 0 ) {
                addRow( layout, "TCH", FormatUtils.formatFeet( tch ) );
            }
            int tdz = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_TDZ_ELEVATION ) );
            if ( tdz > 0 ) {
                addRow( layout, "TDZ elevation", FormatUtils.formatFeet( tdz ) );
            }
            int lahso = rwy.getInt( rwy.getColumnIndex( Runways.BASE_END_LAHSO_DISTANCE ) );
            String lahsoRunway = rwy.getString( rwy.getColumnIndex(
                    Runways.BASE_END_LAHSO_RUNWAY ) );
            if ( lahso > 0 ) {
                if ( lahsoRunway.length() > 0 ) {
                    addRow( layout, "LAHSO distance", String.format( "%s to %s",
                            FormatUtils.formatFeet( lahso ), lahsoRunway ) );
                } else {
                    addRow( layout, "LAHSO distance", FormatUtils.formatFeet( lahso ) );
                }
            }
        }
        setRowBackgroundResource( layout );

        // Show remarks
        showBaseEndRemarks( result );
    }

    protected void showReciprocalEndInformation( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        Cursor rwy = result[ 1 ];

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( getActivity() );
        boolean extra =
                prefs.getBoolean( PreferencesActivity.KEY_SHOW_EXTRA_RUNWAY_DATA, false );
        StringBuilder sb = new StringBuilder();

        String runwayId = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ID ) );
        int heading = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_HEADING ) );
        if ( heading > 0 ) {
            heading = (int) DataUtils.calculateMagneticHeading( heading,
                    Math.round( mDeclination ) );
        } else {
            // Actual heading is not available, try to deduce it from runway id
            heading = DataUtils.getRunwayHeading( runwayId );
        }
        TextView tv = (TextView) findViewById( R.id.rwy_reciprocal_end_label );
        tv.setText( "Runway "+runwayId );

        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_reciprocal_end_details );
        addRow( layout, "Magnetic heading", FormatUtils.formatDegrees( heading ) );

        String ilsType = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_ILS_TYPE ) );
        if ( ilsType.length() > 0 ) {
            String siteNumber = apt.getString( apt.getColumnIndex(
                    Airports.SITE_NUMBER ) );
            Bundle args = new Bundle();
            args.putString( Ils1.SITE_NUMBER, siteNumber );
            args.putString( Ils1.RUNWAY_ID, runwayId );
            args.putString( Ils1.ILS_TYPE, ilsType );
            addClickableRow( layout, "Instrument approach", ilsType, IlsFragment.class, args );
        }
        Float elevation = rwy.getFloat( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_RUNWAY_ELEVATION ) );
        if ( elevation != null && elevation > 0 ) {
            addRow( layout, "Elevation", FormatUtils.formatFeet( elevation ) );
        }
        String rhPattern = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_RIGHT_TRAFFIC ) );
        addRow( layout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );
        double gradient = rwy.getDouble( rwy.getColumnIndex( Runways.RECIPROCAL_END_GRADIENT ) );
        if ( gradient > 0 ) {
            sb.setLength( 0 );
            sb.append( String.format( "%.1f%%", gradient ) );
            String gradientDir = rwy.getString( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_GRADIENT_DIRECTION ) );
            if ( gradientDir.length() > 0 ) {
                sb.append( " " );
                sb.append( gradientDir );
            }
            addRow( layout, "Gradient", sb.toString() );
        }
        Cursor ars = result[ 3 ];
        if ( ars != null && ars.moveToFirst() ) {
            do {
                String arrestingDevice = ars.getString( ars.getColumnIndex(
                        Ars.ARRESTING_DEVICE ) );
                if ( arrestingDevice.length() > 0 ) {
                    addRow( layout, "Arresting device", arrestingDevice );
                }
            } while ( ars.moveToNext() );
        }
        String apchLights = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_APCH_LIGHT_SYSTEM ) );
        if ( apchLights.length() > 0 ) {
            addRow( layout, "Approach lights", apchLights );
        }
        String markings = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_MARKING_TYPE ) );
        String condition = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_MARKING_CONDITION ) );
        if ( markings.length() > 0 ) {
            sb.setLength( 0 );
            sb.append( DataUtils.decodeRunwayMarking( markings ) );
            if ( condition.length() > 0 ) {
                sb.append( ", " );
                sb.append( DataUtils.decodeRunwayMarkingCondition( condition ) );
            }
            addRow( layout, "Markings", sb.toString() );
        }
        String glideSlope = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_VISUAL_GLIDE_SLOPE ) );
        if ( glideSlope.length() > 0 ) {
            addRow( layout, "Glideslope", DataUtils.decodeGlideSlope( glideSlope ) );
        }
        float glideAngle = rwy.getFloat( rwy.getColumnIndex( Runways.RECIPROCAL_END_GLIDE_ANGLE ) );
        if ( glideAngle > 0 ) {
            addRow( layout, "Glide angle", FormatUtils.formatDegrees( glideAngle ) );
        }
        String reil = rwy.getString( rwy.getColumnIndex( Runways.RECIPROCAL_END_REIL_AVAILABLE ) );
        addRow( layout, "REIL", reil.equals( "Y" )? "Yes" : "No" );
        int displacedThreshold = rwy.getInt( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH ) );
        if ( displacedThreshold > 0 ) {
            addRow( layout, "Displaced threshold", FormatUtils.formatFeet( displacedThreshold ) );
        }

        if ( extra ) {
            String centerline = rwy.getString( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE ) );
            addRow( layout, "Centerline lights", centerline.equals( "Y" )? "Yes" : "No" );
            String touchdown = rwy.getString( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE ) );
            addRow( layout, "Touchdown lights", touchdown.equals( "Y" )? "Yes" : "No" );
            int tora = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TORA ) );
            if ( tora > 0 ) {
                addRow( layout, "TORA", FormatUtils.formatFeet( tora ) );
            }
            int toda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TODA ) );
            if ( toda > 0 ) {
                addRow( layout, "TODA", FormatUtils.formatFeet( toda ) );
            }
            int lda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_LDA ) );
            if ( lda > 0 ) {
                addRow( layout, "LDA", FormatUtils.formatFeet( lda ) );
            }
            int asda = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_ASDA ) );
            if ( asda > 0 ) {
                addRow( layout, "ASDA", FormatUtils.formatFeet( asda ) );
            }
            int tch = rwy.getInt( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT ) );
            if ( tch > 0 ) {
                addRow( layout, "TCH", FormatUtils.formatFeet( tch ) );
            }
            int tdz = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_TDZ_ELEVATION ) );
            if ( tdz > 0 ) {
                addRow( layout, "TDZ elevation", FormatUtils.formatFeet( tdz ) );
            }
            int lahso = rwy.getInt( rwy.getColumnIndex( Runways.RECIPROCAL_END_LAHSO_DISTANCE ) );
            String lahsoRunway = rwy.getString( rwy.getColumnIndex(
                    Runways.RECIPROCAL_END_LAHSO_RUNWAY ) );
            if ( lahso > 0 ) {
                if ( lahsoRunway.length() > 0 ) {
                    addRow( layout, "LAHSO distance", String.format( "%s to %s",
                            FormatUtils.formatFeet( lahso ), lahsoRunway ) );
                } else {
                    addRow( layout, "LAHSO distance", FormatUtils.formatFeet( lahso ) );
                }
            }
        }
        setRowBackgroundResource( layout );

        // Show remarks
        showReciprocalEndRemarks( result );
    }

    protected void showHelipadInformation( Cursor[] result ) {
        // Hide the runway sections
        TextView tv = (TextView) findViewById( R.id.rwy_base_end_label );
        tv.setVisibility( View.GONE );
        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_base_end_details );
        layout.setVisibility( View.GONE );
        tv = (TextView) findViewById( R.id.rwy_reciprocal_end_label );
        tv.setVisibility( View.GONE );
        layout = (LinearLayout) findViewById( R.id.rwy_reciprocal_end_details );
        layout.setVisibility( View.GONE );

        Cursor rwy = result[ 1 ];
        String helipadId = rwy.getString( rwy.getColumnIndex( Runways.RUNWAY_ID ) );
        tv = (TextView) findViewById( R.id.rwy_common_label );
        tv.setText( "Helipad "+helipadId );

        layout = (LinearLayout) findViewById( R.id.rwy_common_details );
        int length = rwy.getInt( rwy.getColumnIndex( Runways.RUNWAY_LENGTH ) );
        int width = rwy.getInt( rwy.getColumnIndex( Runways.RUNWAY_WIDTH ) );
        addRow( layout, "Dimensions", String.format( "%s x %s",
                FormatUtils.formatFeet( length ), FormatUtils.formatFeet( width ) ) );
        String surfaceType = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TYPE ) );
        addRow( layout, "Surface type", DataUtils.decodeSurfaceType( surfaceType ) );
        String surfaceTreat = rwy.getString( rwy.getColumnIndex( Runways.SURFACE_TREATMENT ) );
        addRow( layout, "Surface treatment", DataUtils.decodeSurfaceTreatment( surfaceTreat ) );
        String markings = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_MARKING_TYPE ) );
        String condition = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_MARKING_CONDITION ) );
        if ( markings.length() > 0 ) {
            addRow( layout, "Markings", DataUtils.decodeRunwayMarking( markings )
                    +", "+DataUtils.decodeRunwayMarkingCondition( condition ) );
        }
        String edgeLights = rwy.getString( rwy.getColumnIndex( Runways.EDGE_LIGHTS_INTENSITY ) );
        addRow( layout, "Edge lights", DataUtils.decodeRunwayEdgeLights( edgeLights ) );
        String rhPattern = rwy.getString( rwy.getColumnIndex( Runways.BASE_END_RIGHT_TRAFFIC ) );
        addRow( layout, "Traffic pattern", rhPattern.equals( "Y" )? "Right" : "Left" );

        // Show remarks
        LinearLayout rmkLayout = (LinearLayout) findViewById( R.id.rwy_base_end_remarks );
        showRemarks( rmkLayout, result, helipadId );
    }

    protected void showCommonRemarks( Cursor[] result, String runwayId ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.rwy_common_remarks );
        int count = 0;
        Cursor rmk = result[ 4 ];
        if ( rmk.moveToFirst() ) {
            do {
                String rmkName = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_NAME ) );
                if ( rmkName.startsWith( "A81" ) ) {
                    ++count;
                    String rmkText = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                    addBulletedRow( layout, rmkText );
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
        Cursor rmk = result[ 4 ];
        if ( rmk.moveToFirst() ) {
            do {
                String rmkName = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_NAME ) );
                if ( rmkName.endsWith( "-"+runwayId ) ) {
                    ++count;
                    String rmkText = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                    addBulletedRow( layout, rmkText );
                }
            } while ( rmk.moveToNext() );
        }
        return count;
    }

    protected int showBaseEndObstructions( LinearLayout layout, Cursor[] result ) {
        int count = 0;
        Cursor rwy = result[ 1 ];
        String text;
        String object = rwy.getString( rwy.getColumnIndex(
                Runways.BASE_END_CONTROLLING_OBJECT ) );
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
    
                text = String.format( "%s %s, ",
                        FormatUtils.formatFeet( height ), object.toLowerCase( Locale.US ) );
                if ( lighted.length() > 0 ) {
                    text += DataUtils.decodeControllingObjectLighted( lighted )+", ";
                }
                text += String.format( "%s from runway end", FormatUtils.formatFeet( distance ) );
                if ( offset.length() > 0 ) {
                    int value = DataUtils.decodeControllingObjectOffset( offset );
                    String dir = DataUtils.decodeControllingObjectOffsetDirection( offset );
                    text += String.format( ", %s %s of centerline",
                            FormatUtils.formatFeet( value ), dir );
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
        String text;
        String object = rwy.getString( rwy.getColumnIndex(
                Runways.RECIPROCAL_END_CONTROLLING_OBJECT ) );
        if ( object.length() > 0 ) {
            int height = rwy.getInt( rwy.getColumnIndex( 
                    Runways.BASE_END_CONTROLLING_OBJECT_HEIGHT ) );
            if ( height > 0 ) {
                int distance = rwy.getInt( rwy.getColumnIndex( 
                        Runways.RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE ) );
                String lighted = rwy.getString( rwy.getColumnIndex( 
                        Runways.RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED ) );
                String offset = rwy.getString( rwy.getColumnIndex( 
                        Runways.RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET ) );
                int slope = rwy.getInt( rwy.getColumnIndex(
                        Runways.RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE ) );
    
                text = String.format( "%s %s, ",
                        FormatUtils.formatFeet( height ), object.toLowerCase( Locale.US ) );
                if ( lighted.length() > 0 ) {
                    text += DataUtils.decodeControllingObjectLighted( lighted )+", ";
                }
                text += String.format( "%s from runway end", FormatUtils.formatFeet( distance ) );
                if ( offset.length() > 0 ) {
                    int value = DataUtils.decodeControllingObjectOffset( offset );
                    String dir = DataUtils.decodeControllingObjectOffsetDirection( offset );
                    text += String.format( ", %s %s of centerline",
                            FormatUtils.formatFeet( value ), dir );
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

    private final class RunwaysTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            String runwayId = params[ 1 ];
            Cursor[] cursors = new Cursor[ 5 ];

            cursors[ 0 ] = getAirportDetails( siteNumber );

            Cursor apt = cursors[ 0 ];
            double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );

            Location loc = new Location( "" );
            loc.setLatitude( lat );
            loc.setLongitude( lon );
            mDeclination = GeoUtils.getMagneticDeclination( loc );

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Runways.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "*" },
                    Runways.SITE_NUMBER+"=? AND "+Runways.RUNWAY_ID+"=?",
                    new String[] { siteNumber, runwayId }, null, null, null, null );
            c.moveToFirst();
            cursors[ 1 ] = c;

            try {
                String endId = c.getString( c.getColumnIndex( Runways.BASE_END_ID ) );
                builder = new SQLiteQueryBuilder();
                builder.setTables( Ars.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Ars.SITE_NUMBER+"=? AND "+Ars.RUNWAY_ID+"=? AND "+Ars.RUNWAY_END_ID+"=?",
                        new String[] { siteNumber, runwayId, endId }, null, null, null, null );
                cursors[ 2 ] = c;
    
                endId = c.getString( c.getColumnIndex( Runways.RECIPROCAL_END_ID ) );
                builder = new SQLiteQueryBuilder();
                builder.setTables( Ars.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Ars.SITE_NUMBER+"=? AND "+Ars.RUNWAY_ID+"=? AND "+Ars.RUNWAY_END_ID+"=?",
                        new String[] { siteNumber, runwayId, endId }, null, null, null, null );
                cursors[ 3 ] = c;
            } catch ( Exception e ) {
            }

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            c = builder.query( db, new String[] { Remarks.REMARK_NAME, Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND ( substr("+Remarks.REMARK_NAME+", 1, 2) in ('A2', 'A3', 'A4', 'A5', 'A6')"
                    +"OR "+Remarks.REMARK_NAME+" like 'A81%' )",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 4 ] = c;

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

}
