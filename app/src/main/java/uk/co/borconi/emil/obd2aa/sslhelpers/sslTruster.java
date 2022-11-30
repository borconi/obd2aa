package uk.co.borconi.emil.obd2aa.sslhelpers;


/**
 * Created by Emil on 25/03/2018.
 */

import android.util.Log;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

public class sslTruster implements X509TrustManager {
    public final void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {
        Log.d("NaiveTrustManager", "Check client: " + Arrays.toString(paramArrayOfX509Certificate));
    }

    public final void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {
        Log.d("NaiveTrustManager", "Check server: " + Arrays.toString(paramArrayOfX509Certificate));
    }

    public final X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}


