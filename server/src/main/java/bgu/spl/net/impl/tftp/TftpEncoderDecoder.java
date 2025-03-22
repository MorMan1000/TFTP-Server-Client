package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private byte[] bytes = new byte[1 << 10]; // start with 1k
    private int len = 0;
    private byte opCode;
    private int counter = 0;
    private int bytesCounter = 0;
    private int size = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if (bytesCounter == 0) {
            bytes = new byte[1 << 10];
            opCode = 0;
            len = 0;
            counter = 0;
        }
        if (bytesCounter == 1) {
            opCode = nextByte;
        }
        if (opCode == 6) {
            pushByte(nextByte);
            bytesCounter = 0;
            return bytes;
        }
        if (opCode == 10) {
            pushByte(nextByte);
            bytesCounter = 0;
            return bytes;
        }

        if ((opCode == 1 | opCode == 2 | opCode == 7 | opCode == 8 | opCode == 9 | opCode == 5) && nextByte == 0
                && bytesCounter > 2) {
            pushByte(nextByte);
            bytesCounter = 0;
            return bytes;
        }
        if (opCode == 3) {
            if (bytesCounter == 4)
                size = TftpProtocol.bytesToShort(new byte[] { bytes[2], bytes[3] });
            if (bytesCounter > 6) {
                counter++;
                if (counter == size - 1) {
                    pushByte(nextByte);
                    bytesCounter = 0;
                    return bytes;
                }
            }
        }
        if (opCode == 4 && bytesCounter == 3) {
            pushByte(nextByte);
            bytesCounter = 0;
            return bytes;
        }

        pushByte(nextByte);
        bytesCounter++;
        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

}