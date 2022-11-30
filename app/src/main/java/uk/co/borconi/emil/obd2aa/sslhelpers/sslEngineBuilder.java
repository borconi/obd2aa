package uk.co.borconi.emil.obd2aa.sslhelpers;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

/**
 * Created by Emil on 25/03/2018.
 */

public class sslEngineBuilder {

    public static SSLEngine Builder(Context context) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException, KeyManagementException {
        InputStream openRawResource = context.getResources().openRawResource(context.getResources().getIdentifier("keystore", "raw", context.getPackageName()));
        KeyStore instance = KeyStore.getInstance("PKCS12");
        instance.load(openRawResource, "aa".toCharArray());
        KeyManagerFactory instance2 = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        instance2.init(instance, "aa".toCharArray());
        Log.d("SSL", "Before creating ssl context");

        SSLContext instance3 = SSLContext.getInstance("TLSv1.2");
        Log.d("SSL", "Alfter creating ssl context");
        instance3.init(instance2.getKeyManagers(), new TrustManager[]{new adb()}, new SecureRandom());
        Log.d("SSL", "After init");
        SSLEngine createSSLEngine = instance3.createSSLEngine();
        Log.d("SSL", "After create engine");
        createSSLEngine.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.1"});
        Log.d("SSL", "After enable protocol");
        createSSLEngine.setNeedClientAuth(true);
        return createSSLEngine;

    }
}
