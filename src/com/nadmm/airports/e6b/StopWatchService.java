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

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

public class StopWatchService extends Service {

    private final long DELAY_MILLIS = 100;

    private final int STATE_PAUSED = 1;
    private final int STATE_RUNNING = 2;

    private IBinder mBinder = new StopWatchBinder();
    private Handler mHandler = new Handler();
    private OnTickHandler mClient = null;

    private long mStartMillis;
    private long mLastMillis;
    private long mLastDuration;
    private int mState;
    private ArrayList<Long> mLegsList;

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            mLastMillis = SystemClock.elapsedRealtime();
            scheduleNextUpdate();
            notifyClient();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mLegsList = new ArrayList<Long>();
        reset();
    }

    @Override
    public IBinder onBind( Intent intent ) {
        return mBinder;
    }

    public interface OnTickHandler {
        public void onTick( long millis );
    }

    public class StopWatchBinder extends Binder {
        public StopWatchService getService() {
            return StopWatchService.this;
        }
    }

    public void setOnTickHandler( OnTickHandler client ) {
        mClient = client;
    }

    public void startTimimg() {
        if ( mState == STATE_PAUSED ) {
            mState = STATE_RUNNING;
            mLastDuration = getElapsedTime();
            mStartMillis = SystemClock.elapsedRealtime();
            mLastMillis = mStartMillis;
            scheduleNextUpdate();
        }
    }

    public void stopTimimg() {
        if ( mState == STATE_RUNNING ) {
            removeUpdate();
            mState = STATE_PAUSED;
            mLastMillis = SystemClock.elapsedRealtime();
            notifyClient();
        }
    }

    protected void reset() {
        mState = STATE_PAUSED;
        mStartMillis = 0;
        mLastMillis = 0;
        mLastDuration = 0;
        mLegsList.clear();
    }

    public void addLeg() {
        mLegsList.add( getElapsedTime() );
    }

    public boolean isRunning() {
        return mState == STATE_RUNNING;
    }

    public boolean isPaused() {
        return mState == STATE_PAUSED;
    }

    protected void scheduleNextUpdate() {
        mHandler.postDelayed( mRunnable, DELAY_MILLIS );
    }

    protected void notifyClient() {
        if ( mClient != null ) {
            mClient.onTick( getElapsedTime() );
        }
    }

    protected void removeUpdate() {
        mHandler.removeCallbacks( mRunnable );
    }

    protected long getElapsedTime() {
        return (mLastMillis-mStartMillis)+mLastDuration;
    }

    protected ArrayList<Long> getLegs() {
        return mLegsList;
    }

}
