package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

class holder {
    static ConcurrentHashMap<Integer, Boolean> idsLogin = new ConcurrentHashMap<Integer, Boolean>();
    static ConcurrentHashMap<Integer, String> usersList = new ConcurrentHashMap<Integer, String>();
    static int userCounter = 0;

}

public class TftpProtocol implements MessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;
    private FileInputStream fileRead = null;
    private int blockCounter = 0;
    private byte[] fileNames = {};
    private static String path = "Skeleton/client/";

    @Override
    public byte[] process(byte[] message) {
        byte[] ans = null;
        if (message[1] == 3) {// data
            if (InputReader.printFiles) {
                short size = bytesToShort(new byte[] { message[2], message[3] });
                short block = bytesToShort(new byte[] { message[4], message[5] });
                message = Arrays.copyOfRange(message, 6, size + 6);
                if (block - 1 == blockCounter) {
                    fileNames = joinArrays(fileNames, message);
                    if (message.length < 512) {
                        int fileNameIndex = 0;
                        byte[] fileName = new byte[1 << 10];
                        for (int i = 0; i < fileNames.length; i++) {
                            if (fileNames[i] == 0) {
                                System.out.println(new String(fileName));
                                fileName = new byte[1 << 10];
                                fileNameIndex = 0;
                            } else {
                                fileName[fileNameIndex++] = fileNames[i];
                            }

                        }
                        System.out.println(new String(fileName));
                    }
                    blockCounter++;
                    ans = joinArrays(new byte[] { 0, 4 }, shortToBytes((short) blockCounter));
                    System.out.println("ACK" + blockCounter);
                    if (message.length < 512) {
                        blockCounter = 0;
                        InputReader.printFiles = false;
                        fileNames = new byte[0];
                    }
                }
            } else {
                FileOutputStream fileWrite;
                short block = bytesToShort(new byte[] { message[4], message[5] });
                if (block - 1 == blockCounter) {
                    try {
                        fileWrite = new FileOutputStream(path + InputReader.filename, true);
                        if (blockCounter == 0) {
                            System.out.println("file created");
                        }
                        System.out.println("Handling DATA packge number " + (blockCounter + 1));
                        byte[] dataMsg = removeExtra(message);
                        fileWrite.write(dataMsg);
                        fileWrite.close();
                        blockCounter++;
                        ans = joinArrays(new byte[] { 0, 4 }, shortToBytes((short) blockCounter));
                        if (dataMsg.length < 512) {// finish uploding a file
                            blockCounter = 0;
                            System.out.println("RRQ " + InputReader.filename + " complete");
                            return ans;
                        }
                        return ans;
                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                } else {
                    return ans;
                }
            }
        } else if (message[1] == 4) { // ACK
            short block = bytesToShort(new byte[] { message[2], message[3] });
            if (block == 0 && !InputReader.uploding) {
                if (InputReader.connected == false) {
                    shouldTerminate = true;
                }
                System.out.println(
                        "recived " + "{" + message[0] + "," + message[1] + "," + message[2] + "," + message[3] + "}");
                System.out.println("ACK " + blockCounter);
                return ans;
            } else if (block == blockCounter && InputReader.uploding) {
                try {
                    if (blockCounter == 0) {
                        System.out.println("File exists");
                    }
                    File file = new File(path + InputReader.filename);
                    if (fileRead == null)
                        fileRead = new FileInputStream(file);
                    int ch;
                    byte[] packet = new byte[1 << 10];
                    int packetSize = 0;
                    if (fileRead != null) {
                        System.out.println("Handling ACK");
                        while ((ch = fileRead.read()) != -1) {
                            if (packetSize < 511) {
                                packet[packetSize++] = (byte) ch;
                            } else {
                                packet[packetSize++] = (byte) ch;
                                blockCounter++;
                                byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) packetSize));
                                msg = joinArrays(msg, shortToBytes((short) blockCounter));
                                packet = deleteTrailingZeros(packet, packetSize);
                                packet = joinArrays(msg, packet);
                                packetSize = 0;
                                System.out.println("ACK " + (blockCounter - 1));
                                ans = packet;
                                return ans;
                            }
                        }
                        if (packetSize > 0) {
                            blockCounter++;
                            byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) packetSize));
                            msg = joinArrays(msg, shortToBytes((short) blockCounter));
                            packet = deleteTrailingZeros(packet, packetSize);
                            packet = joinArrays(msg, packet);
                            fileRead.close();
                            System.out.println("ACK " + (blockCounter - 1));
                            blockCounter = 0;
                            System.out.println("WRQ " + InputReader.filename + " completed");
                            ans = packet;
                            fileRead = null;
                            InputReader.uploding = false;
                            return ans;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (message[1] == 5) { // ERROR
            byte[] errMsg = Arrays.copyOfRange(message, 4, message.length - 1);
            short errNum = bytesToShort(new byte[] { message[2], message[3] });
            String errorToPrint = new String(errMsg);
            System.out.println("Handling ERROR");
            System.out.println("Error " + errNum + ": " + errorToPrint);
        } else if (message[1] == 9) { // BCAST
            int action = message[2];
            System.out.println("Handling BCAST");
            String fileName = new String(Arrays.copyOfRange(message, 3, message.length - 1));
            System.out.println("BCAST " + (action == 0 ? "Del " : "Add ") + fileName);
        }
        return ans;

    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public static byte[] joinArrays(byte[] arr1, byte[] arr2) {
        byte[] result = new byte[arr1.length + arr2.length];
        for (int i = 0; i < arr1.length; i++)
            result[i] = arr1[i];
        for (int i = 0; i < arr2.length; i++)
            result[i + arr1.length] = arr2[i];
        return result;
    }

    public void printError(byte errorCode, String msg) {
        byte[] error = new byte[] { 0, 5, 0, errorCode };
        error = joinArrays(error, msg.getBytes());
        error = joinArrays(error, new byte[] { 0 });
    }

    public static byte[] shortToBytes(short num) {
        return new byte[] { (byte) (num >> 8), (byte) (num & 0xff) };
    }

    public static short bytesToShort(byte[] bytes) {
        return (short) (((short) ((bytes[0]))) << 8 | (short) ((bytes[1]) & 0x00ff));
    }

    private static byte[] removeExtra(byte[] msg) {
        int size = bytesToShort(new byte[] { msg[2], msg[3] });
        byte[] ans = new byte[size];
        ans = Arrays.copyOfRange(msg, 6, 6 + size);
        return ans;
    }

    private static byte[] deleteTrailingZeros(byte[] msg, int size) {
        byte[] ans = Arrays.copyOfRange(msg, 0, size);
        return ans;
    }
}
