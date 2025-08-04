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
package com.nadmm.airports.afd

import com.nadmm.airports.FragmentActivityBase
import android.os.Bundle
import android.content.Intent
import android.app.SearchManager
import com.nadmm.airports.data.DatabaseManager.Airports
import android.database.Cursor
import com.nadmm.airports.providers.AirportsProvider
import com.nadmm.airports.ListFragmentBase
import android.content.Context
import android.widget.ListView
import android.view.View
import androidx.cursoradapter.widget.CursorAdapter
import com.nadmm.airports.utils.makeAirportBundle

class SearchActivity : FragmentActivityBase() {
    private var mFragment: SearchFragment? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFragment = addFragment(SearchFragment::class.java, null) as SearchFragment
        handleIntent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        if (Intent.ACTION_SEARCH == intent.action) {
            // Perform the search using user provided query string
            intent.getStringExtra(SearchManager.QUERY)?.let { query ->
                showResults(query)
            }
        } else if (Intent.ACTION_VIEW == intent.action) {
            // User clicked on a suggestion
            intent.extras?.getString(SearchManager.EXTRA_DATA_KEY).let { siteNumber ->
                val apt = Intent(this, AirportActivity::class.java)
                    .putExtra(Airports.SITE_NUMBER, siteNumber)
                startActivity(apt)
            }
            finish()
        }
    }

    private fun showResults(query: String?) {
        contentResolver.query(AirportsProvider.CONTENT_URI, null, null, arrayOf(query), null )
        ?.apply {
            if (!moveToFirst()) {
                close()
            } else {
                mFragment!!.setSearchCursor(this)
            }
        }
    }

    class SearchFragment : ListFragmentBase() {
        private var mCursor: Cursor? = null
        override fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter {
            return AirportsCursorAdapter(context, c)
        }

        override fun onListItemClick(l: ListView, v: View, position: Int) {
            val apt = Intent(activity, AirportActivity::class.java).apply {
                putExtras(mCursor!!.makeAirportBundle())
            }
            startActivity(apt)
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            mCursor?.let {
                setCursor(it)
            }
        }

        fun setSearchCursor(c: Cursor) {
            mCursor = c
            if (activity != null && view != null) {
                mCursor?.let {
                    setCursor(it)
                }
            }
        }
    }
}