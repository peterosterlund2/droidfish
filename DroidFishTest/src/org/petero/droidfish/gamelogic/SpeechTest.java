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

package org.petero.droidfish.gamelogic;

import org.petero.droidfish.Speech;

import junit.framework.TestCase;

public class SpeechTest extends TestCase {
    public SpeechTest() {
    }

    public void testEnglish() {
        String lang = "en";
        {
            Game game = new Game(null, new TimeControlData());
            Pair<Boolean,Move> res = game.processString("e4");
            assertEquals("e4", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("d5");
            assertEquals("d5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("exd5");
            assertEquals("e takes d5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Qxd5");
            assertEquals("Queen takes d5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Ne2");
            assertEquals("Knight e2", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Nf6");
            assertEquals("Knight f6", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Nbc3");
            assertEquals("Knight b c3", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("e5");
            assertEquals("e5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("b4");
            assertEquals("b4", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("a5");
            assertEquals("a5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("a3");
            assertEquals("a3", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("axb4");
            assertEquals("ae takes b4", Speech.moveToText(game.prevPos(), res.second, lang));
        
            res = game.processString("axb4");
            assertEquals("ae takes b4", Speech.moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            Pair<Boolean,Move> res = game.processString("d4");
            assertEquals("d4", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("e5");
            assertEquals("e5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("dxe5");
            assertEquals("d take e5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("f6");
            assertEquals("f6", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("exf6");
            assertEquals("e takes f6", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Bb4");
            assertEquals("Bishop b4. Check!", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("c3");
            assertEquals("c3", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Ne7");
            assertEquals("Knight e7", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("cxb4");
            assertEquals("c takes b4", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("O-O");
            assertEquals("Short castle", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("fxg7");
            assertEquals("f takes g7", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("h6");
            assertEquals("h6", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("gxf8Q+");
            assertEquals("g takes f8 Queen. Check!", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Kxf8");
            assertEquals("King takes f8", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("b5");
            assertEquals("b5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("a5");
            assertEquals("a5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("bxa6");
            assertEquals("b takes a6", Speech.moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            Pair<Boolean,Move> res = game.processString("f4");
            assertEquals("f4", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("e5");
            assertEquals("e5", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("g4");
            assertEquals("g4", Speech.moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Qh4");
            assertEquals("Queen h4. Check mate!", Speech.moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            playMoves(game, "d4 d5 Nc3 Nc6 Bf4 Bf5 Qd2 Qd7");
            Pair<Boolean,Move> res = game.processString("O-O-O");
            assertEquals("Long castle", Speech.moveToText(game.prevPos(), res.second, lang));
            playMoves(game, "Nxd4 Nxd5 Qxd5 Qxd4 Qxd4 Nf3 Qxd1 Kxd1");
            res = game.processString("O-O-O");
            assertEquals("Long castle. Check!", Speech.moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            playMoves(game, "e4 e5 h3 Bb4 Ne2 Bc3");
            Pair<Boolean,Move> res = game.processString("Nexc3");
            assertEquals("Knight e takes c3", Speech.moveToText(game.prevPos(), res.second, lang));
        }
    }

    private void playMoves(Game game, String moves) {
        for (String move : moves.split(" ")) {
            Pair<Boolean,Move> res = game.processString(move);
            assertTrue(res.first);
        }
    }
}
