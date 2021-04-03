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

package com.nadmm.airports.e6b;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;

public class UnitConvertFrament extends E6bFragmentBase implements AdapterView.OnItemClickListener {

    private static final Unit[] mTemperatureUnits = new Unit[] {
        new Celsius(),
        new Fahrenheit(),
        new Rankine(),
        new Kelvin()
    };

    private static final Unit[] mLengthUnits = new Unit[] {
        new StatuteMile(),
        new NauticalMile(),
        new Yard(),
        new Foot(),
        new Inch(),
        new KiloMeter(),
        new Centimeter(),
        new Millimeter(),
        new Meter()
    };

    private static final Unit[] mSpeedUnits = new Unit[] {
        new Knot(),
        new MilePerHour(),
        new KilometerPerHour(),
        new FootPerSecond(),
        new MeterPerSecond()
    };

    private static final Unit[] mVolumeUnits = new Unit[] {
        new Gallon(),
        new Liter(),
        new Quart(),
        new FluidOunce(),
        new MilliLiter()
    };

    private static final Unit[] mPressureUnits = new Unit[] {
        new InchOfHg(),
        new HectoPascal(),
        new Millibar()
    };

    private static final Unit[] mMassUnits = new Unit[] {
        new Pound(),
        new Ounce(),
        new KiloGram(),
        new Gram()
    };

    private static final HashMap<String, Unit[]> mUnitTypeMap = new HashMap<>();
    static {
        mUnitTypeMap.put( "Temperature", mTemperatureUnits );
        mUnitTypeMap.put( "Length", mLengthUnits );
        mUnitTypeMap.put( "Speed", mSpeedUnits );
        mUnitTypeMap.put( "Volume", mVolumeUnits );
        mUnitTypeMap.put( "Mass", mMassUnits );
        mUnitTypeMap.put( "Pressure", mPressureUnits );
    }

    private final HashMap<String, ArrayAdapter<Unit>> mUnitAdapters = new HashMap<>();

    private DecimalFormat mFormat;

    private TextInputLayout mFromUnitSpinner;
    private TextInputLayout mToUnitSpinner;
    private TextInputLayout mFromUnitValue;
    private TextInputLayout mToUnitValue;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.e6b_unit_convert_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String subTitle = args.getString( ListMenuFragment.SUBTITLE_TEXT );
        getSupportActionBar().setSubtitle( subTitle );

        // Create the adapters for all unit types
        for ( String type : mUnitTypeMap.keySet() ) {
            ArrayAdapter<Unit> adapter = new ArrayAdapter<>( getActivity(),
                        R.layout.list_item, mUnitTypeMap.get( type ) );
            mUnitAdapters.put( type, adapter );
        }

        mFormat = new DecimalFormat( "#,##0.###" );

        String [] types = mUnitTypeMap.keySet().toArray( new String[ 0 ] );
        Arrays.sort( types );
        ArrayAdapter<String> adapter = new ArrayAdapter<>( getActivity(),
                R.layout.list_item, types );

        TextInputLayout unitTypeSpinner = findViewById( R.id.e6b_unit_type_spinner );
        AutoCompleteTextView textView = getAutoCompleteTextView( unitTypeSpinner );
        textView.setAdapter( adapter );
        textView.setOnItemClickListener( this );

        mFromUnitSpinner = findViewById( R.id.e6b_unit_from_spinner );
        mToUnitSpinner = findViewById( R.id.e6b_unit_to_spinner );
        mFromUnitValue = findViewById( R.id.e6b_unit_from_value );
        mToUnitValue = findViewById( R.id.e6b_unit_to_value );

        addSpinnerField( mFromUnitSpinner );
        addSpinnerField( mToUnitSpinner );
        addEditField( mFromUnitValue );
        addReadOnlyField( mToUnitValue );

