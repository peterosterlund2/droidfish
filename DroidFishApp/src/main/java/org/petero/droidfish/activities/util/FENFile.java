/*
    DroidFish - An Android chess program.
    Copyright (C) 2013  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.activities.util;

import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FENFile {
    private final File fileName;

    public FENFile(String fileName) {
        this.fileName = new File(fileName);
    }

    public final String getName() {
        return fileName.getAbsolutePath();
    }

    public static final class FenInfo {
        public int gameNo;
        public String fen;

        FenInfo(int gameNo, String fen) {
            this.gameNo = gameNo;
            this.fen = fen;
        }

        public String toString() {
            StringBuilder info = new StringBuilder(128);
            info.append(gameNo);
            info.append(". ");
            info.append(fen);
            return info.toString();
        }
    }

    public enum FenInfoResult {
        OK,
        OUT_OF_MEMORY;
    }

    /** Read all FEN strings (one per line) in a file. */
    public final Pair<FenInfoResult,ArrayList<FenInfo>> getFenInfo() {
        ArrayList<FenInfo> fensInFile = new ArrayList<>();
        try (BufferedRandomAccessFileReader f =
                 new BufferedRandomAccessFileReader(fileName.getAbsolutePath())) {
            int fenNo = 1;
            while (true) {
                String line = f.readLine();
                if (line == null)
                    break; // EOF
                if ((line.length() == 0) || (line.charAt(0) == '#'))
                    continue;
                FenInfo fi = new FenInfo(fenNo++, line.trim());
                fensInFile.add(fi);
            }
        } catch (IOException ignore) {
        } catch (OutOfMemoryError e) {
            fensInFile.clear();
            fensInFile = null;
            return new Pair<>(FenInfoResult.OUT_OF_MEMORY, null);
        }
        return new Pair<>(FenInfoResult.OK, fensInFile);
    }
}
