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

import java.util.HashMap;

import org.petero.droidfish.engine.cuckoochess.CuckooChessEngine;

import android.content.Context;

public abstract class UCIEngineBase implements UCIEngine {

    private boolean processAlive;

    public static UCIEngine getEngine(Context context, String engine, Report report) {
        if ("cuckoochess".equals(engine))
            return new CuckooChessEngine(report);
        else if ("stockfish".equals(engine))
            return new InternalStockFish(context, report);
        else
            return new ExternalEngine(context, engine, report);
    }

    protected UCIEngineBase() {
        processAlive = false;
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
    public void shutDown() {
        if (processAlive) {
            writeLineToEngine("quit");
            processAlive = false;
        }
    }

    @Override
    public void setOption(String name, int value) {
        setOption(name, String.format("%d", value));
    }

    @Override
    public void setOption(String name, boolean value) {
        setOption(name, value ? "true" : "false");
    }

    private HashMap<String, String> options = new HashMap<String, String>();

    @Override
    public void setOption(String name, String value) {
        String currVal = options.get(name.toLowerCase());
        if (value.equals(currVal))
            return;
        writeLineToEngine(String.format("setoption name %s value %s", name, value));
        options.put(name.toLowerCase(), value);
    }
}
