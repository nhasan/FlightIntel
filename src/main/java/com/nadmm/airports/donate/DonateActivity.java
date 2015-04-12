/*
 * FlightIntel
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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
import com.nadmm.airports.FlightIntel;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.billing.utils.IabHelper;
import com.nadmm.airports.billing.utils.IabResult;
import com.nadmm.airports.billing.utils.Inventory;
import com.nadmm.airports.billing.utils.Purchase;
import com.nadmm.airports.billing.utils.SkuDetails;
import com.nadmm.airports.donate.DonateDatabase.Donations;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DonateActivity extends ActivityBase {

    static final String TAG = "DonateActivity";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Bundle args = getIntent().getExtras();
        addFragment( DonateFragment.class, args );
    }

    protected void setContentView() {
        setContentView( createContentView( R.layout.fragment_activity_layout ) );
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent( this, FlightIntel.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity( intent );
        finish();
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        DonateFragment f = (DonateFragment) getFragment( DonateFragment.class );
        if ( f != null )
        {
            f.onActivityResult( requestCode, resultCode, data );
        }
    }

    public static class DonateFragment extends FragmentBase implements OnClickListener {

        private final int RC_REQUEST = 10001;
        private IabHelper mHelper;
        private DonateDatabase mDonateDb;

        // Listener that's called when we finish querying the items and subscriptions we own
        private IabHelper.QueryInventoryFinishedListener mGotInventoryListener =
                new IabHelper.QueryInventoryFinishedListener() {
                    public void onQueryInventoryFinished( IabResult result, Inventory inventory ) {
                        Log.d( TAG, "Query inventory finished." );

                        // Have we been disposed off in the meantime? If so, quit.
                        if ( mHelper == null ) return;

                        if ( result.isFailure() ) {
                            showDonationView( null );
                            return;
                        }
                        Log.d( TAG, "Query inventory was successful." );

                        showDonationView( inventory );
                    }
                };

        // Callback for when a purchase is finished
        IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
                = new IabHelper.OnIabPurchaseFinishedListener() {
            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                Log.d( TAG, "Purchase finished: " + result + ", purchase: " + purchase );

                // if we were disposed of in the meantime, quit.
                if ( mHelper == null ) return;

                if ( result.isFailure() ) {
                    showDonationView( null );
                    return;
                }

                Log.d( TAG, "Purchase successful." );
                // Re-query the inventor which will trigger refresh of the display
                mHelper.queryInventoryAsync( mGotInventoryListener );
            }
        };

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            return inflater.inflate( R.layout.donate_detail_view, container, false );
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            super.onActivityCreated( savedInstanceState );

            mDonateDb = new DonateDatabase( getActivity() );

            String base64EncodedPublicKey =
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA16lkZLVQvBijNLmctANqxAzi95ce"+
                            "T7npeFlToXvPRs0ILZRXAz/K3upoGoOB7QNPBdpI7EIboz5CrLdTRe1h3XT9j5Gu5i/shDdb"+
                            "00ydybIF9gImFdwd+63THXN3hKcATNa7+lbLWsgSdz5SKqJQ9J27a5ipEMm+AwBykdUIQkt4"+
                            "+POpmxnBiT5KZO3IMyApgDGUPfHtC5Vr0bEkybDwR1JNmu53uulH45q+DdyX4btQR2q6vjqD"+
                            "BGhUlIdqeqAc+f98ZNONVupwHjLjAuNh3uWO0Aorrcm+q3K1PzQG6+kk0sxuTcSH15ktTqzq"+
                            "ACOY0m8hxy7jc8pAj6sxpWPeWQIDAQAB";

            // Create the helper, passing it our context and the public key to verify signatures with
            Log.d( TAG, "Creating IAB helper." );
            mHelper = new IabHelper( getActivity(), base64EncodedPublicKey );

            // Enable debug logging
            mHelper.enableDebugLogging( true );

            // Start setup. This is asynchronous and the specified listener
            // will be called once setup completes.
            Log.d( TAG, "Starting setup." );
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished( IabResult result ) {
                    if ( !result.isSuccess() ) {
                        Log.d( TAG, "Setup failed." );
                        showDonationView( null );
                        return;
                    }

                    // Have we been disposed of in the meantime? If so, quit.
                    if ( mHelper == null ) return;

                    ArrayList<String> skuList = new ArrayList<String>();
                    skuList.add( "donate_199" );
                    skuList.add( "donate_399" );
                    skuList.add( "donate_599" );

                    // IAB is fully set up. Now, let's get an inventory of stuff we own.
                    Log.d( TAG, "Setup successful. Querying inventory." );
                    mHelper.queryInventoryAsync( true, skuList, mGotInventoryListener );
                }
            });
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mDonateDb.close();

            // very important:
            Log.d( TAG, "Destroying helper." );
            if ( mHelper != null ) {
                mHelper.dispose();
                mHelper = null;
            }
        }

        @Override
        public void onClick( View v ) {
            String sku = (String) v.getTag();
            String payload = "";
            mHelper.launchPurchaseFlow( getActivity(), sku, RC_REQUEST,
                    mPurchaseFinishedListener, payload );
        }

        @Override
        public void onActivityResult( int requestCode, int resultCode, Intent data ) {
            Log.d( TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data );
            if ( mHelper == null ) return;

            // Pass on the activity result to the helper for handling
            if ( !mHelper.handleActivityResult( requestCode, resultCode, data ) ) {
                // not handled, so handle it ourselves (here's where you'd
                // perform any handling of activity results not related to in-app
                // billing...
                super.onActivityResult( requestCode, resultCode, data );
            }
            else {
                Log.d( TAG, "onActivityResult handled by IABUtil." );
            }
        }

        protected void showDonationView( Inventory inventory ) {
            TextView tv = (TextView) findViewById( R.id.donate_text );

            if ( inventory != null ) {
                tv.setText( "Please consider making a donation to help me recover the costs to"
                        + " develop FlightIntel and provide you with timely data updates. You may"
                        + " also choose not to donate at all and that is fine too."
                        + "\n\n"
                        + "Thank you for your support." );

                tv = (TextView) findViewById( R.id.donate_level_label );
                LinearLayout layout = (LinearLayout) findViewById( R.id.donate_level_layout );
                layout.removeAllViews();

                List<String> skus = inventory.getAllSkus();
                List<String> purchases = new ArrayList<String>();
                for ( String sku : skus ) {
                    if ( inventory.hasPurchase( sku )
                            && inventory.getPurchase( sku ).getPurchaseState() == 0 ) {
                        purchases.add( sku );
                    } else {
                        SkuDetails skuDetails = inventory.getSkuDetails( sku );
                        View row = addRow( layout, skuDetails.getTitle(), skuDetails.getPrice() );
                        row.setTag( sku );
                        row.setOnClickListener( this );
                        row.setBackgroundResource( R.drawable.row_selector_middle );
                    }
                }

                if ( layout.getChildCount() > 0 ) {
                    tv.setVisibility( View.VISIBLE );
                    layout.setVisibility( View.VISIBLE );
                } else {
                    tv.setVisibility( View.GONE );
                    layout.setVisibility( View.GONE );
                }

                tv = (TextView) findViewById( R.id.donate_text2 );
                layout = (LinearLayout) findViewById( R.id.past_donations_layout );
                layout.removeAllViews();
                mDonateDb.deleteAllDonations();
                if ( purchases.isEmpty() ) {
                    Application.sDonationDone = false;
                    addRow( layout, "No donations made yet" );
                    tv.setVisibility( View.GONE );
                } else {
                    Application.sDonationDone = true;
                    for ( String sku : purchases ) {
                        Purchase purchase = inventory.getPurchase( sku );
                        Log.d( TAG, "STATE: "+purchase.getPurchaseState() );
                        SkuDetails skuDetails = inventory.getSkuDetails( sku );
                        String orderId = purchase.getOrderId();
                        addRow( layout, skuDetails.getTitle(),
                                skuDetails.getPrice(),
                                TimeUtils.formatDateTimeLocal( getActivity(),
                                        purchase.getPurchaseTime() ),
                                orderId.substring( orderId.indexOf( '.' )+1 ) );
                        mDonateDb.updateDonation( purchase.getOrderId(), sku,
                                purchase.getPurchaseState(), purchase.getPurchaseTime() );
                    }

                    tv.setText( "You can restore the above donations on your other devices by"
                            + " simply visiting donations screen on those devices." );
                    tv.setVisibility( View.VISIBLE );
                }
            } else {
                tv.setText( "ERROR:\nGoogle Play in-app billing is not available or supported." );
            }

            setContentShown( true );
        }

    }

}
