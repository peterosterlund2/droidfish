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

import chess.TranspositionTable.TTEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author petero
 */
public class Search {
    final static int plyScale = 8; // Fractional ply resolution

    Position pos;
    MoveGen moveGen;
    Evaluate eval;
    KillerTable kt;
    History ht;
    long[] posHashList;         // List of hashes for previous positions up to the last "zeroing" move.
    int posHashListSize;        // Number of used entries in posHashList
    int posHashFirstNew;        // First entry in posHashList that has not been played OTB.
    TranspositionTable tt;
    TreeLogger log = null;

    private static final class SearchTreeInfo {
        UndoInfo undoInfo;
        Move hashMove;         // Temporary storage for local hashMove variable
        boolean allowNullMove; // Don't allow two null-moves in a row
        Move bestMove;         // Copy of the best found move at this ply
        Move currentMove;      // Move currently being searched
        int lmr;               // LMR reduction amount
        long nodeIdx;
        SearchTreeInfo() {
            undoInfo = new UndoInfo();
            hashMove = new Move(0, 0, 0);
            allowNullMove = true;
            bestMove = new Move(0, 0, 0);
        }
    }
    SearchTreeInfo[] searchTreeInfo;

    // Time management
    long tStart;            // Time when search started
    long minTimeMillis;     // Minimum recommended thinking time
    long maxTimeMillis;     // Maximum allowed thinking time
    boolean searchNeedMoreTime; // True if negaScout should use up to maxTimeMillis time.
    private long maxNodes;  // Maximum number of nodes to search (approximately)
    int nodesToGo;          // Number of nodes until next time check
    public int nodesBetweenTimeCheck = 5000; // How often to check remaining time

    // Reduced strength variables
    private int strength = 1000; // Strength (0-1000)
    boolean weak = false;        // Set to strength < 1000
    long randomSeed = 0;

    // Search statistics stuff
    long nodes;
    long qNodes;
    int[] nodesPlyVec;
    int[] nodesDepthVec;
    long totalNodes;
    long tLastStats;        // Time when notifyStats was last called
    boolean verbose;
    
    public final static int MATE0 = 32000;

    public final static int UNKNOWN_SCORE = -32767; // Represents unknown static eval score
    int q0Eval; // Static eval score at first level of quiescence search 

    public Search(Position pos, long[] posHashList, int posHashListSize, TranspositionTable tt,
                  History ht) {
        this.pos = new Position(pos);
        this.moveGen = new MoveGen();
        this.posHashList = posHashList;
        this.posHashListSize = posHashListSize;
        this.tt = tt;
        this.ht = ht;
        eval = new Evaluate();
        kt = new KillerTable();
        posHashFirstNew = posHashListSize;
        initNodeStats();
        minTimeMillis = -1;
        maxTimeMillis = -1;
        searchNeedMoreTime = false;
        maxNodes = -1;
        final int vecLen = 200;
        searchTreeInfo = new SearchTreeInfo[vecLen];
        for (int i = 0; i < vecLen; i++) {
            searchTreeInfo[i] = new SearchTreeInfo();
        }
    }

    static final class StopSearch extends Exception {
        private static final long serialVersionUID = -5546906604987117015L;
        public StopSearch() {
        }
        public StopSearch(String msg) {
            super(msg);
        }
    }

    /**
     * Used to get various search information during search
     */
    public interface Listener {
        public void notifyDepth(int depth);
        public void notifyCurrMove(Move m, int moveNr);
        public void notifyPV(int depth, int score, int time, long nodes, int nps,
                boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv);
        public void notifyStats(long nodes, int nps, int time);
    }

    Listener listener;
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private final static class MoveInfo {
        Move move;
        long nodes;
        MoveInfo(Move m, int n) { move = m;  nodes = n; }
        public static final class SortByScore implements Comparator<MoveInfo> {
            public int compare(MoveInfo mi1, MoveInfo mi2) {
                if ((mi1 == null) && (mi2 == null))
                    return 0;
                if (mi1 == null)
                    return 1;
                if (mi2 == null)
                    return -1;
                return mi2.move.score - mi1.move.score;
            }
        }
        public static final class SortByNodes implements Comparator<MoveInfo> {
            public int compare(MoveInfo mi1, MoveInfo mi2) {
                if ((mi1 == null) && (mi2 == null))
                    return 0;
                if (mi1 == null)
                    return 1;
                if (mi2 == null)
                    return -1;
                long d = mi2.nodes - mi1.nodes;
                if (d < 0)
                    return -1;
                else if (d > 0)
                    return 1;
                else
                    return 0;
            }
        }
    }

    final public void timeLimit(int minTimeLimit, int maxTimeLimit) {
        minTimeMillis = minTimeLimit;
        maxTimeMillis = maxTimeLimit;
    }

    final public void setStrength(int strength, long randomSeed) {
        if (strength < 0) strength = 0;
        if (strength > 1000) strength = 1000;
        this.strength = strength;
        weak = strength < 1000;
        this.randomSeed = randomSeed;
    }

