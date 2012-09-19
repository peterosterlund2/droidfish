/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com
    Copyright (C) 2012  Leo Mayer

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

/**
 * Constants for different piece types.
 * @author petero
 */
public class Piece {
    public static final int EMPTY = 0;

    public static final int WKING = 1;
    public static final int WQUEEN = 2;
    public static final int WROOK = 3;
    public static final int WBISHOP = 4;
    public static final int WKNIGHT = 5;
    public static final int WPAWN = 6;

    public static final int BKING = 7;
    public static final int BQUEEN = 8;
    public static final int BROOK = 9;
    public static final int BBISHOP = 10;
    public static final int BKNIGHT = 11;
    public static final int BPAWN = 12;

    public static final int nPieceTypes = 13;

    // Unicode for color neutral chess pieces
    public static final char NOTATION_KING = 0xe050;
    public static final char NOTATION_QUEEN = 0xe051;
    public static final char NOTATION_ROOK = 0xe052;
    public static final char NOTATION_BISHOP = 0xe053;
    public static final char NOTATION_KNIGHT = 0xe054;
    public static final char NOTATION_PAWN = 0xe055;

    // Unicode for white chess pieces
    public static final char WHITE_KING = 0x2654;
    public static final char WHITE_QUEEN = 0x2655;
    public static final char WHITE_ROOK = 0x2656;
    public static final char WHITE_BISHOP = 0x2657;
    public static final char WHITE_KNIGHT = 0x2658;
    public static final char WHITE_PAWN = 0x2659;

    // Unicode for black chess pieces
    public static final char BLACK_KING = 0x265A;
    public static final char BLACK_QUEEN = 0x265B;
    public static final char BLACK_ROOK = 0x265C;
    public static final char BLACK_BISHOP = 0x265D;
    public static final char BLACK_KNIGHT = 0x265E;
    public static final char BLACK_PAWN = 0x265F;

    /**
     * Return true if p is a white piece, false otherwise.
     * Note that if p is EMPTY, an unspecified value is returned.
     */
    public static boolean isWhite(int pType) {
        return pType < BKING;
    }
    public static int makeWhite(int pType) {
        return pType < BKING ? pType : pType - (BKING - WKING);
    }
    public static int makeBlack(int pType) {
        return ((pType >= WKING) && (pType <= WPAWN)) ? pType + (BKING - WKING) : pType;
    }
    public static int swapColor(int pType) {
        if (pType == EMPTY)
            return EMPTY;
        return isWhite(pType) ? pType + (BKING - WKING) : pType - (BKING - WKING);
    }

    /** Converts the piece into a character for the material diff. */
    public final static char toUniCode(int p) {
        // As we assume, the coding of the pieces is sequential, lets do some math.
        return (char)(WHITE_KING + p - 1);
    }
}
