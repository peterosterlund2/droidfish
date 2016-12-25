/*
    DroidFish - An Android chess program.
    Copyright (C) 2012-2013,2016  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.engine;

import java.util.LinkedList;
import java.util.Locale;

/** Implements line-based text communication between threads. */
public class LocalPipe {
    private LinkedList<String> lines = new LinkedList<String>();
    private boolean closed = false;

    /** Write a line to the pipe. */
    public final synchronized void printLine(String format) {
        String s = String.format(Locale.US, format, new Object[]{});
        addLine(s);
    }

    /** Write a line to the pipe. */
    public final synchronized void printLine(String format, Object ... args) {
        String s = String.format(Locale.US, format, args);
        addLine(s);
    }

    public final synchronized void addLine(String line) {
        while (lines.size() > 10000) {
            try {
                wait(10);
            } catch (InterruptedException e) {
            }
        }
        lines.add(line);
        notify();
    }

    /** Read a line from the pipe. Returns null on failure. */
    public final synchronized String readLine() {
        return readLine(-1);
    }

    /** Read a line from the pipe. Returns null on failure. Returns empty string on timeout. */
    public final synchronized String readLine(int timeoutMillis) {
        if (closed)
            return null;
        try {
            if (lines.isEmpty()) {
                if (timeoutMillis > 0)
                    wait(timeoutMillis);
                else
                    wait();
            }
            if (lines.isEmpty())
                return closed ? null : "";
            String ret = lines.get(0);
            lines.remove(0);
            return ret;
        } catch (InterruptedException e) {
            return null;
        }
    }

    /** Close pipe. Makes readLine() return null. */
    public final synchronized void close() {
        closed = true;
        notify();
    }

    /** Return true if writer side has closed the pipe. */
    public final synchronized boolean isClosed() {
        return closed;
    }
}
