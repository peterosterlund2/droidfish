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

package org.petero.droidfish;

/** Settings controlling PGN import/export */
public class PGNOptions {
    public static class Viewer {
        public boolean variations;
        public boolean comments;
        public boolean nag;
        public boolean headers;
    }
    public static class Import {
        public boolean variations;
        public boolean comments;
        public boolean nag;
    }
    public static class Export {
        public boolean variations;
        public boolean comments;
        public boolean nag;
        public boolean playerAction;
        public boolean clockInfo;
        public boolean pgnPromotions;
        public boolean moveNrAfterNag;
    }

    public Viewer view;
    public Import imp;
    public Export exp;

    public PGNOptions() {
        view = new Viewer();
        imp = new Import();
        exp = new Export();
        exp.moveNrAfterNag = true;
    }
}
