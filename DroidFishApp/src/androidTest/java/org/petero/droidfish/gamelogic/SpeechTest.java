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

import android.util.Pair;

import org.petero.droidfish.Speech;

import junit.framework.TestCase;

public class SpeechTest extends TestCase {
    public SpeechTest() {
    }

    private String[] moveToText(Position pos, Move move, String[] langStrs) {
        String[] ret = new String[langStrs.length];
        for (int i = 0; i < langStrs.length; i++) {
            Speech.Language lang = Speech.Language.fromString(langStrs[i]);
            assertTrue(lang != null);
            ret[i] = Speech.moveToText(pos, move, lang);
        }
        return ret;
    }
    
    private static void assertEquals(String[] expected, String[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++)
            assertEquals(expected[i], actual[i]);
    }

    public void testEnglish() {
        String[] lang = {"en", "de", "es"};
        {
            Game game = new Game(null, new TimeControlData());
            Pair<Boolean,Move> res = game.processString("e4");
            assertEquals(new String[]{"e4", "e4", "e4"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("d5");
            assertEquals(new String[]{"d5", "d5", "d5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("exd5");
            assertEquals(new String[]{"e takes, d5", "e schlägt d5", "e captura d5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Qxd5");
            assertEquals(new String[]{"Queen takes, d5", "Dame schlägt d5", "Dama captura d5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Ne2");
            assertEquals(new String[]{"Knight e2", "Springer e2", "Caballo e2"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Nf6");
            assertEquals(new String[]{"Knight f6", "Springer f6", "Caballo f6"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Nbc3");
            assertEquals(new String[]{"Knight b c3", "Springer b c3", "Caballo b c3"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("e5");
            assertEquals(new String[]{"e5", "e5", "e5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("b4");
            assertEquals(new String[]{"b 4", "b 4", "b 4"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("a5");
            assertEquals(new String[]{"a5", "a5", "a5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("a3");
            assertEquals(new String[]{"a3", "a3", "a3"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("axb4");
            assertEquals(new String[]{"ae takes, b 4", "a schlägt b 4", "a captura b 4"},
                    moveToText(game.prevPos(), res.second, lang));
        
            res = game.processString("axb4");
            assertEquals(new String[]{"ae takes, b 4", "a schlägt b 4", "a captura b 4"},
                    moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            Pair<Boolean,Move> res = game.processString("d4");
            assertEquals(new String[]{"d4", "d4", "d4"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("e5");
            assertEquals(new String[]{"e5", "e5", "e5"}, moveToText(game.prevPos(), res.second, lang));

            res = game.processString("dxe5");
            assertEquals(new String[]{"d takes, e5", "d schlägt e5", "d captura e5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("f6");
            assertEquals(new String[]{"f6", "f6", "f6"}, moveToText(game.prevPos(), res.second, lang));

            res = game.processString("exf6");
            assertEquals(new String[]{"e takes, f6", "e schlägt f6", "e captura f6"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Bb4");
            assertEquals(new String[]{"Bishop b 4 , check!", "Läufer b 4 , Schach!", "Alfil b 4 , jaque!"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("c3");
            assertEquals(new String[]{"c3", "c3", "c3"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Ne7");
            assertEquals(new String[]{"Knight e7", "Springer e7", "Caballo e7"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("cxb4");
            assertEquals(new String[]{"c takes, b 4", "c schlägt b 4", "c captura b 4"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("O-O");
            assertEquals(new String[]{"Short castle", "Kleine Rochade", "Enroque corto"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("fxg7");
            assertEquals(new String[]{"f takes, g7", "f schlägt g7", "f captura g7"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("h6");
            assertEquals(new String[]{"h6", "h6", "h6"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("gxf8Q+");
            assertEquals(new String[]{"g takes, f8 Queen , check!", "g schlägt f8 Umwandlung zu Dame , Schach!",
                                      "g captura f8 Dama , jaque!"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Kxf8");
            assertEquals(new String[]{"King takes, f8", "König schlägt f8", "Rey captura f8"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("b5");
            assertEquals(new String[]{"b5", "b5", "b5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("a5");
            assertEquals(new String[]{"a5", "a5", "a5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("bxa6");
            assertEquals(new String[]{"b takes, a6", "b schlägt a6 en passant", "b captura a6 al paso"},
                    moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            Pair<Boolean,Move> res = game.processString("f4");
            assertEquals(new String[]{"f4", "f4", "f4"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("e5");
            assertEquals(new String[]{"e5", "e5", "e5"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("g4");
            assertEquals(new String[]{"g4", "g4", "g4"},
                    moveToText(game.prevPos(), res.second, lang));

            res = game.processString("Qh4");
            assertEquals(new String[]{"Queen h4 , check mate!", "Dame h4 , Schach matt!", "Dama h4 , mate!"},
                    moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            playMoves(game, "d4 d5 Nc3 Nc6 Bf4 Bf5 Qd2 Qd7");
            Pair<Boolean,Move> res = game.processString("O-O-O");
            assertEquals(new String[]{"Long castle", "Große Rochade", "Enroque largo"},
                    moveToText(game.prevPos(), res.second, lang));
            playMoves(game, "Nxd4 Nxd5 Qxd5 Qxd4 Qxd4 Nf3 Qxd1 Kxd1");
            res = game.processString("O-O-O");
            assertEquals(new String[]{"Long castle , check!", "Große Rochade , Schach!", "Enroque largo , jaque!"},
                    moveToText(game.prevPos(), res.second, lang));
            playMoves(game, "Kc1");
            res = game.processString("Rd7");
            assertEquals(new String[]{"Rook d7", "Turm d7", "Torre d7"},
                    moveToText(game.prevPos(), res.second, lang));
        }
        {
            Game game = new Game(null, new TimeControlData());
            playMoves(game, "e4 e5 h3 Bb4 Ne2 Bc3");
            Pair<Boolean,Move> res = game.processString("Nexc3");
            assertEquals(new String[]{"Knight e takes, c3", "Springer e schlägt c3", "Caballo e captura c3"},
                    moveToText(game.prevPos(), res.second, lang));
            assertEquals(new String[]{"", "", ""},
                    moveToText(game.prevPos(), new Move(0, 0, 0), lang));
        }
    }

    private void playMoves(Game game, String moves) {
        for (String move : moves.split(" ")) {
            Pair<Boolean,Move> res = game.processString(move);
            assertTrue(res.first);
        }
    }
}
