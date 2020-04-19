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
    void initialize();

    /** Initialize default options. */
    void initOptions(EngineOptions engineOptions);

    /** Read UCI options from .ini file and send them to the engine. */
    void applyIniFile();

    /** Set engine UCI options. */
    boolean setUCIOptions(Map<String,String> uciOptions);

    /** Save non-default UCI option values to file. */
    void saveIniFile(UCIOptions options);

    /** Get engine UCI options. */
    UCIOptions getUCIOptions();

    /** Return true if engine options have correct values.
     * If false is returned, engine will be restarted. */
    boolean optionsOk(EngineOptions engineOptions);

    /** Shut down engine. */
    void shutDown();

    /**
     * Read a line from the engine.
     * @param timeoutMillis Maximum time to wait for data.
     * @return The line, without terminating newline characters,
     *         or empty string if no data available,
     *         or null if I/O error.
     */
    String readLineFromEngine(int timeoutMillis);

    /** Write a line to the engine. \n will be added automatically. */
    void writeLineToEngine(String data);

    /** Temporarily set the engine Elo strength to use for the next search.
     *  Integer.MAX_VALUE means full strength. */
    void setEloStrength(int elo);

    /** Set an engine integer option. */
    void setOption(String name, int value);

    /** Set an engine boolean option. */
    void setOption(String name, boolean value);

    /** Set an engine option. If the option is not a string option,
     * value is converted to the correct type.
     * @return True if the option was changed. */
    boolean setOption(String name, String value);

    /** Clear list of supported options. */
    void clearOptions();

    /** Register an option as supported by the engine.
     * @param tokens  The UCI option line sent by the engine, split in words. */
    UCIOptions.OptionBase registerOption(String[] tokens);
}
