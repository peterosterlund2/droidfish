/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2014  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.engine;

import java.util.Map;

import org.petero.droidfish.EngineOptions;

public interface UCIEngine {

    /** For reporting engine error messages. */
    public interface Report {
        /** Report error message to GUI. */
        void reportError(String errMsg);
    }

    /** Start engine. */
    public void initialize();

    /** Initialize default options. */
    public void initOptions(EngineOptions engineOptions);

    /** Read UCI options from .ini file and send them to the engine. */
    public void applyIniFile();

    /** Set engine UCI options. */
    public void setUCIOptions(Map<String,String> uciOptions);

    /** Get engine UCI options. */
    public UCIOptions getUCIOptions();

    /** Return true if engine options have correct values.
     * If false is returned, engine will be restarted. */
    public boolean optionsOk(EngineOptions engineOptions);

    /** Shut down engine. */
    public void shutDown();

    /**
     * Read a line from the engine.
     * @param timeoutMillis Maximum time to wait for data.
     * @return The line, without terminating newline characters,
     *         or empty string if no data available,
     *         or null if I/O error.
     */
    public String readLineFromEngine(int timeoutMillis);

    // FIXME!! Writes should be handled by separate thread.
    /** Write a line to the engine. \n will be added automatically. */
    public void writeLineToEngine(String data);

    /** Set the engine strength, allowed values 0 - 1000. */
    public void setStrength(int strength);

    /** Set an engine integer option. */
    public void setOption(String name, int value);

    /** Set an engine boolean option. */
    public void setOption(String name, boolean value);

    /** Set an engine option. If the option is not a string option,
     * value is converted to the correct type.
     * @return True if the option was changed. */
    public boolean setOption(String name, String value);

    /** Clear list of supported options. */
    public void clearOptions();

    /** Register an option as supported by the engine.
     * @param tokens  The UCI option line sent by the engine, split in words. */
    public UCIOptions.OptionBase registerOption(String[] tokens);

    /** Set number of search threads to use. */
    public void setNThreads(int nThreads);
}
