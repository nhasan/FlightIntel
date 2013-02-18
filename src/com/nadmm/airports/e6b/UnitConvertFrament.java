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
import java.util.Set;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;

public class UnitConvertFrament extends FragmentBase implements OnItemSelectedListener {

    // Temperature
    private final static String UNIT_CELCIUS = "Celcius";
    private final static String UNIT_FAHRENHEIT = "Fahrenheit";
    private final static String UNIT_RANKINE = "Rankine";
    private final static String UNIT_KELVINS = "Kelvins";
    // Distance
    private final static String UNIT_SM = "SM";
    private final static String UNIT_NM = "NM";
    private final static String UNIT_YARDS = "Yards";
    private final static String UNIT_FEET = "Feet";
    private final static String UNIT_INCHES = "Inches";
    private final static String UNIT_KILOMETERS = "Km";
    private final static String UNIT_CENTIMETERS = "cm";
    private final static String UNIT_MILLIMETERS = "mm";
    private final static String UNIT_METERS = "Meters";
    // Speed
    private final static String UNIT_KNOTS = "Knots";
    private final static String UNIT_MILES_PER_HOUR = "Miles/h";
    private final static String UNIT_KILOMETERS_PER_HOUR = "Km/h";
    private final static String UNIT_FEET_PER_SECOND = "Feet/s";
    private final static String UNIT_METERS_PER_SECOND = "Meters/s";
    // Volume
    private final static String UNIT_GALONS = "Gallons";
    private final static String UNIT_FL_OZ = "Fl. Oz";
    private final static String UNIT_MILLILITERS = "mL";
    private final static String UNIT_LITERS = "Liters";
    // Pressure
    private final static String UNIT_INCHES_HG = "inHg";
    private final static String UNIT_HECTOPASCALS = "hPa";
    private final static String UNIT_MILLIBARS = "mBar";
    // Weight
    private final static String UNIT_POUNDS = "Lbs";
    private final static String UNIT_OUNCES = "Oz";
    private final static String UNIT_KILOGRAMS = "Kgs";
    private final static String UNIT_GRAMS = "Grams";

    private static HashMap<String, Unit> mTemperatureUnits 
        = new HashMap<String, Unit>();
    static {
        mTemperatureUnits.put( UNIT_CELCIUS, new Celcius() );
        mTemperatureUnits.put( UNIT_FAHRENHEIT, new Fahrenheit() );
        mTemperatureUnits.put( UNIT_RANKINE, new Rankine() );
        mTemperatureUnits.put( UNIT_KELVINS, new Kelvins() );
    }

    private static HashMap<String, Unit> mDistanceUnits 
        = new HashMap<String, Unit>();
    static {
        mDistanceUnits.put( UNIT_SM, new StatuteMiles() );
        mDistanceUnits.put( UNIT_NM, new NauticalMiles() );
        mDistanceUnits.put( UNIT_YARDS, new Yards() );
        mDistanceUnits.put( UNIT_FEET, new Feet() );
        mDistanceUnits.put( UNIT_INCHES, new Inches() );
        mDistanceUnits.put( UNIT_KILOMETERS, new KiloMeters() );
        mDistanceUnits.put( UNIT_CENTIMETERS, new Centimeters() );
        mDistanceUnits.put( UNIT_MILLIMETERS, new Millimeters() );
        mDistanceUnits.put( UNIT_METERS, new Meters() );
    }

    private static HashMap<String, Unit> mSpeedUnits 
        = new HashMap<String, Unit>();
    static {
        mSpeedUnits.put( UNIT_KNOTS, new Knots() );
        mSpeedUnits.put( UNIT_MILES_PER_HOUR, new MilesPerHour() );
        mSpeedUnits.put( UNIT_KILOMETERS_PER_HOUR, new KilometersPerHour() );
        mSpeedUnits.put( UNIT_FEET_PER_SECOND, new FeetPerSecond() );
        mSpeedUnits.put( UNIT_METERS_PER_SECOND, new MetersPerSecond() );
    }

