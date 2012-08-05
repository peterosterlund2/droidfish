/*
    DroidFish - An Android chess program.
    Copyright (C) 2012  Peter Österlund, peterosterlund2@gmail.com

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

/** Engine options, including endgame tablebase probing options. */
public final class EngineOptions {
    public int hashMB;          // Engine hash table size in MB
    public boolean hints;       // Hints when playing/analyzing
    public boolean hintsEdit;   // Hints in "edit board" mode
    public boolean rootProbe;   // Only search optimal moves at root
    public boolean engineProbe; // Let engine use EGTB
    public String gtbPath;      // GTB directory path
    public String networkEngine;// Host:port for network engine

    public EngineOptions() {
        hashMB = 16;
        hints = false;
        hintsEdit = false;
        rootProbe = false;
        engineProbe = false;
        gtbPath = "";
        networkEngine = "";
    }

    public EngineOptions(EngineOptions other) {
        hashMB = other.hashMB;
        hints = other.hints;
        hintsEdit = other.hintsEdit;
        rootProbe = other.rootProbe;
        engineProbe = other.engineProbe;
        gtbPath = other.gtbPath;
        networkEngine = other.networkEngine;
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        EngineOptions other = (EngineOptions)o;

        return ((hashMB == other.hashMB) &&
                (hints == other.hints) &&
                (hintsEdit == other.hintsEdit) &&
                (rootProbe == other.rootProbe) &&
                (engineProbe == other.engineProbe) &&
                gtbPath.equals(other.gtbPath) &&
                networkEngine.equals(other.networkEngine));
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
