/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.petero.droidfish.gamelogic.Pair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

public class PGNFile {
    private final File fileName;

    public PGNFile(String fileName) {
        this.fileName = new File(fileName);
    }

    public final String getName() {
        return fileName.getAbsolutePath();
    }

    static final class GameInfo {
        String info = "";
        long startPos;
        long endPos;

        final GameInfo setNull(long currPos) {
            info = null;
            startPos = currPos;
            endPos = currPos;
            return this;
        }

        final boolean isNull() { return info == null; }

        public String toString() {
            if (info == null)
                return "--";
            return info;
        }
    }


    static private final class BufferedRandomAccessFileReader {
        RandomAccessFile f;
        byte[] buffer = new byte[8192];
        long bufStartFilePos = 0;
        int bufLen = 0;
        int bufPos = 0;

        BufferedRandomAccessFileReader(String fileName) throws FileNotFoundException {
            f = new RandomAccessFile(fileName, "r");
        }
        final long length() throws IOException {
            return f.length();
        }
        final long getFilePointer() throws IOException {
            return bufStartFilePos + bufPos;
        }
        final void close() throws IOException {
            f.close();
        }

        private final static int EOF = -1024;

        final String readLine() throws IOException {
            // First handle the common case where the next line is entirely
            // contained in the buffer
            for (int i = bufPos; i < bufLen; i++) {
                byte b = buffer[i];
                if ((b == '\n') || (b == '\r')) {
                    String line = new String(buffer, bufPos, i - bufPos);
                    for ( ; i < bufLen; i++) {
                        b = buffer[i];
                        if ((b != '\n') && (b != '\r')) {
                            bufPos = i;
                            return line;
                        }
                    }
                    break;
                }
            }

            // Generic case
            byte[] lineBuf = new byte[8192];
            int lineLen = 0;
            int b;
            while (true) {
                b = getByte();
                if (b == '\n' || b == '\r' || b == EOF)
                    break;
                lineBuf[lineLen++] = (byte)b;
                if (lineLen >= lineBuf.length)
                    break;
            }
            while (true) {
                b = getByte();
                if ((b != '\n') && (b != '\r')) {
                    if (b != EOF)
                        bufPos--;
                    break;
                }
            }
            if ((b == EOF) && (lineLen == 0))
                return null;
            else
                return new String(lineBuf, 0, lineLen);
        }

        private final int getByte() throws IOException {
            if (bufPos >= bufLen) {
                bufStartFilePos = f.getFilePointer();
                bufLen = f.read(buffer);
                bufPos = 0;
                if (bufLen <= 0)
                    return EOF;
            }
            return buffer[bufPos++];
        }
    }

    private final static class HeaderInfo {
        String event = "";
        String site = "";
        String date = "";
        String round = "";
        String white = "";
        String black = "";
        String result = "";

        public String toString() {
            StringBuilder info = new StringBuilder(128);
            info.append(white);
            info.append(" - ");
            info.append(black);
            if (date.length() > 0) {
                info.append(' ');
                info.append(date);
            }
            if (round.length() > 0) {
                info.append(' ');
                info.append(round);
            }
            if (event.length() > 0) {
                info.append(' ');
                info.append(event);
            }
            if (site.length() > 0) {
                info.append(' ');
                info.append(site);
            }
            info.append(' ');
            info.append(result);
            return info.toString();
        }
    }

    public static enum GameInfoResult {
        OK,
        CANCEL,
        OUT_OF_MEMORY;
    }

