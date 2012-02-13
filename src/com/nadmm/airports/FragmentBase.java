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

package com.nadmm.airports;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.View;
import android.widget.LinearLayout;

public class FragmentBase extends Fragment {

    private ActivityBase mActivity;

    @Override
    public void onAttach( SupportActivity activity ) {
        super.onAttach( activity );
        mActivity = (ActivityBase) activity;
    }

    public DatabaseManager getDbManager() {
        return mActivity.getDbManager();
    }

    public ActivityBase getActivityBase() {
        return mActivity;
    }

    protected int getSelectorResourceForRow( int curRow, int totRows ) {
        return mActivity.getSelectorResourceForRow( curRow, totRows );
    }

    protected View addRow( LinearLayout layout, String value ) {
        return mActivity.addRow( layout, value );
    }

    protected View addRow( LinearLayout layout, String label, String value ) {
        return mActivity.addRow( layout, label, value );
    }

    protected View addRow( LinearLayout layout, String label, String value1, String value2 ) {
        return mActivity.addRow( layout, label, value1, value2 );
    }

    protected View addRow( LinearLayout layout, String label1, String value1,
            String label2, String value2 ) {
        return mActivity.addRow( layout, label1, value1, label2, value2 );
    }

    protected View addClickableRow( LinearLayout layout, String label,
            final Intent intent, int resid ) {
        return mActivity.addClickableRow( layout, label, null, intent, resid );
    }

    protected View addClickableRow( LinearLayout layout, String label, String value,
            final Intent intent, int resid ) {
        return mActivity.addClickableRow( layout, label, value, intent, resid );
    }

    protected void addBulletedRow( LinearLayout layout, String text ) {
        mActivity.addBulletedRow( layout, text );
    }

    protected void addSeparator( LinearLayout layout ) {
        mActivity.addSeparator( layout );
    }

    protected View findViewById( int id ) {
        return mActivity.findViewById( id );
    }

    protected View inflate( int id ) {
        return mActivity.inflate( id );
    }

}
