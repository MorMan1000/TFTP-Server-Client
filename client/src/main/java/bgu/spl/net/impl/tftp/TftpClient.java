package bgu.spl.net.impl.tftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

public class TftpClient {

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            args = new String[] { "localhost", "hello" };
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }

        // BufferedReader and BufferedWriter automatically using UTF-8 encoding
        try (Socket sock = new Socket(args[0], 7777);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            MessageEncoderDecoder<byte[]> encDec = new TftpEncoderDecoder();
            MessagingProtocol<byte[]> protocol = new TftpProtocol();
            InputReader inputReader = new InputReader(sock, encDec, protocol);
            Thread listener = new Thread(new ListeningThread(sock, encDec, protocol));
            listener.start();
            inputReader.run();
            System.out.println("closing the connection");
        }
    }
}
