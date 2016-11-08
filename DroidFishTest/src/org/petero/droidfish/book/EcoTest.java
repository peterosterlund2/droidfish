/*
    DroidFish - An Android chess program.
    Copyright (C) 2016  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.book;

import org.petero.droidfish.PGNOptions;
import org.petero.droidfish.gamelogic.Game;
import org.petero.droidfish.gamelogic.GameTree;
import org.petero.droidfish.gamelogic.TimeControlData;

import android.test.AndroidTestCase;

/** Test of EcoDb class. */
public class EcoTest extends AndroidTestCase {

    public EcoTest() {
    }

    public void testEco() throws Throwable {
        EcoDb ecoDb = EcoDb.getInstance(getContext());
        {
            String pgn = "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1";
            GameTree gt = readPGN(pgn);
            String eco = ecoDb.getEco(gt, gt.currentNode);
            assertEquals("", eco);

            gt.goForward(0);
            eco = ecoDb.getEco(gt, gt.currentNode);
            assertEquals("B00: King's pawn opening", eco);

            gt.goForward(0);
            eco = ecoDb.getEco(gt, gt.currentNode);
            assertEquals("C20: King's pawn game", eco);

            gt.goForward(0);
            eco = ecoDb.getEco(gt, gt.currentNode);
            assertEquals("C40: King's knight opening", eco);
        
            gt.goForward(0);
            eco = ecoDb.getEco(gt, gt.currentNode);
            assertEquals("C44: King's pawn game", eco);
        
            gt.goForward(0);
            eco = ecoDb.getEco(gt, gt.currentNode);
            assertEquals("C60: Ruy Lopez (Spanish opening)", eco);
        }
        {
            Game game = new Game(null, new TimeControlData());
            game.processString("e4");
            game.processString("e5");
            game.processString("Nf3");
            game.processString("Nf6");
            String eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("C42: Petrov's defence", eco);

            game.processString("Nxe5");
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("C42: Petrov's defence", eco);

            game.processString("d6");
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("C42: Petrov's defence", eco);

            game.processString("Nxf7");
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("C42: Petrov, Cochrane gambit", eco);

            game.undoMove();
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("C42: Petrov's defence", eco);

            game.processString("Nf3");
            game.processString("Nxe4");
            game.processString("d4");
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("C42: Petrov, classical attack", eco);
        }
        {
            Game game = new Game(null, new TimeControlData());
            game.processString("e4");
            game.processString("c5");
            String eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("B20: Sicilian defence", eco);

            game.processString("h3");
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("B20: Sicilian defence", eco);

            game.processString("Nc6");
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("B20: Sicilian defence", eco);

            game.processString("g3");
            eco = ecoDb.getEco(game.tree, game.tree.currentNode);
            assertEquals("B20: Sicilian defence", eco);
        }
    }

    public void testEcoFromFEN() throws Throwable {
        EcoDb ecoDb = EcoDb.getInstance(getContext());
        GameTree gt = gtFromFEN("rnbqkbnr/ppp2ppp/8/3p4/3P4/8/PPP2PPP/RNBQKBNR w KQkq - 0 4");
        String eco = ecoDb.getEco(gt, gt.currentNode);
        assertEquals("C01: French, exchange variation", eco);

        gt = gtFromFEN("rnbqk1nr/ppppppbp/6p1/8/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 1 3");
        eco = ecoDb.getEco(gt, gt.currentNode);
        assertEquals("B06: Robatsch (modern) defence", eco);
    }

    /** Create GameTree from PGN. */
    private GameTree readPGN(String pgn) throws Throwable {
        GameTree gt = new GameTree(null);
        PGNOptions options = new PGNOptions();
        options.imp.variations = true;
        options.imp.comments = true;
        options.imp.nag = true;
        boolean res = gt.readPGN(pgn, options);
        assertEquals(true, res);
        return gt;
    }

    /** Create a GameTree starting from a FEN position, and containing no moves. */
    private GameTree gtFromFEN(String fen) throws Throwable {
        String pgn = String.format("[FEN \"%s\"][SetUp \"1\"", fen);
        return readPGN(pgn);
    }
}
