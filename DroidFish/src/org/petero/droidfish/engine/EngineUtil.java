/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2012  Peter Österlund, peterosterlund2@gmail.com

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

import android.os.Build;

public class EngineUtil {
    static {
        System.loadLibrary("nativeutil");
    }

    /** Return number of physical processors, i.e. hyper-threading ignored. */
    final static native int getNPhysicalProcessors();

    private static final class CpuAbi {
        static final String get() { return Build.CPU_ABI; }
    }

    /** Return file name of the internal stockfish executable,
     * or null if the internal stockfish engine is not supported. */
    public static String internalStockFishName() {
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        if (sdkVersion < 4)
            return "stockfish15.mygz";
        String abi = CpuAbi.get();
        if (!abi.equals("x86") && !abi.equals("armeabi-v7a"))
            abi = "armeabi"; // Unknown ABI, assume original ARM
        return "stockfish-" + abi;
    }

    /** Executes chmod 744 exePath. */
    final static native boolean chmod(String exePath);
}
