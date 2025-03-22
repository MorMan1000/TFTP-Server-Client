package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

public class InputReader implements Runnable {

  private final MessagingProtocol<byte[]> protocol;
  private final MessageEncoderDecoder<byte[]> encdec;
  private final Socket sock;
  public static String filename;
  public static volatile boolean connected = true;
  public static boolean uploding = false;
  public static boolean printFiles = false;
  private boolean disconnect = false;
  private boolean isLogged = false;
  private static String path = "Skeleton/client/";

  public InputReader(Socket sock, MessageEncoderDecoder<byte[]> reader, MessagingProtocol<byte[]> protocol) {
    this.sock = sock;
    this.encdec = reader;
    this.protocol = protocol;
  }

  @Override
  public void run() {
    try (Socket sock = this.sock) { // just for automatic closing
      String read;

      Scanner in = new Scanner(System.in);

      BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());

      while (!protocol.shouldTerminate()) {
        if (!disconnect) {
          read = in.nextLine();
          byte[] bytes = packet(read);
          if (bytes != null) {
            out.write(encdec.encode(bytes));
            out.flush();
          }
        }
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public byte[] packet(String s) {
    byte[] opcode;
    byte[] result = null;
    if (s.startsWith("LOGRQ ")) {
      isLogged = true;
      opcode = new byte[] { 0, 7 };
      result = s.substring(6).getBytes();
      result = joinArrays(opcode, result);
      result = joinArrays(result, new byte[] { 0 });
    } else if (s.startsWith("DELRQ ")) {
      opcode = new byte[] { 0, 8 };
      result = s.substring(6).getBytes();
      result = joinArrays(opcode, result);
      result = joinArrays(result, new byte[] { 0 });
    } else if (s.startsWith("RRQ ")) {
      opcode = new byte[] { 0, 1 };
      filename = s.substring(4);
      File file = new File(path + filename);
      if (!file.exists()) {
        result = s.substring(4).getBytes();
        result = joinArrays(opcode, result);
        result = joinArrays(result, new byte[] { 0 });
      } else {
        System.out.println("file already exists");
      }
    } else if (s.startsWith("WRQ ")) {
      opcode = new byte[] { 0, 2 };
      filename = s.substring(4);
      File file = new File(path + filename);
      if (file.exists()) {
        uploding = true;
        result = s.substring(4).getBytes();
        result = joinArrays(opcode, result);
        result = joinArrays(result, new byte[] { 0 });
      } else {
        System.out.println("file doesnt exists");
      }
    } else if (s.startsWith("DIRQ")) {
      printFiles = true;
      opcode = new byte[] { 0, 6 };
      result = opcode;
    } else if (s.startsWith("DISC")) {
      if (isLogged) {
        disconnect = true;
        connected = false;
      }
      opcode = new byte[] { 0, 10 };
      result = opcode;
    } else {
      System.out.println("Invalid command");
    }
    return result;
  }

  public static byte[] joinArrays(byte[] arr1, byte[] arr2) {
    byte[] result = new byte[arr1.length + arr2.length];
    for (int i = 0; i < arr1.length; i++)
      result[i] = arr1[i];
    for (int i = 0; i < arr2.length; i++)
      result[i + arr1.length] = arr2[i];
    return result;
  }
}
