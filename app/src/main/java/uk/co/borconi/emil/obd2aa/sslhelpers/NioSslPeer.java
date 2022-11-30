package uk.co.borconi.emil.obd2aa.sslhelpers;


import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import uk.co.borconi.emil.obd2aa.helpers.Log;
import uk.co.borconi.emil.obd2aa.streams.InOutStream;


/**
 * A class that represents an SSL/TLS peer, and can be extended to create a client or a server.
 * <p/>
 * It makes use of the JSSE framework, and specifically the {@link SSLEngine} logic, which
 * is described by Oracle as "an advanced API, not appropriate for casual use", since
 * it requires the user to implement much of the communication establishment procedure himself.
 * More information about it can be found here: http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLEngine
 * <p/>
 * {@link NioSslPeer} implements the handshake protocol, required to establish a connection between two peers,
 * which is common for both client and server and provides the abstract  and
 * methods, that need to be implemented by the specific SSL/TLS peer
 * that is going to extend this class.
 *
 * @author <a href="mailto:alex.a.karnezis@gmail.com">Alex Karnezis</a>
 */
public abstract class NioSslPeer {


    private static final String TAG = "AAGateWay";
    /**
     * Will be used to execute tasks that may emerge during handshake in parallel with the server's main thread.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected InOutStream inoutStram;
    /**
     * Will contain this peer's application data in plaintext, that will be later encrypted
     * using {@link SSLEngine#wrap(ByteBuffer, ByteBuffer)} and sent to the other peer. This buffer can typically
     * be of any size, as long as it is large enough to contain this peer's outgoing messages.
     * If this peer tries to send a message bigger than buffer's capacity a {@link BufferOverflowException}
     * will be thrown.
     */
    ByteBuffer myAppData = ByteBuffer.allocate(4 * 16384);
    /**
     * Will contain this peer's encrypted data, that will be generated after {@link SSLEngine#wrap(ByteBuffer, ByteBuffer)}
     * is applied on {@link NioSslPeer#myAppData}. It should be initialized using {@link SSLSession#getPacketBufferSize()},
     * which returns the size up to which, SSL/TLS packets will be generated from the engine under a session.
     * All SSLEngine network buffers should be sized at least this large to avoid insufficient space problems when performing wrap and unwrap calls.
     * All SSLEngine network buffers should be sized at least this large to avoid insufficient space problems when performing wrap and unwrap calls.
     */
    ByteBuffer myNetData = ByteBuffer.allocate(4 * 16384);
    /**
     * Will contain the other peer's (decrypted) application data. It must be large enough to hold the application data
     * from any peer. Can be initialized with {@link SSLSession#getApplicationBufferSize()} for an estimation
     * of the other peer's application data and should be enlarged if this size is not enough.
     */
    ByteBuffer peerAppData = ByteBuffer.allocate(4 * 16384);
    /**
     * Will contain the other peer's encrypted data. The SSL/TLS protocols specify that implementations should produce packets containing at most 16 KB of plaintext,
     * so a buffer sized to this value should normally cause no capacity problems. However, some implementations violate the specification and generate large records up to 32 KB.
     * If the {@link SSLEngine#unwrap(ByteBuffer, ByteBuffer)} detects large inbound packets, the buffer sizes returned by SSLSession will be updated dynamically, so the this peer
     * should check for overflow conditions and enlarge the buffer using the session's (updated) buffer size.
     */
    ByteBuffer peerNetData = ByteBuffer.allocate(4 * 16384);

