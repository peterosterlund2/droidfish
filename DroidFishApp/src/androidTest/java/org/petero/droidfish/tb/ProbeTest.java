/*
    DroidFish - An Android chess program.
    Copyright (C) 2014  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.tb;

import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.tb.Probe;
import org.petero.droidfish.tb.ProbeResult;

import android.os.Environment;

import junit.framework.TestCase;

public class ProbeTest extends TestCase {
    public ProbeTest() {
    }

    public void testDTZProbe() throws Throwable {
        Probe probe = Probe.getInstance();
        String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
        probe.setPath("", sd + "/DroidFish/rtb", true);

        Position pos = TextIO.readFEN("K7/P1k2b2/8/3N4/8/8/8/8 b - - 0 1");
        ProbeResult res = probe.probe(pos);
        assertEquals(ProbeResult.Type.DTZ, res.type);
        assertEquals(1, res.wdl);
        assertEquals(1, res.score);

        pos = TextIO.readFEN("8/5N2/8/8/p1N2k2/3K4/8/8 w - - 0 1"); // Draw because of 50-move rule
        res = probe.probe(pos);
        assertEquals(ProbeResult.Type.DTZ, res.type);
        assertEquals(0, res.wdl);
        assertEquals(0, res.score);
    }
}
