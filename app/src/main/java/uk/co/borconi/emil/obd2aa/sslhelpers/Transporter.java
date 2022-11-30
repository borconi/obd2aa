package uk.co.borconi.emil.obd2aa.sslhelpers;

import java.io.IOException;

public interface Transporter {
    int read(byte[] data) throws IOException;

    void write(byte[] data) throws IOException;
}