    /**
     * Implements the handshake protocol between two peers, required for the establishment of the SSL/TLS connection.
     * During the handshake, encryption configuration information - such as the list of available cipher suites - will be exchanged
     * and if the handshake is successful will lead to an established SSL/TLS session.
     * <p>
     * <p/>
     * A typical handshake will usually contain the following steps:
     *
     * <ul>
     *   <li>1. wrap:     ClientHello</li>
     *   <li>2. unwrap:   ServerHello/Cert/ServerHelloDone</li>
     *   <li>3. wrap:     ClientKeyExchange</li>
     *   <li>4. wrap:     ChangeCipherSpec</li>
     *   <li>5. wrap:     Finished</li>
     *   <li>6. unwrap:   ChangeCipherSpec</li>
     *   <li>7. unwrap:   Finished</li>
     * </ul>
     * <p/>
     * Handshake is also used during the end of the session, in order to properly close the connection between the two peers.
     * A proper connection close will typically include the one peer sending a CLOSE message to another, and then wait for
     * the other's CLOSE message to close the transport link. The other peer from his perspective would read a CLOSE message
     * from his peer and then enter the handshake procedure to send his own CLOSE message as well.
     *
     * @param engine - the engine that will be used for encryption/decryption of the data exchanged with the other peer.
     * @return True if the connection handshake was successful or false if an error occurred.
     * @throws IOException - if an error occurs during read/write to the socket channel.
     */
    boolean doHandshake(SSLEngine engine) throws Exception {

        Log.d(TAG, "About to do handshake...");
        Log.d(TAG, engine.getHandshakeStatus().toString());

        SSLEngineResult result;
        Log.d(TAG, "Line 1");
        HandshakeStatus handshakeStatus;
        Log.d(TAG, "Line 2");
        // NioSslPeer's fields myAppData and peerAppData are supposed to be large enough to hold all message data the peer
        // will send and expects to receive from the other peer respectively. Since the messages to be exchanged will usually be less
        // than 16KB long the capacity of these fields should also be smaller. Here we initialize these two local buffers
        // to be used for the handshake, while keeping client's buffers at the same size.

        int counter = 0;
        Log.d(TAG, "Line 3");

        Log.d(TAG, "Line 4");

        Log.d(TAG, "Line 5");

        Log.d(TAG, "Line 6");

        myNetData.clear();
        peerNetData.clear();

        Log.d(TAG, "Line 7");

        Log.d(TAG, "Line 8");
        handshakeStatus = engine.getHandshakeStatus();
        Log.d(TAG, "Line 9");
        Log.d(TAG, "Before the while: " + (handshakeStatus != HandshakeStatus.FINISHED && handshakeStatus != HandshakeStatus.NOT_HANDSHAKING));


        while (handshakeStatus != HandshakeStatus.FINISHED && handshakeStatus != HandshakeStatus.NOT_HANDSHAKING) {
            byte[] buffer;
            Log.d(TAG, handshakeStatus.toString());
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    counter++;
                    Log.d(TAG, "Got here...");
                    buffer = inoutStram.read();
                    peerAppData.clear();
                    Log.d(TAG, "Read data amount: " + buffer.length);
                    if (buffer.length < 0) {
                        Log.d(TAG, "No data....");
                        if (engine.isInboundDone() && engine.isOutboundDone()) {
                            return false;
                        }
                        try {
                            engine.closeInbound();
                        } catch (SSLException e) {
                            throw new Exception("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
                        }
                        engine.closeOutbound();
                        // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }


                    peerNetData.put(buffer, 6, buffer.length - 6);
                    Log.d(TAG, "before data flipped...");
                    peerNetData.flip();
                    Log.d(TAG, "data flipped...");
                    try {
                        do {
                            result = engine.unwrap(peerNetData, peerAppData);
                            Log.d(TAG, "data unwrapped...");
                            Log.d(TAG, "Handskes is: " + result.getHandshakeStatus().toString() + " Current Status: " + result.getStatus() + " Bytes consumed: " + result.bytesConsumed() + " bytes produce: " + result.bytesProduced());
                        } while (peerNetData.hasRemaining() || result.bytesProduced() > 0);
                        peerNetData.compact();


                        Log.d(TAG, "data compacted...");
                        handshakeStatus = result.getHandshakeStatus();
                        Log.d(TAG, "Handshake status: " + handshakeStatus);
                    } catch (SSLException sslException) {
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        throw new Exception("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection..." + sslException.getMessage());
                    }
                    switch (result.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_OVERFLOW:
                            // Will occur when peerAppData's capacity is smaller than the data derived from peerNetData's unwrap.
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                            break;
                        case BUFFER_UNDERFLOW:
                            // Will occur either when no data was read from the peer or when the peerNetData buffer was too small to hold all peer's data.
                            peerNetData = handleBufferUnderflow(engine, peerNetData);
                            break;
                        case CLOSED:
                            if (engine.isOutboundDone()) {
                                return false;
                            } else {
                                engine.closeOutbound();
                                handshakeStatus = engine.getHandshakeStatus();
                                break;
                            }
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_WRAP:
                    myNetData.clear();
                    Log.d(TAG, "Enetering need wrap, remaining: " + myAppData.remaining());
                    try {
                        myAppData.flip();
                        result = engine.wrap(myAppData, myNetData);
                        Log.d(TAG, "Got a result" + myAppData.toString());
                        myAppData.compact();
                        handshakeStatus = result.getHandshakeStatus();
                        Log.d(TAG, "Handskes is: " + result.getHandshakeStatus().toString() + " Current Status: " + result.getStatus() + " Bytes consumed: " + result.bytesConsumed() + " bytes produce: " + result.bytesProduced() + " bytes remaining: " + myNetData.remaining());
                    } catch (SSLException sslException) {
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        throw new Exception("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                    }
                    switch (result.getStatus()) {
                        case OK:
                            Log.d(TAG, "Case WRAP, OK");
                            myNetData.flip();

                            //  while (myNetData.hasRemaining()) {
                            byte[] arr = new byte[myNetData.remaining() + 6];
                            myNetData.get(arr, 6, myNetData.remaining());
                            arr[0] = 0;
                            arr[1] = 3;
                            arr[2] = (byte) ((arr.length - 4) / 256);
                            arr[3] = (byte) ((arr.length - 4) % 256);
                            arr[4] = 0;
                            arr[5] = 3;


                            inoutStram.write(arr);


                            // }
                            break;
                        case BUFFER_OVERFLOW:
                            Log.d(TAG, "Case WRAP,OverFlow");
                            // Will occur if there is not enough space in myNetData buffer to write all the data that would be generated by the method wrap.
                            // Since myNetData is set to session's packet size we should not get to this point because SSLEngine is supposed
                            // to produce messages smaller or equal to that, but a general handling would be the following:
                            myNetData = enlargePacketBuffer(engine, myNetData);
                            break;
                        case BUFFER_UNDERFLOW:
                            throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                        case CLOSED:
                            try {
                                Log.d(TAG, "Before WRAP FLIP");
                                myNetData.flip();
                                Log.d(TAG, "After WRAP FLIP");
                                while (myNetData.hasRemaining()) {
                                    Log.d(TAG, myNetData.toString());
                                    arr = new byte[myNetData.remaining()];
                                    myNetData.get(arr);
                                    inoutStram.write(arr);
                                }
                                // At this point the handshake status will probably be NEED_UNWRAP so we make sure that peerNetData is clear to read.
                                peerNetData.clear();
                            } catch (Exception e) {
                                handshakeStatus = engine.getHandshakeStatus();
                                throw new Exception("Failed to send server's CLOSE message due to socket channel's failure.");

                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_TASK:
                    Log.d(TAG, "Need task");
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        executor.execute(task);
                    }
                    handshakeStatus = engine.getHandshakeStatus();
                    break;
                case FINISHED:
                    Log.d(TAG, "Handsake status is finished, we are done with the handshake");
                    break;
                case NOT_HANDSHAKING:
                    break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
            }
        }
        Log.d(TAG, "Handshake completed");
        return true;

    }

    protected ByteBuffer enlargePacketBuffer(SSLEngine engine, ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
    }

    protected ByteBuffer enlargeApplicationBuffer(SSLEngine engine, ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
    }

    /**
     * Compares <code>sessionProposedCapacity<code> with buffer's capacity. If buffer's capacity is smaller,
     * returns a buffer with the proposed capacity. If it's equal or larger, returns a buffer
     * with capacity twice the size of the initial one.
     *
     * @param buffer                  - the buffer to be enlarged.
     * @param sessionProposedCapacity - the minimum size of the new buffer, proposed by {@link SSLSession}.
     * @return A new buffer with a larger capacity.
     */
    protected ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
        if (sessionProposedCapacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(sessionProposedCapacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    /**
     * Handles {@link SSLEngineResult.Status#BUFFER_UNDERFLOW}. Will check if the buffer is already filled, and if there is no space problem
     * will return the same buffer, so the client tries to read again. If the buffer is already filled will try to enlarge the buffer either to
     * session's proposed size or to a larger capacity. A buffer underflow can happen only after an unwrap, so the buffer will always be a
     * peerNetData buffer.
     *
     * @param buffer - will always be peerNetData buffer.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @return The same buffer if there is no space problem or a new buffer with the same data but more space.
     * @throws Exception
     */
    protected ByteBuffer handleBufferUnderflow(SSLEngine engine, ByteBuffer buffer) {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
            return buffer;
        } else {
            ByteBuffer replaceBuffer = enlargePacketBuffer(engine, buffer);
            buffer.flip();
            replaceBuffer.put(buffer);
            return replaceBuffer;
        }
    }

    /**
     * This method should be called when this peer wants to explicitly close the connection
     * or when a close message has arrived from the other peer, in order to provide an orderly shutdown.
     * <p/>
     * It first calls {@link SSLEngine#closeOutbound()} which prepares this peer to send its own close message and
     * sets {@link SSLEngine} to the <code>NEED_WRAP</code> state. Then, it delegates the exchange of close messages
     * to the handshake method and finally, it closes socket channel.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine        - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    protected void closeConnection(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        engine.closeOutbound();
        //doHandshake(socketChannel, engine);
        socketChannel.close();
    }

    /**
     * In addition to orderly shutdowns, an unorderly shutdown may occur, when the transport link (socket channel)
     * is severed before close messages are exchanged. This may happen by getting an -1 or {@link IOException}
     * when trying to read from the socket channel, or an {@link IOException} when trying to write to it.
     * In both cases {@link SSLEngine#closeInbound()} should be called and then try to follow the standard procedure.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */


}