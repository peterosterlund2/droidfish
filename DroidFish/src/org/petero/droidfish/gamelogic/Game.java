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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.petero.droidfish.PGNOptions;
import org.petero.droidfish.gamelogic.GameTree.Node;

/**
 *
 * @author petero
 */
public class Game {
    boolean pendingDrawOffer;
    GameTree tree;
    TimeControl timeController;
    private boolean gamePaused;
    /** If true, add new moves as mainline moves. */
    private AddMoveBehavior addMoveBehavior;

    PgnToken.PgnTokenReceiver gameTextListener;

    public Game(PgnToken.PgnTokenReceiver gameTextListener, TimeControlData tcData) {
        this.gameTextListener = gameTextListener;
        timeController = new TimeControl();
        timeController.setTimeControl(tcData);
        gamePaused = false;
        newGame();
        tree.setTimeControlData(tcData);
    }

    /** De-serialize from input stream. */
    final void readFromStream(DataInputStream dis, int version) throws IOException, ChessParseError {
        tree.readFromStream(dis, version);
        if (version >= 3)
            timeController.readFromStream(dis, version);
        updateTimeControl(true);
    }

    /** Serialize to output stream. */
    final synchronized void writeToStream(DataOutputStream dos) throws IOException {
        tree.writeToStream(dos);
        timeController.writeToStream(dos);
    }

    public final void setGamePaused(boolean gamePaused) {
        if (gamePaused != this.gamePaused) {
            this.gamePaused = gamePaused;
            updateTimeControl(false);
        }
    }

    /** Controls behavior when a new move is added to the game.*/
    public static enum AddMoveBehavior {
        /** Add the new move first in the list of variations. */
        ADD_FIRST,
        /** Add the new move last in the list of variations. */
        ADD_LAST,
        /** Remove all variations not matching the new move. */
        REPLACE
    };

    /** Set whether new moves are entered as mainline moves or variations. */
    public final void setAddFirst(AddMoveBehavior amb) {
        addMoveBehavior = amb;
    }

    /** Sets start position and discards the whole game tree. */
    final void setPos(Position pos) {
        tree.setStartPos(new Position(pos));
        updateTimeControl(false);
    }

    final boolean readPGN(String pgn, PGNOptions options) throws ChessParseError {
        boolean ret = tree.readPGN(pgn, options);
        if (ret) {
            TimeControlData tcData = tree.getTimeControlData();
            if (tcData != null)
                timeController.setTimeControl(tcData);
            updateTimeControl(tcData != null);
        }
        return ret;
    }

    final Position currPos() {
        return tree.currentPos;
    }

    final Position prevPos() {
        Move m = tree.currentNode.move;
        if (m != null) {
            tree.goBack();
            Position ret = new Position(currPos());
            tree.goForward(-1);
            return ret;
        } else {
            return currPos();
        }
    }

    public final Move getNextMove() {
        if (canRedoMove()) {
            tree.goForward(-1);
            Move ret = tree.currentNode.move;
            tree.goBack();
            return ret;
        } else {
            return null;
        }
    }

    /**
     * Update the game state according to move/command string from a player.
     * @param str The move or command to process.
     * @return True if str was understood, false otherwise.
     */
    public final boolean processString(String str) {
        if (getGameState() != GameState.ALIVE)
            return false;
        if (str.startsWith("draw ")) {
            String drawCmd = str.substring(str.indexOf(" ") + 1);
            handleDrawCmd(drawCmd);
            return true;
        } else if (str.equals("resign")) {
            addToGameTree(new Move(0, 0, 0), "resign");
            return true;
        }

        Move m = TextIO.UCIstringToMove(str);
        if (m != null)
            if (!TextIO.isValid(currPos(), m))
                m = null;
        if (m == null)
            m = TextIO.stringToMove(currPos(), str);
        if (m == null)
            return false;

        addToGameTree(m, pendingDrawOffer ? "draw offer" : "");
        return true;
    }

    private final void addToGameTree(Move m, String playerAction) {
        if (m.equals(new Move(0, 0, 0))) { // Don't create more than one game-ending move at a node
            List<Move> varMoves = tree.variations();
            for (int i = varMoves.size() - 1; i >= 0; i--) {
                if (varMoves.get(i).equals(m)) {
                    tree.deleteVariation(i);
                }
            }
        }

        boolean movePresent = false;
        int varNo;
        {
            ArrayList<Move> varMoves = tree.variations();
            int nVars = varMoves.size();
            if (addMoveBehavior == AddMoveBehavior.REPLACE) {
                boolean modified = false;
                for (int i = nVars-1; i >= 0; i--) {
                    if (!m.equals(varMoves.get(i))) {
                        tree.deleteVariation(i);
                        modified = true;
                    }
                }
                if (modified) {
                    varMoves = tree.variations();
                    nVars = varMoves.size();
                }
            }
            for (varNo = 0; varNo < nVars; varNo++) {
                if (varMoves.get(varNo).equals(m)) {
                    movePresent = true;
                    break;
                }
            }
        }
        if (!movePresent) {
            String moveStr = TextIO.moveToUCIString(m);
            varNo = tree.addMove(moveStr, playerAction, 0, "", "");
        }
        int newPos = 0;
        if (addMoveBehavior == AddMoveBehavior.ADD_LAST)
            newPos = varNo;
        tree.reorderVariation(varNo, newPos);
        tree.goForward(newPos);
        int remaining = timeController.moveMade(System.currentTimeMillis(), !gamePaused);
        tree.setRemainingTime(remaining);
        updateTimeControl(true);
        pendingDrawOffer = false;
    }

