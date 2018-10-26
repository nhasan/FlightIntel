/*
 * FlightIntel for Pilots
 *
 * Copyright 2018 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.dof;

import android.content.Context;
import android.database.Cursor;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager.DOF;
import com.nadmm.airports.data.DatabaseManager.LocationColumns;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;

import java.util.Locale;

public class DofCursorAdapter extends ResourceCursorAdapter {

    static class ViewHolder {
        TextView obstacleType;
        TextView mslHeight;
        TextView aglHeight;
        TextView markingType;
        TextView lightingType;
        TextView location;
    }

    public DofCursorAdapter( Context context, Cursor c ) {
        super( context, R.layout.dof_list_item, c, 0 );
    }

    @Override
    public void bindView( View view, Context context, Cursor c ) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if ( holder == null ) {
            holder = new ViewHolder();
            holder.obstacleType = view.findViewById( R.id.obstacle_type );
            holder.mslHeight = view.findViewById( R.id.height_msl );
            holder.aglHeight = view.findViewById( R.id.height_agl );
            holder.markingType = view.findViewById( R.id.marking_type );
            holder.lightingType = view.findViewById( R.id.lighting_type );
            holder.location = view.findViewById( R.id.location );
            view.setTag( holder );
        }

        String obstacleType = decodeObstacle( c.getString( c.getColumnIndex( DOF.OBSTACLE_TYPE ) ) );
        int count = c.getInt( c.getColumnIndex( DOF.COUNT ) );
        int mslHeight = c.getInt( c.getColumnIndex( DOF.HEIGHT_MSL ) );
        int aglHeight = c.getInt( c.getColumnIndex( DOF.HEIGHT_AGL ) );
        String marking = decodeMarking( c.getString( c.getColumnIndex( DOF.MARKING_TYPE ) ) );
        String lighting = decodeLighting( c.getString( c.getColumnIndex( DOF.LIGHTING_TYPE ) ) );
        float distance = c.getFloat( c.getColumnIndex( LocationColumns.DISTANCE ) );
        float bearing = c.getFloat( c.getColumnIndex( LocationColumns.BEARING ) );

        if ( count > 1 ) {
            obstacleType = String.format( Locale.US, "%s (%d count)", obstacleType, count );
        }

        holder.obstacleType.setText( obstacleType );
        holder.mslHeight.setText( FormatUtils.formatFeetMsl( mslHeight ) );
        holder.aglHeight.setText( FormatUtils.formatFeetAgl( aglHeight ) );
        holder.markingType.setText( marking );
        holder.lightingType.setText( lighting );
        holder.location.setText( String.format( Locale.US, "%.1f NM %s, heading %.0f\u00B0 M",
                distance, GeoUtils.getCardinalDirection( bearing ), bearing ) );
    }

    private String decodeObstacle( String type ) {
        return type.replace( "TWR", "TOWER" )
                .replace( "BLDG", "BUILDING" );
    }

    private String decodeMarking( String type ) {
        String marking;

        if ( type.equals( "P" ) ) {
            marking = "Orange/White paint marker";
        } else if ( type.equals( "W" ) ) {
            marking = "White paint marker";
        } else if ( type.equals( "M" ) ) {
            marking = "Marked";
        } else if ( type.equals( "F" ) ) {
            marking = "Flag marker";
        } else if ( type.equals( "S" ) ) {
            marking = "Spherical marker";
        } else if ( type.equals( "N" ) ) {
            marking = "Not marked";
        } else {
            marking = "Unknown marking";
        }

        return marking;
    }

    private String decodeLighting( String type ) {
        String lighting;

        if ( type.equals( "R" ) ) {
            lighting = "Red lighting";
        } else if ( type.equals( "D" ) ) {
            lighting = "Medium intensity White Strobe & Red lighting";
        } else if ( type.equals( "H" ) ) {
            lighting = "High intensity White Strobe & Red lighting";
        } else if ( type.equals( "M" ) ) {
            lighting = "Medium intensity White Strobe lighting";
        } else if ( type.equals( "S" ) ) {
            lighting = "High intensity White Strobe lighting";
        } else if ( type.equals( "F" ) ) {
            lighting = "Flood lighting";
        } else if ( type.equals( "C" ) ) {
            lighting = "Dual medium catenary lighting";
        } else if ( type.equals( "W" ) ) {
            lighting = "Synchronized Red lighting";
        } else if ( type.equals( "L" ) ) {
            lighting = "Lighted";
        } else if ( type.equals( "N" ) ) {
            lighting = "Not lighted";
        } else {
            lighting = "Unknown lighting";
        }

        return lighting;
    }

}

