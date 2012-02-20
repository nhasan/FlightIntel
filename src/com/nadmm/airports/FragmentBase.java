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

import java.text.NumberFormat;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.UiUtils;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentBase extends Fragment {

    private ActivityBase mActivity;

    @Override
    public void onAttach( SupportActivity activity ) {
        super.onAttach( activity );
        mActivity = (ActivityBase) activity;
    }

    public DatabaseManager getDbManager() {
        return mActivity.getDbManager();
    }

    public ActivityBase getActivityBase() {
        return mActivity;
    }

    protected View createContentView( View view ) {
        return mActivity.createContentView( view );
    }

    protected void setContentShown( boolean shown ) {
        mActivity.setContentShown( getView(), shown );
    }

    protected int getSelectorResourceForRow( int curRow, int totRows ) {
        return mActivity.getSelectorResourceForRow( curRow, totRows );
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
        tv.setText( icaoCode+" - "+ stationName );
        tv = (TextView) root.findViewById( R.id.wx_station_info );
        if ( awos.moveToFirst() ) {
            String type = awos.getString( awos.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
            if ( type == null || type.length() == 0 ) {
                type = "ASOS/AWOS";
            }
            String city = awos.getString( awos.getColumnIndex( Airports.ASSOC_CITY ) );
            String state = awos.getString( awos.getColumnIndex( Airports.ASSOC_STATE ) );
            tv.setText( type+", "+city+", "+state );
            String phone = awos.getString( awos.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
            if ( phone != null && phone.length() > 0 ) {
                tv = (TextView) root.findViewById( R.id.wx_station_phone );
                tv.setText( phone );
                UiUtils.makeClickToCall( getActivity(), tv );
                tv.setVisibility( View.VISIBLE );
            }
            String freq = awos.getString( awos.getColumnIndex( Awos.STATION_FREQUENCY ) );
            if ( freq != null && freq.length() > 0 ) {
                tv = (TextView) root.findViewById( R.id.wx_station_freq );
                tv.setText( freq );
                tv.setVisibility( View.VISIBLE );
            }
            freq = awos.getString( awos.getColumnIndex( Awos.SECOND_STATION_FREQUENCY ) );
            if ( freq != null && freq.length() > 0 ) {
                tv = (TextView) root.findViewById( R.id.wx_station_freq2 );
                tv.setText( freq );
                tv.setVisibility( View.VISIBLE );
            }
            int elev = wxs.getInt( wxs.getColumnIndex( Wxs.STATION_ELEVATOIN_METER ) );
            NumberFormat decimal = NumberFormat.getNumberInstance();
            tv = (TextView) root.findViewById( R.id.wx_station_info2 );
            tv.setText( String.format( "Located at %s' MSL elevation",
                    decimal.format( DataUtils.metersToFeet( elev ) ) ) );
        } else {
            tv.setText( "ASOS/AWOS" );
        }
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

    protected View findViewById( int id ) {
        return getView().findViewById( id );
    }

    protected View inflate( int id ) {
        return mActivity.inflate( id );
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

    protected void setRefreshItemEnabled( Boolean enable ) {
        mActivity.setRefreshItemEnabled( enable );
    }

}
