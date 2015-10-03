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


import junit.framework.TestCase;

/**
 *
 * @author petero
 */
public class TextIOTest extends TestCase {

    public TextIOTest() {
    }

    /**
     * Test of readFEN method, of class TextIO.
     */
    public void testReadFEN() throws ChessParseError {
        String fen = "rnbqk2r/1p3ppp/p7/1NpPp3/QPP1P1n1/P4N2/4KbPP/R1B2B1R b kq - 0 1";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        assertEquals(pos.getPiece(Position.getSquare(0, 3)), Piece.WQUEEN);
        assertEquals(pos.getPiece(Position.getSquare(4, 7)), Piece.BKING);
        assertEquals(pos.getPiece(Position.getSquare(4, 1)), Piece.WKING);
        assertEquals(pos.whiteMove, false);
        assertEquals(pos.a1Castle(), false);
        assertEquals(pos.h1Castle(), false);
        assertEquals(pos.a8Castle(), true);
        assertEquals(pos.h8Castle(), true);

        fen = "8/3k4/8/5pP1/1P6/1NB5/2QP4/R3K2R w KQ f6 1 2";
        pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        assertEquals(1, pos.halfMoveClock);
        assertEquals(2, pos.fullMoveCounter);

        // Must have exactly one king
        boolean wasError = testFENParseError("8/8/8/8/8/8/8/kk1K4 w - - 0 1");
        assertEquals(true, wasError);

        // Must not be possible to capture the king
        wasError = testFENParseError("8/8/8/8/8/8/8/k1RK4 w - - 0 1");
        assertEquals(true, wasError);

        // Make sure bogus en passant square information is removed
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        pos = TextIO.readFEN(fen);
        assertEquals(-1, pos.getEpSquare());

        // Test for too many rows (slashes)
        wasError = testFENParseError("8/8/8/8/4k3/8/8/8/KBN5 w - - 0 1");
        assertEquals(true, wasError);

        // Test for too many columns
        wasError = testFENParseError("8K/8/8/8/4k3/8/8/8 w - - 0 1");
        assertEquals(true, wasError);

        // Pawns must not be on first/last rank
        wasError = testFENParseError("kp6/8/8/8/8/8/8/K7 w - - 0 1");
        assertEquals(true, wasError);

        wasError = testFENParseError("kr/pppp/8/8/8/8/8/KBR w");
        assertEquals(false, wasError);  // OK not to specify castling flags and ep square

        wasError = testFENParseError("k/8/8/8/8/8/8/K");
        assertEquals(true, wasError);   // Error side to move not specified

        wasError = testFENParseError("");
        assertEquals(true, wasError);

        wasError = testFENParseError("    |");
        assertEquals(true, wasError);

        wasError = testFENParseError("1B1B4/6k1/7r/7P/6q1/r7/q7/7K b - - acn 6; acs 0;");
        assertEquals(false, wasError);  // Extra stuff after FEN string is allowed

        pos = TextIO.readFEN("3r2k1/p4p1p/1ppq2p1/8/2PpQ3/1P5P/P4PP1/3R1K2 b - - 1 26\n");
        assertEquals(26, pos.fullMoveCounter);

        // Test that insane move numbers are rejected. Otherwise, could cause problems
        // (excessive memory usage) for the clock history class.
        pos = TextIO.readFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 10000");
        assertEquals(10000, pos.fullMoveCounter);

        // Test invalid en passant square detection
        pos = TextIO.readFEN("rnbqkbnr/pp1ppppp/8/8/2pPP3/8/PPP2PPP/RNBQKBNR b KQkq d3 0 1");
        assertEquals(TextIO.getSquare("d3"), pos.getEpSquare());

        pos = TextIO.readFEN("rnbqkbnr/pp1ppppp/8/8/2pPP3/8/PPP2PPP/RNBQKBNR w KQkq d3 0 1");
        assertTrue(pos.equals(TextIO.readFEN("rnbqkbnr/pp1ppppp/8/8/2pPP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 1")));

        pos = TextIO.readFEN("rnbqkbnr/ppp2ppp/8/2Ppp3/8/8/PP1PPPPP/RNBQKBNR w KQkq d6 0 1");
        assertEquals(TextIO.getSquare("d6"), pos.getEpSquare());

        pos = TextIO.readFEN("rnbqkbnr/ppp2ppp/8/2Ppp3/8/8/PP1PPPPP/RNBQKBNR b KQkq d6 0 1");
        assertTrue(pos.equals(TextIO.readFEN("rnbqkbnr/ppp2ppp/8/2Ppp3/8/8/PP1PPPPP/RNBQKBNR b KQkq - 0 1")));

        pos = TextIO.readFEN("rnbqkbnr/pppppppp/8/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq d3 0 1");
        assertEquals(-1, pos.getEpSquare());

        pos = TextIO.readFEN("rnbqkbnr/ppp2ppp/8/3pp3/8/8/PPPPPPPP/RNBQKBNR w KQkq e6 0 1");
        assertEquals(-1, pos.getEpSquare());

        pos = TextIO.readFEN("rnbqkbnr/pp1ppppp/8/8/2pPP3/3P4/PP3PPP/RNBQKBNR b KQkq d3 0 1");
        assertTrue(pos.equals(TextIO.readFEN("rnbqkbnr/pp1ppppp/8/8/2pPP3/3P4/PP3PPP/RNBQKBNR b KQkq - 0 1")));
    }