    /** Return info about all PGN games in a file. */
    public final Pair<GameInfoResult,ArrayList<GameInfo>> getGameInfo(Activity activity,
                                                                      final ProgressDialog progress) {
        ArrayList<GameInfo> gamesInFile = new ArrayList<GameInfo>();
        try {
            int percent = -1;
            gamesInFile.clear();
            BufferedRandomAccessFileReader f = new BufferedRandomAccessFileReader(fileName.getAbsolutePath());
            long fileLen = f.length();
            GameInfo gi = null;
            HeaderInfo hi = null;
            boolean inHeader = false;
            long filePos = 0;
            while (true) {
                filePos = f.getFilePointer();
                String line = f.readLine();
                if (line == null)
                    break; // EOF
                int len = line.length();
                if (len == 0)
                    continue;
                boolean isHeader = line.charAt(0) == '[';
                if (isHeader) {
                    if (!line.contains("\"")) // Try to avoid some false positives
                        isHeader = false;
                }
                if (isHeader) {
                    if (!inHeader) { // Start of game
                        inHeader = true;
                        if (gi != null) {
                            gi.endPos = filePos;
                            gi.info = hi.toString();
                            gamesInFile.add(gi);
                            final int newPercent = (int)(filePos * 100 / fileLen);
                            if (newPercent > percent) {
                                percent =  newPercent;
                                if (progress != null) {
                                    activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            progress.setProgress(newPercent);
                                        }
                                    });
                                }
                            }
                            if (Thread.currentThread().isInterrupted())
                                return new Pair<GameInfoResult,ArrayList<GameInfo>>(GameInfoResult.CANCEL, null);
                        }
                        gi = new GameInfo();
                        gi.startPos = filePos;
                        gi.endPos = -1;
                        hi = new HeaderInfo();
                    }
                    if (line.startsWith("[Event ")) {
                        hi.event = line.substring(8, len - 2);
                        if (hi.event.equals("?")) hi.event = "";
                    } else if (line.startsWith("[Site ")) {
                        hi.site = line.substring(7, len - 2);
                        if (hi.site.equals("?")) hi.site = "";
                    } else if (line.startsWith("[Date ")) {
                        hi.date = line.substring(7, len - 2);
                        if (hi.date.equals("?")) hi.date = "";
                    } else if (line.startsWith("[Round ")) {
                        hi.round = line.substring(8, len - 2);
                        if (hi.round.equals("?")) hi.round = "";
                    } else if (line.startsWith("[White ")) {
                        hi.white = line.substring(8, len - 2);
                    } else if (line.startsWith("[Black ")) {
                        hi.black = line.substring(8, len - 2);
                    } else if (line.startsWith("[Result ")) {
                        hi.result = line.substring(9, len - 2);
                        if (hi.result.equals("1-0")) hi.result = "1-0";
                        else if (hi.result.equals("0-1")) hi.result = "0-1";
                        else if ((hi.result.equals("1/2-1/2")) || (hi.result.equals("1/2"))) hi.result = "1/2-1/2";
                        else hi.result = "*";
                    }
                } else {
                    inHeader = false;
                }
            }
            if (gi != null) {
                gi.endPos = filePos;
                gi.info = hi.toString();
                gamesInFile.add(gi);
            }
            f.close();
        } catch (IOException e) {
        } catch (OutOfMemoryError e) {
            gamesInFile.clear();
            gamesInFile = null;
            return new Pair<GameInfoResult,ArrayList<GameInfo>>(GameInfoResult.OUT_OF_MEMORY, null);
        }

        return new Pair<GameInfoResult,ArrayList<GameInfo>>(GameInfoResult.OK, gamesInFile);
    }

    private final void mkDirs() {
        File dirFile = fileName.getParentFile();
        dirFile.mkdirs();
    }

    /** Read one game defined by gi. Return null on failure. */
    final String readOneGame(GameInfo gi) {
        try {
            RandomAccessFile f = new RandomAccessFile(fileName, "r");
            byte[] pgnData = new byte[(int) (gi.endPos - gi.startPos)];
            f.seek(gi.startPos);
            f.readFully(pgnData);
            f.close();
            return new String(pgnData);
        } catch (IOException e) {
        }
        return null;
    }

    /** Append PGN to the end of this PGN file. */
    public final void appendPGN(String pgn, Context context) {
        try {
            mkDirs();
            FileWriter fw = new FileWriter(fileName, true);
            fw.write(pgn);
            fw.close();
            Toast.makeText(context, "Game saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            if (context != null) {
                String msg = "Failed to save game";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    final boolean deleteGame(GameInfo gi, Context context, ArrayList<GameInfo> gamesInFile) {
        try {
            File tmpFile = new File(fileName + ".tmp_delete");
            RandomAccessFile fileReader = new RandomAccessFile(fileName, "r");
            RandomAccessFile fileWriter = new RandomAccessFile(tmpFile, "rw");
            copyData(fileReader, fileWriter, gi.startPos);
            fileReader.seek(gi.endPos);
            copyData(fileReader, fileWriter, fileReader.length() - gi.endPos);
            fileReader.close();
            fileWriter.close();
            if (!tmpFile.renameTo(fileName))
                throw new IOException();

            // Update gamesInFile
            if (gamesInFile != null) {
                gamesInFile.remove(gi);
                final int nGames = gamesInFile.size();
                final long delta = gi.endPos - gi.startPos;
                for (int i = 0; i < nGames; i++) {
                    GameInfo tmpGi = gamesInFile.get(i);
                    if (tmpGi.startPos > gi.startPos) {
                        tmpGi.startPos -= delta;
                        tmpGi.endPos -= delta;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            if (context != null) {
                String msg = "Failed to delete game";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    final boolean replacePGN(String pgnToSave, GameInfo gi, Context context) {
        try {
            File tmpFile = new File(fileName + ".tmp_delete");
            RandomAccessFile fileReader = new RandomAccessFile(fileName, "r");
            RandomAccessFile fileWriter = new RandomAccessFile(tmpFile, "rw");
            copyData(fileReader, fileWriter, gi.startPos);
            fileWriter.write(pgnToSave.getBytes());
            fileReader.seek(gi.endPos);
            copyData(fileReader, fileWriter, fileReader.length() - gi.endPos);
            fileReader.close();
            fileWriter.close();
            tmpFile.renameTo(fileName);
            Toast.makeText(context, "Game saved", Toast.LENGTH_SHORT).show();
            return true;
        } catch (IOException e) {
            if (context != null) {
                String msg = "Failed to save game";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    private final static void copyData(RandomAccessFile fileReader,
               RandomAccessFile fileWriter,
               long nBytes) throws IOException {
        byte[] buffer = new byte[8192];
        while (nBytes > 0) {
            int nRead = fileReader.read(buffer, 0, Math.min(buffer.length, (int)nBytes));
            if (nRead > 0) {
                fileWriter.write(buffer, 0, nRead);
                nBytes -= nRead;
            }
        }
    }

    final boolean delete() {
        return fileName.delete();
    }
}
