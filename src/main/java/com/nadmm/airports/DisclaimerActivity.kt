/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.view.View
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.nadmm.airports.utils.UiUtils
import androidx.core.content.edit

class DisclaimerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Like ActivityBase, so this screen is edge-to-edge on all versions
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.disclaimer_view)
        UiUtils.setupButtonBarInsets(findViewById(R.id.root), findViewById(R.id.button_bar))
        val webView = findViewById<WebView>(R.id.disclaimer_content)
        // Transparent, so the screen's theme background shows through the
        // page in light and dark mode alike
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.loadUrl("file:///android_asset/disclaimer.html")
        val btnAgree = findViewById<Button>(R.id.btn_agree)
        btnAgree.setOnClickListener {
            markDisclaimerAgreed(true)
            val intent = Intent(this@DisclaimerActivity, FlightIntel::class.java)
            startActivity(intent)
            finish()
        }
        val btnDisagree = findViewById<Button>(R.id.btn_disagree)
        btnDisagree.setOnClickListener {
            markDisclaimerAgreed(false)
            finish()
        }
    }

    private fun markDisclaimerAgreed(agreed: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit {
            putBoolean(PreferencesActivity.KEY_DISCLAIMER_AGREED, agreed)
        }
    }
}