    /**
     * Test that readFEN removes bogus castle flags.
     */
    public void testReadFENCastleFlags() throws ChessParseError {
        String fenBogus = "rnbqk2r/1p3ppp/p7/1NpPp3/QPP1P1n1/P4N2/4KbPP/R1B2B1R w KQkq - 0 1";
        Position pos = TextIO.readFEN(fenBogus);
        String fenCorrect = "rnbqk2r/1p3ppp/p7/1NpPp3/QPP1P1n1/P4N2/4KbPP/R1B2B1R w kq - 0 1";
        assertEquals(fenCorrect, TextIO.toFEN(pos));
    }

    /** Tests if trying to parse a FEN string causes an error. */
    private boolean testFENParseError(String fen) {
        boolean wasError;
        wasError = false;
        try {
            TextIO.readFEN(fen);
        } catch (ChessParseError err) {
            wasError = true;
        }
        return wasError;
    }

    private final static String moveToString(Position pos, Move move, boolean longForm) {
        return TextIO.moveToString(pos, move, longForm, false);
    }

    /**
     * Test of moveToString method, of class TextIO.
     */
    public void testMoveToString() throws ChessParseError {
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(pos));
        Move move = new Move(Position.getSquare(4, 1), Position.getSquare(4, 3),
                Piece.EMPTY);
        boolean longForm = true;
        String result = moveToString(pos, move, longForm);
        assertEquals("e2-e4", result);

        move = new Move(Position.getSquare(6, 0), Position.getSquare(5, 2), Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("Ng1-f3", result);

        move = new Move(Position.getSquare(4, 7), Position.getSquare(2, 7),
                Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("O-O-O", result);

        String fen = "1r3k2/2P5/8/8/8/4K3/8/8 w - - 0 1";
        pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        move = new Move(Position.getSquare(2,6), Position.getSquare(1,7), Piece.WROOK);
        result = moveToString(pos, move, longForm);
        assertEquals("c7xb8R+", result);

        move = new Move(Position.getSquare(2,6), Position.getSquare(2,7), Piece.WKNIGHT);
        result = moveToString(pos, move, longForm);
        assertEquals("c7-c8N", result);

        move = new Move(Position.getSquare(2,6), Position.getSquare(2,7), Piece.WQUEEN);
        result = moveToString(pos, move, longForm);
        assertEquals("c7-c8Q+", result);

        // Test null move
        pos = TextIO.readFEN(TextIO.startPosFEN);
        Move nullMove = new Move(0, 0, 0);
        result = moveToString(pos, nullMove, false);
        assertEquals("--", result);
        result = moveToString(pos, nullMove, true);
        assertEquals("--", result);
    }

    /**
     * Test of moveToString method, of class TextIO, mate/stalemate tests.
     */
    public void testMoveToStringMate() throws ChessParseError {
        Position pos = TextIO.readFEN("3k4/1PR5/3N4/8/4K3/8/8/8 w - - 0 1");
        boolean longForm = true;

        Move move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WROOK);
        String result = moveToString(pos, move, longForm);
        assertEquals("b7-b8R+", result);    // check

        move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WQUEEN);
        result = moveToString(pos, move, longForm);
        assertEquals("b7-b8Q#", result);    // check mate

