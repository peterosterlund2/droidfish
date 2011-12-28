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

package org.petero.droidfish.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.petero.droidfish.BookOptions;
import org.petero.droidfish.engine.cuckoochess.CuckooChessEngine;
import org.petero.droidfish.gamelogic.Game;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.MoveGen;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.SearchListener;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.UndoInfo;
import org.petero.droidfish.gamelogic.SearchListener.PvInfo;

/**
 * A computer algorithm player.
 * @author petero
 */
public class DroidComputerPlayer {
    private String engineName = "";

    private UCIEngine uciEngine = null;
    private final SearchListener listener;
    private final DroidBook book;
    /** Set when "ucinewgame" needs to be sent. */
    private boolean newGame = false;
    /** Engine identifier, "cuckoochess" or "stockfish". */
    private String engine = "";
    /** >1 if multiPV mode is supported. */
    private int maxPV = 1;
    private int numCPUs = 1;
    private int strength = 1000;

    private boolean havePonderHit = false;

    /** Constructor. Starts engine process if not already started. */
    public DroidComputerPlayer(String engine, SearchListener listener) {
        this.engine = engine;
        startEngine();
        this.listener = listener;
        book = DroidBook.getInstance();
    }

    /** Return maximum number of PVs supported by engine. */
    public final synchronized int getMaxPV() {
        return maxPV;
    }

    /** Set opening book options. */
    public final void setBookOptions(BookOptions options) {
        book.setOptions(options);
    }

    /** Return all book moves, both as a formatted string and as a list of moves. */
    public final Pair<String, ArrayList<Move>> getBookHints(Position pos) {
        return book.getAllBookMoves(pos);
    }

    /** Get engine reported name, including strength setting. */
    public final synchronized String getEngineName() {
        String ret = engineName;
        if (strength < 1000)
            ret += String.format(" (%.1f%%)", strength * 0.1);
        return ret;
    }

    /** Clear transposition table. Takes effect when next search started. */
    public final synchronized void clearTT() {
        newGame = true;
    }

    /** Sends "ponderhit" command to engine. */
    public final synchronized void ponderHit(Position pos, Move ponderMove) {
        havePonderHit = true;
        uciEngine.writeLineToEngine("ponderhit");
        pvModified = true;
        notifyGUI(pos, ponderMove);
    }

    /** Stop the engine process. */
    public final synchronized void shutdownEngine() {
        if (uciEngine != null) {
            uciEngine.shutDown();
            uciEngine = null;
        }
    }

    /**
     * Do a search and return a command from the computer player.
     * The command can be a valid move string, in which case the move is played
     * and the turn goes over to the other player. The command can also be a special
     * command, such as "draw" and "resign".
     * @param pos  An earlier position from the game
     * @param mList List of moves to go from the earlier position to the current position.
     *              This list makes it possible for the computer to correctly handle draw
     *              by repetition/50 moves.
     * @param ponderEnabled True if pondering is enabled in the GUI. Can affect time management.
     * @param ponderMove Move to ponder, or null for non-ponder search.
     * @param engineThreads  Number of engine threads to use, if supported by engine.
     * @param engine Chess engine to use for searching.
     * @param strength Engine strength setting.
     */
    public final void doSearch(Position prevPos, ArrayList<Move> mList,
                               Position currPos, boolean drawOffer,
                               int wTime, int bTime, int inc, int movesToGo,
                               boolean ponderEnabled, Move ponderMove,
                               int engineThreads,
                               String engine, int strength, Game g) {
        setEngineStrength(engine, strength);
        setNumPV(1);
        listener.notifyBookInfo("", null);
    
        if (ponderMove != null)
            mList.add(ponderMove);
    
        havePonderHit = false;
    
        // Set up for draw detection
        long[] posHashList = new long[mList.size()+1];
        int posHashListSize = 0;
        Position p = new Position(prevPos);
        UndoInfo ui = new UndoInfo();
        for (int i = 0; i < mList.size(); i++) {
            posHashList[posHashListSize++] = p.zobristHash();
            p.makeMove(mList.get(i), ui);
        }
    
        if (ponderMove == null) {
            // If we have a book move, play it.
            Move bookMove = book.getBookMove(currPos);
            if (bookMove != null) {
                if (canClaimDraw(currPos, posHashList, posHashListSize, bookMove) == "") {
                    listener.notifySearchResult(g, TextIO.moveToString(currPos, bookMove, false), null);
                    return;
                }
            }
    
            // If only one legal move, play it without searching
            ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos);
            moves = MoveGen.removeIllegal(currPos, moves);
            if (moves.size() == 0) {
                listener.notifySearchResult(g, "", null); // User set up a position where computer has no valid moves.
                return;
            }
            if (moves.size() == 1) {
                Move bestMove = moves.get(0);
                if (canClaimDraw(currPos, posHashList, posHashListSize, bestMove) == "") {
                    listener.notifySearchResult(g, TextIO.moveToUCIString(bestMove), null);
                    return;
                }
            }
        }
    
