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

import org.petero.droidfish.EGTBOptions;
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
    private Thread startupThread;
    private Thread exitThread;
    private Thread stdInThread;
    private Thread stdErrThread;
    private LocalPipe inLines;
    private boolean startedOk;
    private boolean isRunning;

    public ExternalEngine(Context context, String engine, Report report) {
        this.context = context;
        this.report = report;
        engineFileName = new File(engine);
        engineProc = null;
        startupThread = null;
        exitThread = null;
        stdInThread = null;
        stdErrThread = null;
        inLines = new LocalPipe();
        startedOk = false;
        isRunning = false;
    }

    /** @inheritDoc */
    @Override
    protected void startProcess() {
        try {
            copyFile(engineFileName, new File(exePath));
            chmod(exePath);
            ProcessBuilder pb = new ProcessBuilder(exePath);
            engineProc = pb.start();

            startupThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (startedOk && isRunning && !isUCI)
                        report.reportError(context.getString(R.string.uci_protocol_error));
                }
            });
            startupThread.start();

            exitThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        engineProc.waitFor();
                        isRunning = false;
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
                        boolean first = true;
                        while ((line = br.readLine()) != null) {
                            if ((ep == null) || Thread.currentThread().isInterrupted())
                                return;
                            synchronized (inLines) {
                                inLines.addLine(line);
                                if (first) {
                                    startedOk = true;
                                    isRunning = true;
                                    first = false;
                                }
                            }
                        }
                    } catch (IOException e) {
                    }
                    inLines.close();
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
                            int len = ep.getErrorStream().read(buffer, 0, 1);
                            if (len < 0)
                                break;
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
    public void initOptions(EGTBOptions egtbOptions) {
        super.initOptions(egtbOptions);
        setOption("Hash", 16);
        if (egtbOptions.engineProbe) {
            setOption("GaviotaTbPath", egtbOptions.gtbPath);
            setOption("GaviotaTbCache", 8);
        }
    }

    /** @inheritDoc */
    @Override
    public void setStrength(int strength) {
    }

    /** @inheritDoc */
    @Override
    public String readLineFromEngine(int timeoutMillis) {
        String ret = inLines.readLine(timeoutMillis);
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
        if (startupThread != null)
            startupThread.interrupt();
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
