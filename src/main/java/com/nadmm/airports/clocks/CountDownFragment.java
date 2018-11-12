/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.clocks;

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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.clocks.CountDownService.CountDownBinder;
import com.nadmm.airports.clocks.CountDownService.OnTickHandler;

import java.util.Locale;

public class CountDownFragment extends FragmentBase implements OnTickHandler {

    private final int BLINK_DELAY = 500;
    private final int COUNTDOWN_MODE = 1;
    private final int EDIT_MODE = 2;

    private Button mBtnAction;
    private Button mBtnReset;
    private Button mBtnRestart;
    private ImageButton mBtnMinsPlus;
    private ImageButton mBtnMinsMinus;
    private ImageButton mBtnSecsPlus;
    private ImageButton mBtnSecsMinus;
    private TextView mTimeMinutes;
    private TextView mTimeColon;
    private TextView mTimeSeconds;
    private TextView mTimeTenths;

    private long mLastMillis;
    private long mRemainMillis = 0;
    private int mMode = EDIT_MODE;
    private CountDownService mService = null;
    private CountDownConnection mConnection = new CountDownConnection();
    private Handler mHandler = new Handler();

    private Runnable mBlink = this::blink;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Activity activity = getActivity();
        Intent service = new Intent( activity, CountDownService.class );
        activity.startService( service );
        setRetainInstance( true );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.clocks_countdown_view, container, false );
        return createContentView( view );
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

        ActivityBase activity = getActivityBase();
        activity.setDrawerIndicatorEnabled( false );

        Intent service = new Intent( activity, CountDownService.class );
        activity.bindService( service, mConnection, 0 );
    }

    @Override
    public void onPause() {
        super.onPause();

        setKeepScreenOn( false );
        Activity activity = getActivity();
        activity.unbindService( mConnection );
        stopBlink();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mTimeMinutes = findViewById( R.id.countdown_mins );
        mTimeColon = findViewById( R.id.countdown_colon );
        mTimeSeconds = findViewById( R.id.countdown_secs );
        mTimeTenths = findViewById( R.id.countdown_tenths );

        mBtnMinsPlus = findViewById( R.id.countdown_mins_plus );
        mBtnMinsPlus.setOnClickListener( v -> {
            mRemainMillis += DateUtils.MINUTE_IN_MILLIS;
            if ( mRemainMillis >= DateUtils.HOUR_IN_MILLIS ) {
                mRemainMillis -= DateUtils.HOUR_IN_MILLIS;
            }
            updateUiState();
        } );

        mBtnMinsMinus = findViewById( R.id.countdown_mins_minus );
        mBtnMinsMinus.setOnClickListener( v -> {
            mRemainMillis -= DateUtils.MINUTE_IN_MILLIS;
            if ( mRemainMillis < 0 ) {
                mRemainMillis += DateUtils.HOUR_IN_MILLIS;
            }
            updateUiState();
        } );

        mBtnSecsPlus = findViewById( R.id.countdown_secs_plus );
        mBtnSecsPlus.setOnClickListener( v -> {
            if ( ( mRemainMillis % DateUtils.MINUTE_IN_MILLIS )
                        / DateUtils.SECOND_IN_MILLIS < 59 ) {
                mRemainMillis += DateUtils.SECOND_IN_MILLIS;
            } else {
                mRemainMillis = ( mRemainMillis / DateUtils.MINUTE_IN_MILLIS )
                        * DateUtils.MINUTE_IN_MILLIS;
            }
            updateUiState();
        } );

        mBtnSecsMinus = findViewById( R.id.countdown_secs_minus );
        mBtnSecsMinus.setOnClickListener( v -> {
            if ( ( mRemainMillis % DateUtils.MINUTE_IN_MILLIS ) > 0 ) {
                mRemainMillis -= DateUtils.SECOND_IN_MILLIS;
            } else {
                mRemainMillis = ( ( ( mRemainMillis / DateUtils.MINUTE_IN_MILLIS ) + 1 )
                        * DateUtils.MINUTE_IN_MILLIS ) - DateUtils.SECOND_IN_MILLIS;
            }
            updateUiState();
        } );

        mBtnAction = findViewById( R.id.countdown_action );
        mBtnAction.setOnClickListener( v -> actionPressed() );
        mBtnReset = findViewById( R.id.countdown_reset );
        mBtnReset.setOnClickListener( v -> resetPressed() );
        mBtnRestart = findViewById( R.id.countdown_restart );
        mBtnRestart.setOnClickListener( v -> restartPressed() );

        setFragmentContentShown( true );
    }

    @Override
    public void onTick( long millis ) {
        mRemainMillis = millis;
        showRemainingTime();
        if ( millis == 0 ) {
            showRemainingTime();
            mHandler.postDelayed( this::updateUiState, DateUtils.SECOND_IN_MILLIS );
        }
    }

    private void actionPressed() {
        if ( !mService.isRunning() ) {
            if ( mService.isReset() ) {
                mLastMillis = mRemainMillis;
            }
            mService.startCountDown( mRemainMillis );
            setKeepScreenOn( true );
        } else {
            mService.stopCountDown();
        }
        updateUiState();
    }

    private void resetPressed() {
        if ( mMode == COUNTDOWN_MODE ) {
            mService.resetCountDown();
            mRemainMillis = mLastMillis;
            setKeepScreenOn( false );
        } else {
            mRemainMillis = 0;
        }
        updateUiState();
    }

    private void restartPressed() {
        setKeepScreenOn( false );
        mRemainMillis = mLastMillis;
        mService.startCountDown( mRemainMillis );
        updateUiState();
    }

    private void setKeepScreenOn( boolean keepScreenOn ) {
        if ( getView() != null ) {
            getView().setKeepScreenOn( keepScreenOn );
        }
    }

    private void startBlink() {
        mHandler.postDelayed( mBlink, BLINK_DELAY );
    }

    private void stopBlink() {
        mHandler.removeCallbacks( mBlink );
        setTimeVisibility( View.VISIBLE );
    }

    private void blink() {
        mHandler.postDelayed( mBlink, BLINK_DELAY );
        int visibility = ( mTimeSeconds.getVisibility()==View.VISIBLE )?
                View.INVISIBLE : View.VISIBLE;
        setTimeVisibility( visibility );
    }

    private void setTimeVisibility( int visibility ) {
        mTimeMinutes.setVisibility( visibility );
        mTimeColon.setVisibility( visibility );
        mTimeSeconds.setVisibility( visibility );
        mTimeTenths.setVisibility( visibility );
    }

    private void showRemainingTime() {
        String time = formatElapsedTime( mRemainMillis );
        mTimeMinutes.setText( time.substring( 0, 2 ) );
        mTimeSeconds.setText( time.substring( 3, 5 ) );
        mTimeTenths.setText( time.substring( 5 ) );
    }

    protected String formatElapsedTime( long millis ) {
        long mins = millis / DateUtils.MINUTE_IN_MILLIS;
        long secs = ( millis % DateUtils.MINUTE_IN_MILLIS ) / DateUtils.SECOND_IN_MILLIS;
        long tenths = ( millis % DateUtils.SECOND_IN_MILLIS )/( DateUtils.SECOND_IN_MILLIS/10 );
        return String.format( Locale.US, "%02d:%02d.%01d", mins, secs, tenths );
    }

    private void setCountdownMode() {
        mMode = COUNTDOWN_MODE;
        mBtnMinsPlus.setVisibility( View.INVISIBLE );
        mBtnMinsMinus.setVisibility( View.INVISIBLE );
        mBtnSecsPlus.setVisibility( View.INVISIBLE );
        mBtnSecsMinus.setVisibility( View.INVISIBLE );
    }

    private void setEditMode() {
        mMode = EDIT_MODE;
        mBtnMinsPlus.setVisibility( View.VISIBLE );
        mBtnMinsMinus.setVisibility( View.VISIBLE );
        mBtnSecsPlus.setVisibility( View.VISIBLE );
        mBtnSecsMinus.setVisibility( View.VISIBLE );
    }

    private void updateUiState() {
        showRemainingTime();
        if ( mService.isRunning() ) {
            stopBlink();
            setCountdownMode();
            mBtnAction.setText( R.string.pause );
            mBtnAction.setVisibility( View.VISIBLE );
            mBtnAction.setEnabled( true );
            mBtnReset.setVisibility( View.GONE );
            mBtnRestart.setVisibility( View.GONE );
        } else if ( mService.isReset() ) {
            stopBlink();
            setEditMode();
            mBtnAction.setText( R.string.start );
            mBtnAction.setVisibility( View.VISIBLE );
            mBtnAction.setEnabled( mRemainMillis>0 );
            mBtnReset.setVisibility( mRemainMillis > 0? View.VISIBLE : View.GONE );
            mBtnRestart.setVisibility( View.GONE );
        } else if ( mService.isFinished() ) {
            startBlink();
            setCountdownMode();
            mBtnAction.setVisibility( View.GONE );
            mBtnReset.setVisibility( View.VISIBLE );
            mBtnRestart.setVisibility( View.VISIBLE );
        } else {
            startBlink();
            setCountdownMode();
            mBtnAction.setText( R.string.start );
            mBtnAction.setVisibility( View.VISIBLE );
            mBtnAction.setEnabled( true );
            mBtnReset.setVisibility( mRemainMillis > 0? View.VISIBLE : View.GONE );
            mBtnRestart.setVisibility( View.GONE );
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
