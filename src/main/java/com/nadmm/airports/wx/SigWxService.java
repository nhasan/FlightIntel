package com.nadmm.airports.wx;

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.UiUtils;

import java.io.File;

public class SigWxService extends NoaaService {

    private final String SIGWX_IMAGE_NAME = "ll_%s_cl_new.gif";
    private final String SIGWX_IMAGE_PATH = "/data/products/swl/";

    private final static long SIGWX_CACHE_MAX_AGE = 180*DateUtils.MINUTE_IN_MILLIS;

    public SigWxService() {
        super( "sigwx", SIGWX_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_SIGWX ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format( SIGWX_IMAGE_NAME, code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = SIGWX_IMAGE_PATH+imageName;
                        fetch( AWC_HOST, path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch SIGWX image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                sendImageResultIntent( action, code, imageFile );
            }
        }
    }

}
