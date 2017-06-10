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


import java.util.Arrays;

import junit.framework.TestCase;


public class ObjectCacheTest extends TestCase {
    public ObjectCacheTest() {
    }

    public void testCache() {
        ObjectCache cache = new ObjectCache(DroidFishApp.getContext());
        final int M = ObjectCache.MAX_MEM_SIZE;
        final int N = ObjectCache.MAX_CACHED_OBJS;
        { // Test small string
            String s0 = "testing";
            String token = cache.storeString(s0);
            String s = cache.retrieveString(token);
            assertEquals(s0, s);
        }
        { // Test small byte array
            byte[] b0 = {1,2,3,4,5};
            byte[] token = cache.storeBytes(b0);
            byte[] b = cache.retrieveBytes(token);
            assertTrue(Arrays.equals(b0, b));
        }
        { // Test large string
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < M + 1; i++)
                sb.append('a');
            String s0 = sb.toString();
            String token = cache.storeString(s0);
            String s = cache.retrieveString(token);
            assertEquals(s0, s);
        }
        { // Test large byte array
            byte[] b0 = new byte[M+1];
            for (int i = 0; i < M + 1; i++)
                b0[i] = 'a';
            byte[] token = cache.storeBytes(b0);
            byte[] b = cache.retrieveBytes(token);
            assertTrue(Arrays.equals(b0, b));
        }
        { // Test large string objects
            String[] s0 = new String[N];
            String[] tokens = new String[N];
            for (int i = 0; i < N; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < M + 1 + i * 100; j++)
                    sb.append((char)((i + j) % 255));
                s0[i] = sb.toString();
                tokens[i] = cache.storeString(s0[i]);
            }
            { // Small objects must not evict older entries
                for (int i = 0; i < 100; i++)
                    cache.storeString("abc");
                for (int i = 0; i < 100; i++)
                    cache.storeBytes(new byte[]{(byte)i,(byte)(i*2),(byte)(i+1)});
            }
            for (int i = 0; i < N; i++) {
                String s = cache.retrieveString(tokens[i]);
                assertEquals(s0[i], s);
            }
        }
        { // Test large byte arrays
            byte[][] b0 = new byte[N][];
            byte[][] tokens = new byte[N][];
            for (int i = 0; i < N; i++) {
                byte[] b = new byte[M + 1 + i * 100];
                for (int j = 0; j < b.length; j++)
                    b[j] = (byte)((i + j) % 255);
                b0[i] = b;
                tokens[i] = cache.storeBytes(b0[i]);
            }
            { // Small objects must not evict older entries
                for (int i = 0; i < 100; i++)
                    cache.storeString("abc");
                for (int i = 0; i < 100; i++)
                    cache.storeBytes(new byte[]{(byte)i,(byte)(i*2),(byte)(i+1)});
            }
            for (int i = 0; i < N; i++) {
                byte[] b = cache.retrieveBytes(tokens[i]);
                assertTrue(Arrays.equals(b0[i], b));
            }
        }

        { // Test that not too many file system objects are used
            String[] s0 = new String[N];
            String[] tokens = new String[N];
            for (int i = 0; i < N; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < M + 1 + i * 100; j++)
                    sb.append((char)((i + j) % 255));
                s0[i] = sb.toString();
                tokens[i] = cache.storeString(s0[i]);
            }
            {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < M + 1; j++)
                    sb.append((char)((j + 3) % 255));
                String s = sb.toString();
                String token = cache.storeString(s);
                String s1 = cache.retrieveString(token);
                assertEquals(s, s1);
            }
            assertEquals(null, cache.retrieveString(tokens[0]));
            for (int i = 1; i < N; i++) {
                String s = cache.retrieveString(tokens[i]);
                assertEquals(s0[i], s);
            }
        }
    }
}
