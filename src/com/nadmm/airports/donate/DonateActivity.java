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

package com.nadmm.airports.donate;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.Application;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.billing.BillingService;
import com.nadmm.airports.billing.BillingService.RequestPurchase;
import com.nadmm.airports.billing.BillingService.RestoreTransactions;
import com.nadmm.airports.billing.Consts.PurchaseState;
import com.nadmm.airports.billing.Consts.ResponseCode;
import com.nadmm.airports.billing.PurchaseObserver;
import com.nadmm.airports.billing.ResponseHandler;
import com.nadmm.airports.donate.DonateDatabase.Donations;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public class DonateActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.fragment_activity_layout ) );

        Bundle args = getIntent().getExtras();
        addFragment( DonateFragment.class, args );
    }

    @Override
    public void onBackPressed() {
    }

    public static class DonateFragment extends FragmentBase implements OnClickListener {

        private static final String DONATIONS_INITIALIZED = "donations_initialized";

        private static final int BILLING_CANNOT_CONNECT = 1;
        private static final int BILLING_NOT_SUPPORTED = 2;
        private static final int BILLING_ERROR_RESTORE = 3;
        private static final int BILLING_AVAILABLE = 4;

        private BillingService mBillingService;
        private DonateDatabase mDonateDatabase;
        private DonationsObserver mDonationsObserver;
        private Cursor mCursor;
        private boolean mBillingSupported;

        private class DonationLevel {
            public String productId;
            public String description;
            public double amount;

            public DonationLevel( String productId, String description, double amount ) {
                this.productId = productId;
                this.description = description;
                this.amount = amount;
            }
        }

        private final DonationLevel[] mDonationLevels = {
            new DonationLevel( "donate_199", "Frugal Flyer", 1.99 ),
            new DonationLevel( "donate_399", "Generous Flyer", 3.99 )
        };

        private final class DonateTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                Cursor[] cursors = new Cursor[ 1 ];
                cursors[ 0 ] = mDonateDatabase.queryAlldonations();

                return cursors;
            }

            @Override
            protected boolean onResult( Cursor[] result ) {
                mCursor = result[ 0 ];
                mBillingSupported = mBillingService.checkBillingSupported();
                if ( !mBillingSupported ) {
                    showDonationView( BILLING_CANNOT_CONNECT );
                }
                return false;
            }

        }

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View view = inflater.inflate( R.layout.donate_detail_view, container, false );
            return view;
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            mDonateDatabase = new DonateDatabase( getActivity() );

            Handler handler = new Handler();
            mDonationsObserver = new DonationsObserver( handler );

            mBillingService = new BillingService();
            mBillingService.setContext( getActivity() );

            super.onActivityCreated( savedInstanceState );
        }

        @Override
        public void onStart() {
            setBackgroundTask( new DonateTask() ).execute();
            ResponseHandler.register( mDonationsObserver );
            super.onStart();
        }

        @Override
        public void onStop() {
            if ( mCursor != null ) {
                mCursor.close();
            }
            ResponseHandler.unregister( mDonationsObserver );
            super.onStop();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mDonateDatabase.close();
            mBillingService.unbind();
        }

        protected void refreshDonationsView() {
            setContentShown( false );
            setBackgroundTask( new DonateTask() ).execute();
        }

        protected void showDonationView( int status ) {
            HashMap<String, Time> donations = new HashMap<String, Time>();
            if ( mCursor.moveToFirst() ) {
                do {
                    String productId = mCursor.getString(
                            mCursor.getColumnIndex( Donations.PRODUCT_ID ) );
                    Time time = new Time();
                    time.parse3339( mCursor.getString( mCursor.getColumnIndex(
                            Donations.PURCHASE_TIME ) ) );
                    donations.put( productId, time );
                } while ( mCursor.moveToNext() );
            }

            TextView tv = (TextView) findViewById( R.id.donate_text );

            if ( status == BILLING_CANNOT_CONNECT ) {
                tv.setText( "ERROR:\nCannot connect to Google Play application. Your version of app"
                        + " may be out of date. You can continue to use this app but certain"
                        + " features may not be available." );                
            } else if ( status == BILLING_NOT_SUPPORTED ) {
                tv.setText( "ERROR:\nGoogle Play in-app billing is not available this time. You can"
                        + " continue to use this app but certain features may not be available." );
            } else if ( status == BILLING_ERROR_RESTORE ) {
                tv.setText( "ERROR:\nThere was an error trying to restore your past donations. This"
                        + " could be a temporary failure. Please try again later." );
            } else {
                tv.setText( "Please consider donating to help me recover my costs to develop"
                        + " the app and provide you with data updates. You may also choose not to"
                        + " donate at all and that is fine too."
                        + "\n\n"
                        + "You will be redirected to Google Play checkout to allow you to securely"
                        + " complete your payment transaction."
                        + "\n\n"
                        + "Please donate generously to show your appreciation." );

                tv = (TextView) findViewById( R.id.donate_level_label );
                LinearLayout layout = (LinearLayout) findViewById( R.id.donate_level_layout );
                layout.removeAllViews();

                for ( int i = 0; i < mDonationLevels.length; ++i ) {
                    DonationLevel level = mDonationLevels[ i ];
                    if ( !donations.containsKey( level.productId ) ) {
                        View row = addRow( layout, level.description,
                                FormatUtils.formatCurrency( level.amount ) );
                        row.setTag( level.productId );
                        row.setOnClickListener( this );
                        row.setBackgroundResource( getRowSelector( i, 
                                mDonationLevels.length-donations.size() ) );
                    }
                }

                if ( layout.getChildCount() > 0 ) {
                    tv.setVisibility( View.VISIBLE );
                    layout.setVisibility( View.VISIBLE );
                } else {
                    tv.setVisibility( View.GONE );
                    layout.setVisibility( View.GONE );
                }
            }

            tv = (TextView) findViewById( R.id.donate_text2 );
            LinearLayout layout = (LinearLayout) findViewById( R.id.past_donations_layout );
            layout.removeAllViews();
            if ( donations.isEmpty() ) {
                Application.sDonationDone = false;
                addRow( layout, "No donations made yet" );
                tv.setVisibility( View.GONE );
            } else {
                Application.sDonationDone = true;
                for ( String productId : donations.keySet() ) {
                    Time time = donations.get( productId );
                    DonationLevel level = getDonationLevel( productId );
                    if ( level != null ) {
                        addRow( layout, TimeUtils.formatDateTimeLocal( 
                                getActivity(), time.toMillis( true ) ),
                                FormatUtils.formatCurrency( level.amount ) );
                    }
                }

                tv.setText( "You can restore the above donations on your other devices by"
                        + " simply visiting this screen on those devices." );
                tv.setVisibility( View.VISIBLE );
            }

            setContentShown( true );
        }

        protected int getRowSelector( int row, int total ) {
            int resid;
            if ( total == 1 ) {
                resid = R.drawable.row_selector;
            } else if ( row == 0 ) {
                resid = R.drawable.row_selector_top;
            } else if ( row == total-1 ) {
                resid = R.drawable.row_selector_bottom;
            } else {
                resid = R.drawable.row_selector_middle;
            }
            return resid;
        }

        protected DonationLevel getDonationLevel( String productId ) {
            for ( DonationLevel level : mDonationLevels ) {
                if ( level.productId.equals( productId ) ) {
                    return level;
                }
            }
            return null;
        }

        private class DonationsObserver extends PurchaseObserver {

            public DonationsObserver( Handler handler ) {
                super( getActivity(), handler );
            }

            @Override
            public void onBillingSupported( boolean supported ) {
                Log.d( "onBillingSupported", String.valueOf( supported ) );
                if ( supported ) {
                    if ( restoreDatabase() ) {
                        showDonationView( BILLING_AVAILABLE );
                    }
                } else {
                    showDonationView( BILLING_NOT_SUPPORTED );
                }
            }

            @Override
            public void onPurchaseStateChange( PurchaseState purchaseState,
                    String itemId, int quantity, long purchaseTime,
                    String developerPayload ) {
                refreshDonationsView();
            }

            @Override
            public void onRequestPurchaseResponse( RequestPurchase request,
                    ResponseCode responseCode ) {
                if ( responseCode == ResponseCode.RESULT_OK ) {
                    UiUtils.showToast( getActivity(), "Donation request sent successfully" );
                } else if ( responseCode == ResponseCode.RESULT_USER_CANCELED ) {
                    UiUtils.showToast( getActivity(), "Donation request cancelled" );
                } else {
                    UiUtils.showToast( getActivity(), "Donation request failed" );
                }
            }

            @Override
            public void onRestoreTransactionsResponse( RestoreTransactions request,
                    ResponseCode responseCode ) {
                Log.d( "onRestoreTransactionsResponse", responseCode.toString() );
                if ( responseCode == ResponseCode.RESULT_OK ) {
                    // Update the shared preferences so that we don't perform
                    // a RestoreTransactions again.
                    SharedPreferences prefs = getActivity().getPreferences( Context.MODE_PRIVATE );
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean( DONATIONS_INITIALIZED, true );
                    edit.commit();
                    UiUtils.showToast( getActivity(), "Donation transactions restored" );
                    showDonationView( BILLING_AVAILABLE );
                } else {
                    showDonationView( BILLING_ERROR_RESTORE );
                }
            }

        }

        private boolean restoreDatabase() {
            SharedPreferences prefs = getActivity().getPreferences( MODE_PRIVATE );
            boolean initialized = prefs.getBoolean( DONATIONS_INITIALIZED, false );
            if ( !initialized ) {
                mBillingService.restoreTransactions();
                UiUtils.showToast( getActivity(), "Restoring donation transactions" );
            }
            return initialized;
        }

        @Override
        public void onClick( View v ) {
            String productId = (String) v.getTag();
            if ( !mBillingService.requestPurchase( productId, null ) ) {
                UiUtils.showToast( getActivity(), "Google Play billing service is not supported" );
            }
        }

    }

}
