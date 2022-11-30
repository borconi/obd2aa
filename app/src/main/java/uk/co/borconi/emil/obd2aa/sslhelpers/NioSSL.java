package uk.co.borconi.emil.obd2aa.sslhelpers;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import uk.co.borconi.emil.obd2aa.helpers.Log;
import uk.co.borconi.emil.obd2aa.streams.InOutStream;

/*
import static gb.xxy.hr.TransporterService.m_usb_ep_in;
import static gb.xxy.hr.TransporterService.m_usb_ep_out;*/


public class NioSSL extends NioSslPeer {

    private static final String TAG = "AAGateWay";
    private final ExecutorService decryptThread = Executors.newSingleThreadExecutor();
    private final SSLEngine engine;
    /**
     * Declares if the server is active to serve and create new connections.
     */
    private boolean active;
    /**
     * The context will be initialized with a specific SSL/TLS protocol and will then be used
     * to create {@link SSLEngine} classes for each new connection that arrives to the server.
     */
    private SSLContext context;
    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    private Selector selector;


    /**
     * Server is designed to apply an SSL/TLS protocol and listen to an IP address and port.
     *
     * @throws Exception
     */


    public NioSSL(SSLEngine engine, InOutStream stream, boolean client) throws Exception {
        this.engine = engine;
        this.inoutStram = stream;
        engine.setUseClientMode(client);
        engine.beginHandshake();
        Log.d("AAGateWay", "Begin handshake");
        doHandshake(engine);
    }


    public synchronized byte[] decrypt(byte[] encoded_data, int pos, int i) throws IOException {

        ByteBuffer inData = ByteBuffer.allocate(i);
        inData.put(encoded_data, pos, i);
        inData.flip();

        //peerNetData.clear();
        // peerNetData.put(encoded_data, pos, i);
        // peerNetData.flip();

        byte[] response = null;
        while (inData.hasRemaining()) {
            peerAppData.clear();
            SSLEngineResult result = engine.unwrap(inData, peerAppData);

            switch (result.getStatus()) {
                case OK:
                    peerAppData.flip();
                    response = new byte[peerAppData.remaining()];
                    peerAppData.get(response);

                    break;
                case BUFFER_OVERFLOW:
                    Log.d(TAG, "Buffer underflow, we need more network data");
                    peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                    break;
                case BUFFER_UNDERFLOW:
                    inData = handleBufferUnderflow(engine, inData);
                    break;
                case CLOSED:
                    Log.d("AA_GATEWAY_SERVER", "Client wants to close connection...");
                    // closeConnection(socketChannel, engine);
                    Log.d("AA_GATEWAY_SERVER", "Goodbye client!");
                    return response;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

        return response;
    }


    //Must be synchronized, otherwise SSLENgine might fail on multi-thread.
    public synchronized void encrypt(byte[] message, Integer messageID, int payload, ByteBuffer formated) throws Exception {


        myAppData.clear();
        if (messageID != null) {
            myAppData.put((byte) (messageID >> 8 & 0xFF));
            myAppData.put((byte) (messageID & 0xFF));
        }
        myAppData.put(message);
        myAppData.flip();
        myNetData.clear();


        SSLEngineResult result = engine.wrap(myAppData, myNetData);


        switch (result.getStatus()) {
            case OK:
                myNetData.flip();
                short coded = (short) myNetData.remaining();
                formated.put((byte) (coded >> 8 & 0xFF));
                formated.put((byte) (coded & 0xFF));
                if (payload != 0) {

                    formated.put((byte) (payload >> 24 & 0xFF));
                    formated.put((byte) (payload >> 16 & 0xFF));
                    formated.put((byte) (payload >> 8 & 0xFF));
                    formated.put((byte) (payload & 0xFF));
                }
                formated.put(myNetData.array(), 0, coded);
                break;
            case BUFFER_OVERFLOW:
                myNetData = enlargePacketBuffer(engine, myNetData);
                break;
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
            case CLOSED:
                //closeConnection(socketChannel, engine);
                Log.e(TAG, "Connection is closed");

            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
        }


    }

}
