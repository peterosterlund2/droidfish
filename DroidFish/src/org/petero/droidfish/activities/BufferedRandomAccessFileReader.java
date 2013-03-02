/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2013  Peter Österlund, peterosterlund2@gmail.com

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
package org.petero.droidfish.activities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

final class BufferedRandomAccessFileReader {
    RandomAccessFile f;
    byte[] buffer = new byte[8192];
    long bufStartFilePos = 0;
    int bufLen = 0;
    int bufPos = 0;

    BufferedRandomAccessFileReader(String fileName) throws FileNotFoundException {
        f = new RandomAccessFile(fileName, "r");
    }
    final long length() throws IOException {
        return f.length();
    }
    final long getFilePointer() throws IOException {
        return bufStartFilePos + bufPos;
    }
    final void close() throws IOException {
        f.close();
    }

    private final static int EOF = -1024;

    final String readLine() throws IOException {
        // First handle the common case where the next line is entirely
        // contained in the buffer
        for (int i = bufPos; i < bufLen; i++) {
            byte b = buffer[i];
            if ((b == '\n') || (b == '\r')) {
                String line = new String(buffer, bufPos, i - bufPos);
                for ( ; i < bufLen; i++) {
                    b = buffer[i];
                    if ((b != '\n') && (b != '\r')) {
                        bufPos = i;
                        return line;
                    }
                }
                break;
            }
        }

        // Generic case
        byte[] lineBuf = new byte[8192];
        int lineLen = 0;
        int b;
        while (true) {
            b = getByte();
            if (b == '\n' || b == '\r' || b == EOF)
                break;
            lineBuf[lineLen++] = (byte)b;
            if (lineLen >= lineBuf.length)
                break;
        }
        while (true) {
            b = getByte();
            if ((b != '\n') && (b != '\r')) {
                if (b != EOF)
                    bufPos--;
                break;
            }
        }
        if ((b == EOF) && (lineLen == 0))
            return null;
        else
            return new String(lineBuf, 0, lineLen);
    }

    private final int getByte() throws IOException {
        if (bufPos >= bufLen) {
            bufStartFilePos = f.getFilePointer();
            bufLen = f.read(buffer);
            bufPos = 0;
            if (bufLen <= 0)
                return EOF;
        }
        return buffer[bufPos++];
    }
}
