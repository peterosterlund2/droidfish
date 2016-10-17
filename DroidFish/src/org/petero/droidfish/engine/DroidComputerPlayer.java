/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2014  Peter Österlund, peterosterlund2@gmail.com

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

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.petero.droidfish.EngineOptions;
import org.petero.droidfish.book.BookOptions;
import org.petero.droidfish.book.DroidBook;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.MoveGen;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.SearchListener;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.UndoInfo;
import org.petero.droidfish.gamelogic.SearchListener.PvInfo;
import org.petero.droidfish.tb.Probe;

import android.content.Context;

/**
 * A computer algorithm player.
 * @author petero
 */
public class DroidComputerPlayer {
    private UCIEngine uciEngine = null;
    private final Context context;
    private final SearchListener listener;
    private final DroidBook book;
    private EngineOptions engineOptions = new EngineOptions();
    /** Pending UCI options to send when engine becomes idle. */
    private Map<String,String> pendingOptions = new TreeMap<String,String>();

    /** Set when "ucinewgame" needs to be sent. */
    private boolean newGame = false;

    /** >1 if multiPV mode is supported. */
    private int maxPV = 1;
    private String engineName = "Computer";

    /** Engine state. */
    private static enum MainState {
        READ_OPTIONS,  // "uci" command sent, waiting for "option" and "uciok" response.
        WAIT_READY,    // "isready" sent, waiting for "readyok".
        IDLE,          // engine not searching.
        SEARCH,        // "go" sent, waiting for "bestmove"
        PONDER,        // "go" sent, waiting for "bestmove"
        ANALYZE,       // "go" sent, waiting for "bestmove" (which will be ignored)
        STOP_SEARCH,   // "stop" sent, waiting for "bestmove"
        DEAD,          // engine process has terminated
    }

    /** Engine state details. */
    private static final class EngineState {
        String engine;

        /** Current engine state. */
        MainState state;

        /** ID of current search job. */
        int searchId;

        /** Default constructor. */
        EngineState() {
            engine = "";
            setState(MainState.DEAD);
            searchId = -1;
        }

        final void setState(MainState s) {
//            System.out.printf("state: %s -> %s\n",
//                              (state != null) ? state.toString() : "(null)",
//                              s.toString());
            state = s;
        }
    }

    /** Information about current/next engine search task. */
    public static final class SearchRequest {
        int searchId;           // Unique identifier for this search request
        long startTime;         // System time (milliseconds) when search request was created

        Position prevPos;       // Position at last irreversible move
        ArrayList<Move> mList;  // Moves after prevPos, including ponderMove
        Position currPos;       // currPos = prevPos + mList - ponderMove
        boolean drawOffer;      // True if other side made draw offer

        boolean isSearch;       // True if regular search or ponder search
        boolean isAnalyze;      // True if analysis search
        int wTime;              // White remaining time, milliseconds
        int bTime;              // Black remaining time, milliseconds
        int wInc;               // White time increment per move, milliseconds
        int bInc;               // Black time increment per move, milliseconds
        int movesToGo;          // Number of moves to next time control

        String engine;          // Engine name (identifier)
        int strength;           // Engine strength setting (0 - 1000)
        int numPV;              // Number of PV lines to compute

        boolean ponderEnabled;  // True if pondering enabled, for engine time management
        Move ponderMove;        // Ponder move, or null if not a ponder search

        long[] posHashList;     // For draw decision after completed search
        int posHashListSize;    // For draw decision after completed search
        ArrayList<Move> searchMoves; // Moves to search, or null to search all moves

        /**
         * Create a request to start an engine.
         * @param id Search ID.
         * @param engine Chess engine to use for searching.
         */
        public static SearchRequest startRequest(int id, String engine) {
            SearchRequest sr = new SearchRequest();
            sr.searchId = id;
            sr.isSearch = false;
            sr.isAnalyze = false;
            sr.engine = engine;
            sr.posHashList = null;
            sr.posHashListSize = 0;
            return sr;
        }

