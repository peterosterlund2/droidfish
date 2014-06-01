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

package org.petero.droidfish.gamelogic;

import java.util.ArrayList;


/**
 * Used to get various search information during search.
 */
public interface SearchListener {
    public final static class PvInfo {
        int depth;
        int score;
        int time;
        long nodes;
        int nps;
        long tbHits;
        boolean isMate;
        boolean upperBound;
        boolean lowerBound;
        ArrayList<Move> pv;
        String pvStr = "";

        public PvInfo(int depth, int score, int time, long nodes, int nps, long tbHits,
                      boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv) {
            this.depth = depth;
            this.score = score;
            this.time = time;
            this.nodes = nodes;
            this.nps = nps;
            this.tbHits = tbHits;
            this.isMate = isMate;
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
            this.pv = pv;
        }
    }

    /** Report current engine search depth. */
    public void notifyDepth(int id, int depth);

    /** Report the move, valid in position pos, that the engine is currently searching. */
    public void notifyCurrMove(int id, Position pos, Move m, int moveNr);

    /**
     * Report PV information. If ponderMove is non-null, ponderMove is the first move
     * to play from position pos.
     */
    public void notifyPV(int id, Position pos, ArrayList<PvInfo> pvInfo, Move ponderMove);

    /** Report search statistics. */
    public void notifyStats(int id, long nodes, int nps, long tbHits, int time);

    /** Report opening book information. */
    public void notifyBookInfo(int id, String bookInfo, ArrayList<Move> moveList);

    /** Report move (or command, such as "resign") played by the engine. */
    public void notifySearchResult(int id, String cmd, Move ponder);

    /** Report engine name. */
    public void notifyEngineName(String engineName);

    /** Report engine error. */
    public void reportEngineError(String errMsg);
}
