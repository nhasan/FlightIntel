/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2019 Nadeem Hasan <nhasan@nadmm.com>
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

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Com;
import com.nadmm.airports.data.DatabaseManager.Nav1;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DbUtils;
import com.nadmm.airports.utils.GeoUtils;

import java.util.Arrays;
import java.util.Locale;

public final class NearbyFssFragment extends FragmentBase {

    // Extra column names for the cursor
    private static final String DISTANCE = "DISTANCE";
    private static final String BEARING = "BEARING";

    private int mRadius;

    private static  class ComData implements Comparable<ComData> {

        private String[] mColumnValues;

        private ComData( Cursor c, float declination, Location location ) {
            mColumnValues = new String[ c.getColumnCount()+2 ];
            int i = 0;
            while ( i < c.getColumnCount() ) {
                mColumnValues[ i ] = c.getString( i );
                ++i;
            }

            // Now calculate the distance to this wx station
            float[] results = new float[ 2 ];
            Location.distanceBetween( location.getLatitude(), location.getLongitude(),
                    c.getDouble( c.getColumnIndex( Com.COMM_OUTLET_LATITUDE_DEGREES ) ),
                    c.getDouble( c.getColumnIndex( Com.COMM_OUTLET_LONGITUDE_DEGREES ) ),
                    results );
            // Bearing
            mColumnValues[ i ] = String.valueOf( ( results[ 1 ]+declination+360 )%360 );
            ++i;
            // Distance
            mColumnValues[ i ] = String.valueOf( results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE );
        }

        @Override
        public int compareTo( ComData another ) {
            // Last element in the value array is the distance
            int indexOfDistance = mColumnValues.length-1;
            double distance1 = Double.valueOf( mColumnValues[ indexOfDistance ] );
            double distance2 = Double.valueOf( another.mColumnValues[ indexOfDistance ] );
            if ( distance1 > distance2 ) {
                return 1;
            } else if ( distance1 < distance2 ) {
                return -1;
            }
            return 0;
        }

    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fss_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mRadius = getActivityBase().getPrefNearbyRadius();

        setActionBarTitle( "Nearby FSS", "" );
        setActionBarSubtitle( String.format( Locale.US, "Within %d NM radius", mRadius ) );

        if ( getArguments() != null ) {
            String siteNumber = getArguments().getString( Airports.SITE_NUMBER );
            setBackgroundTask( new FssCommTask( this ) ).execute( siteNumber );
        }
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        showAirportTitle( apt );
        showFssDetails( result );

        setFragmentContentShown( true );
    }

