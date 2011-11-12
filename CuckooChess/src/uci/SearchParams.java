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

package uci;

import chess.Move;
import java.util.ArrayList;
import java.util.List;

/**
 * Store search parameters (times, increments, max depth, etc).
 * @author petero
 */
public class SearchParams {
    List<Move> searchMoves;  // If non-empty, search only these moves
    int wTime;               // White remaining time, ms
    int bTime;               // Black remaining time, ms
    int wInc;                // White increment per move, ms
    int bInc;                // Black increment per move, ms
    int movesToGo;           // Moves to next time control
    int depth;               // If >0, don't search deeper than this
    int nodes;               // If >0, don't search more nodes than this
    int mate;                // If >0, search for mate-in-x
    int moveTime;            // If >0, search for exactly this amount of time, ms
    boolean infinite;

    public SearchParams() {
        searchMoves = new ArrayList<Move>();
    }
}
