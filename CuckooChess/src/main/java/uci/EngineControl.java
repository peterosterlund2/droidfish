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

package uci;

import chess.Book;
import chess.ComputerPlayer;
import chess.History;
import chess.Move;
import chess.MoveGen;
import chess.Parameters;
import chess.Piece;
import chess.Position;
import chess.Search;
import chess.TextIO;
import chess.TranspositionTable;
import chess.Parameters.CheckParam;
import chess.Parameters.ComboParam;
import chess.Parameters.ParamBase;
import chess.Parameters.SpinParam;
import chess.Parameters.StringParam;
import chess.TranspositionTable.TTEntry;
import chess.UndoInfo;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Control the search thread. */
public class EngineControl {
    private PrintStream os;

    private Thread engineThread;
    private final Object threadMutex;
    private Search sc;
    private TranspositionTable tt;
    private History ht;
    private MoveGen moveGen;

    private Position pos;
    private long[] posHashList;
    private int posHashListSize;
    private boolean ponder;     // True if currently doing pondering
    private boolean onePossibleMove;
    private boolean infinite;

    private int minTimeLimit;
    private int maxTimeLimit;
    private int maxDepth;
    private int maxNodes;
    private List<Move> searchMoves;

    // Options
    private int hashSizeMB = 16;
    private boolean ownBook = false;
    private boolean analyseMode = false;
    private boolean ponderMode = true;

    // Reduced strength variables
    private int strength = 1000;
    private boolean limitStrength = false; // If set, overrides strength, using eloToStrength table
    private int elo = Integer.MAX_VALUE;
    private int maxNPS = 0;
    private long randomSeed = 0;

    /**
     * This class is responsible for sending "info" strings during search.
     */
    private static class SearchListener implements Search.Listener {
        PrintStream os;
        
        SearchListener(PrintStream os) {
            this.os = os;
        }

        public void notifyDepth(int depth) {
            os.printf("info depth %d%n", depth);
        }

        public void notifyCurrMove(Move m, int moveNr) {
            os.printf("info currmove %s currmovenumber %d%n", moveToString(m), moveNr);
        }

        public void notifyPV(int depth, int score, int time, long nodes, int nps, boolean isMate,
                boolean upperBound, boolean lowerBound, ArrayList<Move> pv) {
            StringBuilder pvBuf = new StringBuilder();
            for (Move m : pv) {
                pvBuf.append(" ");
                pvBuf.append(moveToString(m));
            }
            String bound = "";
            if (upperBound) {
                bound = " upperbound";
            } else if (lowerBound) {
                bound = " lowerbound";
            }
            os.printf("info depth %d score %s %d%s time %d nodes %d nps %d pv%s%n",
                    depth, isMate ? "mate" : "cp", score, bound, time, nodes, nps, pvBuf.toString());
        }

        public void notifyStats(long nodes, int nps, int time) {
            os.printf("info nodes %d nps %d time %d%n", nodes, nps, time);
        }
    }

    public EngineControl(PrintStream os) {
        this.os = os;
        threadMutex = new Object();
        setupTT();
        ht = new History();
        moveGen = new MoveGen();
    }

    final public void startSearch(Position pos, ArrayList<Move> moves, SearchParams sPar) {
        setupPosition(new Position(pos), moves);
        computeTimeLimit(sPar);
        ponder = false;
        infinite = (maxTimeLimit < 0) && (maxDepth < 0) && (maxNodes < 0);
        searchMoves = sPar.searchMoves;
        startThread(minTimeLimit, maxTimeLimit, maxDepth, maxNodes);
    }

    final public void startPonder(Position pos, List<Move> moves, SearchParams sPar) {
        setupPosition(new Position(pos), moves);
        computeTimeLimit(sPar);
        ponder = true;
        infinite = false;
        startThread(-1, -1, -1, -1);
    }

    final public void ponderHit() {
        Search mySearch;
        synchronized (threadMutex) {
            mySearch = sc;
        }
        if (mySearch != null) {
            if (onePossibleMove) {
                if (minTimeLimit > 1) minTimeLimit = 1;
                if (maxTimeLimit > 1) maxTimeLimit = 1;
            }
            mySearch.timeLimit(minTimeLimit, maxTimeLimit);
        }
        infinite = (maxTimeLimit < 0) && (maxDepth < 0) && (maxNodes < 0);
        ponder = false;
    }

    final public void stopSearch() {
        stopThread();
    }

    final public void newGame() {
        randomSeed = new Random().nextLong();
        tt.clear();
        ht.init();
    }

