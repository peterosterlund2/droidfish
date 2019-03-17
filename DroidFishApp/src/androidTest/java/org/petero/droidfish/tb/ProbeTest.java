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
