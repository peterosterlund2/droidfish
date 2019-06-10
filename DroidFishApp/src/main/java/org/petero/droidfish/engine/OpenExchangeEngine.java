/*
    DroidFish - An Android chess program.
    Copyright (C) 2014,2016  Peter Österlund, peterosterlund2@gmail.com

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

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

/** Engine imported from a different android app, resolved using the open exchange format. */
public class OpenExchangeEngine extends ExternalEngine {

    public OpenExchangeEngine(String engine, String workDir, Report report) {
        super(engine, workDir, report);
    }

    @Override
    protected String copyFile(File from, File exeDir) throws IOException {
        new File(internalSFPath()).delete();
        ChessEngineResolver resolver = new ChessEngineResolver(context);
        List<ChessEngine> engines = resolver.resolveEngines();
        for (ChessEngine engine : engines) {
            if (EngineUtil.openExchangeFileName(engine).equals(from.getName())) {
                File engineFile = engine.copyToFiles(context.getContentResolver(), exeDir);
                return engineFile.getAbsolutePath();
            }
        }
        throw new IOException("Engine not found");
    }
}
