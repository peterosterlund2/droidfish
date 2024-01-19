/*
    EngineServer - Network engine server for DroidFish
    Copyright (C) 2019  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.engineserver;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


/** Listens to a TCP port and connects an engine process to a TCP socket. */
class PortListener {
    private final EngineConfig config;
    private final ErrorHandler errorHandler;

    private final Thread thread;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Process proc;
    private volatile boolean shutDownFlag = false;

    public PortListener(EngineConfig config, ErrorHandler errorHandler) {
        this.config = config;
        this.errorHandler = errorHandler;

        thread = new Thread(() -> {
            try {
                mainLoop();
            } catch (InterruptedException ex) {
                if (!shutDownFlag)
                    reportError("Background thread interrupted", ex);
            } catch (IOException ex) {
                if (!shutDownFlag)
                    reportError("IO error in background thread", ex);
            }
        });
        thread.start();
    }

    private void mainLoop() throws IOException, InterruptedException {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(config.port));

            synchronized (PortListener.class) {
                System.out.printf("Listening on port %d\n", config.port);
            }

            this.serverSocket = serverSocket;
            while (!shutDownFlag) {
                try (Socket clientSocket = serverSocket.accept()) {
                    this.clientSocket = clientSocket;

                    ProcessBuilder builder = new ProcessBuilder();
                    ArrayList<String> args = new ArrayList<>();
                    args.add(config.filename);
                    addArguments(args, config.arguments);
                    builder.command(args);
                    File dir = new File(config.filename).getParentFile();
                    if (dir != null)
                        builder.directory(dir);
                    builder.redirectError(ProcessBuilder.Redirect.INHERIT);

                    Process proc = builder.start();
                    this.proc = proc;
                    Thread t1 = forwardIO(proc.getInputStream(), clientSocket.getOutputStream());
                    Thread t2 = forwardIO(clientSocket.getInputStream(), proc.getOutputStream());
                    try {
                        /* int exitCode = */ proc.waitFor();
//                        if (exitCode != 0) {
//                            errorHandler.reportError("Engine error",
//                                                     "Engine terminated with status " + exitCode);
//                        }
                    } catch (InterruptedException ex) {
                        proc.getOutputStream().close();
                        proc.destroyForcibly();
                    } finally {
                        close(clientSocket);
                        t1.join();
                        t2.join();
                        proc.waitFor();
                        this.proc = null;
                    }
                }
            }
        }
    }

    private void addArguments(ArrayList<String> cmdList, String argString) {
        boolean inQuote = false;
        StringBuilder sb = new StringBuilder();
        int len = argString.length();
        for (int i = 0; i < len; i++) {
            char c = argString.charAt(i);
            switch (c) {
            case '"':
                inQuote = !inQuote;
                if (!inQuote) {
                    cmdList.add(sb.toString());
                    sb.setLength(0);
                }
                break;
            case '\\':
                if (i < len - 1) {
                    sb.append(argString.charAt(i + 1));
                    i++;
                }
                break;
            case ' ':
            case '\t':
                if (!inQuote) {
                    if (!sb.toString().isEmpty()) {
                        cmdList.add(sb.toString());
                        sb.setLength(0);
                    }
                    break;
                }
            default:
                sb.append(c);
                break;
            }
        }
        if (!sb.toString().isEmpty())
            cmdList.add(sb.toString());
    }

    private Thread forwardIO(InputStream is, OutputStream os) {
        Thread t = new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int len = is.read(buffer);
                    if (len < 0)
                        break;
                    os.write(buffer, 0, len);
                    os.flush();
                }
            } catch (IOException ignore) {
            }
            close(is);
            close(os);
            Process p = proc;
            if (p != null)
                p.destroyForcibly();
        });
        t.start();
        return t;
    }

    public void shutdown() {
        shutDownFlag = true;
        thread.interrupt();
        ServerSocket ss = serverSocket;
        if (ss != null)
            close(ss);
        Socket s = clientSocket;
        if (s != null)
            close(s);
        try {
            thread.join();
        } catch (InterruptedException ex) {
            reportError("Failed to shutdown background thread", ex);
        }
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }

    private void reportError(String errMsg, Exception ex) {
        errorHandler.reportError(errMsg, ex.getMessage());
    }
}
