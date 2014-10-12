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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stores the state of a chess position.
 * All required state is stored, except for all previous positions
 * since the last capture or pawn move. That state is only needed
 * for three-fold repetition draw detection, and is better stored
 * in a separate hash table.
 * @author petero
 */
public class Position {
    private int[] squares;

    public boolean whiteMove;

    /** Bit definitions for the castleMask bit mask. */
    public static final int A1_CASTLE = 0; /** White long castle. */
    public static final int H1_CASTLE = 1; /** White short castle. */
    public static final int A8_CASTLE = 2; /** Black long castle. */
    public static final int H8_CASTLE = 3; /** Black short castle. */

    private int castleMask;

    private int epSquare;

    /** Number of half-moves since last 50-move reset. */
    public int halfMoveClock;

    /** Game move number, starting from 1. */
    public int fullMoveCounter;

    private long hashKey;           // Cached Zobrist hash key
    private int wKingSq, bKingSq;   // Cached king positions

    /** Initialize board to empty position. */
    public Position() {
        squares = new int[64];
        for (int i = 0; i < 64; i++)
            squares[i] = Piece.EMPTY;
        whiteMove = true;
        castleMask = 0;
        epSquare = -1;
        halfMoveClock = 0;
        fullMoveCounter = 1;
        hashKey = computeZobristHash();
        wKingSq = bKingSq = -1;
    }

    public Position(Position other) {
        squares = new int[64];
        System.arraycopy(other.squares, 0, squares, 0, 64);
        whiteMove = other.whiteMove;
        castleMask = other.castleMask;
        epSquare = other.epSquare;
        halfMoveClock = other.halfMoveClock;
        fullMoveCounter = other.fullMoveCounter;
        hashKey = other.hashKey;
        wKingSq = other.wKingSq;
        bKingSq = other.bKingSq;
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        Position other = (Position)o;
        if (!drawRuleEquals(other))
            return false;
        if (halfMoveClock != other.halfMoveClock)
            return false;
        if (fullMoveCounter != other.fullMoveCounter)
            return false;
        if (hashKey != other.hashKey)
            return false;
        return true;
    }
    @Override
    public int hashCode() {
        return (int)hashKey;
    }

    /**
     * Return Zobrish hash value for the current position.
     * Everything except the move counters are included in the hash value.
     */
    public final long zobristHash() {
        return hashKey;
    }

    /**
     * Decide if two positions are equal in the sense of the draw by repetition rule.
     * @return True if positions are equal, false otherwise.
     */
    final public boolean drawRuleEquals(Position other) {
        for (int i = 0; i < 64; i++) {
            if (squares[i] != other.squares[i])
                return false;
        }
        if (whiteMove != other.whiteMove)
            return false;
        if (castleMask != other.castleMask)
            return false;
        if (epSquare != other.epSquare)
            return false;
        return true;
    }

    public final void setWhiteMove(boolean whiteMove) {
        if (whiteMove != this.whiteMove) {
            hashKey ^= whiteHashKey;
            this.whiteMove = whiteMove;
        }
    }
    /** Return index in squares[] vector corresponding to (x,y). */
    public final static int getSquare(int x, int y) {
        return y * 8 + x;
    }
    /** Return x position (file) corresponding to a square. */
    public final static int getX(int square) {
        return square & 7;
    }
    /** Return y position (rank) corresponding to a square. */
    public final static int getY(int square) {
        return square >> 3;
    }
    /** Return true if (x,y) is a dark square. */
    public final static boolean darkSquare(int x, int y) {
        return (x & 1) == (y & 1);
    }

    /** Return piece occupying a square. */
    public final int getPiece(int square) {
        return squares[square];
    }
    /** Set a square to a piece value. */
    public final void setPiece(int square, int piece) {
        // Update hash key
        int oldPiece = squares[square];
        hashKey ^= psHashKeys[oldPiece][square];
        hashKey ^= psHashKeys[piece][square];

        // Update board
        squares[square] = piece;

        // Update king position
        if (piece == Piece.WKING) {
            wKingSq = square;
        } else if (piece == Piece.BKING) {
            bKingSq = square;
        }
    }