        StringBuilder posStr = new StringBuilder();
        posStr.append("position fen ");
        posStr.append(TextIO.toFEN(prevPos));
        int nMoves = mList.size();
        if (nMoves > 0) {
            posStr.append(" moves");
            for (int i = 0; i < nMoves; i++) {
                posStr.append(" ");
                posStr.append(TextIO.moveToUCIString(mList.get(i)));
            }
        }
        maybeNewGame();
        uciEngine.setOption("Ponder", ponderEnabled);
        uciEngine.setOption("UCI_AnalyseMode", false);
        uciEngine.setOption("Threads", engineThreads > 0 ? engineThreads : numCPUs);
        uciEngine.writeLineToEngine(posStr.toString());
        if (wTime < 1) wTime = 1;
        if (bTime < 1) bTime = 1;
        StringBuilder goStr = new StringBuilder(96);
        goStr.append(String.format("go wtime %d btime %d", wTime, bTime));
        if (inc > 0)
            goStr.append(String.format(" winc %d binc %d", inc, inc));
        if (movesToGo > 0)
            goStr.append(String.format(" movestogo %d", movesToGo));
        if (ponderMove != null)
            goStr.append(" ponder");
        uciEngine.writeLineToEngine(goStr.toString());
    
        Pair<String,String> pair = monitorEngine(currPos, ponderMove);
        String bestMove = pair.first;
        Move nextPonderMove = TextIO.UCIstringToMove(pair.second);
    
