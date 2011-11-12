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

    /**
     * Return true if p is a white piece, false otherwise.
     * Note that if p is EMPTY, an unspecified value is returned.
     */
    public static final boolean isWhite(int pType) {
        return pType < BKING;
    }
    public static final int makeWhite(int pType) {
        return pType < BKING ? pType : pType - (BKING - WKING);
    }
    public static final int makeBlack(int pType) {
        return ((pType > EMPTY) && (pType < BKING)) ? pType + (BKING - WKING) : pType;
    }
}
