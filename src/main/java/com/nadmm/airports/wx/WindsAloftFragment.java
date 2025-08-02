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

package com.nadmm.airports.wx;

import android.content.Intent;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class WindsAloftFragment extends WxTextFragmentBase {

    private static final Map<String, String> mTypes = Map.of(
            "06", "6 Hour",
            "12", "12 Hour",
            "24", "24 Hour"
    );

    private static final Map<String, String> mAreas = Map.ofEntries(
            Map.entry("us", "Continental US"),
            Map.entry("alaska", "Alaska"),
            Map.entry("bos", "Boston"),
            Map.entry("canada", "Canada"),
            Map.entry("chi", "Chicago"),
            Map.entry("dfw", "Dallas/Fort Worth"),
            Map.entry("hawaii", "Hawaii"),
            Map.entry("mia", "Miami"),
            Map.entry("pacific", "Pacific"),
            Map.entry("sfo", "San Francisco"),
            Map.entry("slc", "Salt Lake City")
    );

    public WindsAloftFragment() {
        super( NoaaService.ACTION_GET_FB, mAreas, mTypes );
    }

    @NonNull
    @Override
    protected String getTitle() {
        return "Winds Aloft";
    }

    @NonNull
    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), WindsAloftService.class );
    }

    @Override
    protected String getProduct() {
        return "windsaloft";
    }

    @Override
    protected @NotNull String getHelpText() {
        return "";
    }
}
