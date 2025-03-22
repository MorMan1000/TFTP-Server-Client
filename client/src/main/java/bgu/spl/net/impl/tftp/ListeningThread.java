package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

public class ListeningThread implements Runnable {
    private final MessagingProtocol<byte[]> protocol;
    private final MessageEncoderDecoder<byte[]> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;

    public ListeningThread(Socket sock, MessageEncoderDecoder<byte[]> reader, MessagingProtocol<byte[]> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { // just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && (read = in.read()) >= 0) {
                byte[] nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    byte[] packet = protocol.process(nextMessage);
                    if (packet != null) {
                        out.write(encdec.encode(packet));
                        out.flush();
                    }
                }
            }
            sock.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
