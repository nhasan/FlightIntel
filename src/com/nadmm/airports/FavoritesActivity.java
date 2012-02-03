/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.DatabaseManager.Wxl;
import com.nadmm.airports.utils.AirportsCursorAdapter;
import com.nadmm.airports.utils.AirportsCursorHelper;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.utils.WxUtils;
import com.nadmm.airports.wx.Metar;
import com.nadmm.airports.wx.MetarService;
import com.nadmm.airports.wx.WxSymbol;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;

public class FavoritesActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_tabs_pager_layout );

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );

        TabsAdapter adapter = new TabsAdapter( this, pager );
        adapter.addTab( "AIRPORTS", FavoriteAirportsFragment.class, null );
        adapter.addTab( "WEATHER", FavoriteWxFragment.class, null );

        TabPageIndicator tabIndicator = (TabPageIndicator) findViewById( R.id.page_titles );
        tabIndicator.setViewPager( pager );

        if ( savedInstanceState != null ) {
            pager.setCurrentItem( savedInstanceState.getInt( "favtab" ) );
        }
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "favtab", pager.getCurrentItem() );
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        menu.findItem( R.id.menu_favorites ).setEnabled( false );
        return super.onPrepareOptionsMenu( menu );
    }

    public static class FavoriteAirportsFragment extends ListFragment {

        public class FavoriteAirportsTask extends AsyncTask<Void, Void, Cursor> {

            private final FavoriteAirportsFragment mFragment;

            public FavoriteAirportsTask( FavoriteAirportsFragment fragment ) {
                super();
                mFragment = fragment;
            }

            @Override
            protected Cursor doInBackground( Void... params ) {
                ActivityBase activity = (ActivityBase) getActivity();
                DatabaseManager dbManager = activity.getDbManager();
                ArrayList<String> favorites = dbManager.getAptFavorites();
                String selection = "";
                for (String site_number : favorites ) {
                    if ( selection.length() > 0 ) {
                        selection += ", ";
                    }
                    selection += "'"+site_number+"'";
                };

                // Query for the favorite airports
                selection = "a."+Airports.SITE_NUMBER+" in ("+selection+")";
                Cursor c = AirportsCursorHelper.query( getActivity(), selection, 
                        null, null, null, Airports.FACILITY_NAME, null );

                return c;
            }

            @Override
            protected void onPostExecute( Cursor c ) {
                mFragment.setCursor( c );
            }

        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            AirportsCursorAdapter adapter = (AirportsCursorAdapter) getListAdapter();
            if ( adapter != null ) {
                Cursor c = adapter.getCursor();
                c.close();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            FavoriteAirportsTask task = new FavoriteAirportsTask( this );
            task.execute( (Void[]) null );
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            super.onActivityCreated( savedInstanceState );
            setEmptyText( "No favorite airports yet" );
        }

        @Override
        public void onListItemClick( ListView l, View view, int position, long id ) {
            Cursor c = (SQLiteCursor) l.getItemAtPosition( position );
            String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            Intent intent = new Intent( getActivity(), AirportDetailsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            startActivity( intent );
        }

        public void setCursor( final Cursor c ) {
            // We may get called here after activity has detached
            if ( getActivity() != null ) {
                AirportsCursorAdapter adapter = (AirportsCursorAdapter) getListAdapter();
                if ( adapter == null ) {
                    adapter = new AirportsCursorAdapter( getActivity(), c );
                    setListAdapter( adapter );
                } else {
                    adapter.changeCursor( c );
                }
            }
        }

    }

    public static class FavoriteWxFragment extends ListFragment {

        protected HashMap<String, Metar> mStationWx = new HashMap<String, Metar>();
        protected BroadcastReceiver mReceiver;
        protected FavoriteWxTask mTask;
        protected int mWxUpdates = 0;

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            mReceiver = new BroadcastReceiver() {
                
                @Override
                public void onReceive( Context context, Intent intent ) {
                    Metar metar = (Metar) intent.getSerializableExtra( MetarService.RESULT );
                    mStationWx.put( metar.stationId, metar );

                    ++mWxUpdates;
                    if ( mWxUpdates == mStationWx.size() ) {
                        // We have all the wx updates, stop the refresh animation
                        mWxUpdates = 0;
                        ActivityBase activity = (ActivityBase) getActivity();
                        activity.stopRefreshAnimation();
                        CursorAdapter adapter = (CursorAdapter) getListAdapter();
                        adapter.notifyDataSetChanged();
                    }
                }
            };

            setHasOptionsMenu( true );
            super.onCreate( savedInstanceState );
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            WxCursorAdapter adapter = (WxCursorAdapter) getListAdapter();
            if ( adapter != null ) {
                Cursor c = adapter.getCursor();
                c.close();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            ActivityBase activity = (ActivityBase) getActivity();
            IntentFilter filter = new IntentFilter();
            filter.addAction( MetarService.ACTION_GET_METAR );
            activity.registerReceiver( mReceiver, filter );
            startTask();
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().unregisterReceiver( mReceiver );
            stopTask();
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            super.onActivityCreated( savedInstanceState );
            setEmptyText( "No favorite weather stations yet" );
        }

        @Override
        public void onListItemClick( ListView l, View view, int position, long id ) {
            Cursor c = (Cursor) l.getItemAtPosition( position );
            String icaoCode = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
            String stationId = c.getString( c.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
            Intent intent = new Intent( getActivity(), WxDetailActivity.class );
            intent.putExtra( MetarService.STATION_ID, icaoCode );
            intent.putExtra( Awos.WX_SENSOR_IDENT, stationId );
            startActivity( intent );
        }

        public void startTask() {
            mTask = new FavoriteWxTask( this );
            mTask.execute( (Void[]) null );
        }

        public void stopTask() {
            if ( mTask.getStatus() != Status.FINISHED ) {
                mTask.cancel( true );
            }
        }

        public void setCursor( final Cursor c ) {
            // We may get called here after activity has detached
            ActivityBase activity = (ActivityBase) getActivity();
            if ( activity != null ) {
                WxCursorAdapter adapter = (WxCursorAdapter) getListAdapter();
                if ( adapter == null ) {
                    adapter = new WxCursorAdapter( activity, c );
                    setListAdapter( adapter );
                } else {
                    adapter.changeCursor( c );
                }
                requestMetars( false );
            }
        }

        protected void requestMetars( Boolean force ) {
            ActivityBase activity = (ActivityBase) getActivity();
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences( activity );
            boolean alwaysAutoFetch = prefs.getBoolean(
                    PreferencesActivity.ALWAYS_AUTO_FETCH_WEATHER, false );
            boolean cacheOnly = ( !alwaysAutoFetch 
                    && !NetworkUtils.isConnectedToWifi( activity ) );

            if ( force || !cacheOnly ) {
                activity.startRefreshAnimation();
            }

            for ( String icaoCode : mStationWx.keySet() ) {
                Intent service = new Intent( activity, MetarService.class );
                service.setAction( MetarService.ACTION_GET_METAR );
                service.putExtra( MetarService.STATION_ID, icaoCode );
                if ( force ) {
                    service.putExtra( MetarService.FORCE_REFRESH, true );
                }
                else if ( cacheOnly ) {
                    service.putExtra( MetarService.CACHE_ONLY, true );
                }
                activity.startService( service );
            }
        }

        @Override
        public void onPrepareOptionsMenu( Menu menu ) {
            ActivityBase activity = (ActivityBase) getActivity();
            activity.setRefreshItemVisible( true );
        }

        @Override
        public boolean onOptionsItemSelected( MenuItem item ) {
            // Handle item selection
            switch ( item.getItemId() ) {
            case R.id.menu_refresh:
                requestMetars( true );
                return true;
            default:
                return super.onOptionsItemSelected( item );
            }
        }

        private final class AwosData implements Comparable<AwosData> {

            public String ICAO_CODE;
            public String SENSOR_IDENT;
            public String SENSOR_TYPE;
            public String FREQUENCY;
            public String FREQUENCY2;
            public String PHONE;
            public String NAME;
            public String CITY;
            public String STATE;
            public int ELEVATION;
            public double LATITUDE;
            public double LONGITUDE;

            public AwosData( String icaoCode, String id, String type, String freq, String freq2,
                    String phone, String name, String city, String state,
                    int elevation, double lat, double lon ) {
                ICAO_CODE = icaoCode;
                SENSOR_IDENT = id;
                SENSOR_TYPE = type;
                FREQUENCY = freq;
                FREQUENCY2 = freq2;
                PHONE = phone;
                NAME = name;
                CITY = city;
                STATE = state;
                ELEVATION = elevation;
                LATITUDE = lat;
                LONGITUDE = lon;
            }

            @Override
            public int compareTo( AwosData another ) {
                return NAME.compareToIgnoreCase( another.NAME);
            }
        }

        public class FavoriteWxTask extends AsyncTask<Void, Void, Cursor> {

            private final FavoriteWxFragment mFragment;

            public FavoriteWxTask( FavoriteWxFragment fragment ) {
                super();
                mFragment = fragment;
            }

            @Override
            protected Cursor doInBackground( Void... params ) {
                ActivityBase activity = (ActivityBase) getActivity();
                DatabaseManager dbManager = activity.getDbManager();
                SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );

                ArrayList<String> favorites = dbManager.getWxFavorites();
                String selectionList = "";
                for (String facilityId : favorites ) {
                    if ( selectionList.length() > 0 ) {
                        selectionList += ", ";
                    }
                    selectionList += "'"+facilityId+"'";
                };

                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables( Awos.TABLE_NAME+" w"
                        +" LEFT JOIN "+Airports.TABLE_NAME+" a"
                        +" ON w."+Awos.SITE_NUMBER+" = a."+Airports.SITE_NUMBER
                        +" LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                        +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
                String selection = Awos.WX_SENSOR_IDENT+" in ("+selectionList+")";
                Cursor wx1 = builder.query( db, new String[] { "w.*, a.*, s.*" },
                        selection, null, null, null, null, null );

                builder = new SQLiteQueryBuilder();
                builder.setTables( Wxl.TABLE_NAME+" w"
                        +" JOIN "+Airports.TABLE_NAME+" a"
                        +" ON w."+Wxl.LOCATION_ID+" = a."+Airports.FAA_CODE
                        +" LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                        +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
                selection = Wxl.LOCATION_ID+" in ("+selectionList+")"
                        +" AND "+Wxl.LOCATION_ID+" not in ( select "
                        +Awos.WX_SENSOR_IDENT+" from "+Awos.TABLE_NAME+")";

                Cursor wx2 = builder.query( db, new String[] { "w.*, a.*, s.*" },
                        selection, null, null, null, null, null );

                AwosData[] awosList = new AwosData[ wx1.getCount()+wx2.getCount() ];
                int index = 0;

                if ( wx1.moveToFirst() ) {
                    do {
                        String id = wx1.getString( wx1.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
                        String type = wx1.getString( wx1.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
                        String freq = wx1.getString( wx1.getColumnIndex( Awos.STATION_FREQUENCY ) );
                        String freq2 = wx1.getString(
                                wx1.getColumnIndex( Awos.SECOND_STATION_FREQUENCY ) );
                        String phone = wx1.getString(
                                wx1.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
                        String icaoCode = wx1.getString( wx1.getColumnIndex( Airports.ICAO_CODE ) );
                        String name = wx1.getString( wx1.getColumnIndex( Airports.FACILITY_NAME ) );
                        String city = wx1.getString( wx1.getColumnIndex( Airports.ASSOC_CITY ) );
                        String state = wx1.getString( wx1.getColumnIndex( States.STATE_NAME ) );
                        int elevation = wx1.getInt( wx1.getColumnIndex( Airports.ELEVATION_MSL ) );
                        double lat = wx1.getFloat(
                                wx1.getColumnIndex( Awos.STATION_LATTITUDE_DEGREES ) );
                        double lon = wx1.getFloat(
                                wx1.getColumnIndex( Awos.STATION_LONGITUDE_DEGREES ) );
                        AwosData awos = new AwosData( icaoCode, id, type, freq, freq2,
                                phone, name, city, state, elevation, lat, lon );
                        awosList[ index++] = awos;
                    } while ( wx1.moveToNext() );
                }
                wx1.close();

                if ( wx2.moveToFirst() ) {
                    do {
                        String icaoCode = wx2.getString( wx2.getColumnIndex( Airports.ICAO_CODE ) );
                        String id = wx2.getString( wx2.getColumnIndex( Wxl.LOCATION_ID ) );
                        String type = "AWOS";
                        String freq = "";
                        String phone = "";
                        String name = wx2.getString( wx2.getColumnIndex( Airports.FACILITY_NAME ) );
                        String city = wx2.getString( wx2.getColumnIndex( Airports.ASSOC_CITY ) );
                        String state = wx2.getString( wx2.getColumnIndex( States.STATE_NAME ) );
                        int elevation = wx2.getInt( wx2.getColumnIndex( Wxl.LOC_ELEVATION_FEET ) );
                        double lat = wx2.getFloat(
                                wx2.getColumnIndex( Wxl.LOC_LATITUDE_DEGREES ) );
                        double lon = wx2.getFloat(
                                wx2.getColumnIndex( Wxl.LOC_LONGITUDE_DEGREES ) );
                        AwosData awos = new AwosData( icaoCode, id, type, freq, freq,
                                phone, name, city, state, elevation, lat, lon );
                        awosList[ index++] = awos;
                    } while ( wx2.moveToNext() );
                }
                wx2.close();

                Arrays.sort( awosList );

                // Build a cursor out of the sorted wx station list
                String[] columns = new String[] {
                        BaseColumns._ID,
                        Airports.ICAO_CODE,
                        Awos.WX_SENSOR_IDENT,
                        Awos.WX_SENSOR_TYPE,
                        Awos.STATION_FREQUENCY,
                        Awos.SECOND_STATION_FREQUENCY,
                        Awos.STATION_PHONE_NUMBER,
                        Airports.FACILITY_NAME,
                        Airports.ASSOC_CITY,
                        Airports.ASSOC_STATE,
                        Airports.ELEVATION_MSL,
                        Airports.REF_LATTITUDE_DEGREES,
                        Airports.REF_LONGITUDE_DEGREES
                };

                MatrixCursor matrix = new MatrixCursor( columns );

                for ( Object o : awosList ) {
                    AwosData awos = (AwosData) o;
                    mStationWx.put( awos.ICAO_CODE, null );
                    MatrixCursor.RowBuilder row = matrix.newRow();
                    row.add( matrix.getPosition() )
                        .add( awos.ICAO_CODE )
                        .add( awos.SENSOR_IDENT )
                        .add( awos.SENSOR_TYPE )
                        .add( awos.FREQUENCY )
                        .add( awos.FREQUENCY2 )
                        .add( awos.PHONE )
                        .add( awos.NAME )
                        .add( awos.CITY )
                        .add( awos.STATE )
                        .add( awos.ELEVATION )
                        .add( awos.LATITUDE )
                        .add( awos.LONGITUDE );
                }

                return matrix;
            }

            @Override
            protected void onPostExecute( Cursor c ) {
                mFragment.setCursor( c );
            }

        }

        public final class WxCursorAdapter extends ResourceCursorAdapter {

            public WxCursorAdapter( Context context, Cursor c ) {
                super( context, R.layout.wx_list_item, c );
            }

            @Override
            public void bindView( View view, Context context, Cursor c ) {
                String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
                TextView tv = (TextView) view.findViewById( R.id.wx_station_name );
                if ( name != null && name.length() > 0 ) {
                    tv.setText( name );
                }

                String id = c.getString( c.getColumnIndex( Awos.WX_SENSOR_IDENT ) );
                String icaoCode = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
                tv = (TextView) view.findViewById( R.id.wx_station_id );
                if ( icaoCode == null || icaoCode.length() == 0 ) {
                    icaoCode = "K"+id;
                }
                tv.setText( icaoCode );

                StringBuilder info = new StringBuilder();
                String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
                if ( info.length() > 0 ) {
                    info.append( ", " );
                }
                info.append( city );
                String state = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
                if ( info.length() > 0 ) {
                    info.append( ", " );
                }
                info.append( state );
                tv = (TextView) view.findViewById( R.id.wx_station_info );
                tv.setText( info.toString() );

                String freq = c.getString( c.getColumnIndex( Awos.STATION_FREQUENCY ) );
                if ( freq == null || freq.length() == 0 ) {
                    freq = c.getString( c.getColumnIndex( Awos.SECOND_STATION_FREQUENCY ) );
                }
                if ( freq != null && freq.length() > 0 ) {
                    try {
                        tv = (TextView) view.findViewById( R.id.wx_station_freq );
                        tv.setText( String.format( "%.3f", Double.valueOf( freq ) ) );
                    } catch ( NumberFormatException e ) {
                    }
                }

                info = new StringBuilder();
                String type = c.getString( c.getColumnIndex( Awos.WX_SENSOR_TYPE ) );
                info.append( type );
                info.append( ", " );
                int elevation = c.getInt( c.getColumnIndex( Airports.ELEVATION_MSL ) );
                info.append( String.format( "%d' MSL", elevation ) );
                tv = (TextView) view.findViewById( R.id.wx_station_info2 );
                tv.setText( info.toString() );

                tv = (TextView) view.findViewById( R.id.wx_station_phone );
                String phone = c.getString( c.getColumnIndex( Awos.STATION_PHONE_NUMBER ) );
                tv.setText( phone );
                UiUtils.makeClickToCall( context, tv );

                Metar metar = mStationWx.get( icaoCode );
                if ( metar != null )
                {
                    if ( metar.isValid ) {
                        // We have METAR for this station
                        double lat = c.getDouble(
                                c.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
                        double lon = c.getDouble(
                                c.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
                        Location location = new Location( "" );
                        location.setLatitude( lat );
                        location.setLongitude( lon );
                        float declination = GeoUtils.getMagneticDeclination( location );
    
                        tv = (TextView) view.findViewById( R.id.wx_station_name );
                        WxUtils.setColorizedWxDrawable( tv, metar, declination );
    
                        info = new StringBuilder();
                        info.append( metar.flightCategory );
                        if ( metar.wxList.size() > 0 ) {
                            for ( WxSymbol wx : metar.wxList ) {
                                info.append( ", " );
                                info.append( wx.toString() );
                            }
                        }
                        if ( metar.windGustKnots < Integer.MAX_VALUE ) {
                            info.append( ", gusting winds" );
                        } else if ( metar.windSpeedKnots == 0 && metar.windDirDegrees == 0 ) {
                            info.append( ", calm winds" );
                        } else if ( metar.windDirDegrees == 0 ) {
                            info.append( ", variable winds" );
                        } else if ( metar.windSpeedKnots > 10 ) {
                            info.append( ", strong winds" );
                        }
                        tv = (TextView) view.findViewById( R.id.wx_station_wx );
                        tv.setVisibility( View.VISIBLE );
                        tv.setText( info.toString() );
    
                        Date now = new Date();
                        long age = now.getTime()-metar.observationTime;
                        tv = (TextView) view.findViewById( R.id.wx_report_age );
                        tv.setVisibility( View.VISIBLE );
                        tv.setText( TimeUtils.formatDuration( age )+" old" );
                    } else {
                        tv = (TextView) view.findViewById( R.id.wx_station_name );
                        WxUtils.setColorizedWxDrawable( tv, metar, 0 );
                    }
                }
            }
        }

    }

    public class TabsAdapter extends FragmentPagerAdapter implements
            TitleProvider, ViewPager.OnPageChangeListener {

        private final Context mContext;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;
            private final String label;
            private Fragment fragment;

            TabInfo( String _label, Class<?> _class, Bundle _args ) {
                clss = _class;
                args = _args;
                label = _label;
                fragment = null;
            }
        }

        public TabsAdapter( FragmentActivity activity, ViewPager pager ) {
            super( activity.getSupportFragmentManager() );
            mContext = activity;
            mViewPager = pager;
            mViewPager.setAdapter( this );
        }

        public void addTab( String label, Class<?> clss, Bundle args ) {
            TabInfo info = new TabInfo( label, clss, args );
            mTabs.add( info );
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem( int position ) {
            TabInfo info = mTabs.get( position );
            if ( info.fragment == null ) {
                info.fragment = Fragment.instantiate( mContext, info.clss.getName(), info.args );
            }
            return info.fragment;
        }

        @Override
        public void onPageScrolled( int position, float positionOffset,
                int positionOffsetPixels ) {
        }

        @Override
        public void onPageSelected( int position ) {
            TabInfo info = mTabs.get( 1 );
            FavoriteWxFragment f = (FavoriteWxFragment) info.fragment;
            if ( info.fragment != null ) {
                if ( position == 1 ) {
                    f.startTask();
                } else {
                    f.stopTask();
                }
            }
        }

        @Override
        public void onPageScrollStateChanged( int state ) {
        }

        @Override
        public String getTitle( int position ) {
            TabInfo info = mTabs.get( position );
            return info.label;
        }

    }

}
