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

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;

import com.nadmm.airports.ListFragmentBase;

public abstract class AirportListFragment extends ListFragmentBase {

    public interface Listener {
        public void onFragmentViewCreated( AirportListFragment fragment );
        public void onFragmentAttached( AirportListFragment fragment );
        public void onFragmentDetached( AirportListFragment fragment );
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated( view, savedInstanceState );
        if ( getActivity() instanceof Listener ) {
            ((Listener) getActivity()).onFragmentViewCreated( this );
        }
    }

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach( activity );
        if ( getActivity() instanceof Listener ) {
            ((Listener) getActivity()).onFragmentAttached( this );
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if ( getActivity() instanceof Listener ) {
            ((Listener) getActivity()).onFragmentDetached( this );
        }
    }

}
