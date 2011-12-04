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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.petero.droidfish.BookOptions;
import org.petero.droidfish.GUIInterface;
import org.petero.droidfish.GameMode;
import org.petero.droidfish.PGNOptions;
import org.petero.droidfish.engine.DroidComputerPlayer;
import org.petero.droidfish.gamelogic.Game.GameState;
import org.petero.droidfish.gamelogic.GameTree.Node;

/**
 * The glue between the chess engine and the GUI.
 * @author petero
 */
public class DroidChessController {
    private DroidComputerPlayer computerPlayer = null;
    private PgnToken.PgnTokenReceiver gameTextListener = null;
    private BookOptions bookOptions = new BookOptions();
    private Game game = null;
    private Move ponderMove = null;
    private GUIInterface gui;
    private GameMode gameMode;
    private PGNOptions pgnOptions;
    private Thread computerThread;
    private Thread analysisThread;

    private String engine = "";
    private int strength = 1000;
    private int numPV = 1;

    private int timeControl;
    private int movesPerSession;
    private int timeIncrement;

    class SearchListener implements org.petero.droidfish.gamelogic.SearchListener {
        private int currDepth = 0;
        private int currMoveNr = 0;
        private String currMove = "";
        private int currNodes = 0;
        private int currNps = 0;
        private int currTime = 0;

        private boolean whiteMove = true;
        private String bookInfo = "";
        private List<Move> bookMoves = null;

        private ArrayList<PvInfo> pvInfoV = new ArrayList<PvInfo>();

        public final void clearSearchInfo() {
            pvInfoV.clear();
            currDepth = 0;
            bookInfo = "";
            bookMoves = null;
            setSearchInfo();
        }

        private final void setSearchInfo() {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < pvInfoV.size(); i++) {
                PvInfo pvi = pvInfoV.get(i);
                if (pvi.depth <= 0)
                    continue;
                buf.append(String.format("[%d] ", pvi.depth));
                boolean negateScore = !whiteMove && gui.whiteBasedScores();
                if (pvi.upperBound || pvi.lowerBound) {
                    boolean upper = pvi.upperBound ^ negateScore;
                    buf.append(upper ? "<=" : ">=");
                }
                int score = negateScore ? -pvi.score : pvi.score;
                if (pvi.isMate) {
                    buf.append(String.format("m%d", score));
                } else {
                    buf.append(String.format("%.2f", score / 100.0));
                }

                buf.append(pvi.pvStr);
                buf.append("\n");
            }
            if (currDepth > 0) {
                buf.append(String.format("d:%d %d:%s t:%.2f n:%d nps:%d", currDepth,
                        currMoveNr, currMove, currTime / 1000.0, currNodes, currNps));
            }
            final String newPV = buf.toString();
            final String newBookInfo = bookInfo;
            final SearchStatus localSS = ss;
            gui.runOnUIThread(new Runnable() {
                public void run() {
                    if (!localSS.searchResultWanted && (bookMoves != null))
                        return;
                    ArrayList<ArrayList<Move>> pvMoves = new ArrayList<ArrayList<Move>>();
                    for (int i = 0; i < pvInfoV.size(); i++)
                        pvMoves.add(pvInfoV.get(i).pv);
                    gui.setThinkingInfo(newPV, newBookInfo, pvMoves, bookMoves);
                }
            });
        }

        @Override
        public void notifyDepth(int depth) {
            currDepth = depth;
            setSearchInfo();
        }

