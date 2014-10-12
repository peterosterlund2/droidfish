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
        assertEquals(true, ProbeResult.compareScore(0, 0, 1, 1) < 0);
        assertEquals(true, ProbeResult.compareScore(1, 1, 1, 2) > 0);
        assertEquals(true, ProbeResult.compareScore(-1, 1, 0, 0) < 0);
        assertEquals(true, ProbeResult.compareScore(-1, 20, -1, 10) > 0);
        assertEquals(true, ProbeResult.compareScore(-1, 20, 1, 21) < 0);
        assertEquals(true, ProbeResult.compareScore(-1, 20, 1, 19) < 0);

        assertEquals(true, ProbeResult.compareScore(1, 0, 0, 0) > 0);
        assertEquals(true, ProbeResult.compareScore(1, 0, 1, 1) > 0);
        assertEquals(true, ProbeResult.compareScore(0, 0, 1, 0) < 0);
        assertEquals(true, ProbeResult.compareScore(1, 1, 1, 0) < 0);

        assertEquals(true, ProbeResult.compareScore(-1, 0, 0, 0) < 0);
        assertEquals(true, ProbeResult.compareScore(-1, 0, -1, 1) < 0);
        assertEquals(true, ProbeResult.compareScore(0, 0, -1, 0) > 0);
        assertEquals(true, ProbeResult.compareScore(-1, 1, -1, 0) > 0);

        assertEquals(true, ProbeResult.compareScore(-1, 0, 1, 0) < 0);
        assertEquals(true, ProbeResult.compareScore(1, 0, -1, 0) > 0);
    }

    public void testCompareProbeResult() {
        // NONE vs NONE
        assertEquals(0, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)));
        assertEquals(0, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 2)));
        assertEquals(0, new ProbeResult(Type.NONE, 1, 2).compareTo(new ProbeResult(Type.NONE, -1, 3)));
        assertEquals(0, new ProbeResult(Type.NONE, 1, 2).compareTo(new ProbeResult(Type.NONE, 0, 0)));
        assertEquals(0, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 2)));

        // NONE vs DTM,DTZ,WDL
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.WDL, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.WDL, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTM, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.DTZ, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, 0, 0).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, 1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.NONE, 0, 0)) < 0);

        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.WDL, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.WDL, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTM, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.DTZ, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, 1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, 0, 0).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, 1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.NONE, 1, 1)) < 0);

        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.WDL, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.WDL, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTM, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.DTZ, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.NONE, -1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, 0, 0).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, 1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.NONE, -1, 1)) < 0);

        // DTM vs DTM
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTM, 1, 10)) == 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTM, 1, 11)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 11).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.DTM, -1, 1)) == 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 3).compareTo(new ProbeResult(Type.DTM, -1, 3)) == 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 3).compareTo(new ProbeResult(Type.DTM, -1, 5)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 5).compareTo(new ProbeResult(Type.DTM, -1, 3)) < 0);

        // DTZ vs DTZ
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 10)) == 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 11)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 11).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.DTZ, -1, 1)) == 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 3).compareTo(new ProbeResult(Type.DTZ, -1, 3)) == 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 3).compareTo(new ProbeResult(Type.DTZ, -1, 5)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 5).compareTo(new ProbeResult(Type.DTZ, -1, 3)) < 0);

        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) == 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.WDL,  0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.WDL,  0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.WDL,  0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.WDL,  1, 1)) == 0);

        // DTM vs DTZ
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 11)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, 1, 9)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, -1, 11)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.DTZ, -1, 9)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ,  1, 11).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ,   1, 9).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ,   0, 0).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 11).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ,  -1, 9).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);

        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 3)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTZ, -1, 4)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 3).compareTo(new ProbeResult(Type.DTM, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 4).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);

        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, -1, 7)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, -1, 9)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, 1, 7)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.DTZ, 1, 9)) > 0);

        // DTM vs WDL
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.WDL, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.WDL, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 1, 10).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTM, 1, 10)) > 0);

        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.WDL,  0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTM, 0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTM, 0, 0)) > 0);

        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.WDL,  0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 8).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);

        // DTZ vs WDL
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.WDL, 1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.WDL, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 1, 10).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTZ, 1, 10)) > 0);

        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.WDL,  0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.WDL, -1, 1)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL,  0, 0).compareTo(new ProbeResult(Type.DTZ, 0, 0)) == 0);
        assertEquals(true, new ProbeResult(Type.WDL,  1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.WDL, -1, 1).compareTo(new ProbeResult(Type.DTZ, 0, 0)) > 0);

        assertEquals(true, new ProbeResult(Type.DTZ, -1, 8).compareTo(new ProbeResult(Type.WDL, -1, 1)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 8).compareTo(new ProbeResult(Type.WDL,  0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 8).compareTo(new ProbeResult(Type.WDL,  1, 1)) > 0);

        // Win-in-zero and loss-in-zero
        assertEquals(true, new ProbeResult(Type.DTM, 1, 0).compareTo(new ProbeResult(Type.DTM,  0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM,  1, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, -1, 0).compareTo(new ProbeResult(Type.DTM,  0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTM, 0, 0).compareTo(new ProbeResult(Type.DTM,  -1, 0)) < 0);

        assertEquals(true, new ProbeResult(Type.DTZ, 1, 0).compareTo(new ProbeResult(Type.DTZ,  0, 0)) < 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ,  1, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, -1, 0).compareTo(new ProbeResult(Type.DTZ,  0, 0)) > 0);
        assertEquals(true, new ProbeResult(Type.DTZ, 0, 0).compareTo(new ProbeResult(Type.DTZ,  -1, 0)) < 0);
    }
}
