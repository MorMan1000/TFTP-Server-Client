package bgu.spl.net.impl.echo;

public class EchoServer {

    public static void main(String[] args) {

        // // you can use any server...
        // Server.threadPerClient(
        // 7777, // port
        // () -> new EchoProtocol(), // protocol factory
        // LineMessageEncoderDecoder::new // message encoder decoder factory
        // ).serve();

    }
}
