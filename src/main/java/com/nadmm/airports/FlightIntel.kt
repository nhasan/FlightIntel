/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2026 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessaging
import com.nadmm.airports.afd.AfdMainActivity
import com.nadmm.airports.data.DatabaseManager.Catalog
import com.nadmm.airports.data.DownloadActivity
import com.nadmm.airports.utils.ExternalStorageActivity
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.UiUtils
import com.nadmm.airports.wx.WxMainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The launcher: checks the installed data off the main thread, then opens
 * the disclaimer, the download screen, or the chosen home screen.
 */
class FlightIntel : ActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Application.version = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
        }

        lifecycleScope.launch {
            val installed = withContext(Dispatchers.IO) { countInstalledDatabases() }
            startMainActivity(installed)
        }
    }

    /**
     * How many current databases are installed and can be opened, stopping
     * at the first that cannot.
     */
    private fun countInstalledDatabases(): Int {
        var installed = 0
        dbManager.currentFromCatalog?.use { c ->
            if (!c.moveToFirst()) return@use
            do {
                val end = TimeUtils.parse3339(c.getString(c.getColumnIndexOrThrow(Catalog.END_DATE)))
                    ?: break
                // Try to make sure we can open the database
                val type = c.getString(c.getColumnIndexOrThrow(Catalog.TYPE))
                if (dbManager.getDatabase(type) == null) break
                installed++
            } while (c.moveToNext())
        }
        return installed
    }

    private fun startMainActivity(installed: Int) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        FirebaseMessaging.getInstance().apply {
            if (prefAlertsEnabled) {
                subscribeToTopic("all")
                if (prefHomeAirport == "TEST") {
                    subscribeToTopic("test")
                }
            } else {
                unsubscribeFromTopic("all")
                unsubscribeFromTopic("test")
            }
        }

        val intent = when {
            !prefDisclaimerAgreed -> Intent(this, DisclaimerActivity::class.java)
            !SystemUtils.isExternalStorageAvailable ->
                Intent(this, ExternalStorageActivity::class.java)
            installed < REQUIRED_DATABASES -> {
                UiUtils.showToast(this, "Please download the required database")
                Intent(this, DownloadActivity::class.java)
            }
            prefHomeScreen == getString(R.string.afd) -> Intent(this, AfdMainActivity::class.java)
            else -> Intent(this, WxMainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        // The databases the app needs before it can show anything
        private const val REQUIRED_DATABASES = 4
    }
}
