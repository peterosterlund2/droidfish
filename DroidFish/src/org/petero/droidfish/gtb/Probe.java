/*
    GtbCuckoo - Interface to Gaviota endgame tablebases.
    Copyright (C) 2011-2012  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.gtb;

import java.util.ArrayList;

import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.MoveGen;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.UndoInfo;

/** Interface between Position class and GTB probing code. */
public class Probe {
    private final GtbProbe gtb;
    private final int whiteSquares[];
    private final int blackSquares[];
    private final byte whitePieces[];
    private final byte blackPieces[];

    private static final Probe INSTANCE = new Probe();

    /** Get singleton instance. */
    public static Probe getInstance() {
        return INSTANCE;
    }

    /** Constructor. */
    private Probe() {
        gtb = new GtbProbe();
        whiteSquares = new int[65];
        blackSquares = new int[65];
        whitePieces = new byte[65];
        blackPieces = new byte[65];
    }

    public void setPath(String tbPath, boolean forceReload) {
        gtb.setPath(tbPath, forceReload);
    }

    public static final class ProbeResult {
        public final static int DRAW    = 0;
        public final static int WMATE   = 1;
        public final static int BMATE   = 2;
        public final static int UNKNOWN = 3;

        public int result;
        public int movesToMate; // Full moves to mate, or 0 if DRAW or UNKNOWN.
    }

    /**
     * Probe table bases.
     * @param pos    The position to probe.
     * @param result Two element array. Set to [tbinfo, plies].
     * @return True if success.
     */
    public final ProbeResult probeHard(Position pos) {
        int castleMask = 0;
        if (pos.a1Castle()) castleMask |= GtbProbe.A1_CASTLE;
        if (pos.h1Castle()) castleMask |= GtbProbe.H1_CASTLE;
        if (pos.a8Castle()) castleMask |= GtbProbe.A8_CASTLE;
        if (pos.h8Castle()) castleMask |= GtbProbe.H8_CASTLE;

        int nWhite = 0;
        int nBlack = 0;
        for (int sq = 0; sq < 64; sq++) {
            int p = pos.getPiece(sq);
            switch (p) {
            case Piece.WKING:
                whiteSquares[nWhite] = sq;
                whitePieces[nWhite++] = GtbProbe.KING;
                break;
            case Piece.WQUEEN:
                whiteSquares[nWhite] = sq;
                whitePieces[nWhite++] = GtbProbe.QUEEN;
                break;
            case Piece.WROOK:
                whiteSquares[nWhite] = sq;
                whitePieces[nWhite++] = GtbProbe.ROOK;
                break;
            case Piece.WBISHOP:
                whiteSquares[nWhite] = sq;
                whitePieces[nWhite++] = GtbProbe.BISHOP;
                break;
            case Piece.WKNIGHT:
                whiteSquares[nWhite] = sq;
                whitePieces[nWhite++] = GtbProbe.KNIGHT;
                break;
            case Piece.WPAWN:
                whiteSquares[nWhite] = sq;
                whitePieces[nWhite++] = GtbProbe.PAWN;
                break;

            case Piece.BKING:
                blackSquares[nBlack] = sq;
                blackPieces[nBlack++] = GtbProbe.KING;
                break;
            case Piece.BQUEEN:
                blackSquares[nBlack] = sq;
                blackPieces[nBlack++] = GtbProbe.QUEEN;
                break;
            case Piece.BROOK:
                blackSquares[nBlack] = sq;
                blackPieces[nBlack++] = GtbProbe.ROOK;
                break;
            case Piece.BBISHOP:
                blackSquares[nBlack] = sq;
                blackPieces[nBlack++] = GtbProbe.BISHOP;
                break;
            case Piece.BKNIGHT:
                blackSquares[nBlack] = sq;
                blackPieces[nBlack++] = GtbProbe.KNIGHT;
                break;
            case Piece.BPAWN:
                blackSquares[nBlack] = sq;
                blackPieces[nBlack++] = GtbProbe.PAWN;
                break;
            }
        }
        whiteSquares[nWhite] = GtbProbe.NOSQUARE;
        blackSquares[nBlack] = GtbProbe.NOSQUARE;
        whitePieces[nWhite] = GtbProbe.NOPIECE;
        blackPieces[nBlack] = GtbProbe.NOPIECE;
        int epSquare = pos.getEpSquare();
        if (epSquare == -1)
            epSquare = GtbProbe.NOSQUARE;

        int[] result = new int[2];
        boolean res = false;
        if (nWhite + nBlack <= 5) {
            gtb.initIfNeeded();
            res = gtb.probeHard(pos.whiteMove, epSquare, castleMask,
                                whiteSquares, blackSquares, whitePieces, blackPieces,
                                result);
        }
        ProbeResult ret = new ProbeResult();
        if (res) {
            switch (result[0]) {
            case GtbProbe.DRAW:
                ret.result = ProbeResult.DRAW;
                ret.movesToMate = 0;
                break;
            case GtbProbe.WMATE:
                ret.result = ProbeResult.WMATE;
                ret.movesToMate = (result[1] + 1) / 2;
                break;
            case GtbProbe.BMATE:
                ret.result = ProbeResult.BMATE;
                ret.movesToMate = (result[1] + 1) / 2;
                break;
            default:
                ret.result = ProbeResult.UNKNOWN;
                ret.movesToMate = 0;
                break;
            }
        } else {
            ret.result = ProbeResult.UNKNOWN;
            ret.movesToMate = 0;
        }
        return ret;
    }

