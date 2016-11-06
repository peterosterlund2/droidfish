package org.petero.droidfish;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class FileUtil {
    /** Read a text file. Return string array with one string per line. */
    public static String[] readFile(String filename) throws IOException {
        ArrayList<String> ret = new ArrayList<String>();
        InputStream inStream = new FileInputStream(filename);
        InputStreamReader inFile = new InputStreamReader(inStream);
        BufferedReader inBuf = new BufferedReader(inFile);
        String line;
        while ((line = inBuf.readLine()) != null)
            ret.add(line);
        inBuf.close();
        return ret.toArray(new String[ret.size()]);
    }

    /** Read all data from an input stream. Return null if IO error. */
    public static String readFromStream(InputStream is) {
        InputStreamReader isr;
        try {
            isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            br.close();
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
