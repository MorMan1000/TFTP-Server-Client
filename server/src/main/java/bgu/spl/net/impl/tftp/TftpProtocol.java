package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

class holder {
    static ConcurrentHashMap<Integer, Boolean> idsLogin = new ConcurrentHashMap<Integer, Boolean>();
    static ConcurrentHashMap<Integer, String> usersList = new ConcurrentHashMap<Integer, String>();
    static int userCounter = 0;
    static Vector<String> uploadingFiles = new Vector<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<byte[]> connections;
    private String fileName;
    private File[] filesDir = null;
    private int fileNumForDirq = 0;
    private FileInputStream fileRead;
    private int blockCounter = 0;
    private int lastFileRemaining = 0;
    private static String path = "Skeleton/server/Flies/";

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {

        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(byte[] message) {
        if (message[1] == 7) {// log
            byte[] message2 = Arrays.copyOfRange(message, 2, message.length - 1);
            String str = readBytes(message2);
            if (!holder.idsLogin.containsKey(connectionId) && !holder.usersList.containsValue(str)) {
                holder.idsLogin.put(connectionId, true);
                holder.usersList.put(holder.userCounter, str);// return ACK
                holder.userCounter++;
                byte[] ack = new byte[] { 0, 4, 0, 0 };
                connections.send(this.connectionId, ack);
            } else {
                String errorOut = "User already logged in – Login username already connected.";
                sendError((byte) 7, errorOut);
            }
        } else if (holder.idsLogin.containsKey(connectionId)) {
            if (message[1] == 8) {// delete
                byte[] message2 = Arrays.copyOfRange(message, 2, message.length - 1);
                String fname = readBytes(message2);
                File toDelte = new File(path + fname);
                if (!toDelte.exists() || holder.uploadingFiles.contains(toDelte.getName())) {
                    String errorOut = "File Not Found - RRQ DELRQ of non-existing file.";
                    sendError((byte) 1, errorOut);
                } else {
                    toDelte.delete(); // check sync
                    byte[] ack = new byte[] { 0, 4, 0, 0 };
                    connections.send(this.connectionId, ack);
                    for (Integer k : holder.idsLogin.keySet()) {
                        connections.send(k, delProcces(fname.getBytes()));
                    }

                }
            } else if (message[1] == 1) {// read
                byte[] message2 = Arrays.copyOfRange(message, 2, message.length - 1);
                String name = readBytes(message2);
                try {
                    File file = new File(path + name);
                    if (file.exists()) {
                        fileRead = new FileInputStream(file);

                        int ch;
                        byte[] packet = new byte[1 << 10];
                        blockCounter = 0;
                        int packetSize = 0;
                        boolean fullPacket = false;
                        while (!fullPacket && (ch = fileRead.read()) >= 0) {
                            if (packetSize < 511) {
                                packet[packetSize++] = (byte) ch;
                            } else {
                                packet[packetSize++] = (byte) ch;
                                blockCounter++;
                                byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) packetSize));
                                msg = joinArrays(msg, shortToBytes((short) blockCounter));
                                packet = deleteTrailingZeros(packet, packetSize);
                                packet = joinArrays(msg, packet);
                                connections.send(this.connectionId, packet);
                                packetSize = 0;
                                fullPacket = true;
                            }
                        }
                        if (packetSize > 0) {
                            blockCounter++;
                            byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) (packetSize)));
                            msg = joinArrays(msg, shortToBytes((short) blockCounter));
                            packet = deleteTrailingZeros(packet, packetSize);
                            packet = joinArrays(msg, packet);
                            connections.send(this.connectionId, packet);
                            fileRead.close();// In the moment there is send the client send rrq complete
                            fileRead = null;
                        }
                    } else {
                        String errorOut = "File Not Found - RRQ DELRQ of non-existing file.";
                        sendError((byte) 1, errorOut);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (message[1] == 2) { // write
                byte[] message2 = Arrays.copyOfRange(message, 2, message.length - 1);
                fileName = readBytes(message2);
                try {
                    File file = new File(path + fileName);
                    if (!file.exists()) {// need to send ack back to client
                        holder.uploadingFiles.add(fileName);
                        byte[] ack = new byte[] { 0, 4, 0, 0 };
                        connections.send(this.connectionId, ack);
                    } else {
                        String errorOut = "File already exists – File name exists on WRQ.";
                        sendError((byte) 5, errorOut);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (message[1] == 3) {// data
                FileOutputStream fileWrite;
                short block = bytesToShort(new byte[] { message[4], message[5] });
                if (block - 1 == blockCounter) {
                    try {
                        fileWrite = new FileOutputStream(path + fileName, true);
                        byte[] dataMsg = removeExtra(message);
                        fileWrite.write(dataMsg);
                        fileWrite.close();
                        blockCounter++;
                        byte[] ack = joinArrays(new byte[] { 0, 4 }, shortToBytes((short) blockCounter));
                        connections.send(this.connectionId, ack);
                        if (dataMsg.length < 512) {// finish uploding a file
                            blockCounter = 0;
                            holder.uploadingFiles.remove(fileName);
                            for (Integer k : holder.idsLogin.keySet()) {
                                connections.send(k, addProcces(fileName.getBytes()));
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String errorOut = "Not defined, see error message (if any).";
                    sendError((byte) 0, errorOut);
                }

            } else if (message[1] == 6) { // diRQ
                File dir = new File(path);
                filesDir = dir.listFiles();
                byte[] packet = new byte[1 << 10];
                int packetSize = 0;
                int remaining = 0;
                for (int i = 0; i < filesDir.length; i++) {
                    if (holder.uploadingFiles.contains(filesDir[i].getName()))
                        continue;
                    byte[] fileNameBytes = filesDir[i].getName().getBytes();
                    if (packetSize + fileNameBytes.length <= 512) {
                        packet = deleteTrailingZeros(packet, packetSize);
                        packet = joinArrays(packet, fileNameBytes);
                        packet = joinArrays(packet, new byte[] { 0 });
                        packetSize = packetSize + fileNameBytes.length + 1;
                    } else {
                        remaining = 512 - packetSize;
                        if (remaining > 0) {
                            lastFileRemaining = fileNameBytes.length - remaining;
                            packet = joinArrays(packet, Arrays.copyOfRange(fileNameBytes, 0, remaining));
                        }
                        blockCounter++;
                        byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) (packetSize + remaining)));
                        msg = joinArrays(msg, shortToBytes((short) blockCounter));
                        packet = joinArrays(msg, packet);
                        connections.send(this.connectionId, packet);
                        packetSize = 0;
                        fileNumForDirq = i;
                        break;
                    }
                }

                if (packetSize > 0) {
                    blockCounter++;
                    byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) (packetSize - 1)));
                    msg = joinArrays(msg, shortToBytes((short) blockCounter));
                    packet = joinArrays(msg, Arrays.copyOfRange(packet, 0, packet.length - 1));
                    connections.send(this.connectionId, packet);
                    blockCounter = 0;
                    filesDir = null;
                    fileNumForDirq = 0;
                    lastFileRemaining = 0;
                }

            } else if (message[1] == 4) { // ACK
                try {
                    if (filesDir != null) {
                        byte[] packet = new byte[1 << 10];
                        int packetSize = 0;
                        int remaining = 0;
                        for (int i = fileNumForDirq; i < filesDir.length; i++) {
                            if (holder.uploadingFiles.contains(filesDir[i].getName()))
                                continue;
                            byte[] fileNameBytes = filesDir[i].getName().getBytes();
                            if (lastFileRemaining > 0) {
                                remaining = fileNameBytes.length - lastFileRemaining;
                                fileNameBytes = Arrays.copyOfRange(fileNameBytes, remaining, fileNameBytes.length);
                                lastFileRemaining = 0;
                            }
                            if (packetSize + fileNameBytes.length <= 512) {
                                packet = deleteTrailingZeros(packet, packetSize);
                                packet = joinArrays(packet, fileNameBytes);
                                packet = joinArrays(packet, new byte[] { 0 });
                                packetSize = packetSize + fileNameBytes.length + 1;
                            } else {
                                remaining = 512 - packetSize;
                                if (remaining > 0) {
                                    lastFileRemaining = fileNameBytes.length - remaining;
                                    packet = joinArrays(packet, Arrays.copyOfRange(fileNameBytes, 0, remaining));
                                }
                                blockCounter++;
                                byte[] msg = joinArrays(new byte[] { 0, 3 },
                                        shortToBytes((short) (packetSize + remaining)));
                                msg = joinArrays(msg, shortToBytes((short) blockCounter));
                                packet = joinArrays(msg, packet);
                                connections.send(this.connectionId, packet);
                                packetSize = 0;
                                fileNumForDirq = i;
                                break;
                            }
                        }
                        if (packetSize > 0) {
                            blockCounter++;
                            byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) (packetSize - 1)));
                            msg = joinArrays(msg, shortToBytes((short) blockCounter));
                            packet = joinArrays(msg, Arrays.copyOfRange(packet, 0, packet.length - 1));
                            connections.send(this.connectionId, packet);
                            blockCounter = 0;
                            filesDir = null;
                            fileNumForDirq = 0;
                            lastFileRemaining = 0;
                        }

                    } else if (fileRead != null) {
                        int ch;
                        byte[] packet = new byte[1 << 10];
                        int packetSize = 0;
                        boolean fullPacket = false;
                        while (!fullPacket && (ch = fileRead.read()) >= 0) {
                            if (packetSize < 511) {
                                packet[packetSize++] = (byte) ch;
                            } else {
                                packet[packetSize++] = (byte) ch;
                                blockCounter++;
                                byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) packetSize));
                                msg = joinArrays(msg, shortToBytes((short) blockCounter));
                                packet = deleteTrailingZeros(packet, packetSize);
                                packet = joinArrays(msg, packet);
                                connections.send(this.connectionId, packet);
                                packetSize = 0;
                                fullPacket = true;
                            }
                        }
                        if (packetSize > 0) {
                            blockCounter++;
                            byte[] msg = joinArrays(new byte[] { 0, 3 }, shortToBytes((short) packetSize));
                            msg = joinArrays(msg, shortToBytes((short) blockCounter));
                            packet = deleteTrailingZeros(packet, packetSize);
                            packet = joinArrays(msg, packet);
                            connections.send(this.connectionId, packet);
                            fileRead.close();
                            fileRead = null;
                            blockCounter = 0;
                        }
                    } else {
                        blockCounter = 0;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (message[1] == 10) { // DISC
                shouldTerminate = true;
                byte[] ack = new byte[] { 0, 4, 0, 0 };
                connections.send(this.connectionId, ack);
                disconnect();
            } else {
                String errorOut = "Illegal TFTP operation – Unknown Opcode.";
                sendError((byte) 4, errorOut);
            }
        } else {
            String errorOut = "User not logged in – Any opcode received before Login completes.";
            sendError((byte) 6, errorOut);
        }
    }

    private byte[] addProcces(byte[] message) {
        byte[] bcast = new byte[] { 0, 9, 1 };
        bcast = joinArrays(bcast, message);
        bcast = joinArrays(bcast, new byte[] { 0 });
        return bcast;

    }

    private byte[] delProcces(byte[] message) {
        byte[] bcast = new byte[] { 0, 9, 0 };
        bcast = joinArrays(bcast, message);
        bcast = joinArrays(bcast, new byte[] { 0 });
        return bcast;

    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void disconnect() {
        holder.idsLogin.remove(this.connectionId);
        holder.userCounter--;
        holder.usersList.remove(this.connectionId);
        this.connections.disconnect(this.connectionId);
    }

    public static byte[] joinArrays(byte[] arr1, byte[] arr2) {
        byte[] result = new byte[arr1.length + arr2.length];
        for (int i = 0; i < arr1.length; i++)
            result[i] = arr1[i];
        for (int i = 0; i < arr2.length; i++)
            result[i + arr1.length] = arr2[i];
        return result;
    }

    public void sendError(byte errorCode, String msg) {
        byte[] error = new byte[] { 0, 5, 0, errorCode };
        error = joinArrays(error, msg.getBytes());
        error = joinArrays(error, new byte[] { 0 });
        connections.send(this.connectionId, error);
    }

    public static byte[] shortToBytes(short num) {
        return new byte[] { (byte) (num >> 8), (byte) (num & 0xff) };
    }

    public static short bytesToShort(byte[] bytes) {
        return (short) (((short) ((bytes[0]))) << 8 | (short) ((bytes[1]) & 0x00ff));
    }

    private static String readBytes(byte[] bytes) {
        int end = 0;
        while (bytes[end] != 0) {
            end++;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
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
