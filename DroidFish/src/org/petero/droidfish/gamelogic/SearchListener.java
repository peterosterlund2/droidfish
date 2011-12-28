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
import java.util.List;


/**
 * Used to get various search information during search.
 */
public interface SearchListener {
    public final static class PvInfo {
        int depth;
        int score;
        int time;
        int nodes;
        int nps;
        boolean isMate;
        boolean upperBound;
        boolean lowerBound;
        ArrayList<Move> pv;
        String pvStr = "";

        public PvInfo(PvInfo pvi) {
            depth = pvi.depth;
            score = pvi.score;
            time = pvi.time;
            nodes = pvi.nodes;
            nps = pvi.nps;
            isMate = pvi.isMate;
            upperBound = pvi.upperBound;
            lowerBound = pvi.lowerBound;
            pv = new ArrayList<Move>(pvi.pv.size());
            for (int i = 0; i < pvi.pv.size(); i++)
                pv.add(pvi.pv.get(i));
            pvStr = pvi.pvStr;
        }

        public PvInfo(int depth, int score, int time, int nodes, int nps,
                      boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv) {
            this.depth = depth;
            this.score = score;
            this.time = time;
            this.nodes = nodes;
            this.nps = nps;
            this.isMate = isMate;
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
            this.pv = pv;
        }

        public final void removeFirstMove() {
            if (!pv.isEmpty())
                pv.remove(0);
        }
    }

    public void notifyDepth(int depth);
    public void notifyCurrMove(Position pos, Move m, int moveNr);
    public void notifyPV(Position pos, ArrayList<PvInfo> pvInfo, boolean isPonder);
    public void notifyStats(int nodes, int nps, int time);
    public void notifyBookInfo(String bookInfo, List<Move> moveList);

    public void notifySearchResult(Game g, String cmd, Move ponder);
}
