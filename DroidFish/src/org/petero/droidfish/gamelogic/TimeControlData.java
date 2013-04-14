package org.petero.droidfish.gamelogic;

import java.util.ArrayList;

public final class TimeControlData {
    public static final class TimeControlField {
        long timeControl;      // Time in milliseconds
        int movesPerSession;
        long increment;        // Increment in milliseconds

        public TimeControlField(long time, int moves, long inc) {
            timeControl = time;
            movesPerSession = moves;
            increment = inc;
        }
    }

    ArrayList<TimeControlField> tcW, tcB;

    /** Constructor. Set a default time control. */
    public TimeControlData() {
        tcW = new ArrayList<TimeControlField>();
        tcW.add(new TimeControlField(5*60*1000, 60, 0));
        tcB = new ArrayList<TimeControlField>();
        tcB.add(new TimeControlField(5*60*1000, 60, 0));
    }

    /** Set a single time control for both white and black. */
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

    /** Return true if white and black time controls are equal. */
    public boolean isSymmetric() {
        return arrayEquals(tcW, tcB);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TimeControlData))
            return false;
        TimeControlData tc2 = (TimeControlData)o;
        return arrayEquals(tcW, tc2.tcW) && arrayEquals(tcB, tc2.tcB);
    }

    private static boolean arrayEquals(ArrayList<TimeControlField> a1,
                                     ArrayList<TimeControlField> a2) {
        if (a1.size() != a2.size())
            return false;
        for (int i = 0; i < a1.size(); i++) {
            TimeControlField f1 = a1.get(i);
            TimeControlField f2 = a2.get(i);
            if ((f1.timeControl != f2.timeControl) ||
                (f1.movesPerSession != f2.movesPerSession) ||
                (f1.increment != f2.increment))
                return false;
        }
        return true;
    }
}
