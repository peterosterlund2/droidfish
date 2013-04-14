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

import junit.framework.TestCase;


/**
 *
 * @author petero
 */
public class GameTest extends TestCase {

    public GameTest() {
    }

    /**
     * Test of haveDrawOffer method, of class Game.
     */
    public void testHaveDrawOffer() {
        Game game = new Game(null, new TimeControlData());
        assertEquals(false, game.haveDrawOffer());

        boolean res = game.processString("e4");
        assertEquals(true, res);
        assertEquals(false, game.haveDrawOffer());

        res = game.processString("draw offer e5");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw offer does not imply draw
        assertEquals(Piece.BPAWN, game.currPos().getPiece(Position.getSquare(4, 4))); // e5 move made

        res = game.processString("draw offer Nf3");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw offer does not imply draw
        assertEquals(Piece.WKNIGHT, game.currPos().getPiece(Position.getSquare(5, 2))); // Nf3 move made

        res = game.processString("Nc6");
        assertEquals(true, res);
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        assertEquals(Piece.BKNIGHT, game.currPos().getPiece(Position.getSquare(2, 5))); // Nc6 move made

        res = game.processString("draw offer Bb5");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        assertEquals(Piece.WBISHOP, game.currPos().getPiece(Position.getSquare(1, 4))); // Bb5 move made

        res = game.processString("draw accept");
        assertEquals(true, res);
        assertEquals(Game.GameState.DRAW_AGREE, game.getGameState());    // Draw by agreement

        game.undoMove(); // Undo "draw accept"
        assertEquals(Piece.WBISHOP, game.currPos().getPiece(TextIO.getSquare("b5")));
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.undoMove(); // Undo "Bb5"
        assertEquals(Piece.EMPTY, game.currPos().getPiece(Position.getSquare(1, 4))); // Bb5 move undone
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.undoMove();
        assertEquals(Piece.EMPTY, game.currPos().getPiece(Position.getSquare(2, 5))); // Nc6 move undone
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());

        game.redoMove();
        assertEquals(Piece.BKNIGHT, game.currPos().getPiece(Position.getSquare(2, 5))); // Nc6 move redone
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.redoMove();
        assertEquals(Piece.WBISHOP, game.currPos().getPiece(Position.getSquare(1, 4))); // Bb5 move redone
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.redoMove();
        assertEquals(Game.GameState.DRAW_AGREE, game.getGameState());    // Can redo draw accept