    private static HashMap<String, Unit> mVolumeUnits
        = new HashMap<String, Unit>();
    static {
        mVolumeUnits.put( UNIT_GALONS, new Gallons() );
        mVolumeUnits.put( UNIT_FL_OZ, new FluidOunces() );
        mVolumeUnits.put( UNIT_MILLILITERS, new MilliLiters() );
        mVolumeUnits.put( UNIT_LITERS, new Liters() );
    }

    private static HashMap<String, Unit> mPressureUnits
        = new HashMap<String, Unit>();
    static {
        mPressureUnits.put( UNIT_INCHES_HG, new InchesOfHg() );
        mPressureUnits.put( UNIT_HECTOPASCALS, new HectoPascals() );
        mPressureUnits.put( UNIT_MILLIBARS, new Millibars() );
    }

    private static HashMap<String, Unit> mWeightUnits
        = new HashMap<String, Unit>();
    static {
        mWeightUnits.put( UNIT_POUNDS, new Pounds() );
        mWeightUnits.put( UNIT_OUNCES, new Ounces() );
        mWeightUnits.put( UNIT_KILOGRAMS, new KiloGrams() );
        mWeightUnits.put( UNIT_GRAMS, new Grams() );
    }

    private HashMap<String, ArrayAdapter<String>> mUnitAdapters;
    private Spinner mUnitTypeSpinner;
    private Spinner mFromUnitSpinner;
    private Spinner mToUnitSpinner;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflate( R.layout.unit_convert_view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mUnitAdapters = new HashMap<String, ArrayAdapter<String>>();
        mUnitAdapters.put( "Temperature", getArrayAdapter( mTemperatureUnits.keySet() ) );
        mUnitAdapters.put( "Distance", getArrayAdapter( mDistanceUnits.keySet() ) );
        mUnitAdapters.put( "Speed", getArrayAdapter( mSpeedUnits.keySet() ) );
        mUnitAdapters.put( "Volume", getArrayAdapter( mVolumeUnits.keySet() ) );
        mUnitAdapters.put( "Weight", getArrayAdapter( mWeightUnits.keySet() ) );
        mUnitAdapters.put( "Pressure", getArrayAdapter( mPressureUnits.keySet() ) );

        mUnitTypeSpinner = (Spinner) findViewById( R.id.unit_type_spinner );
        ArrayAdapter<String> adapter = getArrayAdapter( mUnitAdapters.keySet() );
        mUnitTypeSpinner.setAdapter( adapter );
        mUnitTypeSpinner.setOnItemSelectedListener( this );

        mFromUnitSpinner = (Spinner) findViewById( R.id.unit_from_spinner );
        mFromUnitSpinner.setOnItemSelectedListener( this );
        mToUnitSpinner = (Spinner) findViewById( R.id.unit_to_spinner );
        mToUnitSpinner.setOnItemSelectedListener( this );
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
            return ( value+459.67 )*5/9;
        }

        @Override
        protected double fromNormalized( double value ) {
            return ( value*9/5 )-459.67;
        }
    }

    private static class Rankine extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 5/9;
        }
    }

    private static class Kelvins extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1;
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
            return 1852;
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
            return 1000;
        }
    }

    private static class Meters extends Unit {

        @Override
        protected double multiplicationFactor() {
            return 1;
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
            return 1;
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
            return 1;
        }
    }

    // Pressure conversion via Millibar
    private static class InchesOfHg extends Unit {

        @Override
        public double multiplicationFactor() {
            return 33.86389;
        }
    }

    private static class HectoPascals extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1;
        }
    }

    private static class Millibars extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1;
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
            return 1000;
        }
    }

    private static class Grams extends Unit {

        @Override
        public double multiplicationFactor() {
            return 1;
        }
    }

}
