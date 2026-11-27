/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Shows a web page, such as a chart on vfrmap.com, in the app rather than
 * the browser. The page fills the screen below the toolbar, edge to edge.
 * Links within the page's site stay here; others open in the browser.
 */
class WebViewActivity : ActivityBase() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity)

        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        setActionBarTitle(intent.getStringExtra(EXTRA_TITLE) ?: "", intent.getStringExtra(EXTRA_SUBTITLE))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webView = findViewById(R.id.web_view)
        // Clear of the display cutout at the sides; the page runs on under
        // the navigation bar at the bottom
        ViewCompat.setOnApplyWindowInsetsListener(webView) { v, insets ->
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(left = cutout.left, right = cutout.right)
            insets
        }
        val host = url.toUri().host
        webView.settings.apply {
            // The chart viewers are scripted maps
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == host) return false
                startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }
        }

        // Back steps back within the page first
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // After a rotation, the page picks up where it was
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(url)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "URL"
        private const val EXTRA_TITLE = "TITLE"
        private const val EXTRA_SUBTITLE = "SUBTITLE"

        fun intent(context: Context, url: String, title: String, subtitle: String? = null): Intent =
            Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_SUBTITLE, subtitle)
    }
}
