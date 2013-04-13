/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

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

import java.util.ArrayList;

/** Keep track of time control information for both players. */
public class TimeControl {
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

    private ArrayList<TimeControlField> tcW;
    private ArrayList<TimeControlField> tcB;

    private long whiteBaseTime; // Current remaining time, or remaining time when clock started
    private long blackBaseTime; // Current remaining time, or remaining time when clock started

    int currentMove;
    boolean whiteToMove;

    private long elapsed; // Accumulated elapsed time for this move.
    private long timerT0; // Time when timer started. 0 if timer is stopped.


    /** Constructor. Sets time control to "game in 5min". */
    public TimeControl() {
        setTimeControl(5 * 60 * 1000, 0, 0);
        reset();
    }

    public final void reset() {
        currentMove = 1;
        whiteToMove = true;
        elapsed = 0;
        timerT0 = 0;
    }

    /** Set time control to "moves" moves in "time" milliseconds, + inc milliseconds per move. */
    public final void setTimeControl(long time, int moves, long inc) {
        tcW = new ArrayList<TimeControlField>();
        tcW.add(new TimeControlField(time, moves, inc));
        tcB = new ArrayList<TimeControlField>();
        tcB.add(new TimeControlField(time, moves, inc));
    }

    /** Set time controls for white and black players. */
    public final void setTimeControl(ArrayList<TimeControlField> whiteTC,
                                     ArrayList<TimeControlField> blackTC) {
        tcW = whiteTC;
        tcB = blackTC;
    }

    public final void setCurrentMove(int move, boolean whiteToMove, long whiteBaseTime, long blackBaseTime) {
        currentMove = move;
        this.whiteToMove = whiteToMove;
        this.whiteBaseTime = whiteBaseTime;
        this.blackBaseTime = blackBaseTime;
        timerT0 = 0;
        elapsed = 0;
    }

    public final boolean clockRunning() {
        return timerT0 != 0;
    }

    public final void startTimer(long now) {
        if (!clockRunning()) {
            timerT0 = now;
        }
    }

    public final void stopTimer(long now) {
        if (clockRunning()) {
            long timerT1 = now;
            long currElapsed = timerT1 - timerT0;
            timerT0 = 0;
            if (currElapsed > 0) {
                elapsed += currElapsed;
            }
        }
    }

    /** Compute new remaining time after a move is made. */
    public final int moveMade(long now, boolean useIncrement) {
        stopTimer(now);

        ArrayList<TimeControlField> tc = whiteToMove ? tcW : tcB;
        Pair<Integer,Integer> tcInfo = getCurrentTC(whiteToMove);
        int tcIdx = tcInfo.first;
        int movesToTc = tcInfo.second;

        long remaining = getRemainingTime(whiteToMove, now);
        if (useIncrement) {
            remaining += tc.get(tcIdx).increment;
            if (movesToTc == 1) {
                if (tcIdx+1 < tc.size())
                    tcIdx++;
                remaining += tc.get(tcIdx).timeControl;
            }
        }
        elapsed = 0;
        return (int)remaining;
    }

    /** Get remaining time */
    public final int getRemainingTime(boolean whiteToMove, long now) {
        long remaining = whiteToMove ? whiteBaseTime : blackBaseTime;
        if (whiteToMove == this.whiteToMove) {
            remaining -= elapsed;
            if (timerT0 != 0) {
                remaining -= now - timerT0;
            }
        }
        return (int)remaining;
    }

    /** Get initial thinking time in milliseconds. */
    public final int getInitialTime(boolean whiteMove) {
        ArrayList<TimeControlField> tc = whiteMove ? tcW : tcB;
        return (int)tc.get(0).timeControl;
    }

    /** Get time increment in milliseconds after playing next move. */
    public final int getIncrement(boolean whiteMove) {
        ArrayList<TimeControlField> tc = whiteMove ? tcW : tcB;
        int tcIdx = getCurrentTC(whiteMove).first;
        return (int)tc.get(tcIdx).increment;
    }

    /** Return number of moves to the next time control, or 0 if "sudden death". */
    public final int getMovesToTC(boolean whiteMove) {
        return getCurrentTC(whiteMove).second;
    }

    /** Return the current active time control index and number of moves to next time control. */
    private Pair<Integer,Integer> getCurrentTC(boolean whiteMove) {
        ArrayList<TimeControlField> tc = whiteMove ? tcW : tcB;
        int tcIdx = 0;
        final int lastTcIdx = tc.size() - 1;
        int nextTC = 1;
        int currMove = currentMove;
        if (!whiteToMove && whiteMove)
            currMove++;
        while (true) {
            if (tc.get(tcIdx).movesPerSession <= 0)
                return new Pair<Integer,Integer>(tcIdx, 0);
            nextTC += tc.get(tcIdx).movesPerSession;
            if (nextTC > currMove)
                break;
            if (tcIdx < lastTcIdx)
                tcIdx++;
        }
        return new Pair<Integer,Integer>(tcIdx, nextTC - currMove);
    }
}
