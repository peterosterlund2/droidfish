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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author petero
 */
public class Game {
    protected List<Move> moveList = null;
    protected List<UndoInfo> uiInfoList = null;
    List<Boolean> drawOfferList = null;
    protected int currentMove;
    boolean pendingDrawOffer;
    GameState drawState;
    String drawStateMoveStr; // Move required to claim DRAW_REP or DRAW_50
    GameState resignState;
    public Position pos = null;
    protected Player whitePlayer;
    protected Player blackPlayer;
    
    public Game(Player whitePlayer, Player blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        handleCommand("new");
    }

    /**
     * Update the game state according to move/command string from a player.
     * @param str The move or command to process.
     * @return True if str was understood, false otherwise.
     */
    public boolean processString(String str) {
        if (handleCommand(str)) {
            return true;
        }
        if (getGameState() != GameState.ALIVE) {
            return false;
        }

        Move m = TextIO.stringToMove(pos, str);
        if (m == null) {
            return false;
        }

        UndoInfo ui = new UndoInfo();
        pos.makeMove(m, ui);
        TextIO.fixupEPSquare(pos);
        while (currentMove < moveList.size()) {
            moveList.remove(currentMove);
            uiInfoList.remove(currentMove);
            drawOfferList.remove(currentMove);
        }
        moveList.add(m);
        uiInfoList.add(ui);
        drawOfferList.add(pendingDrawOffer);
        pendingDrawOffer = false;
        currentMove++;
        return true;
    }