        // Test draw offer in connection with invalid move
        game.newGame();
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());

        res = game.processString("draw offer e5");
        assertEquals(true, res);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.currPos()));   // Move invalid, not executed
        res = game.processString("e4");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());   // Previous draw offer still valid
        assertEquals(Piece.WPAWN, game.currPos().getPiece(Position.getSquare(4, 3))); // e4 move made

        // Undo/redo shall clear "pendingDrawOffer".
        game.newGame();
        game.processString("e4");
        game.processString("draw offer e4");       // Invalid black move
        assertEquals(true, game.pendingDrawOffer);
        game.undoMove();
        game.redoMove();
        game.processString("e5");
        assertEquals(true,game.currPos().whiteMove);
        assertEquals(false, game.haveDrawOffer());
    }

    /**
     * Test of draw by 50 move rule, of class Game.
     */
    public void testDraw50() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(false, game.haveDrawOffer());
        boolean res = game.processString("draw 50");
        assertEquals(true, res);
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw claim invalid
        res = game.processString("e4");
        assertEquals(true, game.haveDrawOffer());   // Invalid claim converted to draw offer

        String fen = "8/4k3/8/P7/8/8/8/1N2K2R w K - 99 83";
        game.setPos(TextIO.readFEN(fen));
        res = game.processString("draw 50");
        assertEquals(Game.GameState.ALIVE, game.getGameState());      // Draw claim invalid

        game.setPos(TextIO.readFEN(fen));
        game.processString("draw 50 Nc3");
        assertEquals(Game.GameState.DRAW_50, game.getGameState());    // Draw claim valid
        assertEquals("Nc3", game.getDrawInfo(false));

        game.setPos(TextIO.readFEN(fen));
        game.processString("draw 50 a6");
        assertEquals(Game.GameState.ALIVE, game.getGameState());      // Pawn move resets counter
        assertEquals(Piece.WPAWN, game.currPos().getPiece(Position.getSquare(0, 5))); // Move a6 made

        game.setPos(TextIO.readFEN(fen));
        game.processString("draw 50 O-O");
        assertEquals(Game.GameState.DRAW_50, game.getGameState());    // Castling doesn't reset counter

        game.setPos(TextIO.readFEN(fen));
        game.processString("draw 50 Kf2");
        assertEquals(Game.GameState.DRAW_50, game.getGameState());    // Loss of castling right doesn't reset counter

        game.setPos(TextIO.readFEN(fen));
        game.processString("draw 50 Ke3");
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Ke3 is invalid
        assertEquals(true, game.currPos().whiteMove);
        game.processString("a6");
        assertEquals(true, game.haveDrawOffer());   // Previous invalid claim converted to offer
        game.processString("draw 50");
        assertEquals(Game.GameState.ALIVE, game.getGameState());  // 50 move counter reset.
        res = game.processString("draw accept");
        assertEquals(true, res);
        assertEquals(Game.GameState.DRAW_AGREE, game.getGameState()); // Can accept previous implicit offer

        fen = "3k4/R7/3K4/8/8/8/8/8 w - - 99 78";
        game.setPos(TextIO.readFEN(fen));
        game.processString("Ra8");
        assertEquals(Game.GameState.WHITE_MATE, game.getGameState());
        game.processString("draw 50");
        assertEquals(Game.GameState.WHITE_MATE, game.getGameState()); // Can't claim draw when game over
    }

    /**
     * Test of draw by repetition, of class Game.
     */
    public void testDrawRep() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(false, game.haveDrawOffer());
        game.processString("Nc3");
        game.processString("Nc6");
        game.processString("Nb1");
        game.processString("Nb8");
        game.processString("Nf3");
        game.processString("Nf6");
        game.processString("Ng1");
        assertEquals(false, game.haveDrawOffer());
        game.processString("draw rep");
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Claim not valid, one more move needed
        game.processString("draw rep Nc6");
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Claim not valid, wrong move claimed
        assertEquals(Piece.BKNIGHT, game.currPos().getPiece(Position.getSquare(2, 5)));   // Move Nc6 made
        assertEquals(true, game.haveDrawOffer());
        game.undoMove();
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Piece.EMPTY, game.currPos().getPiece(Position.getSquare(2, 5)));
        game.processString("draw rep Ng8");
        assertEquals(Game.GameState.DRAW_REP, game.getGameState());
        assertEquals(Piece.EMPTY, game.currPos().getPiece(Position.getSquare(6, 7))); // Ng8 not played

        // Test draw by repetition when a "potential ep square but not real ep square" position is present.
        game.newGame();
        game.processString("e4");   // e3 is not a real epSquare here
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("Ng1");
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("draw rep Ng1");
        assertEquals(Game.GameState.DRAW_REP, game.getGameState());

        // Now check the case when e3 *is* an epSquare
        game.newGame();
        game.processString("Nf3");
        game.processString("d5");
        game.processString("Ng1");
        game.processString("d4");
        game.processString("e4");   // Here e3 is a real epSquare
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("Ng1");
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("draw rep Ng1");
        assertEquals(Game.GameState.ALIVE, game.getGameState());

        // EP capture not valid because it would leave the king in check. Therefore
        // the position has been repeated three times at the end of the move sequence.
        game.setPos(TextIO.readFEN("4k2n/8/8/8/4p3/8/3P4/3KR2N w - - 0 1"));
        game.processString("d4");
        game.processString("Ng6");
        game.processString("Ng3");
        game.processString("Nh8");
        game.processString("Nh1");
        game.processString("Ng6");
        game.processString("Ng3");
        game.processString("Nh8");
        game.processString("draw rep Nh1");
        assertEquals(Game.GameState.DRAW_REP, game.getGameState());
    }

    /**
     * Test of draw offer/accept/request command.
     */
    public void testDrawBug() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(false, game.haveDrawOffer());
        game.processString("e4");
        game.processString("c5");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("draw accept");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("draw rep");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("draw 50");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        assertEquals(Piece.EMPTY, game.tree.currentPos.getPiece(TextIO.getSquare("e5")));
    }

    /**
     * Test of resign command, of class Game.
     */
    public void testResign() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("f3");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("resign");
        assertEquals(Game.GameState.RESIGN_BLACK, game.getGameState());
        game.undoMove();
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("f3");
        game.processString("e5");
        game.processString("resign");
        assertEquals(Game.GameState.RESIGN_WHITE, game.getGameState());
        game.undoMove();
        game.processString("e5");
        game.processString("g4");
        game.processString("Qh4");
        assertEquals(Game.GameState.BLACK_MATE, game.getGameState());
        game.processString("resign");
        assertEquals(Game.GameState.BLACK_MATE, game.getGameState());   // Can't resign after game over

        String fen = "8/1p6/2rp2p1/8/p3Qqk1/6R1/PP4PK/8 b - - 3 42";
        game.setPos(TextIO.readFEN(fen));
        game.processString("resign");
        assertEquals(Game.GameState.RESIGN_BLACK, game.getGameState());
    }

    /**
     * Test of processString method, of class Game.
     */
    public void testProcessString() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.currPos()));
        boolean res = game.processString("Nf3");
        assertEquals(true, res);
        assertEquals(1, game.currPos().halfMoveClock);
        assertEquals(1, game.currPos().fullMoveCounter);
        res = game.processString("d5");
        assertEquals(true, res);
        assertEquals(0, game.currPos().halfMoveClock);
        assertEquals(2, game.currPos().fullMoveCounter);

        game.undoMove();
        assertEquals(1, game.currPos().halfMoveClock);
        assertEquals(1, game.currPos().fullMoveCounter);
        game.undoMove();
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.currPos()));
        game.undoMove();
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.currPos()));

        game.redoMove();
        assertEquals(1, game.currPos().halfMoveClock);
        assertEquals(1, game.currPos().fullMoveCounter);
        game.redoMove();
        assertEquals(0, game.currPos().halfMoveClock);
        assertEquals(2, game.currPos().fullMoveCounter);
        game.redoMove();
        assertEquals(0, game.currPos().halfMoveClock);
        assertEquals(2, game.currPos().fullMoveCounter);

        game.newGame();
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.currPos()));

        String fen = "8/8/8/4k3/8/8/2p5/5K2 b - - 47 68";
        Position pos = TextIO.readFEN(fen);
        game.setPos(TextIO.readFEN(fen));
        assertEquals(pos, game.currPos());

        res = game.processString("junk");
        assertEquals(false, res);

        game.newGame();
        res = game.processString("e7e5");
        assertEquals(false, res);
    }

    /**
     * Test of getGameState method, of class Game.
     */
    public void testGetGameState() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("f3");
        game.processString("e5");
        game.processString("g4");
        game.processString("Qh4");
        assertEquals(Game.GameState.BLACK_MATE, game.getGameState());

        game.setPos(TextIO.readFEN("5k2/5P2/5K2/8/8/8/8/8 b - - 0 1"));
        assertEquals(Game.GameState.BLACK_STALEMATE, game.getGameState());
    }

    /**
     * Test of insufficientMaterial method, of class Game.
     */
    public void testInsufficientMaterial() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.setPos(TextIO.readFEN("4k3/8/8/8/8/8/8/4K3 w - - 0 1"));
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        final int a1 = Position.getSquare(0, 0);
        Position pos = new Position(game.currPos());
        pos.setPiece(a1, Piece.WROOK); game.setPos(pos);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        pos.setPiece(a1, Piece.BQUEEN); game.setPos(pos);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        pos.setPiece(a1, Piece.WPAWN); game.setPos(pos);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        pos.setPiece(a1, Piece.BKNIGHT); game.setPos(pos);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        pos.setPiece(a1, Piece.WBISHOP); game.setPos(pos);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());

        final int c1 = Position.getSquare(2, 0);
        pos.setPiece(c1, Piece.WKNIGHT); game.setPos(pos);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        pos.setPiece(c1, Piece.BBISHOP); game.setPos(pos);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        pos.setPiece(c1, Piece.WBISHOP); game.setPos(pos);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());

        final int b2 = Position.getSquare(1, 1);
        pos.setPiece(b2, Piece.WBISHOP); game.setPos(pos);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        pos.setPiece(b2, Piece.BBISHOP); game.setPos(pos);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());

        final int b3 = Position.getSquare(1, 2);
        pos.setPiece(b3, Piece.WBISHOP); game.setPos(pos);
        assertEquals(Game.GameState.ALIVE, game.getGameState());

        // Can't force mate with KNNK, but still not an automatic draw.
        game.setPos(TextIO.readFEN("8/8/8/8/8/8/8/K3nnk1 w - - 0 1"));
        assertEquals(Game.GameState.ALIVE, game.getGameState());
    }

    /** Test that UCI history is not longer than necessary.
     * We can't expect engines to handle null moves, for example. */
    public void testUCIHistory() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());

        Pair<Position, ArrayList<Move>> hist = game.getUCIHistory();
        assertEquals(0, hist.second.size());
        Position expectedPos = new Position(game.currPos());
        assertEquals(expectedPos, hist.first);

        game.processString("Nf3");
        hist = game.getUCIHistory();
        assertEquals(1, hist.second.size());
        assertEquals(TextIO.UCIstringToMove("g1f3"), hist.second.get(0));
        assertEquals(expectedPos, hist.first);

        game.processString("e5");
        hist = game.getUCIHistory();
        expectedPos = new Position(game.currPos());
        assertEquals(0, hist.second.size());
        assertEquals(expectedPos, hist.first);

        game.processString("Nc3");
        hist = game.getUCIHistory();
        assertEquals(1, hist.second.size());
        assertEquals(TextIO.UCIstringToMove("b1c3"), hist.second.get(0));
        assertEquals(expectedPos, hist.first);

        game.processString("Nc6");
        hist = game.getUCIHistory();
        assertEquals(2, hist.second.size());
        assertEquals(TextIO.UCIstringToMove("b1c3"), hist.second.get(0));
        assertEquals(TextIO.UCIstringToMove("b8c6"), hist.second.get(1));
        assertEquals(expectedPos, hist.first);

        game.processString("--");
        hist = game.getUCIHistory();
        expectedPos = new Position(game.currPos());
        assertEquals(0, hist.second.size());
        assertEquals(expectedPos, hist.first);

        game.processString("Nf6");
        hist = game.getUCIHistory();
        assertEquals(1, hist.second.size());
        assertEquals(TextIO.UCIstringToMove("g8f6"), hist.second.get(0));
        assertEquals(expectedPos, hist.first);

        for (int i = 0; i < 6; i++)
            game.undoMove();
        hist = game.getUCIHistory();
        assertEquals(0, hist.second.size());
        expectedPos = TextIO.readFEN(TextIO.startPosFEN);
        assertEquals(expectedPos, hist.first);
    }
}
