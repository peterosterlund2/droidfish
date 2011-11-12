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

package org.petero.droidfish.engine.cuckoochess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

/** Simple PrintStream look-alike on top of nio. */
class NioPrintStream {
    Pipe.SinkChannel out;

    public NioPrintStream(Pipe pipe) {
        out = pipe.sink();
    }

    public void printf(String format) {
        try {
            String s = String.format(format, new Object[]{});
            out.write(ByteBuffer.wrap(s.getBytes()));
        } catch (IOException e) {
        }
    }

    public void printf(String format, Object ... args) {
        try {
            String s = String.format(format, args);
            out.write(ByteBuffer.wrap(s.getBytes()));
        } catch (IOException e) {
        }
    }
}