        move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WKNIGHT);
        result = moveToString(pos, move, longForm);
        assertEquals("b7-b8N", result);

        move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WBISHOP);
        result = moveToString(pos, move, longForm);
        assertEquals("b7-b8B", result);     // stalemate
    }

    /**
     * Test of moveToString method, of class TextIO, short form.
     */
    public void testMoveToStringShortForm() throws ChessParseError {
        String fen = "r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        boolean longForm = false;

        Move move = new Move(Position.getSquare(4,5), Position.getSquare(4,3), Piece.EMPTY);
        String result = moveToString(pos, move, longForm);
        assertEquals("Qee4", result);   // File disambiguation needed

        move = new Move(Position.getSquare(2,5), Position.getSquare(4,3), Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("Qc6e4", result);  // Full disambiguation needed

        move = new Move(Position.getSquare(2,3), Position.getSquare(4,3), Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("Q4e4", result);   // Row disambiguation needed

        move = new Move(Position.getSquare(2,3), Position.getSquare(2,0), Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("Qc1+", result);   // No disambiguation needed

        move = new Move(Position.getSquare(0,1), Position.getSquare(0,0), Piece.BQUEEN);
        result = moveToString(pos, move, longForm);
        assertEquals("a1Q", result);    // Normal promotion

        move = new Move(Position.getSquare(0,1), Position.getSquare(1,0), Piece.BQUEEN);
        result = moveToString(pos, move, longForm);
        assertEquals("axb1Q#", result); // Capture promotion and check mate

        move = new Move(Position.getSquare(0,1), Position.getSquare(1,0), Piece.BKNIGHT);
        result = moveToString(pos, move, longForm);
        assertEquals("axb1N", result);  // Capture promotion

        move = new Move(Position.getSquare(3,6), Position.getSquare(4,4), Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("Ne5", result);    // Other knight pinned, no disambiguation needed

        move = new Move(Position.getSquare(7,6), Position.getSquare(7,4), Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("h5", result);     // Regular pawn move

        move = new Move(Position.getSquare(5,7), Position.getSquare(3,7), Piece.EMPTY);
        result = moveToString(pos, move, longForm);
        assertEquals("Rfd8", result);     // File disambiguation needed
    }

    /**
     * Test of stringToMove method, of class TextIO.
     */
    public void testStringToMove() throws ChessParseError {
        Position pos = TextIO.readFEN("r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1");

        Move mNe5 = new Move(Position.getSquare(3, 6), Position.getSquare(4, 4), Piece.EMPTY);
        Move m = TextIO.stringToMove(pos, "Ne5");
        assertEquals(mNe5, m);
        m = TextIO.stringToMove(pos, "ne");
        assertEquals(mNe5, m);
        m = TextIO.stringToMove(pos, "N");
        assertEquals(null, m);

        Move mQc6e4 = new Move(Position.getSquare(2, 5), Position.getSquare(4, 3), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "Qc6-e4");
        assertEquals(mQc6e4, m);
        m = TextIO.stringToMove(pos, "Qc6e4");
        assertEquals(mQc6e4, m);
        m = TextIO.stringToMove(pos, "Qce4");
        assertEquals(null, m);
        m = TextIO.stringToMove(pos, "Q6e4");
        assertEquals(null, m);

        Move maxb1Q = new Move(Position.getSquare(0, 1), Position.getSquare(1, 0), Piece.BQUEEN);
        m = TextIO.stringToMove(pos, "axb1Q");
        assertEquals(maxb1Q, m);
        m = TextIO.stringToMove(pos, "axb1Q#");
        assertEquals(maxb1Q, m);
        m = TextIO.stringToMove(pos, "axb1Q+");
        assertEquals(maxb1Q, m);

        Move mh5= new Move(Position.getSquare(7, 6), Position.getSquare(7, 4), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "h5");
        assertEquals(mh5, m);
        m = TextIO.stringToMove(pos, "h7-h5");
        assertEquals(mh5, m);
        m = TextIO.stringToMove(pos, "h");
        assertEquals(null, m);

        pos = TextIO.readFEN("r1b1k2r/1pqpppbp/p5pn/3BP3/8/2pP4/PPPBQPPP/R3K2R w KQkq - 0 12");
        m = TextIO.stringToMove(pos, "bxc3");
        assertEquals(TextIO.getSquare("b2"), m.from);
        m = TextIO.stringToMove(pos, "Bxc3");
        assertEquals(TextIO.getSquare("d2"), m.from);
        m = TextIO.stringToMove(pos, "bxc");
        assertEquals(TextIO.getSquare("b2"), m.from);
        m = TextIO.stringToMove(pos, "Bxc");
        assertEquals(TextIO.getSquare("d2"), m.from);

        // Test castling. o-o is a substring of o-o-o, which could cause problems.
        pos = TextIO.readFEN("5k2/p1pQn3/1p2Bp1r/8/4P1pN/2N5/PPP2PPP/R3K2R w KQ - 0 16");
        Move kCastle = new Move(Position.getSquare(4,0), Position.getSquare(6,0), Piece.EMPTY);
        Move qCastle = new Move(Position.getSquare(4,0), Position.getSquare(2,0), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "o");
        assertEquals(null, m);
        m = TextIO.stringToMove(pos, "o-o");
        assertEquals(kCastle, m);
        m = TextIO.stringToMove(pos, "O-O");
        assertEquals(kCastle, m);
        m = TextIO.stringToMove(pos, "0-0");
        assertEquals(kCastle, m);
        m = TextIO.stringToMove(pos, "O-O-O");
        assertEquals(qCastle, m);
        m = TextIO.stringToMove(pos, "o-o-o");
        assertEquals(qCastle, m);
        m = TextIO.stringToMove(pos, "0-0-0");
        assertEquals(qCastle, m);

        // Test 'o-o+'
        pos.setPiece(Position.getSquare(5,1), Piece.EMPTY);
        pos.setPiece(Position.getSquare(5,5), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "o");
        assertEquals(null, m);
        m = TextIO.stringToMove(pos, "o-o");
        assertEquals(kCastle, m);
        m = TextIO.stringToMove(pos, "o-o-o");
        assertEquals(qCastle, m);
        m = TextIO.stringToMove(pos, "o-o+");
        assertEquals(kCastle, m);

        // Test d8=Q+ syntax
        pos = TextIO.readFEN("1r3r2/2kP2Rp/p1bN1p2/2p5/5P2/2P5/P5PP/3R2K1 w - -");
        m = TextIO.stringToMove(pos, "d8=Q+");
        Move m2 = TextIO.stringToMove(pos, "d8Q");
        assertEquals(m2, m);

        // Test null move
        pos = TextIO.readFEN(TextIO.startPosFEN);
        Move nullMove = new Move(0, 0, 0);
        m = TextIO.stringToMove(pos, "--");
        assertEquals(nullMove, m);

        // Test extra characters
        pos = TextIO.readFEN(TextIO.startPosFEN);
        Move mNf3 = new Move(TextIO.getSquare("g1"), TextIO.getSquare("f3"), Piece.EMPTY);
        assertEquals(mNf3, TextIO.stringToMove(pos, "Ngf3"));
        assertEquals(mNf3, TextIO.stringToMove(pos, "Ng1f3"));
        assertEquals(mNf3, TextIO.stringToMove(pos, "Ng1-f3"));
        assertEquals(mNf3, TextIO.stringToMove(pos, "g1f3"));
        assertEquals(mNf3, TextIO.stringToMove(pos, "N1f3"));
        assertEquals(mNf3, TextIO.stringToMove(pos, "Ngf"));
        assertEquals(mNf3, TextIO.stringToMove(pos, "Nf"));
    }

    /**
     * Test of getSquare method, of class TextIO.
     */
    public void testGetSquare() throws ChessParseError {
        assertEquals(Position.getSquare(0, 0), TextIO.getSquare("a1"));
        assertEquals(Position.getSquare(1, 7), TextIO.getSquare("b8"));
        assertEquals(Position.getSquare(3, 3), TextIO.getSquare("d4"));
        assertEquals(Position.getSquare(4, 3), TextIO.getSquare("e4"));
        assertEquals(Position.getSquare(3, 1), TextIO.getSquare("d2"));
        assertEquals(Position.getSquare(7, 7), TextIO.getSquare("h8"));
    }

    /**
     * Test of squareToString method, of class TextIO.
     */
    public void testSquareToString() {
        assertEquals("a1", TextIO.squareToString(Position.getSquare(0, 0)));
        assertEquals("h6", TextIO.squareToString(Position.getSquare(7, 5)));
        assertEquals("e4", TextIO.squareToString(Position.getSquare(4, 3)));
    }

    /**
     * Test of asciiBoard method, of class TextIO.
     */
    public void testAsciiBoard() throws ChessParseError {
        Position pos = TextIO.readFEN("r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1");
        String aBrd = TextIO.asciiBoard(pos);
//        System.out.print(aBrd);
        assertEquals(12, aBrd.length() - aBrd.replaceAll("\\*", "").length()); // 12 black pieces
        assertEquals(3, aBrd.length() - aBrd.replaceAll("\\*Q", " ").length()); // 3 black queens
        assertEquals(3, aBrd.length() - aBrd.replaceAll(" P", " ").length()); // 3 white pawns
    }
}
