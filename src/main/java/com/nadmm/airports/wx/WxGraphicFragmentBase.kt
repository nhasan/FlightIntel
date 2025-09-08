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

abstract class WxGraphicFragmentBase(
    action: String,
    private val wxGraphics: Map<String, String>,
    private val wxTypes: Map<String, String> = mapOf()
) : WxFragmentBase(action) {
    private var selectedRow: View? = null
    protected var graphicTypeLabel = ""
    protected var graphicLabel = ""
    protected var title = ""
    protected var helpText = ""

    private var _binding: WxMapDetailViewBinding? = null
    private val binding get() = _binding!!

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
            if (graphicLabel.isNotEmpty()) {
                wxMapLabel.text = graphicLabel
                wxMapLabel.visibility = View.VISIBLE
            }

            if (helpText.isNotEmpty()) {
                wxHelpText.text = helpText
                wxHelpText.visibility = View.VISIBLE
            }

            val listener = View.OnClickListener { v: View? ->
                if (selectedRow == null && v != null) {
                    selectedRow = v
                    val tag = v.tag as String?
                    tag?.let { code ->
                        requestWxMap(code)
                    }
                }
            }

            for (graphic in wxGraphics) {
                val row = addWxRow(wxMapLayout, graphic.value, graphic.key)
                row.setOnClickListener(listener)
            }

            if (wxTypes.isNotEmpty()) {
                if (graphicTypeLabel.isNotEmpty()) {
                    wxMapTypeLabel.visibility = View.VISIBLE
                    wxMapTypeLabel.text = graphicTypeLabel
                }
                wxMapTypeLayout.visibility = View.VISIBLE
                getAutoCompleteTextView(wxMapType)?.let { textView ->
                    val types = wxTypes.values.toList()
                    val adapter = ArrayAdapter(
                        requireActivity(),
                        R.layout.list_item, types
                    )
                    textView.setAdapter(adapter)
                    textView.setText(types.first(), false)
                }
            }
        }

        setFragmentContentShown(true)
    }

    override fun onResume() {
        super.onResume()

        selectedRow = null
    }

    private fun requestWxMap(code: String) {
        setSpinnerVisible(true)

        val service = serviceIntent
        service.setAction(action)
        service.putExtra(NoaaService.TYPE, NoaaService.TYPE_GRAPHIC)
        service.putExtra(NoaaService.IMAGE_CODE, code)
        if (wxTypes.isNotEmpty()) {
            val text = getSelectedItemText(binding.wxMapType)
            val type = wxTypes.entries.first { it.value == text }.key
            service.putExtra(NoaaService.IMAGE_TYPE, type)
        }
        activity?.startService(service)
    }

    override fun processResult(result: Bundle) {
        val type = result.getString(NoaaService.TYPE)
        if (type == NoaaService.TYPE_GRAPHIC) {
            val path = result.getString(NoaaService.RESULT)
            if (!path.isNullOrEmpty()) {
                val view = Intent(activity, ImageViewActivity::class.java)
                view.putExtra(ImageViewActivity.IMAGE_PATH, path)
                view.putExtra(ImageViewActivity.IMAGE_TITLE, title.ifEmpty { activity?.title })
                result.getString(NoaaService.IMAGE_CODE)?.let { code ->
                    wxGraphics[code]?.let { name ->
                        view.putExtra(ImageViewActivity.IMAGE_SUBTITLE, name)
                    }
                }
                startActivity(view)
            }
            setSpinnerVisible(false)
            selectedRow = null
        }
    }

    private fun setSpinnerVisible(visible: Boolean) {
        selectedRow?.let { selectedRow ->
            val view = selectedRow.findViewById<View>(R.id.progress)
            view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    protected abstract val serviceIntent: Intent
}
