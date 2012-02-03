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
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.nadmm.airports.utils.UiUtils;

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

    protected void makeClickToCall( TextView tv ) {
        UiUtils.makeClickToCall( mActivity, tv );
    }

    protected int getSelectorResourceForRow( int curRow, int totRows ) {
        return mActivity.getSelectorResourceForRow( curRow, totRows );
    }

    protected void addRow( TableLayout table, String label, String text ) {
        mActivity.addRow( table, label, text );
    }

    protected void addRow( LinearLayout layout, String text ) {
        mActivity.addRow( layout, text );
    }

    protected void addRow( LinearLayout layout, String label, String text ) {
        mActivity.addRow( layout, label, text );
    }

    protected void addRow( TableLayout table, String label, Pair<String, String> values ) {
        mActivity.addRow( table, label, values );
    }

    protected void addClickableRow( TableLayout table, String label,
            final Intent intent, int resid ) {
        mActivity.addClickableRow( table, label, null, intent, resid );
    }

    protected void addClickableRow( TableLayout table, String label, String value,
            final Intent intent, int resid ) {
        mActivity.addClickableRow( table, label, value, intent, resid );
    }

    protected void addPhoneRow( TableLayout table, String label, final String phone ) {
        mActivity.addPhoneRow( table, label, phone );
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