        /**
         * Create a search request object.
         * @param id Search ID.
         * @param now Current system time.
         * @param pos An earlier position from the game.
         * @param mList List of moves to go from the earlier position to the current position.
         *              This list makes it possible for the computer to correctly handle draw
         *              by repetition/50 moves.
         * @param ponderEnabled True if pondering is enabled in the GUI. Can affect time management.
         * @param ponderMove Move to ponder, or null for non-ponder search.
         * @param engine Chess engine to use for searching.
         * @param strength Engine strength setting.
         */
        public static SearchRequest searchRequest(int id, long now,
                                                  Position prevPos, ArrayList<Move> mList,
                                                  Position currPos, boolean drawOffer,
                                                  int wTime, int bTime, int wInc, int bInc, int movesToGo,
                                                  boolean ponderEnabled, Move ponderMove,
                                                  String engine, int strength) {
            SearchRequest sr = new SearchRequest();
            sr.searchId = id;
            sr.startTime = now;
            sr.prevPos = prevPos;
            sr.mList = mList;
            sr.currPos = currPos;
            sr.drawOffer = drawOffer;
            sr.isSearch = true;
            sr.isAnalyze = false;
            sr.wTime = wTime;
            sr.bTime = bTime;
            sr.wInc = wInc;
            sr.bInc = bInc;
            sr.movesToGo = movesToGo;
            sr.engine = engine;
            sr.strength = strength;
            sr.numPV = 1;
            sr.ponderEnabled = ponderEnabled;
            sr.ponderMove = ponderMove;
            sr.posHashList = null;
            sr.posHashListSize = 0;
            return sr;
        }

        /**
         * Create an analysis request object.
         * @param id Search ID.
         * @param prevPos Position corresponding to last irreversible move.
         * @param mList   List of moves from prevPos to currPos.
         * @param currPos Position to analyze.
         * @param drawOffer True if other side have offered draw.
         * @param engine Chess engine to use for searching
         * @param numPV    Multi-PV mode.
         */
        public static SearchRequest analyzeRequest(int id, Position prevPos,
                                                   ArrayList<Move> mList,
                                                   Position currPos,
                                                   boolean drawOffer,
                                                   String engine,
                                                   int numPV) {
            SearchRequest sr = new SearchRequest();
            sr.searchId = id;
            sr.startTime = System.currentTimeMillis();
            sr.prevPos = prevPos;
            sr.mList = mList;
            sr.currPos = currPos;
            sr.drawOffer = drawOffer;
            sr.isSearch = false;
            sr.isAnalyze = true;
            sr.wTime = sr.bTime = sr.wInc = sr.bInc = sr.movesToGo = 0;
            sr.engine = engine;
            sr.strength = 1000;
            sr.numPV = numPV;
            sr.ponderEnabled = false;
            sr.ponderMove = null;
            sr.posHashList = null;
            sr.posHashListSize = 0;
            return sr;
        }

        /** Update data for ponder hit. */
        final void ponderHit() {
            if (ponderMove == null)
                return;
            UndoInfo ui = new UndoInfo();
            currPos.makeMove(ponderMove, ui);
            ponderMove = null;
        }
    }

    private EngineState engineState = new EngineState();
    private SearchRequest searchRequest = null;
    private Thread engineMonitor;

    /** Constructor. Starts engine process if not already started. */
    public DroidComputerPlayer(Context context, SearchListener listener) {
        this.context = context;
        this.listener = listener;
        book = DroidBook.getInstance();
    }

    /** Return true if computer player is consuming CPU time. */
    public final synchronized boolean computerBusy() {
        switch (engineState.state) {
        case SEARCH:
        case PONDER:
        case ANALYZE:
        case STOP_SEARCH:
            return true;
        default:
            return false;
        }
    }

    /** Return true if computer player has been loaded. */
    public final synchronized boolean computerLoaded() {
        return (engineState.state != MainState.READ_OPTIONS) &&
               (engineState.state != MainState.DEAD);
    }

