package org.petero.droidfish.tb;

import org.petero.droidfish.tb.ProbeResult;
import org.petero.droidfish.tb.ProbeResult.Type;

import junit.framework.TestCase;

public class ProbeResultTest extends TestCase {
    public ProbeResultTest() {
    }

    public void testCompareScore() {
        assertEquals(0, ProbeResult.compareScore(0, 0, 0, 0));
        assertEquals(0, ProbeResult.compareScore(1, 3, 1, 3));
        assertEquals(0, ProbeResult.compareScore(-1, 4, -1, 4));
        assertTrue(ProbeResult.compareScore(0, 0, 1, 1) < 0);
        assertTrue(ProbeResult.compareScore(1, 1, 1, 2) > 0);
        assertTrue(ProbeResult.compareScore(-1, 1, 0, 0) < 0);
        assertTrue(ProbeResult.compareScore(-1, 20, -1, 10) > 0);
        assertTrue(ProbeResult.compareScore(-1, 20, 1, 21) < 0);
        assertTrue(ProbeResult.compareScore(-1, 20, 1, 19) < 0);

        assertTrue(ProbeResult.compareScore(1, 0, 0, 0) > 0);
        assertTrue(ProbeResult.compareScore(1, 0, 1, 1) > 0);
        assertTrue(ProbeResult.compareScore(0, 0, 1, 0) < 0);
        assertTrue(ProbeResult.compareScore(1, 1, 1, 0) < 0);

        assertTrue(ProbeResult.compareScore(-1, 0, 0, 0) < 0);
        assertTrue(ProbeResult.compareScore(-1, 0, -1, 1) < 0);
        assertTrue(ProbeResult.compareScore(0, 0, -1, 0) > 0);
        assertTrue(ProbeResult.compareScore(-1, 1, -1, 0) > 0);

        assertTrue(ProbeResult.compareScore(-1, 0, 1, 0) < 0);
        assertTrue(ProbeResult.compareScore(1, 0, -1, 0) > 0);
    }

    public void testCompareProbeResult() {
        // NONE vs NONE
        assertEquals(0, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)));
        assertEquals(0, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 2)));
        assertEquals(0, new ProbeResult(Type.NONE, 1, 2).compareTo(new ProbeResult(Type.NONE, -1, 3)));
        assertEquals(0, new ProbeResult(Type.NONE, 1, 2).compareTo(new ProbeResult(Type.NONE, 0, 0)));
        assertEquals(0, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 2)));

        // NONE vs DTM,DTZ,WDL
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.WDL, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.WDL, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTM, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTZ, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.WDL, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.WDL, 1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);

        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.WDL, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.WDL, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTM, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTZ, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL, 1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);

        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.WDL, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.WDL, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTM, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTZ, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL, 0, 0).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL, 1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);

        // DTM vs DTM
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTM, 1, 10)) == 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTM, 1, 11)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 11).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) == 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.DTM, -1, 1)) == 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 3).compareTo(new ProbeResult(Type.DTM, -1, 3)) == 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 3).compareTo(new ProbeResult(Type.DTM, -1, 5)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 5).compareTo(new ProbeResult(Type.DTM, -1, 3)) < 0);

        // DTZ vs DTZ
        assertTrue(new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 10)) == 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 11)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 11).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) == 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.DTZ, -1, 1)) == 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 3).compareTo(new ProbeResult(Type.DTZ, -1, 3)) == 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 3).compareTo(new ProbeResult(Type.DTZ, -1, 5)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 5).compareTo(new ProbeResult(Type.DTZ, -1, 3)) < 0);

        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) == 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.WDL,  0, 0)) > 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertTrue(new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.WDL,  0, 0)) == 0);
        assertTrue(new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertTrue(new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.WDL,  0, 0)) < 0);
        assertTrue(new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.WDL,  1, 1)) == 0);

        // DTM vs DTZ
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 11)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 9)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, -1, 11)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, -1, 9)) < 0);
        assertTrue(new ProbeResult(Type.DTZ,  1, 11).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.DTZ,   1, 9).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.DTZ,   0, 0).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 11).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.DTZ,  -1, 9).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);

        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) == 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 3)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTZ, -1, 4)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) == 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 3).compareTo(new ProbeResult(Type.DTM, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 4).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);

        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, -1, 7)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, -1, 9)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, 1, 7)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, 1, 9)) > 0);

        // DTM vs WDL
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.WDL, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.WDL, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);

        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.WDL,  0, 0)) == 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) == 0);
        assertTrue(new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);

        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.WDL,  0, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);

        // DTZ vs WDL
        assertTrue(new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.WDL, 1, 1)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.WDL, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);

        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.WDL,  0, 0)) == 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertTrue(new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) == 0);
        assertTrue(new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) < 0);
        assertTrue(new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);

        assertTrue(new ProbeResult(Type.DTZ, -1, 8).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 8).compareTo(new ProbeResult(Type.WDL,  0, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 8).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);

        // Win-in-zero and loss-in-zero
        assertTrue(new ProbeResult(Type.DTM, 1, 0).compareTo(new ProbeResult(Type.DTM,  0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM,  1, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTM, -1, 0).compareTo(new ProbeResult(Type.DTM,  0, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM,  -1, 0)) < 0);

        assertTrue(new ProbeResult(Type.DTZ, 1, 0).compareTo(new ProbeResult(Type.DTZ,  0, 0)) < 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ,  1, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, -1, 0).compareTo(new ProbeResult(Type.DTZ,  0, 0)) > 0);
        assertTrue(new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ,  -1, 0)) < 0);
    }
}
