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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import org.petero.droidfish.R;
import android.content.Context;

/** Engine running as a process started from an external resource. */
public class ExternalEngine extends UCIEngineBase {
    protected final Context context;

    private File engineFileName;
    protected static final String intSfPath = "/data/data/org.petero.droidfish/internal_sf";
    private static final String exePath = "/data/data/org.petero.droidfish/engine.exe";
    private final Report report;
    private Process engineProc;
    private Thread exitThread;
    private Thread stdInThread;
    private Thread stdErrThread;
    private List<String> inLines;
    private boolean startedOk;

    public ExternalEngine(Context context, String engine, Report report) {
        this.context = context;
        this.report = report;
        engineFileName = new File(engine);
        engineProc = null;
        exitThread = null;
        stdInThread = null;
        stdErrThread = null;
        inLines = new LinkedList<String>();
        startedOk = false;
    }

    /** @inheritDoc */
    @Override
    protected void startProcess() {
        try {
            copyFile(engineFileName, new File(exePath));
            chmod(exePath);
            ProcessBuilder pb = new ProcessBuilder(exePath);
            engineProc = pb.start();

            exitThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        engineProc.waitFor();
                        if (!startedOk)
                            report.reportError(context.getString(R.string.failed_to_start_engine));
                        else {
                            report.reportError(context.getString(R.string.engine_terminated));
                        }
                    } catch (InterruptedException e) {
                    }
                }
            });
            exitThread.start();
            
            // Start a thread to read stdin
            stdInThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Process ep = engineProc;
                    if (ep == null)
                        return;
                    InputStream is = ep.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr, 8192);
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            if ((ep == null) || Thread.currentThread().isInterrupted())
                                return;
                            synchronized (inLines) {
                                inLines.add(line);
                                inLines.notify();
                                startedOk = true;
                            }
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            stdInThread.start();

            // Start a thread to ignore stderr
            stdErrThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[128];
                    while (true) {
                        Process ep = engineProc;
                        if ((ep == null) || Thread.currentThread().isInterrupted())
                            return;
                        try {
                            ep.getErrorStream().read(buffer);
                        } catch (IOException e) {
                            return;
                        }
                    }
                }
            });
            stdErrThread.start();
        } catch (IOException ex) {
            report.reportError(ex.getMessage());
        }
    }

    /** @inheritDoc */
    @Override
    public void initOptions() {
        setOption("Hash", 16);
    }

    /** @inheritDoc */
    @Override
    public void setStrength(int strength) {
    }

    /** @inheritDoc */
    @Override
    public String readLineFromEngine(int timeoutMillis) {
        try {
            synchronized (inLines) {
                if (inLines.size() == 0) {
                    Thread inThread = stdInThread;
                    if ((inThread == null) || !inThread.isAlive())
                        return null;
                    inLines.wait(timeoutMillis);
                }
            }
            synchronized (inLines) {
                if (inLines.size() > 0) {
                    String ret = inLines.get(0);
                    inLines.remove(0);
//                    System.out.printf("Engine -> GUI: %s\n", ret);
                    return ret;
                }
            }
        } catch (InterruptedException e) {
        }
        return "";
    }

    /** @inheritDoc */
    @Override
    public void writeLineToEngine(String data) {
//        System.out.printf("GUI -> Engine: %s\n", data);
        data += "\n";
        try {
            Process ep = engineProc;
            if (ep != null)
                ep.getOutputStream().write(data.getBytes());
        } catch (IOException e) {
        }
    }

    /** @inheritDoc */
    @Override
    public void shutDown() {
        if (exitThread != null)
            exitThread.interrupt();
        super.shutDown();
        if (engineProc != null)
            engineProc.destroy();
        engineProc = null;
        if (stdInThread != null)
            stdInThread.interrupt();
        if (stdErrThread != null)
            stdErrThread.interrupt();
    }

    protected void copyFile(File from, File to) throws IOException {
        new File(intSfPath).delete();
        if (to.exists() && (from.length() == to.length()) && (from.lastModified() == to.lastModified()))
            return;
        if (to.exists())
            to.delete();
        to.createNewFile();
        FileChannel inFC = null;
        FileChannel outFC = null;
        try {
            inFC = new FileInputStream(from).getChannel();
            outFC = new FileOutputStream(to).getChannel();
            long cnt = outFC.transferFrom(inFC, 0, inFC.size());
            if (cnt < inFC.size())
                throw new IOException("File copy failed");
        } finally {
            if (inFC != null) { try { inFC.close(); } catch (IOException ex) {} }
            if (outFC != null) { try { outFC.close(); } catch (IOException ex) {} }
            to.setLastModified(from.lastModified());
        }
    }

    private final void chmod(String exePath) throws IOException {
        Process proc = Runtime.getRuntime().exec(new String[]{"chmod", "744", exePath});
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            proc.destroy();
            throw new IOException("chmod failed");
        }
    }
}
