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
package com.nadmm.airports.wx

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.nadmm.airports.R
import com.nadmm.airports.databinding.ListItemText1Binding
import com.nadmm.airports.databinding.WxMapDetailViewBinding
import com.nadmm.airports.utils.TextFileViewActivity

abstract class WxTextFragmentBase : WxFragmentBase {
    private val mAction: String
    private val mWxAreas: Map<String, String>
    private val mWxTypes: Map<String, String>
    private var mPendingRow: View? = null
    private var _binding: WxMapDetailViewBinding? = null
    private val binding get() = _binding!!

    constructor(action: String, areas: Map<String, String>, types: Map<String, String> = mapOf()) {
        mAction = action
        mWxAreas = areas
        mWxTypes = types
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBroadcastFilter(mAction)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = WxMapDetailViewBinding.inflate(inflater)
        return createContentView(binding.root)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            wxMapLabel.text = "Select Area"
            wxMapLabel.visibility = View.VISIBLE

            val listener = View.OnClickListener { v: View? ->
                if (mPendingRow == null && v != null) {
                    mPendingRow = v
                    val code = v.tag as String
                    requestWxText(code)
                }
            }

            val areas = mWxAreas
                .toList()
                .sortedBy { (_, value) -> value }
                .toMap()
            for (area in areas) {
                val row = addWxRow(wxMapLayout, area.value, area.key)
                row.setOnClickListener(listener)
            }

            if (helpText.isNotEmpty()) {
                wxHelpText.text = helpText
                wxHelpText.visibility = View.VISIBLE
            }

            if (mWxTypes.isNotEmpty()) {
                wxMapTypeLabel.visibility = View.VISIBLE
                wxMapTypeLayout.visibility = View.VISIBLE
                val types = mWxTypes.toSortedMap().values.toList()
                val adapter = ArrayAdapter(
                    requireActivity(),
                    R.layout.list_item, types)
                val textView = getAutoCompleteTextView(wxMapType)
                textView?.setAdapter(adapter)
                textView?.setText(types.first(), false)
            }
        }

        setFragmentContentShown(true)
    }

    override fun onResume() {
        super.onResume()

        mPendingRow = null
    }

    override fun handleBroadcast(intent: Intent) {
        val path = intent.getStringExtra(NoaaService.RESULT)
        if (!path.isNullOrEmpty()) {
            val item = ListItemText1Binding.bind(mPendingRow!!)
            val label = item.text.text.toString()
            val viewer = Intent(activity, TextFileViewActivity::class.java)
            viewer.putExtra(TextFileViewActivity.FILE_PATH, path)
            viewer.putExtra(TextFileViewActivity.TITLE_TEXT, title)
            viewer.putExtra(TextFileViewActivity.LABEL_TEXT, label)
            activity?.startActivity(viewer)
        }
        setSpinnerVisible(false)
        mPendingRow = null
    }

    private fun requestWxText(code: String?) {
        setSpinnerVisible(true)
        val service = this.serviceIntent
        service.setAction(mAction)
        service.putExtra(NoaaService.TEXT_CODE, code)
        if (mWxTypes.isNotEmpty()) {
            val text = getSelectedItemText(binding.wxMapType)
            val type = mWxTypes.entries.first { it.value == text }.key
            service.putExtra(NoaaService.TEXT_TYPE, type)
        }
        activity?.startService(service)
    }

    private fun setSpinnerVisible(visible: Boolean) {
        if (mPendingRow != null) {
            val view = mPendingRow!!.findViewById<View>(R.id.progress)
            view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    protected abstract val title: String
    protected abstract val serviceIntent: Intent
    protected abstract val helpText: String
}
