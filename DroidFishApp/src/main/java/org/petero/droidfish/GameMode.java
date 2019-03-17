/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2012  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish;

public class GameMode {
    private final int modeNr;

    public static final int PLAYER_WHITE  = 1;
    public static final int PLAYER_BLACK  = 2;
    public static final int TWO_PLAYERS   = 3;
    public static final int ANALYSIS      = 4;
    public static final int TWO_COMPUTERS = 5;
    public static final int EDIT_GAME     = 6;

    public GameMode(int modeNr) {
        this.modeNr = modeNr;
    }

    public int getModeNr() {
        return modeNr;
    }

    /** Return true if white side is controlled by a human. */
    public final boolean playerWhite() {
        switch (modeNr) {
        case PLAYER_WHITE:
        case TWO_PLAYERS:
        case ANALYSIS:
        case EDIT_GAME:
            return true;
        default:
            return false;
        }
    }

    /** Return true if black side is controlled by a human. */
    public final boolean playerBlack() {
        switch (modeNr) {
        case PLAYER_BLACK:
        case TWO_PLAYERS:
        case ANALYSIS:
        case EDIT_GAME:
            return true;
        default:
            return false;
        }
    }

    public final boolean analysisMode() {
        return modeNr == ANALYSIS;
    }

    /** Return true if it is a humans turn to move. */
    public final boolean humansTurn(boolean whiteMove) {
        return whiteMove ? playerWhite() : playerBlack();
    }

    /** Return true if the clocks are running. */
    public final boolean clocksActive() {
        switch (modeNr) {
        case PLAYER_WHITE:
        case PLAYER_BLACK:
        case TWO_PLAYERS:
        case TWO_COMPUTERS:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        GameMode other = (GameMode)o;
        return modeNr == other.modeNr;
    }

    @Override
    public int hashCode() {
        return modeNr;
    }
}
