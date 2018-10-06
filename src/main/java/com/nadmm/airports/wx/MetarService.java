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

package com.nadmm.airports.wx;

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.UiUtils;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

public class MetarService extends NoaaService {

    private final String METAR_IMAGE_NAME = "sfc_%s.gif";
    private final String METAR_TEXT_QUERY = "datasource=metars&requesttype=retrieve"
    		+ "&hoursBeforeNow=%d&mostRecentForEachStation=constraint"
            + "&format=xml&compression=gzip&stationString=%s";
    private final String METAR_IMAGE_PATH = "/adds/data/metars/";

    private static final long METAR_CACHE_MAX_AGE = 30*DateUtils.MINUTE_IN_MILLIS;

    private MetarParser mParser;

    public MetarService() {
        super( "metar", METAR_CACHE_MAX_AGE );
        mParser = new MetarParser();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_METAR ) || action.equals( ACTION_CACHE_METAR ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_TEXT ) ) {
                // Get request parameters
                ArrayList<String> stationIds = intent.getStringArrayListExtra( STATION_IDS );
                int hours = intent.getIntExtra( HOURS_BEFORE, 3 );
                boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
                boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

                if ( action.equals( ACTION_CACHE_METAR ) ) {
                    // Do not try to use the cache. We are populating the cache.
                    cacheOnly = false;
                }

                if ( forceRefresh || !cacheOnly ) {
                    ArrayList<String> missing = new ArrayList<>();
                    for ( String stationId : stationIds ) {
                        if ( forceRefresh|| !getObjFile( stationId ).exists() ) {
                            missing.add( stationId );
                        }
                    }

                    if ( !missing.isEmpty() ) {
                        StringBuilder param = new StringBuilder();
                        for ( String stationId : missing ) {
                            if ( param.length() > 0 ) {
                                param.append( "," );
                            }
                            param.append( stationId );
                        }

                        File tmpFile = null;
                        try {
                            tmpFile = File.createTempFile( "metar", null );
                            String query = String.format( Locale.US, METAR_TEXT_QUERY, hours, param );
                            fetchFromNoaa( query, tmpFile, true );
                            parseMetars( tmpFile, missing );
                        } catch ( Exception e ) {
                            UiUtils.showToast( this, "Unable to fetch METAR: "+e.getMessage() );
                        } finally {
                            if ( tmpFile != null ) {
                                tmpFile.delete();
                            }
                        }
                    }
                }

                if ( action.equals( ACTION_GET_METAR ) ) {
                    for ( String stationId : stationIds ) {
                        File objFile = getObjFile( stationId );
                        Metar metar;
                        if ( objFile.exists() ) {
                            metar = (Metar) readObject( objFile );
                        } else {
                            metar = new Metar();
                        }

                        // Broadcast the result
                        sendSerializableResultIntent( action, stationId, metar );
                    }
                }
            } else if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE ).toLowerCase();
                String imageName = String.format( METAR_IMAGE_NAME, code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = METAR_IMAGE_PATH+imageName;
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch METAR image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                sendImageResultIntent( action, code, imageFile );
            }
        }
    }

    private void parseMetars( File xmlFile, ArrayList<String> stationIds )
            throws ParserConfigurationException, SAXException, IOException {
        if ( xmlFile.exists() ) {
            ArrayList<Metar> metars = mParser.parse( xmlFile, stationIds );
            for ( Metar metar : metars ) {
                File objFile = getObjFile( metar.stationId );
                writeObject( metar, objFile );
            }
        }
    }

    private File getObjFile( String stationId ) {
        return getDataFile( "METAR_"+stationId+".obj" );
    }

}
