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

package org.petero.droidfish.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.os.Build;

/** Stockfish engine running as process, started from assets resource. */
public class InternalStockFish extends ExternalEngine {

    public InternalStockFish(Context context, Report report) {
        super(context, "", report);
    }

    /** @inheritDoc */
    @Override
    public final void initOptions() {
        setOption("Hash", 16);
    }

    /** @inheritDoc */
    @Override
    public final void setStrength(int strength) {
        setOption("Skill Level", strength/50);
    }

    @Override
    protected void copyFile(File from, File to) throws IOException {
        if (new File(intSfPath).exists())
            return;

        if (to.exists())
            to.delete();
        to.createNewFile();

        InputStream is = context.getAssets().open("stockfish-" + Build.CPU_ABI);
        OutputStream os = new FileOutputStream(to);

        try {
            byte[] buf = new byte[8192];
            while (true) {
                int len = is.read(buf);
                if (len <= 0)
                    break;
                os.write(buf, 0, len);
            }
        } finally {
            if (is != null) try { is.close(); } catch (IOException ex) {}
            if (os != null) try { os.close(); } catch (IOException ex) {}
        }

        new File(intSfPath).createNewFile();
    }
}
