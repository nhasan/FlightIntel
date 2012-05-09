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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos1;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.UiUtils;

public class FragmentBase extends Fragment {

    private ActivityBase mActivity;
    private CursorAsyncTask mTask;
    private final OnClickListener mOnRowClickListener = new OnClickListener() {
        @Override
        public void onClick( View v ) {
            Intent intent = (Intent) v.getTag();
            if ( intent != null ) {
                startActivity( intent );
            }
        }
    };
    private final OnClickListener mOnPhoneClickListener = new OnClickListener() {
        
        @Override
        public void onClick( View v ) {
            TextView tv = (TextView) v;
            String action = (String) tv.getTag();
            String phone = DataUtils.decodePhoneNumber( tv.getText().toString() );
            Intent intent = new Intent( action, Uri.parse( "tel:"+phone ) );
            startActivity( intent );
        }

    };

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

    protected void setContentShownNoAnimation( boolean shown ) {
        mActivity.setContentShownNoAnimation( shown );
    }

    protected void setContentMsg( String msg ) {
        mActivity.setContentMsg( msg );
    }

    protected void setFragmentContentShown( boolean shown ) {
        mActivity.setContentShown( getView(), shown );
    }

    protected void setFragmentContentShownNoAnimation( boolean shown ) {
        mActivity.setContentShownNoAnimation( getView(), shown );
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

    protected void setActionBarSubtitle( String subtitle ) {
        mActivity.setActionBarSubtitle( subtitle );
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
        mActivity.showFaddsEffectiveDate( c );
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
                    Toast.makeText( mActivity, "Added to favorites list",
                            Toast.LENGTH_SHORT ).show();
                } else {
                    getDbManager().removeFromFavoriteWx( icaoCode );
                    Toast.makeText( mActivity, "Removed from favorites list",
                            Toast.LENGTH_SHORT ).show();
                }
            }

        } );
    }

    protected void makeRowClickable( View row, Intent intent, int resid ) {
        row.setBackgroundResource( resid );
        row.setTag( intent );
        row.setOnClickListener( mOnRowClickListener );
    }

    protected void makeClickToCall( TextView tv ) {
        PackageManager pm = mActivity.getPackageManager();
        boolean hasTelephony = pm.hasSystemFeature( PackageManager.FEATURE_TELEPHONY );
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( mActivity );
        String tapAction = prefs.getString( PreferencesActivity.KEY_PHONE_TAP_ACTION, "dial" );
        if ( hasTelephony && !tapAction.equals( "ignore" ) ) {
            if ( tv.getText().length() > 0 ) {
                String action = tapAction.equals( "call" )?
                        Intent.ACTION_CALL : Intent.ACTION_DIAL;
                tv.setCompoundDrawablesWithIntrinsicBounds( R.drawable.phone, 0, 0, 0 );
                tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( mActivity, 3 ) );
                tv.setTag( action );
                tv.setOnClickListener( mOnPhoneClickListener );
            } else {
                tv.setCompoundDrawablesWithIntrinsicBounds( 0, 0, 0, 0 );
                tv.setOnClickListener( null );
            }
        }
    }

    protected View addClickableRow( LinearLayout layout, View row, Intent intent, int resid ) {
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }

        makeRowClickable( row, intent, resid );
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected View addClickableRow( LinearLayout layout, String label,
            Intent intent, int resid ) {
        return addClickableRow( layout, label, null, intent, resid );
    }

    protected View addClickableRow( LinearLayout layout, String label, String value,
            Intent intent, int resid ) {
        View row = addRow( layout, label+"...", value );
        makeRowClickable( row, intent, resid );
        return row;
    }

    protected View addClickableRow( LinearLayout layout, String label1, String value1,
            String label2, String value2, Intent intent, int resid ) {
        View row = addRow( layout, label1, value1, label2, value2 );
        makeRowClickable( row, intent, resid );
        return row;
    }

    protected View addPhoneRow( LinearLayout layout, String label, String phone ) {
        View row = addRow( layout, label, phone );
        TextView tv = (TextView) row.findViewById( R.id.item_value );
        makeClickToCall( tv );
        return row;
    }

    protected View addPhoneRow( LinearLayout layout, String label, String phone,
            String label2, String value2 ) {
        View row = addRow( layout, label, phone, label2, value2 );
        TextView tv = (TextView) row.findViewById( R.id.item_value );
        makeClickToCall( tv );
        return row;
    }

    protected View addRow( LinearLayout layout, String label ) {
        return addRow( layout, label, null );
    }

    protected View addRow( LinearLayout layout, String label, String value ) {
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }

        LinearLayout row = (LinearLayout) inflate( R.layout.detail_row_item2 );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label );
        tv = (TextView) row.findViewById( R.id.item_value );
        if ( value != null && value.length() > 0 ) {
            tv.setText( value );
        } else {
            tv.setVisibility( View.GONE );
        }
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected View addRow( LinearLayout layout, String label, String value1, String value2 ) {
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }

        LinearLayout row = (LinearLayout) inflate( R.layout.detail_row_item3 );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label );
        tv = (TextView) row.findViewById( R.id.item_value );
        if ( value1 != null && value1.length() > 0 ) {
            tv.setText( value1 );
        } else {
            tv.setVisibility( View.GONE );
        }
        tv = (TextView) row.findViewById( R.id.item_extra_value );
        if ( value2 != null && value2.length() > 0 ) {
            tv.setText( value2 );
        } else {
            tv.setVisibility( View.GONE );
        }
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected View addRow( LinearLayout layout, String label1, String value1,
            String label2, String value2 ) {
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }

        LinearLayout row = (LinearLayout) inflate( R.layout.detail_row_item4 );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label1 );
        tv = (TextView) row.findViewById( R.id.item_value );
        if ( value1 != null && value1.length() > 0 ) {
            tv.setText( value1 );
        } else {
            tv.setVisibility( View.GONE );
        }
        tv = (TextView) row.findViewById( R.id.item_extra_label );
        if ( label2 != null && label2.length() > 0 ) {
            tv.setText( label2 );
        } else {
            tv.setVisibility( View.GONE );
        }
        tv = (TextView) row.findViewById( R.id.item_extra_value );
        if ( value2 != null && value2.length() > 0 ) {
            tv.setText( value2 );
        } else {
            tv.setVisibility( View.GONE );
        }
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected void addBulletedRow( LinearLayout layout, String text ) {
        LinearLayout innerLayout = new LinearLayout( mActivity );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( mActivity );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( UiUtils.convertDpToPx( mActivity, 6 ), 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( mActivity );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( text );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    private void addSeparator( LinearLayout layout ) {
        View separator = new View( mActivity );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.MATCH_PARENT, 1 ) );
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