        @Override
        public void notifyCurrMove(Position pos, Move m, int moveNr) {
            currMove = TextIO.moveToString(pos, m, false);
            currMoveNr = moveNr;
            setSearchInfo();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void notifyPV(Position pos, ArrayList<PvInfo> pvInfo, boolean isPonder) {
            pvInfoV = (ArrayList<PvInfo>) pvInfo.clone();
            for (PvInfo pv : pvInfo) {
                currTime = pv.time;
                currNodes = pv.nodes;
                currNps = pv.nps;

                StringBuilder buf = new StringBuilder();
                Position tmpPos = new Position(pos);
                UndoInfo ui = new UndoInfo();
                boolean first = true;
                for (Move m : pv.pv) {
                    String moveStr = TextIO.moveToString(tmpPos, m, false);
                    if (first && isPonder) {
                        buf.append(String.format(" [%s]", moveStr));
                        first = false;
                    } else {
                        buf.append(String.format(" %s", moveStr));
                    }
                    tmpPos.makeMove(m, ui);
                }
                pv.pvStr = buf.toString();
            }
            whiteMove = pos.whiteMove ^ isPonder;

            setSearchInfo();
        }

        @Override
        public void notifyStats(int nodes, int nps, int time) {
            currNodes = nodes;
            currNps = nps;
            currTime = time;
            setSearchInfo();
        }

        @Override
        public void notifyBookInfo(String bookInfo, List<Move> moveList) {
            this.bookInfo = bookInfo;
            bookMoves = moveList;
            setSearchInfo();
        }

        public void prefsChanged() {
            setSearchInfo();
        }
    }
    SearchListener listener;


    public DroidChessController(GUIInterface gui, PgnToken.PgnTokenReceiver gameTextListener, PGNOptions options) {
        this.gui = gui;
        this.gameTextListener = gameTextListener;
        pgnOptions = options;
        listener = new SearchListener();
    }

    public final void setBookOptions(BookOptions options) {
        if (!bookOptions.equals(options)) {
            bookOptions = options;
            if (computerPlayer != null) {
                computerPlayer.setBookOptions(bookOptions);
                if (analysisThread != null) {
                    stopAnalysis();
                    startAnalysis();
                }
                updateBookHints();
            }
        }
    }

    private final void updateBookHints() {
        if (gameMode != null) {
            boolean analysis = gameMode.analysisMode();
            if (!analysis && humansTurn()) {
                ss = new SearchStatus();
                Pair<String, ArrayList<Move>> bi = computerPlayer.getBookHints(game.currPos());
                listener.notifyBookInfo(bi.first, bi.second);
            }
        }
    }

    private final static class SearchStatus {
        boolean searchResultWanted = true;
    }
    SearchStatus ss = new SearchStatus();

    public final void newGame(GameMode gameMode) {
        ss.searchResultWanted = false;
        stopComputerThinking();
        stopAnalysis();
        this.gameMode = gameMode;
        ponderMove = null;
        if (computerPlayer == null) {
            computerPlayer = new DroidComputerPlayer(engine);
            computerPlayer.setListener(listener);
            computerPlayer.setBookOptions(bookOptions);
        }
        computerPlayer.setEngineStrength(engine, strength);
        computerPlayer.setNumPV(numPV);
        game = new Game(computerPlayer, gameTextListener, timeControl, movesPerSession, timeIncrement);
        setPlayerNames(game);
        updateGameMode();
    }

    public final void startGame() {
        updateComputeThreads(true);
        setSelection();
        updateGUI();
        updateGameMode();
    }

    private boolean guiPaused = false;
    public final void setGuiPaused(boolean paused) {
        guiPaused = paused;
        updateGameMode();
    }

    private final void updateGameMode() {
        if (game != null) {
            boolean gamePaused = !gameMode.clocksActive() || (humansTurn() && guiPaused);
            game.setGamePaused(gamePaused);
            updateRemainingTime();
            boolean addFirst = gameMode.clocksActive();
            game.setAddFirst(addFirst);
        }
    }

    private final void updateComputeThreads(boolean clearPV) {
        boolean analysis = gameMode.analysisMode();
        boolean computersTurn = !humansTurn();
        boolean ponder = gui.ponderMode() && !analysis && !computersTurn && (ponderMove != null);
        if (!analysis)
            stopAnalysis();
        if (!(computersTurn || ponder))
            stopComputerThinking();
        if (clearPV) {
            listener.clearSearchInfo();
            updateBookHints();
        }
        if (analysis)
            startAnalysis();
        if (computersTurn || ponder)
            startComputerThinking(ponder);
    }

