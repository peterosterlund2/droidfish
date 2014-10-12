package org.petero.droidfish.tb;

/** Tablebase probe result. */
public final class ProbeResult implements Comparable<ProbeResult> {
    public static enum Type {
        DTM,    // score is distance (full moves) to mate, or 0
        DTZ,    // score is distance (full moves) to zeroing move, or 0
        WDL,    // score is +-1 or 0
        NONE,   // No info available, score is 0
    }

    public Type type;
    public int wdl;   // +1 if if side to move wins, 0 for draw, -1 for loss
    public int score; // Distance to win in plies. Always >= 0.
                      // Note! Zero if side to move is checkmated.

    ProbeResult(Type type, int wdl, int score) {
        this.type = type;
        this.wdl = wdl;
        this.score = score;
    }

    /**
     * Return > 0 if other is "better" than this.
     * A win is better than a draw, which is better than a loss.
     * A DTM win is better than a DTZ win, which is better than a WDL win.
     * A WDL loss is better than a DTZ loss, which is better than a DTM loss.
     */
    @Override
    public final int compareTo(ProbeResult other) {
        final Type type1 = this.type;
        final Type type2 = other.type;
        final boolean none1 = type1 == Type.NONE;
        final boolean none2 = type2 == Type.NONE;
        if (none1 != none2)
            return none2 ? -1 : 1;
        if (none1)
            return 0;
        final int wdl1 = this.wdl;
        final int wdl2 = other.wdl;
        final boolean win1 = wdl1 > 0;
        final boolean win2 = wdl2 > 0;
        if (win1 != win2)
            return win2 ? 1 : -1;
        final boolean draw1 = wdl1 == 0;
        final boolean draw2 = wdl2 == 0;
        if (draw1 != draw2)
            return draw2 ? 1 : -1;
        final int score1 = this.score;
        final int score2 = other.score;
        if (win1) {
            final boolean dtm1 = type1 == Type.DTM;
            final boolean dtm2 = type2 == Type.DTM;
            if (dtm1 != dtm2)
                return dtm2 ? 1 : -1;
            if (dtm1)
                return -compareScore(wdl1, score1, wdl2, score2);
            final boolean dtz1 = type1 == Type.DTZ;
            final boolean dtz2 = type2 == Type.DTZ;
            if (dtz1 != dtz2)
                return dtz2 ? 1 : -1;
            return -compareScore(wdl1, score1, wdl2, score2);
        } else if (draw1) {
            return 0;
        } else {
            final boolean wdlType1 = type1 == Type.WDL;
            final boolean wdlType2 = type2 == Type.WDL;
            if (wdlType1 != wdlType2)
                return wdlType2 ? 1 : -1;
            if (wdlType1)
                return -compareScore(wdl1, score1, wdl2, score2);
            final boolean dtzType1 = type1 == Type.DTZ;
            final boolean dtzType2 = type2 == Type.DTZ;
            if (dtzType1 != dtzType2)
                return dtzType2 ? 1 : -1;
            return -compareScore(wdl1, score1, wdl2, score2);
        }
    }

    /** Return f((wdl1,score1)) - f((wdl2,score2)), where f(x) modifies
     * the score so that  larger values are better. */
    final static int compareScore(int wdl1, int score1,
                                  int wdl2, int score2) {
        final int M = 1000;
        if (wdl1 > 0)
            score1 = M - score1;
        else if (wdl1 < 0)
            score1 = -M + score1;

        if (wdl2 > 0)
            score2 = M - score2;
        else if (wdl2 < 0)
            score2 = -M + score2;
        return score1 - score2;
    }
}
