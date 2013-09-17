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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.petero.droidfish.gamelogic.TimeControlData.TimeControlField;

/** Keep track of time control information for both players. */
public class TimeControl {
    TimeControlData tcData;

    private int whiteBaseTime; // Current remaining time, or remaining time when clock started
    private int blackBaseTime; // Current remaining time, or remaining time when clock started

    int currentMove;
    boolean whiteToMove;

    private int elapsed;  // Accumulated elapsed time for this move.
    private long timerT0; // Time when timer started. 0 if timer is stopped.


    /** Constructor. Sets time control to "game in 5min". */
    public TimeControl() {
        tcData = new TimeControlData();
        reset();
    }

    public final void reset() {
        currentMove = 1;
        whiteToMove = true;
        elapsed = 0;
        timerT0 = 0;
    }

    /** Set time controls for white and black players. */
    public final void setTimeControl(TimeControlData tcData) {
        this.tcData = tcData;
    }

    public final void setCurrentMove(int move, boolean whiteToMove, int whiteBaseTime, int blackBaseTime) {
        currentMove = move;
        this.whiteToMove = whiteToMove;
        this.whiteBaseTime = whiteBaseTime;
        this.blackBaseTime = blackBaseTime;
        timerT0 = 0;
        elapsed = 0;
    }

    /** Move current move "delta" half-moves forward. */
    public final void advanceMove(int delta) {
        while (delta > 0) {
            if (!whiteToMove)
                currentMove++;
            whiteToMove = !whiteToMove;
            delta--;
        }
        while (delta < 0) {
            whiteToMove = !whiteToMove;
            if (!whiteToMove)
                currentMove--;
            delta++;
        }
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
            int currElapsed = (int)(now - timerT0);
            timerT0 = 0;
            if (currElapsed > 0)
                elapsed += currElapsed;
        }
    }

    /** Compute new remaining time after a move is made. */
    public final int moveMade(long now, boolean useIncrement) {
        stopTimer(now);

        ArrayList<TimeControlField> tc = tcData.getTC(whiteToMove);
        Pair<Integer,Integer> tcInfo = getCurrentTC(whiteToMove);
        int tcIdx = tcInfo.first;
        int movesToTc = tcInfo.second;

        int remaining = getRemainingTime(whiteToMove, now);
        if (useIncrement) {
            remaining += tc.get(tcIdx).increment;
            if (movesToTc == 1) {
                if (tcIdx+1 < tc.size())
                    tcIdx++;
                remaining += tc.get(tcIdx).timeControl;
            }
        }
        elapsed = 0;
        return remaining;
    }

    /** Get remaining time */
    public final int getRemainingTime(boolean whiteToMove, long now) {
        int remaining = whiteToMove ? whiteBaseTime : blackBaseTime;
        if (whiteToMove == this.whiteToMove) {
            remaining -= elapsed;
            if (timerT0 != 0)
                remaining -= now - timerT0;
        }
        return remaining;
    }

    /** Get initial thinking time in milliseconds. */
    public final int getInitialTime(boolean whiteMove) {
        ArrayList<TimeControlField> tc = tcData.getTC(whiteMove);
        return tc.get(0).timeControl;
    }

    /** Get time increment in milliseconds after playing next move. */
    public final int getIncrement(boolean whiteMove) {
        ArrayList<TimeControlField> tc = tcData.getTC(whiteMove);
        int tcIdx = getCurrentTC(whiteMove).first;
        return tc.get(tcIdx).increment;
    }

    /** Return number of moves to the next time control, or 0 if "sudden death". */
    public final int getMovesToTC(boolean whiteMove) {
        return getCurrentTC(whiteMove).second;
    }

    /** @return Array containing time control, moves per session and time increment. */
    public int[] getTimeLimit(boolean whiteMove) {
        ArrayList<TimeControlField> tc = tcData.getTC(whiteMove);
        int tcIdx = getCurrentTC(whiteMove).first;
        TimeControlField t = tc.get(tcIdx);
        return new int[]{t.timeControl, t.movesPerSession, t.increment};
    }

    /** Return the current active time control index and number of moves to next time control. */
    private Pair<Integer,Integer> getCurrentTC(boolean whiteMove) {
        ArrayList<TimeControlField> tc = tcData.getTC(whiteMove);
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

    /** De-serialize from input stream. */
    public void readFromStream(DataInputStream dis, int version) throws IOException {
        tcData.readFromStream(dis, version);
    }

    /** Serialize to output stream. */
    public void writeToStream(DataOutputStream dos) throws IOException {
        tcData.writeToStream(dos);
    }
}
