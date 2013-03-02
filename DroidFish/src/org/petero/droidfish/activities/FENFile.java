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

package org.petero.droidfish.activities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.petero.droidfish.gamelogic.Pair;

import android.app.Activity;
import android.app.ProgressDialog;

public class FENFile {
    private final File fileName;

    public FENFile(String fileName) {
        this.fileName = new File(fileName);
    }

    public final String getName() {
        return fileName.getAbsolutePath();
    }

    static final class FenInfo {
        int gameNo;
        String fen;

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

    public static enum FenInfoResult {
        OK,
        CANCEL,
        OUT_OF_MEMORY;
    }

    /** Read all FEN strings (one per line) in a file. */
    public final Pair<FenInfoResult,ArrayList<FenInfo>> getFenInfo(Activity activity,
                                                                   final ProgressDialog progress) {
        ArrayList<FenInfo> fensInFile = new ArrayList<FenInfo>();
        try {
            int percent = -1;
            fensInFile.clear();
            BufferedRandomAccessFileReader f = new BufferedRandomAccessFileReader(fileName.getAbsolutePath());
            long fileLen = f.length();
            long filePos = 0;
            int fenNo = 1;
            while (true) {
                filePos = f.getFilePointer();
                String line = f.readLine();
                if (line == null)
                    break; // EOF
                if ((line.length() == 0) || (line.charAt(0) == '#'))
                    continue;
                FenInfo fi = new FenInfo(fenNo++, line.trim());
                fensInFile.add(fi);
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
                    return new Pair<FenInfoResult,ArrayList<FenInfo>>(FenInfoResult.CANCEL, null);
            }
            f.close();
        } catch (IOException e) {
        } catch (OutOfMemoryError e) {
            fensInFile.clear();
            fensInFile = null;
            return new Pair<FenInfoResult,ArrayList<FenInfo>>(FenInfoResult.OUT_OF_MEMORY, null);
        }
        return new Pair<FenInfoResult,ArrayList<FenInfo>>(FenInfoResult.OK, fensInFile);
    }
}
