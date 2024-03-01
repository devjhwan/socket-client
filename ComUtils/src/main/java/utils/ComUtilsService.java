package utils;

import java.io.*;

public class ComUtilsService {
    private ComUtils comUtils;

    public ComUtilsService(InputStream inputStream, OutputStream outputStream) throws IOException {
        comUtils = new ComUtils(inputStream, outputStream);
    }

    public void writeTest() {
        try {
            String sendString = "This is a test";
            int strLen = sendString.length();
            comUtils.write_int32(strLen);
            comUtils.write_string(sendString);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public String readTest() {
        String result = "";
        try {
            int strSize = comUtils.read_int32();
            result = comUtils.read_string(strSize);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    public void write_char() {
        try {
            comUtils.write_char('a');
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public char read_char() {
        char result = 0;
        try {
            result = comUtils.read_char();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

}
