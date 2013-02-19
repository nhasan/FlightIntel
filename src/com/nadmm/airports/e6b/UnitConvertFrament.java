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

package com.nadmm.airports.e6b;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;

public class UnitConvertFrament extends FragmentBase implements OnItemSelectedListener {

    private static final HashMap<String, Unit> mTemperatureUnits 
        = new HashMap<String, Unit>();
    static {
        mTemperatureUnits.put( "Celcius", new Celcius() );
        mTemperatureUnits.put( "Fahrenheit", new Fahrenheit() );
        mTemperatureUnits.put( "Rankine", new Rankine() );
        mTemperatureUnits.put( "Kelvins", new Kelvins() );
    }

    private static final HashMap<String, Unit> mLengthUnits 
        = new HashMap<String, Unit>();
    static {
        mLengthUnits.put( "mi", new StatuteMiles() );
        mLengthUnits.put( "nm", new NauticalMiles() );
        mLengthUnits.put( "yd", new Yards() );
        mLengthUnits.put( "ft", new Feet() );
        mLengthUnits.put( "in", new Inches() );
        mLengthUnits.put( "km", new KiloMeters() );
        mLengthUnits.put( "cm", new Centimeters() );
        mLengthUnits.put( "mm", new Millimeters() );
        mLengthUnits.put( "m", new Meters() );
    }

    private static final HashMap<String, Unit> mSpeedUnits 
        = new HashMap<String, Unit>();
    static {
        mSpeedUnits.put( "knots", new Knots() );
        mSpeedUnits.put( "mi/h", new MilesPerHour() );
        mSpeedUnits.put( "km/h", new KilometersPerHour() );
        mSpeedUnits.put( "ft/s", new FeetPerSecond() );
        mSpeedUnits.put( "m/s", new MetersPerSecond() );
    }

    private static final HashMap<String, Unit> mVolumeUnits
        = new HashMap<String, Unit>();
    static {
        mVolumeUnits.put( "gal", new Gallons() );
        mVolumeUnits.put( "fl oz", new FluidOunces() );
        mVolumeUnits.put( "mL", new MilliLiters() );
        mVolumeUnits.put( "L", new Liters() );
    }

    private static final HashMap<String, Unit> mPressureUnits
        = new HashMap<String, Unit>();
    static {
        mPressureUnits.put( "inHg", new InchesOfHg() );
        mPressureUnits.put( "hPa", new HectoPascals() );
        mPressureUnits.put( "mbar", new Millibars() );
    }

    private static final HashMap<String, Unit> mMassUnits
        = new HashMap<String, Unit>();
    static {
        mMassUnits.put( "lb", new Pounds() );
        mMassUnits.put( "oz", new Ounces() );
        mMassUnits.put( "kg", new KiloGrams() );
        mMassUnits.put( "g", new Grams() );
    }

    private static final HashMap<String, HashMap<String, Unit>> mUnitTypeMap
        = new HashMap<String, HashMap<String,Unit>>();
    static {
        mUnitTypeMap.put( "Temperature", mTemperatureUnits );
        mUnitTypeMap.put( "Length", mLengthUnits );
        mUnitTypeMap.put( "Speed", mSpeedUnits );
        mUnitTypeMap.put( "Volume", mVolumeUnits );
        mUnitTypeMap.put( "Mass", mMassUnits );
        mUnitTypeMap.put( "Pressure", mPressureUnits );
    }

    private final HashMap<String, ArrayAdapter<String>> mUnitAdapters
            = new HashMap<String, ArrayAdapter<String>>();

