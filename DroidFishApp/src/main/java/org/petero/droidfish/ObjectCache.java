/*
    DroidFish - An Android chess program.
    Copyright (C) 2017  Peter Österlund, peterosterlund2@gmail.com

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import android.content.Context;

/**
 * Stores large objects temporarily in the file system to avoid
 * too large transactions when communicating between activities.
 * The cache has a limited size, so trying to retrieve a stored
 * object can fail in which case null is returned. */
public class ObjectCache {
    public final static int MAX_MEM_SIZE = 16384; // Max size of object to store in memory
    public final static int MAX_CACHED_OBJS = 10; // Max no of objects to cache in file system
    private final Context context;

    public ObjectCache() {
        this(DroidFishApp.getContext());
    }

    public ObjectCache(Context context) {
        this.context = context;
    }

    /** Store a string in the cache and return a token that can be
     *  used to retrieve the original string. */
    public String storeString(String s) {
        if (s.length() < MAX_MEM_SIZE) {
            return "0" + s;
        } else {
            long token = storeInCache(s.getBytes());
            return "1" + Long.toString(token);
        }
    }

    /** Retrieve a string from the cache using a token previously
     *  returned by storeString(). 
     *  @return The string, or null if not found in the cache. */
    public String retrieveString(String token) {
        if (token.startsWith("0")) {
            return token.substring(1);
        } else {
            String tokStr = token.substring(1);
            long longTok = Long.valueOf(tokStr);
            byte[] buf = retrieveFromCache(longTok);
            return buf == null ? null : new String(buf);
        }
    }

    /** Store a byte array in the cache and return a token that can be
     *  used to retrieve the original byte array. */
    public byte[] storeBytes(byte[] b) {
        if (b.length < MAX_MEM_SIZE) {
            byte[] ret = new byte[b.length + 1];
            ret[0] = 0;
            System.arraycopy(b, 0, ret, 1, b.length);
            return ret;
        } else {
            long token = storeInCache(b);
            byte[] tokBuf = Long.toString(token).getBytes();
            byte[] ret = new byte[1 + tokBuf.length];
            ret[0] = 1;
            System.arraycopy(tokBuf, 0, ret, 1, tokBuf.length);
            return ret;
        }
    }

    /** Retrieve a byte array from the cache using a token previously
     *  returned by storeBytes().
     *  @return The byte array, or null if not found in the cache. */
    public byte[] retrieveBytes(byte[] token) {
        if (token[0] == 0) {
            byte[] ret = new byte[token.length - 1];
            System.arraycopy(token, 1, ret, 0, token.length - 1);
            return ret;
        } else {
            String tokStr = new String(token, 1, token.length - 1);
            long longTok = Long.valueOf(tokStr);
            return retrieveFromCache(longTok);
        }
    }

    private final static String cacheDir = "objcache";
    
    private long storeInCache(byte[] b) {
        File cd = context.getCacheDir();
        File dir = new File(cd, cacheDir);
        if (dir.exists() || dir.mkdir()) {
            try {
                File[] files = dir.listFiles();
                if (files != null) {
                    long[] tokens = new long[files.length];
                    long token = -1;
                    for (int i = 0; i < files.length; i++) {
                        try {
                            tokens[i] = Long.valueOf(files[i].getName());
                            token = Math.max(token, tokens[i]);
                        } catch (NumberFormatException nfe) {
                        }
                    }
                    Arrays.sort(tokens);
                    for (int i = 0; i < files.length - (MAX_CACHED_OBJS - 1); i++) {
                        File f = new File(dir, String.valueOf(tokens[i]));
                        f.delete();
                    }
                    int maxTries = 10;
                    for (int i = 0; i < maxTries; i++) {
                        token++;
                        File f = new File(dir, String.valueOf(token));
                        if (f.createNewFile()) {
                            FileOutputStream fos = new FileOutputStream(f);
                            try {
                                fos.write(b);
                                return token;
                            } finally {
                                fos.close();
                            }
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
        return -1;
    }

    private byte[] retrieveFromCache(long token) {
        File cd = context.getCacheDir();
        File dir = new File(cd, cacheDir);
        if (dir.exists()) {
            File f = new File(dir, String.valueOf(token));
            try {
                RandomAccessFile raf = new RandomAccessFile(f, "r");
                try {
                    int len = (int)raf.length();
                    byte[] buf = new byte[len];
                    int offs = 0;
                    while (offs < len) {
                        int l = raf.read(buf, offs, len - offs);
                        if (l <= 0)
                            return null;
                        offs += l;
                    }
                    return buf;
                } finally {
                    raf.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }
}
