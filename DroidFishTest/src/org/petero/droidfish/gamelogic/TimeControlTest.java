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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.petero.droidfish.gamelogic.TimeControlData.TimeControlField;

import junit.framework.TestCase;


public class TimeControlTest extends TestCase {
    public TimeControlTest() {
    }

    public void testElapsedTime() {
        TimeControl tc = new TimeControl();
        int totTime = 5 * 60 * 1000;
        long t0 = 1000;
        TimeControlData tcData = new TimeControlData();
        tcData.setTimeControl(totTime, 0, 0);
        tc.setTimeControl(tcData);
        tc.setCurrentMove(1, true, totTime, totTime);
        assertEquals(0, tc.getMovesToTC(true));
        assertEquals(0, tc.getMovesToTC(false));
        assertEquals(0, tc.getIncrement(true));
        assertEquals(0, tc.getIncrement(false));
        assertEquals(totTime, tc.getRemainingTime(true, 0));
        tc.startTimer(t0);
        int remain = tc.moveMade(t0 + 1000, true);
        assertEquals(totTime - 1000, remain);

        tc.setCurrentMove(2, true, totTime - 1000, totTime);
        assertEquals(0, tc.getMovesToTC(true));
        assertEquals(0, tc.getMovesToTC(false));
        assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(totTime, tc.getRemainingTime(false, t0 + 4711));

        tc.setCurrentMove(1, false, totTime - 1000, totTime);
        assertEquals(0, tc.getMovesToTC(true));
        assertEquals(0, tc.getMovesToTC(false));
        assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(totTime, tc.getRemainingTime(false, t0 + 4711));

        tc.startTimer(t0 + 3000);
        assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 5000));
        assertEquals(totTime - 2000, tc.getRemainingTime(false, t0 + 5000));
        tc.stopTimer(t0 + 8000);
        assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(totTime - 5000, tc.getRemainingTime(false, t0 + 4711));
        remain = tc.moveMade(t0 + 8000, true);
        assertEquals(totTime - 5000, remain);
        tc.setCurrentMove(2, true, totTime - 1000, totTime - 5000);
        assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(totTime - 5000, tc.getRemainingTime(false, t0 + 4711));
    }

    /** Test getMovesToTC */
    public void testTimeControl() {
        TimeControl tc = new TimeControl();
        TimeControlData tcData = new TimeControlData();
        tcData.setTimeControl(2 * 60 * 1000, 40, 0);
        tc.setTimeControl(tcData);
        tc.setCurrentMove(1, true, 0, 0);
        assertEquals(40, tc.getMovesToTC(true));
        assertEquals(40, tc.getMovesToTC(false));
        tc.setCurrentMove(1, false, 0, 0);
        assertEquals(39, tc.getMovesToTC(true));
        assertEquals(40, tc.getMovesToTC(false));

        tc.setCurrentMove(2, true, 0, 0);
        assertEquals(39, tc.getMovesToTC(true));
        assertEquals(39, tc.getMovesToTC(false));

        tc.setCurrentMove(40, true, 0, 0);
        assertEquals(1, tc.getMovesToTC(true));
        assertEquals(1, tc.getMovesToTC(false));

        tc.setCurrentMove(40, false, 0, 0);
        assertEquals(40, tc.getMovesToTC(true));
        assertEquals(1, tc.getMovesToTC(false));

        tc.setCurrentMove(41, true, 0, 0);
        assertEquals(40, tc.getMovesToTC(true));
        assertEquals(40, tc.getMovesToTC(false));

        tc.setCurrentMove(80, true, 0, 0);
        assertEquals(1, tc.getMovesToTC(true));
        assertEquals(1, tc.getMovesToTC(false));

        tc.setCurrentMove(80, false, 0, 0);
        assertEquals(40, tc.getMovesToTC(true));
        assertEquals(1, tc.getMovesToTC(false));

        tc.setCurrentMove(81, true, 0, 0);
        assertEquals(40, tc.getMovesToTC(true));
        assertEquals(40, tc.getMovesToTC(false));
    }

    private TimeControlField tcf(int time, int moves, int inc) {
        return new TimeControlField(time, moves, inc);
    }

    /** Test multiple time controls. */
    public void testMultiTimeControl() {
        TimeControl tc = new TimeControl();
        TimeControlData tcData = new TimeControlData();
        tcData.tcW = new ArrayList<TimeControlField>();
        tcData.tcW.add(tcf(120*60*1000, 40, 0));
        tcData.tcW.add(tcf(60*60*1000, 20, 0));
        tcData.tcW.add(tcf(30*60*1000, 0, 15*1000));
        tcData.tcB = new ArrayList<TimeControlField>();
        tcData.tcB.add(tcf(5*60*1000, 60, 1000));
        tc.setTimeControl(tcData);

        assertEquals(40, tc.getMovesToTC(true));
        assertEquals(60, tc.getMovesToTC(false));
        assertEquals(0, tc.getIncrement(true));
        assertEquals(1000, tc.getIncrement(false));

        tc.setCurrentMove(40, true, 0, 0);
        assertEquals(1, tc.getMovesToTC(true));
        assertEquals(21, tc.getMovesToTC(false));
        assertEquals(0, tc.getIncrement(true));
        assertEquals(1000, tc.getIncrement(false));

        tc.setCurrentMove(40, false, 0, 0);
        assertEquals(20, tc.getMovesToTC(true));
        assertEquals(21, tc.getMovesToTC(false));
        assertEquals(0, tc.getIncrement(true));
        assertEquals(1000, tc.getIncrement(false));

        tc.setCurrentMove(60, true, 0, 0);
        assertEquals(1, tc.getMovesToTC(true));
        assertEquals(1, tc.getMovesToTC(false));
        assertEquals(0, tc.getIncrement(true));
        assertEquals(1000, tc.getIncrement(false));

        tc.setCurrentMove(61, true, 0, 0);
        assertEquals(0, tc.getMovesToTC(true));
        assertEquals(60, tc.getMovesToTC(false));
        assertEquals(15000, tc.getIncrement(true));
        assertEquals(1000, tc.getIncrement(false));


        int wBaseTime = 60*1000;
        int bBaseTime = 50*1000;
        tc.setCurrentMove(30, true, wBaseTime, bBaseTime);
        tc.startTimer(1500);
        wBaseTime = tc.moveMade(1500 + 3000, true);
        assertEquals(60*1000-3000, wBaseTime);
        tc.setCurrentMove(30, false, wBaseTime, bBaseTime);
        assertEquals(60*1000-3000, tc.getRemainingTime(true, 1500 + 3000));
        assertEquals(50*1000, tc.getRemainingTime(false, 1500 + 3000));

        tc.startTimer(5000);
        bBaseTime = tc.moveMade(9000, true);
        assertEquals(50000 - 4000 + 1000, bBaseTime);
        tc.setCurrentMove(31, true, wBaseTime, bBaseTime);
        assertEquals(60*1000-3000, tc.getRemainingTime(true, 9000));
        assertEquals(50000 - 4000 + 1000, tc.getRemainingTime(false, 9000));
    }

    public void testExtraTime() {
        TimeControl tc = new TimeControl();
        final int timeCont = 60 * 1000;
        int wBaseTime = timeCont;
        int bBaseTime = timeCont;
        final int inc = 700;
        TimeControlData tcData = new TimeControlData();
        tcData.setTimeControl(timeCont, 5, inc);
        tc.setTimeControl(tcData);
        tc.setCurrentMove(5, true, wBaseTime, bBaseTime);
        int t0 = 1342134;
        assertEquals(timeCont, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(timeCont, tc.getRemainingTime(false, t0 + 4711));

        tc.startTimer(t0 + 1000);
        wBaseTime = tc.moveMade(t0 + 2000, true);
        tc.setCurrentMove(5, false, wBaseTime, bBaseTime);
        assertEquals(timeCont - 1000 + timeCont + inc, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(timeCont, tc.getRemainingTime(false, t0 + 4711));

        tc.startTimer(t0 + 2000);
        bBaseTime = tc.moveMade(t0 + 6000, true);
        tc.setCurrentMove(6, true, wBaseTime, bBaseTime);
        assertEquals(timeCont - 1000 + timeCont + inc, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(timeCont - 4000 + timeCont + inc, tc.getRemainingTime(false, t0 + 4711));

        tc.startTimer(t0 + 6000);
        wBaseTime = tc.moveMade(t0 + 9000, true);
        tc.setCurrentMove(6, false, wBaseTime, bBaseTime);
        assertEquals(timeCont - 1000 + timeCont + inc - 3000 + inc, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(timeCont - 4000 + timeCont + inc, tc.getRemainingTime(false, t0 + 4711));

        // No increment when move made in paused mode, ie analysis mode
        tc.startTimer(t0 + 9000);
        bBaseTime = tc.moveMade(t0 + 10000, false);
        tc.setCurrentMove(7, true, wBaseTime, bBaseTime);
        assertEquals(timeCont - 1000 + timeCont + inc - 3000 + inc, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(timeCont - 4000 + timeCont + inc - 1000, tc.getRemainingTime(false, t0 + 4711));

        // No extra time when passing time control in analysis mode
        tcData.setTimeControl(timeCont, 1, inc);
        tc.setTimeControl(tcData);
        wBaseTime = bBaseTime = timeCont;
        tc.setCurrentMove(1, true, wBaseTime, bBaseTime);
        tc.startTimer(t0 + 1000);
        wBaseTime = tc.moveMade(t0 + 3000, false);
        tc.setCurrentMove(1, false, wBaseTime, bBaseTime);
        assertEquals(timeCont - 2000 + (timeCont + inc)*0, tc.getRemainingTime(true, t0 + 4711));
        assertEquals(timeCont, tc.getRemainingTime(false, t0 + 4711));
    }

    public void testSerialize() throws IOException {
        TimeControl tc = new TimeControl();
        TimeControlData tcData = new TimeControlData();
        tcData.tcW = new ArrayList<TimeControlField>();
        tcData.tcW.add(tcf(120*60*1000, 40, 0));
        tcData.tcW.add(tcf(60*60*1000, 20, 0));
        tcData.tcW.add(tcf(30*60*1000, 0, 15*1000));
        tcData.tcB = new ArrayList<TimeControlField>();
        tcData.tcB.add(tcf(5*60*1000, 60, 1000));
        tc.setTimeControl(tcData);

        byte[] serialState = null;
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
            DataOutputStream dos = new DataOutputStream(baos);
            tc.writeToStream(dos);
            dos.flush();
            serialState = baos.toByteArray();
            dos.close();
            baos.close();
        }
        TimeControl tc2 = new TimeControl();
        {
            ByteArrayInputStream bais = new ByteArrayInputStream(serialState);
            DataInputStream dis = new DataInputStream(bais);
            tc2.readFromStream(dis, 3);
            dis.close();
            bais.close();
        }
        assertEquals(tcData, tc2.tcData);
    }
}
