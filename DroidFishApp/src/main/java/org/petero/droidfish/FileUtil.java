/*
    DroidFish - An Android chess program.
    Copyright (C) 2016  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class FileUtil {
    /** Read a text file. Return string array with one string per line. */
    public static String[] readFile(String filename) throws IOException {
        ArrayList<String> ret = new ArrayList<>();
        try (InputStream inStream = new FileInputStream(filename);
             InputStreamReader inFile = new InputStreamReader(inStream, "UTF-8");
             BufferedReader inBuf = new BufferedReader(inFile)) {
            String line;
            while ((line = inBuf.readLine()) != null)
                ret.add(line);
            return ret.toArray(new String[0]);
        }
    }

    /** Read all data from an input stream. Return null if IO error. */
    public static String readFromStream(InputStream is) {
        try (InputStreamReader isr = new InputStreamReader(is, "UTF-8");
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Read data from input stream and write to file. */
    public static void writeFile(InputStream is, String outFile) throws IOException {
        try (OutputStream os = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[16384];
            while (true) {
                int len = is.read(buffer);
                if (len <= 0)
                    break;
                os.write(buffer, 0, len);
            }
        }
    }

    /** Return the length of a file, or -1 if length can not be determined. */
    public static long getFileLength(String filename) {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            return raf.length();
        } catch (IOException ex) {
            return -1;
        }
    }
}