    @SuppressLint( "SetTextI18n" )
    private void showFssDetails( Cursor[] result ) {
        Cursor com = result[ 1 ];
        LinearLayout detailLayout = findViewById( R.id.fss_detail_layout );
        if ( com.moveToFirst() ) {
            do {
                String outletId = com.getString( com.getColumnIndex( Com.COMM_OUTLET_ID ) );
                String outletType = com.getString( com.getColumnIndex( Com.COMM_OUTLET_TYPE ) );
                String outletCall = com.getString( com.getColumnIndex( Com.COMM_OUTLET_CALL ) );
                String navId = com.getString( com.getColumnIndex( Com.ASSOC_NAVAID_ID ) );
                String navName = com.getString( com.getColumnIndex( Nav1.NAVAID_NAME ) );
                String navType = com.getString( com.getColumnIndex( Nav1.NAVAID_TYPE ) );
                String navFreq = com.getString( com.getColumnIndex( Nav1.NAVAID_FREQUENCY ) );
                String freqs = com.getString( com.getColumnIndex( Com.COMM_OUTLET_FREQS ) );
                String fssName = com.getString( com.getColumnIndex( Com.FSS_NAME ) );
                float bearing  = com.getFloat( com.getColumnIndex( BEARING ) );
                float distance  = com.getFloat( com.getColumnIndex( DISTANCE ) );

                RelativeLayout item = inflate( R.layout.grouped_detail_item );
                TextView tv = item.findViewById( R.id.group_name );
                if ( navId.length() > 0 ) {
                    tv.setText( navId+" - "+navName+" "+navType );
                } else {
                    tv.setText( outletId+" - "+outletCall+" outlet" );
                }

                tv = item.findViewById( R.id.group_extra );
                if ( distance < 1.0 ) {
                    tv.setText( "On-site" );
                } else {
                    tv.setText( String.format( Locale.US, "%.0f NM %s", distance,
                        GeoUtils.getCardinalDirection( bearing ) ) );
                }

                LinearLayout layout = item.findViewById( R.id.group_details );
                addRow( layout, "Call", fssName+" Radio" );
                if ( navId.length() > 0 ) {
                    addRow( layout, navId, navFreq+"T" );
                }

                int i =0;
                while ( i < freqs.length() ) {
                    int end = Math.min( i+9, freqs.length() );
                    String freq = freqs.substring( i, end ).trim();
                    addRow( layout, outletType, freq );
                    i = end;
                }

                detailLayout.addView( item, new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
            } while ( com.moveToNext() );
        } else {
            setContentMsg( String.format( Locale.US,
                    "No FSS outlets found within %dNM radius.", mRadius ) );
        }
    }

    private Cursor[] doQuery( String siteNumber ) {

        SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
        Cursor[] result = new Cursor[ 2 ];

        Cursor apt = getAirportDetails( siteNumber );
        result[ 0 ] = apt;

        double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
        double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
        Location location = new Location( "" );
        location.setLatitude( lat );
        location.setLongitude( lon );

        String tableName = Com.TABLE_NAME+" c LEFT OUTER JOIN "+Nav1.TABLE_NAME+" n"
                +" ON c."+Com.ASSOC_NAVAID_ID+" = n."+Nav1.NAVAID_ID
                +" AND n."+Nav1.NAVAID_TYPE+" <> 'VOT'";
        String[] columns = new String[] { "c.*", "n."+Nav1.NAVAID_NAME,
                "n."+Nav1.NAVAID_TYPE, "n."+Nav1.NAVAID_FREQUENCY };

        Cursor c = DbUtils.getBoundingBoxCursor( db, tableName, columns,
                Com.COMM_OUTLET_LATITUDE_DEGREES, Com.COMM_OUTLET_LONGITUDE_DEGREES,
                location, mRadius );

        String[] columnNames = new String[ c.getColumnCount()+2 ];
        int i = 0;
        for ( String col : c.getColumnNames() ) {
            columnNames[ i++ ] = col;
        }
        columnNames[ i++ ] = BEARING;
        columnNames[ i ] = DISTANCE;
        @SuppressWarnings("resource")
        MatrixCursor matrix = new MatrixCursor( columnNames );

        if ( c.moveToFirst() ) {
            // Now find the magnetic declination at this location
            float declination = GeoUtils.getMagneticDeclination( location );

            ComData[] comDataList = new ComData[ c.getCount() ];
            int row = 0;
            do {
                ComData com = new ComData( c, declination, location );
                comDataList[ row++ ] = com;
            } while ( c.moveToNext() );

            // Sort the FSS Com list by distance
            Arrays.sort( comDataList );

            // Build a cursor out of the sorted FSS station list
            for ( ComData com : comDataList ) {
                float distance = Float.valueOf(
                        com.mColumnValues[ matrix.getColumnIndex( DISTANCE ) ] );
                if ( distance <= mRadius ) {
                    matrix.addRow( com.mColumnValues );
                }
            }
        }

        c.close();

        result[ 1 ] = matrix;

        return result;
    }

    private static class FssCommTask extends CursorAsyncTask<NearbyFssFragment> {

        private FssCommTask( NearbyFssFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( NearbyFssFragment fragment, String... params ) {
            String siteNumber = params[ 0 ];
            return fragment.doQuery( siteNumber );
        }

        @Override
        protected boolean onResult( NearbyFssFragment fragment, Cursor[] result ) {
            fragment.showDetails( result );
            return true;
        }

    }

}
