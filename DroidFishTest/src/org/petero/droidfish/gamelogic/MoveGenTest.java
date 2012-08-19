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

import junit.framework.TestCase;

/**
 *
 * @author petero
 */
public class MoveGenTest extends TestCase {

    public MoveGenTest() {
    }

    /**
     * Test of pseudoLegalMoves method, of class MoveGen.
     */
    public void testPseudoLegalMoves() throws ChessParseError {
        String fen = "8/3k4/8/2n2pP1/1P6/1NB5/2QP4/R3K2R w KQ f6 0 2";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        List<String> strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("Ra1-d1"));
        assertTrue(!strMoves.contains("Ra1-e1"));
        assertTrue(!strMoves.contains("Ra1-f1"));
        assertTrue(strMoves.contains("Ra1-a7+"));
        assertTrue(strMoves.contains("Ke1-f2"));
        assertTrue(!strMoves.contains("Ke1-g3"));
        assertTrue(strMoves.contains("Bc3-f6"));
        assertTrue(!strMoves.contains("Nb3xd2"));

        // Test castling
        assertTrue(strMoves.contains("O-O"));
        assertTrue(strMoves.contains("O-O-O"));
        assertEquals(49, strMoves.size());

        pos.setPiece(Position.getSquare(4,3), Piece.BROOK);
        strMoves = getMoveList(pos, false);
        assertTrue(!strMoves.contains("O-O"));      // In check, not castling possible
        assertTrue(!strMoves.contains("O-O-O"));

        pos.setPiece(Position.getSquare(4, 3), Piece.EMPTY);
        pos.setPiece(Position.getSquare(5, 3), Piece.BROOK);
        strMoves = getMoveList(pos, false);
        assertTrue(!strMoves.contains("O-O"));      // f1 attacked, short castle not possible
        assertTrue(strMoves.contains("O-O-O"));