    /** Set game mode. */
    public final void setGameMode(GameMode newMode) {
        if (!gameMode.equals(newMode)) {
            if (newMode.humansTurn(game.currPos().whiteMove))
                ss.searchResultWanted = false;
            gameMode = newMode;
            if (!gameMode.playerWhite() || !gameMode.playerBlack())
                setPlayerNames(game); // If computer player involved, set player names
            updateGameMode();
            ponderMove = null;
            ss.searchResultWanted = false;
            stopComputerThinking();
            updateComputeThreads(true);
            updateGUI();
        }
    }

    public final void prefsChanged() {
        updateBookHints();
        updateMoveList();
        listener.prefsChanged();
    }

    private final void setPlayerNames(Game game) {
        if (game != null) {
            String engine = (computerPlayer != null) ? computerPlayer.getEngineName() :
                                                       "Computer";
            String white = gameMode.playerWhite() ? "Player" : engine;
            String black = gameMode.playerBlack() ? "Player" : engine;
            game.tree.setPlayerNames(white, black);
        }
    }

    public final void fromByteArray(byte[] data) {
        game.fromByteArray(data);
    }

    public final byte[] toByteArray() {
        return game.tree.toByteArray();
    }

    public final String getFEN() {
        return TextIO.toFEN(game.tree.currentPos);
    }

    /** Convert current game to PGN format. */
    public final String getPGN() {
        return game.tree.toPGN(pgnOptions);
    }

    public final void setFENOrPGN(String fenPgn) throws ChessParseError {
        Game newGame = new Game(null, gameTextListener, timeControl, movesPerSession, timeIncrement);
        try {
            Position pos = TextIO.readFEN(fenPgn);
            newGame.setPos(pos);
            setPlayerNames(newGame);
        } catch (ChessParseError e) {
            // Try read as PGN instead
            if (!newGame.readPGN(fenPgn, pgnOptions)) {
                throw e;
            }
        }
        ss.searchResultWanted = false;
        game = newGame;
        game.setComputerPlayer(computerPlayer);
        gameTextListener.clear();
        updateGameMode();
        stopAnalysis();
        stopComputerThinking();
        computerPlayer.clearTT();
        ponderMove = null;
        updateComputeThreads(true);
        gui.setSelection(-1);
        updateGUI();
    }

    /** True if human's turn to make a move. (True in analysis mode.) */
    public final boolean humansTurn() {
        return gameMode.humansTurn(game.currPos().whiteMove);
    }

    /** Return true if computer player is using CPU power. */
    public final boolean computerBusy() {
        return (computerThread != null) || (analysisThread != null);
    }

    private final boolean undoMoveNoUpdate() {
        if (game.getLastMove() == null)
            return false;
        ss.searchResultWanted = false;
        game.undoMove();
        ponderMove = null;
        if (!humansTurn()) {
            if (game.getLastMove() != null) {
                game.undoMove();
                if (!humansTurn()) {
                    game.redoMove();
                }
            } else {
                // Don't undo first white move if playing black vs computer,
                // because that would cause computer to immediately make
                // a new move and the whole redo history will be lost.
                if (gameMode.playerWhite() || gameMode.playerBlack()) {
                    game.redoMove();
                    return false;
                }
            }
        }
        return true;
    }

    public final void undoMove() {
        if (game.getLastMove() != null) {
            ss.searchResultWanted = false;
            stopAnalysis();
            stopComputerThinking();
            boolean didUndo = undoMoveNoUpdate();
            updateComputeThreads(true);
            setSelection();
            if (didUndo)
                setAnimMove(game.currPos(), game.getNextMove(), false);
            updateGUI();
        }
    }

    private final void redoMoveNoUpdate() {
        if (game.canRedoMove()) {
            ss.searchResultWanted = false;
            game.redoMove();
            ponderMove = null;
            if (!humansTurn() && game.canRedoMove()) {
                game.redoMove();
                if (!humansTurn())
                    game.undoMove();
            }
        }
    }

