/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListMenuFragment;
import com.nadmm.airports.R;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Set;

public class UnitConvertFrament extends FragmentBase implements OnItemSelectedListener {

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

    private Spinner mUnitTypeSpinner;
    private Spinner mFromUnitSpinner;
    private Spinner mToUnitSpinner;
    private EditText mFromUnitValue;
    private EditText mToUnitValue;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.unit_convert_view, container, false );
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
            mUnitAdapters.put( type, getArrayAdapter( mUnitTypeMap.get( type ) ) );
        }

        mFormat = new DecimalFormat( "#,##0.###" );

        mUnitTypeSpinner = (Spinner) findViewById( R.id.unit_type_spinner );
        ArrayAdapter<String> adapter = getArrayAdapter( mUnitAdapters.keySet() );
        mUnitTypeSpinner.setAdapter( adapter );
        mUnitTypeSpinner.setOnItemSelectedListener( this );

        mFromUnitSpinner = (Spinner) findViewById( R.id.unit_from_spinner );
        mFromUnitSpinner.setOnItemSelectedListener( this );
        mToUnitSpinner = (Spinner) findViewById( R.id.unit_to_spinner );
        mToUnitSpinner.setOnItemSelectedListener( this );

        mToUnitValue = (EditText) findViewById( R.id.unit_to_value );
        mToUnitValue.setFocusable( false );
        mFromUnitValue = (EditText) findViewById( R.id.unit_from_value );
        mFromUnitValue.addTextChangedListener( new TextWatcher() {

            @Override
            public void onTextChanged( CharSequence s, int start, int before, int count ) {
            }

            @Override
            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            @Override
            public void afterTextChanged( Editable s ) {
                doConversion();
            }
        } );

        Button btn = (Button) findViewById( R.id.btnReset );
        btn.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                mFromUnitValue.setText( "" );
                mToUnitValue.setText( "" );
            }
        } );

        setFragmentContentShown( true );
    }

    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int pos, long id ) {
        int spinnerId = parent.getId();
        if ( spinnerId == R.id.unit_type_spinner ) {
            String type = (String) mUnitTypeSpinner.getSelectedItem();
            SpinnerAdapter unitAdapter = mUnitAdapters.get( type );
            mFromUnitSpinner.setAdapter( unitAdapter );
            mFromUnitSpinner.setSelection( 0, true );
            mToUnitSpinner.setAdapter( unitAdapter );
            mToUnitSpinner.setSelection( 1, true );
        } else {
            Unit fromUnit = (Unit) mFromUnitSpinner.getSelectedItem();
            mFromUnitValue.setInputType( fromUnit.getInputType() );
            doConversion();
        }
    }

    @Override
    public void onNothingSelected( AdapterView<?> parent ) {
    }

    protected void doConversion() {
        if ( mFromUnitSpinner.getSelectedItemPosition() >= 0
             && mToUnitSpinner.getSelectedItemPosition() >= 0 ) {
            try {
                double fromValue = Double.valueOf( mFromUnitValue.getText().toString() );
                Unit fromUnit = (Unit) mFromUnitSpinner.getSelectedItem();
                Unit toUnit = (Unit) mToUnitSpinner.getSelectedItem();
                double toValue = fromUnit.convertTo( toUnit, fromValue );
                mToUnitValue.setText( mFormat.format( toValue ) );
            } catch ( NumberFormatException e ) {
                mToUnitValue.setText( "" );
            }
        }
    }

    protected ArrayAdapter<Unit> getArrayAdapter( Unit[] units ) {
        ArrayAdapter<Unit> adapter = new ArrayAdapter<>( getActivity(),
                android.R.layout.simple_spinner_item, units );
        adapter.setDropDownViewResource( R.layout.support_simple_spinner_dropdown_item );
        return adapter;
    }

    protected ArrayAdapter<String> getArrayAdapter( Set<String> list ) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>( getActivity(),
                android.R.layout.simple_spinner_item, list.toArray( new String[ list.size() ] ) );
        adapter.setDropDownViewResource( R.layout.support_simple_spinner_dropdown_item );
        return adapter;
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

        @Override
        public String toString() {
            return "Celsius";
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

        @Override
        public String toString() {
            return "Fahrenheit";
        }
    }

    private static class Rankine extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 5.0/9.0;
        }

        @Override
        public String toString() {
            return "Rankine";
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

        @Override
        public String toString() {
            return "Kelvins";
        }
    }

    // Distance conversion via Meters
    private static class StatuteMile extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1609.344;
        }

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

        @Override
        public String toString() {
            return "g";
        }
    }

}
