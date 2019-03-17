/*
    GtbProbe - Java interface to Gaviota endgame tablebases.
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

package org.petero.droidfish.tb;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.petero.droidfish.engine.EngineUtil;

/** Interface to native gtb probing code. */
class GtbProbe {
    static {
        System.loadLibrary("gtb");
    }

    private String currTbPath = "";
    private ConcurrentLinkedQueue<String> tbPathQueue = new ConcurrentLinkedQueue<String>();

    GtbProbe() {
    }

    public final void setPath(String tbPath, boolean forceReload) {
        if (forceReload || !tbPathQueue.isEmpty() || !currTbPath.equals(tbPath)) {
            tbPathQueue.add(tbPath);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Sleep 0.5s to increase probability that engine
                    // is initialized before TB.
                    try { Thread.sleep(500); } catch (InterruptedException e) { }
                    initIfNeeded();
                }
            });
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
    }

    public final synchronized void initIfNeeded() {
        String path = tbPathQueue.poll();
        while (!tbPathQueue.isEmpty())
            path = tbPathQueue.poll();
        if (path != null) {
            currTbPath = path;
            synchronized (EngineUtil.nativeLock) {
                init(currTbPath);
            }
        }
    }

    final static int NOPIECE = 0;
    final static int PAWN    = 1;
    final static int KNIGHT  = 2;
    final static int BISHOP  = 3;
    final static int ROOK    = 4;
    final static int QUEEN   = 5;
    final static int KING    = 6;

    final static int NOSQUARE = 64;

    // Castle masks
    final static int H1_CASTLE = 8;
    final static int A1_CASTLE = 4;
    final static int H8_CASTLE = 2;
    final static int A8_CASTLE = 1;

    // tbinfo values
    final static int DRAW    = 0;
    final static int WMATE   = 1;
    final static int BMATE   = 2;
    final static int FORBID  = 3;
    final static int UNKNOWN = 7;

    /**
     * Probe tablebases.
     * @param wtm           True if white to move.
     * @param epSq          En passant square, or NOSQUARE.
     * @param castleMask    Castle mask.
     * @param whiteSquares  Array of squares occupied by white pieces, terminated with NOSQUARE.
     * @param blackSquares  Array of squares occupied by black pieces, terminated with NOSQUARE.
     * @param whitePieces   Array of white pieces, terminated with NOPIECE.
     * @param blackPieces   Array of black pieces, terminated with NOPIECE.
     * @param result        Two element array. Set to [tbinfo, plies].
     * @return              True if success.
     */
    public final native boolean probeHard(boolean wtm, int epSq,
                                          int castleMask,
                                          int[] whiteSquares,
                                          int[] blackSquares,
                                          byte[] whitePieces,
                                          byte[] blackPieces,
                                          int[] result);

    private final native static boolean init(String tbPath);
}