        setFragmentContentShown( true );
    }

    @Override
    protected String getMessage() {
        return null;
    }

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
        String type = parent.getItemAtPosition( position ).toString();
        ArrayAdapter<Unit> unitAdapter = mUnitAdapters.get( type );
        clearEditText( mFromUnitValue );
        clearEditText( mToUnitValue );
        setSpinnerAdapter( mFromUnitSpinner, unitAdapter, 0 );
        setSpinnerAdapter( mToUnitSpinner, unitAdapter, 1 );
    }

    @Override
    protected void processInput() {
        try {
            Unit fromUnit = (Unit) getSelectedItem( mFromUnitSpinner );
            Unit toUnit = (Unit) getSelectedItem( mToUnitSpinner );
            if ( fromUnit != null && toUnit != null ) {
                EditText editText = mFromUnitValue.getEditText();
                editText.setInputType( fromUnit.getInputType() );
                double fromValue = parseDouble( mFromUnitValue );
                double toValue = fromUnit.convertTo( toUnit, fromValue );
                showValue( mToUnitValue, mFormat.format( toValue ) );
            }
        } catch ( NumberFormatException e ) {
            clearEditText( mToUnitValue );
        }
    }

    private static abstract class Unit {

        protected abstract double multiplicationFactor();

        protected double toNormalized( double value ) {
            return value*multiplicationFactor();
        }

        protected double fromNormalized( double value ) {
            return value/multiplicationFactor();
        }

        public double convertTo( Unit to, double value ) {
            return to.fromNormalized( toNormalized( value ) );
        }

        public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        }
    }

    // See http://en.wikipedia.org/wiki/Conversion_of_units

    // Temperature conversion via Kelvin
    private static class Celsius extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0;
        }

        @Override
        protected double toNormalized( double value ) {
            return value+273.15;
        }

        @Override
        protected double fromNormalized( double value ) {
            return value-273.15;
        }

        public int getInputType() {
            return super.getInputType() | InputType.TYPE_NUMBER_FLAG_SIGNED;
        }

        @NonNull
        @Override
        public String toString() {
            return "\u00B0C";
        }
    }

    private static class Fahrenheit extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0;
        }

        @Override
        protected double toNormalized( double value ) {
            return ( value+459.67 )*5.0/9.0;
        }

        @Override
        protected double fromNormalized( double value ) {
            return ( value*9/5 )-459.67;
        }

        public int getInputType() {
            return super.getInputType() | InputType.TYPE_NUMBER_FLAG_SIGNED;
        }

        @NonNull
        @Override
        public String toString() {
            return "\u00B0F";
        }
    }

    private static class Rankine extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 5.0/9.0;
        }

        public int getInputType() {
            return super.getInputType() | InputType.TYPE_NUMBER_FLAG_SIGNED;
        }

        @NonNull
        @Override
        public String toString() {
            return "\u00B0Ra";
        }
    }

    private static class Kelvin extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1.0;
        }

        public int getInputType() {
            return super.getInputType() | InputType.TYPE_NUMBER_FLAG_SIGNED;
        }

        @NonNull
        @Override
        public String toString() {
            return "K";
        }
    }

    // Distance conversion via Meters
    private static class StatuteMile extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1609.344;
        }

        @NonNull
        @Override
        public String toString() {
            return "mi";
        }
    }

    private static class NauticalMile extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1852.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "nm";
        }
    }

    private static class Yard extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.9144;
        }

        @NonNull
        @Override
        public String toString() {
            return "yd";
        }
    }

    private static class Foot extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.3048;
        }

        @NonNull
        @Override
        public String toString() {
            return "ft";
        }
    }

    private static class Inch extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.0254;
        }

        @NonNull
        @Override
        public String toString() {
            return "in";
        }
    }

    private static class Centimeter extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.01;
        }

        @NonNull
        @Override
        public String toString() {
            return "cm";
        }
    }

    private static class Millimeter extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.001;
        }

        @NonNull
        @Override
        public String toString() {
            return "mm";
        }
    }

    private static class KiloMeter extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1000.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "km";
        }
    }

    private static class Meter extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "m";
        }
    }

    // Speed conversion via meters/s
    private static class Knot extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.514444;
        }

        @NonNull
        @Override
        public String toString() {
            return "knot";
        }
    }

    private static class MilePerHour extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.44704;
        }

        @NonNull
        @Override
        public String toString() {
            return "mi/h";
        }
    }

    private static class KilometerPerHour extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.277778;
        }

        @NonNull
        @Override
        public String toString() {
            return "km/h";
        }
    }

    private static class FootPerSecond extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.3048;
        }

        @NonNull
        @Override
        public String toString() {
            return "ft/s";
        }
    }

    private static class MeterPerSecond extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "m/s";
        }
    }

    // Volume conversion via Litres
    private static class Gallon extends Unit {

        @Override
        public double multiplicationFactor() {
            return 3.785411784;
        }

        @NonNull
        @Override
        public String toString() {
            return "gal";
        }
    }

    private static class Quart extends Unit {

        @Override
        public double multiplicationFactor() {
            return 0.946353;
        }

        @NonNull
        @Override
        public String toString() {
            return "qt";
        }
    }

    private static class FluidOunce extends Unit {

        @Override
        public double multiplicationFactor() {
            return 0.0295735296;
        }

        @NonNull
        @Override
        public String toString() {
            return "fl oz";
        }
    }

    private static class MilliLiter extends Unit {

        @Override
        public double multiplicationFactor() {
            return 0.001;
        }

        @NonNull
        @Override
        public String toString() {
            return "mL";
        }
    }

    private static class Liter extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "L";
        }
    }

    // Pressure conversion via Millibar
    private static class InchOfHg extends Unit {

        @Override
        public double multiplicationFactor() {
            return 33.863753;
        }

        @NonNull
        @Override
        public String toString() {
            return "inHg";
        }
    }

    private static class HectoPascal extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "hPa";
        }
    }

    private static class Millibar extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "mbar";
        }
    }

    // Weight conversion via Grams
    private static class Pound extends Unit {

        @Override
        public double multiplicationFactor() {
            return 453.59237;
        }

        @NonNull
        @Override
        public String toString() {
            return "lb";
        }
    }

    private static class Ounce extends Unit {

        @Override
        public double multiplicationFactor() {
            return 28.349523;
        }

        @NonNull
        @Override
        public String toString() {
            return "oz";
        }
    }

    private static class KiloGram extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1000.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "kg";
        }
    }

    private static class Gram extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }

        @NonNull
        @Override
        public String toString() {
            return "g";
        }
    }

}