    /** Return true if white long castling right has not been lost. */
    public final boolean a1Castle() {
        return (castleMask & (1 << A1_CASTLE)) != 0;
    }
    /** Return true if white short castling right has not been lost. */
    public final boolean h1Castle() {
        return (castleMask & (1 << H1_CASTLE)) != 0;
    }
    /** Return true if black long castling right has not been lost. */
    public final boolean a8Castle() {
        return (castleMask & (1 << A8_CASTLE)) != 0;
    }
    /** Return true if black short castling right has not been lost. */
    public final boolean h8Castle() {
        return (castleMask & (1 << H8_CASTLE)) != 0;
    }
    /** Bitmask describing castling rights. */
    public final int getCastleMask() {
        return castleMask;
    }
    public final void setCastleMask(int castleMask) {
        hashKey ^= castleHashKeys[this.castleMask];
        hashKey ^= castleHashKeys[castleMask];
        this.castleMask = castleMask;
    }

    /** En passant square, or -1 if no ep possible. */
    public final int getEpSquare() {
        return epSquare;
    }
    public final void setEpSquare(int epSquare) {
        if (this.epSquare != epSquare) {
            hashKey ^= epHashKeys[(this.epSquare >= 0) ? getX(this.epSquare) + 1 : 0];
            hashKey ^= epHashKeys[(epSquare >= 0) ? getX(epSquare) + 1 : 0];
            this.epSquare = epSquare;
        }
    }


    public final int getKingSq(boolean whiteMove) {
        return whiteMove ? wKingSq : bKingSq;
    }

    /** Count number of pieces of a certain type. */
    public final int nPieces(int pType) {
        int ret = 0;
        for (int sq = 0; sq < 64; sq++)
            if (squares[sq] == pType)
                ret++;
        return ret;
    }

    /** Count total number of pieces. */
    public final int nPieces() {
        int ret = 0;
        for (int sq = 0; sq < 64; sq++)
            if (squares[sq] != Piece.EMPTY)
                ret++;
        return ret;
    }

    /** Apply a move to the current position. */
    public final void makeMove(Move move, UndoInfo ui) {
        ui.capturedPiece = squares[move.to];
        ui.castleMask = castleMask;
        ui.epSquare = epSquare;
        ui.halfMoveClock = halfMoveClock;
        boolean wtm = whiteMove;

        int p = squares[move.from];
        int capP = squares[move.to];

        boolean nullMove = (move.from == 0) && (move.to == 0);

        if (nullMove || (capP != Piece.EMPTY) || (p == (wtm ? Piece.WPAWN : Piece.BPAWN))) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
        if (!wtm) {
            fullMoveCounter++;
        }

        // Handle castling
        int king = wtm ? Piece.WKING : Piece.BKING;
        int k0 = move.from;
        if (p == king) {
            if (move.to == k0 + 2) { // O-O
                setPiece(k0 + 1, squares[k0 + 3]);
                setPiece(k0 + 3, Piece.EMPTY);
            } else if (move.to == k0 - 2) { // O-O-O
                setPiece(k0 - 1, squares[k0 - 4]);
                setPiece(k0 - 4, Piece.EMPTY);
            }
            if (wtm) {
                setCastleMask(castleMask & ~(1 << Position.A1_CASTLE));
                setCastleMask(castleMask & ~(1 << Position.H1_CASTLE));
            } else {
                setCastleMask(castleMask & ~(1 << Position.A8_CASTLE));
                setCastleMask(castleMask & ~(1 << Position.H8_CASTLE));
            }
        }
        if (!nullMove) {
            int rook = wtm ? Piece.WROOK : Piece.BROOK;
            if (p == rook) {
                removeCastleRights(move.from);
            }
            int oRook = wtm ? Piece.BROOK : Piece.WROOK;
            if (capP == oRook) {
                removeCastleRights(move.to);
            }
        }

        // Handle en passant and epSquare
        int prevEpSquare = epSquare;
        setEpSquare(-1);
        if (p == Piece.WPAWN) {
            if (move.to - move.from == 2 * 8) {
                int x = Position.getX(move.to);
                if (    ((x > 0) && (squares[move.to - 1] == Piece.BPAWN)) ||
                        ((x < 7) && (squares[move.to + 1] == Piece.BPAWN))) {
                    setEpSquare(move.from + 8);
                }
            } else if (move.to == prevEpSquare) {
                setPiece(move.to - 8, Piece.EMPTY);
            }
        } else if (p == Piece.BPAWN) {
            if (move.to - move.from == -2 * 8) {
                int x = Position.getX(move.to);
                if (    ((x > 0) && (squares[move.to - 1] == Piece.WPAWN)) ||
                        ((x < 7) && (squares[move.to + 1] == Piece.WPAWN))) {
                    setEpSquare(move.from - 8);
                }
            } else if (move.to == prevEpSquare) {
                setPiece(move.to + 8, Piece.EMPTY);
            }
        }

        // Perform move
        setPiece(move.from, Piece.EMPTY);
        // Handle promotion
        if (move.promoteTo != Piece.EMPTY) {
            setPiece(move.to, move.promoteTo);
        } else {
            setPiece(move.to, p);
        }
        setWhiteMove(!wtm);
    }

