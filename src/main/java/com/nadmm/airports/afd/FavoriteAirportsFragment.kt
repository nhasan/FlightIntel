/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2019 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.cursoradapter.widget.CursorAdapter
import com.nadmm.airports.ListFragmentBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.utils.CursorAsyncTask

class FavoriteAirportsFragment : ListFragmentBase() {

    override fun onResume() {
        super.onResume()

        setBackgroundTask(FavoriteAirportsTask(this)).execute()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setEmptyText("No favorite airports selected.")
    }

    override fun newListAdapter(context: Context, c: Cursor): CursorAdapter? {
        return AirportsCursorAdapter(context, c)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        val c = l.getItemAtPosition(position) as Cursor
        val siteNumber = c.getString(c.getColumnIndex(Airports.SITE_NUMBER))
        val intent = Intent(activity, AirportActivity::class.java)
        intent.putExtra(Airports.SITE_NUMBER, siteNumber)
        startActivity(intent)
    }

    private fun doQuery(): Array<Cursor?> {
        val siteNumbers = dbManager.aptFavorites.joinToString { "'${it}'"}

        val db = getDatabase(DatabaseManager.DB_FADDS)
        val selection = "a.${Airports.SITE_NUMBER} in (${siteNumbers})"
        val c = AirportsCursorHelper.query(db, selection, null, null,
                null, Airports.FACILITY_NAME, null)

        return arrayOf(c)
    }

    private class FavoriteAirportsTask(fragment: FavoriteAirportsFragment)
        : CursorAsyncTask<FavoriteAirportsFragment>(fragment) {

        override fun onExecute(fragment: FavoriteAirportsFragment, vararg params: String)
                : Array<Cursor?> {
            return fragment.doQuery()
        }

        override fun onResult(fragment: FavoriteAirportsFragment, result: Array<Cursor?>)
                : Boolean {
            fragment.setCursor(result[0])
            return false
        }
    }

}
