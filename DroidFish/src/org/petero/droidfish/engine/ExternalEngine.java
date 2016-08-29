/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2014  Peter Österlund, peterosterlund2@gmail.com

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

import org.petero.droidfish.EngineOptions;
import org.petero.droidfish.R;
import android.content.Context;

/** Engine running as a process started from an external resource. */
public class ExternalEngine extends UCIEngineBase {
    protected final Context context;

    private File engineFileName;
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

    protected String internalSFPath() {
        return context.getFilesDir().getAbsolutePath() + "/internal_sf";
    }

    /** @inheritDoc */
    @Override
    protected void startProcess() {
        try {
            File exeDir = new File(context.getFilesDir(), "engine");
            exeDir.mkdir();
            String exePath = copyFile(engineFileName, exeDir);
            chmod(exePath);
            cleanUpExeDir(exeDir, exePath);
            ProcessBuilder pb = new ProcessBuilder(exePath);
            synchronized (EngineUtil.nativeLock) {
                engineProc = pb.start();
            }
            reNice();

            startupThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
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
                        Process ep = engineProc;
                        if (ep != null)
                            ep.waitFor();
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

    /** Try to lower the engine process priority. */
    private void reNice() {
        try {
            java.lang.reflect.Field f = engineProc.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            int pid = f.getInt(engineProc);
            EngineUtil.reNice(pid, 10);
        } catch (Throwable t) {
        }
    }

    /** Remove all files except exePath from exeDir. */
    private void cleanUpExeDir(File exeDir, String exePath) {
        try {
            exePath = new File(exePath).getCanonicalPath();
            File[] files = exeDir.listFiles();
            if (files == null)
                return;
            for (File f : files) {
                if (!f.getCanonicalPath().equals(exePath))
                    f.delete();
            }
            new File(context.getFilesDir(), "engine.exe").delete();
        } catch (IOException e) {
        }
    }

    private int hashMB = -1;
    private String gaviotaTbPath = "";
    private String syzygyPath = "";
    private boolean optionsInitialized = false;

    /** @inheritDoc */
    @Override
    public void initOptions(EngineOptions engineOptions) {
        super.initOptions(engineOptions);
        hashMB = getHashMB(engineOptions);
        setOption("Hash", hashMB);
        syzygyPath = engineOptions.getEngineRtbPath(false);
        setOption("SyzygyPath", syzygyPath);
        gaviotaTbPath = engineOptions.getEngineGtbPath(false);
        setOption("GaviotaTbPath", gaviotaTbPath);
        optionsInitialized = true;
    }

    @Override
    protected File getOptionsFile() {
        return new File(engineFileName.getAbsolutePath() + ".ini");
    }

    /** Reduce too large hash sizes. */
    private final static int getHashMB(EngineOptions engineOptions) {
        int hashMB = engineOptions.hashMB;
        if (hashMB > 16 && !engineOptions.unSafeHash) {
            int maxMem = (int)(Runtime.getRuntime().maxMemory() / (1024*1024));
            if (maxMem < 16)
                maxMem = 16;
            if (hashMB > maxMem)
                hashMB = maxMem;
        }
        return hashMB;
    }

    /** @inheritDoc */
    @Override
    public boolean optionsOk(EngineOptions engineOptions) {
        if (!optionsInitialized)
            return true;
        if (hashMB != getHashMB(engineOptions))
            return false;
        if (hasOption("gaviotatbpath") && !gaviotaTbPath.equals(engineOptions.getEngineGtbPath(false)))
            return false;
        if (hasOption("syzygypath") && !syzygyPath.equals(engineOptions.getEngineRtbPath(false)))
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
            if (ep != null) {
                ep.getOutputStream().write(data.getBytes());
                ep.getOutputStream().flush();
            }
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

    protected String copyFile(File from, File exeDir) throws IOException {
        File to = new File(exeDir, "engine.exe");
        new File(internalSFPath()).delete();
        if (to.exists() && (from.length() == to.length()) && (from.lastModified() == to.lastModified()))
            return to.getAbsolutePath();
        if (to.exists())
            to.delete();
        to.createNewFile();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(from);
            FileChannel inFC = fis.getChannel();
            fos = new FileOutputStream(to);
            FileChannel outFC = fos.getChannel();
            long cnt = outFC.transferFrom(inFC, 0, inFC.size());
            if (cnt < inFC.size())
                throw new IOException("File copy failed");
        } finally {
            if (fis != null) { try { fis.close(); } catch (IOException ex) {} }
            if (fos != null) { try { fos.close(); } catch (IOException ex) {} }
            to.setLastModified(from.lastModified());
        }
        return to.getAbsolutePath();
    }

    private final void chmod(String exePath) throws IOException {
        if (!EngineUtil.chmod(exePath))
            throw new IOException("chmod failed");
    }
}
