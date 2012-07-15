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
    public int[] squares;

    // Bitboards
    public long[] pieceTypeBB;
    public long whiteBB, blackBB;
    
    // Piece square table scores
    public short[] psScore1, psScore2;

    public boolean whiteMove;

    /** Bit definitions for the castleMask bit mask. */
    public static final int A1_CASTLE = 0; /** White long castle. */
    public static final int H1_CASTLE = 1; /** White short castle. */
    public static final int A8_CASTLE = 2; /** Black long castle. */
    public static final int H8_CASTLE = 3; /** Black short castle. */
    
    private int castleMask;

    private int epSquare;
    
    /** Number of half-moves since last 50-move reset. */
    int halfMoveClock;
    
    /** Game move number, starting from 1. */
    public int fullMoveCounter;

    private long hashKey;           // Cached Zobrist hash key
    private long pHashKey;
    public int wKingSq, bKingSq;   // Cached king positions
    public int wMtrl;      // Total value of all white pieces and pawns
    public int bMtrl;      // Total value of all black pieces and pawns
    public int wMtrlPawns; // Total value of all white pawns
    public int bMtrlPawns; // Total value of all black pawns

    /** Initialize board to empty position. */
    public Position() {
        squares = new int[64];
        for (int i = 0; i < 64; i++)
            squares[i] = Piece.EMPTY;
        pieceTypeBB = new long[Piece.nPieceTypes];
        psScore1 = new short[Piece.nPieceTypes];
        psScore2 = new short[Piece.nPieceTypes];
        for (int i = 0; i < Piece.nPieceTypes; i++) {
            pieceTypeBB[i] = 0L;
            psScore1[i] = 0;
            psScore2[i] = 0;
        }
        whiteBB = blackBB = 0L;
        whiteMove = true;
        castleMask = 0;
        epSquare = -1;
        halfMoveClock = 0;
        fullMoveCounter = 1;
        hashKey = computeZobristHash();
        wKingSq = bKingSq = -1;
        wMtrl = bMtrl = -Evaluate.kV;
        wMtrlPawns = bMtrlPawns = 0;
    }

    public Position(Position other) {
        squares = new int[64];
        for (int i = 0; i < 64; i++)
            squares[i] = other.squares[i];
        pieceTypeBB = new long[Piece.nPieceTypes];
        psScore1 = new short[Piece.nPieceTypes];
        psScore2 = new short[Piece.nPieceTypes];
        for (int i = 0; i < Piece.nPieceTypes; i++) {
            pieceTypeBB[i] = other.pieceTypeBB[i];
            psScore1[i] = other.psScore1[i];
            psScore2[i] = other.psScore2[i];
        }
        whiteBB = other.whiteBB;
        blackBB = other.blackBB;
        whiteMove = other.whiteMove;
        castleMask = other.castleMask;
        epSquare = other.epSquare;
        halfMoveClock = other.halfMoveClock;
        fullMoveCounter = other.fullMoveCounter;
        hashKey = other.hashKey;
        pHashKey = other.pHashKey;
        wKingSq = other.wKingSq;
        bKingSq = other.bKingSq;
        wMtrl = other.wMtrl;
        bMtrl = other.bMtrl;
        wMtrlPawns = other.wMtrlPawns;
        bMtrlPawns = other.bMtrlPawns;
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
        if (pHashKey != other.pHashKey)
            return false;
        return true;
    }
    @Override
    public int hashCode() {
        return (int)hashKey;
    }

    /**
     * Return Zobrist hash value for the current position.
     * Everything except the move counters are included in the hash value.
     */
    public final long zobristHash() {
        return hashKey;
    }
    public final long pawnZobristHash() {
        return pHashKey;
    }
    public final long kingZobristHash() {
        return psHashKeys[Piece.WKING][wKingSq] ^ 
               psHashKeys[Piece.BKING][bKingSq];
    }

    public final long historyHash() {
        long ret = hashKey;
        if (halfMoveClock >= 80) {
            ret ^= moveCntKeys[Math.min(halfMoveClock, 100)];
        }
        return ret;
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

    /** Move a non-pawn piece to an empty square. */
    private final void movePieceNotPawn(int from, int to) {
        final int piece = squares[from];
        hashKey ^= psHashKeys[piece][from];
        hashKey ^= psHashKeys[piece][to];
        hashKey ^= psHashKeys[Piece.EMPTY][from];
        hashKey ^= psHashKeys[Piece.EMPTY][to];
        
        squares[from] = Piece.EMPTY;
        squares[to] = piece;

        final long sqMaskF = 1L << from;
        final long sqMaskT = 1L << to;
        pieceTypeBB[piece] &= ~sqMaskF;
        pieceTypeBB[piece] |= sqMaskT;
        if (Piece.isWhite(piece)) {
            whiteBB &= ~sqMaskF;
            whiteBB |= sqMaskT;
            if (piece == Piece.WKING)
                wKingSq = to;
        } else {
            blackBB &= ~sqMaskF;
            blackBB |= sqMaskT;
            if (piece == Piece.BKING)
                bKingSq = to;
        }

        psScore1[piece] += Evaluate.psTab1[piece][to] - Evaluate.psTab1[piece][from];
        psScore2[piece] += Evaluate.psTab2[piece][to] - Evaluate.psTab2[piece][from];
    }

    /** Set a square to a piece value. */
    public final void setPiece(int square, int piece) {
        int removedPiece = squares[square];
        squares[square] = piece;

        // Update hash key
        hashKey ^= psHashKeys[removedPiece][square];
        hashKey ^= psHashKeys[piece][square];

        // Update bitboards
        final long sqMask = 1L << square;
        pieceTypeBB[removedPiece] &= ~sqMask;
        pieceTypeBB[piece] |= sqMask;

        if (removedPiece != Piece.EMPTY) {
            int pVal = Evaluate.pieceValue[removedPiece];
            if (Piece.isWhite(removedPiece)) {
                wMtrl -= pVal;
                whiteBB &= ~sqMask;
                if (removedPiece == Piece.WPAWN) {
                    wMtrlPawns -= pVal;
                    pHashKey ^= psHashKeys[Piece.WPAWN][square];
                }
            } else {
                bMtrl -= pVal;
                blackBB &= ~sqMask;
                if (removedPiece == Piece.BPAWN) {
                    bMtrlPawns -= pVal;
                    pHashKey ^= psHashKeys[Piece.BPAWN][square];
                }
            }
        }

        if (piece != Piece.EMPTY) {
            int pVal = Evaluate.pieceValue[piece];
            if (Piece.isWhite(piece)) {
                wMtrl += pVal;
                whiteBB |= sqMask;
                if (piece == Piece.WPAWN) {
                    wMtrlPawns += pVal;
                    pHashKey ^= psHashKeys[Piece.WPAWN][square];
                }
                if (piece == Piece.WKING)
                    wKingSq = square;
            } else {
                bMtrl += pVal;
                blackBB |= sqMask;
                if (piece == Piece.BPAWN) {
                    bMtrlPawns += pVal;
                    pHashKey ^= psHashKeys[Piece.BPAWN][square];
                }
                if (piece == Piece.BKING)
                    bKingSq = square;
            }
        }

        // Update piece/square table scores
        psScore1[removedPiece] -= Evaluate.psTab1[removedPiece][square];
        psScore2[removedPiece] -= Evaluate.psTab2[removedPiece][square];
        psScore1[piece]        += Evaluate.psTab1[piece][square];
        psScore2[piece]        += Evaluate.psTab2[piece][square];
    }

    /**
     * Set a square to a piece value.
     * Special version that only updates enough of the state for the SEE function to be happy.
     */
    public final void setSEEPiece(int square, int piece) {
        int removedPiece = squares[square];

        // Update board
        squares[square] = piece;

        // Update bitboards
        long sqMask = 1L << square;
        pieceTypeBB[removedPiece] &= ~sqMask;
        pieceTypeBB[piece] |= sqMask;
        if (removedPiece != Piece.EMPTY) {
            if (Piece.isWhite(removedPiece))
                whiteBB &= ~sqMask;
            else
                blackBB &= ~sqMask;
        }
        if (piece != Piece.EMPTY) {
            if (Piece.isWhite(piece))
                whiteBB |= sqMask;
            else
                blackBB |= sqMask;
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


    public final int getKingSq(boolean white) {
        return white ? wKingSq : bKingSq;
    }

    /** Apply a move to the current position. */
    public final void makeMove(Move move, UndoInfo ui) {
        ui.capturedPiece = squares[move.to];
        ui.castleMask = castleMask;
        ui.epSquare = epSquare;
        ui.halfMoveClock = halfMoveClock;
        boolean wtm = whiteMove;
        
        final int p = squares[move.from];
        int capP = squares[move.to];
        long fromMask = 1L << move.from;

        int prevEpSquare = epSquare;
        setEpSquare(-1);

        if ((capP != Piece.EMPTY) || (((pieceTypeBB[Piece.WPAWN] | pieceTypeBB[Piece.BPAWN]) & fromMask) != 0)) {
            halfMoveClock = 0;

            // Handle en passant and epSquare
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

            if (((pieceTypeBB[Piece.WKING] | pieceTypeBB[Piece.BKING]) & fromMask) != 0) {
                if (wtm) {
                    setCastleMask(castleMask & ~(1 << Position.A1_CASTLE));
                    setCastleMask(castleMask & ~(1 << Position.H1_CASTLE));
                } else {
                    setCastleMask(castleMask & ~(1 << Position.A8_CASTLE));
                    setCastleMask(castleMask & ~(1 << Position.H8_CASTLE));
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
        } else {
            halfMoveClock++;

            // Handle castling
            if (((pieceTypeBB[Piece.WKING] | pieceTypeBB[Piece.BKING]) & fromMask) != 0) {
                int k0 = move.from;
                if (move.to == k0 + 2) { // O-O
                    movePieceNotPawn(k0 + 3, k0 + 1);
                } else if (move.to == k0 - 2) { // O-O-O
                    movePieceNotPawn(k0 - 4, k0 - 1);
                }
                if (wtm) {
                    setCastleMask(castleMask & ~(1 << Position.A1_CASTLE));
                    setCastleMask(castleMask & ~(1 << Position.H1_CASTLE));
                } else {
                    setCastleMask(castleMask & ~(1 << Position.A8_CASTLE));
                    setCastleMask(castleMask & ~(1 << Position.H8_CASTLE));
                }
            }

            // Perform move
            movePieceNotPawn(move.from, move.to);
        }
        if (wtm) {
            // Update castling rights when rook moves
            if ((BitBoard.maskCorners & fromMask) != 0) {
                if (p == Piece.WROOK)
                    removeCastleRights(move.from);
            }
            if ((BitBoard.maskCorners & (1L << move.to)) != 0) {
                if (capP == Piece.BROOK)
                    removeCastleRights(move.to);
            }
        } else {
            fullMoveCounter++;
            // Update castling rights when rook moves
            if ((BitBoard.maskCorners & fromMask) != 0) {
                if (p == Piece.BROOK)
                    removeCastleRights(move.from);
            }
            if ((BitBoard.maskCorners & (1L << move.to)) != 0) {
                if (capP == Piece.WROOK)
                    removeCastleRights(move.to);
            }
        }

        hashKey ^= whiteHashKey;
        whiteMove = !wtm;
    }

    public final void unMakeMove(Move move, UndoInfo ui) {
        hashKey ^= whiteHashKey;
        whiteMove = !whiteMove;
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
        if (p == king) {
            int k0 = move.from;
            if (move.to == k0 + 2) { // O-O
                movePieceNotPawn(k0 + 1, k0 + 3);
            } else if (move.to == k0 - 2) { // O-O-O
                movePieceNotPawn(k0 - 1, k0 - 4);
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

    /**
     * Apply a move to the current position.
     * Special version that only updates enough of the state for the SEE function to be happy.
     */
    public final void makeSEEMove(Move move, UndoInfo ui) {
        ui.capturedPiece = squares[move.to];
        
        int p = squares[move.from];

        // Handle en passant
        if (move.to == epSquare) {
            if (p == Piece.WPAWN) {
                setSEEPiece(move.to - 8, Piece.EMPTY);
            } else if (p == Piece.BPAWN) {
                setSEEPiece(move.to + 8, Piece.EMPTY);
            }
        }

        // Perform move
        setSEEPiece(move.from, Piece.EMPTY);
        setSEEPiece(move.to, p);
        whiteMove = !whiteMove;
    }

    public final void unMakeSEEMove(Move move, UndoInfo ui) {
        whiteMove = !whiteMove;
        int p = squares[move.to];
        setSEEPiece(move.from, p);
        setSEEPiece(move.to, ui.capturedPiece);

        // Handle en passant
        if (move.to == epSquare) {
            if (p == Piece.WPAWN) {
                setSEEPiece(move.to - 8, Piece.BPAWN);
            } else if (p == Piece.BPAWN) {
                setSEEPiece(move.to + 8, Piece.WPAWN);
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
    
    static final long[][] psHashKeys;    // [piece][square]
    private static final long whiteHashKey;
    private static final long[] castleHashKeys;  // [castleMask]
    private static final long[] epHashKeys;      // [epFile + 1] (epFile==-1 for no ep)
    private static final long[] moveCntKeys;     // [min(halfMoveClock, 100)]

    static {
        psHashKeys = new long[Piece.nPieceTypes][64];
        castleHashKeys = new long[16];
        epHashKeys = new long[9];
        moveCntKeys = new long[101];
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
        for (int mc = 0; mc < moveCntKeys.length; mc++)
            moveCntKeys[mc] = getRandomHashVal(rndNo++);
    }

    /**
     * Compute the Zobrist hash value non-incrementally. Only useful for test programs.
     */
    final long computeZobristHash() {
        long hash = 0;
        for (int sq = 0; sq < 64; sq++) {
            int p = squares[sq];
            hash ^= psHashKeys[p][sq];
            if ((p == Piece.WPAWN) || (p == Piece.BPAWN))
                pHashKey ^= psHashKeys[p][sq];
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
        return TextIO.asciiBoard(this) + (whiteMove ? "white\n" : "black\n") +
                Long.toHexString(zobristHash()) + "\n";
    }
}