    public final void redoMove() {
        if (game.canRedoMove()) {
            ss.searchResultWanted = false;
            stopAnalysis();
            stopComputerThinking();
            redoMoveNoUpdate();
            updateComputeThreads(true);
            setSelection();
            setAnimMove(game.prevPos(), game.getLastMove(), true);
            updateGUI();
        }
    }

    public final int numVariations() {
        return game.numVariations();
    }

    public final int currVariation() {
        return game.currVariation();
    }

    public final void changeVariation(int delta) {
        if (game.numVariations() > 1) {
            ss.searchResultWanted = false;
            stopAnalysis();
            stopComputerThinking();
            game.changeVariation(delta);
            ponderMove = null;
            updateComputeThreads(true);
            setSelection();
            updateGUI();
        }
    }

    public final void removeSubTree() {
        ss.searchResultWanted = false;
        stopAnalysis();
        stopComputerThinking();
        game.removeSubTree();
        ponderMove = null;
        updateComputeThreads(true);
        setSelection();
        updateGUI();
    }

    public final void moveVariation(int delta) {
        if (game.numVariations() > 1) {
            game.moveVariation(delta);
            updateGUI();
        }
    }

    public final void addVariation(String preComment, List<Move> pvMoves, boolean updateDefault) {
        for (int i = 0; i < pvMoves.size(); i++) {
            Move m = pvMoves.get(i);
            String moveStr = TextIO.moveToUCIString(m);
            String pre = (i == 0) ? preComment : "";
            int varNo = game.tree.addMove(moveStr, "", 0, pre, "");
            game.tree.goForward(varNo, updateDefault);
        }
        for (int i = 0; i < pvMoves.size(); i++)
            game.tree.goBack();
        gameTextListener.clear();
        updateGUI();
    }

    public final void gotoMove(int moveNr) {
        boolean needUpdate = false;
        while (game.currPos().fullMoveCounter > moveNr) { // Go backward
            int before = game.currPos().fullMoveCounter * 2 + (game.currPos().whiteMove ? 0 : 1);
            undoMoveNoUpdate();
            int after = game.currPos().fullMoveCounter * 2 + (game.currPos().whiteMove ? 0 : 1);
            if (after >= before)
                break;
            needUpdate = true;
        }
        while (game.currPos().fullMoveCounter < moveNr) { // Go forward
            int before = game.currPos().fullMoveCounter * 2 + (game.currPos().whiteMove ? 0 : 1);
            redoMoveNoUpdate();
            int after = game.currPos().fullMoveCounter * 2 + (game.currPos().whiteMove ? 0 : 1);
            if (after <= before)
                break;
            needUpdate = true;
        }
        if (needUpdate) {
            ss.searchResultWanted = false;
            stopAnalysis();
            stopComputerThinking();
            ponderMove = null;
            updateComputeThreads(true);
            setSelection();
            updateGUI();
        }
    }

    public final void gotoStartOfVariation() {
        ss.searchResultWanted = false;
        stopAnalysis();
        stopComputerThinking();
        while (true) {
            if (!undoMoveNoUpdate())
                break;
            if (game.numVariations() > 1)
                break;
        }
        ponderMove = null;
        updateComputeThreads(true);
        setSelection();
        updateGUI();
    }

    public final void makeHumanMove(Move m) {
        if (humansTurn()) {
            Position oldPos = new Position(game.currPos());
            if (doMove(m)) {
                if (m.equals(ponderMove) && !gameMode.analysisMode() &&
                    (analysisThread == null) && (computerThread != null)) {
                    computerPlayer.ponderHit(oldPos, ponderMove);
                } else {
                    ss.searchResultWanted = false;
                    stopAnalysis();
                    stopComputerThinking();
                    updateComputeThreads(true);
                }
                setAnimMove(oldPos, m, true);
                updateGUI();
            } else {
                gui.setSelection(-1);
            }
        }
    }

