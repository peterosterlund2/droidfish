/*
    DroidFish - An Android chess program.
    Copyright (C) 2012-2014  Peter Österlund, peterosterlund2@gmail.com

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import org.petero.droidfish.EngineOptions;
import org.petero.droidfish.R;
import org.petero.droidfish.Util;

import android.content.Context;

/** Engine running on a different computer. */
public class NetworkEngine extends UCIEngineBase {
    protected final Context context;
    private final Report report;

    private String fileName;
    private String networkID;
    private Socket socket;
    private Thread startupThread;
    private Thread stdInThread;
    private Thread stdOutThread;
    private LocalPipe guiToEngine;
    private LocalPipe engineToGui;
    private boolean startedOk;
    private boolean isRunning;
    private boolean isError;

    public NetworkEngine(Context context, String engine, EngineOptions engineOptions, Report report) {
        this.context = context;
        this.report = report;
        fileName = engine;
        networkID = engineOptions.networkID;
        startupThread = null;
        stdInThread = null;
        guiToEngine = new LocalPipe();
        engineToGui = new LocalPipe();
        startedOk = false;
        isRunning = false;
        isError = false;
    }

    /** Create socket connection to remote server. */
    private final synchronized void connect() {
        if (socket == null) {
            String host = null;
            String port = null;
            boolean ok = false;
            if (EngineUtil.isNetEngine(fileName)) {
                try {
                    String[] lines = Util.readFile(fileName);
                    if (lines.length >= 3) {
                        host = lines[1];
                        port = lines[2];
                        ok = true;
                    }
                } catch (IOException e1) {
                }
            }
            if (!ok) {
                isError = true;
                report.reportError(context.getString(R.string.network_engine_config_error));
            } else {
                try {
                    int portNr = Integer.parseInt(port);
                    socket = new Socket(host, portNr);
                    socket.setTcpNoDelay(true);
                } catch (UnknownHostException e) {
                    isError = true;
                    report.reportError(e.getMessage());
                } catch (NumberFormatException nfe) {
                    isError = true;
                    report.reportError(context.getString(R.string.invalid_network_port));
                } catch (IOException e) {
                    isError = true;
                    report.reportError(e.getMessage());
                }
            }
            if (socket == null)
                socket = new Socket();
        }
    }

    /** @inheritDoc */
    @Override
    protected void startProcess() {
        // Start thread to check for startup error
        startupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    return;
                }
                if (startedOk && isRunning && !isUCI) {
                    isError = true;
                    report.reportError(context.getString(R.string.uci_protocol_error));
                }
            }
        });
        startupThread.start();

        // Start a thread to read data from engine
        stdInThread = new Thread(new Runnable() {
            @Override
            public void run() {
                connect();
                try {
                    InputStream is = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr, 8192);
                    String line;
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        if (Thread.currentThread().isInterrupted())
                            return;
                        synchronized (engineToGui) {
                            engineToGui.addLine(line);
                            if (first) {
                                startedOk = true;
                                isRunning = true;
                                first = false;
                            }
                        }
                    }
                } catch (IOException e) {
                } finally {
                    if (isRunning) {
                        isError = true;
                        isRunning = false;
                        if (!startedOk)
                            report.reportError(context.getString(R.string.failed_to_start_engine));
                        else
                            report.reportError(context.getString(R.string.engine_terminated));
                    }
                    try { socket.close(); } catch (IOException e) {}
                }
                engineToGui.close();
            }
        });
        stdInThread.start();

        // Start a thread to write data to engine
        stdOutThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connect();
                    String line;
                    while ((line = guiToEngine.readLine()) != null) {
                        if (Thread.currentThread().isInterrupted())
                            return;
                        line += "\n";
                        socket.getOutputStream().write(line.getBytes());
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        isError = true;
                        report.reportError(e.getMessage());
                    }
                } finally {
                    if (isRunning && !isError) {
                        isError = true;
                        report.reportError(context.getString(R.string.engine_terminated));
                    }
                    isRunning = false;
                    try { socket.close(); } catch (IOException ex) {}
                }
            }
        });
        stdOutThread.start();
    }

    private int hashMB = -1;
    private String gaviotaTbPath = "";
    private String syzygyPath = "";
    private boolean optionsInitialized = false;

    /** @inheritDoc */
    @Override
    public void initOptions(EngineOptions engineOptions) {
        super.initOptions(engineOptions);
        hashMB = engineOptions.hashMB;
        setOption("Hash", engineOptions.hashMB);
        syzygyPath = engineOptions.getEngineRtbPath(true);
        setOption("SyzygyPath", syzygyPath);
        gaviotaTbPath = engineOptions.getEngineGtbPath(true);
        setOption("GaviotaTbPath", gaviotaTbPath);
        optionsInitialized = true;
    }

    @Override
    protected File getOptionsFile() {
        return new File(fileName + ".ini");
    }

    /** @inheritDoc */
    @Override
    public boolean optionsOk(EngineOptions engineOptions) {
        if (isError)
            return false;
        if (!optionsInitialized)
            return true;
        if (!networkID.equals(engineOptions.networkID))
            return false;
        if (hashMB != engineOptions.hashMB)
            return false;
        if (hasOption("gaviotatbpath") && !gaviotaTbPath.equals(engineOptions.getEngineGtbPath(true)))
            return false;
        if (hasOption("syzygypath") && !syzygyPath.equals(engineOptions.getEngineRtbPath(true)))
            return false;
        return true;
    }

    /** @inheritDoc */
    @Override
    public void setStrength(int strength) {
    }

    /** @inheritDoc */
    @Override
    public String readLineFromEngine(int timeoutMillis) {
        String ret = engineToGui.readLine(timeoutMillis);
        if (ret == null)
            return null;
        if (ret.length() > 0) {
//            System.out.printf("Engine -> GUI: %s\n", ret);
        }
        return ret;
    }

    /** @inheritDoc */
    @Override
    public void writeLineToEngine(String data) {
//        System.out.printf("GUI -> Engine: %s\n", data);
        guiToEngine.addLine(data);
    }

    /** @inheritDoc */
    @Override
    public void shutDown() {
        isRunning = false;
        if (startupThread != null)
            startupThread.interrupt();
        if (socket != null) {
            try { socket.getOutputStream().write("quit\n".getBytes()); } catch (IOException e) {}
            try { socket.close(); } catch (IOException e) {}
        }
        super.shutDown();
        if (stdOutThread != null)
            stdOutThread.interrupt();
        if (stdInThread != null)
            stdInThread.interrupt();
    }
}
