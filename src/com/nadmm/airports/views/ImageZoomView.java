/*
 * Copyright 2010 Sony Ericsson Mobile Communications AB
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.nadmm.airports.views;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

public class ImageZoomView extends View implements Observer {

    /** Paint object used when drawing bitmap. */
    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    /** Rectangle used (and re-used) for cropping source image. */
    private final Rect mRectSrc = new Rect();

    /** Rectangle used (and re-used) for specifying drawing area on canvas. */
    private final Rect mRectDst = new Rect();

    /** Object holding aspect quotient */
    private final AspectQuotient mAspectQuotient = new AspectQuotient();

    /** The bitmap that we're zooming in, and drawing on the screen. */
    private Bitmap mBitmap;

    /** State of the zoom. */
    private ZoomState mState;

    private DynamicZoomControl mZoomControl;
    private PinchZoomListener mPinchZoomListener;

    // Public methods

    /**
     * Constructor
     */
    public ImageZoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mZoomControl = new DynamicZoomControl();
        mPinchZoomListener = new PinchZoomListener(getContext().getApplicationContext());
        mPinchZoomListener.setZoomControl(mZoomControl);
        setZoomState(mZoomControl.getZoomState());
        mZoomControl.setAspectQuotient(getAspectQuotient());

        resetZoomState();
        
        setOnTouchListener(mPinchZoomListener);
    }

    /**
     * Set image bitmap
     * 
     * @param bitmap The bitmap to view and zoom into
     */
    public void setImage(Bitmap bitmap) {
        mBitmap = bitmap;

        mAspectQuotient.updateAspectQuotient(getWidth(), getHeight(),
                mBitmap.getWidth(), mBitmap.getHeight());
        mAspectQuotient.notifyObservers();

        invalidate();
    }

    /**
     * Set object holding the zoom state that should be used
     * 
     * @param state The zoom state
     */
    public void setZoomState(ZoomState state) {
        if (mState != null) {
            mState.deleteObserver(this);
        }

        mState = state;
        mState.addObserver(this);

        invalidate();
    }

    public void resetZoomState() {
        mZoomControl.getZoomState().setPanX(0.5f);
        mZoomControl.getZoomState().setPanY(0.5f);
        mZoomControl.getZoomState().setZoom(1f);
        mZoomControl.getZoomState().notifyObservers();
    }

    /**
     * Gets reference to object holding aspect quotient
     * 
     * @return Object holding aspect quotient
     */
    public AspectQuotient getAspectQuotient() {
        return mAspectQuotient;
    }

    // Superclass overrides

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null && mState != null) {
            final float aspectQuotient = mAspectQuotient.get();

            final int viewWidth = getWidth();
            final int viewHeight = getHeight();
            final int bitmapWidth = mBitmap.getWidth();
            final int bitmapHeight = mBitmap.getHeight();

            final float panX = mState.getPanX();
            final float panY = mState.getPanY();
            final float zoomX = mState.getZoomX(aspectQuotient) * viewWidth / bitmapWidth;
            final float zoomY = mState.getZoomY(aspectQuotient) * viewHeight / bitmapHeight;

            // Setup source and destination rectangles
            mRectSrc.left = (int)(panX * bitmapWidth - viewWidth / (zoomX * 2));
            mRectSrc.top = (int)(panY * bitmapHeight - viewHeight / (zoomY * 2));
            mRectSrc.right = (int)(mRectSrc.left + viewWidth / zoomX);
            mRectSrc.bottom = (int)(mRectSrc.top + viewHeight / zoomY);
            mRectDst.left = getLeft();
            mRectDst.top = getTop();
            mRectDst.right = getRight();
            mRectDst.bottom = getBottom();

            // Adjust source rectangle so that it fits within the source image.
            if (mRectSrc.left < 0) {
                mRectDst.left += -mRectSrc.left * zoomX;
                mRectSrc.left = 0;
            }
            if (mRectSrc.right > bitmapWidth) {
                mRectDst.right -= (mRectSrc.right - bitmapWidth) * zoomX;
                mRectSrc.right = bitmapWidth;
            }
            if (mRectSrc.top < 0) {
                mRectDst.top += -mRectSrc.top * zoomY;
                mRectSrc.top = 0;
            }
            if (mRectSrc.bottom > bitmapHeight) {
                mRectDst.bottom -= (mRectSrc.bottom - bitmapHeight) * zoomY;
                mRectSrc.bottom = bitmapHeight;
            }

            canvas.drawBitmap(mBitmap, mRectSrc, mRectDst, mPaint);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mAspectQuotient.updateAspectQuotient(right - left, bottom - top, mBitmap.getWidth(),
                mBitmap.getHeight());
        mAspectQuotient.notifyObservers();
    }

    // implements Observer
    public void update(Observable observable, Object data) {
        invalidate();
    }

    public class AspectQuotient extends Observable {

        /**
         * Aspect quotient
         */
        private float mAspectQuotient;

        // Public methods

        /**
         * Gets aspect quotient
         * 
         * @return The aspect quotient
         */
        public float get() {
            return mAspectQuotient;
        }

        /**
         * Updates and recalculates aspect quotient based on supplied view and
         * content dimensions.
         * 
         * @param viewWidth Width of view
         * @param viewHeight Height of view
         * @param contentWidth Width of content
         * @param contentHeight Height of content
         */
        public void updateAspectQuotient(float viewWidth, float viewHeight, float contentWidth,
                float contentHeight) {
            final float aspectQuotient = (contentWidth / contentHeight) / (viewWidth / viewHeight);

            if (aspectQuotient != mAspectQuotient) {
                mAspectQuotient = aspectQuotient;
                setChanged();
            }
        }
    }

    public class ZoomState extends Observable {
        /**
         * Zoom level A value of 1.0 means the content fits the view.
         */
        private float mZoom;

        /**
         * Pan position x-coordinate X-coordinate of zoom window center position,
         * relative to the width of the content.
         */
        private float mPanX;

        /**
         * Pan position y-coordinate Y-coordinate of zoom window center position,
         * relative to the height of the content.
         */
        private float mPanY;

        // Public methods

        /**
         * Get current x-pan
         * 
         * @return current x-pan
         */
        public float getPanX() {
            return mPanX;
        }

        /**
         * Get current y-pan
         * 
         * @return Current y-pan
         */
        public float getPanY() {
            return mPanY;
        }

        /**
         * Get current zoom value
         * 
         * @return Current zoom value
         */
        public float getZoom() {
            return mZoom;
        }

        /**
         * Help function for calculating current zoom value in x-dimension
         * 
         * @param aspectQuotient (Aspect ratio content) / (Aspect ratio view)
         * @return Current zoom value in x-dimension
         */
        public float getZoomX(float aspectQuotient) {
            return Math.min(mZoom, mZoom * aspectQuotient);
        }

        /**
         * Help function for calculating current zoom value in y-dimension
         * 
         * @param aspectQuotient (Aspect ratio content) / (Aspect ratio view)
         * @return Current zoom value in y-dimension
         */
        public float getZoomY(float aspectQuotient) {
            return Math.min(mZoom, mZoom / aspectQuotient);
        }

        /**
         * Set pan-x
         * 
         * @param panX Pan-x value to set
         */
        public void setPanX(float panX) {
            if (panX != mPanX) {
                mPanX = panX;
                setChanged();
            }
        }

        /**
         * Set pan-y
         * 
         * @param panY Pan-y value to set
         */
        public void setPanY(float panY) {
            if (panY != mPanY) {
                mPanY = panY;
                setChanged();
            }
        }

        /**
         * Set zoom
         * 
         * @param zoom Zoom value to set
         */
        public void setZoom(float zoom) {
            if (zoom != mZoom) {
                mZoom = zoom;
                setChanged();
            }
        }
    }

    public abstract class Dynamics {
        /**
         * The maximum delta time, in milliseconds, between two updates
         */
        private static final int MAX_TIMESTEP = 50;

        /** The current position */
        protected float mPosition;

        /** The current velocity */
        protected float mVelocity;

        /** The current maximum position */
        protected float mMaxPosition = Float.MAX_VALUE;

        /** The current minimum position */
        protected float mMinPosition = -Float.MAX_VALUE;

        /** The time of the last update */
        protected long mLastTime = 0;

        /**
         * Sets the state of the dynamics object. Should be called before starting
         * to call update.
         * 
         * @param position The current position.
         * @param velocity The current velocity in pixels per second.
         * @param now The current time
         */
        public void setState(final float position, final float velocity, final long now) {
            mVelocity = velocity;
            mPosition = position;
            mLastTime = now;
        }

        /**
         * Returns the current position. Normally used after a call to update() in
         * order to get the updated position.
         * 
         * @return The current position
         */
        public float getPosition() {
            return mPosition;
        }

        /**
         * Gets the velocity. Unit is in pixels per second.
         * 
         * @return The velocity in pixels per second
         */
        public float getVelocity() {
            return mVelocity;
        }

        /**
         * Used to find out if the list is at rest, that is, has no velocity and is
         * inside the the limits. Normally used to know if more calls to update are
         * needed.
         * 
         * @param velocityTolerance Velocity is regarded as 0 if less than
         *            velocityTolerance
         * @param positionTolerance Position is regarded as inside the limits even
         *            if positionTolerance above or below
         * 
         * @return true if list is at rest, false otherwise
         */
        public boolean isAtRest(final float velocityTolerance, final float positionTolerance) {
            final boolean standingStill = Math.abs(mVelocity) < velocityTolerance;
            final boolean withinLimits = mPosition - positionTolerance < mMaxPosition
                    && mPosition + positionTolerance > mMinPosition;
            return standingStill && withinLimits;
        }

        /**
         * Sets the maximum position.
         * 
         * @param maxPosition The maximum value of the position
         */
        public void setMaxPosition(final float maxPosition) {
            mMaxPosition = maxPosition;
        }

        /**
         * Sets the minimum position.
         * 
         * @param minPosition The minimum value of the position
         */
        public void setMinPosition(final float minPosition) {
            mMinPosition = minPosition;
        }

        /**
         * Updates the position and velocity.
         * 
         * @param now The current time
         */
        public void update(final long now) {
            int dt = (int)(now - mLastTime);
            if (dt > MAX_TIMESTEP) {
                dt = MAX_TIMESTEP;
            }

            onUpdate(dt);

            mLastTime = now;
        }

        /**
         * Gets the distance to the closest limit (max and min position).
         * 
         * @return If position is more than max position: distance to max position. If
         *         position is less than min position: distance to min position. If
         *         within limits: 0
         */
        protected float getDistanceToLimit() {
            float distanceToLimit = 0;

            if (mPosition > mMaxPosition) {
                distanceToLimit = mMaxPosition - mPosition;
            } else if (mPosition < mMinPosition) {
                distanceToLimit = mMinPosition - mPosition;
            }

            return distanceToLimit;
        }

        /**
         * Updates the position and velocity.
         * 
         * @param dt The delta time since last time
         */
        abstract protected void onUpdate(int dt);
    }

    /**
     * SpringDynamics is a Dynamics object that uses friction and spring physics to
     * snap to boundaries and give a natural and organic dynamic.
     */
    public class SpringDynamics extends Dynamics {

        /** Friction factor */
        private float mFriction;

        /** Spring stiffness factor */
        private float mStiffness;

        /** Spring damping */
        private float mDamping;

        /**
         * Set friction parameter, friction physics are applied when inside of snap
         * bounds.
         * 
         * @param friction Friction factor
         */
        public void setFriction(float friction) {
            mFriction = friction;
        }

        /**
         * Set spring parameters, spring physics are applied when outside of snap
         * bounds.
         * 
         * @param stiffness Spring stiffness
         * @param dampingRatio Damping ratio, < 1 underdamped, > 1 overdamped
         */
        public void setSpring(float stiffness, float dampingRatio) {
            mStiffness = stiffness;
            mDamping = dampingRatio * 2 * (float)FloatMath.sqrt(stiffness);
        }

        /**
         * Calculate acceleration at the current state
         * 
         * @return Current acceleration
         */
        private float calculateAcceleration() {
            float acceleration;

            final float distanceFromLimit = getDistanceToLimit();
            if (distanceFromLimit != 0) {
                acceleration = distanceFromLimit * mStiffness - mDamping * mVelocity;
            } else {
                acceleration = -mFriction * mVelocity;
            }

            return acceleration;
        }

        @Override
        protected void onUpdate(int dt) {
            // Update position and velocity using the Velocity verlet algorithm

            // Calculate dt in seconds as float
            final float fdt = dt / 1000f;

            // Calculate current acceleration
            final float at = calculateAcceleration();

            // Calculate next position based on current velocity and acceleration
            mPosition += mVelocity * fdt + .5f * at * fdt * fdt;

            // Calculate velocity at time t + dt/2
            // (that is velocity at half way to new time)
            mVelocity += .5f * at * fdt;

            // Calculate acceleration at new position,
            // will be used for calculating velocity at next position.
            final float atdt = calculateAcceleration();

            // Calculate velocity at time (t + dt/2) + dt/2 = t + dt
            // (that is velocity at the new time)
            mVelocity += .5f * atdt * fdt;
        }

    }

    public static class PinchZoomListener implements View.OnTouchListener {
        /**
         * Enum defining listener modes. Before the view is touched the listener is
         * in the UNDEFINED mode. Once touch starts it can enter either one of the
         * other two modes: If the user scrolls over the view the listener will
         * enter PAN mode, if the user lets his finger rest and makes a long press
         * the listener will enter ZOOM mode.
         */
        private enum Mode {
            UNDEFINED, PAN, PINCHZOOM
        }

        /** Current listener mode */
        private Mode mMode = Mode.UNDEFINED;

        /** Zoom control to manipulate */
        private DynamicZoomControl mZoomControl;

        /** X-coordinate of previously handled touch event */
        private float mX;

        /** Y-coordinate of previously handled touch event */
        private float mY;

        /** X-coordinate of latest down event */
        private float mDownX;

        /** Y-coordinate of latest down event */
        private float mDownY;

        private PointF mMidPoint = new PointF();

        /** Velocity tracker for touch events */
        private VelocityTracker mVelocityTracker;

        /** Distance touch can wander before we think it's scrolling */
        private final int mScaledTouchSlop;

        /** Maximum velocity for fling */
        private final int mScaledMaximumFlingVelocity;

        /** Distance between fingers */
        private float oldDist = 1f;
        
        private long panAfterPinchTimeout = 0;

        public PinchZoomListener(Context context) {
            mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            mScaledMaximumFlingVelocity = ViewConfiguration.get(context)
                    .getScaledMaximumFlingVelocity();
        }

        /**
         * Sets the zoom control to manipulate
         * 
         * @param control Zoom control
         */
        public void setZoomControl(DynamicZoomControl control) {
            mZoomControl = control;
        }

        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction() & MotionEvent.ACTION_MASK;
            final float x = event.getX();
            final float y = event.getY();

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mZoomControl.stopFling();
                    mDownX = x;
                    mDownY = y;
                    mX = x;
                    mY = y;
                    break;
                    
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() > 1) {
                        oldDist = spacing(event);
                        if (oldDist > 10f) {
                            midPoint(mMidPoint, event);
                            mMode = Mode.PINCHZOOM;
                        }
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                    if (mMode == Mode.PAN) {
                        final long now = System.currentTimeMillis();
                        if(panAfterPinchTimeout < now){
                            mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
                            mZoomControl.startFling(-mVelocityTracker.getXVelocity() / v.getWidth(),
                                    -mVelocityTracker.getYVelocity() / v.getHeight());
                        }
                    } else if(mMode != Mode.PINCHZOOM) {
                        mZoomControl.startFling(0, 0);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                case MotionEvent.ACTION_POINTER_UP:
                    if(event.getPointerCount() > 1 &&  mMode == Mode.PINCHZOOM){
                        panAfterPinchTimeout = System.currentTimeMillis() + 100;
                    }
                    mMode = Mode.UNDEFINED;                
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    final float dx = (x - mX) / v.getWidth();
                    final float dy = (y - mY) / v.getHeight();
                   
                    if (mMode == Mode.PAN) {
                        mZoomControl.pan(-dx, -dy);
                    } else if (mMode == Mode.PINCHZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            final float scale = newDist / oldDist;
                            final float xx = mMidPoint.x / v.getWidth();
                            final float yy = mMidPoint.y / v.getHeight();
                            mZoomControl.zoom(scale, xx, yy);
                            oldDist = newDist;
                        }
                    } else {
                        final float scrollX = mDownX - x;
                        final float scrollY = mDownY - y;

                        final float dist = (float)FloatMath.sqrt(scrollX * scrollX + scrollY * scrollY);

                        if (dist >= mScaledTouchSlop ){
                            mMode = Mode.PAN;
                        }
                    }
                    
                    mX = x;
                    mY = y;
                    break;

                default:
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    mMode = Mode.UNDEFINED;
                    break;
            }
            return true;
        }

        /** Determine the space between the first two fingers */
        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return FloatMath.sqrt(x * x + y * y);
        }

        /** Calculate the mid point of the first two fingers */
        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }
    }
    /**
     * The DynamicZoomControl is responsible for controlling a ZoomState. It makes
     * sure that pan movement follows the finger, that limits are satisfied and that
     * we can zoom into specific positions.
     * 
     * In order to implement these control mechanisms access to certain content and
     * view state data is required which is made possible through the
     * ZoomContentViewState.
     */
    public class DynamicZoomControl implements Observer {

        /** Minimum zoom level limit */
        private static final float MIN_ZOOM = 1;

        /** Maximum zoom level limit */
        private static final float MAX_ZOOM = 8;

        /** Velocity tolerance for calculating if dynamic state is resting */
        private static final float REST_VELOCITY_TOLERANCE = 0.004f;

        /** Position tolerance for calculating if dynamic state is resting */
        private static final float REST_POSITION_TOLERANCE = 0.01f;

        /** Target FPS when animating behavior such as fling and snap to */
        private static final int FPS = 60;

        /** Factor applied to pan motion outside of pan snap limits. */
        private static final float PAN_OUTSIDE_SNAP_FACTOR = .25f;

        /** Zoom state under control */
        private final ZoomState mState = new ZoomState();

        /** Object holding aspect quotient of view and content */
        private AspectQuotient mAspectQuotient;

        /**
         * Dynamics object for creating dynamic fling and snap to behavior for pan
         * in x-dimension.
         */
        private final SpringDynamics mPanDynamicsX = new SpringDynamics();

        /**
         * Dynamics object for creating dynamic fling and snap to behavior for pan
         * in y-dimension.
         */
        private final SpringDynamics mPanDynamicsY = new SpringDynamics();

        /** Minimum snap to position for pan in x-dimension */
        private float mPanMinX;

        /** Maximum snap to position for pan in x-dimension */
        private float mPanMaxX;

        /** Minimum snap to position for pan in y-dimension */
        private float mPanMinY;

        /** Maximum snap to position for pan in y-dimension */
        private float mPanMaxY;

        /** Handler for posting runnables */
        private final Handler mHandler = new Handler();

        /** Creates new zoom control */
        public DynamicZoomControl() {
            mPanDynamicsX.setFriction(3f);
            mPanDynamicsY.setFriction(3f);
            mPanDynamicsX.setSpring(50f, 1f);
            mPanDynamicsY.setSpring(50f, 1f);
        }

        /**
         * Set reference object holding aspect quotient
         * 
         * @param aspectQuotient Object holding aspect quotient
         */
        public void setAspectQuotient(AspectQuotient aspectQuotient) {
            if (mAspectQuotient != null) {
                mAspectQuotient.deleteObserver(this);
            }

            mAspectQuotient = aspectQuotient;
            mAspectQuotient.addObserver(this);
        }

        /**
         * Get zoom state being controlled
         * 
         * @return The zoom state
         */
        public ZoomState getZoomState() {
            return mState;
        }

        /**
         * Zoom
         * 
         * @param f Factor of zoom to apply
         * @param x X-coordinate of invariant position
         * @param y Y-coordinate of invariant position
         */
        public void zoom(float f, float x, float y) {
            final float aspectQuotient = mAspectQuotient.get();

            final float prevZoomX = mState.getZoomX(aspectQuotient);
            final float prevZoomY = mState.getZoomY(aspectQuotient);

            mState.setZoom(mState.getZoom() * f);
            limitZoom();

            final float newZoomX = mState.getZoomX(aspectQuotient);
            final float newZoomY = mState.getZoomY(aspectQuotient);

            // Pan to keep x and y coordinate invariant
            mState.setPanX(mState.getPanX() + (x - .5f) * (1f / prevZoomX - 1f / newZoomX));
            mState.setPanY(mState.getPanY() + (y - .5f) * (1f / prevZoomY - 1f / newZoomY));

            updatePanLimits();

            mState.notifyObservers();
        }

        /**
         * Pan
         * 
         * @param dx Amount to pan in x-dimension
         * @param dy Amount to pan in y-dimension
         */
        public void pan(float dx, float dy) {
            final float aspectQuotient = mAspectQuotient.get();

            dx /= mState.getZoomX(aspectQuotient);
            dy /= mState.getZoomY(aspectQuotient);

            if (mState.getPanX() > mPanMaxX && dx > 0 || mState.getPanX() < mPanMinX && dx < 0) {
                dx *= PAN_OUTSIDE_SNAP_FACTOR;
            }
            if (mState.getPanY() > mPanMaxY && dy > 0 || mState.getPanY() < mPanMinY && dy < 0) {
                dy *= PAN_OUTSIDE_SNAP_FACTOR;
            }

            final float newPanX = mState.getPanX() + dx;
            final float newPanY = mState.getPanY() + dy;

            mState.setPanX(newPanX);
            mState.setPanY(newPanY);

            mState.notifyObservers();
        }

        /**
         * Runnable that updates dynamics state
         */
        private final Runnable mUpdateRunnable = new Runnable() {
            public void run() {
                final long startTime = SystemClock.uptimeMillis();
                mPanDynamicsX.update(startTime);
                mPanDynamicsY.update(startTime);
                final boolean isAtRest = mPanDynamicsX.isAtRest(REST_VELOCITY_TOLERANCE,
                        REST_POSITION_TOLERANCE)
                        && mPanDynamicsY.isAtRest(REST_VELOCITY_TOLERANCE, REST_POSITION_TOLERANCE);
                mState.setPanX(mPanDynamicsX.getPosition());
                mState.setPanY(mPanDynamicsY.getPosition());

                if (!isAtRest) {
                    final long stopTime = SystemClock.uptimeMillis();
                    mHandler.postDelayed(mUpdateRunnable, 1000 / FPS - (stopTime - startTime));
                }

                mState.notifyObservers();
            }
        };

        /**
         * Release control and start pan fling animation
         * 
         * @param vx Velocity in x-dimension
         * @param vy Velocity in y-dimension
         */
        public void startFling(float vx, float vy) {
            final float aspectQuotient = mAspectQuotient.get();
            final long now = SystemClock.uptimeMillis();

            mPanDynamicsX.setState(mState.getPanX(), vx / mState.getZoomX(aspectQuotient), now);
            mPanDynamicsY.setState(mState.getPanY(), vy / mState.getZoomY(aspectQuotient), now);

            mPanDynamicsX.setMinPosition(mPanMinX);
            mPanDynamicsX.setMaxPosition(mPanMaxX);
            mPanDynamicsY.setMinPosition(mPanMinY);
            mPanDynamicsY.setMaxPosition(mPanMaxY);

            mHandler.post(mUpdateRunnable);
        }

        /**
         * Stop fling animation
         */
        public void stopFling() {
            mHandler.removeCallbacks(mUpdateRunnable);
        }

        /**
         * Help function to figure out max delta of pan from center position.
         * 
         * @param zoom Zoom value
         * @return Max delta of pan
         */
        private float getMaxPanDelta(float zoom) {
            return Math.max(0f, .5f * ((zoom - 1) / zoom));
        }

        /**
         * Force zoom to stay within limits
         */
        private void limitZoom() {
            if (mState.getZoom() < MIN_ZOOM) {
                mState.setZoom(MIN_ZOOM);
            } else if (mState.getZoom() > MAX_ZOOM) {
                mState.setZoom(MAX_ZOOM);
            }
        }

        /**
         * Update limit values for pan
         */
        private void updatePanLimits() {
            final float aspectQuotient = mAspectQuotient.get();

            final float zoomX = mState.getZoomX(aspectQuotient);
            final float zoomY = mState.getZoomY(aspectQuotient);

            mPanMinX = .5f - getMaxPanDelta(zoomX);
            mPanMaxX = .5f + getMaxPanDelta(zoomX);
            mPanMinY = .5f - getMaxPanDelta(zoomY);
            mPanMaxY = .5f + getMaxPanDelta(zoomY);
        }

        // Observable interface implementation

        public void update(Observable observable, Object data) {
            limitZoom();
            updatePanLimits();
        }

    }

}