        // Claim draw if appropriate
        if (statScore <= 0) {
            String drawClaim = canClaimDraw(currPos, posHashList, posHashListSize, TextIO.UCIstringToMove(bestMove));
            if (drawClaim != "")
                bestMove = drawClaim;
        }
        // Accept draw offer if engine is losing
        if (drawOffer && !statIsMate && (statScore <= -300)) {
            bestMove = "draw accept";
        }
        listener.notifySearchResult(g, bestMove, nextPonderMove);
    }

    public boolean shouldStop = false;

    /** Tell engine to stop searching. */
    public final synchronized void stopSearch() {
        shouldStop = true;
        if (uciEngine != null)
            uciEngine.writeLineToEngine("stop");
        havePonderHit = false;
    }

    /** Start analyzing a position.
     * @param prevPos Position corresponding to last irreversible move.
     * @param mList   List of moves from prevPos to currPos.
     * @param currPos Position to analyze.
     * @param drawOffer True if other side have offered draw.
     * @param engineThreads Number of threads to use, or 0 for default value.
     * @param engine Chess engine to use for searching
     * @param numPV    Multi-PV mode.
     */
    public final void analyze(Position prevPos, ArrayList<Move> mList, Position currPos,
                              boolean drawOffer, int engineThreads,
                              String engine, int numPV) {
        setEngineStrength(engine, 1000);
        setNumPV(numPV);
        if (shouldStop)
            return;
        {
            Pair<String, ArrayList<Move>> bi = getBookHints(currPos);
            listener.notifyBookInfo(bi.first, bi.second);
        }

        // If no legal moves, there is nothing to analyze
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos);
        moves = MoveGen.removeIllegal(currPos, moves);
        if (moves.size() == 0)
            return;
    
        StringBuilder posStr = new StringBuilder();
        posStr.append("position fen ");
        posStr.append(TextIO.toFEN(prevPos));
        int nMoves = mList.size();
        if (nMoves > 0) {
            posStr.append(" moves");
            for (int i = 0; i < nMoves; i++) {
                posStr.append(" ");
                posStr.append(TextIO.moveToUCIString(mList.get(i)));
            }
        }
        maybeNewGame();
        uciEngine.writeLineToEngine(posStr.toString());
        uciEngine.setOption("UCI_AnalyseMode", true);
        uciEngine.setOption("Threads", engineThreads > 0 ? engineThreads : numCPUs);
        String goStr = String.format("go infinite");
        uciEngine.writeLineToEngine(goStr);
    
        monitorEngine(currPos, null);
    }

    /** Set engine and engine strength.
     * @param engine Name of engine.
     * @param strength Engine strength, 0 - 1000. */
    private final synchronized void setEngineStrength(String engine, int strength) {
        if (!engine.equals(this.engine)) {
            shutdownEngine();
            this.engine = engine;
            startEngine();
        }
        this.strength = strength;
        if (uciEngine != null)
            uciEngine.setStrength(strength);
    }

    /** Set engine multi-PV mode. */
    private final synchronized void setNumPV(int numPV) {
        if ((uciEngine != null) && (maxPV > 1)) {
            int num = Math.min(maxPV, numPV);
            uciEngine.setOption("MultiPV", num);
        }
    }

    private final synchronized void startEngine() {
        boolean useCuckoo = engine.equals("cuckoochess");
        if (uciEngine == null) {
            if (useCuckoo) {
                uciEngine = new CuckooChessEngine();
            } else {
                uciEngine = new NativePipedProcess();
            }
            uciEngine.initialize();
            uciEngine.writeLineToEngine("uci");
            readUCIOptions();
            int nThreads = getNumCPUs(); 
            if (nThreads > 8) nThreads = 8;
            numCPUs = nThreads;
            if (!useCuckoo)
                uciEngine.setOption("Hash", 16);
            uciEngine.setOption("Ponder", false);
            uciEngine.writeLineToEngine("ucinewgame");
            syncReady();
        }
    }

    /** Sends "ucinewgame" to engine if clearTT() has previously been called. */
    private final void maybeNewGame() {
        if (newGame) {
            newGame = false;
            if (uciEngine != null) {
                uciEngine.writeLineToEngine("ucinewgame");
                syncReady();
            }
        }
    }

    private static final int getNumCPUs() {
        int nCPUsFromProc = 1;
        try {
            FileReader fr = new FileReader("/proc/stat");
            BufferedReader inBuf = new BufferedReader(fr, 8192);
            String line;
            int nCPUs = 0;
            while ((line = inBuf.readLine()) != null) {
                if ((line.length() >= 4) && line.startsWith("cpu") && Character.isDigit(line.charAt(3)))
                    nCPUs++;
            }
            inBuf.close();
            if (nCPUs < 1) nCPUs = 1;
            nCPUsFromProc = nCPUs;
        } catch (IOException e) {
        }
        int nCPUsFromOS = NativePipedProcess.getNPhysicalProcessors();
        return Math.max(nCPUsFromProc, nCPUsFromOS);
    }

    private final void readUCIOptions() {
        int timeout = 1000;
        maxPV = 1;
        while (true) {
            String s = uciEngine.readLineFromEngine(timeout);
            String[] tokens = tokenize(s);
            if (tokens[0].equals("uciok"))
                break;
            else if (tokens[0].equals("id")) {
                if (tokens[1].equals("name")) {
                    engineName = "";
                    for (int i = 2; i < tokens.length; i++) {
                        if (engineName.length() > 0)
                            engineName += " ";
                        engineName += tokens[i];
                    }
                }
            } else if ((tokens.length > 2) && tokens[2].toLowerCase().equals("multipv")) {
                try {
                    for (int i = 3; i < tokens.length; i++) {
                        if (tokens[i].equals("max") && (i+1 < tokens.length)) {
                            maxPV = Math.max(maxPV, Integer.parseInt(tokens[i+1]));
                            break;
                        }
                    }
                } catch (NumberFormatException nfe) { }
            }
        }
    }

    /** Convert a string to tokens by splitting at whitespace characters. */
    private final String[] tokenize(String cmdLine) {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }

    private final void syncReady() {
        uciEngine.writeLineToEngine("isready");
        while (true) {
            String s = uciEngine.readLineFromEngine(1000);
            if (s.equals("readyok"))
                break;
        }
    }

    /** Wait for engine to respond with bestMove and ponderMove.
     * While waiting, monitor and report search info. */
    private final Pair<String,String> monitorEngine(Position pos, Move ponderMove) {
        // Monitor engine response
        clearInfo();
        boolean stopSent = false;
        while (true) {
            int timeout = 2000;
            while (true) {
                UCIEngine uci = uciEngine;
                if (uci == null)
                    break;
                if (shouldStop && !stopSent) {
                    uci.writeLineToEngine("stop");
                    stopSent = true;
                }
                String s = uci.readLineFromEngine(timeout);
                if (s.length() == 0)
                    break;
                String[] tokens = tokenize(s);
                if (tokens[0].equals("info")) {
                    parseInfoCmd(tokens, ponderMove);
                } else if (tokens[0].equals("bestmove")) {
                    String bestMove = tokens[1];
                    String nextPonderMove = "";
                    if ((tokens.length >= 4) && (tokens[2].equals("ponder")))
                        nextPonderMove = tokens[3];
                    return new Pair<String,String>(bestMove, nextPonderMove);
                }
                timeout = 0;
            }
            notifyGUI(pos, ponderMove);
            try {
                Thread.sleep(100); // 10 GUI updates per second is enough
            } catch (InterruptedException e) {
            }
        }
    }

    /** Check if a draw claim is allowed, possibly after playing "move".
     * @param move The move that may have to be made before claiming draw.
     * @return The draw string that claims the draw, or empty string if draw claim not valid.
     */
    private final String canClaimDraw(Position pos, long[] posHashList, int posHashListSize, Move move) {
        String drawStr = "";
        if (canClaimDraw50(pos)) {
            drawStr = "draw 50";
        } else if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashListSize)) {
            drawStr = "draw rep";
        } else if (move != null) {
            String strMove = TextIO.moveToString(pos, move, false);
            posHashList[posHashListSize++] = pos.zobristHash();
            UndoInfo ui = new UndoInfo();
            pos.makeMove(move, ui);
            if (canClaimDraw50(pos)) {
                drawStr = "draw 50 " + strMove;
            } else if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashListSize)) {
                drawStr = "draw rep " + strMove;
            }
            pos.unMakeMove(move, ui);
        }
        return drawStr;
    }

    private final static boolean canClaimDraw50(Position pos) {
        return (pos.halfMoveClock >= 100);
    }

    private final static boolean canClaimDrawRep(Position pos, long[] posHashList, int posHashListSize, int posHashFirstNew) {
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


    private int statCurrDepth = 0;
    private int statPVDepth = 0;
    private int statScore = 0;
    private boolean statIsMate = false;
    private boolean statUpperBound = false;
    private boolean statLowerBound = false;
    private int statTime = 0;
    private int statNodes = 0;
    private int statNps = 0;
    private int pvNum = 0;
    private ArrayList<String> statPV = new ArrayList<String>();
    private String statCurrMove = "";
    private int statCurrMoveNr = 0;

    private ArrayList<PvInfo> statPvInfo = new ArrayList<PvInfo>();

    private boolean depthModified = false;
    private boolean currMoveModified = false;
    private boolean pvModified = false;
    private boolean statsModified = false;

    private final void clearInfo() {
        depthModified = false;
        currMoveModified = false;
        pvModified = false;
        statsModified = false;
        statPvInfo.clear();
    }

    private final void parseInfoCmd(String[] tokens, Move ponderMove) {
        try {
            boolean havePvData = false;
            int nTokens = tokens.length;
            int i = 1;
            while (i < nTokens - 1) {
                String is = tokens[i++];
                if (is.equals("depth")) {
                    statCurrDepth = Integer.parseInt(tokens[i++]);
                    depthModified = true;
                } else if (is.equals("currmove")) {
                    statCurrMove = tokens[i++];
                    currMoveModified = true;
                } else if (is.equals("currmovenumber")) {
                    statCurrMoveNr = Integer.parseInt(tokens[i++]);
                    currMoveModified = true;
                } else if (is.equals("time")) {
                    statTime = Integer.parseInt(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("nodes")) {
                    statNodes = Integer.parseInt(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("nps")) {
                    statNps = Integer.parseInt(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("multipv")) {
                    pvNum = Integer.parseInt(tokens[i++]) - 1;
                    if (pvNum < 0) pvNum = 0;
                    if (pvNum > 255) pvNum = 255;
                    pvModified = true;
                } else if (is.equals("pv")) {
                    statPV.clear();
                    while (i < nTokens)
                        statPV.add(tokens[i++]);
                    pvModified = true;
                    havePvData = true;
                    statPVDepth = statCurrDepth;
                } else if (is.equals("score")) {
                    statIsMate = tokens[i++].equals("mate");
                    statScore = Integer.parseInt(tokens[i++]);
                    statUpperBound = false;
                    statLowerBound = false;
                    if (tokens[i].equals("upperbound")) {
                        statUpperBound = true;
                        i++;
                    } else if (tokens[i].equals("lowerbound")) {
                        statLowerBound = true;
                        i++;
                    }
                    pvModified = true;
                }
            }
            if (havePvData) {
                while (statPvInfo.size() < pvNum)
                    statPvInfo.add(new PvInfo(0, 0, 0, 0, 0, false, false, false, new ArrayList<Move>()));
                while (statPvInfo.size() <= pvNum)
                    statPvInfo.add(null);
                ArrayList<Move> moves = new ArrayList<Move>();
                if (ponderMove != null)
                    moves.add(ponderMove);
                int nMoves = statPV.size();
                for (i = 0; i < nMoves; i++)
                    moves.add(TextIO.UCIstringToMove(statPV.get(i)));
                statPvInfo.set(pvNum, new PvInfo(statPVDepth, statScore, statTime, statNodes, statNps, statIsMate,
                                             statUpperBound, statLowerBound, moves));
            }
        } catch (NumberFormatException nfe) {
            // Ignore
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // Ignore
        }
    }

    /** Notify GUI about search statistics. */
    private final synchronized void notifyGUI(Position pos, Move ponderMove) {
        if (depthModified) {
            listener.notifyDepth(statCurrDepth);
            depthModified = false;
        }
        if (currMoveModified) {
            Move m = TextIO.UCIstringToMove(statCurrMove);
            listener.notifyCurrMove(pos, m, statCurrMoveNr);
            currMoveModified = false;
        }
        if (pvModified) {
            Position notifyPos = pos;
            ArrayList<PvInfo> pvInfo = statPvInfo;
            boolean isPonder = ponderMove != null;
            if (isPonder && havePonderHit) {
                isPonder = false;

                UndoInfo ui = new UndoInfo();
                notifyPos = new Position(pos);
                notifyPos.makeMove(ponderMove, ui);

                pvInfo = new ArrayList<PvInfo>(statPvInfo.size());
                for (int i = 0; i < statPvInfo.size(); i++) {
                    PvInfo pvi = new PvInfo(statPvInfo.get(i));
                    pvi.removeFirstMove();
                    pvInfo.add(pvi);
                }
            }
            listener.notifyPV(notifyPos, pvInfo, isPonder);
            pvModified = false;
        }
        if (statsModified) {
            listener.notifyStats(statNodes, statNps, statTime);
            statsModified = false;
        }
    }
}