    final public Move iterativeDeepening(MoveGen.MoveList scMovesIn,
            int maxDepth, long initialMaxNodes, boolean verbose) {
        tStart = System.currentTimeMillis();
//        log = TreeLogger.getWriter("/home/petero/treelog.dmp", pos);
        totalNodes = 0;
        if (scMovesIn.size <= 0)
            return null; // No moves to search

        MoveInfo[] scMoves;
        {
            // If strength is < 10%, only include a subset of the root moves.
            // At least one move is always included though.
            boolean[] includedMoves = new boolean[scMovesIn.size];
            long rndL = pos.zobristHash() ^ randomSeed;
            includedMoves[(int)(Math.abs(rndL) % scMovesIn.size)] = true;
            int nIncludedMoves = 1;
            double pIncl = (strength < 100) ? strength * strength * 1e-4 : 1.0;
            for (int mi = 0; mi < scMovesIn.size; mi++) {
                rndL = 6364136223846793005L * rndL + 1442695040888963407L;
                double rnd = ((rndL & 0x7fffffffffffffffL) % 1000000000) / 1e9;
                if (!includedMoves[mi] && (rnd < pIncl)) {
                    includedMoves[mi] = true;
                    nIncludedMoves++;
                }
            }
            scMoves = new MoveInfo[nIncludedMoves];
            for (int mi = 0, len = 0; mi < scMovesIn.size; mi++) {
                if (includedMoves[mi]) {
                    Move m = scMovesIn.m[mi];
                    scMoves[len++] = new MoveInfo(m, 0);
                }
            }
        }
        maxNodes = initialMaxNodes;
        nodesToGo = 0;
        Position origPos = new Position(pos);
        int bestScoreLastIter = 0;
        boolean firstIteration = true;
        Move bestMove = scMoves[0].move;
        this.verbose = verbose;
        if ((maxDepth < 0) || (maxDepth > 100)) {
            maxDepth = 100;
        }
        for (int i = 0; i < searchTreeInfo.length; i++) {
            searchTreeInfo[i].allowNullMove = true;
        }
        try {
        for (int depthS = plyScale; ; depthS += plyScale, firstIteration = false) {
            initNodeStats();
            if (listener != null) listener.notifyDepth(depthS/plyScale);
            int aspirationDelta = (Math.abs(bestScoreLastIter) <= MATE0 / 2) ? 20 : 1000;
            int alpha = firstIteration ? -Search.MATE0 : Math.max(bestScoreLastIter - aspirationDelta, -Search.MATE0);
            int bestScore = -Search.MATE0;
            UndoInfo ui = new UndoInfo();
            boolean needMoreTime = false;
            for (int mi = 0; mi < scMoves.length; mi++) {
                searchNeedMoreTime = (mi > 0);
                Move m = scMoves[mi].move;
                if ((listener != null) && (System.currentTimeMillis() - tStart >= 1000)) {
                    listener.notifyCurrMove(m, mi + 1);
                }
                nodes = qNodes = 0;
                posHashList[posHashListSize++] = pos.zobristHash();
                boolean givesCheck = MoveGen.givesCheck(pos, m);
                int beta;
                if (firstIteration) {
                    beta = Search.MATE0;
                } else {
                    beta = (mi == 0) ? Math.min(bestScoreLastIter + aspirationDelta, Search.MATE0) : alpha + 1;
                }

                int lmrS = 0;
                boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY);
                boolean isPromotion = (m.promoteTo != Piece.EMPTY);
                if ((depthS >= 3*plyScale) && !isCapture && !isPromotion) {
                    if (!givesCheck && !passedPawnPush(pos, m)) {
                        if (mi >= 3)
                            lmrS = plyScale;
                    }
                }
/*                long nodes0 = nodes;
                long qNodes0 = qNodes;
                System.out.printf("%2d %5s %5d %5d %6s %6s ",
                        mi, "-", alpha, beta, "-", "-");
                System.out.printf("%-6s...\n", TextIO.moveToUCIString(m)); */
                pos.makeMove(m, ui);
                SearchTreeInfo sti = searchTreeInfo[0];
                sti.currentMove = m;
                sti.lmr = lmrS;
                sti.nodeIdx = -1;
                int score = -negaScout(-beta, -alpha, 1, depthS - lmrS - plyScale, -1, givesCheck);
                if ((lmrS > 0) && (score > alpha)) {
                    sti.lmr = 0;
                    score = -negaScout(-beta, -alpha, 1, depthS - plyScale, -1, givesCheck);
                }
                long nodesThisMove = nodes + qNodes;
                posHashListSize--;
                pos.unMakeMove(m, ui);
                {
                    int type = TTEntry.T_EXACT;
                    if (score <= alpha) {
                        type = TTEntry.T_LE;
                    } else if (score >= beta) {
                        type = TTEntry.T_GE;
                    }
                    m.score = score;
                    tt.insert(pos.historyHash(), m, type, 0, depthS, UNKNOWN_SCORE);
                }
                if (score >= beta) {
                    int retryDelta = aspirationDelta * 2;
                    while (score >= beta) {
                        beta = Math.min(score + retryDelta, Search.MATE0);
                        retryDelta = Search.MATE0 * 2;
                        if (mi != 0)
                            needMoreTime = true;
                        bestMove = m;
                        if (verbose)
                            System.out.printf("%-6s %6d %6d %6d >=\n", TextIO.moveToString(pos, m, false),
                                    score, nodes, qNodes);
                        notifyPV(depthS/plyScale, score, false, true, m);
                        nodes = qNodes = 0;
                        posHashList[posHashListSize++] = pos.zobristHash();
                        pos.makeMove(m, ui);
                        int score2 = -negaScout(-beta, -score, 1, depthS - plyScale, -1, givesCheck);
                        score = Math.max(score, score2);
                        nodesThisMove += nodes + qNodes;
                        posHashListSize--;
                        pos.unMakeMove(m, ui);
                    }
                } else if ((mi == 0) && (score <= alpha)) {
                    int retryDelta = Search.MATE0 * 2;
                    while (score <= alpha) {
                        alpha = Math.max(score - retryDelta, -Search.MATE0);
                        retryDelta = Search.MATE0 * 2;
                        needMoreTime = searchNeedMoreTime = true;
                        if (verbose)
                            System.out.printf("%-6s %6d %6d %6d <=\n", TextIO.moveToString(pos, m, false),
                                    score, nodes, qNodes);
                        notifyPV(depthS/plyScale, score, true, false, m);
                        nodes = qNodes = 0;
                        posHashList[posHashListSize++] = pos.zobristHash();
                        pos.makeMove(m, ui);
                        score = -negaScout(-score, -alpha, 1, depthS - plyScale, -1, givesCheck);
                        nodesThisMove += nodes + qNodes;
                        posHashListSize--;
                        pos.unMakeMove(m, ui);
                    }
                }
                if (verbose || ((listener != null) && !firstIteration)) {
                    boolean havePV = false;
                    String PV = "";
                    if ((score > alpha) || (mi == 0)) {
                        havePV = true;
                        if (verbose) {
                            PV = TextIO.moveToString(pos, m, false) + " ";
                            pos.makeMove(m, ui);
                            PV += tt.extractPV(pos);
                            pos.unMakeMove(m, ui);
                        }
                    }
                    if (verbose) {
/*                        System.out.printf("%2d %5d %5d %5d %6d %6d ",
                                mi, score, alpha, beta, nodes-nodes0, qNodes-qNodes0);
                        System.out.printf("%-6s\n", TextIO.moveToUCIString(m)); */
                        System.out.printf("%-6s %6d %6d %6d%s %s\n",
                                TextIO.moveToString(pos, m, false), score,
                                nodes, qNodes, (score > alpha ? " *" : ""), PV);
                    }
                    if (havePV && !firstIteration) {
                        notifyPV(depthS/plyScale, score, false, false, m);
                    }
                }
                scMoves[mi].move.score = score;
                scMoves[mi].nodes = nodesThisMove;
                bestScore = Math.max(bestScore, score);
                if (!firstIteration) {
                    if ((score > alpha) || (mi == 0)) {
                        alpha = score;
                        MoveInfo tmp = scMoves[mi];
                        for (int i = mi - 1; i >= 0;  i--) {
                            scMoves[i + 1] = scMoves[i];
                        }
                        scMoves[0] = tmp;
                        bestMove = scMoves[0].move;
                    }
                }
                if (!firstIteration) {
                    long timeLimit = needMoreTime ? maxTimeMillis : minTimeMillis;
                    if (timeLimit >= 0) {
                        long tNow = System.currentTimeMillis();
                        if (tNow - tStart >= timeLimit)
                            break;
                    }
                }
            }
            if (firstIteration) {
                Arrays.sort(scMoves, new MoveInfo.SortByScore());
                bestMove = scMoves[0].move;
                notifyPV(depthS/plyScale, bestMove.score, false, false, bestMove);
            }
            long tNow = System.currentTimeMillis();
            if (verbose) {
                for (int i = 0; i < 20; i++) {
                    System.out.printf("%2d %7d %7d\n", i, nodesPlyVec[i], nodesDepthVec[i]);
                }
                System.out.printf("Time: %.3f depth:%.2f nps:%d\n", (tNow - tStart) * .001, depthS/(double)plyScale,
                        (int)(totalNodes / ((tNow - tStart) * .001)));
            }
            if (maxTimeMillis >= 0) {
                if (tNow - tStart >= minTimeMillis)
                    break;
            }
            if (depthS >= maxDepth * plyScale)
                break;
            if (maxNodes >= 0) {
                if (totalNodes >= maxNodes)
                    break;
            }
            int plyToMate = Search.MATE0 - Math.abs(bestScore);
            if (depthS >= plyToMate * plyScale)
                break;
            bestScoreLastIter = bestScore;

            if (!firstIteration) {
                // Moves that were hard to search should be searched early in the next iteration
                Arrays.sort(scMoves, 1, scMoves.length, new MoveInfo.SortByNodes());
            }
        }
        } catch (StopSearch ss) {
            pos = origPos;
        }
        notifyStats();

