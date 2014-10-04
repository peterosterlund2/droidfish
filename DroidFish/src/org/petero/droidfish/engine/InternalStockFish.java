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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;

/** Stockfish engine running as process, started from assets resource. */
public class InternalStockFish extends ExternalEngine {

    public InternalStockFish(Context context, Report report) {
        super(context, "", report);
    }

    @Override
    protected File getOptionsFile() {
        File extDir = Environment.getExternalStorageDirectory();
        return new File(extDir, "/DroidFish/uci/stockfish.ini");
    }

    @Override
    protected boolean configurableOption(String name) {
        name = name.toLowerCase(Locale.US);
        if (!super.configurableOption(name))
            return false;
        if (name.equals("skill level") || name.equals("write debug log") ||
            name.equals("write search log") || name.equals("search log filename") ||
            name.equals("book file") || name.equals("best book move") ||
            name.equals("ownbook"))
            return false;
        return true;
    }

    /** @inheritDoc */
    @Override
    public final void setStrength(int strength) {
        setOption("Skill Level", strength/50);
    }

    private final long readCheckSum(File f) {
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(is);
            return dis.readLong();
        } catch (IOException e) {
            return 0;
        } finally {
            if (is != null) try { is.close(); } catch (IOException ex) {}
        }
    }

    private final void writeCheckSum(File f, long checkSum) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(f);
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeLong(checkSum);
        } catch (IOException e) {
        } finally {
            if (os != null) try { os.close(); } catch (IOException ex) {}
        }
    }

    private final long computeAssetsCheckSum(String sfExe) {
        InputStream is = null;
        try {
            is = context.getAssets().open(sfExe);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[8192];
            while (true) {
                int len = is.read(buf);
                if (len <= 0)
                    break;
                md.update(buf, 0, len);
            }
            byte[] digest = md.digest(new byte[]{0});
            long ret = 0;
            for (int i = 0; i < 8; i++) {
                ret ^= ((long)digest[i]) << (i * 8);
            }
            return ret;
        } catch (IOException e) {
            return -1;
        } catch (NoSuchAlgorithmException e) {
            return -1;
        } finally {
            if (is != null) try { is.close(); } catch (IOException ex) {}
        }
    }

    @Override
    protected String copyFile(File from, File exeDir) throws IOException {
        File to = new File(exeDir, "engine.exe");
        final String sfExe = EngineUtil.internalStockFishName();

        // The checksum test is to avoid writing to /data unless necessary,
        // on the assumption that it will reduce memory wear.
        long oldCSum = readCheckSum(new File(internalSFPath()));
        long newCSum = computeAssetsCheckSum(sfExe);
        if (oldCSum == newCSum)
            return to.getAbsolutePath();

        if (to.exists())
            to.delete();
        to.createNewFile();

        InputStream is = context.getAssets().open(sfExe);
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

        writeCheckSum(new File(internalSFPath()), newCSum);
        return to.getAbsolutePath();
    }
}
