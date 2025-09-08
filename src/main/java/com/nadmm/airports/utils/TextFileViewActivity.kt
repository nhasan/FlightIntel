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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.nadmm.airports.FragmentActivityBase
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import java.io.IOException

class TextFileViewActivity : FragmentActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras
        if (args != null) {
            val title = args.getString(TITLE_TEXT)
            setActionBarTitle(title!!)
        }

        addFragment(TextViewFragment::class.java, args)
    }

    class TextViewFragment : FragmentBase() {
        @SuppressLint("SetTextI18n")
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            var view: View? = null
            val args = arguments
            if (args != null) {
                val label = args.getString(LABEL_TEXT)
                val path = args.getString(FILE_PATH)

                view = inflate(R.layout.text_view)
                var tv = view.findViewById<TextView>(R.id.text_label)
                tv.text = label
                tv = view.findViewById<TextView>(R.id.text_content)
                try {
                    val text = FileUtils.readFile(path)
                    tv.text = text
                } catch (e: IOException) {
                    tv.text = "Unable to read FA file: " + e.message
                }
                UiUtils.setupWindowInsetsListener(view)
            }

            return view
        }
    }

    companion object {
        const val FILE_PATH: String = "FILE_PATH"
        const val LABEL_TEXT: String = "LABEL_TEXT"
        const val TITLE_TEXT: String = "TITLE_TEXT"
    }
}
