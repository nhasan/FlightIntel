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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.nadmm.airports.ImageViewActivity
import com.nadmm.airports.R
import com.nadmm.airports.databinding.WxMapDetailViewBinding

abstract class WxGraphicFragmentBase : WxFragmentBase {
    private val mAction: String
    private val mWxGraphics: Map<String, String>
    private val mWxTypes: Map<String, String>
    private var mTypeName: String = ""
    private var mLabel: String = ""
    private var mTitle: String = ""
    private var mHelpText: String = ""
    private var mPendingRow: View? = null

    private var _binding: WxMapDetailViewBinding? = null
    private val binding get() = _binding!!

    constructor(action: String, graphics: Map<String, String>) {
        mAction = action
        mWxGraphics = graphics
        mWxTypes = mapOf()
    }

    constructor(action: String, graphics: Map<String, String>, types: Map<String, String>) {
        mAction = action
        mWxGraphics = graphics
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
        _binding = WxMapDetailViewBinding.inflate(inflater, container, false)
        val view = binding.root
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            if (mLabel.isNotEmpty()) {
                wxMapLabel.text = mLabel
                wxMapLabel.visibility = View.VISIBLE
            }

            if (mHelpText.isNotEmpty()) {
                wxHelpText.text = mHelpText
                wxHelpText.visibility = View.VISIBLE
            }

            val listener = View.OnClickListener { v: View? ->
                if (mPendingRow == null) {
                    mPendingRow = v
                    val code = getMapCode(v!!)
                    requestWxMap(code)
                }
            }

            val graphics = mWxGraphics.toSortedMap()
            for (graphic in graphics) {
                val row = addWxRow(wxMapLayout, graphic.value, graphic.key)
                row.setOnClickListener(listener)
            }

            if (mWxTypes.isNotEmpty()) {
                if (mTypeName.isNotEmpty()) {
                    wxMapTypeLabel.visibility = View.VISIBLE
                    wxMapTypeLabel.text = mTypeName
                }
                wxMapTypeLayout.visibility = View.VISIBLE
                val types = mWxTypes.toSortedMap().values.toList()
                val adapter = ArrayAdapter(
                    requireActivity(),
                    R.layout.list_item, types
                )
                val textView = getAutoCompleteTextView(wxMapType)
                textView!!.setAdapter(adapter)
                textView.setText(types.first(), false)
            }
        }

        setFragmentContentShown(true)
    }

    override fun onResume() {
        super.onResume()

        mPendingRow = null
    }

    private fun requestWxMap(code: String) {
        setSpinnerVisible(true)

        val service = serviceIntent
        service.setAction(mAction)
        service.putExtra(NoaaService.TYPE, NoaaService.TYPE_GRAPHIC)
        service.putExtra(NoaaService.IMAGE_CODE, code)
        if (mWxTypes.isNotEmpty()) {
            val text = getSelectedItemText(binding.wxMapType)
            val type = mWxTypes.entries.first { it.value == text }.key
            service.putExtra(NoaaService.IMAGE_TYPE, type)
        }
        setServiceParams(service)
        activity?.startService(service)
    }

    override fun handleBroadcast(intent: Intent) {
        val type = intent.getStringExtra(NoaaService.TYPE)
        if (type == NoaaService.TYPE_GRAPHIC) {
            val path = intent.getStringExtra(NoaaService.RESULT)
            if (!path.isNullOrEmpty()) {
                val view = Intent(activity, WxImageViewActivity::class.java)
                view.putExtra(ImageViewActivity.IMAGE_PATH, path)
                val title = mTitle.ifEmpty { activity?.title }
                view.putExtra(ImageViewActivity.IMAGE_TITLE, title)
                val code = intent.getStringExtra(NoaaService.IMAGE_CODE)
                mWxGraphics[code!!]?.let { name ->
                    view.putExtra(ImageViewActivity.IMAGE_SUBTITLE, name)
                }
                startActivity(view)
            }
            setSpinnerVisible(false)
            mPendingRow = null
        }
    }

    protected fun setServiceParams(intent: Intent) {
    }

    private fun getMapCode(v: View): String {
        return v.tag as String? ?: ""
    }

    protected fun setLabel(label: String) {
        mLabel = label
    }

    protected fun setGraphicTypeName(typeName: String) {
        mTypeName = typeName
    }

    protected fun setTitle(title: String) {
        mTitle = title
    }

    protected fun setHelpText(text: String) {
        mHelpText = text
    }

    private fun setSpinnerVisible(visible: Boolean) {
        if (mPendingRow != null) {
            val view = mPendingRow!!.findViewById<View>(R.id.progress)
            view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    protected abstract val serviceIntent: Intent
}
