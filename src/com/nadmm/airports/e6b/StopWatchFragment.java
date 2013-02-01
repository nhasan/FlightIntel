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

package com.nadmm.airports.e6b;

import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;

public class StopWatchFragment extends FragmentBase {

    private final long DELAY_MILLIS = 100;
    private final int STATE_STOPPED = 1;
    private final int STATE_RUNNING = 2;

    private Handler mHandler;
    private Runnable mRunnable;
    private long mElapsedMillis;

    private Button mBtnAction;
    private Button mBtnReset;
    private Button mBtnLeg;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflate( R.layout.e6b_stopwatch_layout );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );
        mHandler = new Handler();
        mElapsedMillis = 0;

        mBtnAction = (Button) findViewById( R.id.stopwatch_action );
        mBtnAction.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                actionPressed();
            }
        } );
        mBtnReset = (Button) findViewById( R.id.stopwatch_reset );
        mBtnReset.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                resetPressed();
            }
        } );
        mBtnLeg = (Button) findViewById( R.id.stopwatch_leg );
        mBtnLeg.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                legPressed();
            }
        } );

        scheduleUpdate();
    }

    protected void actionPressed() {        
    }

    protected void resetPressed() {
    }

    protected void legPressed() {
    }

    protected void setState( int state ) {
        if ( state == STATE_STOPPED ) {
            mBtnAction.setText( R.string.start );
            mBtnReset.setVisibility( mElapsedMillis > 0? View.VISIBLE : View.GONE );
            mBtnLeg.setVisibility( View.GONE );
        } else if ( state == STATE_RUNNING ) {
            mBtnAction.setText( R.string.stop );
            mBtnReset.setVisibility( View.GONE );
            mBtnLeg.setVisibility( View.VISIBLE );
        }
    }

    protected void scheduleUpdate() {
        if ( mRunnable == null ) {
            mRunnable = new Runnable() {

                @Override
                public void run() {
                    mElapsedMillis += DELAY_MILLIS;
                    updateTime();
                    scheduleUpdate();
                }
            };
        }
        mHandler.postDelayed( mRunnable, DELAY_MILLIS );
    }

    protected void updateTime() {
        long hrs = mElapsedMillis/DateUtils.HOUR_IN_MILLIS;
        long mins = (mElapsedMillis%DateUtils.HOUR_IN_MILLIS)/DateUtils.MINUTE_IN_MILLIS;
        long secs = (mElapsedMillis%DateUtils.MINUTE_IN_MILLIS)/DateUtils.SECOND_IN_MILLIS;
        long tenths = (mElapsedMillis%DateUtils.SECOND_IN_MILLIS)/(DateUtils.SECOND_IN_MILLIS/10);

        TextView tv = (TextView) findViewById( R.id.stopwatch_time );
        if ( hrs > 0 ) {
            tv.setText( String.format( "%02d:%02d:%02d", hrs, mins, secs ) );
        } else {
            tv.setText( String.format( "%02d:%02d", mins, secs ) );
        }
        tv = (TextView) findViewById( R.id.stopwatch_tenths );
        tv.setText( String.format( ".%01d", tenths ) );
    }

}
