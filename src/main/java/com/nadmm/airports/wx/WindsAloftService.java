package com.nadmm.airports.wx;

import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

import com.nadmm.airports.utils.UiUtils;

import java.io.File;


public class WindsAloftService extends NoaaService {

    private final String FB_TEXT_PATH = "/data/products/nws/winds";

    private static final long FB_CACHE_MAX_AGE = DateUtils.HOUR_IN_MILLIS;


    public WindsAloftService() {
        super( "fb", FB_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_FB ) ) {
            String code = intent.getStringExtra( STATION_ID );
            int pos = code.lastIndexOf( "/" );
            File file = getDataFile( code.substring( pos+1 ) );
            if ( !file.exists() ) {
                try {
                    String path = FB_TEXT_PATH+code;
                    Log.d( "PATH", path );
                    fetch( AWC_HOST, path, null, file, false );
                } catch ( Exception e ) {
                    UiUtils.showToast( this, "Unable to fetch FB text: "+e.getMessage() );
                }
            }

            // Broadcast the result
            sendFileResultIntent( action, code, file );
        }
    }

}