    /**
     * Compute thinking time for current search.
     */
    final public void computeTimeLimit(SearchParams sPar) {
        minTimeLimit = -1;
        maxTimeLimit = -1;
        maxDepth = -1;
        maxNodes = -1;
        if (sPar.infinite) {
            minTimeLimit = -1;
            maxTimeLimit = -1;
            maxDepth = -1;
        } else if (sPar.depth > 0) {
            maxDepth = sPar.depth;
        } else if (sPar.mate > 0) {
            maxDepth = sPar.mate * 2 - 1;
        } else if (sPar.moveTime > 0) {
            minTimeLimit = maxTimeLimit = sPar.moveTime;
        } else if (sPar.nodes > 0) {
            maxNodes = sPar.nodes;
        } else {
            int moves = sPar.movesToGo;
            if (moves == 0) {
                moves = 999;
            }
            moves = Math.min(moves, 45); // Assume 45 more moves until end of game
            if (ponderMode) {
                final double ponderHitRate = 0.35;
                moves = (int)Math.ceil(moves * (1 - ponderHitRate));
            }
            boolean white = pos.whiteMove;
            int time = white ? sPar.wTime : sPar.bTime;
            int inc  = white ? sPar.wInc : sPar.bInc;
            final int margin = Math.min(1000, time * 9 / 10);
            int timeLimit = (time + inc * (moves - 1) - margin) / moves;
            minTimeLimit = (int)(timeLimit * 0.85);
            maxTimeLimit = (int)(minTimeLimit * (Math.max(2.5, Math.min(4.0, moves * 0.5))));

            // Leave at least 1s on the clock, but can't use negative time
            minTimeLimit = clamp(minTimeLimit, 1, time - margin);
            maxTimeLimit = clamp(maxTimeLimit, 1, time - margin);
        }
    }

    private static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private void startThread(final int minTimeLimit, final int maxTimeLimit,
                             int maxDepth, final int maxNodes) {
        synchronized (threadMutex) {} // Must not start new search until old search is finished
        sc = new Search(pos, posHashList, posHashListSize, tt, ht);
        sc.timeLimit(minTimeLimit, maxTimeLimit);
        sc.setListener(new SearchListener(os));
        sc.setStrength(getStrength(), randomSeed, getMaxNPS());
        MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, moves);
        if ((searchMoves != null) && (searchMoves.size() > 0))
            moves.filter(searchMoves);
        final MoveGen.MoveList srchMoves = moves;
        onePossibleMove = false;
        if ((srchMoves.size < 2) && !infinite) {
            onePossibleMove = true;
            if (!ponder) {
                if ((maxDepth < 0) || (maxDepth > 2)) maxDepth = 2;
            }
        }
        tt.nextGeneration();
        final int srchmaxDepth = maxDepth;
        engineThread = new Thread(() -> {
            Move m = null;
            if (ownBook && !analyseMode) {
                Book book = new Book(false);
                m = book.getBookMove(pos);
            }
            if (m == null) {
                m = sc.iterativeDeepening(srchMoves, srchmaxDepth, maxNodes, false);
            }
            while (ponder || infinite) {
                // We should not respond until told to do so. Just wait until
                // we are allowed to respond.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            Move ponderMove = getPonderMove(pos, m);
            synchronized (threadMutex) {
                if (ponderMove != null) {
                    os.printf("bestmove %s ponder %s%n", moveToString(m), moveToString(ponderMove));
                } else {
                    os.printf("bestmove %s%n", moveToString(m));
                }
                engineThread = null;
                sc = null;
            }
        });
        engineThread.start();
    }

    private void stopThread() {
        Thread myThread;
        Search mySearch;
        synchronized (threadMutex) {
            myThread = engineThread;
            mySearch = sc;
        }
        if (myThread != null) {
            mySearch.timeLimit(0, 0);
            infinite = false;
            ponder = false;
            try {
                myThread.join();
            } catch (InterruptedException ex) {
                throw new RuntimeException();
            }
        }
    }


    private void setupTT() {
        int nEntries = hashSizeMB > 0 ? hashSizeMB * (1 << 20) / 24 : 1024;
        int logSize = (int) Math.floor(Math.log(nEntries) / Math.log(2));
        tt = new TranspositionTable(logSize);
    }

    private void setupPosition(Position pos, List<Move> moves) {
        UndoInfo ui = new UndoInfo();
        posHashList = new long[200 + moves.size()];
        posHashListSize = 0;
        for (Move m : moves) {
            posHashList[posHashListSize++] = pos.zobristHash();
            pos.makeMove(m, ui);
        }
        this.pos = pos;
    }

    /**
     * Try to find a move to ponder from the transposition table.
     */
    private Move getPonderMove(Position pos, Move m) {
        if (m == null) return null;
        Move ret = null;
        UndoInfo ui = new UndoInfo();
        pos.makeMove(m, ui);
        TTEntry ent = tt.probe(pos.historyHash());
        if (ent.type != TTEntry.T_EMPTY) {
            ret = new Move(0, 0, 0);
            ent.getMove(ret);
            MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
            MoveGen.removeIllegal(pos, moves);
            boolean contains = false;
            for (int mi = 0; mi < moves.size; mi++)
                if (moves.m[mi].equals(ret)) {
                    contains = true;
                    break;
                }
            if  (!contains)
                ret = null;
        }
        pos.unMakeMove(m, ui);
        return ret;
    }

