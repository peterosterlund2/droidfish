/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

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

public class StockFishJNI extends UCIEngineBase {
    static {
        System.loadLibrary("stockfishjni");
    }

    /** @inheritDoc */
    @Override
    public final void initOptions() {
        setOption("Hash", 16);
    }

    /** @inheritDoc */
    @Override
    public final void setStrength(int strength) {
        setOption("Skill Level", strength/50);
    }

    /** @inheritDoc */
    @Override
    public final String readLineFromEngine(int timeoutMillis) {
        String ret = readFromProcess(timeoutMillis);
        if (ret == null)
            return null;
        if (ret.length() > 0) {
//          System.out.printf("Engine -> GUI: %s\n", ret);
        }
        return ret;
    }

    /** @inheritDoc */
    @Override
    public final synchronized void writeLineToEngine(String data) {
//      System.out.printf("GUI -> Engine: %s\n", data);
        writeToProcess(data + "\n");
    }

    /** @inheritDoc */
    @Override
    protected final native void startProcess();

    /**
     * Read a line of data from the process.
     * Return as soon as there is a full line of data to return,
     * or when timeoutMillis milliseconds have passed.
     */
    private final native String readFromProcess(int timeoutMillis);

    /** Write data to the process. */
    private final native void writeToProcess(String data);

    /** Return number of physical processors, i.e. hyper-threading ignored. */
    final static native int getNPhysicalProcessors();
}