    /** Return a list of all moves in moveList that are not known to be non-optimal.
     * Returns null if no legal move could be excluded. */
    public final ArrayList<Move> removeNonOptimal(Position pos, ArrayList<Move> moveList) {
        ArrayList<Move> optimalMoves = new ArrayList<Move>();
        ArrayList<Move> unknownMoves = new ArrayList<Move>();
        final int MATE0 = 100000;
        int bestScore = -1000000;
        UndoInfo ui = new UndoInfo();
        for (Move m : moveList) {
            pos.makeMove(m, ui);
            ProbeResult res = probeHard(pos);
            pos.unMakeMove(m, ui);
            if (res.result == ProbeResult.UNKNOWN) {
                unknownMoves.add(m);
            } else {
                int wScore;
                if (res.result == ProbeResult.WMATE)
                    wScore = MATE0 - res.movesToMate;
                else if (res.result == ProbeResult.BMATE)
                    wScore = -(MATE0 - res.movesToMate);
                else
                    wScore = 0;
                int score = pos.whiteMove ? wScore : -wScore;
                if (score > bestScore) {
                    optimalMoves.clear();
                    optimalMoves.add(m);
                    bestScore = score;
                } else if (score == bestScore) {
                    optimalMoves.add(m);
                } else {
                    // Ignore move
                }
            }
        }
        for (Move m : unknownMoves)
            optimalMoves.add(m);
        return (optimalMoves.size() < moveList.size()) ? optimalMoves : null;
    }

    /** For a given position and from square, return EGTB information
     * about all legal destination squares. Return null if no information available. */
    public final ArrayList<Pair<Integer,Integer>> movePieceProbe(Position pos, int fromSq) {
        int p = pos.getPiece(fromSq);
        if ((p == Piece.EMPTY) || (pos.whiteMove != Piece.isWhite(p)))
            return null;
        ArrayList<Pair<Integer,Integer>> ret = new ArrayList<Pair<Integer,Integer>>();

        ArrayList<Move> moveList = new MoveGen().legalMoves(pos);
        UndoInfo ui = new UndoInfo();
        for (Move m : moveList) {
            if (m.from != fromSq)
                continue;
            pos.makeMove(m, ui);
            ProbeResult res = probeHard(pos);
            pos.unMakeMove(m, ui);
            if (res.result == ProbeResult.UNKNOWN)
                continue;
            int score = 0;
            if (res.result == ProbeResult.WMATE) {
                score = pos.whiteMove ? res.movesToMate + 1 : -res.movesToMate;
            } else if (res.result == ProbeResult.BMATE) {
                score = pos.whiteMove ? -res.movesToMate : res.movesToMate + 1;
            }
            ret.add(new Pair<Integer,Integer>(m.to, score));
        }
        return ret;
    }

    /** For a given position and from square, return EGTB information
     * about all legal alternative positions for the piece on from square.
     * Return null if no information is available. */
    public final ArrayList<Pair<Integer, Integer>> relocatePieceProbe(Position pos, int fromSq) {
        int p = pos.getPiece(fromSq);
        if (p == Piece.EMPTY)
            return null;
        boolean isPawn = (Piece.makeWhite(p) == Piece.WPAWN);
        ArrayList<Pair<Integer,Integer>> ret = new ArrayList<Pair<Integer,Integer>>();
        for (int sq = 0; sq < 64; sq++) {
            if ((sq != fromSq) && (pos.getPiece(sq) != Piece.EMPTY))
                continue;
            if (isPawn && ((sq < 8) || (sq >= 56)))
                continue;
            pos.setPiece(fromSq, Piece.EMPTY);
            pos.setPiece(sq, p);
            ProbeResult res = probeHard(pos);
            pos.setPiece(sq, Piece.EMPTY);
            pos.setPiece(fromSq, p);
            if (res.result == ProbeResult.UNKNOWN)
                continue;
            int score = 0;
            if (res.result == ProbeResult.WMATE) {
                score = res.movesToMate;
            } else if (res.result == ProbeResult.BMATE) {
                score = -res.movesToMate;
            }
            ret.add(new Pair<Integer,Integer>(sq, score));
        }
        return ret;
    }
}