    public final void unMakeMove(Move move, UndoInfo ui) {
        setWhiteMove(!whiteMove);
        int p = squares[move.to];
        setPiece(move.from, p);
        setPiece(move.to, ui.capturedPiece);
        setCastleMask(ui.castleMask);
        setEpSquare(ui.epSquare);
        halfMoveClock = ui.halfMoveClock;
        boolean wtm = whiteMove;
        if (move.promoteTo != Piece.EMPTY) {
            p = wtm ? Piece.WPAWN : Piece.BPAWN;
            setPiece(move.from, p);
        }
        if (!wtm) {
            fullMoveCounter--;
        }

        // Handle castling
        int king = wtm ? Piece.WKING : Piece.BKING;
        int k0 = move.from;
        if (p == king) {
            if (move.to == k0 + 2) { // O-O
                setPiece(k0 + 3, squares[k0 + 1]);
                setPiece(k0 + 1, Piece.EMPTY);
            } else if (move.to == k0 - 2) { // O-O-O
                setPiece(k0 - 4, squares[k0 - 1]);
                setPiece(k0 - 1, Piece.EMPTY);
            }
        }

        // Handle en passant
        if (move.to == epSquare) {
            if (p == Piece.WPAWN) {
                setPiece(move.to - 8, Piece.BPAWN);
            } else if (p == Piece.BPAWN) {
                setPiece(move.to + 8, Piece.WPAWN);
            }
        }
    }

    private final void removeCastleRights(int square) {
        if (square == Position.getSquare(0, 0)) {
            setCastleMask(castleMask & ~(1 << Position.A1_CASTLE));
        } else if (square == Position.getSquare(7, 0)) {
            setCastleMask(castleMask & ~(1 << Position.H1_CASTLE));
        } else if (square == Position.getSquare(0, 7)) {
            setCastleMask(castleMask & ~(1 << Position.A8_CASTLE));
        } else if (square == Position.getSquare(7, 7)) {
            setCastleMask(castleMask & ~(1 << Position.H8_CASTLE));
        }
    }

    /* ------------- Hashing code ------------------ */

    private static long[][] psHashKeys;    // [piece][square]
    private static long whiteHashKey;
    private static long[] castleHashKeys;  // [castleMask]
    private static long[] epHashKeys;      // [epFile + 1] (epFile==-1 for no ep)

    static {
        psHashKeys = new long[Piece.nPieceTypes][64];
        castleHashKeys = new long[16];
        epHashKeys = new long[9];
        int rndNo = 0;
        for (int p = 0; p < Piece.nPieceTypes; p++) {
            for (int sq = 0; sq < 64; sq++) {
                psHashKeys[p][sq] = getRandomHashVal(rndNo++);
            }
        }
        whiteHashKey = getRandomHashVal(rndNo++);
        for (int cm = 0; cm < castleHashKeys.length; cm++)
            castleHashKeys[cm] = getRandomHashVal(rndNo++);
        for (int f = 0; f < epHashKeys.length; f++)
            epHashKeys[f] = getRandomHashVal(rndNo++);
    }

    /**
     * Compute the Zobrist hash value non-incrementally. Only useful for test programs.
     */
    final long computeZobristHash() {
        long hash = 0;
        for (int sq = 0; sq < 64; sq++) {
            int p = squares[sq];
            hash ^= psHashKeys[p][sq];
        }
        if (whiteMove)
            hash ^= whiteHashKey;
        hash ^= castleHashKeys[castleMask];
        hash ^= epHashKeys[(epSquare >= 0) ? getX(epSquare) + 1 : 0];
        return hash;
    }

    private final static long getRandomHashVal(int rndNo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] input = new byte[4];
            for (int i = 0; i < 4; i++)
                input[i] = (byte)((rndNo >> (i * 8)) & 0xff);
            byte[] digest = md.digest(input);
            long ret = 0;
            for (int i = 0; i < 8; i++) {
                ret ^= ((long)digest[i]) << (i * 8);
            }
            return ret;
        } catch (NoSuchAlgorithmException ex) {
            throw new UnsupportedOperationException("SHA-1 not available");
        }
    }

    /** Useful for debugging. */
    public final String toString() {
        return TextIO.asciiBoard(this);
    }
}
