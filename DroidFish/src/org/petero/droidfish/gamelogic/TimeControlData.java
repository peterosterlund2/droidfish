package org.petero.droidfish.gamelogic;

import java.util.ArrayList;

public final class TimeControlData {
    public static final class TimeControlField {
        long timeControl;
        int movesPerSession;
        long increment;

        public TimeControlField(long time, int moves, long inc) {
            timeControl = time;
            movesPerSession = moves;
            increment = inc;
        }
    }

    ArrayList<TimeControlField> tcW, tcB;

    TimeControlData() {
        tcW = new ArrayList<TimeControlField>();
        tcW.add(new TimeControlField(5*60*1000, 60, 0));
        tcB = new ArrayList<TimeControlField>();
        tcB.add(new TimeControlField(5*60*1000, 60, 0));
    }

    public final void setTimeControl(long time, int moves, long inc) {
        tcW = new ArrayList<TimeControlField>();
        tcW.add(new TimeControlField(time, moves, inc));
        tcB = new ArrayList<TimeControlField>();
        tcB.add(new TimeControlField(time, moves, inc));
    }

    /** Get time control data array for white or black player. */
    public ArrayList<TimeControlField> getTC(boolean whiteMove) {
        return whiteMove ? tcW : tcB;
    }
}
