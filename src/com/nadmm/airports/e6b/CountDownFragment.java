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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.e6b.CountDownService.CountDownBinder;
import com.nadmm.airports.e6b.CountDownService.OnTickHandler;

public class CountDownFragment extends FragmentBase implements OnTickHandler {

    private final int BLINK_DELAY = 500;

    private Button mBtnAction;
    private Button mBtnReset;
    private TextView mTimeSeconds;
    private TextView mTimeTenths;

    private long mRemainMillis = 0;
    private CountDownService mService = null;
    private CountDownConnection mConnection = new CountDownConnection();
    private Handler mHandler = new Handler();
    private Runnable mBlink = new Runnable() {

        @Override
        public void run() {
            blink();
        }
    };

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        Activity activity = getActivity();
        Intent service = new Intent( activity, StopWatchService.class );
        activity.startService( service );
        setRetainInstance( true );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mService != null ) {
            mService.stopSelf();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();
        Intent service = new Intent( activity, CountDownService.class );
        activity.bindService( service, mConnection, 0 );
    }

    @Override
    public void onPause() {
        super.onPause();

        Activity activity = getActivity();
        activity.unbindService( mConnection );
        mHandler.removeCallbacks( mBlink );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflate( R.layout.e6b_countdown_layout );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mTimeSeconds = (TextView) findViewById( R.id.countdown_time );
        mTimeTenths = (TextView) findViewById( R.id.countdown_tenths );
        mBtnAction = (Button) findViewById( R.id.countdown_action );
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
    }

    protected void actionPressed() {
        if ( !mService.isRunning() ) {
            mService.startCountDown( 20000 );
            mTimeSeconds.setVisibility( View.VISIBLE );
            mTimeTenths.setVisibility( View.VISIBLE );
            mHandler.removeCallbacks( mBlink );
        } else {
            mService.stopCountDown();
            mHandler.postDelayed( mBlink, BLINK_DELAY );
        }
        updateUiState();
    }

    protected void resetPressed() {
    }

    @Override
    public void onTick( long millis ) {
        mRemainMillis = millis;
        showRemainingTime();
    }

    protected void blink() {
        mHandler.postDelayed( mBlink, BLINK_DELAY );
        boolean visible = ( mTimeSeconds.getVisibility()==View.VISIBLE );
        mTimeSeconds.setVisibility( visible? View.INVISIBLE : View.VISIBLE );
        mTimeTenths.setVisibility( visible? View.INVISIBLE : View.VISIBLE );
    }

    protected void updateUiState() {
        if ( mService != null && mService.isRunning() ) {
            mBtnAction.setText( R.string.pause );
            mBtnReset.setVisibility( View.GONE );
        } else {
            mBtnAction.setText( R.string.start );
            mBtnReset.setVisibility( mRemainMillis > 0? View.VISIBLE : View.GONE );
        }
        showRemainingTime();
    }

    protected void showRemainingTime() {
        String time = formatElapsedTime( mRemainMillis );
        int dot = time.indexOf( '.' );
        mTimeSeconds.setText( time.substring( 0, dot ) );
        mTimeTenths.setText( time.substring( dot ) );
        if ( mRemainMillis == 0 && mService.isFinished() ) {
            mHandler.postDelayed( mBlink, BLINK_DELAY );
        }
    }

    @SuppressLint("DefaultLocale")
    protected String formatElapsedTime( long millis ) {
        long hrs = millis / DateUtils.HOUR_IN_MILLIS;
        long mins = ( millis % DateUtils.HOUR_IN_MILLIS ) / DateUtils.MINUTE_IN_MILLIS;
        long secs = ( millis % DateUtils.MINUTE_IN_MILLIS ) / DateUtils.SECOND_IN_MILLIS;
        long tenths = ( millis % DateUtils.SECOND_IN_MILLIS )/( DateUtils.SECOND_IN_MILLIS/10 );
        if ( hrs > 0 ) {
            return String.format( "%02d:%02d:%02d.%01d", hrs, mins, secs, tenths );
        } else {
            return String.format( "%02d:%02d.%01d", mins, secs, tenths );
        }
    }

    private class CountDownConnection implements ServiceConnection {

        @Override
        public void onServiceConnected( ComponentName name, IBinder service ) {
            CountDownBinder binder = (CountDownBinder) service;
            mService = binder.getService();
            mService.setOnTickHandler( CountDownFragment.this );
            updateUiState();
        }

        @Override
        public void onServiceDisconnected( ComponentName name ) {
            mService.setOnTickHandler( null );
            mService = null;
        }        
    }

}
