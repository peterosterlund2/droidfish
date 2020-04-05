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

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;

import org.petero.droidfish.PGNOptions;
import org.petero.droidfish.gamelogic.Game.CommentInfo;

import junit.framework.TestCase;


public class GameTest extends TestCase {

    public GameTest() {
    }

    public void testHaveDrawOffer() {
        Game game = new Game(null, new TimeControlData());
        assertEquals(false, game.haveDrawOffer());

        Pair<Boolean,Move> p = game.processString("e4");
        boolean res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("e2e4"), p.second);
        assertEquals(false, game.haveDrawOffer());

        p = game.processString("draw offer e5");
        res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("e7e5"), p.second);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw offer does not imply draw
        assertEquals(Piece.BPAWN, game.currPos().getPiece(Position.getSquare(4, 4))); // e5 move made

        p = game.processString("draw offer Nf3");
        res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("g1f3"), p.second);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw offer does not imply draw
        assertEquals(Piece.WKNIGHT, game.currPos().getPiece(Position.getSquare(5, 2))); // Nf3 move made

        p = game.processString("Nc6");
        res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("b8c6"), p.second);
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        assertEquals(Piece.BKNIGHT, game.currPos().getPiece(Position.getSquare(2, 5))); // Nc6 move made

        p = game.processString("draw offer Bb5");
        res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("f1b5"), p.second);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        assertEquals(Piece.WBISHOP, game.currPos().getPiece(Position.getSquare(1, 4))); // Bb5 move made

        p = game.processString("draw accept");
        res = p.first;
        assertEquals(true, res);
        assertEquals(null, p.second);
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

        p = game.processString("draw offer e5");
        res = p.first;
        assertEquals(true, res);
        assertEquals(null, p.second);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.currPos()));   // Move invalid, not executed
        p = game.processString("e4");
        res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("e2e4"), p.second);
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

    public void testDraw50() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(false, game.haveDrawOffer());
        Pair<Boolean,Move> p = game.processString("draw 50");
        boolean res = p.first;
        assertEquals(true, res);
        assertEquals(null, p.second);
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw claim invalid
        p = game.processString("e4");
        res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("e2e4"), p.second);
        assertEquals(true, game.haveDrawOffer());   // Invalid claim converted to draw offer

        String fen = "8/4k3/8/P7/8/8/8/1N2K2R w K - 99 83";
        game.setPos(TextIO.readFEN(fen));
        game.processString("draw 50");
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
        p = game.processString("draw accept");
        res = p.first;
        assertEquals(true, res);
        assertEquals(null, p.second);
        assertEquals(Game.GameState.DRAW_AGREE, game.getGameState()); // Can accept previous implicit offer

        fen = "3k4/R7/3K4/8/8/8/8/8 w - - 99 78";
        game.setPos(TextIO.readFEN(fen));
        game.processString("Ra8");
        assertEquals(Game.GameState.WHITE_MATE, game.getGameState());
        game.processString("draw 50");
        assertEquals(Game.GameState.WHITE_MATE, game.getGameState()); // Can't claim draw when game over
    }

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

    public void testProcessString() throws ChessParseError {
        Game game = new Game(null, new TimeControlData());
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.currPos()));
        Pair<Boolean,Move> p = game.processString("Nf3");
        boolean res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("g1f3"), p.second);
        assertEquals(1, game.currPos().halfMoveClock);
        assertEquals(1, game.currPos().fullMoveCounter);
        p = game.processString("d5");
        res = p.first;
        assertEquals(true, res);
        assertEquals(TextIO.UCIstringToMove("d7d5"), p.second);
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

        p = game.processString("junk");
        res = p.first;
        assertEquals(false, res);
        assertEquals(null, p.second);

        game.newGame();
        p = game.processString("e7e5");
        res = p.first;
        assertEquals(false, res);
        assertEquals(null, p.second);
    }

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

    /** Test that UCI history does not include null moves. */
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
        assertEquals(2, hist.second.size());
        assertEquals(TextIO.UCIstringToMove("g1f3"), hist.second.get(0));
        assertEquals(TextIO.UCIstringToMove("e7e5"), hist.second.get(1));
        assertEquals(expectedPos, hist.first);

        game.processString("Nc3");
        hist = game.getUCIHistory();
        assertEquals(3, hist.second.size());
        assertEquals(TextIO.UCIstringToMove("g1f3"), hist.second.get(0));
        assertEquals(TextIO.UCIstringToMove("e7e5"), hist.second.get(1));
        assertEquals(TextIO.UCIstringToMove("b1c3"), hist.second.get(2));
        assertEquals(expectedPos, hist.first);

        game.processString("Nc6");
        hist = game.getUCIHistory();
        assertEquals(4, hist.second.size());
        assertEquals(TextIO.UCIstringToMove("g1f3"), hist.second.get(0));
        assertEquals(TextIO.UCIstringToMove("e7e5"), hist.second.get(1));
        assertEquals(TextIO.UCIstringToMove("b1c3"), hist.second.get(2));
        assertEquals(TextIO.UCIstringToMove("b8c6"), hist.second.get(3));
        assertEquals(expectedPos, hist.first);

        int varNo = game.tree.addMove("--", "", 0, "", "");
        assertEquals(0, varNo);
        game.tree.goForward(varNo);
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

    public final void testDuplicateMoves() throws ChessParseError {
        PGNOptions options = new PGNOptions();
        options.imp.variations = true;
        options.imp.comments = true;
        options.imp.nag = true;

        {
            Game game = new Game(null, new TimeControlData());
            boolean res = game.readPGN("[Event \"\"]\n[Result \"0-1\"]\n\ne4 0-1", options);
            assertEquals(true, res);
            assertEquals("e4", GameTreeTest.getVariationsAsString(game.tree));

            Pair<Boolean,Move> p = game.processString("e4");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("e2e4"), p.second);
            game.undoMove();
            assertEquals("e4 e4", GameTreeTest.getVariationsAsString(game.tree));

            p = game.processString("e4");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("e2e4"), p.second);
            game.undoMove();
            assertEquals("e4 e4", GameTreeTest.getVariationsAsString(game.tree));

            p = game.processString("d4");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("d2d4"), p.second);
            game.undoMove();
            assertEquals("d4 e4 e4", GameTreeTest.getVariationsAsString(game.tree));
        }
        {
            Game game = new Game(null, new TimeControlData());
            game.setPos(TextIO.readFEN("k7/5R2/6R1/2K5/8/8/8/8 w - - 0 1"));
            Pair<Boolean,Move> p = game.processString("Rg8");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("g6g8"), p.second);
            game.undoMove();
            assertEquals("Rg8#", GameTreeTest.getVariationsAsString(game.tree));

            p = game.processString("Rg8");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("g6g8"), p.second);
            game.undoMove();
            assertEquals("Rg8#", GameTreeTest.getVariationsAsString(game.tree));

            p = game.processString("Rgg7");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("g6g7"), p.second);
            game.undoMove();
            assertEquals("Rgg7 Rg8#", GameTreeTest.getVariationsAsString(game.tree));
        }
        {
            Game game = new Game(null, new TimeControlData());
            game.setPos(TextIO.readFEN("k7/8/1K6/8/8/8/2Q5/8 w - - 0 1"));
            Pair<Boolean,Move> p = game.processString("Qc7");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("c2c7"), p.second);
            game.undoMove();
            assertEquals("Qc7", GameTreeTest.getVariationsAsString(game.tree));

            p = game.processString("Qc7");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("c2c7"), p.second);
            game.undoMove();
            assertEquals("Qc7", GameTreeTest.getVariationsAsString(game.tree));

            p = game.processString("Qc8");
            assertEquals(true, (boolean)p.first);
            assertEquals(TextIO.UCIstringToMove("c2c8"), p.second);
            game.undoMove();
            assertEquals("Qc8# Qc7", GameTreeTest.getVariationsAsString(game.tree));
        }
    }

    public final void testComments() throws ChessParseError {
        PGNOptions options = new PGNOptions();
        options.imp.variations = true;
        options.imp.comments = true;
        options.imp.nag = true;
        {
            Game game = new Game(null, new TimeControlData());
            String pgn = "{a} 1. e4 {b} 1... e5 {c} ({g} 1... c6 {h} 2. Nf3 {i} 2... d5) " +
                         "2. Nf3 {d} 2... Nc6 {e} 3. d3 {f} 3... Nf6 4. Nc3 d5 *";
            boolean res = game.readPGN(pgn, options);
            assertEquals(true, res);
            Pair<CommentInfo,Boolean> p = game.getComments();
//            assertEquals(Boolean.FALSE, p.second);
            assertEquals("", p.first.preComment);
            assertEquals("a", p.first.postComment);

            game.tree.goForward(0); // At "e4"
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("a", p.first.preComment);
            assertEquals("b", p.first.postComment);

            game.tree.goForward(0); // At "e5"
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("b", p.first.preComment);
            assertEquals("c", p.first.postComment);

            game.tree.goForward(0); // At "Nf3" in mainline
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("", p.first.preComment);
            assertEquals("d", p.first.postComment);

            game.tree.goForward(0); // At "Nc6" in mainline
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("d", p.first.preComment);
            assertEquals("e", p.first.postComment);

            game.tree.goForward(0); // At "d3" in mainline
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("e", p.first.preComment);
            assertEquals("f", p.first.postComment);

            game.tree.goBack();
            game.tree.goBack();
            game.tree.goBack();
            game.tree.goBack();
            game.tree.goForward(1); // At "c6" in variation
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("g", p.first.preComment);
            assertEquals("h", p.first.postComment);

            game.tree.goForward(1); // At "Nf3" in variation
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("h", p.first.preComment);
            assertEquals("i", p.first.postComment);

            game.tree.goForward(1); // At "d5" in variation
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("i", p.first.preComment);
            assertEquals("", p.first.postComment);

            game.tree.goBack();
            game.tree.goBack(); // At "c6" in variation
            game.moveVariation(-1); // At "c6" which is now mainline
            p = game.getComments();
            assertEquals(Boolean.TRUE, p.second);
            assertEquals("b g", p.first.preComment);
            assertEquals("h", p.first.postComment);

            game.tree.goBack();
            game.tree.goForward(1); // At "e5" in variation
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("", p.first.preComment);
            assertEquals("c", p.first.postComment);

            p.first.preComment = "x";
            game.setComments(p.first);
            p = game.getComments();
            assertEquals(Boolean.FALSE, p.second);
            assertEquals("x", p.first.preComment);
            assertEquals("c", p.first.postComment);

            game.moveVariation(-1); // Still at "e5", now mainline again
            game.tree.goBack(); // At "e4"
            p = game.getComments();
            assertEquals(Boolean.TRUE, p.second);
            assertEquals("a", p.first.preComment);
            assertEquals("b g x", p.first.postComment);
        }
        {
            Game game = new Game(null, new TimeControlData());
            String pgn = "{a} 1. e4 (1. d4) *";
            boolean res = game.readPGN(pgn, options);
            assertEquals(true, res);
            Pair<CommentInfo,Boolean> p = game.getComments();
            assertEquals("", p.first.preComment);
            assertEquals("a", p.first.postComment);
        }
        {
            Game game = new Game(null, new TimeControlData());
            String pgn = "1. e4 e5 (1... c6 2. Nf3 d5) 2. Nf3 Nc6 3. d3 Nf6 4. Nc3 d5 *";
            boolean res = game.readPGN(pgn, options);
            assertEquals(true, res);

            CommentInfo info = game.getComments().first;
            info.postComment = "a";
            game.setComments(info);

            game.tree.goForward(0);
            game.tree.goForward(0);
            info = game.getComments().first;
            info.preComment = "b";
            info.postComment = "c";
            game.setComments(info);

            game.tree.goForward(0);
            game.tree.goForward(0);
            info = game.getComments().first;
            info.preComment = "d";
            info.postComment = "e";
            game.setComments(info);

            game.tree.goForward(0);
            info = game.getComments().first;
            info.postComment = "f";
            game.setComments(info);

            game.tree.goBack();
            game.tree.goBack();
            game.tree.goBack();
            game.tree.goBack();
            game.tree.goForward(1); // At "c6" in variation
            info = game.getComments().first;
            info.preComment = "g";
            info.postComment = "h";
            game.setComments(info);

            game.tree.goForward(0);
            game.tree.goForward(0);
            info = game.getComments().first;
            info.preComment = "i";
            info.postComment = "j";
            game.setComments(info);

            PGNOptions expOpts = new PGNOptions();
            expOpts.exp.variations = true;
            expOpts.exp.comments = true;
            String exported = game.tree.toPGN(expOpts);
            String[] split = exported.split("\n");
            split = Arrays.stream(split).filter(e -> !e.startsWith("[") && e.length() > 0)
                .toArray(String[]::new);
            exported = String.join(" ", split);
            String expected = "{a} 1. e4 {b} 1... e5 {c} ({g} 1... c6 {h} 2. Nf3 {i} 2... d5 {j}) " +
                              "2. Nf3 {d} 2... Nc6 {e} 3. d3 {f} 3... Nf6 4. Nc3 d5 *";
            assertEquals(expected, exported);
        }
    }
}
