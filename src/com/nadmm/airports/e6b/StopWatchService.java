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

    private final int STATE_STOPPED = 1;
    private final int STATE_RUNNING = 2;

    private IBinder mBinder = new StopWatchBinder();
    private OnTickHandler mTickHandler = null;
    private long mStartMillis = 0;
    private long mLastMillis = 0;
    private int mState = STATE_STOPPED;
    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            mLastMillis = SystemClock.elapsedRealtime();
            scheduleNextUpdate();
            if ( mTickHandler != null ) {
                mTickHandler.onTick( getElapsedTime() );
            }
        }
    };

    private ArrayList<Long> mLegsList;

    @Override
    public void onCreate() {
        super.onCreate();
        mLegsList = new ArrayList<Long>();
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

    public void setOnTickHandler( OnTickHandler handler ) {
        mTickHandler = handler;
    }

    public void startTimimg() {
        if ( mState == STATE_STOPPED ) {
            mState = STATE_RUNNING;
            mStartMillis = SystemClock.elapsedRealtime();
            mLastMillis = mStartMillis;
            scheduleNextUpdate();
        }
    }

    public void stopTimimg() {
        if ( mState == STATE_RUNNING ) {
            removeUpdate();
            mState = STATE_STOPPED;
            mLastMillis = SystemClock.elapsedRealtime();
            if ( mTickHandler != null ) {
                mTickHandler.onTick( getElapsedTime() );
            }
        }
    }

    protected void reset() {
        if ( mState == STATE_STOPPED ) {
            mStartMillis = 0;
            mLastMillis = 0;
            mLegsList.clear();
        }
    }

    public void addLeg() {
        if ( mState == STATE_RUNNING ) {
            mLegsList.add( getElapsedTime() );
        }
    }

    public boolean isRunning() {
        return mState == STATE_RUNNING;
    }

    protected void scheduleNextUpdate() {
        mHandler.postDelayed( mRunnable, DELAY_MILLIS );
    }

    protected void removeUpdate() {
        mHandler.removeCallbacks( mRunnable );
    }

    protected long getElapsedTime() {
        return mLastMillis-mStartMillis;
    }

    protected ArrayList<Long> getLegs() {
        return mLegsList;
    }

}