        if (log != null) {
            log.close();
            log = null;
        }
        return bestMove;
    }

    private final void notifyPV(int depth, int score, boolean uBound, boolean lBound, Move m) {
        if (listener != null) {
            boolean isMate = false;
            if (score > MATE0 / 2) {
                isMate = true;
                score = (MATE0 - score) / 2;
            } else if (score < -MATE0 / 2) {
                isMate = true;
                score = -((MATE0 + score - 1) / 2);
            }
            long tNow = System.currentTimeMillis();
            int time = (int) (tNow - tStart);
            int nps = (time > 0) ? (int)(totalNodes / (time / 1000.0)) : 0;
            ArrayList<Move> pv = tt.extractPVMoves(pos, m);
            listener.notifyPV(depth, score, time, totalNodes, nps, isMate, uBound, lBound, pv);
        }
    }

    private final void notifyStats() {
        long tNow = System.currentTimeMillis();
        if (listener != null) {
            int time = (int) (tNow - tStart);
            int nps = (time > 0) ? (int)(totalNodes / (time / 1000.0)) : 0;
            listener.notifyStats(totalNodes, nps, time);
        }
        tLastStats = tNow;
    }

    private static final Move emptyMove = new Move(0, 0, Piece.EMPTY, 0);

    /** 
     * Main recursive search algorithm.
     * @return Score for the side to make a move, in position given by "pos".
     */
    final public int negaScout(int alpha, int beta, int ply, int depth, int recaptureSquare,
                               final boolean inCheck) throws StopSearch {
        if (log != null) {
            SearchTreeInfo sti = searchTreeInfo[ply-1];
            long idx = log.logNodeStart(sti.nodeIdx, sti.currentMove, alpha, beta, ply, depth/plyScale);
            searchTreeInfo[ply].nodeIdx = idx;
        }
        if (--nodesToGo <= 0) {
            nodesToGo = nodesBetweenTimeCheck;
            long tNow = System.currentTimeMillis();
            long timeLimit = searchNeedMoreTime ? maxTimeMillis : minTimeMillis;
            if (    ((timeLimit >= 0) && (tNow - tStart >= timeLimit)) ||
                    ((maxNodes >= 0) && (totalNodes >= maxNodes))) {
                throw new StopSearch();
            }
            if (tNow - tLastStats >= 1000) {
                notifyStats();
            }
        }
        
        // Collect statistics
        if (verbose) {
            if (ply < 20) nodesPlyVec[ply]++;
            if (depth < 20*plyScale) nodesDepthVec[depth/plyScale]++;
        }
        final long hKey = pos.historyHash();

        // Draw tests
        if (canClaimDraw50(pos)) {
            if (MoveGen.canTakeKing(pos)) {
                int score = MATE0 - ply;
                if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
                return score;
            }
            if (inCheck) {
                MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
                MoveGen.removeIllegal(pos, moves);
                if (moves.size == 0) {            // Can't claim draw if already check mated.
                    int score = -(MATE0-(ply+1));
                    if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
                    moveGen.returnMoveList(moves);
                    return score;
                }
                moveGen.returnMoveList(moves);
            }
            if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, 0, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
            return 0;
        }
        if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashFirstNew)) {
            if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, 0, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
            return 0;            // No need to test for mate here, since it would have been
                                 // discovered the first time the position came up.
        }

        int evalScore = UNKNOWN_SCORE;
        // Check transposition table
        TTEntry ent = tt.probe(hKey);
        Move hashMove = null;
        SearchTreeInfo sti = searchTreeInfo[ply];
        if (ent.type != TTEntry.T_EMPTY) {
            int score = ent.getScore(ply);
            evalScore = ent.evalScore;
            int plyToMate = MATE0 - Math.abs(score);
            int eDepth = ent.getDepth();
            hashMove = sti.hashMove;
            ent.getMove(hashMove);
            if ((beta == alpha + 1) && ((eDepth >= depth) || (eDepth >= plyToMate*plyScale))) {
                if (    (ent.type == TTEntry.T_EXACT) ||
                        (ent.type == TTEntry.T_GE) && (score >= beta) ||
                        (ent.type == TTEntry.T_LE) && (score <= alpha)) {
                    if (score >= beta) {
                        hashMove = sti.hashMove;
                        if ((hashMove != null) && (hashMove.from != hashMove.to))
                            if (pos.getPiece(hashMove.to) == Piece.EMPTY)
                                kt.addKiller(ply, hashMove);
                    }
                    sti.bestMove = hashMove;
                    if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, ent.type, evalScore, hKey);
                    return score;
                }
            }
        }
        
        int posExtend = inCheck ? plyScale : 0; // Check extension

        // If out of depth, perform quiescence search
        if (depth + posExtend <= 0) {
            q0Eval = evalScore;
            sti.bestMove.clear();
            int score = quiesce(alpha, beta, ply, 0, inCheck);
            int type = TTEntry.T_EXACT;
            if (score <= alpha) {
                type = TTEntry.T_LE;
            } else if (score >= beta) {
                type = TTEntry.T_GE;
            }
            sti.bestMove.score = score;
            tt.insert(hKey, sti.bestMove, type, ply, depth, q0Eval);
            if (log != null) log.logNodeEnd(sti.nodeIdx, score, type, q0Eval, hKey);
            return score;
        }

        // Razoring
        if ((Math.abs(alpha) <= MATE0 / 2) && (depth < 4*plyScale) && (beta == alpha + 1)) {
            if (evalScore == UNKNOWN_SCORE) {
                evalScore = eval.evalPos(pos);
            }
            final int razorMargin = 250;
            if (evalScore < beta - razorMargin) {
                q0Eval = evalScore;
                int score = quiesce(alpha-razorMargin, beta-razorMargin, ply, 0, inCheck);
                if (score <= alpha-razorMargin) {
                    emptyMove.score = score;
                    tt.insert(hKey, emptyMove, TTEntry.T_LE, ply, depth, q0Eval);
                    if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_LE, q0Eval, hKey);
                    return score;
                }
            }
        }

        // Reverse futility pruning
        if (!inCheck && (depth < 5*plyScale) && (posExtend == 0) && 
            (Math.abs(alpha) <= MATE0 / 2) && (Math.abs(beta) <= MATE0 / 2)) {
            boolean mtrlOk;
            if (pos.whiteMove) {
                mtrlOk = (pos.wMtrl > pos.wMtrlPawns) && (pos.wMtrlPawns > 0);
            } else {
                mtrlOk = (pos.bMtrl > pos.bMtrlPawns) && (pos.bMtrlPawns > 0);
            }
            if (mtrlOk) {
                int margin;
                if (depth <= plyScale)        margin = 204;
                else if (depth <= 2*plyScale) margin = 420;
                else if (depth <= 3*plyScale) margin = 533;
                else                          margin = 788;
                if (evalScore == UNKNOWN_SCORE)
                    evalScore = eval.evalPos(pos);
                if (evalScore - margin >= beta) {
                    emptyMove.score = evalScore - margin;
                    tt.insert(hKey, emptyMove, TTEntry.T_GE, ply, depth, evalScore);
                    if (log != null) log.logNodeEnd(sti.nodeIdx, evalScore - margin, TTEntry.T_GE, evalScore, hKey);
                    return evalScore - margin;
                }
            }
        }

        // Try null-move pruning
        sti.currentMove = emptyMove;
        if (    (depth >= 3*plyScale) && !inCheck && sti.allowNullMove &&
                (Math.abs(beta) <= MATE0 / 2)) {
            if (MoveGen.canTakeKing(pos)) {
                int score = MATE0 - ply;
                if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_EXACT, evalScore, hKey);
                return score;
            }
            boolean nullOk;
            if (pos.whiteMove) {
                nullOk = (pos.wMtrl > pos.wMtrlPawns) && (pos.wMtrlPawns > 0);
            } else {
                nullOk = (pos.bMtrl > pos.bMtrlPawns) && (pos.bMtrlPawns > 0);
            }
            if (nullOk) {
                if (evalScore == UNKNOWN_SCORE)
                    evalScore = eval.evalPos(pos);
                if (evalScore < beta)
                    nullOk = false;
            }
            if (nullOk) {
                final int R = (depth > 6*plyScale) ? 4*plyScale : 3*plyScale;
                pos.setWhiteMove(!pos.whiteMove);
                int epSquare = pos.getEpSquare();
                pos.setEpSquare(-1);
                searchTreeInfo[ply+1].allowNullMove = false;
                searchTreeInfo[ply+1].bestMove.clear();
                int score = -negaScout(-beta, -(beta - 1), ply + 1, depth - R, -1, false);
                searchTreeInfo[ply+1].allowNullMove = true;
                pos.setEpSquare(epSquare);
                pos.setWhiteMove(!pos.whiteMove);
                if (score >= beta) {
                    if (score > MATE0 / 2)
                        score = beta;
                    emptyMove.score = score;
                    tt.insert(hKey, emptyMove, TTEntry.T_GE, ply, depth, evalScore);
                    if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_GE, evalScore, hKey);
                    return score;
                } else {
                    if ((searchTreeInfo[ply-1].lmr > 0) && (depth < 5*plyScale)) {
                        Move m1 = searchTreeInfo[ply-1].currentMove;
                        Move m2 = searchTreeInfo[ply+1].bestMove; // threat move
                        if (relatedMoves(m1, m2)) {
                            // if the threat move was made possible by a reduced
                            // move on the previous ply, the reduction was unsafe.
                            // Return alpha to trigger a non-reduced re-search.
                            if (log != null) log.logNodeEnd(sti.nodeIdx, alpha, TTEntry.T_LE, evalScore, hKey);
                            return alpha;
                        }
                    }
                }
            }
        }

        boolean futilityPrune = false;
        int futilityScore = alpha;
        if (!inCheck && (depth < 5*plyScale) && (posExtend == 0)) {
            if ((Math.abs(alpha) <= MATE0 / 2) && (Math.abs(beta) <= MATE0 / 2)) {
                int margin;
                if (depth <= plyScale)        margin = 61;
                else if (depth <= 2*plyScale) margin = 144;
                else if (depth <= 3*plyScale) margin = 268;
                else                          margin = 334;
                if (evalScore == UNKNOWN_SCORE)
                    evalScore = eval.evalPos(pos);
                futilityScore = evalScore + margin;
                if (futilityScore <= alpha)
                    futilityPrune = true;
            }
        }

        if ((depth > 4*plyScale) && ((hashMove == null) || (hashMove.from == hashMove.to))) {
            boolean isPv = beta > alpha + 1;
            if (isPv || (depth > 8 * plyScale)) {
                // No hash move. Try internal iterative deepening.
                long savedNodeIdx = sti.nodeIdx;
                int newDepth = isPv ? depth  - 2 * plyScale : depth * 3 / 8;
                negaScout(alpha, beta, ply, newDepth, -1, inCheck);
                sti.nodeIdx = savedNodeIdx;
                ent = tt.probe(hKey);
                if (ent.type != TTEntry.T_EMPTY) {
                    hashMove = sti.hashMove;
                    ent.getMove(hashMove);
                }
            }
        }

        // Start searching move alternatives
        MoveGen.MoveList moves;
        if (inCheck)
            moves = moveGen.checkEvasions(pos);
        else 
            moves = moveGen.pseudoLegalMoves(pos);
        boolean seeDone = false;
        boolean hashMoveSelected = true;
        if (!selectHashMove(moves, hashMove)) {
            scoreMoveList(moves, ply);
            seeDone = true;
            hashMoveSelected = false;
        }

        UndoInfo ui = sti.undoInfo;
        boolean haveLegalMoves = false;
        int illegalScore = -(MATE0-(ply+1));
        int b = beta;
        int bestScore = illegalScore;
        int bestMove = -1;
        int lmrCount = 0;
        for (int mi = 0; mi < moves.size; mi++) {
            if ((mi == 1) && !seeDone) {
                scoreMoveList(moves, ply, 1);
                seeDone = true;
            }
            if ((mi > 0) || !hashMoveSelected) {
                selectBest(moves, mi);
            }
            Move m = moves.m[mi];
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                moveGen.returnMoveList(moves);
                int score = MATE0-ply;
                if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_EXACT, evalScore, hKey);
                return score;       // King capture
            }
            int newCaptureSquare = -1;
            boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY);
            boolean isPromotion = (m.promoteTo != Piece.EMPTY);
            int sVal = Integer.MIN_VALUE;
            boolean mayReduce = (m.score < 53) && (!isCapture || m.score < 0) && !isPromotion;
            boolean givesCheck = MoveGen.givesCheck(pos, m); 
            boolean doFutility = false;
            if (mayReduce && haveLegalMoves && !givesCheck && !passedPawnPush(pos, m)) {
                if ((Math.abs(alpha) <= MATE0 / 2) && (Math.abs(beta) <= MATE0 / 2)) {
                    int moveCountLimit;
                    if (depth <= plyScale)          moveCountLimit = 3;
                    else if (depth <= 2 * plyScale) moveCountLimit = 6;
                    else if (depth <= 3 * plyScale) moveCountLimit = 12;
                    else if (depth <= 4 * plyScale) moveCountLimit = 24;
                    else moveCountLimit = 256;
                    if (mi >= moveCountLimit)
                        continue; // Late move pruning
                }
                if (futilityPrune)
                    doFutility = true;
            }
            int score;
            if (doFutility) {
                score = futilityScore;
            } else {
                int moveExtend = 0;
                if (posExtend == 0) {
                    final int pV = Evaluate.pV;
                    if ((m.to == recaptureSquare)) {
                        if (sVal == Integer.MIN_VALUE) sVal = SEE(m);
                        int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                        if (sVal > tVal - pV / 2)
                            moveExtend = plyScale;
                    }
                    if ((moveExtend < plyScale) && isCapture && (pos.wMtrlPawns + pos.bMtrlPawns > pV)) {
                        // Extend if going into pawn endgame
                        int capVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                        if (pos.whiteMove) {
                            if ((pos.wMtrl == pos.wMtrlPawns) && (pos.bMtrl - pos.bMtrlPawns == capVal))
                                moveExtend = plyScale;
                        } else {
                            if ((pos.bMtrl == pos.bMtrlPawns) && (pos.wMtrl - pos.wMtrlPawns == capVal))
                                moveExtend = plyScale;
                        }
                    }
                }
                int extend = Math.max(posExtend, moveExtend);
                int lmr = 0;
                if ((depth >= 3*plyScale) && mayReduce && (extend == 0)) {
                    if (!givesCheck && !passedPawnPush(pos, m)) {
                        lmrCount++;
                        if ((lmrCount > 3) && (depth > 3*plyScale) && !isCapture) {
                            lmr = 2*plyScale;
                        } else {
                            lmr = 1*plyScale;
                        }
                    }
                }
                int newDepth = depth - plyScale + extend - lmr;
                if (isCapture && (givesCheck || (depth + extend) > plyScale)) {
                    // Compute recapture target square, but only if we are not going
                    // into q-search at the next ply.
                    int fVal = Evaluate.pieceValue[pos.getPiece(m.from)];
                    int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                    final int pV = Evaluate.pV;
                    if (Math.abs(tVal - fVal) < pV / 2) {    // "Equal" capture
                        sVal = SEE(m);
                        if (Math.abs(sVal) < pV / 2)
                            newCaptureSquare = m.to;
                    }
                }
                posHashList[posHashListSize++] = pos.zobristHash();
                pos.makeMove(m, ui);
                nodes++;
                totalNodes++;
                sti.currentMove = m;
/*              long nodes0 = nodes;
                long qNodes0 = qNodes;
                if ((ply < 3) && (newDepth > plyScale)) {
                    System.out.printf("%2d %5s %5d %5d %6s %6s ",
                            mi, "-", alpha, beta, "-", "-");
                    for (int i = 0; i < ply; i++)
                        System.out.printf("      ");
                    System.out.printf("%-6s...\n", TextIO.moveToUCIString(m));
                } */
                sti.lmr = lmr;
                score = -negaScout(-b, -alpha, ply + 1, newDepth, newCaptureSquare, givesCheck);
                if (((lmr > 0) && (score > alpha)) ||
                    ((score > alpha) && (score < beta) && (b != beta) && (score != illegalScore))) {
                    sti.lmr = 0;
                    newDepth += lmr;
                    score = -negaScout(-beta, -alpha, ply + 1, newDepth, newCaptureSquare, givesCheck);
                }
/*              if (ply <= 3) {
                    System.out.printf("%2d %5d %5d %5d %6d %6d ",
                            mi, score, alpha, beta, nodes-nodes0, qNodes-qNodes0);
                    for (int i = 0; i < ply; i++)
                        System.out.printf("      ");
                    System.out.printf("%-6s\n", TextIO.moveToUCIString(m));
                }*/
                posHashListSize--;
                pos.unMakeMove(m, ui);
            }
            if (weak && haveLegalMoves)
                if (weakPlaySkipMove(pos, m, ply))
                    score = illegalScore;
            m.score = score;

            if (score != illegalScore) {
                haveLegalMoves = true;
            }
            bestScore = Math.max(bestScore, score);
            if (score > alpha) {
                alpha = score;
                bestMove = mi;
                sti.bestMove.from      = m.from;
                sti.bestMove.to        = m.to;
                sti.bestMove.promoteTo = m.promoteTo;
            }
            if (alpha >= beta) {
                if (pos.getPiece(m.to) == Piece.EMPTY) {
                    kt.addKiller(ply, m);
                    ht.addSuccess(pos, m, depth/plyScale);
                    for (int mi2 = mi - 1; mi2 >= 0; mi2--) {
                        Move m2 = moves.m[mi2];
                        if (pos.getPiece(m2.to) == Piece.EMPTY)
                            ht.addFail(pos, m2, depth/plyScale);
                    }
                }
                tt.insert(hKey, m, TTEntry.T_GE, ply, depth, evalScore);
                moveGen.returnMoveList(moves);
                if (log != null) log.logNodeEnd(sti.nodeIdx, alpha, TTEntry.T_GE, evalScore, hKey);
                return alpha;
            }
            b = alpha + 1;
        }
        if (!haveLegalMoves && !inCheck) {
            moveGen.returnMoveList(moves);
            if (log != null) log.logNodeEnd(sti.nodeIdx, 0, TTEntry.T_EXACT, evalScore, hKey);
            return 0;       // Stale-mate
        }
        if (bestMove >= 0) {
            tt.insert(hKey, moves.m[bestMove], TTEntry.T_EXACT, ply, depth, evalScore);
            if (log != null) log.logNodeEnd(sti.nodeIdx, bestScore, TTEntry.T_EXACT, evalScore, hKey);
        } else {
            emptyMove.score = bestScore;
            tt.insert(hKey, emptyMove, TTEntry.T_LE, ply, depth, evalScore);
            if (log != null) log.logNodeEnd(sti.nodeIdx, bestScore, TTEntry.T_LE, evalScore, hKey);
        }
        moveGen.returnMoveList(moves);
        return bestScore;
    }

    /** Return true if move m2 was made possible by move m1. */
    private final boolean relatedMoves(Move m1, Move m2) {
        if ((m1.from == m1.to) || (m2.from == m2.to))
            return false;
        if ((m1.to == m2.from) || (m1.from == m2.to) ||
            ((BitBoard.squaresBetween[m2.from][m2.to] & (1L << m1.from)) != 0))
            return true;
        return false;
    }

    /** Return true if move should be skipped in order to make engine play weaker. */
    private final boolean weakPlaySkipMove(Position pos, Move m, int ply) {
        long rndL = pos.zobristHash() ^ Position.psHashKeys[0][m.from] ^
                    Position.psHashKeys[0][m.to] ^ randomSeed;
        double rnd = ((rndL & 0x7fffffffffffffffL) % 1000000000) / 1e9;

        double s = strength * 1e-3;
        double offs = (17 - 50 * s) / 3;
        double effPly = ply * Evaluate.interpolate(pos.wMtrl + pos.bMtrl, 0, 30, Evaluate.qV * 4, 100) * 1e-2;
        double t = effPly + offs;
        double p = 1/(1+Math.exp(t)); // Probability to "see" move
        boolean easyMove = ((pos.getPiece(m.to) != Piece.EMPTY) ||
                            (ply < 2) || (searchTreeInfo[ply-2].currentMove.to == m.from));
        if (easyMove)
            p = 1-(1-p)*(1-p);
        if (rnd > p)
            return true;
        return false;
    }

    private static final boolean passedPawnPush(Position pos, Move m) {
        int p = pos.getPiece(m.from);
        if (pos.whiteMove) {
            if (p != Piece.WPAWN)
                return false;
            if ((BitBoard.wPawnBlockerMask[m.to] & pos.pieceTypeBB[Piece.BPAWN]) != 0)
                return false;
            return m.to >= 40;
        } else {
            if (p != Piece.BPAWN)
                return false;
            if ((BitBoard.bPawnBlockerMask[m.to] & pos.pieceTypeBB[Piece.WPAWN]) != 0)
                return false;
            return m.to <= 23;
        }
    }

    /**
     * Quiescence search. Only non-losing captures are searched.
     */
    final private int quiesce(int alpha, int beta, int ply, int depth, final boolean inCheck) {
        int score;
        if (inCheck) {
            score = -(MATE0 - (ply+1));
        } else {
            if ((depth == 0) && (q0Eval != UNKNOWN_SCORE)) {
                score = q0Eval;
            } else {
                score = eval.evalPos(pos);
                if (depth == 0)
                    q0Eval = score;
            }
        }
        if (score >= beta) {
            if ((depth == 0) && (score < MATE0 - ply)) {
                if (MoveGen.canTakeKing(pos)) {
                    // To make stale-mate detection work
                    score = MATE0 - ply;
                }
            }
            return score;
        }
        final int evalScore = score;
        if (score > alpha)
            alpha = score;
        int bestScore = score;
        final boolean tryChecks = (depth > -1);
        MoveGen.MoveList moves;
        if (inCheck) {
            moves = moveGen.checkEvasions(pos);
        } else if (tryChecks) {
            moves = moveGen.pseudoLegalCapturesAndChecks(pos);
        } else {
            moves = moveGen.pseudoLegalCaptures(pos);
        }
        scoreMoveListMvvLva(moves);
        UndoInfo ui = searchTreeInfo[ply].undoInfo;
        for (int mi = 0; mi < moves.size; mi++) {
            if (mi < 8) {
                // If the first 8 moves didn't fail high, this is probably an ALL-node,
                // so spending more effort on move ordering is probably wasted time.
                selectBest(moves, mi);
            }
            Move m = moves.m[mi];
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                moveGen.returnMoveList(moves);
                return MATE0-ply;       // King capture
            }
            boolean givesCheck = false;
            boolean givesCheckComputed = false;
            if (inCheck) {
                // Allow all moves
            } else {
                if ((pos.getPiece(m.to) == Piece.EMPTY) && (m.promoteTo == Piece.EMPTY)) {
                    // Non-capture
                    if (!tryChecks)
                        continue;
                    givesCheck = MoveGen.givesCheck(pos, m);
                    givesCheckComputed = true;
                    if (!givesCheck)
                        continue;
                    if (negSEE(m)) // Needed because m.score is not computed for non-captures
                        continue;
                } else {
                    if (negSEE(m))
                        continue;
                    int capt = Evaluate.pieceValue[pos.getPiece(m.to)];
                    int prom = Evaluate.pieceValue[m.promoteTo];
                    int optimisticScore = evalScore + capt + prom + 200;
                    if (optimisticScore < alpha) { // Delta pruning
                        if ((pos.wMtrlPawns > 0) && (pos.wMtrl > capt + pos.wMtrlPawns) &&
                            (pos.bMtrlPawns > 0) && (pos.bMtrl > capt + pos.bMtrlPawns)) {
                            if (depth -1 > -2) {
                                givesCheck = MoveGen.givesCheck(pos, m);
                                givesCheckComputed = true;
                            }
                            if (!givesCheck) {
                                if (optimisticScore > bestScore)
                                    bestScore = optimisticScore;
                                continue;
                            }
                        }
                    }
                }
            }

            if (!givesCheckComputed) {
                if (depth - 1 > -2) {
                    givesCheck = MoveGen.givesCheck(pos, m);
                }
            }
            final boolean nextInCheck = (depth - 1) > -2 ? givesCheck : false;

            pos.makeMove(m, ui); 
            qNodes++;
            totalNodes++;
            score = -quiesce(-beta, -alpha, ply + 1, depth - 1, nextInCheck);
            pos.unMakeMove(m, ui);
            if (score > bestScore) {
                bestScore = score;
                if (score > alpha) {
                    if (depth == 0) {
                        SearchTreeInfo sti = searchTreeInfo[ply];
                        sti.bestMove.setMove(m.from, m.to, m.promoteTo, score);
                    }
                    alpha = score;
                    if (alpha >= beta) {
                        moveGen.returnMoveList(moves);
                        return alpha;
                    }
                }
            }
        }
        moveGen.returnMoveList(moves);
        return bestScore;
    }

    /** Return >0, 0, <0, depending on the sign of SEE(m). */
    final public int signSEE(Move m) {
        int p0 = Evaluate.pieceValue[pos.getPiece(m.from)];
        int p1 = Evaluate.pieceValue[pos.getPiece(m.to)];
        if (p0 < p1)
            return 1;
        return SEE(m);
    }

    /** Return true if SEE(m) < 0. */
    final public boolean negSEE(Move m) {
        int p0 = Evaluate.pieceValue[pos.getPiece(m.from)];
        int p1 = Evaluate.pieceValue[pos.getPiece(m.to)];
        if (p1 >= p0)
            return false;
        return SEE(m) < 0;
    }

    private int[] captures = new int[64];   // Value of captured pieces
    private UndoInfo seeUi = new UndoInfo();

    /**
     * Static exchange evaluation function.
     * @return SEE score for m. Positive value is good for the side that makes the first move.
     */
    final public int SEE(Move m) {
        final int kV = Evaluate.kV;
        
        final int square = m.to;
        if (square == pos.getEpSquare()) {
            captures[0] = Evaluate.pV;
        } else {
            captures[0] = Evaluate.pieceValue[pos.getPiece(square)];
            if (captures[0] == kV)
                return kV;
        }
        int nCapt = 1;                  // Number of entries in captures[]

        pos.makeSEEMove(m, seeUi);
        boolean white = pos.whiteMove;
        int valOnSquare = Evaluate.pieceValue[pos.getPiece(square)];
        long occupied = pos.whiteBB | pos.blackBB;
        while (true) {
            int bestValue = Integer.MAX_VALUE;
            long atk;
            if (white) {
                atk = BitBoard.bPawnAttacks[square] & pos.pieceTypeBB[Piece.WPAWN] & occupied;
                if (atk != 0) {
                    bestValue = Evaluate.pV;
                } else {
                    atk = BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.WKNIGHT] & occupied;
                    if (atk != 0) {
                        bestValue = Evaluate.nV;
                    } else {
                        long bAtk = BitBoard.bishopAttacks(square, occupied) & occupied;
                        atk = bAtk & pos.pieceTypeBB[Piece.WBISHOP];
                        if (atk != 0) {
                            bestValue = Evaluate.bV;
                        } else {
                            long rAtk = BitBoard.rookAttacks(square, occupied) & occupied;
                            atk = rAtk & pos.pieceTypeBB[Piece.WROOK];
                            if (atk != 0) {
                                bestValue = Evaluate.rV;
                            } else {
                                atk = (bAtk | rAtk) & pos.pieceTypeBB[Piece.WQUEEN];
                                if (atk != 0) {
                                    bestValue = Evaluate.qV;
                                } else {
                                    atk = BitBoard.kingAttacks[square] & pos.pieceTypeBB[Piece.WKING] & occupied;
                                    if (atk != 0) {
                                        bestValue = kV;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                atk = BitBoard.wPawnAttacks[square] & pos.pieceTypeBB[Piece.BPAWN] & occupied;
                if (atk != 0) {
                    bestValue = Evaluate.pV;
                } else {
                    atk = BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.BKNIGHT] & occupied;
                    if (atk != 0) {
                        bestValue = Evaluate.nV;
                    } else {
                        long bAtk = BitBoard.bishopAttacks(square, occupied) & occupied;
                        atk = bAtk & pos.pieceTypeBB[Piece.BBISHOP];
                        if (atk != 0) {
                            bestValue = Evaluate.bV;
                        } else {
                            long rAtk = BitBoard.rookAttacks(square, occupied) & occupied;
                            atk = rAtk & pos.pieceTypeBB[Piece.BROOK];
                            if (atk != 0) {
                                bestValue = Evaluate.rV;
                            } else {
                                atk = (bAtk | rAtk) & pos.pieceTypeBB[Piece.BQUEEN];
                                if (atk != 0) {
                                    bestValue = Evaluate.qV;
                                } else {
                                    atk = BitBoard.kingAttacks[square] & pos.pieceTypeBB[Piece.BKING] & occupied;
                                    if (atk != 0) {
                                        bestValue = kV;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            captures[nCapt++] = valOnSquare;
            if (valOnSquare == kV)
                break;
            valOnSquare = bestValue;
            occupied &= ~(atk & -atk);
            white = !white;
        }
        pos.unMakeSEEMove(m, seeUi);
        
        int score = 0;
        for (int i = nCapt - 1; i > 0; i--) {
            score = Math.max(0, captures[i] - score);
        }
        return captures[0] - score;
    }

    /**
     * Compute scores for each move in a move list, using SEE, killer and history information.
     * @param moves  List of moves to score.
     */
    final void scoreMoveList(MoveGen.MoveList moves, int ply) {
        scoreMoveList(moves, ply, 0);
    }
    final void scoreMoveList(MoveGen.MoveList moves, int ply, int startIdx) {
        for (int i = startIdx; i < moves.size; i++) {
            Move m = moves.m[i];
            boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY) || (m.promoteTo != Piece.EMPTY);
            int score = 0;
            if (isCapture) {
                int seeScore = isCapture ? signSEE(m) : 0;
                int v = pos.getPiece(m.to);
                int a = pos.getPiece(m.from);
                score = Evaluate.pieceValue[v]/10 * 1000 - Evaluate.pieceValue[a]/10;
                if (seeScore > 0)
                    score += 2000000;
                else if (seeScore == 0)
                    score += 1000000;
                else
                    score -= 1000000;
                score *= 100;
            }
            int ks = kt.getKillerScore(ply, m);
            if (ks > 0) {
                score += ks + 50;
            } else {
                int hs = ht.getHistScore(pos, m);
                score += hs;
            }
            m.score = score;
        }
    }
    private final void scoreMoveListMvvLva(MoveGen.MoveList moves) {
        for (int i = 0; i < moves.size; i++) {
            Move m = moves.m[i];
            int v = pos.getPiece(m.to);
            int a = pos.getPiece(m.from);
            m.score = Evaluate.pieceValue[v] * 10000 - Evaluate.pieceValue[a];
        }
    }

    /**
     * Find move with highest score and move it to the front of the list.
     */
    final static void selectBest(MoveGen.MoveList moves, int startIdx) {
        int bestIdx = startIdx;
        int bestScore = moves.m[bestIdx].score;
        for (int i = startIdx + 1; i < moves.size; i++) {
            int sc = moves.m[i].score;
            if (sc > bestScore) {
                bestIdx = i;
                bestScore = sc;
            }
        }
        if (bestIdx != startIdx) {
            Move m = moves.m[startIdx];
            moves.m[startIdx] = moves.m[bestIdx];
            moves.m[bestIdx] = m;
        }
    }

    /** If hashMove exists in the move list, move the hash move to the front of the list. */
    final static boolean selectHashMove(MoveGen.MoveList moves, Move hashMove) {
        if (hashMove == null) {
            return false;
        }
        for (int i = 0; i < moves.size; i++) {
            Move m = moves.m[i];
            if (m.equals(hashMove)) {
                moves.m[i] = moves.m[0];
                moves.m[0] = m;
                m.score = 10000;
                return true;
            }
        }
        return false;
    }

    public final static boolean canClaimDraw50(Position pos) {
        return (pos.halfMoveClock >= 100);
    }
    
    public final static boolean canClaimDrawRep(Position pos, long[] posHashList, int posHashListSize, int posHashFirstNew) {
        int reps = 0;
        for (int i = posHashListSize - 4; i >= 0; i -= 2) {
            if (pos.zobristHash() == posHashList[i]) {
                reps++;
                if (i >= posHashFirstNew) {
                    reps++;
                    break;
                }
            }
        }
        return (reps >= 2);
    }

    private final void initNodeStats() {
        nodes = qNodes = 0;
        nodesPlyVec = new int[20];
        nodesDepthVec = new int[20];
        for (int i = 0; i < 20; i++) {
            nodesPlyVec[i] = 0;
            nodesDepthVec[i] = 0;
        }
    }
}
