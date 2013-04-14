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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import org.petero.droidfish.EngineOptions;
import org.petero.droidfish.engine.cuckoochess.CuckooChessEngine;

import android.content.Context;

public abstract class UCIEngineBase implements UCIEngine {

    private boolean processAlive;
    private HashSet<String> allOptions;
    private HashMap<String, String> currOptions;
    protected boolean isUCI;

    public static UCIEngine getEngine(Context context, String engine,
                                      EngineOptions engineOptions, Report report) {
        if ("stockfish".equals(engine) && (EngineUtil.internalStockFishName() == null))
            engine = "cuckoochess";
        if ("cuckoochess".equals(engine))
            return new CuckooChessEngine(report);
        else if ("stockfish".equals(engine))
            return new InternalStockFish(context, report);
        else {
            boolean netEngine = false;
            try {
                InputStream inStream = new FileInputStream(engine);
                InputStreamReader inFile = new InputStreamReader(inStream);
                char[] buf = new char[4];
                if ((inFile.read(buf) == 4) && "NETE".equals(new String(buf)))
                    netEngine = true;
                inFile.close();
            } catch (IOException e) {
            }
            if (netEngine)
                return new NetworkEngine(context, engine, engineOptions, report);
            else
                return new ExternalEngine(context, engine, report);
        }
    }

    protected UCIEngineBase() {
        processAlive = false;
        allOptions = new HashSet<String>();
        currOptions = new HashMap<String, String>();
        isUCI = false;
    }

    protected abstract void startProcess();

    @Override
    public final void initialize() {
        if (!processAlive) {
            startProcess();
            processAlive = true;
        }
    }

    @Override
    public void initOptions(EngineOptions engineOptions) {
        isUCI = true;
    }

    @Override
    public void shutDown() {
        if (processAlive) {
            writeLineToEngine("quit");
            processAlive = false;
        }
    }

    @Override
    public void clearOptions() {
        allOptions.clear();
    }

    @Override
    public void registerOption(String optName) {
        allOptions.add(optName);
    }

    /** Return true if engine has option optName. */
    protected boolean haveOption(String optName) {
        return allOptions.contains(optName);
    }

    @Override
    public void setOption(String name, int value) {
        setOption(name, String.format(Locale.US, "%d", value));
    }

    @Override
    public void setOption(String name, boolean value) {
        setOption(name, value ? "true" : "false");
    }

    @Override
    public void setOption(String name, String value) {
        String lcName = name.toLowerCase(Locale.US);
        if (!allOptions.contains(lcName))
            return;
        String currVal = currOptions.get(lcName);
        if (value.equals(currVal))
            return;
        writeLineToEngine(String.format(Locale.US, "setoption name %s value %s", name, value));
        currOptions.put(lcName, value);
    }
}
