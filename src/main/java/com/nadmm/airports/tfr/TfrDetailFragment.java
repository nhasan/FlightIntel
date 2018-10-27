/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.TimeUtils;

import java.util.Locale;

public class TfrDetailFragment extends FragmentBase {

    private Tfr mTfr;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.tfr_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated( view, savedInstanceState );

        Button btnGraphic = view.findViewById( R.id.btnViewGraphic );
        btnGraphic.setOnClickListener( v -> {
            Intent intent = new Intent( getActivity(), TfrImageActivity.class );
            intent.putExtra( TfrListActivity.EXTRA_TFR, mTfr );
            startActivity( intent );
        } );

        Bundle args = getArguments();
        mTfr = (Tfr) args.getSerializable( TfrListActivity.EXTRA_TFR );

        LinearLayout layout = (LinearLayout) view.findViewById( R.id.tfr_header_layout );
        addRow( layout, "Name", mTfr.name );
        addRow( layout, "NOTAM", mTfr.notamId );
        addRow( layout, "Type", mTfr.type );
        addRow( layout, "Status", mTfr.isExpired()? "Expired"
                : mTfr.isActive()? "Active" : "Inactive" );
        addRow( layout, "Time", mTfr.formatTimeRange( getActivityBase() ) );
        addRow( layout, "Altitudes", mTfr.formatAltitudeRange() );

        layout = view.findViewById( R.id.tfr_time_layout );
        if ( mTfr.createTime < Long.MAX_VALUE ) {
            addRow( layout, "Created",
                    TimeUtils.formatDateTimeYear( getActivityBase(), mTfr.createTime ) );
        }
        if ( mTfr.modifyTime < Long.MAX_VALUE && mTfr.modifyTime > mTfr.createTime ) {
            addRow( layout, "Modified",
                    TimeUtils.formatDateTimeYear( getActivityBase(), mTfr.modifyTime ) );
        }

        layout = view.findViewById( R.id.tfr_text_layout );
        addRow( layout, mTfr.text.replace( "\\n", "\n" ) );

        TextView tv = view.findViewById( R.id.tfr_warning_text );
        tv.setText( String.format( Locale.US,
                "Depicted TFR data may not be a complete listing. Pilots should not use "
                + "the information for flight planning purposes. For the latest information, "
                + "call your local Flight Service Station at 1-800-WX-BRIEF." ) );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( mTfr.name );
        setActionBarSubtitle( "TFR Details" );

        setFragmentContentShown( true );
    }

}
