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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.kalab.chess.enginesupport.ChessEngine;

import android.os.Build;

public class EngineUtil {
    static {
        System.loadLibrary("nativeutil");
    }

    /** Return file name of the internal stockfish executable. */
    public static String internalStockFishName() {
        String abi = Build.CPU_ABI;
        if (!"x86".equals(abi) &&
                !"x86_64".equals(abi) &&
                !"arm64-v8a".equals(abi)) {
            abi = "armeabi-v7a"; // Unknown ABI, assume 32-bit arm
        }
        return abi + "/stockfish";
    }

    /** Return true if file "engine" is a network engine. */
    public static boolean isNetEngine(String engine) {
        boolean netEngine = false;
        try {
            InputStream inStream = new FileInputStream(engine);
            InputStreamReader inFile = new InputStreamReader(inStream);
            char[] buf = new char[4];
            if ((inFile.read(buf) == 4) && "NETE".equals(new String(buf)))
                netEngine = true;
            inFile.close();
        } catch (IOException ignore) {
        }
        return netEngine;
    }

    public static final String openExchangeDir = "oex";

    /** Return true if file "engine" is an open exchange engine. */
    public static boolean isOpenExchangeEngine(String engine) {
        File parent = new File(engine).getParentFile();
        if (parent == null)
            return false;
        String parentDir = parent.getName();
        return openExchangeDir.equals(parentDir);
    }

    /** Return a filename (without path) representing an open exchange engine. */
    public static String openExchangeFileName(ChessEngine engine) {
        String ret = "";
        if (engine.getPackageName() != null)
            ret += sanitizeString(engine.getPackageName());
        ret += "-";
        if (engine.getFileName() != null)
            ret += sanitizeString(engine.getFileName());
        return ret;
    }

    /** Remove characters from s that are not safe to use in a filename. */
    private static String sanitizeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (((ch >= 'A') && (ch <= 'Z')) ||
                ((ch >= 'a') && (ch <= 'z')) ||
                ((ch >= '0') && (ch <= '9')))
                sb.append(ch);
            else
                sb.append('_');
        }
        return sb.toString();
    }

    /** Executes chmod 744 exePath. */
    static native boolean chmod(String exePath);

    /** Change the priority of a process. */
    static native void reNice(int pid, int prio);

    /** For synchronizing non thread safe native calls. */
    public static final Object nativeLock = new Object();
}
