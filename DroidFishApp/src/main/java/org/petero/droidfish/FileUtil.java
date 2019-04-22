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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class FileUtil {
    /**
     * Read a text file. Return string array with one string per line.
     */
    public static String[] readFile(String filename) throws FileNotFoundException {
        return readFromStream(new FileInputStream(filename)).split("\n");
    }

    /**
     * Read all data from an input stream. Return null if IO error.
     */
    public static String readFromStream(InputStream is) {
        // http://stackoverflow.com/a/5445161
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Read data from input stream and write to file.
     */
    public static void writeFile(InputStream is, String outFile) throws IOException {
        OutputStream os = new FileOutputStream(outFile);
        try {
            byte[] buffer = new byte[16384];
            while (true) {
                int len = is.read(buffer);
                if (len <= 0)
                    break;
                os.write(buffer, 0, len);
            }
        } finally {
            os.close();
        }
    }

    /**
     * Return the length of a file, or -1 if length can not be determined.
     */
    public static long getFileLength(String filename) {
        try {
            RandomAccessFile raf = new RandomAccessFile(filename, "r");
            try {
                return raf.length();
            } finally {
                raf.close();
            }
        } catch (IOException ex) {
            return -1;
        }
    }
}
