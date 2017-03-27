/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2017 Nadeem Hasan <nhasan@nadmm.com>
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

public class WindsAloftFragment extends WxTextFragmentBase {

    private static final String[] mAreaCodes = {
        "/alaska/l_06hr.alaska",
        "/alaska/l_12hr.alaska",
        "/alaska/l_24hr.alaska",
        "/bos/l_06hr.bos_fa",
        "/bos/l_12hr.bos_fa",
        "/bos/l_24hr.bos_fa",
        "/chi/l_06hr.chi_fa",
        "/chi/l_12hr.chi_fa",
        "/chi/l_24hr.chi_fa",
        "/dfw/l_06hr.dfw_fa",
        "/dfw/l_12hr.dfw_fa",
        "/dfw/l_24hr.dfw_fa",
        "/hawaii/l_06hr.hawaii",
        "/hawaii/l_12hr.hawaii",
        "/hawaii/l_24hr.hawaii",
        "/mia/l_06hr.mia_fa",
        "/mia/l_12hr.mia_fa",
        "/mia/l_24hr.mia_fa",
        "/other_pac/l_06hr.other_pacific",
        "/other_pac/l_12hr.other_pacific",
        "/other_pac/l_24hr.other_pacific",
        "/sfo/l_06hr.sfo_fa",
        "/sfo/l_12hr.sfo_fa",
        "/sfo/l_24hr.sfo_fa",
        "/slc/l_06hr.slc_fa",
        "/slc/l_12hr.slc_fa",
        "/slc/l_24hr.slc_fa",
    };

    private static final String[] mAreaNames = {
        "Alaska 6 hr Forecast",
        "Alaska 12 hr Forecast",
        "Alaska 24 hr Forecast",
        "Boston 6 hr Forecast",
        "Boston 12 hr Forecast",
        "Boston 24 hr Forecast",
        "Chicago 6 hr Forecast",
        "Chicago 12 hr Forecast",
        "Chicago 24 hr Forecast",
        "Dallas/Fort Worth 6 hr Forecast",
        "Dallas/Fort Worth 12 hr Forecast",
        "Dallas/Fort Worth 24 hr Forecast",
        "Hawaii 6 hr Forecast",
        "Hawaii 12 hr Forecast",
        "Hawaii 24 hr Forecast",
        "Miami 6 hr Forecast",
        "Miami 12 hr Forecast",
        "Miami 24 hr Forecast",
        "Pacific 6 hr Forecast",
        "Pacific 12 hr Forecast",
        "Pacific 24 hr Forecast",
        "San Francisco 6 hr Forecast",
        "San Francisco 12 hr Forecast",
        "San Francisco 24 hr Forecast",
        "Salt Lake City 6 hr Forecast",
        "Salt Lake City 12 hr Forecast",
        "Salt Lake City 24 hr Forecast",
    };

    public WindsAloftFragment() {
        super( NoaaService.ACTION_GET_FB, mAreaCodes, mAreaNames );
    }

    @Override
    protected String getTitle() {
        return "Winds Aloft";
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), WindsAloftService.class );
    }

    @Override
    protected String getProduct() {
        return "windsaloft";
    }
}
