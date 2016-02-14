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

package org.petero.droidfish.tb;

import java.util.ArrayList;

import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.MoveGen;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.UndoInfo;

/** Interface between Position class and GTB/RTB probing code. */
public class Probe {
    private final GtbProbe gtb;
    private final RtbProbe rtb;
    private final int whiteSquares[];
    private final int blackSquares[];
    private final byte whitePieces[];
    private final byte blackPieces[];

    private static final Probe instance = new Probe();

    /** Get singleton instance. */
    public static Probe getInstance() {
        return instance;
    }

    /** Constructor. */
    private Probe() {
        gtb = new GtbProbe();
        rtb = new RtbProbe();
        whiteSquares = new int[65];
        blackSquares = new int[65];
        whitePieces = new byte[65];
        blackPieces = new byte[65];
    }

    public void setPath(String gtbPath, String rtbPath, boolean forceReload) {
        gtb.setPath(gtbPath, forceReload);
        rtb.setPath(rtbPath, forceReload);
    }

    private static final class GtbProbeResult {
        public final static int DRAW    = 0;
        public final static int WMATE   = 1;
        public final static int BMATE   = 2;
        public final static int UNKNOWN = 3;

        public int result;
        public int pliesToMate; // Plies to mate, or 0 if DRAW or UNKNOWN.
    }

    /**
     * Probe GTB tablebases.
     * @param pos    The position to probe.
     * @param result Two element array. Set to [tbinfo, plies].
     * @return True if success.
     */
    private final GtbProbeResult gtbProbe(Position pos) {
        GtbProbeResult ret = gtbProbeRaw(pos);
        if (ret.result == GtbProbeResult.DRAW && pos.getEpSquare() != -1) {
            ArrayList<Move> moveList = MoveGen.instance.legalMoves(pos);
            int pawn = pos.whiteMove ? Piece.WPAWN : Piece.BPAWN;
            int maxMate = -1;
            UndoInfo ui = new UndoInfo();
            for (Move move : moveList) {
                if ((move.to != pos.getEpSquare()) || (pos.getPiece(move.from) != pawn))
                    return ret;
                pos.makeMove(move, ui);
                GtbProbeResult ret2 = gtbProbe(pos);
                pos.unMakeMove(move, ui);
                switch (ret2.result) {
                case GtbProbeResult.DRAW:
                    break;
                case GtbProbeResult.WMATE:
                case GtbProbeResult.BMATE:
                    maxMate = Math.max(maxMate, ret2.pliesToMate);
                    break;
                case GtbProbeResult.UNKNOWN:
                    ret.result = GtbProbeResult.UNKNOWN;
                    return ret;
                }
            }
            if (maxMate != -1) {
                ret.result = pos.whiteMove ? GtbProbeResult.BMATE : GtbProbeResult.WMATE;
                ret.pliesToMate = maxMate;
            }
        }
        return ret;
    }

    private final GtbProbeResult gtbProbeRaw(Position pos) {
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
        GtbProbeResult ret = new GtbProbeResult();
        if (res) {
            switch (result[0]) {
            case GtbProbe.DRAW:
                ret.result = GtbProbeResult.DRAW;
                ret.pliesToMate = 0;
                break;
            case GtbProbe.WMATE:
                ret.result = GtbProbeResult.WMATE;
                ret.pliesToMate = result[1];
                break;
            case GtbProbe.BMATE:
                ret.result = GtbProbeResult.BMATE;
                ret.pliesToMate = result[1];
                break;
            default:
                ret.result = GtbProbeResult.UNKNOWN;
                ret.pliesToMate = 0;
                break;
            }
        } else {
            ret.result = GtbProbeResult.UNKNOWN;
            ret.pliesToMate = 0;
        }
        return ret;
    }

    private final ProbeResult rtbProbe(Position pos) {
        if (pos.nPieces() > 6)
            return new ProbeResult(ProbeResult.Type.NONE, 0, 0);

        // Make sure position is valid. Required by native move generation code.
        try {
            TextIO.readFEN(TextIO.toFEN(pos));
        } catch (ChessParseError ex) {
            return new ProbeResult(ProbeResult.Type.NONE, 0, 0);
        }

        rtb.initIfNeeded();

        byte[] squares = new byte[64];
        for (int sq = 0; sq < 64; sq++)
            squares[sq] = (byte)pos.getPiece(sq);
        int[] result = new int[2];
        rtb.probe(squares, pos.whiteMove, pos.getEpSquare(), pos.getCastleMask(),
                  pos.halfMoveClock, pos.fullMoveCounter, result);
        int wdl = 0;
        if (result[1] != RtbProbe.NOINFO) {
            int score = result[1];
            if (score > 0) {
                wdl = 1;
            } else if (score < 0) {
                wdl = -1;
                score = -score;
            }
            return new ProbeResult(ProbeResult.Type.DTZ, wdl, score);
        } else if (result[0] != RtbProbe.NOINFO) {
            return new ProbeResult(ProbeResult.Type.WDL, result[0], 0);
        } else {
            return new ProbeResult(ProbeResult.Type.NONE, 0, 0);
        }
    }

