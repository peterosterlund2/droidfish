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
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class MoveGenTest {

    public MoveGenTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of pseudoLegalMoves method, of class MoveGen.
     */
    @Test
    public void testPseudoLegalMoves() throws ChessParseError {
        System.out.println("pseudoLegalMoves");
        String fen = "8/3k4/8/2n2pP1/1P6/1NB5/2QP4/R3K2R w KQ f6 0 2";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        List<String> strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("a1d1"));
        assertTrue(!strMoves.contains("a1e1"));
        assertTrue(!strMoves.contains("a1f1"));
        assertTrue(strMoves.contains("a1a7"));
        assertTrue(strMoves.contains("e1f2"));
        assertTrue(!strMoves.contains("e1g3"));
        assertTrue(strMoves.contains("c3f6"));
        assertTrue(!strMoves.contains("b3d2"));

        // Test castling
        assertTrue(strMoves.contains("e1g1"));
        assertTrue(strMoves.contains("e1c1"));
        assertEquals(49, strMoves.size());

        pos.setPiece(Position.getSquare(4,3), Piece.BROOK);
        strMoves = getMoveList(pos, false);
        assertTrue(!strMoves.contains("e1g1"));      // In check, no castling possible
        assertTrue(!strMoves.contains("e1c1"));

        pos.setPiece(Position.getSquare(4, 3), Piece.EMPTY);
        pos.setPiece(Position.getSquare(5, 3), Piece.BROOK);
        strMoves = getMoveList(pos, false);
        assertTrue(!strMoves.contains("e1g1"));      // f1 attacked, short castle not possible
        assertTrue(strMoves.contains("e1c1"));

        pos.setPiece(Position.getSquare(5, 3), Piece.EMPTY);
        pos.setPiece(Position.getSquare(6, 3), Piece.BBISHOP);
        strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("e1g1"));      // d1 attacked, long castle not possible
        assertTrue(!strMoves.contains("e1c1"));

        pos.setPiece(Position.getSquare(6, 3), Piece.EMPTY);
        pos.setCastleMask(1 << Position.A1_CASTLE);
        strMoves = getMoveList(pos, false);
        assertTrue(!strMoves.contains("e1g1"));      // short castle right has been lost
        assertTrue(strMoves.contains("e1c1"));
    }

    /**
     * Test of pseudoLegalMoves method, of class MoveGen. Pawn moves.
     */
    @Test
    public void testPawnMoves() throws ChessParseError {
        System.out.println("pawnMoves");
        String fen = "1r2k3/P1pppp1p/8/1pP3p1/1nPp2P1/n4p1P/1P2PP2/4KBNR w K b6 0 1";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        List<String> strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("c5b6"));     // En passant capture
        assertTrue(strMoves.contains("a7a8q"));    // promotion
        assertTrue(strMoves.contains("a7a8n"));    // under promotion
        assertTrue(strMoves.contains("a7b8r"));   // capture promotion
        assertTrue(strMoves.contains("b2b3"));     // pawn single move
        assertTrue(strMoves.contains("b2a3"));     // pawn capture to the left
        assertTrue(strMoves.contains("e2e4"));     // pawn double move
        assertTrue(strMoves.contains("e2f3"));     // pawn capture to the right
        assertEquals(22, strMoves.size());

        pos.setEpSquare(-1);
        strMoves = getMoveList(pos, false);
        assertEquals(21, strMoves.size());          // No ep, one less move possible
        
        // Check black pawn moves
        pos.setWhiteMove(false);
        strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("f3e2"));
        assertTrue(strMoves.contains("d4d3"));
        assertTrue(strMoves.contains("e7e6"));
        assertTrue(strMoves.contains("e7e5"));
        assertEquals(28, strMoves.size());

        // Check black pawn promotion
        pos.setPiece(Position.getSquare(0,1), Piece.BPAWN);
        strMoves = getMoveList(pos, false);
        assertTrue(strMoves.contains("a2a1q"));
        assertTrue(strMoves.contains("a2a1r"));
        assertTrue(strMoves.contains("a2a1n"));
        assertTrue(strMoves.contains("a2a1b"));
    }

    /**
     * Test of inCheck method, of class MoveGen.
     */
    @Test
    public void testInCheck() {
        System.out.println("inCheck");
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
     * Test of givesCheck method, of class MoveGen.
     */
    @Test
    public void testGivesCheck() throws ChessParseError {
        System.out.println("givesCheck");
        Position pos = new Position();
        UndoInfo ui = new UndoInfo();
        pos.setPiece(TextIO.getSquare("e3"), Piece.WKING);
        pos.setPiece(TextIO.getSquare("e8"), Piece.BKING);
        pos.setPiece(TextIO.getSquare("c2"), Piece.WROOK);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Rc8")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Rc6")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Rc7")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Re2")));
        
        pos.setPiece(TextIO.getSquare("c2"), Piece.EMPTY);
        pos.setPiece(TextIO.getSquare("e2"), Piece.WROOK);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Kd3")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Kd4")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Ke4")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Kf2")));

        pos.setPiece(TextIO.getSquare("e4"), Piece.WBISHOP);
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Bd5")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Bc6")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Kd3")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Re1")));

        pos = TextIO.readFEN("4k3/3p4/8/8/4B3/2K5/4R3/8 w - - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Bc6")));
        pos = TextIO.readFEN("4k3/8/5K2/8/6N1/8/8/8 w - - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Ke6")));
        assertTrue(!MoveGen.givesCheck(pos, new Move(TextIO.getSquare("f6"),
                                                     TextIO.getSquare("e7"),
                                                     Piece.EMPTY)));

        pos = TextIO.readFEN("8/2k5/8/4N3/8/2K3B1/8/8 w - - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Kc4")));
        pos.setPiece(TextIO.getSquare("g3"), Piece.WROOK);
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        pos.setPiece(TextIO.getSquare("g3"), Piece.WQUEEN);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        pos.setPiece(TextIO.getSquare("g3"), Piece.WKNIGHT);
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        pos.setPiece(TextIO.getSquare("g3"), Piece.WPAWN);
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        pos.setPiece(TextIO.getSquare("c3"), Piece.EMPTY);
        pos.setPiece(TextIO.getSquare("g3"), Piece.WKING);
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        
        pos = TextIO.readFEN("8/2k5/3p4/4N3/8/2K3B1/8/8 w - - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));

        pos = TextIO.readFEN("8/2k5/8/4N3/8/6q1/2K5/8 w - - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        pos = TextIO.readFEN("8/2k5/8/4N3/8/8/2K5/8 w - - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "Nf7")));
        pos = TextIO.readFEN("2nk4/3P4/8/8/3R4/8/2K5/8 w - - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "dxc8N")));

        pos = TextIO.readFEN("8/2k5/2p5/1P1P4/8/2K5/8/8 w - - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "dxc6")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "d6")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "bxc6")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "b6")));
        
        pos = TextIO.readFEN("8/8/R1PkP2R/8/8/2K5/8/8 w - - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c7")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "e7")));

        // Test pawn promotion
        pos = TextIO.readFEN("8/1P6/2kP4/8/8/2K5/8/8 w - - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "d7")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "b8Q")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "b8N")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "b8R")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "b8B")));
        
        pos = TextIO.readFEN("8/2P1P3/2k5/8/8/2K5/8/8 w - - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "e8Q")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "e8N")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "e8R")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "e8B")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c8Q")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c8N")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c8R")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c8B")));

        // Test castling
        pos = TextIO.readFEN("8/8/8/8/5k2/8/8/R3K2R w KQ - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O-O")));
        pos = TextIO.readFEN("8/8/8/8/6k1/8/8/R3K2R w KQ - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));
        pos = TextIO.readFEN("8/8/8/8/3k4/8/8/R3K2R w KQ - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O-O")));
        pos = TextIO.readFEN("8/8/8/8/5k2/8/5P2/R3K2R w KQ - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));
        pos = TextIO.readFEN("8/8/8/8/8/8/8/R3K2k w Q - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O-O")));
        pos = TextIO.readFEN("8/8/8/8/8/8/8/2k1K2R w K - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));
        pos.setPiece(TextIO.getSquare("d1"), Piece.WKNIGHT);
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));

        // Test en passant
        pos = TextIO.readFEN("8/1kp5/8/3P4/8/8/8/4K3 b - - 0 1");
        pos.makeMove(TextIO.stringToMove(pos, "c5"), ui);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "dxc6")));

        pos = TextIO.readFEN("3k4/2p5/8/3P4/8/8/3R4/4K3 b - - 0 1");
        pos.makeMove(TextIO.stringToMove(pos, "c5"), ui);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "dxc6")));
        
        pos = TextIO.readFEN("5k2/2p5/8/3P4/8/B7/8/4K3 b - - 0 1");
        pos.makeMove(TextIO.stringToMove(pos, "c5"), ui);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "dxc6")));
        
        pos = TextIO.readFEN("5k2/2p5/8/3P4/1P6/B7/8/4K3 b - - 0 1");
        pos.makeMove(TextIO.stringToMove(pos, "c5"), ui);
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "dxc6")));

        pos = TextIO.readFEN("8/2p5/8/R2P1k2/8/8/8/4K3 b - - 0 1");
        pos.makeMove(TextIO.stringToMove(pos, "c5"), ui);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "dxc6")));

        // Black pawn moves
        pos = TextIO.readFEN("8/2p5/8/R4k2/1K6/8/8/8 b - - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c5")));
        pos = TextIO.readFEN("8/2p5/8/R4k2/2K5/8/8/8 b - - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c5")));
        pos = TextIO.readFEN("8/2p5/8/R4k2/3K4/8/8/8 b - - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "c5")));

        // Black castling
        pos = TextIO.readFEN("r3k2r/8/8/5K2/8/8/8/8 b kq - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O-O")));
        pos = TextIO.readFEN("r3k2r/8/8/6K1/8/8/8/8 b kq - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O")));
        pos = TextIO.readFEN("r3k2r/8/8/2K5/8/8/8/8 b kq - 0 1");
        assertTrue(!MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O-O")));
        pos = TextIO.readFEN("r3k2r/8/8/3K4/8/8/8/8 b kq - 0 1");
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "O-O-O")));

        // Black en passant
        pos = TextIO.readFEN("8/8/4k3/8/4p3/8/5PK1/8 w - - 0 1");
        pos.makeMove(TextIO.stringToMove(pos, "f4"), ui);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "exf3")));

        pos = TextIO.readFEN("8/8/4k3/8/K3p1r1/8/5P2/8 w - - 0 1");
        pos.makeMove(TextIO.stringToMove(pos, "f4"), ui);
        assertTrue(MoveGen.givesCheck(pos, TextIO.stringToMove(pos, "exf3")));
    }

    /**
     * Test of removeIllegal method, of class MoveGen.
     */
    @Test
    public void testRemoveIllegal() throws ChessParseError {
        System.out.println("removeIllegal");
        Position pos = TextIO.readFEN("8/3k4/8/2n1rpP1/1P6/1NB5/2QP4/R3K2R w KQ f6 0 1");
        List<String> strMoves = getMoveList(pos, true);
        assertTrue(strMoves.contains("c2e4"));
        assertTrue(strMoves.contains("c3e5"));
        assertTrue(strMoves.contains("e1d1"));
        assertTrue(strMoves.contains("e1f1"));
        assertTrue(strMoves.contains("e1f2"));
        assertEquals(5, strMoves.size());

        pos = TextIO.readFEN("4k3/8/8/2KPp1r1/8/8/8/8 w - e6 0 2");
        strMoves = getMoveList(pos, true);
        assertTrue(!strMoves.contains("d5e6"));
        assertEquals(7, strMoves.size());

        pos = TextIO.readFEN("8/6p1/4p3/2k1Pp1B/4KP1p/6rP/8/8 w - f6 0 55");
        strMoves = getMoveList(pos, true);
        assertTrue(strMoves.contains("e5f6"));
        assertEquals(1, strMoves.size());
    }
    
    /**
     * Test that if king capture is possible, only a king capture move is returned in the move list.
     */
    @Test
    public void testKingCapture() throws ChessParseError {
        System.out.println("kingCapture");
        Position pos = TextIO.readFEN("8/4k3/8/8/8/8/8/4RK2 b - - 0 1");
        pos.setWhiteMove(true);
        List<String> strMoves = getMoveList(pos, false);
        assertEquals(1, strMoves.size());
        assertEquals("e1e7", strMoves.get(0));

        pos.setPiece(Position.getSquare(0, 2), Piece.WBISHOP);
        pos.setPiece(Position.getSquare(4, 1), Piece.WPAWN);
        strMoves = getMoveList(pos, false);
        assertEquals(1, strMoves.size());
        assertEquals("a3e7", strMoves.get(0));

        pos.setPiece(Position.getSquare(1, 3), Piece.WPAWN);
        pos.setPiece(Position.getSquare(5, 5), Piece.WPAWN);
        strMoves = getMoveList(pos, false);
        assertEquals(1, strMoves.size());
        assertEquals("f6e7", strMoves.get(0));
    }

    /** Test that captureList and captureAndcheckList are generated correctly. */
    @Test
    public void testCaptureList() throws ChessParseError {
        System.out.println("captureList");
        Position pos = TextIO.readFEN("rnbqkbnr/ppp2ppp/3p1p2/R7/4N3/8/PPPPQPPP/2B1KB1R w Kkq - 0 1");
        getMoveList(pos, false);
        
        pos = TextIO.readFEN("rnb1kbn1/ppp1qppp/5p2/4p3/3N3r/3P4/PPP2PPP/R1BQKB1R b KQq - 0 1");
        getMoveList(pos, false);
        
        pos = TextIO.readFEN("rnb1k1n1/ppp1qppp/5p2/b3p3/1r1N4/3P4/PPP2PPP/R1BQKB1R b KQq - 0 1");
        getMoveList(pos, false);
        
        pos = TextIO.readFEN("8/8/8/8/3k4/8/4P3/4K3 w - - 0 1");
        getMoveList(pos, false);
        
        pos = TextIO.readFEN("8/8/8/3k4/8/8/4P3/4K3 w - - 0 1");
        getMoveList(pos, false);
        
        pos = TextIO.readFEN("8/8/8/3k4/4p3/8/3KP3/8 b - - 0 1");
        getMoveList(pos, false);

        pos = TextIO.readFEN("3k4/r2p2K1/8/8/8/8/8/8 b - - 0 1");
        getMoveList(pos, false);
    }

    @Test
    public void testCheckEvasions() throws ChessParseError {
        System.out.println("checkEvasions");
        Position pos = TextIO.readFEN("n7/8/8/7k/5pP1/5K2/8/8 b - g3 0 1");
        getMoveList(pos, false);

        pos = TextIO.readFEN("rn1qkbnr/pppB1ppp/3p4/4p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 0 1");
        getMoveList(pos, false);

        // King captures must be included in check evasions
        pos = TextIO.readFEN("r1bq2r1/pp3pbk/2p1p1P1/8/3P4/2PB1N2/PP3PPR/2KR4 b - - 0 1");
        UndoInfo ui = new UndoInfo();
        pos.makeMove(TextIO.uciStringToMove("g7h6"), ui);
        getMoveList(pos, false);
        List<String> evList = getCheckEvasions(pos, false);
        assertTrue(evList.contains("g6h7"));

        pos = TextIO.readFEN("1R6/1brk2p1/2P1p2p/p3Pp2/P7/6P1/1P4P1/2R3K1 b - - 0 1");
        getMoveList(pos, false);
        evList = getCheckEvasions(pos, false);
        assertTrue(evList.contains("b7c6"));
    }

    private List<String> getMoveList(Position pos, boolean onlyLegal) {
        Position swap = EvaluateTest.swapColors(pos);
        List<String> swapList = getMoveList0(swap, onlyLegal);
        List<String> ret = getMoveList0(pos, onlyLegal);
        assertEquals(swapList.size(), ret.size());
        return ret;
    }

    private List<String> getMoveList0(Position pos, boolean onlyLegal) {
        MoveGen moveGen = new MoveGen();
        MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
        if (onlyLegal)
            MoveGen.removeIllegal(pos, moves);
        ArrayList<String> strMoves = new ArrayList<String>();
        for (int mi = 0; mi < moves.size; mi++) {
            Move m = moves.m[mi];
            String mStr = TextIO.moveToUCIString(m);
            strMoves.add(mStr);
        }

        List<String> capList1 = getCaptureList(pos, false, onlyLegal);
        assertTrue(strMoves.containsAll(capList1));

        List<String> capList2 = getCaptureList(pos, true, onlyLegal);
        assertTrue(strMoves.containsAll(capList2));

        List<String> evList = getCheckEvasions(pos, onlyLegal);
        if (evList != null)
            assertTrue(strMoves.containsAll(evList));
        UndoInfo ui = new UndoInfo();
        for (String sm : strMoves) {
            Move m = TextIO.uciStringToMove(sm);
            if (m != null) {
                pos.makeMove(m, ui);
                boolean invalid = MoveGen.canTakeKing(pos);
                pos.unMakeMove(m, ui);
                if (invalid) m = null;
            }
            if (m == null) // Move was illegal (but pseudo-legal)
                continue;
            boolean qProm = false; // Promotion types considered in qsearch
            switch (m.promoteTo) {
            case Piece.WQUEEN: case Piece.BQUEEN:
            case Piece.WKNIGHT: case Piece.BKNIGHT:
            case Piece.EMPTY:
                qProm = true;
                break;
            default:
                break;
            }
            if (!MoveGen.canTakeKing(pos) && MoveGen.givesCheck(pos, m)) {
                if (qProm)
                    assertTrue(capList2.contains(sm));
            } else {
                switch (m.promoteTo) {
                case Piece.WQUEEN: case Piece.BQUEEN:
                case Piece.WKNIGHT: case Piece.BKNIGHT:
                    assertTrue(capList1.contains(sm)); // All queen/knight promotions
                    assertTrue(capList2.contains(sm)); // All queen/knight promotions
                    break;
                case Piece.EMPTY:
                    break;
                default:
                    assertTrue(!capList1.contains(sm)); // No rook/bishop promotions
                    assertTrue(!capList2.contains(sm)); // No rook/bishop promotions
                    break;
                }
            }
            if (pos.getPiece(m.to) != Piece.EMPTY) {
                if (qProm) {
                    assertTrue(capList1.contains(sm));
                    assertTrue(capList2.contains(sm));
                }
            }
            if (evList != null) {
                assertTrue(evList.contains(sm));
            }
        }

        return strMoves;
    }

    private List<String> getCaptureList(Position pos, boolean includeChecks, boolean onlyLegal) {
        MoveGen.MoveList moves;
        if (includeChecks) {
            moves = new MoveGen().pseudoLegalCapturesAndChecks(pos);
        } else {
            moves = new MoveGen().pseudoLegalCaptures(pos);
        }
        if (onlyLegal)
            MoveGen.removeIllegal(pos, moves);
        ArrayList<String> strMoves = new ArrayList<String>();
        for (int mi = 0; mi < moves.size; mi++) {
            Move m = moves.m[mi];
            String mStr = TextIO.moveToUCIString(m);
            strMoves.add(mStr);
        }
        return strMoves;
    }

    private List<String> getCheckEvasions(Position pos, boolean onlyLegal) {
        if (!MoveGen.inCheck(pos))
            return null;
        MoveGen.MoveList moves = new MoveGen().checkEvasions(pos);
        if (onlyLegal)
            MoveGen.removeIllegal(pos, moves);
        ArrayList<String> strMoves = new ArrayList<String>();
        for (int mi = 0; mi < moves.size; mi++) {
            Move m = moves.m[mi];
            String mStr = TextIO.moveToUCIString(m);
            strMoves.add(mStr);
        }
        return strMoves;
     }
}
