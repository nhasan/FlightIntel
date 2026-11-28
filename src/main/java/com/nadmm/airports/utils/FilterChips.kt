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
package com.nadmm.airports.utils

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nadmm.airports.R

/**
 * A row of filter chips. Tapping a chip offers its values in a menu, each
 * with how many items it would show when the caller gives counts. A chosen
 * value fills the chip, which then clears it; the choices are remembered in
 * [prefs]. The chips share the width of [row], so all of them, and each
 * one's icon, stay on screen: they keep their own width while they all fit,
 * and otherwise the widest are shortened, their labels ending in an ellipsis.
 *
 * Style the chips in the layout as checkable filter chips with a close icon;
 * the checked state shows that a value is chosen.
 */
class FilterChips(
    private val row: View,
    private val group: ChipGroup,
    private val prefs: SharedPreferences,
    private val onChange: () -> Unit,
) {
    /** A value a filter offers, with how many items it would show, when known. */
    class Choice(val key: String, val label: String, val count: Int? = null)

    /**
     * A filter on [chip], named [title] while it has no value. With a
     * [default], it always has one, offered with the others; otherwise its
     * menu starts with All, counting [allCount]. [label] gives a value's text
     * on the chip, and [choices] the values to offer, worked out when the
     * menu opens.
     */
    inner class Filter(
        val chip: Chip,
        val title: String,
        private val prefKey: String,
        private val default: String? = null,
        private val label: (String) -> String = { it },
        private val allCount: () -> Int? = { null },
        private val choices: () -> List<Choice>,
    ) {
        var value: String? = prefs.getString(prefKey, null) ?: default
            private set

        /** True when a value other than the default is chosen. */
        val isChosen: Boolean
            get() = value != null && value != default

        internal fun choose(key: String?) {
            value = key ?: default
            prefs.edit {
                if (isChosen) putString(prefKey, value) else remove(prefKey)
            }
            onChange()
        }

        internal fun showChoices() {
            // Tapping a checkable chip toggles it; it should show only the choice
            chip.isChecked = isChosen
            val offered = choices().toMutableList()
            // A remembered choice stays on offer even when nothing has it now
            value?.takeIf { v -> offered.none { it.key == v } }
                ?.let { offered.add(Choice(it, label(it), 0)) }
            PopupMenu(chip.context, chip).apply {
                if (default == null) {
                    menu.add(0, 0, 0, withCount("All", allCount()))
                }
                offered.forEachIndexed { i, choice ->
                    menu.add(0, i + 1, i + 1, withCount(choice.label, choice.count))
                }
                setOnMenuItemClickListener { item ->
                    choose(if (item.itemId == 0) null else offered[item.itemId - 1].key)
                    true
                }
                show()
            }
        }

        internal fun update(arrow: Drawable?) {
            chip.text = value?.let(label) ?: title
            chip.isChecked = isChosen
            chip.closeIcon = if (isChosen) clearIcon else arrow
            chip.closeIconContentDescription =
                if (isChosen) "Clear ${title.lowercase()} filter" else "Choose ${title.lowercase()}"
        }

        private fun withCount(text: String, count: Int?) =
            if (count != null) "$text ($count)" else text
    }

    private val filters = mutableListOf<Filter>()
    // The chips' own close icon, shown once a value is chosen
    private var clearIcon: Drawable? = null

    init {
        // Again whenever the row's width changes, such as on rotation
        row.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) row.post { fit() }
        }
    }

    /** Adds [filter], whose chip then offers its values. */
    fun add(filter: Filter): Filter {
        if (clearIcon == null) clearIcon = filter.chip.closeIcon
        filters.add(filter)
        // A tap offers the values to pick from
        filter.chip.setOnClickListener { filter.showChoices() }
        filter.chip.setOnCloseIconClickListener {
            if (filter.isChosen) filter.choose(null) else filter.showChoices()
        }
        return filter
    }

    /** Shows each filter's value on its chip, after a change or new items. */
    fun update() {
        val arrow = ContextCompat.getDrawable(row.context, R.drawable.ic_outline_arrow_drop_down_24)
        filters.forEach { it.update(arrow) }
        // Once the new labels are measured
        row.post { fit() }
    }

    /**
     * Shares the row's width among the chips: each keeps its own width while
     * all fit; otherwise the widest are shortened to equal shares of the width
     * the narrower ones leave.
     */
    private fun fit() {
        val spacing = group.chipSpacingHorizontal * (filters.size - 1)
        var remaining = row.width - row.paddingStart - row.paddingEnd - spacing
        if (remaining <= 0) return
        val natural = filters.associate { filter ->
            filter.chip.maxWidth = Int.MAX_VALUE
            filter.chip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            filter.chip to filter.chip.measuredWidth
        }
        var left = natural.size
        for ((chip, width) in natural.entries.sortedBy { it.value }) {
            val fitted = minOf(width, remaining / left)
            chip.maxWidth = fitted
            remaining -= fitted
            left--
        }
    }
}