    private static String moveToString(Move m) {
        if (m == null)
            return "0000";
        String ret = TextIO.squareToString(m.from);
        ret += TextIO.squareToString(m.to);
        switch (m.promoteTo) {
            case Piece.WQUEEN:
            case Piece.BQUEEN:
                ret += "q";
                break;
            case Piece.WROOK:
            case Piece.BROOK:
                ret += "r";
                break;
            case Piece.WBISHOP:
            case Piece.BBISHOP:
                ret += "b";
                break;
            case Piece.WKNIGHT:
            case Piece.BKNIGHT:
                ret += "n";
                break;
            default:
                break;
        }
        return ret;
    }

    static void printOptions(PrintStream os) {
        os.println("option name Hash type spin default 16 min 1 max 2048");
        os.println("option name OwnBook type check default false");
        os.println("option name Ponder type check default true");
        os.println("option name UCI_AnalyseMode type check default false");

        os.printf("option name UCI_EngineAbout type string default %s by Peter Osterlund, see %s%n",
                ComputerPlayer.engineName, "http://hem.bredband.net/petero2b/javachess/index.html");
        os.print("option name Strength type spin default 1000 min 0 max 1000\n");
        os.print("option name UCI_LimitStrength type check default false\n");
        os.print("option name UCI_Elo type spin default 1500 min -625 max 2400\n");
        os.print("option name maxNPS type spin default 0 min 0 max 10000000\n");

        for (String pName : Parameters.instance().getParamNames()) {
            ParamBase p = Parameters.instance().getParam(pName);
            switch (p.type) {
            case CHECK: {
                CheckParam cp = (CheckParam)p;
                os.printf("option name %s type check default %s\n",
                        p.name, cp.defaultValue?"true":"false");
                break;
            }
            case SPIN: {
                SpinParam sp = (SpinParam)p;
                os.printf("option name %s type spin default %d min %d max %d\n",
                        p.name, sp.defaultValue, sp.minValue, sp.maxValue);
                break;
            }
            case COMBO: {
                ComboParam cp = (ComboParam)p;
                os.printf("option name %s type combo default %s ", cp.name, cp.defaultValue);
                for (String s : cp.allowedValues)
                    os.printf(" var %s", s);
                os.print("\n");
                break;
            }
            case BUTTON:
                os.printf("option name %s type button\n", p.name);
                break;
            case STRING: {
                StringParam sp = (StringParam)p;
                os.printf("option name %s type string default %s\n",
                        p.name, sp.defaultValue);
                break;
            }
            }
        }
    }

    final void setOption(String optionName, String optionValue) {
        try {
            if (optionName.equals("hash")) {
                hashSizeMB = Integer.parseInt(optionValue);
                setupTT();
            } else if (optionName.equals("ownbook")) {
                ownBook = Boolean.parseBoolean(optionValue);
            } else if (optionName.equals("ponder")) {
                ponderMode = Boolean.parseBoolean(optionValue);
            } else if (optionName.equals("uci_analysemode")) {
                analyseMode = Boolean.parseBoolean(optionValue);
            } else if (optionName.equals("strength")) {
                strength = Integer.parseInt(optionValue);
            } else if (optionName.equals("uci_limitstrength")) {
                limitStrength = Boolean.parseBoolean(optionValue);
            } else if (optionName.equals("uci_elo")) {
                elo = Integer.parseInt(optionValue);
            } else if (optionName.equals("maxnps")) {
                maxNPS = Integer.parseInt(optionValue);
            } else {
                Parameters.instance().set(optionName, optionValue);
            }
        } catch (NumberFormatException ignore) {
        }
    }

    private static int[][] eloToStrength = {
        { -625,    0 },
        { -572,   10 },
        { -396,   20 },
        { -145,   30 },
        {  204,   45 },
        {  473,   60 },
        {  679,   75 },
        {  891,  100 },
        {  917,  200 },
        { 1055,  300 },
        { 1321,  375 },
        { 1408,  400 },
        { 1694,  500 },
        { 1938,  600 },
        { 2073,  675 },
        { 2182,  750 },
        { 2294,  875 },
        { 2360,  950 },
        { 2410, 1000 },
    };

    /** Get strength setting, possibly by interpolating in eloToStrength table. */
    private int getStrength() {
        if (!limitStrength)
            return strength;
        if (elo <= eloToStrength[0][0])
            return eloToStrength[0][1];
        int n = eloToStrength.length;
        for (int i = 1; i < n; i++) {
            if (elo <= eloToStrength[i][0]) {
                double a  = eloToStrength[i-1][0];
                double b  = eloToStrength[i  ][0];
                double fa = eloToStrength[i-1][1];
                double fb = eloToStrength[i  ][1];
                return (int)Math.round(fa + (elo - a) / (b - a) * (fb - fa));
            }
        }
        return eloToStrength[n-1][1];
    }

    /** Return adjusted maxNPS value if UCI_LimitStrength is enabled. */
    private int getMaxNPS() {
        int intMax = Integer.MAX_VALUE;
        int nps1 = maxNPS == 0 ? intMax : maxNPS;
        int nps2 = nps1;
        if (limitStrength) {
            if (elo < 1350)
                nps2 = Math.min(10000, nps2);
            else
                nps2 = Math.min(100000, nps2);
        }
        int nps = Math.min(nps1, nps2);
        return nps == intMax ? 0 : nps;
    }
}