        pos.setPiece(Position.getSquare(5, 3), Piece.EMPTY);
        pos.setPiece(Position.getSquare(6, 3), Piece.BBISHOP);
        strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("O-O"));      // d1 attacked, long castle not possible
        assertTrue(!strMoves.contains("O-O-O"));

        pos.setPiece(Position.getSquare(6, 3), Piece.EMPTY);
        pos.setCastleMask(1 << Position.A1_CASTLE);
        strMoves = getMoveList(pos, false);
        assertTrue(!strMoves.contains("O-O"));      // short castle right has been lost
        assertTrue(strMoves.contains("O-O-O"));
    }

    /**
     * Test of pseudoLegalMoves method, of class MoveGen. Pawn moves.
     */
    public void testPawnMoves() throws ChessParseError {
        String fen = "1r2k3/P1pppp2/8/1pP3p1/1nPp2P1/n4p1P/1P2PP2/4KBNR w K b6 0 1";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        List<String> strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("c5xb6"));     // En passant capture
        assertTrue(strMoves.contains("a7-a8Q"));    // promotion
        assertTrue(strMoves.contains("a7-a8N"));    // under promotion
        assertTrue(strMoves.contains("a7xb8R#"));   // capture promotion
        assertTrue(strMoves.contains("b2-b3"));     // pawn single move
        assertTrue(strMoves.contains("b2xa3"));     // pawn capture to the left
        assertTrue(strMoves.contains("e2-e4"));     // pawn double move
        assertTrue(strMoves.contains("e2xf3"));     // pawn capture to the right
        assertEquals(22, strMoves.size());

        pos.setEpSquare(-1);
        strMoves = getMoveList(pos, false);
        assertEquals(21, strMoves.size());          // No ep, one less move possible

        // Check black pawn moves
        pos.setWhiteMove(false);
        strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("f3xe2"));
        assertTrue(strMoves.contains("d4-d3"));
        assertTrue(strMoves.contains("e7-e6"));
        assertTrue(strMoves.contains("e7-e5"));
        assertEquals(26, strMoves.size());

        // Check black pawn promotion
        pos.setPiece(Position.getSquare(0,1), Piece.BPAWN);
        strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("a2-a1Q+"));
        assertTrue(strMoves.contains("a2-a1R+"));
        assertTrue(strMoves.contains("a2-a1N"));
        assertTrue(strMoves.contains("a2-a1B"));
    }

    /**
     * Test of inCheck method, of class MoveGen.
     */
    public void testInCheck() {
        Position pos = new Position();
        pos.setPiece(Position.getSquare(4,2), Piece.WKING);
        pos.setPiece(Position.getSquare(4,7), Piece.BKING);
        assertEquals(false, MoveGen.inCheck(pos));

        pos.setPiece(Position.getSquare(3,3), Piece.BQUEEN);
        assertEquals(true, MoveGen.inCheck(pos));
        pos.setPiece(Position.getSquare(3,3), Piece.BROOK);
        assertEquals(false, MoveGen.inCheck(pos));
        pos.setPiece(Position.getSquare(3,3), Piece.BPAWN);
        assertEquals(true, MoveGen.inCheck(pos));

        pos.setPiece(Position.getSquare(3,3), Piece.EMPTY);
        pos.setPiece(Position.getSquare(5,3), Piece.WQUEEN);
        assertEquals(false, MoveGen.inCheck(pos));

        pos.setPiece(Position.getSquare(4, 6), Piece.BROOK);
        assertEquals(true, MoveGen.inCheck(pos));
        pos.setPiece(Position.getSquare(4, 4), Piece.WPAWN);
        assertEquals(false, MoveGen.inCheck(pos));

        pos.setPiece(Position.getSquare(2, 3), Piece.BKNIGHT);
        assertEquals(true, MoveGen.inCheck(pos));

        pos.setPiece(Position.getSquare(2, 3), Piece.EMPTY);
        pos.setPiece(Position.getSquare(0, 4), Piece.BKNIGHT);
        assertEquals(false, MoveGen.inCheck(pos));
    }

    /**
     * Test of removeIllegal method, of class MoveGen.
     */
    public void testRemoveIllegal() throws ChessParseError {
        Position pos = TextIO.readFEN("8/3k4/8/2n1rpP1/1P6/1NB5/2QP4/R3K2R w KQ f6 0 1");
        List<String> strMoves = getMoveList(pos, true);
        assertTrue(strMoves.contains("Qc2-e4"));
        assertTrue(strMoves.contains("Bc3xe5"));
        assertTrue(strMoves.contains("Ke1-d1"));
        assertTrue(strMoves.contains("Ke1-f1"));
        assertTrue(strMoves.contains("Ke1-f2"));
        assertEquals(5, strMoves.size());
    }

    /**
     * Test that if king capture is possible, only a king capture move is returned in the move list.
     */
    public void testKingCapture() throws ChessParseError {
        Position pos = TextIO.readFEN("8/4k3/8/8/8/8/8/4RK2 b - - 0 1");
        pos.setWhiteMove(true);
        List<String> strMoves = getMoveList(pos, false);
        assertEquals(1, strMoves.size());
        assertEquals("Re1xe7", strMoves.get(0));

        pos.setPiece(Position.getSquare(0, 2), Piece.WBISHOP);
        pos.setPiece(Position.getSquare(4, 1), Piece.WPAWN);
        strMoves = getMoveList(pos, false);
        assertEquals(1, strMoves.size());
        assertEquals("Ba3xe7", strMoves.get(0));

        pos.setPiece(Position.getSquare(1, 3), Piece.WPAWN);
        pos.setPiece(Position.getSquare(5, 5), Piece.WPAWN);
        strMoves = getMoveList(pos, false);
        assertEquals(1, strMoves.size());
        assertEquals("f6xe7", strMoves.get(0));
    }

    private List<String> getMoveList(Position pos, boolean onlyLegal) {
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        if (onlyLegal) {
            moves = MoveGen.removeIllegal(pos, moves);
        }
        ArrayList<String> strMoves = new ArrayList<String>();
        for (Move m : moves) {
            String mStr = TextIO.moveToString(pos, m, true, false);
            strMoves.add(mStr);
//            System.out.println(mStr);
        }
        return strMoves;
    }
}
