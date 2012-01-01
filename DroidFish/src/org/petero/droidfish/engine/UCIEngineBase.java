package org.petero.droidfish.engine;

import java.util.HashMap;

import org.petero.droidfish.engine.cuckoochess.CuckooChessEngine;

public abstract class UCIEngineBase implements UCIEngine {

    private boolean processAlive;

    public static UCIEngine getEngine(String engine, Report report) {
        if ("cuckoochess".equals(engine))
            return new CuckooChessEngine(report);
        else if ("stockfish".equals(engine))
            return new StockFishJNI();
        else
            return new ExternalEngine(engine, report);
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
