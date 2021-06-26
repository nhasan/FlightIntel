/*
 * FlightIntel for Pilots
 *
 * Copyright 2017-2021 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.e6b;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

import java.util.Locale;

abstract class E6bFragmentBase extends FragmentBase {

    protected final TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged( CharSequence s, int start, int before, int count ) {
        }

        @Override
        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
        }

        @Override
        public void afterTextChanged( Editable s ) {
            processInput();
        }
    };

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        TextView label = findViewById( R.id.e6b_label );
        Bundle args = getArguments();
        if ( label != null && args != null ) {
            String title = args.getString( ListMenuFragment.SUBTITLE_TEXT );
            label.setText( title );
        }

        String text = getMessage();
        if ( text != null ) {
            TextView msg = findViewById( R.id.e6b_msg );
            msg.setText( text );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hideKeyboard();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard();
    }

    protected void addEditField( TextInputLayout textInputLayout, int textHintId )
    {
        addEditField( textInputLayout );
        textInputLayout.setHint( textHintId );
    }

    protected void addEditField( TextInputLayout textInputLayout )
    {
        EditText editText = textInputLayout.getEditText();
        if ( editText != null ) {
            editText.addTextChangedListener( mTextWatcher );
        }
    }

    protected void addSpinnerField( TextInputLayout textInputLayout )
    {
        AutoCompleteTextView textView = getAutoCompleteTextView( textInputLayout );
        if ( textView != null ) {
            textView.setOnItemClickListener( ( parent, view, position, id ) -> processInput() );
        }
    }

    protected void setSpinnerAdapter( TextInputLayout textInputLayout, ArrayAdapter<?> adapter,
                                      int selectedIndex )
    {
        AutoCompleteTextView textView = getAutoCompleteTextView( textInputLayout );
        if ( textView != null ) {
            if ( adapter != null ) {
                textView.setAdapter( adapter );
            }
            textView.setText( adapter.getItem( selectedIndex ).toString(), false );
        }
    }

    protected Object getSelectedItem( TextInputLayout textInputLayout )
    {
        AutoCompleteTextView textView = getAutoCompleteTextView( textInputLayout );
        if ( textView == null ) return null;

        ArrayAdapter<?> adapter = (ArrayAdapter<?>) textView.getAdapter();
        if ( adapter == null ) return null;

        String text = textView.getText().toString();
        if ( text != null && !text.isEmpty() ) {
            int count = adapter.getCount();
            for ( int i = 0; i < count; ++i ) {
                Object o = adapter.getItem( i );
                if ( o.toString().equals( text ) ) return o;
            }
        }
        return null;
    }

    protected AutoCompleteTextView getAutoCompleteTextView( TextInputLayout textInputLayout) {
        return (AutoCompleteTextView) textInputLayout.getEditText();
    }

    protected void addReadOnlyField( TextInputLayout textInputLayout, int textHintId )
    {
        addReadOnlyField( textInputLayout );
        textInputLayout.setHint( textHintId );
    }

    protected void addReadOnlyField( TextInputLayout textInputLayout )
    {
        EditText editText = textInputLayout.getEditText();
        if ( editText != null ) {
            editText.setInputType( InputType.TYPE_NULL );
            editText.setKeyListener( null );
        }
    }

    protected String getValue( TextInputLayout textInputLayout )
    {
        EditText edit = textInputLayout.getEditText();
        return edit != null? edit.getText().toString() : "";
    }

    protected double parseDouble( TextInputLayout textInputLayout )
    {
        return Double.parseDouble( getValue( textInputLayout ) );
    }

    protected long parseLong( TextInputLayout textInputLayout )
    {
        return Long.parseLong( getValue( textInputLayout ) );
    }

    protected double parseDirection( TextInputLayout textInputLayout )
    {
        double direction = parseDouble( textInputLayout );
        if ( direction == 0 || direction > 360 ) {
            textInputLayout.setError( "Valid values: 1 to 360" );
            throw( new NumberFormatException() );
        } else {
            direction = Math.toRadians( direction );
            textInputLayout.setError( "" );
        }
        return direction;
    }

    protected double parseDeclination( TextInputLayout textInputLayout )
    {
        double declination = parseDouble( textInputLayout );
        if ( declination < -45 || declination > 45 ) {
            textInputLayout.setError( "Valid values: -45 to +45" );
            throw( new NumberFormatException() );
        } else {
            textInputLayout.setError( "" );
        }
        return declination;
    }

    protected double parseAltitude( TextInputLayout textInputLayout )
    {
        double altitude = parseDouble( textInputLayout );
        if ( altitude < 0 || altitude > 65620 ) {
            textInputLayout.setError( "Valid values: 0 to 65,620" );
            throw( new NumberFormatException() );
        } else {
            textInputLayout.setError( "" );
        }
        return altitude;
    }

    protected double parseRunway( TextInputLayout textInputLayout )
    {
        double rwy = parseDouble( textInputLayout );
        if ( rwy < 1 || rwy > 36 ) {
            textInputLayout.setError( "Valid values: 1 to 36" );
            throw( new NumberFormatException() );
        } else {
            textInputLayout.setError( "" );
        }
        return rwy;
    }

    protected void showValue( TextInputLayout textInputLayout, String value )
    {
        EditText editText = textInputLayout.getEditText();
        if ( editText != null ) {
            editText.setText( value );
        }
    }

    protected void showValue( TextInputLayout textInputLayout, double value )
    {
        showValue( textInputLayout, String.valueOf( Math.round( value ) ) );
    }

    protected void showDecimalValue( TextInputLayout textInputLayout, double value )
    {
        showDecimalValue( textInputLayout, value, 1 );
    }

    protected void showDecimalValue( TextInputLayout textInputLayout, double value, int decimals )
    {
        String fmt = String.format( Locale.US, "%%.%df", decimals );
        showValue( textInputLayout, String.format( Locale.US, fmt, value ) );
    }

    protected void showDirection( TextInputLayout textInputLayout, double dirRadians )
    {
        double dirDegrees = Math.toDegrees( normalizeDir( dirRadians ) );
        showValue( textInputLayout, Math.round( dirDegrees ) );
    }

    protected void clearEditText( TextInputLayout textInputLayout )
    {
        showValue( textInputLayout, "" );
    }

    protected double normalizeDir( double radians ) {
        if ( radians <= 0 ) {
            return radians+2*Math.PI;
        } else if ( radians > 2*Math.PI ) {
            return radians-2*Math.PI;
        }
        return radians;
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService( Activity.INPUT_METHOD_SERVICE );
        imm.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
    }

    protected abstract String getMessage();

    protected abstract void processInput();

}
