/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2013  Peter Österlund, peterosterlund2@gmail.com

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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.petero.droidfish.DroidFishApp;
import org.petero.droidfish.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.widget.Toast;

public class PGNFile {
    private final File fileName;

    public PGNFile(String fileName) {
        this.fileName = new File(fileName);
    }

    public String getName() {
        return fileName.getAbsolutePath();
    }

    public static final class GameInfo {
        public String info = "";
        public long startPos;
        public long endPos;

        GameInfo setNull(long currPos) {
            info = null;
            startPos = currPos;
            endPos = currPos;
            return this;
        }

        boolean isNull() { return info == null; }

        public String toString() {
            if (info == null)
                return "--";
            return info;
        }
    }

    private final static class HeaderInfo {
        int gameNo;
        String event = "";
        String site = "";
        String date = "";
        String round = "";
        String white = "";
        String black = "";
        String result = "";

        HeaderInfo(int gameNo) {
            this.gameNo = gameNo;
        }

        public String toString() {
            StringBuilder info = new StringBuilder(128);
            info.append(gameNo);
            info.append(". ");
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

    private static class BytesToString {
        private byte[] buf = new byte[256];
        private int len = 0;

        public void write(int c) {
            if (len < 256)
                buf[len++] = (byte)c;
        }
        public void reset() {
            len = 0;
        }
        @Override
        public String toString() {
            return new String(buf, 0, len);
        }
    }
    
    private static class BufferedInput implements Closeable {
        private byte buf[] = new byte[8192];
        private int bufLen = 0;
        private int pos = 0;
        private InputStream is;
        public BufferedInput(InputStream is) {
            this.is = is;
        }
        public int read() throws IOException {
            if (pos >= bufLen) {
                int len = is.read(buf);
                if (len <= 0)
                    return -1;
                pos = 0;
                bufLen = len;
            }
            return buf[pos++] & 0xff;
        }
        @Override
        public void close() {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static class ProgressHandler {
        final ProgressDialog progress;
        final Activity activity;
        int percent = -1;
        long fileLen = -1;

        ProgressHandler(File file, Activity activity, ProgressDialog progress) {
            this.activity = activity;
            this.progress = progress;
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                fileLen = raf.length();
            } catch (IOException ignore) {
            }
        }

        void reportProgress(long nRead) {
            int newPercent = fileLen > 0 ? (int)(nRead * 100 / fileLen) : 0;
            if (newPercent > percent) {
                percent = newPercent;
                activity.runOnUiThread(() -> progress.setProgress(newPercent));
            }
        }
    }

    public static class NotPgnFile extends IOException {
        NotPgnFile() {
            super("");
        }
    }

    public static class CancelException extends IOException {
        CancelException() {
            super("");
        }
    }

    /** Return info about all PGN games in a file. */
    public ArrayList<GameInfo> getGameInfo(Activity activity,
                                           ProgressDialog progress) throws IOException {
        if (activity == null || progress == null)
            return getGameInfoFromFile(null, -1);
        ProgressHandler handler = new ProgressHandler(fileName, activity, progress);
        return getGameInfoFromFile(handler, -1);
    }

    /** Return info about up to "maxGames" PGN games in a file. */
    public ArrayList<GameInfo> getGameInfo(int maxGames) throws IOException {
        return getGameInfoFromFile(null, maxGames);
    }

    public static ArrayList<GameInfo> getGameInfo(String pgnData, int maxGames) {
        try (InputStream is = new ByteArrayInputStream(pgnData.getBytes("UTF-8"))) {
            return getGameInfo(is, null, maxGames);
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    private ArrayList<GameInfo> getGameInfoFromFile(ProgressHandler progress,
                                                    int maxGames) throws IOException {
        try (InputStream is = new FileInputStream(fileName)) {
            return getGameInfo(is, progress, maxGames);
        }
    }

    /** Return info about PGN games in a file. */
    private static ArrayList<GameInfo> getGameInfo(InputStream is, ProgressHandler progress,
                                                   int maxGames) throws IOException {
        ArrayList<GameInfo> gamesInFile = new ArrayList<>();
        long nRead = 0;
        try (BufferedInput f = new BufferedInput(is)) {
            GameInfo gi = null;
            HeaderInfo hi = null;
            boolean inHeader = false;
            boolean inHeaderSection = false;
            long filePos = 0;
            int gameNo = 1;

            final int INITIAL       = 0;
            final int NORMAL        = 1;
            final int BRACE_COMMENT = 2;
            final int LINE_COMMENT  = 3;
            final int STRING        = 4;
            final int STRING_ESCAPE = 5;
            final int HEADER        = 6;
            final int HEADER_SYMBOL = 7;
            final int EOF           = 8;
            int state = INITIAL;

            boolean firstColumn = true;
            BytesToString lastSymbol = new BytesToString();
            BytesToString lastString = new BytesToString();
            while (state != EOF) {
                filePos = nRead;
                int c = f.read();
                nRead++;

                if (c == -1) {
                    state = EOF;
                    continue;
                }

                if (firstColumn) { // Handle % escape mechanism
                    if (c == '%') {
                        state = LINE_COMMENT;
                        continue;
                    }
                }
                firstColumn = (c == '\n' || c == '\r');

                switch (state) {
                case BRACE_COMMENT:
                    if (c == '}')
                        state = NORMAL;
                    break;
                case LINE_COMMENT:
                    if (c == '\n' || c == '\r')
                        state = NORMAL;
                    break;
                case STRING:
                    if (c == '"')
                        state = NORMAL;
                    else if (c == '\\')
                        state = STRING_ESCAPE;
                    else
                        lastString.write(c);
                    break;
                case STRING_ESCAPE:
                    lastString.write(c);
                    state = STRING;
                    break;
                case HEADER_SYMBOL:
                    switch (c) {
                    case '"':
                        state = STRING;
                        lastString.reset();
                        break;
                    case ' ': case '\n': case '\r': case '\t': case 160: case ']':
                        state = NORMAL;
                        break;
                    default:
                        lastSymbol.write(c);
                        break;
                    }
                    break;
                case HEADER:
                case INITIAL:
                case NORMAL:
                    switch (c) {
                    case -1:
                        state = EOF;
                        break;
                    case '[':
                        state = HEADER;
                        inHeader = true;
                        break;
                    case ']':
                        if (inHeader) {
                            inHeader = false;
                            String tag = lastSymbol.toString();
                            String value = lastString.toString();
                            if ("Event".equals(tag)) {
                                hi.event = value.equals("?") ? "" : value;
                            } else if ("Site".equals(tag)) {
                                hi.site = value.equals("?") ? "" : value;
                            } else if ("Date".equals(tag)) {
                                hi.date = value.equals("?") ? "" : value;
                            } else if ("Round".equals(tag)) {
                                hi.round = value.equals("?") ? "" : value;
                            } else if ("White".equals(tag)) {
                                hi.white = value;
                            } else if ("Black".equals(tag)) {
                                hi.black = value;
                            } else if ("Result".equals(tag)) {
                                if (value.equals("1-0")) hi.result = "1-0";
                                else if (value.equals("0-1")) hi.result = "0-1";
                                else if ((value.equals("1/2-1/2")) || (value.equals("1/2"))) hi.result = "1/2-1/2";
                                else hi.result = "*";
                            }
                        }
                        state = NORMAL;
                        break;
                    case '.':
                    case '*':
                    case '(':
                    case ')':
                    case '$':
                        inHeaderSection = false;
                        break;
                    case '{':
                        state = BRACE_COMMENT;
                        inHeaderSection = false;
                        break;
                    case ';':
                        state = LINE_COMMENT;
                        inHeaderSection = false;
                        break;
                    case '"':
                        state = STRING;
                        lastString.reset();
                        break;
                    case ' ': case '\n': case '\r': case '\t': case 160:
                        break;
                    default:
                        if (inHeader) {
                            state = HEADER_SYMBOL;
                            lastSymbol.reset();
                            lastSymbol.write(c);
                        } else {
                            inHeaderSection = false;
                        }
                        break;
                    }
                }

                if (state == HEADER) {
                    if (!inHeaderSection) { // Start of game
                        inHeaderSection = true;
                        if (gi != null) {
                            gi.endPos = filePos;
                            gi.info = hi.toString();
                            gamesInFile.add(gi);
                            if ((maxGames > 0) && gamesInFile.size() >= maxGames) {
                                gi = null;
                                break;
                            }
                            if (progress != null)
                                progress.reportProgress(filePos);
                            if (Thread.currentThread().isInterrupted())
                                throw new CancelException();
                        }
                        gi = new GameInfo();
                        gi.startPos = filePos;
                        gi.endPos = -1;
                        hi = new HeaderInfo(gameNo++);
                    }
                }
            }
            if (gi != null) {
                gi.endPos = filePos;
                gi.info = hi.toString();
                gamesInFile.add(gi);
            }
        }
        if (gamesInFile.isEmpty() && nRead > 1)
            throw new NotPgnFile();

        return gamesInFile;
    }

    private void mkDirs() {
        File dirFile = fileName.getParentFile();
        dirFile.mkdirs();
    }

    /** Read one game defined by gi. Return null on failure. */
    String readOneGame(GameInfo gi) {
        try (RandomAccessFile f = new RandomAccessFile(fileName, "r")) {
            byte[] pgnData = new byte[(int) (gi.endPos - gi.startPos)];
            f.seek(gi.startPos);
            f.readFully(pgnData);
            return new String(pgnData);
        } catch (IOException ignore) {
        }
        return null;
    }

    /** Append PGN to the end of this PGN file. */
    public void appendPGN(String pgn, boolean silent) {
        mkDirs();
        try (FileWriter fw = new FileWriter(fileName, true)) {
            fw.write(pgn);
            if (!silent)
                DroidFishApp.toast(R.string.game_saved, Toast.LENGTH_SHORT);
        } catch (IOException e) {
            DroidFishApp.toast(R.string.failed_to_save_game, Toast.LENGTH_SHORT);
        }
    }

    /** Save a PGN game first in the file and remove games at the end of the file
     *  to enforce a maximum number of games in the auto-save file. */
    public void autoSave(String pgn) {
        final int maxAutoSaveGames = 20;
        try {
            if (!fileName.exists()) {
                appendPGN(pgn, true);
            } else {
                ArrayList<GameInfo> gamesInFile = getGameInfo(null, null);
                for (int i = gamesInFile.size() - 1; i >= 0; i--) {
                    GameInfo gi = gamesInFile.get(i);
                    String oldGame = readOneGame(gi);
                    if (pgn.equals(oldGame))
                        deleteGame(gi, gamesInFile);
                }
                while (gamesInFile.size() > maxAutoSaveGames - 1)
                    deleteGame(gamesInFile.get(gamesInFile.size() - 1), gamesInFile);
                GameInfo gi = new GameInfo().setNull(0);
                replacePGN(pgn, gi, true);
            }
        } catch (IOException e) {
            DroidFishApp.toast(R.string.failed_to_save_game, Toast.LENGTH_SHORT);
        }
    }

    boolean deleteGame(GameInfo gi, ArrayList<GameInfo> gamesInFile) {
        try {
            File tmpFile = new File(fileName + ".tmp_delete");
            try (RandomAccessFile fileReader = new RandomAccessFile(fileName, "r");
                 RandomAccessFile fileWriter = new RandomAccessFile(tmpFile, "rw")) {
                copyData(fileReader, fileWriter, gi.startPos);
                fileReader.seek(gi.endPos);
                copyData(fileReader, fileWriter, fileReader.length() - gi.endPos);
            }
            if (!tmpFile.renameTo(fileName))
                throw new IOException();

            // Update gamesInFile
            if (gamesInFile != null) {
                gamesInFile.remove(gi);
                int nGames = gamesInFile.size();
                long delta = gi.endPos - gi.startPos;
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
            DroidFishApp.toast(R.string.failed_to_delete_game, Toast.LENGTH_SHORT);
        }
        return false;
    }

    void replacePGN(String pgnToSave, GameInfo gi, boolean silent) {
        try {
            File tmpFile = new File(fileName + ".tmp_delete");
            try (RandomAccessFile fileReader = new RandomAccessFile(fileName, "r");
                 RandomAccessFile fileWriter = new RandomAccessFile(tmpFile, "rw")) {
                copyData(fileReader, fileWriter, gi.startPos);
                fileWriter.write(pgnToSave.getBytes());
                fileReader.seek(gi.endPos);
                copyData(fileReader, fileWriter, fileReader.length() - gi.endPos);
            }
            if (!tmpFile.renameTo(fileName))
                throw new IOException();
            if (!silent)
                DroidFishApp.toast(R.string.game_saved, Toast.LENGTH_SHORT);
        } catch (IOException e) {
            DroidFishApp.toast(R.string.failed_to_save_game, Toast.LENGTH_SHORT);
        }
    }

    private static void copyData(RandomAccessFile fileReader,
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

    /** Delete the file. */
    boolean delete() {
        return fileName.delete();
    }
}
