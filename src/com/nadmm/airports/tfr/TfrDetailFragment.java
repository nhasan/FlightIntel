/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.tfr;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.TimeUtils;

public class TfrDetailFragment extends FragmentBase {

    private Tfr mTfr;

    @Override
    public void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        Context context = getActivity();
        View view = inflater.inflate( R.layout.tfr_detail_view, container, false );

        Bundle args = getArguments();
        mTfr = (Tfr) args.getSerializable( TfrActivity.EXTRA_TFR );

        LinearLayout layout = (LinearLayout) view.findViewById( R.id.tfr_header_layout );
        addRow( layout, "Name", mTfr.name );
        addRow( layout, "NOTAM", mTfr.notamId );
        addRow( layout, "Type", mTfr.type );
        addRow( layout, "Status", mTfr.isExpired()? "Expired"
                : mTfr.isActive()? "Active" : "Inactive" );
        addRow( layout, "Time", mTfr.formatTimeRange( context ) );
        addRow( layout, "Altitudes", mTfr.formatAltitudeRange() );

        layout = (LinearLayout) view.findViewById( R.id.tfr_time_layout );
        if ( mTfr.createTime < Long.MAX_VALUE ) {
            addRow( layout, "Created", TimeUtils.formatDateTimeYear( context, mTfr.createTime ) );
        }
        if ( mTfr.modifyTime < Long.MAX_VALUE ) {
            addRow( layout, "Modified", TimeUtils.formatDateTimeYear( context, mTfr.modifyTime ) );
        }

        TextView tv = (TextView) view.findViewById( R.id.tfr_text_view );
        mTfr.comment.replace( "\\n", new String( new char[] { 10 } ) );
        tv.setText( mTfr.comment.replace( "\\n", "\n" ) );

        tv = (TextView) view.findViewById( R.id.tfr_warning_text );
        tv.setText( "Depicted TFR data may not be a complete listing. Pilots should not use "
        		+ "the information for flight planning purposes. For the latest information, "
        		+ "call your local Flight Service Station at 1-800-WX-BRIEF." );

        setContentShown( true );

        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        setActionBarTitle( "TFR Details" );
        setActionBarSubtitle( mTfr.name );

        super.onActivityCreated( savedInstanceState );
    }

}