    private final void updateTimeControl(boolean discardElapsed) {
        Position currPos = currPos();
        int move = currPos.fullMoveCounter;
        boolean wtm = currPos.whiteMove;
        if (discardElapsed || (move != timeController.currentMove) || (wtm != timeController.whiteToMove)) {
            int whiteBaseTime = tree.getRemainingTime(true, timeController.getInitialTime(true));
            int blackBaseTime = tree.getRemainingTime(false, timeController.getInitialTime(false));
            timeController.setCurrentMove(move, wtm, whiteBaseTime, blackBaseTime);
        }
        long now = System.currentTimeMillis();
        boolean stopTimer = gamePaused || (getGameState() != GameState.ALIVE);
        if (!stopTimer) {
            try {
                if (TextIO.readFEN(TextIO.startPosFEN).equals(currPos))
                    stopTimer = true;
            } catch (ChessParseError e) {
            }
        }
        if (stopTimer) {
            timeController.stopTimer(now);
        } else {
            timeController.startTimer(now);
        }
    }

    public final String getDrawInfo(boolean localized) {
        return tree.getGameStateInfo(localized);
    }

    /**
     * Get the last played move, or null if no moves played yet.
     */
    public final Move getLastMove() {
        return tree.currentNode.move;
    }

    /** Return true if there is a move to redo. */
    public final boolean canRedoMove() {
        int nVar = tree.variations().size();
        return nVar > 0;
    }

    /** Get number of variations in current game position. */
    public final int numVariations() {
        if (tree.currentNode == tree.rootNode)
            return 1;
        tree.goBack();
        int nChildren = tree.variations().size();
        tree.goForward(-1);
        return nChildren;
    }

    /** Get current variation in current position. */
    public final int currVariation() {
        if (tree.currentNode == tree.rootNode)
            return 0;
        tree.goBack();
        int defChild = tree.currentNode.defaultChild;
        tree.goForward(-1);
        return defChild;
    }

    /** Go to a new variation in the game tree. */
    public final void changeVariation(int delta) {
        if (tree.currentNode == tree.rootNode)
            return;
        tree.goBack();
        int defChild = tree.currentNode.defaultChild;
        int nChildren = tree.variations().size();
        int newChild = defChild + delta;
        newChild = Math.max(newChild, 0);
        newChild = Math.min(newChild, nChildren - 1);
        tree.goForward(newChild);
        pendingDrawOffer = false;
        updateTimeControl(true);
    }

    /** Move current variation up/down in the game tree. */
    public final void moveVariation(int delta) {
        int nBack = 0;
        boolean found = false;
        while (tree.currentNode != tree.rootNode) {
            tree.goBack();
            nBack++;
            if (((delta < 0) && tree.currentNode.defaultChild > 0) ||
                ((delta > 0) && tree.currentNode.defaultChild < tree.variations().size() - 1)) {
                found = true;
                break;
            }
        }
        if (found) {
            int varNo = tree.currentNode.defaultChild;
            int nChildren = tree.variations().size();
            int newPos = varNo + delta;
            newPos = Math.max(newPos, 0);
            newPos = Math.min(newPos, nChildren - 1);
            tree.reorderVariation(varNo, newPos);
            tree.goForward(newPos);
            nBack--;
        }
        while (nBack > 0) {
            tree.goForward(-1);
            nBack--;
        }
        pendingDrawOffer = false;
        updateTimeControl(true);
    }

    /** Return true if the current variation can be moved up/down. */
    public final boolean canMoveVariation(int delta) {
        int nBack = 0;
        boolean found = false;
        while (tree.currentNode != tree.rootNode) {
            tree.goBack();
            nBack++;
            if (((delta < 0) && tree.currentNode.defaultChild > 0) ||
                ((delta > 0) && tree.currentNode.defaultChild < tree.variations().size() - 1)) {
                found = true;
                break;
            }
        }
        while (nBack > 0) {
            tree.goForward(-1);
            nBack--;
        }
        return found;
    }

    /** Delete whole game sub-tree rooted at current position. */
    public final void removeSubTree() {
        if (getLastMove() != null) {
            tree.goBack();
            int defChild = tree.currentNode.defaultChild;
            tree.deleteVariation(defChild);
        } else {
            while (canRedoMove())
                tree.deleteVariation(0);
        }
        pendingDrawOffer = false;
        updateTimeControl(true);
    }