    public final String getGameStateString() {
        switch (getGameState()) {
            case ALIVE:
                return "";
            case WHITE_MATE:
                return "Game over, white mates!";
            case BLACK_MATE:
                return "Game over, black mates!";
            case WHITE_STALEMATE:
            case BLACK_STALEMATE:
                return "Game over, draw by stalemate!";
            case DRAW_REP:
            {
                String ret = "Game over, draw by repetition!";
                if ((drawStateMoveStr != null) && (drawStateMoveStr.length() > 0)) {
                    ret = ret + " [" + drawStateMoveStr + "]";
                }
                return ret;
            }
            case DRAW_50:
            {
                String ret = "Game over, draw by 50 move rule!";
                if ((drawStateMoveStr != null) && (drawStateMoveStr.length() > 0)) {
                    ret = ret + " [" + drawStateMoveStr + "]";  
                }
                return ret;
            }
            case DRAW_NO_MATE:
                return "Game over, draw by impossibility of mate!";
            case DRAW_AGREE:
                return "Game over, draw by agreement!";
            case RESIGN_WHITE:
                return "Game over, white resigns!";
            case RESIGN_BLACK:
                return "Game over, black resigns!";
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Get the last played move, or null if no moves played yet.
     */
    public Move getLastMove() {
        Move m = null;
        if (currentMove > 0) {
            m = moveList.get(currentMove - 1);
        }
        return m;
    }

    public enum GameState {
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
     * Get the current state of the game.
     */
    public GameState getGameState() {
        MoveGen.MoveList moves = new MoveGen().pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, moves);
        if (moves.size == 0) {
            if (MoveGen.inCheck(pos)) {
                return pos.whiteMove ? GameState.BLACK_MATE : GameState.WHITE_MATE;
            } else {
                return pos.whiteMove ? GameState.WHITE_STALEMATE : GameState.BLACK_STALEMATE;
            }
        }
        if (insufficientMaterial()) {
            return GameState.DRAW_NO_MATE;
        }
        if (resignState != GameState.ALIVE) {
            return resignState;
        }
        return drawState;
    }

    /**
     * Check if a draw offer is available.
     * @return True if the current player has the option to accept a draw offer.
     */
    public boolean haveDrawOffer() {
        if (currentMove > 0) {
            return drawOfferList.get(currentMove - 1);
        } else {
            return false;
        }
    }
    
    /**
     * Handle a special command.
     * @param moveStr  The command to handle
     * @return  True if command handled, false otherwise.
     */
    protected boolean handleCommand(String moveStr) {
        if (moveStr.equals("new")) {
            moveList = new ArrayList<Move>();
            uiInfoList = new ArrayList<UndoInfo>();
            drawOfferList = new ArrayList<Boolean>();
            currentMove = 0;
            pendingDrawOffer = false;
            drawState = GameState.ALIVE;
            resignState = GameState.ALIVE;
            try {
                pos = TextIO.readFEN(TextIO.startPosFEN);
            } catch (ChessParseError ex) {
                throw new RuntimeException();
            }
            whitePlayer.clearTT();
            blackPlayer.clearTT();
            activateHumanPlayer();
            return true;
        } else if (moveStr.equals("undo")) {
            if (currentMove > 0) {
                pos.unMakeMove(moveList.get(currentMove - 1), uiInfoList.get(currentMove - 1));
                currentMove--;
                pendingDrawOffer = false;
                drawState = GameState.ALIVE;
                resignState = GameState.ALIVE;
                return handleCommand("swap");
            } else {
                System.out.println("Nothing to undo");
            }
            return true;
        } else if (moveStr.equals("redo")) {
            if (currentMove < moveList.size()) {
                pos.makeMove(moveList.get(currentMove), uiInfoList.get(currentMove));
                currentMove++;
                pendingDrawOffer = false;
                return handleCommand("swap");
            } else {
                System.out.println("Nothing to redo");
            }
            return true;
        } else if (moveStr.equals("swap") || moveStr.equals("go")) {
            Player tmp = whitePlayer;
            whitePlayer = blackPlayer;
            blackPlayer = tmp;
            return true;
        } else if (moveStr.equals("list")) {
            listMoves();
            return true;
        } else if (moveStr.startsWith("setpos ")) {
            String fen = moveStr.substring(moveStr.indexOf(" ") + 1);
            Position newPos = null;
            try {
                newPos = TextIO.readFEN(fen);
            } catch (ChessParseError ex) {
                System.out.printf("Invalid FEN: %s (%s)%n", fen, ex.getMessage());
            }
            if (newPos != null) {
                handleCommand("new");
                pos = newPos;
                activateHumanPlayer();
            }
            return true;
        } else if (moveStr.equals("getpos")) {
            String fen = TextIO.toFEN(pos);
            System.out.println(fen);
            return true;
        } else if (moveStr.startsWith("draw ")) {
            if (getGameState() == GameState.ALIVE) {
                String drawCmd = moveStr.substring(moveStr.indexOf(" ") + 1);
                return handleDrawCmd(drawCmd);
            } else {
                return true;
            }
        } else if (moveStr.equals("resign")) {
            if (getGameState()== GameState.ALIVE) {
                resignState = pos.whiteMove ? GameState.RESIGN_WHITE : GameState.RESIGN_BLACK;
                return true;
            } else {
                return true;
            }
        } else if (moveStr.startsWith("book")) {
            String bookCmd = moveStr.substring(moveStr.indexOf(" ") + 1);
            return handleBookCmd(bookCmd);
        } else if (moveStr.startsWith("time")) {
            try {
                String timeStr = moveStr.substring(moveStr.indexOf(" ") + 1);
                int timeLimit = Integer.parseInt(timeStr);
                whitePlayer.timeLimit(timeLimit, timeLimit, false);
                blackPlayer.timeLimit(timeLimit, timeLimit, false);
                return true;
            }
            catch (NumberFormatException nfe) {
                System.out.printf("Number format exception: %s\n", nfe.getMessage());
                return false;
            }
        } else if (moveStr.startsWith("perft ")) {
            try {
                String depthStr = moveStr.substring(moveStr.indexOf(" ") + 1);
                int depth = Integer.parseInt(depthStr);
                MoveGen moveGen = new MoveGen();
                long t0 = System.currentTimeMillis();
                long nodes = perfT(moveGen, pos, depth);
                long t1 = System.currentTimeMillis();
                System.out.printf("perft(%d) = %d, t=%.3fs\n", depth, nodes, (t1 - t0)*1e-3);
            }
            catch (NumberFormatException nfe) {
                System.out.printf("Number format exception: %s\n", nfe.getMessage());
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /** Swap players around if needed to make the human player in control of the next move. */
    protected void activateHumanPlayer() {
        if (!(pos.whiteMove ? whitePlayer : blackPlayer).isHumanPlayer()) {
            Player tmp = whitePlayer;
            whitePlayer = blackPlayer;
            blackPlayer = tmp;
        }
    }

    public List<String> getPosHistory() {
        List<String> ret = new ArrayList<String>();
        
        Position pos = new Position(this.pos);
        for (int i = currentMove; i > 0; i--) {
            pos.unMakeMove(moveList.get(i - 1), uiInfoList.get(i - 1));
        }
        ret.add(TextIO.toFEN(pos)); // Store initial FEN

        StringBuilder moves = new StringBuilder();
        for (int i = 0; i < moveList.size(); i++) {
            Move move = moveList.get(i);
            String strMove = TextIO.moveToString(pos, move, false);
            moves.append(String.format(Locale.US, " %s", strMove));
            UndoInfo ui = new UndoInfo();
            pos.makeMove(move, ui);
        }
        ret.add(moves.toString()); // Store move list string
        int numUndo = moveList.size() - currentMove;
        ret.add(((Integer)numUndo).toString());
        return ret;
    }

    /**
     * Print a list of all moves.
     */
    private void listMoves() {
        String movesStr = getMoveListString(false);
        System.out.printf("%s", movesStr);
    }

    final public String getMoveListString(boolean compressed) {
        StringBuilder ret = new StringBuilder();

        // Undo all moves in move history.
        Position pos = new Position(this.pos);
        for (int i = currentMove; i > 0; i--) {
            pos.unMakeMove(moveList.get(i - 1), uiInfoList.get(i - 1));
        }

        // Print all moves
        String whiteMove = "";
        String blackMove = "";
        for (int i = 0; i < currentMove; i++) {
            Move move = moveList.get(i);
            String strMove = TextIO.moveToString(pos, move, false);
            if (drawOfferList.get(i)) {
                strMove += " (d)";
            }
            if (pos.whiteMove) {
                whiteMove = strMove;
            } else {
                blackMove = strMove;
                if (whiteMove.length() == 0) {
                    whiteMove = "...";
                }
                if (compressed) {
                    ret.append(String.format(Locale.US, "%d. %s %s ",
                            pos.fullMoveCounter, whiteMove, blackMove));
                } else {
                    ret.append(String.format(Locale.US, "%3d.  %-10s %-10s%n",
                            pos.fullMoveCounter, whiteMove, blackMove));
                }
                whiteMove = "";
                blackMove = "";
            }
            UndoInfo ui = new UndoInfo();
            pos.makeMove(move, ui);
        }
        if ((whiteMove.length() > 0) || (blackMove.length() > 0)) {
            if (whiteMove.length() == 0) {
                whiteMove = "...";
            }
            if (compressed) {
                ret.append(String.format(Locale.US, "%d. %s %s ",
                        pos.fullMoveCounter, whiteMove, blackMove));
            } else {
                ret.append(String.format(Locale.US, "%3d.  %-8s %-8s%n",
                        pos.fullMoveCounter, whiteMove, blackMove));
            }
        }
        String gameResult = getPGNResultString();
        if (!gameResult.equals("*")) {
            if (compressed) {
                ret.append(gameResult);
            } else {
                ret.append(String.format(Locale.US, "%s%n", gameResult));
            }
        }
        return ret.toString();
    }
    
    public final String getPGNResultString() {
        String gameResult = "*";
        switch (getGameState()) {
            case ALIVE:
                break;
            case WHITE_MATE:
            case RESIGN_BLACK:
                gameResult = "1-0";
                break;
            case BLACK_MATE:
            case RESIGN_WHITE:
                gameResult = "0-1";
                break;
            case WHITE_STALEMATE:
            case BLACK_STALEMATE:
            case DRAW_REP:
            case DRAW_50:
            case DRAW_NO_MATE:
            case DRAW_AGREE:
                gameResult = "1/2-1/2";
                break;
        }
        return gameResult;
    }

    /** Return a list of previous positions in this game, back to the last "zeroing" move. */
    public ArrayList<Position> getHistory() {
        ArrayList<Position> posList = new ArrayList<Position>();
        Position pos = new Position(this.pos);
        for (int i = currentMove; i > 0; i--) {
            if (pos.halfMoveClock == 0)
                break;
            pos.unMakeMove(moveList.get(i- 1), uiInfoList.get(i- 1));
            posList.add(new Position(pos));
        }
        Collections.reverse(posList);
        return posList;
    }

    private boolean handleDrawCmd(String drawCmd) {
        if (drawCmd.startsWith("rep") || drawCmd.startsWith("50")) {
            boolean rep = drawCmd.startsWith("rep");
            Move m = null;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (ms.length() > 0) {
                m = TextIO.stringToMove(pos, ms);
            }
            boolean valid;
            if (rep) {
                valid = false;
                List<Position> oldPositions = new ArrayList<Position>();
                if (m != null) {
                    UndoInfo ui = new UndoInfo();
                    Position tmpPos = new Position(pos);
                    tmpPos.makeMove(m, ui);
                    oldPositions.add(tmpPos);
                }
                oldPositions.add(pos);
                Position tmpPos = pos;
                for (int i = currentMove - 1; i >= 0; i--) {
                    tmpPos = new Position(tmpPos);
                    tmpPos.unMakeMove(moveList.get(i), uiInfoList.get(i));
                    oldPositions.add(tmpPos);
                }
                int repetitions = 0;
                Position firstPos = oldPositions.get(0);
                for (Position p : oldPositions) {
                    if (p.drawRuleEquals(firstPos))
                        repetitions++;
                }
                if (repetitions >= 3) {
                    valid = true;
                }
            } else {
                Position tmpPos = new Position(pos);
                if (m != null) {
                    UndoInfo ui = new UndoInfo();
                    tmpPos.makeMove(m, ui);
                }
                valid = tmpPos.halfMoveClock >= 100;
            }
            if (valid) {
                drawState = rep ? GameState.DRAW_REP : GameState.DRAW_50;
                drawStateMoveStr = null;
                if (m != null) {
                    drawStateMoveStr = TextIO.moveToString(pos, m, false);
                }
            } else {
                pendingDrawOffer = true;
                if (m != null) {
                    processString(ms);
                }
            }
            return true;
        } else if (drawCmd.startsWith("offer ")) {
            pendingDrawOffer = true;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (TextIO.stringToMove(pos, ms) != null) {
                processString(ms);
            }
            return true;
        } else if (drawCmd.equals("accept")) {
            if (haveDrawOffer()) {
                drawState = GameState.DRAW_AGREE;
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean handleBookCmd(String bookCmd) {
        if (bookCmd.equals("off")) {
            whitePlayer.useBook(false);
            blackPlayer.useBook(false);
            return true;
        } else if (bookCmd.equals("on")) {
            whitePlayer.useBook(true);
            whitePlayer.useBook(true);
            return true;
        }
        return false;
    }

    private boolean insufficientMaterial() {
        if (pos.pieceTypeBB[Piece.WQUEEN] != 0) return false;
        if (pos.pieceTypeBB[Piece.WROOK]  != 0) return false;
        if (pos.pieceTypeBB[Piece.WPAWN]  != 0) return false;
        if (pos.pieceTypeBB[Piece.BQUEEN] != 0) return false;
        if (pos.pieceTypeBB[Piece.BROOK]  != 0) return false;
        if (pos.pieceTypeBB[Piece.BPAWN]  != 0) return false;
        int wb = Long.bitCount(pos.pieceTypeBB[Piece.WBISHOP]);
        int wn = Long.bitCount(pos.pieceTypeBB[Piece.WKNIGHT]);
        int bb = Long.bitCount(pos.pieceTypeBB[Piece.BBISHOP]);
        int bn = Long.bitCount(pos.pieceTypeBB[Piece.BKNIGHT]);
        if (wb + wn + bb + bn <= 1) {
            return true;    // King + bishop/knight vs king is draw
        }
        if (wn + bn == 0) {
            // Only bishops. If they are all on the same color, the position is a draw.
            long bMask = pos.pieceTypeBB[Piece.WBISHOP] | pos.pieceTypeBB[Piece.BBISHOP];
            if (((bMask & BitBoard.maskDarkSq) == 0) ||
                ((bMask & BitBoard.maskLightSq) == 0))
                return true;
        }

        return false;
    }

    final static long perfT(MoveGen moveGen, Position pos, int depth) {
        if (depth == 0)
            return 1;
        long nodes = 0;
        MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, moves);
        if (depth == 1) {
            int ret = moves.size;
            moveGen.returnMoveList(moves);
            return ret;
        }
        UndoInfo ui = new UndoInfo();
        for (int mi = 0; mi < moves.size; mi++) {
            Move m = moves.m[mi];
            pos.makeMove(m, ui);
            nodes += perfT(moveGen, pos, depth - 1);
            pos.unMakeMove(m, ui);
        }
        moveGen.returnMoveList(moves);
        return nodes;
    }
}
