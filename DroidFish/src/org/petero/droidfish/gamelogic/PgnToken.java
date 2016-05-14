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

/** A token in a PGN data stream. Used by the PGN parser. */
public class PgnToken {
    // These are tokens according to the PGN spec
    public static final int STRING = 0;
    public static final int INTEGER = 1;
    public static final int PERIOD = 2;
    public static final int ASTERISK = 3;
    public static final int LEFT_BRACKET = 4;
    public static final int RIGHT_BRACKET = 5;
    public static final int LEFT_PAREN = 6;
    public static final int RIGHT_PAREN = 7;
    public static final int NAG = 8;
    public static final int SYMBOL = 9;

    // These are not tokens according to the PGN spec, but the parser
    // extracts these anyway for convenience.
    public static final int COMMENT = 10;
    public static final int EOF = 11;

    // Actual token data
    int type;
    String token;

    PgnToken(int type, String token) {
        this.type = type;
        this.token = token;
    }

    /** PGN parser visitor interface. */
    public interface PgnTokenReceiver {
        /** If this method returns false, the object needs a full re-initialization, using clear() and processToken(). */
        public boolean isUpToDate();

        /** Clear object state. */
        public void clear();

        /** Update object state with one token from a PGN game. */
        public void processToken(GameTree.Node node, int type, String token);

        /** Change current move number. */
        public void setCurrent(GameTree.Node node);
    }
}
