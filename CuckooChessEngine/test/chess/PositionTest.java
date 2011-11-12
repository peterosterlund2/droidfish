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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class PositionTest {

    public PositionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getPiece method, of class Position.
     */
    @Test
    public void testGetPiece() throws ChessParseError {
        System.out.println("getPiece");
        Position pos = new Position();
        int result = pos.getPiece(0);
        assertEquals(result, Piece.EMPTY);

        pos = TextIO.readFEN(TextIO.startPosFEN);
        result = pos.getPiece(0);
        assertEquals(result, Piece.WROOK);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 2; y++) {
                int p1 = pos.getPiece(Position.getSquare(x, y));
                int p2 = pos.getPiece(Position.getSquare(x, 7-y));
                int bwDiff = Piece.BPAWN - Piece.WPAWN;
                assertEquals(p2, p1 + bwDiff);
            }
        }
    }

    /**
     * Test of getIndex method, of class Position.
     */
    @Test
    public void testGetIndex() {
        System.out.println("getIndex");
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int sq = Position.getSquare(x, y);
                int x2 = Position.getX(sq);
                int y2 = Position.getY(sq);
                assertEquals(x, x2);
                assertEquals(y, y2);
            }
        }
    }

    /**
     * Test of setPiece method, of class Position.
     */
    @Test
    public void testSetPiece() {
        System.out.println("setPiece");
        Position instance = new Position();
        assertEquals(Piece.EMPTY, instance.getPiece(Position.getSquare(0, 0)));
        instance.setPiece(Position.getSquare(3, 4), Piece.WKING);
        assertEquals(Piece.WKING, instance.getPiece(Position.getSquare(3, 4)));
    }

    /**
     * Test of makeMove method, of class Position.
     */
    @Test
    public void testMakeMove() throws ChessParseError {
        System.out.println("makeMove");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        Position origPos = new Position(pos);
        assertTrue(pos.equals(origPos));
        Move move = new Move(Position.getSquare(4,1), Position.getSquare(4,3), Piece.EMPTY);
        UndoInfo ui = new UndoInfo();
        pos.makeMove(move, ui);
        assertEquals(pos.whiteMove, false);
        assertEquals(-1, pos.getEpSquare());
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(4,1)));
        assertEquals(Piece.WPAWN, pos.getPiece(Position.getSquare(4,3)));
        assertTrue(!pos.equals(origPos));
        int castleMask = (1 << Position.A1_CASTLE) |
                         (1 << Position.H1_CASTLE) |
                         (1 << Position.A8_CASTLE) |
                         (1 << Position.H8_CASTLE);
        assertEquals(castleMask,pos.getCastleMask());
        pos.unMakeMove(move, ui);
        assertEquals(pos.whiteMove, true);
        assertEquals(Piece.WPAWN, pos.getPiece(Position.getSquare(4,1)));
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(4,3)));
        assertTrue(pos.equals(origPos));

        String fen = "r1bqk2r/2ppbppp/p1n2n2/1pP1p3/B3P3/5N2/PP1P1PPP/RNBQK2R w KQkq b6 0 2";
        pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        origPos = new Position(pos);
        assertEquals(Position.getSquare(1,5), pos.getEpSquare());

        // Test capture
        move = new Move(Position.getSquare(0, 3), Position.getSquare(1,4), Piece.EMPTY);
        pos.makeMove(move, ui);
        assertEquals(-1, pos.getEpSquare());
        assertEquals(Piece.WBISHOP, pos.getPiece(Position.getSquare(1,4)));
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(0,3)));
        pos.unMakeMove(move, ui);
        assertTrue(pos.equals(origPos));
        
        // Test castling
        move = new Move(Position.getSquare(4, 0), Position.getSquare(6,0), Piece.EMPTY);
        pos.makeMove(move, ui);
        assertEquals(Piece.WROOK, pos.getPiece(Position.getSquare(5,0)));
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(7,0)));
        castleMask = (1 << Position.A8_CASTLE) |
                     (1 << Position.H8_CASTLE);
        assertEquals(castleMask,pos.getCastleMask());
        assertEquals(-1, pos.getEpSquare());
        pos.unMakeMove(move, ui);
        assertTrue(pos.equals(origPos));

        // Test castling rights (king move)
        move = new Move(Position.getSquare(4, 0), Position.getSquare(4,1), Piece.EMPTY);
        pos.makeMove(move, ui);
        castleMask = (1 << Position.A8_CASTLE) |
                     (1 << Position.H8_CASTLE);
        assertEquals(castleMask,pos.getCastleMask());
        assertEquals(-1, pos.getEpSquare());
        pos.unMakeMove(move, ui);
        assertTrue(pos.equals(origPos));

        // Test castling rights (rook move)
        move = new Move(Position.getSquare(7, 0), Position.getSquare(6,0), Piece.EMPTY);
        pos.makeMove(move, ui);
        castleMask = (1 << Position.A1_CASTLE) |
                     (1 << Position.A8_CASTLE) |
                     (1 << Position.H8_CASTLE);
        assertEquals(castleMask,pos.getCastleMask());
        assertEquals(-1, pos.getEpSquare());
        pos.unMakeMove(move, ui);
        assertTrue(pos.equals(origPos));
        
        // Test en passant
        move = new Move(Position.getSquare(2, 4), Position.getSquare(1,5), Piece.EMPTY);
        pos.makeMove(move, ui);
        assertEquals(Piece.WPAWN, pos.getPiece(Position.getSquare(1,5)));
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(2,4)));
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(1,4)));
        pos.unMakeMove(move, ui);
        assertTrue(pos.equals(origPos));
        
        // Test castling rights loss when rook captured
        pos.setPiece(Position.getSquare(6,2), Piece.BKNIGHT);
        pos.setWhiteMove(false);
        Position origPos2 = new Position(pos);
        move = new Move(Position.getSquare(6,2), Position.getSquare(7,0), Piece.EMPTY);
        pos.makeMove(move, ui);
        castleMask = (1 << Position.A1_CASTLE) |
                     (1 << Position.A8_CASTLE) |
                     (1 << Position.H8_CASTLE);
        assertEquals(castleMask,pos.getCastleMask());
        assertEquals(-1, pos.getEpSquare());
        pos.unMakeMove(move, ui);
        assertTrue(pos.equals(origPos2));
    }

    @Test
    public void testCastleMask() throws ChessParseError {
        System.out.println("castleMask");
        Position pos = TextIO.readFEN("rnbqk1nr/pppp1ppp/8/4p3/4P3/2N2N2/PPPP1bPP/R1BQKB1R w KQkq - 0 1");
        UndoInfo ui = new UndoInfo();
        Move m = TextIO.stringToMove(pos, "Kxf2");
        pos.makeMove(m, ui);
        int castleMask = (1 << Position.A8_CASTLE) |
                         (1 << Position.H8_CASTLE);
        assertEquals(castleMask, pos.getCastleMask());
    }

    /**
     * Test of makeMove method, of class Position.
     */
    @Test
    public void testPromotion() throws ChessParseError {
        System.out.println("promotion");
        String fen = "r1bqk2r/1Pppbppp/p1n2n2/2P1p3/B3P3/5N2/Pp1P1PPP/R1BQK2R w KQkq - 0 1";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        Position origPos = new Position(pos);
        assertEquals(origPos, pos);

        Move move = new Move(Position.getSquare(1, 6), Position.getSquare(0,7), Piece.WQUEEN);
        UndoInfo ui = new UndoInfo();
        pos.makeMove(move, ui);
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(1,6)));
        assertEquals(Piece.WQUEEN, pos.getPiece(Position.getSquare(0,7)));
        pos.unMakeMove(move, ui);
        assertEquals(origPos, pos);

        move = new Move(Position.getSquare(1, 6), Position.getSquare(1,7), Piece.WKNIGHT);
        ui = new UndoInfo();
        pos.makeMove(move, ui);
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(1,6)));
        assertEquals(Piece.WKNIGHT, pos.getPiece(Position.getSquare(1,7)));
        pos.unMakeMove(move, ui);
        assertEquals(origPos, pos);

        pos.setWhiteMove(false);
        origPos = new Position(pos);

        move = new Move(Position.getSquare(1, 1), Position.getSquare(2, 0), Piece.BROOK);
        ui = new UndoInfo();
        pos.makeMove(move, ui);
        assertEquals(Piece.EMPTY, pos.getPiece(Position.getSquare(1,1)));
        assertEquals(Piece.BROOK, pos.getPiece(Position.getSquare(2,0)));
        pos.unMakeMove(move, ui);
        assertEquals(origPos, pos);
    }
    
    /**
     * Test move counters, of class Position.
     */
    @Test
    public void testMoveCounters() throws ChessParseError {
        System.out.println("moveCounters");
        String fen = "r1bqk2r/2ppbppp/p1n2n2/1pP1p3/B3P3/5N2/PP1P1PPP/RNBQK2R w KQkq b6 0 7";
        Position pos = TextIO.readFEN(fen);
        
        Move move = TextIO.stringToMove(pos, "Nc3");
        UndoInfo ui = new UndoInfo();
        pos.makeMove(move, ui);
        assertEquals(1, pos.halfMoveClock);
        assertEquals(7, pos.fullMoveCounter);
        pos.unMakeMove(move, ui);
        
        move = TextIO.stringToMove(pos, "O-O");
        pos.makeMove(move, ui);
        assertEquals(1, pos.halfMoveClock);     // Castling does not reset 50 move counter
        assertEquals(7, pos.fullMoveCounter);
        pos.unMakeMove(move, ui);
        
        move = TextIO.stringToMove(pos, "a3");
        pos.makeMove(move, ui);
        assertEquals(0, pos.halfMoveClock);     // Pawn move resets 50 move counter
        assertEquals(7, pos.fullMoveCounter);
        pos.unMakeMove(move, ui);
        
        move = TextIO.stringToMove(pos, "Nxe5");
        pos.makeMove(move, ui);
        assertEquals(0, pos.halfMoveClock);     // Capture move resets 50 move counter
        assertEquals(7, pos.fullMoveCounter);
        pos.unMakeMove(move, ui);

        move = TextIO.stringToMove(pos, "cxb6");
        pos.makeMove(move, ui);
        assertEquals(0, pos.halfMoveClock);     // EP capture move resets 50 move counter
        assertEquals(7, pos.fullMoveCounter);
        pos.unMakeMove(move, ui);

        move = TextIO.stringToMove(pos, "Kf1");
        pos.makeMove(move, ui);
        assertEquals(1, pos.halfMoveClock);     // Loss of castling rights does not reset 50 move counter
        assertEquals(7, pos.fullMoveCounter);
        pos.unMakeMove(move, ui);
        
        Move firstMove = TextIO.stringToMove(pos, "Nc3");
        UndoInfo firstUi = new UndoInfo();
        pos.makeMove(move, firstUi);
        move = TextIO.stringToMove(pos, "O-O");
        pos.makeMove(move, ui);
        assertEquals(2, pos.halfMoveClock);
        assertEquals(8, pos.fullMoveCounter);   // Black move increases fullMoveCounter
        pos.unMakeMove(move, ui);
        pos.unMakeMove(firstMove, firstUi);
        
        fen = "8/8/8/4k3/8/8/2p5/5K2 b - - 47 68";
        pos = TextIO.readFEN(fen);
        move = TextIO.stringToMove(pos, "c1Q");
        pos.makeMove(move, ui);
        assertEquals(0, pos.halfMoveClock);     // Pawn promotion resets 50 move counter
        assertEquals(69, pos.fullMoveCounter);
    }
    
    /**
     * Test of drawRuleEquals, of class Position.
     */
    @Test
    public void testDrawRuleEquals() throws ChessParseError {
        System.out.println("drawRuleEquals");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        Position origPos = new Position(pos);
        UndoInfo ui = new UndoInfo();
        pos.makeMove(TextIO.stringToMove(pos, "Nf3"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));
        pos.makeMove(TextIO.stringToMove(pos, "Nf6"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));
        pos.makeMove(TextIO.stringToMove(pos, "Ng1"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));
        pos.makeMove(TextIO.stringToMove(pos, "Ng8"), ui);
        assertEquals(true, pos.drawRuleEquals(origPos));
        assertEquals(false, pos.equals(origPos));       // Move counters have changed
        
        String fen = "r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 1";
        pos = TextIO.readFEN(fen);
        origPos = new Position(pos);
        pos.makeMove(TextIO.stringToMove(pos, "Ke2"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));
        pos.makeMove(TextIO.stringToMove(pos, "Be7"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));
        pos.makeMove(TextIO.stringToMove(pos, "Ke1"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));
        pos.makeMove(TextIO.stringToMove(pos, "Bf8"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));   // Not equal, castling rights lost

        pos = TextIO.readFEN(TextIO.startPosFEN);
        pos.makeMove(TextIO.stringToMove(pos, "c4"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "a6"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "c5"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "b5"), ui);
        assertEquals(Position.getSquare(1, 5), pos.getEpSquare());
        origPos = new Position(pos);
        pos.makeMove(TextIO.stringToMove(pos, "Nc3"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nc6"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nb1"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nb8"), ui);
        assertEquals(false, pos.drawRuleEquals(origPos));   // Not equal, en passant rights lost
    }

    /**
     * Test of hashCode method, of class Position.
     */
    @Test
    public void testHashCode() throws ChessParseError {
        System.out.println("hashCode");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        long h1 = pos.zobristHash();
        assertEquals(h1, pos.computeZobristHash());
        UndoInfo ui = new UndoInfo();
        Move move = TextIO.stringToMove(pos, "e4");
        pos.makeMove(move, ui);
        assertTrue(h1 != pos.zobristHash());
        pos.unMakeMove(move, ui);
        assertTrue(h1 == pos.zobristHash());
        
        pos.setWhiteMove(!pos.whiteMove);
        long h4 = pos.zobristHash();
        assertEquals(h4, pos.computeZobristHash());
        assertTrue(h1 != pos.zobristHash());
        pos.setWhiteMove(!pos.whiteMove);
        assertTrue(h1 == pos.zobristHash());
        
        pos.setCastleMask(0);
        assertTrue(h1 != pos.zobristHash());

        pos = TextIO.readFEN("rnbqkbnr/pppp1ppp/8/2P1p3/8/8/PP1PPPPP/RNBQKBNR b KQkq - 0 1");
        h1 = pos.zobristHash();
        assertEquals(h1, pos.computeZobristHash());
        
        String[] moves = { 
            "b5", "Nc3", "Nf6", "Nb1", "Ng8", "Nc3", "Nf6", "Nb1", "Ng8", "Nc3", "d5",
            "cxd6", "Qxd6", "h4", "Be6", "h5", "Nc6", "h6", "o-o-o", "hxg7", "Nf6", "gxh8Q", "Be7"
        };
        List<UndoInfo> uiList = new ArrayList<UndoInfo>();
        List<Long> hashList = new ArrayList<Long>();
        List<Move> moveList = new ArrayList<Move>();
        for (int i = 0; i < moves.length; i++) {
            uiList.add(new UndoInfo());
            Move m = TextIO.stringToMove(pos, moves[i]);
            moveList.add(m);
            pos.makeMove(m, uiList.get(i));
            long h = pos.zobristHash();
            assertEquals(h, pos.computeZobristHash());
            hashList.add(h);
        }
        assertTrue(!hashList.get(0).equals(hashList.get(4)));
        assertTrue(hashList.get(4).equals(hashList.get(8)));
        for (int i = moves.length - 1; i >= 0; i--) {
            pos.unMakeMove(moveList.get(i), uiList.get(i));
            long h = pos.zobristHash();
            assertEquals(h, pos.computeZobristHash());
            assertEquals(h, i > 0 ? hashList.get(i - 1) : h1);
        }
    }

    /**
     * Test of getKingSq method, of class Position.
     */
    @Test
    public void testGetKingSq() throws ChessParseError {
        System.out.println("getKingSq");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        assertEquals(TextIO.getSquare("e1"), pos.getKingSq(true));
        assertEquals(TextIO.getSquare("e8"), pos.getKingSq(false));
        pos = TextIO.readFEN("r1bq1bnr/ppppkppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQ - 0 4");
        assertEquals(TextIO.getSquare("e1"), pos.getKingSq(true));
        assertEquals(TextIO.getSquare("e7"), pos.getKingSq(false));
        UndoInfo ui = new UndoInfo();
        pos.makeMove(TextIO.stringToMove(pos, "o-o"), ui);
        assertEquals(TextIO.getSquare("g1"), pos.getKingSq(true));
        assertEquals(TextIO.getSquare("e7"), pos.getKingSq(false));
        pos.makeMove(TextIO.stringToMove(pos, "Kd6"), ui);
        assertEquals(TextIO.getSquare("g1"), pos.getKingSq(true));
        assertEquals(TextIO.getSquare("d6"), pos.getKingSq(false));
    }
}