    private Spinner mUnitTypeSpinner;
    private Spinner mFromUnitSpinner;
    private Spinner mToUnitSpinner;
    private EditText mFromUnitValue;
    private EditText mToUnitValue;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflate( R.layout.unit_convert_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        // Create the adapters for all unit types
        for ( String type : mUnitTypeMap.keySet() ) {
            mUnitAdapters.put( type, getArrayAdapter( mUnitTypeMap.get( type ).keySet() ) );
        }

        mUnitTypeSpinner = (Spinner) findViewById( R.id.unit_type_spinner );
        ArrayAdapter<String> adapter = getArrayAdapter( mUnitAdapters.keySet() );
        mUnitTypeSpinner.setAdapter( adapter );
        mUnitTypeSpinner.setOnItemSelectedListener( this );

        mFromUnitSpinner = (Spinner) findViewById( R.id.unit_from_spinner );
        mFromUnitSpinner.setOnItemSelectedListener( this );
        mToUnitSpinner = (Spinner) findViewById( R.id.unit_to_spinner );
        mToUnitSpinner.setOnItemSelectedListener( this );

        mFromUnitValue = (EditText) findViewById( R.id.unit_from_value );
        mToUnitValue = (EditText) findViewById( R.id.unit_to_value );
        mToUnitValue.setFocusable( false );

        Button btn = (Button) findViewById( R.id.btnConvert );
        btn.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                String type = (String) mUnitTypeSpinner.getSelectedItem();
                HashMap<String, Unit> mUnits = mUnitTypeMap.get( type );
                Unit fromUnit = mUnits.get( (String) mFromUnitSpinner.getSelectedItem() );
                Unit toUnit = mUnits.get( (String) mToUnitSpinner.getSelectedItem() );
                double fromValue = Double.valueOf( mFromUnitValue.getText().toString() );
                double toValue = fromUnit.convertTo( toUnit, fromValue );
                mToUnitValue.setText( String.format( Locale.US, "%.03f", toValue ) );
            }
        } );
    }

    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int pos, long id ) {
        int spinnerId = parent.getId();
        if ( spinnerId == R.id.unit_type_spinner ) {
            Adapter adapter = mUnitTypeSpinner.getAdapter();
            String type = (String) adapter.getItem( pos );
            SpinnerAdapter unitAdapter = mUnitAdapters.get( type );
            mFromUnitSpinner.setAdapter( unitAdapter );
            mToUnitSpinner.setAdapter( unitAdapter );
        } else {

        }
    }

    @Override
    public void onNothingSelected( AdapterView<?> parent ) {
    }

    protected ArrayAdapter<String> getArrayAdapter( Set<String> entries ) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( getActivity(),
                android.R.layout.simple_spinner_item,
                entries.toArray( new String[ entries.size() ]) );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
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
    }

    // See http://en.wikipedia.org/wiki/Conversion_of_units

    // Temperature conversion via Kelvin
    private static class Celcius extends Unit {

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
    }

    private static class Rankine extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 5.0/9.0;
        }
    }

    private static class Kelvins extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1.0;
        }
    }

    // Distance conversion via Meters
    private static class StatuteMiles extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1609.344;
        }
    }

    private static class NauticalMiles extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1852.0;
        }
    }

    private static class Yards extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.9144;
        }
    }

    private static class Feet extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.3048;
        }
    }

    private static class Inches extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.0254;
        }
    }

    private static class Centimeters extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.01;
        }
    }

    private static class Millimeters extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.001;
        }
    }

    private static class KiloMeters extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1000.0;
        }
    }

    private static class Meters extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1.0;
        }
    }

    // Speed conversion via meters/s
    private static class Knots extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.514444;
        }        
    }

    private static class MilesPerHour extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.44704;
        }        
    }

    private static class KilometersPerHour extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.277778;
        }        
    }

    private static class FeetPerSecond extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 0.3048;
        }        
    }

    private static class MetersPerSecond extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1.0;
        }        
    }

    // Volume conversion via Litres
    private static class Gallons extends Unit {

        @Override
        public double multiplicationFactor() {
            return 3.785411784;
        }
    }

    private static class FluidOunces extends Unit {

        @Override
        public double multiplicationFactor() {
            return 0.0295735296;
        }
    }

    private static class MilliLiters extends Unit {

        @Override
        public double multiplicationFactor() {
            return 0.001;
        }
    }

    private static class Liters extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }
    }

    // Pressure conversion via Millibar
    private static class InchesOfHg extends Unit {

        @Override
        public double multiplicationFactor() {
            return 33.863753;
        }
    }

    private static class HectoPascals extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }
    }

    private static class Millibars extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }
    }

    // Weight conversion via Grams
    private static class Pounds extends Unit {

        @Override
        public double multiplicationFactor() {
            return 453.59237;
        }
    }

    private static class Ounces extends Unit {

        @Override
        public double multiplicationFactor() {
            return 28.349523;
        }
    }

    private static class KiloGrams extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1000.0;
        }
    }

    private static class Grams extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1.0;
        }
    }

}
