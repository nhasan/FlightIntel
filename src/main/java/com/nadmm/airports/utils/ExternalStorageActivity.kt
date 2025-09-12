/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.FlightIntel
import com.nadmm.airports.R

class ExternalStorageActivity : ActivityBase() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.external_storage_view)

        val tv = findViewById<TextView>(R.id.storage_desc_text)
        tv.text = "This application uses external SD card for storing it's databases. " +
                "As a result, it will not function if the external SD card is not available." +
                "\n\n" +
                "The SD card is normally unavailable if the device is connected via USB to a " +
                "computer and mounted as a storage device."
        val btnTryNow = findViewById<Button>(R.id.btn_trynow)
        btnTryNow.setOnClickListener { tryAgain() }
    }

    override fun onResume() {
        super.onResume()
        externalStorageStatusChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun externalStorageStatusChanged() {
        if (SystemUtils.isExternalStorageAvailable()) {
            var tv = findViewById<TextView>(R.id.storage_status_text)
            tv.text = "External SD card is available for use"
            tv = findViewById(R.id.storage_desc_text2)
            tv.text = "You should be able to use the application at this time."
            tv.setTextColor(Color.GREEN)
            val btnTryNow = findViewById<Button>(R.id.btn_trynow)
            btnTryNow.visibility = View.VISIBLE
        } else {
            var tv = findViewById<TextView>(R.id.storage_status_text)
            tv.text = "External SD card is not available for use"
            tv = findViewById(R.id.storage_desc_text2)
            tv.text = "Please disconnect or unmount the device from the computer."
            tv.setTextColor(Color.RED)
            val btnTryNow = findViewById<Button>(R.id.btn_trynow)
            btnTryNow.visibility = View.GONE
        }
    }

    private fun tryAgain() {
        val intent = Intent(this, FlightIntel::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return false
    }
}
