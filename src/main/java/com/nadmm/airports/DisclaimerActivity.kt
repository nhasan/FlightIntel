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

import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.view.View
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class DisclaimerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.disclaimer_view)
        val webView = findViewById<WebView>(R.id.disclaimer_content)
        webView.loadUrl("file:///android_asset/disclaimer.html")
        val btnAgree = findViewById<Button>(R.id.btn_agree)
        btnAgree.setOnClickListener { v: View? ->
            markDisclaimerAgreed(true)
            val intent = Intent(this@DisclaimerActivity, FlightIntel::class.java)
            startActivity(intent)
            finish()
        }
        val btnDisagree = findViewById<Button>(R.id.btn_disagree)
        btnDisagree.setOnClickListener { v: View? ->
            markDisclaimerAgreed(false)
            finish()
        }
    }

    private fun markDisclaimerAgreed(agreed: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        editor.putBoolean(PreferencesActivity.KEY_DISCLAIMER_AGREED, agreed)
        editor.apply()
    }
}