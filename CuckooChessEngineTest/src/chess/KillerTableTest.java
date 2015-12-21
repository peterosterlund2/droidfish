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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class KillerTableTest {

    public KillerTableTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of addKiller method, of class KillerTable.
     */
    @Test
    public void testAddKiller() {
        System.out.println("addKiller");
        KillerTable kt = new KillerTable();
        Move m = new Move(TextIO.getSquare("b1"), TextIO.getSquare("b5"), Piece.EMPTY);
        kt.addKiller(3, m);
        kt.addKiller(7, m);
        kt.addKiller(3, m);
        kt.addKiller(3, m);
    }

    /**
     * Test of getKillerScore method, of class KillerTable.
     */
    @Test
    public void testGetKillerScore() {
        System.out.println("getKillerScore");
        KillerTable kt = new KillerTable();
        Move m1 = new Move(TextIO.getSquare("b1"), TextIO.getSquare("b5"), Piece.EMPTY);
        Move m2 = new Move(TextIO.getSquare("c1"), TextIO.getSquare("d2"), Piece.EMPTY);
        Move m3 = new Move(TextIO.getSquare("e1"), TextIO.getSquare("g1"), Piece.EMPTY);
        kt.addKiller(0, m1);
        assertEquals(4, kt.getKillerScore(0, m1));
        assertEquals(0, kt.getKillerScore(0, m2));
        assertEquals(0, kt.getKillerScore(0, new Move(m2)));
        kt.addKiller(0, m1);
        assertEquals(4, kt.getKillerScore(0, m1));
        kt.addKiller(0, m2);
        assertEquals(4, kt.getKillerScore(0, m2));
        assertEquals(4, kt.getKillerScore(0, new Move(m2)));    // Must compare by value
        assertEquals(3, kt.getKillerScore(0, m1));
        kt.addKiller(0, new Move(m2));
        assertEquals(4, kt.getKillerScore(0, m2));
        assertEquals(3, kt.getKillerScore(0, m1));
        assertEquals(0, kt.getKillerScore(0, m3));
        kt.addKiller(0, m3);
        assertEquals(0, kt.getKillerScore(0, m1));
        assertEquals(3, kt.getKillerScore(0, m2));
        assertEquals(4, kt.getKillerScore(0, m3));

        assertEquals(0, kt.getKillerScore(1, m3));
        assertEquals(2, kt.getKillerScore(2, m3));
        assertEquals(0, kt.getKillerScore(3, m3));
        assertEquals(0, kt.getKillerScore(4, m3));

        kt.addKiller(2, m2);
        assertEquals(4, kt.getKillerScore(2, m2));
        assertEquals(3, kt.getKillerScore(0, m2));
    }
}
