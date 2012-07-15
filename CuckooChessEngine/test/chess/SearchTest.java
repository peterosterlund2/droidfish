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

import chess.Search.StopSearch;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class SearchTest {
    static final long[] nullHist = new long[200];
    static TranspositionTable tt = new TranspositionTable(19);
    static History ht = new History();
    
    public SearchTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of negaScout method, of class Search.
     */
    @Test
    public void testNegaScout() throws ChessParseError, StopSearch {
        System.out.println("negaScout");
        final int mate0 = Search.MATE0;

        Position pos = TextIO.readFEN("3k4/8/3K2R1/8/8/8/8/8 w - - 0 1");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        final int plyScale = Search.plyScale;
        int score = sc.negaScout(-mate0, mate0, 0, 2*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(mate0 - 2, score);     // depth 2 is enough to find mate in 1
        int score2 = idSearch(sc, 2).score;
        assertEquals(score, score2);
        
        pos = TextIO.readFEN("8/1P6/k7/2K5/8/8/8/8 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        score = sc.negaScout(-mate0, mate0, 0, 4*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(mate0 - 4, score);     // depth 4 is enough to find mate in 2
        score2 = idSearch(sc, 4).score;
        assertEquals(score, score2);
        
        pos = TextIO.readFEN("8/5P1k/5K2/8/8/8/8/8 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        score = sc.negaScout(-mate0, mate0, 0, 5*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(mate0 - 4, score);     // must avoid stale-mate after f8Q
        score2 = idSearch(sc, 5).score;
        assertEquals(score, score2);

        pos = TextIO.readFEN("4k3/8/3K1Q2/8/8/8/8/8 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        score = sc.negaScout(-mate0, mate0, 0, 2*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(0, score);             // Position is stale-mate

        pos = TextIO.readFEN("3kB3/8/1N1K4/8/8/8/8/8 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        score = sc.negaScout(-mate0, mate0, 0, 3*plyScale, -1, MoveGen.inCheck(pos));
        assertTrue(Math.abs(score) < 50);   // Stale-mate trap
        score2 = idSearch(sc, 5).score;
        assertEquals(score, score2);

        pos = TextIO.readFEN("8/8/2K5/3QP3/P6P/1q6/8/k7 w - - 31 51");
        sc = new Search(pos, nullHist, 0, tt, ht);
        Move bestM = idSearch(sc, 2);
        assertTrue(!TextIO.moveToString(pos, bestM, false).equals("Qxb3"));
    }

    /**
     * Test of draw by 50 move rule, of class Search.
     */
    @Test
    public void testDraw50() throws ChessParseError, StopSearch {
        System.out.println("draw50");
        final int mate0 = Search.MATE0;
        final int mateInOne = mate0 - 2;
        final int matedInOne = -mate0 + 3;
        final int mateInTwo = mate0 - 4;
        final int mateInThree = mate0 - 6;

        Position pos = TextIO.readFEN("8/1R2k3/R7/8/8/8/8/1K6 b - - 0 1");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        final int plyScale = Search.plyScale;
        int score = sc.negaScout(-mate0, mate0, 0, 2*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(matedInOne, score);
        
        pos = TextIO.readFEN("8/1R2k3/R7/8/8/8/8/1K6 b - - 99 80");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = sc.negaScout(-mate0, mate0, 0, 2*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(0, score);     // Draw by 50-move rule

        pos = TextIO.readFEN("8/1R2k3/R7/8/8/8/8/1K6 b - - 98 80");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = sc.negaScout(-mate0, mate0, 0, 2*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(matedInOne, score);     // No draw
        
        pos = TextIO.readFEN("8/1R2k3/R7/8/8/8/8/1K6 b - - 99 80");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 3).score;
        assertEquals(0, score);

        pos = TextIO.readFEN("3k4/1R6/R7/8/8/8/8/1K6 w - - 100 80");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 2).score;
        assertEquals(mateInOne, score); // Black forgot to claim draw. Now it's too late.
        
        pos = TextIO.readFEN("8/7k/1R6/R7/8/7P/8/1K6 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 3).score;
        assertEquals(mateInTwo, score);
        
        pos = TextIO.readFEN("8/7k/1R6/R7/8/7P/8/1K6 w - - 98 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 6).score;
        assertEquals(mateInThree, score);   // Need an extra pawn move to avoid 50-move rule
        
        pos = TextIO.readFEN("8/7k/1R6/R7/8/7P/8/1K6 w - - 125 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 6).score;
        assertEquals(mateInThree, score);   // Need an extra pawn move to avoid 50-move rule
    }

    /**
     * Test of draw by repetition rule, of class Search.
     */
    @Test
    public void testDrawRep() throws ChessParseError, StopSearch {
        System.out.println("drawRep");
        final int mate0 = Search.MATE0;
        Position pos = TextIO.readFEN("7k/5RR1/8/8/8/8/q3q3/2K5 w - - 0 1");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        final int plyScale = Search.plyScale;
        int score = sc.negaScout(-mate0, mate0, 0, 3*plyScale, -1, MoveGen.inCheck(pos));
        assertEquals(0, score);
        
        pos = TextIO.readFEN("7k/5RR1/8/8/8/8/q3q3/2K5 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 3).score;
        assertEquals(0, score);

        pos = TextIO.readFEN("7k/5RR1/8/8/8/8/1q3q2/3K4 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 4).score;
        assertTrue(score < 0);

        pos = TextIO.readFEN("7k/5RR1/8/8/8/8/1q3q2/3K4 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = sc.negaScout(-mate0, mate0, 0, 3*plyScale, -1, MoveGen.inCheck(pos));
        assertTrue(score < 0);
        
        pos = TextIO.readFEN("qn6/qn4k1/pp3R2/5R2/8/8/8/K7 w - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        sc.maxTimeMillis = -1;
        score = idSearch(sc, 7).score;
        assertEquals(0, score); // Draw, black can not escape from perpetual checks
    }

    /**
     * Test of hash table, of class Search.
     */
    @Test
    public void testHashing() throws ChessParseError {
        System.out.println("hashing");
        Position pos = TextIO.readFEN("/k/3p/p2P1p/P2P1P///K/ w - -");  // Fine #70
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        Move bestM = idSearch(sc, 28);
        assertEquals(TextIO.stringToMove(pos, "Kb1"), new Move(bestM));
    }

    /**
     * Late move pruning must not be used in mate search.
     */
    @Test
    public void testLMP() throws ChessParseError {
        Position pos = TextIO.readFEN("2r2rk1/6p1/p3pq1p/1p1b1p2/3P1n2/PP3N2/3N1PPP/1Q2RR1K b");  // WAC 174
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        Move bestM = idSearch(sc, 2);
        assertTrue(bestM.score < Search.MATE0 / 2);
    }

    @Test
    public void testCheckEvasion() throws ChessParseError {
        System.out.println("check evasion");
        Position pos = TextIO.readFEN("6r1/R5PK/2p5/1k6/8/8/p7/8 b - - 0 62");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        Move bestM = idSearch(sc, 3);
        assertTrue(bestM.score < 0);

        pos = TextIO.readFEN("r1bq2rk/pp3pbp/2p1p1pQ/7P/3P4/2PB1N2/PP3PPR/2KR4 w - -"); // WAC 004
        sc = new Search(pos, nullHist, 0, tt, ht);
        bestM = idSearch(sc, 1);
        assertEquals(Search.MATE0 - 4, bestM.score);
        assertEquals(TextIO.stringToMove(pos, "Qxh7+"), new Move(bestM));
    }

    @Test
    public void testStalemateTrap() throws ChessParseError {
        System.out.println("stalemate trap");
        Position pos = TextIO.readFEN("7k/1P3R1P/6r1/5K2/8/8/6R1/8 b - - 98 194");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        Move bestM = idSearch(sc, 3);
        assertEquals(0, bestM.score);
    }

    @Test
    public void testKQKRNullMove() throws ChessParseError {
        System.out.println("kqkrNullMove");
        Position pos = TextIO.readFEN("7K/6R1/5k2/3q4/8/8/8/8 b - - 0 1");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        Move bestM = idSearch(sc, 10);
        assertEquals(Search.MATE0-18, bestM.score);
    }

    private Move idSearch(Search sc, int maxDepth) {
        MoveGen.MoveList moves = new MoveGen().pseudoLegalMoves(sc.pos);
        MoveGen.removeIllegal(sc.pos, moves);
        sc.scoreMoveList(moves, 0);
        sc.timeLimit(-1, -1);
        Move bestM = sc.iterativeDeepening(moves, maxDepth, -1, false);
        return bestM;
    }

    /** Compute SEE(m) and assure that signSEE and negSEE give matching results. */
    private int getSEE(Search sc, Move m) {
        int see = sc.SEE(m);
        boolean neg = sc.negSEE(m);
        assertEquals(neg, see < 0);
        int sign = sc.signSEE(m);
        if (sign > 0)
            assertTrue(see > 0);
        else if (sign == 0)
            assertEquals(0, see);
        else
            assertTrue(see < 0);
        return see;
    }

    /**
     * Test of SEE method, of class Search.
     */
    @Test
    public void testSEE() throws ChessParseError {
        System.out.println("SEE");
        final int pV = Evaluate.pV;
        final int nV = Evaluate.nV;
        final int bV = Evaluate.bV;
        final int rV = Evaluate.rV;

        // Basic tests
        Position pos = TextIO.readFEN("r2qk2r/ppp2ppp/1bnp1nb1/1N2p3/3PP3/1PP2N2/1P3PPP/R1BQRBK1 w kq - 0 1");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "dxe5")));
        assertEquals(pV - nV, getSEE(sc, TextIO.stringToMove(pos, "Nxe5")));
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxa7")));
        assertEquals(pV - nV, getSEE(sc, TextIO.stringToMove(pos, "Nxa7")));
        assertEquals(pV - nV, getSEE(sc, TextIO.stringToMove(pos, "Nxd6")));
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "d5")));
        assertEquals(-bV, getSEE(sc, TextIO.stringToMove(pos, "Bf4")));
        assertEquals(-bV, getSEE(sc, TextIO.stringToMove(pos, "Bh6")));
        assertEquals(-rV, getSEE(sc, TextIO.stringToMove(pos, "Ra5")));
        assertEquals(-rV, getSEE(sc, TextIO.stringToMove(pos, "Ra6")));        

        pos.setWhiteMove(false);
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertTrue(nV <= bV);   // Assumed by following test
        assertEquals(pV - nV, getSEE(sc, TextIO.stringToMove(pos, "Nxd4")));
        assertEquals(pV - bV, getSEE(sc, TextIO.stringToMove(pos, "Bxd4")));
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "exd4")));
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "Nxe4")));
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "Bxe4")));
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "d5")));
        assertEquals(-nV, getSEE(sc, TextIO.stringToMove(pos, "Nd5")));
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "a6")));

        // Test X-ray attacks
        pos = TextIO.readFEN("3r2k1/pp1q1ppp/1bnr1nb1/1Np1p3/1P1PP3/2P1BN2/1Q1R1PPP/3R1BK1 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 1 1 1 3 3 3 3 3 5 5 9 5 5
        // 0 1 0 1 0 3 0 3 0 5 0 9 0 5
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "exd4")));
        // 1 3 1 1 3 1 3 3 5 5 5 9 9
        //-1 2 1 0 3 0 3 0 5 0 5 0 9
        assertEquals(2 * pV - nV, getSEE(sc, TextIO.stringToMove(pos, "Nxd4")));

        pos.setPiece(TextIO.getSquare("b2"), Piece.EMPTY);  // Remove white queen
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 1 1 1 3 3 3 3 3 5 5 9 5
        // 0 1 0 1 0 3 0 3 0 4 1 4 5
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "exd4")));
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "cxb4")));
        
        pos.setPiece(TextIO.getSquare("b5"), Piece.EMPTY);  // Remove white knight
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 1 1 1 3 3 3 3 5 5 5
        // 1 0 1 0 3 0 3 0 5 0 5
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "exd4")));
        
        pos.setPiece(TextIO.getSquare("b2"), Piece.WQUEEN);  // Restore white queen
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 1 1 1 3 3 3 3 5 5 5 9 9
        // 1 0 1 0 3 0 3 0 5 0 5 0 9
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "exd4")));

        pos.setPiece(TextIO.getSquare("b6"), Piece.EMPTY);  // Remove black bishop
        pos.setPiece(TextIO.getSquare("c6"), Piece.EMPTY);  // Remove black knight
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(-pV, getSEE(sc, TextIO.stringToMove(pos, "a5")));
        
        // Test EP capture
        pos = TextIO.readFEN("2b3k1/1p3ppp/8/pP6/8/2PB4/5PPP/6K1 w - a6 0 2");
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 1 1 3
        // 0 1 0 3
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "bxa6")));
        
        pos.setPiece(TextIO.getSquare("b7"), Piece.EMPTY);  // Remove black pawn
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 1 3
        // 1 0 3
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "bxa6")));

        
        // Test king capture
        pos = TextIO.readFEN("8/8/8/4k3/r3P3/4K3/8/4R3 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 5 99
        // 1 0 99
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "Rxe4+")));
        
        pos = TextIO.readFEN("8/8/8/4k3/r3P1R1/4K3/8/8 b - - 0 1");
        final int kV = Evaluate.kV;
        sc = new Search(pos, nullHist, 0, tt, ht);
        // 1 5 5 99
        //-4 5 0 99
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxe4+")));
        //  1 99
        //-98 99
        assertEquals(pV - kV, getSEE(sc, new Move(TextIO.getSquare("e5"), TextIO.getSquare("e4"), Piece.EMPTY)));
        
        pos = TextIO.readFEN("8/8/4k3/8/r3P3/4K3/8/8 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxe4+")));

        // Test king too far away
        pos = TextIO.readFEN("8/8/4k3/8/r3P3/8/4K3/8 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "Rxe4+")));
        
        pos = TextIO.readFEN("8/3k4/8/8/r1K5/8/8/2R5 w - - 0 1");
        pos.setWhiteMove(false);
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(kV, getSEE(sc, new Move(TextIO.getSquare("a4"), TextIO.getSquare("c4"), Piece.EMPTY)));

        
        // Test blocking pieces
        pos = TextIO.readFEN("r7/p2k4/8/r7/P7/8/4K3/R7 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxa4")));    // Ra8 doesn't help
        
        pos.setPiece(TextIO.getSquare("a7"), Piece.BBISHOP);
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxa4")));    // Ra8 doesn't help

        pos.setPiece(TextIO.getSquare("a7"), Piece.BPAWN);
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxa4")));    // Ra8 doesn't help

        pos.setPiece(TextIO.getSquare("a7"), Piece.BQUEEN);
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV, getSEE(sc, TextIO.stringToMove(pos, "Rxa4")));         // Ra8 does help

        pos = TextIO.readFEN("8/3k4/R7/r7/P7/8/4K3/8 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxa4")));

        pos = TextIO.readFEN("Q7/q6k/R7/r7/P7/8/4K3/8 b - - 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        assertEquals(pV - rV, getSEE(sc, TextIO.stringToMove(pos, "Rxa4")));

        pos = TextIO.readFEN("8/3k4/5R2/8/4pP2/8/8/3K4 b - f3 0 1");
        sc = new Search(pos, nullHist, 0, tt, ht);
        int score1 = EvaluateTest.evalWhite(sc.pos);
        long h1 = sc.pos.zobristHash();
        assertEquals(0, getSEE(sc, TextIO.stringToMove(pos, "exf3")));
        int score2 = EvaluateTest.evalWhite(sc.pos);
        long h2 = sc.pos.zobristHash();
        assertEquals(score1, score2);
        assertEquals(h1, h2);
    }

    /**
     * Test of scoreMoveList method, of class Search.
     */
    @Test
    public void testScoreMoveList() throws ChessParseError {
        System.out.println("SEEorder");
        Position pos = TextIO.readFEN("r2qk2r/ppp2ppp/1bnp1nb1/1N2p3/3PP3/1PP2N2/1P3PPP/R1BQRBK1 w kq - 0 1");
        Search sc = new Search(pos, nullHist, 0, tt, ht);
        MoveGen moveGen = new MoveGen();
        MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
        sc.scoreMoveList(moves, 0);
        for (int i = 0; i < moves.size; i++) {
            Search.selectBest(moves, i);
            if (i > 0) {
                int sc1 = moves.m[i - 1].score;
                int sc2 = moves.m[i].score;
                assertTrue(sc2 <= sc1);
            }
        }

        moves = moveGen.pseudoLegalMoves(pos);
        moves.m[0].score = 17;
        moves.m[1].score = 666;
        moves.m[2].score = 4711;
        sc.scoreMoveList(moves, 0, 2);
        assertEquals(17, moves.m[0].score);
        assertEquals(666, moves.m[1].score);
        for (int i = 1; i < moves.size; i++) {
            Search.selectBest(moves, i);
            if (i > 1) {
                int sc1 = moves.m[i - 1].score;
                int sc2 = moves.m[i].score;
                assertTrue(sc2 <= sc1);
            }
        }

        // The hashMove should be first in the list
        Move m = TextIO.stringToMove(pos, "Ra6");
        moves = moveGen.pseudoLegalMoves(pos);
        boolean res = Search.selectHashMove(moves, m);
        assertEquals(true, res);
        assertEquals(m, moves.m[0]);
    }
}
