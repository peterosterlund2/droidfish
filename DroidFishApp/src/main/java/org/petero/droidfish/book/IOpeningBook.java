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

package org.petero.droidfish.book;

import android.util.Pair;

import java.util.ArrayList;

import org.petero.droidfish.book.DroidBook.BookEntry;
import org.petero.droidfish.gamelogic.Game;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Position;

public interface IOpeningBook {
    /** Return true if book is currently enabled. */
    boolean enabled();

    /** Set book options, including filename. */
    void setOptions(BookOptions options);

    /** Information required to query an opening book. */
    class BookPosInput {
        private final Position currPos;

        private Game game;
        private Position prevPos;
        private ArrayList<Move> moves;

        public BookPosInput(Position currPos, Position prevPos, ArrayList<Move> moves) {
            this.currPos = currPos;
            this.prevPos = prevPos;
            this.moves = moves;
        }

        public BookPosInput(Game game) {
            currPos = game.currPos();
            this.game = game;
        }

        public Position getCurrPos() {
            return currPos;
        }
        public Position getPrevPos() {
            lazyInit();
            return prevPos;
        }
        public ArrayList<Move> getMoves() {
            lazyInit();
            return moves;
        }

        private void lazyInit() {
            if (prevPos == null) {
                Pair<Position, ArrayList<Move>> ph = game.getUCIHistory();
                prevPos = ph.first;
                moves = ph.second;
            }
        }
    }
    /** Get all book entries for a position. */
    ArrayList<BookEntry> getBookEntries(BookPosInput posInput);
}
