package uk.co.borconi.emil.obd2aa;


import static uk.co.borconi.emil.obd2aa.services.HackerService.bytesToHex;

import androidx.annotation.NonNull;

import com.google.protobuf.GeneratedMessageV3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import uk.co.borconi.emil.obd2aa.helpers.Log;
import uk.co.borconi.emil.obd2aa.sslhelpers.NioSSL;


public class AAMessage {

    public final static byte CONTROLCHANNEL = 0;


    public final static byte FLAG_FIRST = 1;
    public final static byte FLAG_MIDDLE = 0;
    public final static byte FLAG_LAST = 2;
    public final static byte FLAG_ALL = 3; //ToDo check usage!


    public final static byte FLAG_CONTROL = 4; //ToDo check usage!
    public final static byte FLAG_SPECIFIC = 0;

    public final static byte FLAG_ENCRYPTED = 0x08;
    public final static byte FLAG_PLAIN = 0;

    public byte channel;
    public Integer messageID = null;
    public byte[] data;
    byte flag;
    short enc_len;
    int payload;
    byte encrypted = FLAG_ENCRYPTED;
    byte type = FLAG_SPECIFIC;


    public AAMessage(byte[] read, NioSSL nioSSL) throws IOException {

        this.channel = read[0];
        byte flag = read[1];
        this.encrypted = (byte) (flag & FLAG_ENCRYPTED);
        this.type = (byte) (flag & FLAG_CONTROL);
        this.flag = (byte) (flag & FLAG_ALL);

        this.enc_len = (short) ((read[2] & 0xFF) << 8 | (read[3] & 0xFF));
        // Log.d("AAMessage", "Channel: " + channel+", Flag: " + flag+", encryptyed = " + encrypted + "len: "+ enc_len + "raw: " + Util.bytesToHex(read));


        if (this.flag == FLAG_FIRST)
            payload = ((read[4] & 0xFF) << 24 | (read[5] & 0xFF) << 16 | (read[6] & 0xFF) << 8 | (read[7] & 0xFF));


        if (this.flag == FLAG_FIRST && this.encrypted == FLAG_ENCRYPTED)
            data = nioSSL.decrypt(read, 8, enc_len);
        else if (this.encrypted == FLAG_ENCRYPTED)
            data = nioSSL.decrypt(read, 4, enc_len);
        else
            data = Arrays.copyOfRange(read, 4, 4 + enc_len);


        if (data.length > 0 && this.flag != FLAG_MIDDLE && this.flag != FLAG_LAST) {
            this.messageID = ((data[0] & 0xFF) << 8 | (data[1] & 0xFF));
            trim();

        }


    }

    public AAMessage(byte channel, byte flag, int messageID) {
        this.channel = channel;
        this.flag = flag;
        this.messageID = messageID;
    }

    public AAMessage(byte channel, byte flag, short enc_len, short payload, int messageID, byte[] data) {
        this.channel = channel;
        this.flag = flag;
        this.enc_len = enc_len;
        this.payload = payload;
        this.messageID = messageID;
        this.data = data;
    }

    public AAMessage(byte channel, byte flag, int messageID, byte[] toByteArray) {
        this.channel = channel;
        this.flag = flag;
        this.messageID = messageID;
        this.data = toByteArray;
        this.encrypted = FLAG_ENCRYPTED;
    }

    public byte getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(byte encrypted) {
        this.encrypted = encrypted;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getChannel() {
        return channel;
    }

    public void setChannel(byte channel) {
        this.channel = channel;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public short getEnc_len() {
        return enc_len;
    }

    public void setEnc_len(short enc_len) {
        this.enc_len = enc_len;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(short payload) {
        this.payload = payload;
    }

    public int getMessageID() {
        return messageID;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(GeneratedMessageV3.Builder msg) {
        this.data = msg.build().toByteArray();

    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void trim() {
        data = Arrays.copyOfRange(data, 2, data.length);
    }

    public byte[] getFormated(NioSSL ssl) {
        ByteBuffer formated;
        if (this.encrypted == FLAG_ENCRYPTED)
            try {
                formated = ByteBuffer.allocate(16384);
                //Log.d("AAMSG", "Data to be encrypted: " + data.length + " messageid: " + messageID + ",Channel: " + channel + ", hex: " + bytesToHex(data));
                formated.put(channel);
                formated.put((byte) (flag + encrypted + type));
                ssl.encrypt(data, messageID, payload, formated);
                formated.flip();
                byte[] ba = new byte[formated.remaining()];
                formated.get(ba);
                //  Log.d("AAMSG","Encrypted: "+bytesToHex(ba));
                return ba;
            } catch (Exception e) {
                return null;
            }
        else {
            //if (messageID>0)

            formated = ByteBuffer.allocate(data.length + 6);
            //else
            //  formated=ByteBuffer.allocate(data.length+4);
            formated.put(channel);
            formated.put((byte) (flag + type));
            formated.put((byte) (data.length + 2 >> 8 & 0xFF));
            formated.put((byte) (data.length + 2 & 0xFF));
            //if (messageID>0)
            //{
            formated.put((byte) (messageID >> 8 & 0xFF));
            formated.put((byte) (messageID & 0xFF));
            //}

            formated.put(data);
            Log.d("AAMSG", "Not encrypted: " + bytesToHex(formated.array()));
            return formated.array();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Channel: " + channel + ", flag: " + flag + ", messageid: " + messageID + ", payload: " + payload + ", data: " + bytesToHex(data);
    }
}