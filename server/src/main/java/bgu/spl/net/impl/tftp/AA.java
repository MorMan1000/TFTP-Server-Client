package bgu.spl.net.impl.tftp;

import java.io.File;

public class AA {
    // public static void main(String[] args) {

    public static void main(String[] args) {
        String name = "cat";
        File file = new File("Skeleton/server/Flies/" + name);
        System.out.println(System.getProperty("user.dir"));

    }

    public static byte[] shortToBytes(short num) {
        return new byte[] { (byte) (num >> 8), (byte) (num & 0xff) };
    }

}