    public final synchronized UCIOptions getUCIOptions() {
        UCIEngine uci = uciEngine;
        if (uci == null)
            return null;
        UCIOptions opts = uci.getUCIOptions();
        if (opts == null)
            return null;
        try {
            opts = opts.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
        for (Map.Entry<String,String> e : pendingOptions.entrySet()) {
            UCIOptions.OptionBase o = opts.getOption(e.getKey());
            if (o != null)
                o.setFromString(e.getValue());
        }
        return opts;
    }

    /** Return maximum number of PVs supported by engine. */
    public final synchronized int getMaxPV() {
        return maxPV;
    }

    /** Set opening book options. */
    public final void setBookOptions(BookOptions options) {
        book.setOptions(options);
    }

    public final void setEngineOptions(EngineOptions options) {
        engineOptions = options;
    }

    /** Send pending UCI option changes to the engine. */
    private synchronized boolean applyPendingOptions() {
        if (pendingOptions.isEmpty())
            return false;
        boolean modified = false;
        UCIEngine uci = uciEngine;
        if (uci != null)
            modified = uci.setUCIOptions(pendingOptions);
        pendingOptions.clear();
        return modified;
    }

    public synchronized void setEngineUCIOptions(Map<String,String> uciOptions) {
        pendingOptions.putAll(uciOptions);
        boolean modified = true;
        if (engineState.state == MainState.IDLE)
            modified = applyPendingOptions();
        if (modified) {
            UCIEngine uci = uciEngine;
            if (uci != null)
                uci.saveIniFile(getUCIOptions());
        }
    }

    /** Return all book moves, both as a formatted string and as a list of moves. */
    public final Pair<String, ArrayList<Move>> getBookHints(Position pos, boolean localized) {
        return book.getAllBookMoves(pos, localized);
    }

    /** Get engine reported name. */
    public final synchronized String getEngineName() {
        return engineName;
    }

    /** Sends "ucinewgame". Takes effect when next search started. */
    public final synchronized void uciNewGame() {
        newGame = true;
    }

    /** Sends "ponderhit" command to engine. */
    public final synchronized void ponderHit(int id) {
        if ((searchRequest == null) ||
            (searchRequest.ponderMove == null) ||
            (searchRequest.searchId != id))
            return;

        searchRequest.ponderHit();
        if (engineState.state != MainState.PONDER)
            searchRequest.startTime = System.currentTimeMillis();

        if (engineState.state == MainState.PONDER) {
            uciEngine.writeLineToEngine("ponderhit");
            engineState.setState(MainState.SEARCH);
            pvModified = true;
            notifyGUI();
        }
    }

    /** Stop the engine process. */
    public final synchronized void shutdownEngine() {
        if (uciEngine != null) {
            engineMonitor.interrupt();
            engineMonitor = null;
            uciEngine.shutDown();
            uciEngine = null;
        }
        engineState.setState(MainState.DEAD);
    }

    /** Start an engine, if not already started.
     * Will shut down old engine first, if needed. */
    public final synchronized void queueStartEngine(int id, String engine) {
        killOldEngine(engine);
        stopSearch();
        searchRequest = SearchRequest.startRequest(id, engine);
        handleQueue();
    }

    /** Decide what moves to search. Filters out non-optimal moves if tablebases are used. */
    private final ArrayList<Move> movesToSearch(SearchRequest sr) {
        ArrayList<Move> moves = null;
        ArrayList<Move> legalMoves = new MoveGen().legalMoves(sr.currPos);
        if (engineOptions.rootProbe)
            moves = Probe.getInstance().removeNonOptimal(sr.currPos, legalMoves);
        if (moves != null) {
            sr.searchMoves = moves;
        } else {
            moves = legalMoves;
            sr.searchMoves = null;
        }
        return moves;
    }

    /**
     * Start a search. Search result is returned to the search listener object.
     * The result can be a valid move string, in which case the move is played
     * and the turn goes over to the other player. The result can also be a special
     * command, such as "draw" and "resign".
     */
    public final synchronized void queueSearchRequest(SearchRequest sr) {
        killOldEngine(sr.engine);
        stopSearch();

        if (sr.ponderMove != null)
            sr.mList.add(sr.ponderMove);

        // Set up for draw detection
        long[] posHashList = new long[sr.mList.size()+1];
        int posHashListSize = 0;
        Position p = new Position(sr.prevPos);
        UndoInfo ui = new UndoInfo();
        for (int i = 0; i < sr.mList.size(); i++) {
            posHashList[posHashListSize++] = p.zobristHash();
            p.makeMove(sr.mList.get(i), ui);
        }

        if (sr.ponderMove == null) {
            // If we have a book move, play it.
            Move bookMove = book.getBookMove(sr.currPos);
            if (bookMove != null) {
                if (canClaimDraw(sr.currPos, posHashList, posHashListSize, bookMove) == "") {
                    listener.notifySearchResult(sr.searchId,
                                                TextIO.moveToString(sr.currPos, bookMove, false, false),
                                                null);
                    return;
                }
            }

            // If only one move to search, play it without searching
            ArrayList<Move> moves = movesToSearch(sr);
            if (moves.size() == 0) {
                listener.notifySearchResult(sr.searchId, "", null); // User set up a position where computer has no valid moves.
                return;
            }
            if (moves.size() == 1) {
                Move bestMove = moves.get(0);
                if (canClaimDraw(sr.currPos, posHashList, posHashListSize, bestMove) == "") {
                    listener.notifySearchResult(sr.searchId, TextIO.moveToUCIString(bestMove), null);
                    return;
                }
            }
        }

        sr.posHashList = posHashList;
        sr.posHashListSize = posHashListSize;

        searchRequest = sr;
        handleQueue();
    }

    /** Start analyzing a position. */
    public final synchronized void queueAnalyzeRequest(SearchRequest sr) {
        killOldEngine(sr.engine);
        stopSearch();

        // If no legal moves, there is nothing to analyze
        ArrayList<Move> moves = movesToSearch(sr);
        if (moves.size() == 0)
            return;

        searchRequest = sr;
        handleQueue();
    }

    private final void handleQueue() {
        if (engineState.state == MainState.DEAD) {
            engineState.engine = "";
            engineState.setState(MainState.IDLE);
        }
        if (engineState.state == MainState.IDLE)
            handleIdleState();
    }

    private void killOldEngine(String engine) {
        boolean needShutDown = !engine.equals(engineState.engine);
        if (!needShutDown) {
            UCIEngine uci = uciEngine;
            if (uci != null)
                needShutDown = !uci.optionsOk(engineOptions);
        }
        if (needShutDown)
            shutdownEngine();
    }

    /** Tell engine to stop searching. */
    public final synchronized boolean stopSearch() {
        searchRequest = null;
        switch (engineState.state) {
        case SEARCH:
        case PONDER:
        case ANALYZE:
            uciEngine.writeLineToEngine("stop");
            engineState.setState(MainState.STOP_SEARCH);
            return true;
        default:
            return false;
        }
    }

    /** Tell engine to move now. */
    public void moveNow() {
        if (engineState.state == MainState.SEARCH)
            uciEngine.writeLineToEngine("stop");
    }

    /** Return true if current search job is equal to id. */
    public final synchronized boolean sameSearchId(int id) {
        return (searchRequest != null) && (searchRequest.searchId == id);
    }

    /** Type of search the engine is currently requested to perform. */
    public static enum SearchType {
        NONE,
        SEARCH,
        PONDER,
        ANALYZE
    }

    /** Return type of search the engine is currently requested to perform. */
    public final synchronized SearchType getSearchType() {
        if (searchRequest == null)
            return SearchType.NONE;
        if (searchRequest.isAnalyze)
            return SearchType.ANALYZE;
        if (!searchRequest.isSearch)
            return SearchType.NONE;
        if (searchRequest.ponderMove == null)
            return SearchType.SEARCH;
        else
            return SearchType.PONDER;
    }

    /** Determine what to do next when in idle state. */
    private final void handleIdleState() {
        SearchRequest sr = searchRequest;
        if (sr == null)
            return;

        // Make sure correct engine is running
        if ((uciEngine == null) || !engineState.engine.equals(sr.engine)) {
            shutdownEngine();
            startEngine();
            return;
        }

        // Send "ucinewgame" if needed
        if (newGame) {
            uciEngine.writeLineToEngine("ucinewgame");
            uciEngine.writeLineToEngine("isready");
            engineState.setState(MainState.WAIT_READY);
            newGame = false;
            return;
        }

        // Apply pending UCI option changes
        if (applyPendingOptions()) {
            uciEngine.writeLineToEngine("isready");
            engineState.setState(MainState.WAIT_READY);
            return;
        }

        // Check if only engine start was requested
        boolean isSearch = sr.isSearch;
        boolean isAnalyze = sr.isAnalyze;
        if (!isSearch && !isAnalyze) {
            searchRequest = null;
            return;
        }

        engineState.searchId = searchRequest.searchId;

        // Reduce remaining time if there was an engine delay
        if (isSearch) {
            long now = System.currentTimeMillis();
            int delay = (int)(now - searchRequest.startTime);
            boolean wtm = searchRequest.currPos.whiteMove ^ (searchRequest.ponderMove != null);
            if (wtm)
                searchRequest.wTime = Math.max(1, searchRequest.wTime - delay);
            else
                searchRequest.bTime = Math.max(1, searchRequest.bTime - delay);
        }

        // Set strength and MultiPV parameters
        clearInfo();
        uciEngine.setStrength(searchRequest.strength);
        if (maxPV > 1) {
            int num = Math.min(maxPV, searchRequest.numPV);
            uciEngine.setOption("MultiPV", num);
        }

        if (isSearch) { // Search or ponder search
            StringBuilder posStr = new StringBuilder();
            posStr.append("position fen ");
            posStr.append(TextIO.toFEN(sr.prevPos));
            int nMoves = sr.mList.size();
            if (nMoves > 0) {
                posStr.append(" moves");
                for (int i = 0; i < nMoves; i++) {
                    posStr.append(" ");
                    posStr.append(TextIO.moveToUCIString(sr.mList.get(i)));
                }
            }
            uciEngine.setOption("Ponder", sr.ponderEnabled);
            uciEngine.setOption("UCI_AnalyseMode", false);
            uciEngine.writeLineToEngine(posStr.toString());
            if (sr.wTime < 1) sr.wTime = 1;
            if (sr.bTime < 1) sr.bTime = 1;
            StringBuilder goStr = new StringBuilder(96);
            goStr.append(String.format(Locale.US, "go wtime %d btime %d", sr.wTime, sr.bTime));
            if (sr.wInc > 0)
                goStr.append(String.format(Locale.US, " winc %d", sr.wInc));
            if (sr.bInc > 0)
                goStr.append(String.format(Locale.US, " binc %d", sr.bInc));
            if (sr.movesToGo > 0)
                goStr.append(String.format(Locale.US, " movestogo %d", sr.movesToGo));
            if (sr.ponderMove != null)
                goStr.append(" ponder");
            if (sr.searchMoves != null) {
                goStr.append(" searchmoves");
                for (Move m : sr.searchMoves) {
                    goStr.append(' ');
                    goStr.append(TextIO.moveToUCIString(m));
                }
            }
            uciEngine.writeLineToEngine(goStr.toString());
            engineState.setState((sr.ponderMove == null) ? MainState.SEARCH : MainState.PONDER);
        } else { // Analyze
            StringBuilder posStr = new StringBuilder();
            posStr.append("position fen ");
            posStr.append(TextIO.toFEN(sr.prevPos));
            int nMoves = sr.mList.size();
            if (nMoves > 0) {
                posStr.append(" moves");
                for (int i = 0; i < nMoves; i++) {
                    posStr.append(" ");
                    posStr.append(TextIO.moveToUCIString(sr.mList.get(i)));
                }
            }
            uciEngine.writeLineToEngine(posStr.toString());
            uciEngine.setOption("UCI_AnalyseMode", true);
            StringBuilder goStr = new StringBuilder(96);
            goStr.append("go infinite");
            if (sr.searchMoves != null) {
                goStr.append(" searchmoves");
                for (Move m : sr.searchMoves) {
                    goStr.append(' ');
                    goStr.append(TextIO.moveToUCIString(m));
                }
            }
            uciEngine.writeLineToEngine(goStr.toString());
            engineState.setState(MainState.ANALYZE);
        }
    }

    private final void startEngine() {
        myAssert(uciEngine == null);
        myAssert(engineMonitor == null);
        myAssert(engineState.state == MainState.DEAD);
        myAssert(searchRequest != null);

        engineName = "Computer";
        uciEngine = UCIEngineBase.getEngine(context, searchRequest.engine,
                                            engineOptions,
                                            new UCIEngine.Report() {
            @Override
            public void reportError(String errMsg) {
                if (errMsg == null)
                    errMsg = "";
                listener.reportEngineError(errMsg);
            }
        });
        uciEngine.initialize();

        final UCIEngine uci = uciEngine;
        engineMonitor = new Thread(new Runnable() {
            public void run() {
                monitorLoop(uci);
            }
        });
        engineMonitor.start();

        uciEngine.clearOptions();
        uciEngine.writeLineToEngine("uci");
        maxPV = 1;
        engineState.engine = searchRequest.engine;
        engineState.setState(MainState.READ_OPTIONS);
    }


    private final static long guiUpdateInterval = 100;
    private long lastGUIUpdate = 0;

    private final void monitorLoop(UCIEngine uci) {
        while (true) {
            int timeout = getReadTimeout();
            if (Thread.currentThread().isInterrupted())
                return;
            String s = uci.readLineFromEngine(timeout);
            if ((s == null) || Thread.currentThread().isInterrupted())
                return;
            processEngineOutput(uci, s);
            if (Thread.currentThread().isInterrupted())
                return;
            notifyGUI();
            if (Thread.currentThread().isInterrupted())
                return;
        }
    }

    /** Process one line of data from the engine. */
    private final synchronized void processEngineOutput(UCIEngine uci, String s) {
        if (Thread.currentThread().isInterrupted())
            return;

        if (s == null) {
            shutdownEngine();
            return;
        }

        if (s.length() == 0)
            return;

        switch (engineState.state) {
        case READ_OPTIONS: {
            if (readUCIOption(uci, s)) {
                pendingOptions.clear();
                uci.initOptions(engineOptions);
                uci.applyIniFile();
                uci.writeLineToEngine("ucinewgame");
                uci.writeLineToEngine("isready");
                engineState.setState(MainState.WAIT_READY);
            }
            break;
        }
        case WAIT_READY: {
            if ("readyok".equals(s)) {
                engineState.setState(MainState.IDLE);
                handleIdleState();
            }
            break;
        }
        case SEARCH:
        case PONDER:
        case ANALYZE: {
            String[] tokens = tokenize(s);
            if (tokens[0].equals("info")) {
                parseInfoCmd(tokens);
            } else if (tokens[0].equals("bestmove")) {
                String bestMove = tokens[1];
                String nextPonderMoveStr = "";
                if ((tokens.length >= 4) && (tokens[2].equals("ponder")))
                    nextPonderMoveStr = tokens[3];
                Move nextPonderMove = TextIO.UCIstringToMove(nextPonderMoveStr);

                if (engineState.state == MainState.SEARCH)
                    reportMove(bestMove, nextPonderMove);

                engineState.setState(MainState.IDLE);
                searchRequest = null;
                handleIdleState();
            }
            break;
        }
        case STOP_SEARCH: {
            String[] tokens = tokenize(s);
            if (tokens[0].equals("bestmove")) {
                uci.writeLineToEngine("isready");
                engineState.setState(MainState.WAIT_READY);
            }
            break;
        }
        default:
        }
    }

    /** Handle reading of UCI options. Return true when finished. */
    private final boolean readUCIOption(UCIEngine uci, String s) {
        String[] tokens = tokenize(s);
        if (tokens[0].equals("uciok"))
            return true;

        if (tokens[0].equals("id")) {
            if (tokens[1].equals("name")) {
                engineName = "";
                for (int i = 2; i < tokens.length; i++) {
                    if (engineName.length() > 0)
                        engineName += " ";
                    engineName += tokens[i];
                }
                listener.notifyEngineName(engineName);
            }
        } else if (tokens[0].equals("option")) {
            UCIOptions.OptionBase o = uci.registerOption(tokens);
            if (o instanceof UCIOptions.SpinOption &&
                o.name.toLowerCase(Locale.US).equals("multipv"))
                maxPV = Math.max(maxPV, ((UCIOptions.SpinOption)o).maxValue);
        }
        return false;
    }

    private final void reportMove(String bestMove, Move nextPonderMove) {
        SearchRequest sr = searchRequest;
        boolean canPonder = true;

        // Claim draw if appropriate
        if (statScore <= 0) {
            String drawClaim = canClaimDraw(sr.currPos, sr.posHashList, sr.posHashListSize,
                                            TextIO.UCIstringToMove(bestMove));
            if (drawClaim != "") {
                bestMove = drawClaim;
                canPonder = false;
            }
        }
        // Accept draw offer if engine is losing
        if (sr.drawOffer && !statIsMate && (statScore <= -300)) {
            bestMove = "draw accept";
            canPonder = false;
        }

        if (canPonder) {
            Move bestM = TextIO.stringToMove(sr.currPos, bestMove);
            if ((bestM == null) || !TextIO.isValid(sr.currPos, bestM))
                canPonder = false;
            if (canPonder) {
                Position tmpPos = new Position(sr.currPos);
                UndoInfo ui = new UndoInfo();
                tmpPos.makeMove(bestM, ui);
                if (!TextIO.isValid(tmpPos, nextPonderMove))
                    canPonder = false;
            }
        }
        if (!canPonder)
            nextPonderMove = null;
        listener.notifySearchResult(sr.searchId, bestMove, nextPonderMove);
    }

    /** Convert a string to tokens by splitting at whitespace characters. */
    private final String[] tokenize(String cmdLine) {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }

    /** Check if a draw claim is allowed, possibly after playing "move".
     * @param move The move that may have to be made before claiming draw.
     * @return The draw string that claims the draw, or empty string if draw claim not valid.
     */
    private final static String canClaimDraw(Position pos, long[] posHashList, int posHashListSize, Move move) {
        String drawStr = "";
        if (canClaimDraw50(pos)) {
            drawStr = "draw 50";
        } else if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashListSize)) {
            drawStr = "draw rep";
        } else if (move != null) {
            String strMove = TextIO.moveToString(pos, move, false, false);
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
    private long statNodes = 0;
    private long statTBHits = 0;
    private int statHash = 0;
    private int statNps = 0;
    private ArrayList<String> statPV = new ArrayList<String>();
    private String statCurrMove = "";
    private int statCurrMoveNr = 0;

    private ArrayList<PvInfo> statPvInfo = new ArrayList<PvInfo>();

    private boolean depthModified = false;
    private boolean currMoveModified = false;
    private boolean pvModified = false;
    private boolean statsModified = false;

    private final void clearInfo() {
        statCurrDepth = statPVDepth = statScore = 0;
        statIsMate = statUpperBound = statLowerBound = false;
        statTime = 0;
        statNodes = statTBHits = 0;
        statHash = 0;
        statNps = 0;
        depthModified = true;
        currMoveModified = true;
        pvModified = true;
        statsModified = true;
        statPvInfo.clear();
        statCurrMove = "";
        statCurrMoveNr = 0;
    }

    private final synchronized int getReadTimeout() {
        boolean needGuiUpdate = depthModified || currMoveModified || pvModified || statsModified;
        int timeout = 2000000000;
        if (needGuiUpdate) {
            long now = System.currentTimeMillis();
            timeout = (int)(lastGUIUpdate + guiUpdateInterval - now + 1);
            timeout = Math.max(1, Math.min(1000, timeout));
        }
        return timeout;
    }

    private final void parseInfoCmd(String[] tokens) {
        try {
            boolean havePvData = false;
            int nTokens = tokens.length;
            int i = 1;
            int pvNum = 0;
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
                    statNodes = Long.parseLong(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("tbhits")) {
                    statTBHits = Long.parseLong(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("hashfull")) {
                    statHash = Integer.parseInt(tokens[i++]);
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
                    statPvInfo.add(new PvInfo(0, 0, 0, 0, 0, 0, 0, false, false, false, new ArrayList<Move>()));
                while (statPvInfo.size() <= pvNum)
                    statPvInfo.add(null);
                ArrayList<Move> moves = new ArrayList<Move>();
                int nMoves = statPV.size();
                for (i = 0; i < nMoves; i++)
                    moves.add(TextIO.UCIstringToMove(statPV.get(i)));
                statPvInfo.set(pvNum, new PvInfo(statPVDepth, statScore, statTime, statNodes, statNps,
                                                 statTBHits, statHash,
                                                 statIsMate, statUpperBound, statLowerBound, moves));
            }
        } catch (NumberFormatException nfe) {
            // Ignore
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // Ignore
        }
    }

    /** Notify GUI about search statistics. */
    private final synchronized void notifyGUI() {
        if (Thread.currentThread().isInterrupted())
            return;

        long now = System.currentTimeMillis();
        if (now < lastGUIUpdate + guiUpdateInterval)
            return;
        lastGUIUpdate = now;

        if ((searchRequest == null) || (searchRequest.currPos == null))
            return;

        int id = engineState.searchId;
        if (depthModified) {
            listener.notifyDepth(id, statCurrDepth);
            depthModified = false;
        }
        if (currMoveModified) {
            Move m = TextIO.UCIstringToMove(statCurrMove);
            Position pos = searchRequest.currPos;
            if ((searchRequest.ponderMove != null) && (m != null)) {
                pos = new Position(pos);
                UndoInfo ui = new UndoInfo();
                pos.makeMove(searchRequest.ponderMove, ui);
            }
            listener.notifyCurrMove(id, pos, m, statCurrMoveNr);
            currMoveModified = false;
        }
        if (pvModified) {
            listener.notifyPV(id, searchRequest.currPos, statPvInfo,
                              searchRequest.ponderMove);
            pvModified = false;
        }
        if (statsModified) {
            listener.notifyStats(id, statNodes, statNps, statTBHits, statHash, statTime);
            statsModified = false;
        }
    }

    private final static void myAssert(boolean b) {
        if (!b)
            throw new RuntimeException();
    }
}
