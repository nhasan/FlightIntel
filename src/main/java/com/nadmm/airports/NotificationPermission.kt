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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Asks once, on Android 13 and later, for permission to show app messages,
 * which are on by default. A short explanation comes first; declining turns
 * app messages off so the setting matches what the user sees.
 */
class NotificationPermission(private val activity: ActivityBase) {
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) setAppMessagesEnabled(activity, false)
    }

    /**
     * Shows the explanation, then the system request, unless the permission
     * is granted, app messages are off, or it was asked before. Waits for a
     * location request in progress, so the two never overlap.
     */
    fun askOnce() {
        if (isGranted(activity)) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        if (prefs.getBoolean(KEY_ASKED, false)) return
        if (!prefs.getBoolean(PreferencesActivity.KEY_FCM_ENABLE, true)) return
        // Let the screen's fragments start their own requests first
        activity.postRunnable({
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                || LocationListFragmentBase.isRequestingPermission
                || prefs.getBoolean(KEY_ASKED, false)
            ) {
                // Asked again from onResume once the other request is done
                return@postRunnable
            }
            prefs.edit { putBoolean(KEY_ASKED, true) }
            MaterialAlertDialogBuilder(activity)
                .setTitle("Allow notifications?")
                .setMessage("FlightIntel occasionally sends messages about the app and its " +
                        "data, such as when new data is available. You can turn them off " +
                        "later in Settings.")
                .setPositiveButton("Continue") { _, _ ->
                    markRequested(activity)
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton("Not now") { _, _ ->
                    setAppMessagesEnabled(activity, false)
                }
                .setCancelable(false)
                .show()
        }, PROMPT_DELAY_MILLIS)
    }

    companion object {
        // The explanation was shown, and the system request was shown
        private const val KEY_ASKED = "notification_permission_asked"
        private const val KEY_REQUESTED = "notification_permission_requested"
        private const val PROMPT_DELAY_MILLIS = 500L

        /** True when the app may show notifications; always so before Android 13. */
        fun isGranted(context: Context) =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        /**
         * True once the system request has been shown, after which Android
         * may stop showing it and only the system settings can grant it.
         */
        fun wasRequested(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTED, false)

        fun markRequested(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putBoolean(KEY_ASKED, true)
                putBoolean(KEY_REQUESTED, true)
            }

        /** Turns app messages on or off, in the settings and with Firebase. */
        fun setAppMessagesEnabled(context: Context, enabled: Boolean) {
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putBoolean(PreferencesActivity.KEY_FCM_ENABLE, enabled)
            }
            val messaging = FirebaseMessaging.getInstance()
            if (enabled) messaging.subscribeToTopic("all")
            else messaging.unsubscribeFromTopic("all")
        }

        /** Opens the system notification settings for this app. */
        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }
}
