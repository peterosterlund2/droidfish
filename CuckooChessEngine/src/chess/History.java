/*
    CuckooChess - A java chess program.
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

package chess;

/**
 * Implements the relative history heuristic.
 * @author petero
 */
public final class History {
    private int countSuccess[][];
    private int countFail[][];
    private int score[][];

    public History() {
        init();
    }

    public void init() {
        countSuccess = new int[Piece.nPieceTypes][64];
        countFail = new int[Piece.nPieceTypes][64];
        score = new int[Piece.nPieceTypes][64];
        for (int p = 0; p < Piece.nPieceTypes; p++) {
            for (int sq = 0; sq < 64; sq++) {
                countSuccess[p][sq] = 0;
                countFail[p][sq] = 0;
                score[p][sq] = -1;
            }
        }
    }

    /** Record move as a success. */
    public final void addSuccess(Position pos, Move m, int depth) {
        int p = pos.getPiece(m.from);
        int cnt = depth;
        int val = countSuccess[p][m.to] + cnt;
        if (val > 1000) {
            val /= 2;
            countFail[p][m.to] /= 2;
        }
        countSuccess[p][m.to] = val;
        score[p][m.to] = -1;
    }

    /** Record move as a failure. */
    public final void addFail(Position pos, Move m, int depth) {
        int p = pos.getPiece(m.from);
        int cnt = depth;
        countFail[p][m.to] += cnt;
        score[p][m.to] = -1;
    }

    /** Get a score between 0 and 49, depending of the success/fail ratio of the move. */
    public final int getHistScore(Position pos, Move m) {
        int p = pos.getPiece(m.from);
        int ret = score[p][m.to];
        if (ret >= 0)
            return ret;
        int succ = countSuccess[p][m.to];
        int fail = countFail[p][m.to];
        if (succ + fail > 0) {
            ret = succ * 49 / (succ + fail);
        } else {
            ret = 0;
        }
        score[p][m.to] = ret;
        return ret;
    }
}
