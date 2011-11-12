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
public class HistoryTest {

    public HistoryTest() {
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
     * Test of getHistScore method, of class History.
     */
    @Test
    public void testGetHistScore() throws ChessParseError {
        System.out.println("getHistScore");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        History hs = new History();
        Move m1 = TextIO.stringToMove(pos, "e4");
        Move m2 = TextIO.stringToMove(pos, "d4");
        assertEquals(0, hs.getHistScore(pos, m1));

        hs.addSuccess(pos, m1, 1);
        assertEquals(1 * 49 / 1, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addSuccess(pos, m1, 1);
        assertEquals(1 * 49 / 1, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addFail(pos, m1, 1);
        assertEquals(2 * 49 / 3, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addFail(pos, m1, 1);
        assertEquals(2 * 49 / 4, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addSuccess(pos, m2, 1);
        assertEquals(2 * 49 / 4, hs.getHistScore(pos, m1));
        assertEquals(1 * 49 / 1, hs.getHistScore(pos, m2));
    }
}
