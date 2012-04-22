/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos1;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;

public class FragmentBase extends Fragment {

    private ActivityBase mActivity;
    private CursorAsyncTask mTask;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        setRetainInstance( true );

        super.onCreate( savedInstanceState );
    }

    @Override
    public void onAttach( SupportActivity activity ) {
        super.onAttach( activity );
        mActivity = (ActivityBase) activity;
    }

    @Override
    public void onPause() {
        if ( mTask != null ) {
            mTask.cancel( true );
        }
        super.onPause();
    }

    public DatabaseManager getDbManager() {
        return mActivity.getDbManager();
    }

    public SQLiteDatabase getDatabase( String type ) {
        return mActivity.getDatabase( type );
    }

    public ActivityBase getActivityBase() {
        return mActivity;
    }

    protected View createContentView( int id ) {
        return mActivity.createContentView( id );
    }

    protected View createContentView( View view ) {
        return mActivity.createContentView( view );
    }

    protected void setContentShown( boolean shown ) {
        mActivity.setContentShown( shown );
    }

    protected void setContentMsg( String msg ) {
        mActivity.setContentMsg( msg );
    }

    protected void setFragmentContentShown( boolean shown ) {
        mActivity.setContentShown( getView(), shown );
    }

    protected CursorAsyncTask setBackgroundTask( CursorAsyncTask task ) {
        mTask = task;
        return mTask;
    }

    protected ActionBar getSupportActionBar() {
        return mActivity.getSupportActionBar();
    }

    protected void setActionBarTitle( Cursor c, String subtitle ) {
        mActivity.setActionBarTitle( c, subtitle );
    }

    protected void setActionBarTitle( String title ) {
        mActivity.setActionBarTitle( title );
    }

    protected int getSelectorResourceForRow( int curRow, int totRows ) {
        return mActivity.getSelectorResourceForRow( curRow, totRows );
    }

    public Cursor getAirportDetails( String siteNumber ) {
        return mActivity.getAirportDetails( siteNumber );
    }

    public void showAirportTitle( Cursor c ) {
        mActivity.setActionBarTitle( c );
        mActivity.showAirportTitle( c );
    }

    protected void showNavaidTitle( Cursor c ) {
        mActivity.showNavaidTitle( c );
    }

    public void showWxTitle( Cursor[] cursors ) {
        Cursor wxs = cursors[ 0 ];
        Cursor awos = cursors[ 1 ];

        View root = getView();
        if ( root == null ) {
            return;
        }

        TextView tv = (TextView) root.findViewById( R.id.wx_station_name );
        String icaoCode = wxs.getString( wxs.getColumnIndex( Wxs.STATION_ID ) );
        String stationName = wxs.getString( wxs.getColumnIndex( Wxs.STATION_NAME ) );
        tv.setText( String.format( "%s - %s", icaoCode, stationName ) );
        if ( awos.moveToFirst() ) {
            tv = (TextView) root.findViewById( R.id.wx_station_info );
            String type = awos.getString( awos.getColumnIndex( Awos1.WX_SENSOR_TYPE ) );
            if ( type == null || type.length() == 0 ) {
                type = "ASOS/AWOS";
            }
            String city = awos.getString( awos.getColumnIndex( Airports.ASSOC_CITY ) );
            String state = awos.getString( awos.getColumnIndex( Airports.ASSOC_STATE ) );
            tv.setText( String.format( "%s, %s, %s", type, city, state ) );

            String phone = awos.getString( awos.getColumnIndex( Awos1.STATION_PHONE_NUMBER ) );
            if ( phone != null && phone.length() > 0 ) {
                tv = (TextView) root.findViewById( R.id.wx_station_phone );
                tv.setText( phone );
                makeClickToCall( tv );
                tv.setVisibility( View.VISIBLE );
            }

            String freq = awos.getString( awos.getColumnIndex( Awos1.STATION_FREQUENCY ) );
            if ( freq != null && freq.length() > 0 ) {
                tv = (TextView) root.findViewById( R.id.wx_station_freq );
                tv.setText( freq );
                tv.setVisibility( View.VISIBLE );
            }

            freq = awos.getString( awos.getColumnIndex( Awos1.SECOND_STATION_FREQUENCY ) );
            if ( freq != null && freq.length() > 0 ) {
                tv = (TextView) root.findViewById( R.id.wx_station_freq2 );
                tv.setText( freq );
                tv.setVisibility( View.VISIBLE );
            }
        } else {
            tv = (TextView) root.findViewById( R.id.wx_station_info );
            tv.setText( "ASOS/AWOS" );
        }
        int elev = wxs.getInt( wxs.getColumnIndex( Wxs.STATION_ELEVATOIN_METER ) );
        tv = (TextView) root.findViewById( R.id.wx_station_info2 );
        tv.setText( String.format( "Located at %s MSL elevation",
                FormatUtils.formatFeet( DataUtils.metersToFeet( elev ) ) ) );

        CheckBox cb = (CheckBox) root.findViewById( R.id.airport_star );
        cb.setChecked( getDbManager().isFavoriteWx( icaoCode ) );
        cb.setTag( icaoCode );
        cb.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                CheckBox cb = (CheckBox) v;
                String icaoCode = (String) cb.getTag();
                if ( cb.isChecked() ) {
                    getDbManager().addToFavoriteWx( icaoCode );
                    Toast.makeText( getActivity(), "Added to favorites list",
                            Toast.LENGTH_SHORT ).show();
                } else {
                    getDbManager().removeFromFavoriteWx( icaoCode );
                    Toast.makeText( getActivity(), "Removed from favorites list",
                            Toast.LENGTH_SHORT ).show();
                }
            }

        } );
    }

    protected View addRow( LinearLayout layout, String value ) {
        return mActivity.addRow( layout, value );
    }

    protected View addRow( LinearLayout layout, String label, String value ) {
        return mActivity.addRow( layout, label, value );
    }

    protected View addRow( LinearLayout layout, String label, String value1, String value2 ) {
        return mActivity.addRow( layout, label, value1, value2 );
    }

    protected View addRow( LinearLayout layout, String label1, String value1,
            String label2, String value2 ) {
        return mActivity.addRow( layout, label1, value1, label2, value2 );
    }

    protected View addClickableRow( LinearLayout layout, View row, Intent intent, int resid ) {
        return mActivity.addClickableRow( layout, row, intent, resid );
    }

    protected View addClickableRow( LinearLayout layout, String label,
            final Intent intent, int resid ) {
        return mActivity.addClickableRow( layout, label, null, intent, resid );
    }

    protected View addClickableRow( LinearLayout layout, String label, String value,
            final Intent intent, int resid ) {
        return mActivity.addClickableRow( layout, label, value, intent, resid );
    }

    protected View addClickableRow( LinearLayout layout, String label1, String value1,
            String label2, String value2, final Intent intent, int resid ) {
        return mActivity.addClickableRow( layout, label1, value1, label2, value2, intent, resid );
    }

    protected void addBulletedRow( LinearLayout layout, String text ) {
        mActivity.addBulletedRow( layout, text );
    }

    protected void addSeparator( LinearLayout layout ) {
        mActivity.addSeparator( layout );
    }

    protected View addPhoneRow( LinearLayout layout, String label, String phone ) {
        return mActivity.addPhoneRow( layout, label, phone );
    }

    protected View addPhoneRow( LinearLayout layout, String label, String phone,
            String label2, String value2 ) {
        return mActivity.addPhoneRow( layout, label, phone, label2, value2 );
    }

    protected void makeClickToCall( TextView tv ) {
        mActivity.makeClickToCall( tv );
    }

    protected View findViewById( int id ) {
        return getView().findViewById( id );
    }

    protected View inflate( int id ) {
        return mActivity.inflate( id );
    }

    protected View inflate( int id, ViewGroup root ) {
        return mActivity.inflate( id, root );
    }

    protected void startRefreshAnimation() {
        mActivity.startRefreshAnimation();
    }

    protected void stopRefreshAnimation() {
        mActivity.stopRefreshAnimation();
    }

    protected void setRefreshItemVisible( Boolean visible ) {
        mActivity.setRefreshItemVisible( visible );
    }

}
