/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

import java.text.NumberFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.GuiUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.WxUtils;
import com.nadmm.airports.wx.Metar;
import com.nadmm.airports.wx.Metar.Flags;
import com.nadmm.airports.wx.MetarService;
import com.nadmm.airports.wx.SkyCondition;
import com.nadmm.airports.wx.WxSymbol;

public class WxDetailActivity extends ActivityBase {

    private BroadcastReceiver mReceiver;
    private View mContentView;
    private Cursor[] mCursors;
    private String mIcaoCode;
    private long mElevation;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String icaoCode = intent.getStringExtra( MetarService.STATION_ID );
                if ( mIcaoCode.equals( icaoCode ) ) {
                    showWeather( intent );
                }
            }

        };

        
        Intent intent = getIntent();
        String icaoCode = intent.getStringExtra( MetarService.STATION_ID );
        String sensorId = intent.getStringExtra( Awos.WX_SENSOR_IDENT );
        WxDetailTask task = new WxDetailTask();
        task.execute( icaoCode, sensorId );
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction( MetarService.ACTION_GET_METAR );
        registerReceiver( mReceiver, filter );
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver( mReceiver );
    }

    private final class WxDetailTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            mIcaoCode = params[ 0 ];
            String sensorId = params[ 1 ];

            Cursor[] cursors = new Cursor[ 1 ];
            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Awos.TABLE_NAME+" w LEFT OUTER JOIN "+Airports.TABLE_NAME+" a"
                    +" ON w."+Awos.WX_SENSOR_IDENT+"=a."+Airports.FAA_CODE );
            Cursor c = builder.query( db, new String[] { "*" }, Awos.WX_SENSOR_IDENT+"=?",
                    new String[] { sensorId }, null, null, null, null );
            cursors[ 0 ] = c;

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            // Now request the weather
            mCursors = result;

            Cursor awos = result[ 0 ];
            if ( !awos.moveToFirst() ) {
                GuiUtils.showToast( WxDetailActivity.this, "Unable to get weather station info" );
                finish();
                return;
            }

            mContentView = inflate( R.layout.wx_detail_view );
            setContentView( mContentView );

            mElevation = awos.getInt( awos.getColumnIndex( Airports.ELEVATION_MSL ) );

            showWxTitle( mCursors );

            String phone = awos.getString( awos.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
            TextView tv = (TextView) findViewById( R.id.wx_station_phone );
            if ( phone.length() > 0 ) {
                tv.setText( phone );
                makeClickToCall( tv );
                tv.setVisibility( View.VISIBLE );
            }
            String freq = awos.getString( awos.getColumnIndex( Awos.STATION_FREQUENCY ) );
            tv = (TextView) findViewById( R.id.wx_station_freq );
            if ( freq.length() > 0 ) {
                tv.setText( freq );
                tv.setVisibility( View.VISIBLE );
            }

            ImageView iv = (ImageView) findViewById( R.id.wx_refresh );
            AnimationDrawable refresh = (AnimationDrawable) iv.getDrawable();
            refresh.start();

            Intent service = new Intent( WxDetailActivity.this, MetarService.class );
            service.setAction( MetarService.ACTION_GET_METAR );
            service.putExtra( MetarService.STATION_ID, mIcaoCode );
            startService( service );
        }

    }

    protected void showWeather( Intent intent ) {
        ImageView iv = (ImageView) findViewById( R.id.wx_refresh );
        if ( iv == null ) {
            // We got the broadcast when we are not ready
            return;
        }

        AnimationDrawable refresh = (AnimationDrawable) iv.getDrawable();

        iv.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                ImageView iv = (ImageView) findViewById( R.id.wx_refresh );
                AnimationDrawable refresh = (AnimationDrawable) iv.getDrawable();
                refresh.start();
                Intent service = new Intent( WxDetailActivity.this, MetarService.class );
                service.setAction( MetarService.ACTION_GET_METAR );
                service.putExtra( MetarService.STATION_ID, mIcaoCode );
                service.putExtra( MetarService.FORCE_REFRESH, true );
                startService( service );
            }

        } );

        LinearLayout layout;
        TextView tv;

        View detail = findViewById( R.id.wx_detail_layout );

        tv =(TextView) findViewById( R.id.status_msg );
        layout = (LinearLayout) findViewById( R.id.wx_status_layout );
        layout.removeAllViews();
        if ( !intent.hasExtra( MetarService.RESULT ) ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );
            tv.setText( R.string.metar_error );
            addRow( layout, "This could be due to the following reasons:" );
            addBulletedRow( layout, "Network connection is not available" );
            addBulletedRow( layout, "ADDS does not publish METAR for this station" );
            addBulletedRow( layout, "Station is currently out of service" );
            addBulletedRow( layout, "Station has not updated the METAR for more than 3 hours" );
            detail.setVisibility( View.GONE );
            refresh.stop();
            return;
        } else {
            tv.setText( "" );
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
            detail.setVisibility( View.VISIBLE );
        }

        Metar metar = (Metar) intent.getSerializableExtra( MetarService.RESULT );

        Date now = new Date();

        if ( metar.stationElevationMeters < Integer.MAX_VALUE ) {
            mElevation = DataUtils.metersToFeet( metar.stationElevationMeters );
            tv = (TextView) findViewById( R.id.wx_station_info2 );
            tv.setText( String.format( "Located at %d' MSL elevation", mElevation ) );
        } else {
            metar.stationElevationMeters = DataUtils.feetToMeters( mElevation );
        }

        tv = (TextView) findViewById( R.id.wx_age );
        long age = now.getTime()-metar.observationTime;
        tv.setText( TimeUtils.formatDuration( age )+" old" );

        tv = (TextView) findViewById( R.id.wx_station_info3 );
        tv.setText( metar.flightCategory+" conditions prevailing" );
        WxUtils.setFlightCategoryDrawable( tv, metar );

        // Raw Text
        tv = (TextView) findViewById( R.id.wx_raw_metar );
        tv.setText( metar.rawText );

        // Winds
        tv = (TextView) findViewById( R.id.wx_wind_label );
        layout = (LinearLayout) findViewById( R.id.wx_wind_layout );
        layout.removeAllViews();
        if ( metar.windSpeedKnots < Integer.MAX_VALUE ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );

            addWindRow( layout, metar );
            if ( metar.wshft ) {
                String s = "Wind shift detected during past hour";
                if ( metar.fropa ) {
                    s += " due to frontal passage";
                }
                addSeparator( layout );
                addRow( layout, s );
            }
        } else {
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }

        // Visibility
        tv = (TextView) findViewById( R.id.wx_vis_label );
        layout = (LinearLayout) findViewById( R.id.wx_vis_layout );
        layout.removeAllViews();
        if ( metar.visibilitySM < Float.MAX_VALUE ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );

            if ( metar.flags.contains( Flags.AutoReport ) && metar.visibilitySM == 10 ) {
                addRow( layout, "10 statute miles or more horizontal" );
            } else {
                NumberFormat decimal = NumberFormat.getNumberInstance();
                decimal.setMaximumFractionDigits( 2 );
                decimal.setMinimumFractionDigits( 0 );
                addRow( layout, String.format( "%s statute miles horizontal",
                        decimal.format( metar.visibilitySM ) ) );
            }
        } else {
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }

        // Weather
        layout = (LinearLayout) findViewById( R.id.wx_weather_layout );
        layout.removeAllViews();
        if ( metar.wxList != null && !metar.wxList.isEmpty() ) {
            int row = 0;
            for ( WxSymbol wx : metar.wxList ) {
                if ( row > 0 ) {
                    addSeparator( layout );
                }
                addWeatherRow( layout, wx, metar.flightCategory );
                ++row;
            }
        } else {
            addRow( layout, "No significant weather observed at this time" );
        }

        // Sky Conditions
        tv = (TextView) findViewById( R.id.wx_sky_cond_label );
        layout = (LinearLayout) findViewById( R.id.wx_sky_cond_layout );
        layout.removeAllViews();
        if ( !metar.skyConditions.isEmpty() ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );

            int i = 0;
            for ( SkyCondition sky : metar.skyConditions ) {
                if ( i > 0 ) {
                    addSeparator( layout );
                }
                addSkyConditionRow( layout, sky, metar.flightCategory );
                ++i;
            }
        } else {
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }

        // Temperature
        tv = (TextView) findViewById( R.id.wx_temp_label );
        layout = (LinearLayout) findViewById( R.id.wx_temp_layout );
        layout.removeAllViews();
        if ( metar.tempCelsius < Float.MAX_VALUE ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );

            addRow( layout, "Temperature",
                    String.format( "%.1f\u00B0C (%.0f\u00B0F)", metar.tempCelsius,
                    WxUtils.celsiusToFahrenheit( metar.tempCelsius ) ) );
            addSeparator( layout );
            addRow( layout, "Dew point",
                    String.format( "%.1f\u00B0C (%.0f\u00B0F)", metar.dewpointCelsius,
                    WxUtils.celsiusToFahrenheit( metar.dewpointCelsius ) ) );
            addSeparator( layout );
            addRow( layout,"Relative humidity", String.format( "%.0f%%",
                    WxUtils.getRelativeHumidity( metar.tempCelsius, metar.dewpointCelsius ) ) );

            long denAlt = WxUtils.getDensityAltitude( metar );
            if ( denAlt > mElevation ) {
                NumberFormat decimal = NumberFormat.getNumberInstance();
                long presAlt = WxUtils.getPressureAltitude( metar );
                if ( presAlt > mElevation ) {
                    addSeparator( layout );
                    addRow( layout, "Pressure altitude",
                            String.format( "%s ft", decimal.format( presAlt ) ) );
                }
                addSeparator( layout );
                addRow( layout, "Density altitude",
                        String.format( "%s ft", decimal.format( denAlt ) ) );
            }

            if ( metar.maxTemp6HrCentigrade < Float.MAX_VALUE ) {
                addSeparator( layout );
                addRow( layout, "6-hour maximum", String.format( "%.1f\u00B0C (%.0f\u00B0F)",
                        metar.maxTemp6HrCentigrade,
                        WxUtils.celsiusToFahrenheit( metar.maxTemp6HrCentigrade ) ) );
            }
            if ( metar.minTemp6HrCentigrade < Float.MAX_VALUE ) {
                addSeparator( layout );
                addRow( layout, "6-hour minimum", String.format( "%.1f\u00B0C (%.0f\u00B0F)",
                        metar.minTemp6HrCentigrade,
                        WxUtils.celsiusToFahrenheit( metar.minTemp6HrCentigrade ) ) );
            }
            if ( metar.maxTemp24HrCentigrade < Float.MAX_VALUE ) {
                addSeparator( layout );
                addRow( layout, "24-hour maximum", String.format( "%.1f\u00B0C (%.0f\u00B0F)",
                        metar.maxTemp24HrCentigrade,
                        WxUtils.celsiusToFahrenheit( metar.maxTemp24HrCentigrade ) ) );
            }
            if ( metar.minTemp24HrCentigrade < Float.MAX_VALUE ) {
                addSeparator( layout );
                addRow( layout, "24-hour minimum", String.format( "%.1f\u00B0C (%.0f\u00B0F)",
                        metar.minTemp24HrCentigrade,
                        WxUtils.celsiusToFahrenheit( metar.minTemp24HrCentigrade ) ) );
            }
        } else {
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }

        // Pressure
        tv = (TextView) findViewById( R.id.wx_pressure_label );
        layout = (LinearLayout) findViewById( R.id.wx_pressure_layout );
        layout.removeAllViews();
        if ( metar.altimeterHg < Float.MAX_VALUE ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );

            addRow( layout, "Altimeter", String.format( "%.2f' Hg (%.1f mb)",
                    metar.altimeterHg, WxUtils.hgToMillibar( metar.altimeterHg ) ) );
            if ( metar.seaLevelPressureMb < Float.MAX_VALUE ) {
                addSeparator( layout );
                addRow( layout, "Sea level pressure",
                        String.format( "%.1f mb", metar.seaLevelPressureMb ) );
            }
            if ( metar.pressureTend3HrMb < Float.MAX_VALUE ) {
                addSeparator( layout );
                addRow( layout, "3-hour tendency", String.format( "%+.2f mb",
                        metar.pressureTend3HrMb ) );
            }
            if ( metar.presfr ) {
                addSeparator( layout );
                addRow( layout, "Pressure falling rapidly" );
            } if ( metar.presrr ) {
                addSeparator( layout );
                addRow( layout, "Pressure rising rapidly" );
            }
        } else {
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }

        // Precipitation
        tv = (TextView) findViewById( R.id.wx_precip_label );
        layout = (LinearLayout) findViewById( R.id.wx_precip_layout );
        layout.removeAllViews();
        int row = 0;
        if ( metar.precipInches < Float.MAX_VALUE ) {
            if ( row > 0 ) {
                addSeparator( layout );
            }
            addRow( layout, "1-hour precipitation", String.format( "%.2f'",
                    metar.precipInches ) );
            ++row;
        }
        if ( metar.precip3HrInches < Float.MAX_VALUE ) {
            if ( row > 0 ) {
                addSeparator( layout );
            }
            addRow( layout, "3-hour precipitation", String.format( "%.2f'",
                    metar.precip3HrInches ) );
            ++row;
        }
        if ( metar.precip6HrInches < Float.MAX_VALUE ) {
            if ( row > 0 ) {
                addSeparator( layout );
            }
            addRow( layout, "6-hour precipitation", String.format( "%.2f'",
                    metar.precip6HrInches ) );
            ++row;
        }
        if ( metar.precip24HrInches < Float.MAX_VALUE ) {
            if ( row > 0 ) {
                addSeparator( layout );
            }
            addRow( layout, "24-hour precipitation", String.format( "%.2f'",
                    metar.precip24HrInches ) );
            ++row;
        }
        if ( metar.snowInches < Float.MAX_VALUE ) {
            if ( row > 0 ) {
                addSeparator( layout );
            }
            addRow( layout, "Snow depth", String.format( "%.0f'", metar.snowInches ) );
            ++row;
        }
        if ( metar.snincr ) {
            if ( row > 0 ) {
                addSeparator( layout );
            }
            addRow( layout, "Snow is increasing rapidly" );
            ++row;
        }

        if ( row > 0 ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );
        } else {
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }

        // Remarks
        tv = (TextView) findViewById( R.id.wx_remarks_label );
        layout = (LinearLayout) findViewById( R.id.wx_remarks_layout );
        layout.removeAllViews();
        if ( !metar.flags.isEmpty() ) {
            tv.setVisibility( View.VISIBLE );
            layout.setVisibility( View.VISIBLE );

            for ( Flags flag : metar.flags ) {
                addBulletedRow( layout, flag.toString() );
            }
        } else {
            tv.setVisibility( View.GONE );
            layout.setVisibility( View.GONE );
        }

        tv = (TextView) findViewById( R.id.wx_fetch_time );
        tv.setText( "Fetched on "
                +DateFormat.format( "MMM dd, yyyy h:mmaa", new Date( metar.fetchTime ) ) );
        tv.setVisibility( View.VISIBLE );

        refresh.stop();
    }

    protected String getWindsDescription( Metar metar ) {
        StringBuilder s = new StringBuilder();
        if ( metar.windDirDegrees == 0 && metar.windSpeedKnots == 0 ) {
            s.append( "Winds are calm" );
        } else if ( metar.windDirDegrees == 0 ) {
            s.append( String.format( "Winds variable at %d knots", metar.windSpeedKnots ) );
        } else {
            s.append( String.format( "From %s (%03d\u00B0) at %d knots",
                    GeoUtils.getCardinalDirection( metar.windDirDegrees ),
                    metar.windDirDegrees, metar.windSpeedKnots ) );
            if ( metar.windGustKnots < Integer.MAX_VALUE ) {
                s.append( String.format( " gusting to %d knots", metar.windGustKnots ) );
            }
            if ( metar.windPeakKnots < Integer.MAX_VALUE ) {
                s.append( String.format( ", peaking at %d knots", metar.windPeakKnots ) );
            }
        }
        return s.toString();
    }

    protected Drawable getWindDrawable( Metar metar ) {
        Bitmap windsock = BitmapFactory.decodeResource( getResources(), R.drawable.windsock );
        Bitmap bmp = Bitmap.createBitmap( windsock.getWidth(), windsock.getHeight(),
                Bitmap.Config.ARGB_8888 );
        Canvas canvas = new Canvas( bmp );
        canvas.rotate( metar.windDirDegrees, bmp.getWidth()/2, bmp.getHeight()/2 );
        canvas.drawBitmap( windsock, 0, 0, null );
        return new BitmapDrawable( bmp );
    }

    protected void addWindRow( LinearLayout layout, Metar metar ) {
        LinearLayout row = (LinearLayout) inflate( R.layout.simple_row_item );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( getWindsDescription( metar ) );
        if ( metar.windDirDegrees > 0 ) {
            Drawable wind = getWindDrawable( metar );
            tv.setCompoundDrawablesWithIntrinsicBounds( wind, null, null, null );
            tv.setCompoundDrawablePadding( convertDpToPx( 6 ) );
        }

        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSkyConditionRow( LinearLayout layout, SkyCondition sky,
            String flightCategory ) {
        LinearLayout row = (LinearLayout) inflate( R.layout.simple_row_item );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( sky.toString() );
        WxUtils.setColorizedDrawable( tv, flightCategory, sky.getDrawable() );

        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addWeatherRow( LinearLayout layout, WxSymbol wx, String flightCategory ) {
        LinearLayout row = (LinearLayout) inflate( R.layout.simple_row_item );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( wx.toString() );
        WxUtils.setColorizedDrawable( tv, flightCategory, wx.getDrawable() );

        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

}
