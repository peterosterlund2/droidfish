/*
    DroidFish - An Android chess program.
    Copyright (C) 2020  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish;

import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.TextIO;

public class PieceFontInfo {

    // Unicode for color neutral chess pieces
    public static final char NOTATION_KING = 0xe050;
    public static final char NOTATION_QUEEN = 0xe051;
    public static final char NOTATION_ROOK = 0xe052;
    public static final char NOTATION_BISHOP = 0xe053;
    public static final char NOTATION_KNIGHT = 0xe054;
    public static final char NOTATION_PAWN = 0xe055;

    // Unicode for white chess pieces
    private static final char WHITE_KING = 0x2654;
//  private static final char WHITE_QUEEN = 0x2655;
//  private static final char WHITE_ROOK = 0x2656;
//  private static final char WHITE_BISHOP = 0x2657;
//  private static final char WHITE_KNIGHT = 0x2658;
//  private static final char WHITE_PAWN = 0x2659;

    // Unicode for black chess pieces
//  private static final char BLACK_KING = 0x265A;
//  private static final char BLACK_QUEEN = 0x265B;
//  private static final char BLACK_ROOK = 0x265C;
//  private static final char BLACK_BISHOP = 0x265D;
//  private static final char BLACK_KNIGHT = 0x265E;
//  private static final char BLACK_PAWN = 0x265F;

    /** Converts the piece into a character for the figurine font. */
    public static char toUniCode(int p) {
        // As we assume the coding of the pieces is sequential, lets do some math.
        return (char)(WHITE_KING + p - 1);
    }

    /** Convert a piece and a square to a string, such as Nf3. */
    public static String pieceAndSquareToString(int currentPieceType, int p, int sq) {
        StringBuilder ret = new StringBuilder();
        if (currentPieceType == PGNOptions.PT_FIGURINE) {
            ret.append(PieceFontInfo.toUniCode(p));
        } else {
            boolean localized = currentPieceType != PGNOptions.PT_ENGLISH;
            ret.append(localized ? TextIO.pieceToCharLocalized(p, true)
                                 : TextIO.pieceToChar(p, true));
        }
        ret.append(TextIO.squareToString(sq));
        return ret.toString();
    }
}
