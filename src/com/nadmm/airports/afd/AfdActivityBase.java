/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.afd;

import android.os.Bundle;
import android.view.View;
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;

public class AfdActivityBase extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        View v = getContentView();
        if ( v != null ) {
            setContentView( v );
        }
    }

    protected View getContentView() {
        return createContentView( R.layout.airport_activity_layout );
    }

}
