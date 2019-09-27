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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.petero.droidfish.DroidFishApp;
import org.petero.droidfish.activities.PGNFile;
import org.petero.droidfish.activities.PGNFile.GameInfo;

import junit.framework.TestCase;

public class PGNFileTest extends TestCase {

    public PGNFileTest() {
    }
    
    public void testParsePGNFile() throws Throwable {
        File tmpDir = DroidFishApp.getContext().getCacheDir();
        File f = new File(tmpDir, "test.pgn");
        {
            String[] lines = {
                    "[Event \"x\"]",
                    "*"
            };
            writeFile(f, lines);
            PGNFile pgnFile = new PGNFile(f.getAbsolutePath());
            ArrayList<GameInfo> gi = pgnFile.getGameInfo(null, null);
            assertEquals(1, gi.size());
            assertEquals(0, gi.get(0).startPos);
            assertEquals(14, gi.get(0).endPos);
            assertEquals("1.  -  x ", gi.get(0).info);
        }
        {
            String[] lines = {
                    "[Event \"\"]",
                    "[Site \"\"]",
                    "[Date \"2007.??.??\"]",
                    "[Round \"?\"]",
                    "[White \"Tomashevsky Evgeny\"]",
                    "[Black \"Morozevich Alexander\"]",
                    "[Result \"0-1\"]",
                    "[WhiteElo \"2646\"]",
                    "[BlackElo \"2755\"]",
                    "[ECO \"A09\"]",
                    "",
                    "1.Nf3 d5 2.c4 d4 3.g3 c5 4.e3 Nc6 5.exd4 cxd4 6.Bg2 e5 7.O-O f6 8.d3 Nge7 ",
                    "9.a3 a5 10.Nbd2 Ng6 11.h4 Be7 12.Re1 O-O 13.h5 Nh8 14.Nh4 g5 15.hxg6 hxg6 ",
                    "16.Be4 f5 17.Bxc6 bxc6 18.Nhf3 Nf7 19.Nxe5 Nxe5 20.Rxe5 Bd6 21.Re1 c5 22.",
                    "Nf3 Kg7 23.Bg5 Qc7 24.Nh4 Rf7 25.Qe2 Bd7 26.f4 Rh8 27.Kf2 Qb7 28.Rab1 Rh5 ",
                    "29.Nf3 Bc6 30.Rg1 Rf8 31.Nh4 Rxg5 32.fxg5 f4 33.g4 Re8 34.Qd2 Re3 35.b4 ",
                    "Qe7 36.bxc5 Qxg5 37.Rh1 Bxh1 38.Rxh1 Bxc5 39.Qd1 Qe7 40.a4 Rg3 41.Ng2 Bb4 ",
                    "42.Rh2 Qe5 43.c5 f3 44.Nh4 Qf4 0-1",
                    "",
                    "[Event \"\"]",
                    "[Site \"\"]",
                    "[Date \"2007.??.??\"]",
                    "[Round \"?\"]",
                    "[White \"Grischuk Alexander\"]",
                    "[Black \"Jakovenko Dmitry\"]",
                    "[Result \"1/2-1/2\"]",
                    "[WhiteElo \"2715\"]",
                    "[BlackElo \"2710\"]",
                    "[ECO \"A30\"]",
                    "",
                    "1.c4 Nf6 2.Nc3 c5 3.Nf3 e6 4.g3 b6 5.Bg2 Bb7 6.O-O Be7 7.b3 d6 8.Bb2 O-O ",
                    "9.e3 Nbd7 10.d4 Ne4 11.d5 Nxc3 12.Bxc3 exd5 13.cxd5 Bf6 14.Bxf6 Qxf6 15.e4",
                    "Rfe8 16.Re1 Ba6 17.Rc1 Rac8 18.Re3 h6 19.h4 Ne5 20.Bh3 Rcd8 21.Nxe5 Qxe5 ",
                    "22.f4 Qd4 23.Qxd4 cxd4 24.Ree1 Bd3 25.Red1 Bxe4 26.Rxd4 Re7 27.Kf2 g6 28.",
                    "Be6 Bf5 29.Bxf5 gxf5 30.Rd2 h5 31.Re2 Kf8 32.a4 Rxe2+ 33.Kxe2 Ke7 34.Rc7+ ",
                    "Rd7 35.Rc8 Rd8 36.Rc7+ Rd7 37.Rc6 Rd8 38.Rc7+ 1/2-1/2",
                    ""
            };
            writeFile(f, lines);
            PGNFile pgnFile = new PGNFile(f.getAbsolutePath());
            ArrayList<GameInfo> gi = pgnFile.getGameInfo(null, null);
            assertEquals(2, gi.size());
            assertEquals(0, gi.get(0).startPos);
            assertEquals(660, gi.get(0).endPos);
            assertEquals("1. Tomashevsky Evgeny - Morozevich Alexander 2007.??.?? 0-1", gi.get(0).info);
            assertEquals(660, gi.get(1).startPos);
            assertEquals(1264, gi.get(1).endPos);
            assertEquals("2. Grischuk Alexander - Jakovenko Dmitry 2007.??.?? 1/2-1/2", gi.get(1).info);
        }
        {
            String[] lines = {
                    "\ufeff [ White \"test\\\"abc\\\"\" ]",
                    "[Black\"\\\"\"]",
                    "[Result \"0-1\"] ",
                    "",
                    "{ test",
                    "[#] \"test\" }",
                    "*",
                    "",
                    "[White \"w\"]",
                    "[Black \"b\"]",
                    "%[Black \"b2\"]",
                    "[Result \"1-0\" ]",
                    "",
                    "*",
            };
            writeFile(f, lines);
            PGNFile pgnFile = new PGNFile(f.getAbsolutePath());
            ArrayList<GameInfo> gi = pgnFile.getGameInfo(null, null);
            assertEquals(2, gi.size());
            assertEquals(4, gi.get(0).startPos);
            assertEquals(80, gi.get(0).endPos);
            assertEquals("1. test\"abc\" - \" 0-1", gi.get(0).info);
            assertEquals(80, gi.get(1).startPos);
            assertEquals(137, gi.get(1).endPos);
            assertEquals("2. w - b 1-0", gi.get(1).info);

            gi = pgnFile.getGameInfo(1);
            assertEquals(1, gi.size());
        }
    }
    
    
    private void writeFile(File f, String[] lines) throws IOException {
        FileOutputStream fs = new FileOutputStream(f);
        for (String s : lines) {
            fs.write(s.getBytes());
            fs.write('\n');
        }
        fs.close();
    }
}