    public static enum GameState {
        ALIVE,
        WHITE_MATE,         // White mates
        BLACK_MATE,         // Black mates
        WHITE_STALEMATE,    // White is stalemated
        BLACK_STALEMATE,    // Black is stalemated
        DRAW_REP,           // Draw by 3-fold repetition
        DRAW_50,            // Draw by 50 move rule
        DRAW_NO_MATE,       // Draw by impossibility of check mate
        DRAW_AGREE,         // Draw by agreement
        RESIGN_WHITE,       // White resigns
        RESIGN_BLACK        // Black resigns
    }

    /**
     * Get the current state (draw, mate, ongoing, etc) of the game.
     */
    public final GameState getGameState() {
        return tree.getGameState();
    }

    /**
     * Check if a draw offer is available.
     * @return True if the current player has the option to accept a draw offer.
     */
    public final boolean haveDrawOffer() {
        return tree.currentNode.playerAction.equals("draw offer");
    }

    public final void undoMove() {
        Move m = tree.currentNode.move;
        if (m != null) {
            tree.goBack();
            pendingDrawOffer = false;
            updateTimeControl(true);
        }
    }

    public final void redoMove() {
        if (canRedoMove()) {
            tree.goForward(-1);
            pendingDrawOffer = false;
            updateTimeControl(true);
        }
    }

    /** Go to given node in game tree.
     * @return True if current node changed, false otherwise. */
    public final boolean goNode(Node node) {
        if (!tree.goNode(node))
            return false;
        pendingDrawOffer = false;
        updateTimeControl(true);
        return true;
    }

    public final void newGame() {
        tree = new GameTree(gameTextListener);
        timeController.reset();
        pendingDrawOffer = false;
        updateTimeControl(true);
    }


    /**
     * Return the last zeroing position and a list of moves
     * to go from that position to the current position.
     */
    public final Pair<Position, ArrayList<Move>> getUCIHistory() {
        Pair<List<Node>, Integer> ml = tree.getMoveList();
        List<Node> moveList = ml.first;
        Position pos = new Position(tree.startPos);
        ArrayList<Move> mList = new ArrayList<Move>();
        Position currPos = new Position(pos);
        UndoInfo ui = new UndoInfo();
        int nMoves = ml.second;
        for (int i = 0; i < nMoves; i++) {
            Node n = moveList.get(i);
            mList.add(n.move);
            currPos.makeMove(n.move, ui);
            if (currPos.halfMoveClock == 0) {
                pos = new Position(currPos);
                mList.clear();
            }
        }
        return new Pair<Position, ArrayList<Move>>(pos, mList);
    }

    private final void handleDrawCmd(String drawCmd) {
        Position pos = tree.currentPos;
        if (drawCmd.startsWith("rep") || drawCmd.startsWith("50")) {
            boolean rep = drawCmd.startsWith("rep");
            Move m = null;
            String ms = null;
            int firstSpace = drawCmd.indexOf(" ");
            if (firstSpace >= 0) {
                ms = drawCmd.substring(firstSpace + 1);
                if (ms.length() > 0) {
                    m = TextIO.stringToMove(pos, ms);
                }
            }
            boolean valid;
            if (rep) {
                valid = false;
                UndoInfo ui = new UndoInfo();
                int repetitions = 0;
                Position posToCompare = new Position(tree.currentPos);
                if (m != null) {
                    posToCompare.makeMove(m, ui);
                    repetitions = 1;
                }
                Pair<List<Node>, Integer> ml = tree.getMoveList();
                List<Node> moveList = ml.first;
                Position tmpPos = new Position(tree.startPos);
                if (tmpPos.drawRuleEquals(posToCompare))
                    repetitions++;
                int nMoves = ml.second;
                for (int i = 0; i < nMoves; i++) {
                    Node n = moveList.get(i);
                    tmpPos.makeMove(n.move, ui);
                    TextIO.fixupEPSquare(tmpPos);
                    if (tmpPos.drawRuleEquals(posToCompare))
                        repetitions++;
                }
                if (repetitions >= 3)
                    valid = true;
            } else {
                Position tmpPos = new Position(pos);
                if (m != null) {
                    UndoInfo ui = new UndoInfo();
                    tmpPos.makeMove(m, ui);
                }
                valid = tmpPos.halfMoveClock >= 100;
            }
            if (valid) {
                String playerAction = rep ? "draw rep" : "draw 50";
                if (m != null)
                    playerAction += " " + TextIO.moveToString(pos, m, false, false);
                addToGameTree(new Move(0, 0, 0), playerAction);
            } else {
                pendingDrawOffer = true;
                if (m != null) {
                    processString(ms);
                }
            }
        } else if (drawCmd.startsWith("offer ")) {
            pendingDrawOffer = true;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (TextIO.stringToMove(pos, ms) != null) {
                processString(ms);
            }
        } else if (drawCmd.equals("accept")) {
            if (haveDrawOffer())
                addToGameTree(new Move(0, 0, 0), "draw accept");
        }
    }
}