    final ProbeResult probe(Position pos) {
        GtbProbeResult gtbRes = gtbProbe(pos);
        if (gtbRes.result != GtbProbeResult.UNKNOWN) {
            int wdl = 0;
            int score = 0;
            if (gtbRes.result == GtbProbeResult.WMATE) {
                wdl = 1;
                score = gtbRes.pliesToMate;
            } else if (gtbRes.result == GtbProbeResult.BMATE) {
                wdl = -1;
                score = gtbRes.pliesToMate;
            }
            if (!pos.whiteMove)
                wdl = -wdl;
            return new ProbeResult(ProbeResult.Type.DTM, wdl, score);
        }
        return rtbProbe(pos);
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
            int pliesToDraw = Math.max(100 - pos.halfMoveClock, 1);
            GtbProbeResult res = gtbProbe(pos);
            pos.unMakeMove(m, ui);
            if (res.result == GtbProbeResult.UNKNOWN) {
                unknownMoves.add(m);
            } else {
                int wScore;
                if (res.result == GtbProbeResult.WMATE) {
                    if (res.pliesToMate <= pliesToDraw)
                        wScore = MATE0 - res.pliesToMate;
                    else
                        wScore = 1;
                } else if (res.result == GtbProbeResult.BMATE) {
                    if (res.pliesToMate <= pliesToDraw)
                        wScore = -(MATE0 - res.pliesToMate);
                    else
                        wScore = -1;
                } else {
                    wScore = 0;
                }
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
    public final ArrayList<Pair<Integer,ProbeResult>> movePieceProbe(Position pos, int fromSq) {
        int p = pos.getPiece(fromSq);
        if ((p == Piece.EMPTY) || (pos.whiteMove != Piece.isWhite(p)))
            return null;
        ArrayList<Pair<Integer,ProbeResult>> ret = new ArrayList<Pair<Integer,ProbeResult>>();

        ArrayList<Move> moveList = new MoveGen().legalMoves(pos);
        UndoInfo ui = new UndoInfo();
        for (Move m : moveList) {
            if (m.from != fromSq)
                continue;
            pos.makeMove(m, ui);
            boolean isZeroing = pos.halfMoveClock == 0;
            ProbeResult res = probe(pos);
            pos.unMakeMove(m, ui);
            if (res.type == ProbeResult.Type.NONE)
                continue;
            res.wdl = -res.wdl;
            if (isZeroing && (res.type == ProbeResult.Type.DTZ)) {
                res.score = 1;
            } else if (res.type != ProbeResult.Type.WDL) {
                res.score++;
            }
            ret.add(new Pair<Integer,ProbeResult>(m.to, res));
        }
        return ret;
    }

    /** For a given position and from square, return EGTB information
     * about all legal alternative positions for the piece on from square.
     * Return null if no information is available. */
    public final ArrayList<Pair<Integer,ProbeResult>> relocatePieceProbe(Position pos, int fromSq) {
        int p = pos.getPiece(fromSq);
        if (p == Piece.EMPTY)
            return null;
        boolean isPawn = (Piece.makeWhite(p) == Piece.WPAWN);
        ArrayList<Pair<Integer,ProbeResult>> ret = new ArrayList<Pair<Integer,ProbeResult>>();
        for (int sq = 0; sq < 64; sq++) {
            if ((sq != fromSq) && (pos.getPiece(sq) != Piece.EMPTY))
                continue;
            if (isPawn && ((sq < 8) || (sq >= 56)))
                continue;
            pos.setPiece(fromSq, Piece.EMPTY);
            pos.setPiece(sq, p);
            ProbeResult res = probe(pos);
            pos.setPiece(sq, Piece.EMPTY);
            pos.setPiece(fromSq, p);
            if (res.type == ProbeResult.Type.NONE)
                continue;
            if (!pos.whiteMove)
                res.wdl = -res.wdl;
            ret.add(new Pair<Integer,ProbeResult>(sq, res));
        }
        return ret;
    }
}
