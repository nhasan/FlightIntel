package com.nadmm.airports.wx;

import android.content.Intent;

public class SigWxFragment extends WxMapFragmentBase {

    private static final String[] sSigWxCodes = new String[] {
        "12_0",
        "18_0",
        "00_0",
        "06_0",
        "00_1",
        "06_1",
        "12_1",
        "18_1"
    };

    private static final String[] sSigWxNames = new String[] {
        "12 hr SIGWX Prognosis (0000 UTC)",
        "12 hr SIGWX Prognosis (0600 UTC)",
        "12 hr SIGWX Prognosis (1200 UTC)",
        "12 hr SIGWX Prognosis (1800 UTC)",
        "24 hr SIGWX Prognosis (0000 UTC)",
        "24 hr SIGWX Prognosis (0600 UTC)",
        "24 hr SIGWX Prognosis (1200 UTC)",
        "24 hr SIGWX Prognosis (1800 UTC)",
    };

    public SigWxFragment() {
        super( NoaaService.ACTION_GET_SIGWX, sSigWxCodes, sSigWxNames );
        setTitle( "SIGWX Images" );
        setLabel( "Select SigWx Image" );
    }

    @Override
    protected Intent getServiceIntent() {
        return new Intent( getActivity(), SigWxService.class );
    }

}
