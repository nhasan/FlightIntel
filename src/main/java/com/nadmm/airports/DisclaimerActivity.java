/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2018 Nadeem Hasan <nhasan@nadmm.com>
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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;

import androidx.preference.PreferenceManager;

public class DisclaimerActivity extends Activity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.disclaimer_view );
        WebView webView = findViewById( R.id.disclaimer_content );
        webView.loadUrl( "file:///android_asset/disclaimer.html" );

        Button btnAgree = findViewById( R.id.btn_agree );
        btnAgree.setOnClickListener( v -> {
            markDisclaimerAgreed( true );
            Intent intent = new Intent( DisclaimerActivity.this, FlightIntel.class );
            startActivity( intent );
            finish();
        } );

        Button btnDisagree = findViewById( R.id.btn_disagree );
        btnDisagree.setOnClickListener( v -> {
            markDisclaimerAgreed( false );
            finish();
        } );
    }

    protected void markDisclaimerAgreed( boolean agreed ) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean( PreferencesActivity.KEY_DISCLAIMER_AGREED, agreed );
        editor.apply();
    }

}
