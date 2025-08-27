/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.messaging.FirebaseMessaging

class PreferencesActivity : FragmentActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = findViewById<Toolbar?>(R.id.toolbar_actionbar)

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }

        addPreferencesFragment()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override val selfNavDrawerItem: Int
        get() = R.id.navdrawer_settings

    fun addPreferencesFragment() {
        val clss = PreferencesFragment::class.java
        val tag = clss.getSimpleName()
        val fm = supportFragmentManager
        var f = fm.findFragmentByTag(tag)
        if (f == null) {
            f = fm.getFragmentFactory().instantiate(getClassLoader(), clss.getName())
            f.setArguments(getIntent().getExtras())
            val ft = fm.beginTransaction()
            ft.add(R.id.fragment_container, f, tag)
            ft.commit()
        }
    }

    class PreferencesFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
        private var mSharedPrefs: SharedPreferences? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val homeAirport = findPreference<EditTextPreference?>(KEY_HOME_AIRPORT)
            if (homeAirport != null) {
                homeAirport.setSummaryProvider(
                    SummaryProvider { preference: EditTextPreference? ->
                        val value = preference!!.text
                        if (TextUtils.isEmpty(value)) {
                            return@SummaryProvider "Home airport is not set"
                        } else {
                            return@SummaryProvider "Home airport set to: $value"
                        }
                    })

                homeAirport.setOnBindEditTextListener { editText: EditText? ->
                    editText!!.setInputType(
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                    )
                }
            }

            val radius = findPreference<ListPreference?>(KEY_LOCATION_NEARBY_RADIUS)
            radius?.setSummaryProvider(SummaryProvider { preference: Preference? -> "Show locations within " + radius.getValue() + " NM radius" })

            val homeScreen = findPreference<ListPreference?>(KEY_HOME_SCREEN)
            homeScreen?.setSummaryProvider(SummaryProvider { preference: Preference? -> "Show " + homeScreen.getValue() + " screen on startup" })
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            setupWindowInsetsListener()
        }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            addPreferencesFromResource(R.xml.preferences)
            mSharedPrefs = preferenceScreen.getSharedPreferences()
        }

        override fun onResume() {
            super.onResume()

            // Initialize the preference screen
            onSharedPreferenceChanged(mSharedPrefs, KEY_LOCATION_NEARBY_RADIUS)
            onSharedPreferenceChanged(mSharedPrefs, KEY_HOME_AIRPORT)
            onSharedPreferenceChanged(mSharedPrefs, KEY_HOME_SCREEN)

            // Set up a listener whenever a key changes
            mSharedPrefs!!.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            // Unregister the listener whenever a key changes
            mSharedPrefs!!.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (KEY_FCM_ENABLE == key) {
                val enabled = mSharedPrefs!!.getBoolean(KEY_FCM_ENABLE, true)
                if (enabled) {
                    FirebaseMessaging.getInstance().subscribeToTopic("all")
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
                }
            }
        }

        private fun setupWindowInsetsListener() {
            ViewCompat.setOnApplyWindowInsetsListener(listView) { v, insets ->
                val innerPadding = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(
                    innerPadding.left,
                    0,
                    innerPadding.right,
                    innerPadding.bottom)
                insets
            }
        }

    }

    companion object {
        const val KEY_HOME_AIRPORT: String = "home_airport"
        const val KEY_LOCATION_USE_GPS: String = "location_use_gps"
        const val KEY_LOCATION_NEARBY_RADIUS: String = "location_nearby_radius"
        const val KEY_SHOW_EXTRA_RUNWAY_DATA: String = "extra_runway_data"
        const val KEY_AUTO_DOWNLOAD_ON_3G: String = "auto_download_on_3G"
        const val KEY_DISCLAIMER_AGREED: String = "disclaimer_agreed"
        const val KEY_SHOW_LOCAL_TIME: String = "show_local_time"
        const val KEY_HOME_SCREEN: String = "home_screen"
        const val KEY_ALWAYS_SHOW_NEARBY: String = "always_show_nearby"
        const val KEY_FCM_ENABLE: String = "fcm_enable"
    }
}