    public final void makeHumanNullMove() {
        if (humansTurn()) {
            int varNo = game.tree.addMove("--", "", 0, "", "");
            game.tree.goForward(varNo);
            ss.searchResultWanted = false;
            stopAnalysis();
            stopComputerThinking();
            ponderMove = null;
            updateComputeThreads(true);
            updateGUI();
            gui.setSelection(-1);
        }
    }

    private Move promoteMove;
    public final void reportPromotePiece(int choice) {
        if (promoteMove == null)
            return;
        final boolean white = game.currPos().whiteMove;
        int promoteTo;
        switch (choice) {
            case 1:
                promoteTo = white ? Piece.WROOK : Piece.BROOK;
                break;
            case 2:
                promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
                break;
            case 3:
                promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
                break;
            default:
                promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
                break;
        }
        promoteMove.promoteTo = promoteTo;
        Move m = promoteMove;
        promoteMove = null;
        makeHumanMove(m);
    }

    /**
     * Move a piece from one square to another.
     * @return True if the move was legal, false otherwise.
     */
    final private boolean doMove(Move move) {
        Position pos = game.currPos();
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        int promoteTo = move.promoteTo;
        for (Move m : moves) {
            if ((m.from == move.from) && (m.to == move.to)) {
                if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
                    promoteMove = m;
                    gui.requestPromotePiece();
                    return false;
                }
                if (m.promoteTo == promoteTo) {
                    String strMove = TextIO.moveToString(pos, m, false);
                    game.processString(strMove);
                    return true;
                }
            }
        }
        gui.reportInvalidMove(move);
        return false;
    }

    final private void updateGUI() {
        String str;
        if (game.getGameState() == Game.GameState.ALIVE) {
            str = Integer.valueOf(game.currPos().fullMoveCounter).toString();
            str += game.currPos().whiteMove ? ". White's move" : "... Black's move";
            if (computerThread != null)
                str += humansTurn() ? " (ponder)" : " (thinking)";
            if (analysisThread != null) str += " (analyzing)";
        } else {
            str = game.getGameStateString();
        }
        gui.setStatusString(str);
        updateMoveList();

        StringBuilder sb = new StringBuilder();
        if (game.tree.currentNode != game.tree.rootNode) {
            game.tree.goBack();
            Position pos = game.currPos();
            List<Move> prevVarList = game.tree.variations();
            for (int i = 0; i < prevVarList.size(); i++) {
                if (i > 0) sb.append(' ');
                if (i == game.tree.currentNode.defaultChild)
                    sb.append("<b>");
                sb.append(TextIO.moveToString(pos, prevVarList.get(i), false));
                if (i == game.tree.currentNode.defaultChild)
                    sb.append("</b>");
            }
            game.tree.goForward(-1);
        }
        gui.setPosition(game.currPos(), sb.toString(), game.tree.variations());

        updateRemainingTime();
    }

    final private void updateMoveList() {
        if (game == null)
            return;
        if (!gameTextListener.isUpToDate()) {
            PGNOptions tmpOptions = new PGNOptions();
            tmpOptions.exp.variations     = pgnOptions.view.variations;
            tmpOptions.exp.comments       = pgnOptions.view.comments;
            tmpOptions.exp.nag            = pgnOptions.view.nag;
            tmpOptions.exp.playerAction   = false;
            tmpOptions.exp.clockInfo      = false;
            tmpOptions.exp.moveNrAfterNag = false;
            gameTextListener.clear();
            game.tree.pgnTreeWalker(tmpOptions, gameTextListener);
        }
        gameTextListener.setCurrent(game.tree.currentNode);
        gui.moveListUpdated();
    }

    final public void updateRemainingTime() {
        // Update remaining time
        long now = System.currentTimeMillis();
        long wTime = game.timeController.getRemainingTime(true, now);
        long bTime = game.timeController.getRemainingTime(false, now);
        long nextUpdate = 0;
        if (game.timeController.clockRunning()) {
            long t = game.currPos().whiteMove ? wTime : bTime;
            nextUpdate = (t % 1000);
            if (nextUpdate < 0) nextUpdate += 1000;
            nextUpdate += 1;
        }
        gui.setRemainingTime(wTime, bTime, nextUpdate);
    }

    final private void setSelection() {
        Move m = game.getLastMove();
        int sq = ((m != null) && (m.from != m.to)) ? m.to : -1;
        gui.setSelection(sq);
    }

    private void setAnimMove(Position sourcePos, Move move, boolean forward) {
        gui.setAnimMove(sourcePos, move, forward);
    }

    private final synchronized void startComputerThinking(boolean ponder) {
        if (analysisThread != null) return;
        if (game.getGameState() != GameState.ALIVE) return;
        if (computerThread == null) {
            ss = new SearchStatus();
            final Pair<Position, ArrayList<Move>> ph = game.getUCIHistory();
            final Game g = game;
            final boolean haveDrawOffer = g.haveDrawOffer();
            final Position currPos = new Position(g.currPos());
            long now = System.currentTimeMillis();
            final int wTime = game.timeController.getRemainingTime(true, now);
            final int bTime = game.timeController.getRemainingTime(false, now);
            final int inc = game.timeController.getIncrement();
            int movesToGo = game.timeController.getMovesToTC();
            if (ponder && !currPos.whiteMove && (movesToGo > 0)) {
                movesToGo--;
                if (movesToGo <= 0)
                    movesToGo += game.timeController.getMovesPerSession();
            }
            final int fMovesToGo = movesToGo;
            final Move fPonderMove = ponder ? ponderMove : null;
            computerThread = new Thread(new Runnable() {
                public void run() {
                    computerPlayer.setEngineStrength(engine, strength);
                    computerPlayer.setNumPV(1);
                    final Pair<String,Move> pair =
                        computerPlayer.doSearch(ph.first, ph.second, currPos, haveDrawOffer,
                                                wTime, bTime, inc, fMovesToGo,
                                                gui.ponderMode(), fPonderMove,
                                                gui.engineThreads());
                    final String cmd = pair.first;
                    final Move ponder = pair.second;
                    final SearchStatus localSS = ss;
                    gui.runOnUIThread(new Runnable() {
                        public void run() {
                            synchronized (shutdownEngineLock) {
                                if (!localSS.searchResultWanted)
                                    return;
                                Position oldPos = new Position(g.currPos());
                                g.processString(cmd);
                                ponderMove = ponder;
                                updateGameMode();
                                gui.computerMoveMade();
                                listener.clearSearchInfo();
                                stopComputerThinking();
                                stopAnalysis(); // To force analysis to restart for new position
                                updateComputeThreads(true);
                                setSelection();
                                setAnimMove(oldPos, g.getLastMove(), true);
                                updateGUI();
                            }
                        }
                    });
                }
            });
            listener.clearSearchInfo();
            computerPlayer.shouldStop = false;
            computerThread.start();
            updateGUI();
        }
    }

    private final synchronized void stopComputerThinking() {
        if (computerThread != null) {
            computerPlayer.stopSearch();
            try {
                computerThread.join();
            } catch (InterruptedException ex) {
                System.out.printf("Could not stop computer thread%n");
            }
            computerThread = null;
            updateGUI();
        }
    }

    private final synchronized void startAnalysis() {
        if (gameMode.analysisMode()) {
            if (computerThread != null) return;
            if (analysisThread == null) {
                ss = new SearchStatus();
                final Pair<Position, ArrayList<Move>> ph = game.getUCIHistory();
                final boolean haveDrawOffer = game.haveDrawOffer();
                final Position currPos = new Position(game.currPos());
                final boolean alive = game.tree.getGameState() == GameState.ALIVE;
                analysisThread = new Thread(new Runnable() {
                    public void run() {
                        if (alive) {
                            computerPlayer.setEngineStrength(engine, 1000);
                            computerPlayer.setNumPV(numPV);
                            computerPlayer.analyze(ph.first, ph.second, currPos, haveDrawOffer,
                                                   gui.engineThreads());
                        }
                    }
                });
                listener.clearSearchInfo();
                computerPlayer.shouldStop = false;
                analysisThread.start();
                updateGUI();
            }
        }
    }

    private final synchronized void stopAnalysis() {
        if (analysisThread != null) {
            computerPlayer.stopSearch();
            try {
                analysisThread.join();
            } catch (InterruptedException ex) {
                System.out.printf("Could not stop analysis thread%n");
            }
            analysisThread = null;
            listener.clearSearchInfo();
            updateGUI();
        }
    }

    public final synchronized void setEngineStrength(String engine, int strength) {
        boolean newEngine = !engine.equals(this.engine);
        if (newEngine)
            numPV = 1;
        if (newEngine || (strength != this.strength)) {
            this.engine = engine;
            this.strength = strength;
            if (newEngine && ((analysisThread != null) || (computerThread != null))) {
                stopAnalysis();
                updateComputeThreads(true);
                updateGUI();
            }
        }
    }

    public final synchronized int maxPV() {
        if (computerPlayer == null)
            return 1;
        return computerPlayer.getMaxPV();
    }

    public final int getNumPV() {
        return this.numPV;
    }

    public final synchronized void setMultiPVMode(int numPV) {
        if (numPV < 1) numPV = 1;
        if (numPV > maxPV()) numPV = maxPV();
        if (numPV != this.numPV) {
            this.numPV = numPV;
            if (analysisThread != null) {
                stopAnalysis();
                ponderMove = null;
                updateComputeThreads(true);
                updateGUI();
            }
        }
    }

    public final synchronized void setTimeLimit(int time, int moves, int inc) {
        timeControl = time;
        movesPerSession = moves;
        timeIncrement = inc;
        if (game != null)
            game.timeController.setTimeControl(timeControl, movesPerSession, timeIncrement);
    }

    public final void stopSearch() {
        if (computerThread != null) {
            computerPlayer.stopSearch();
        }
    }

    public final void stopPonder() {
        if ((computerThread != null) && humansTurn())
            stopComputerThinking();
    }

    private Object shutdownEngineLock = new Object();
    public final synchronized void shutdownEngine() {
        synchronized (shutdownEngineLock) {
            gameMode = new GameMode(GameMode.TWO_PLAYERS);
            ss.searchResultWanted = false;
            stopComputerThinking();
            stopAnalysis();
            ponderMove = null;
            computerPlayer.shutdownEngine();
        }
    }

    /** Help human to claim a draw by trying to find and execute a valid draw claim. */
    public final boolean claimDrawIfPossible() {
        if (!findValidDrawClaim())
            return false;
        updateGUI();
        return true;
    }

    private final boolean findValidDrawClaim() {
        if (game.getGameState() != GameState.ALIVE) return true;
        game.processString("draw accept");
        if (game.getGameState() != GameState.ALIVE) return true;
        game.processString("draw rep");
        if (game.getGameState() != GameState.ALIVE) return true;
        game.processString("draw 50");
        if (game.getGameState() != GameState.ALIVE) return true;
        return false;
    }

    public final void resignGame() {
        if (game.getGameState() == GameState.ALIVE) {
            game.processString("resign");
            updateGUI();
        }
    }

    public final void setHeaders(Map<String,String> headers) {
        game.tree.setHeaders(headers);
        gameTextListener.clear();
        updateGUI();
    }

    public final void getHeaders(Map<String,String> headers) {
        game.tree.getHeaders(headers);
    }

    public static final class CommentInfo {
        public String move;
        public String preComment, postComment;
        public int nag;
    }
    public final CommentInfo getComments() {
        Node cur = game.tree.currentNode;
        CommentInfo ret = new CommentInfo();
        ret.move = cur.moveStr;
        ret.preComment = cur.preComment;
        ret.postComment = cur.postComment;
        ret.nag = cur.nag;
        return ret;
    }

    public final void setComments(CommentInfo commInfo) {
        Node cur = game.tree.currentNode;
        cur.preComment = commInfo.preComment;
        cur.postComment = commInfo.postComment;
        cur.nag = commInfo.nag;
        gameTextListener.clear();
        updateGUI();
    }
}
