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
