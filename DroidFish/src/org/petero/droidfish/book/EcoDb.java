/*
    DroidFish - An Android chess program.
    Copyright (C) 2016 Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.book;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.GameTree;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.UndoInfo;

/** ECO code database. */
@SuppressLint("UseSparseArrays")
public class EcoDb {
    private static EcoDb instance;

    /** Get singleton instance. */
    public static EcoDb getInstance(Context context) {
        if (instance == null) {
            instance = new EcoDb(context);
        }
        return instance;
    }

    /** Get ECO classification for a given tree node. */
    public String getEco(GameTree gt, GameTree.Node node) {
        ArrayList<GameTree.Node> gtNodePath = new ArrayList<GameTree.Node>();
        int nodeIdx = -1;
        while (node != null) {
            nodeIdx = findNode(node);
            if (nodeIdx != -1)
                break;
            if (node == gt.rootNode) {
                Short idx = posHashToNodeIdx.get(gt.startPos.zobristHash());
                if (idx != null) {
                    nodeIdx = idx;
                    break;
                }
            }
            gtNodePath.add(node);
            node = node.getParent();
        }
        if (nodeIdx != -1) {
            Node ecoNode = readNode(nodeIdx);
            boolean childFound = true;
            for (int i = gtNodePath.size() - 1; i >= 0; i--) {
                GameTree.Node gtNode = gtNodePath.get(i);
                int m = gtNode.move.getCompressedMove();
                int child = childFound ? ecoNode.firstChild : -1;
                while (child != -1) {
                    Node cNode = readNode(child);
                    if (cNode.move == m)
                        break;
                    child = cNode.nextSibling;
                }
                if (child != -1) {
                    nodeIdx = child;
                    ecoNode = readNode(nodeIdx);
                } else
                    childFound = false;
                cacheNode(gtNode, nodeIdx);
            }
        }

        if (nodeIdx != -1) {
            Node n = readNode(nodeIdx);
            if (n.nameIdx >= 0)
                return ecoNames[n.nameIdx];
        }
        return "";
    }


    private static class Node {
        int move;       // Move (compressed) leading to the position corresponding to this node
        int nameIdx;    // Index in names array, or -1
        int firstChild;
        int nextSibling;
    }

    private byte[] nodesBuffer;
    private String[] ecoNames;
    private HashMap<Long, Short> posHashToNodeIdx;
    private WeakLRUCache<GameTree.Node, Integer> gtNodeToIdx;

    /** Return cached Node index corresponding to a GameTree.Node, or -1 if not found. */
    private int findNode(GameTree.Node node) {
        Integer idx = gtNodeToIdx.get(node);
        return idx == null ? -1 : idx;
    }

    /** Store GameTree.Node to Node index in cache. */
    private void cacheNode(GameTree.Node node, int nodeIdx) {
        gtNodeToIdx.put(node, nodeIdx);
    }

    /** Constructor. */
    private EcoDb(Context context) {
        posHashToNodeIdx = new HashMap<Long, Short>();
        gtNodeToIdx = new WeakLRUCache<GameTree.Node, Integer>(50);
        try {
            ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
            InputStream inStream = context.getAssets().open("eco.dat");
            if (inStream == null)
                throw new IOException("Can't read ECO database");
            byte[] buf = new byte[1024];
            while (true) {
                int len = inStream.read(buf);
                if (len <= 0) break;
                bufStream.write(buf, 0, len);
            }
            inStream.close();
            bufStream.flush();
            buf = bufStream.toByteArray();
            int nNodes = 0;
            while (true) {
                Node n = readNode(nNodes, buf);
                if (n.move == 0xffff)
                    break;
                nNodes++;
            }
            nodesBuffer = new byte[nNodes * 8];
            System.arraycopy(buf, 0, nodesBuffer, 0, nNodes * 8);

            ArrayList<String> names = new ArrayList<String>();
            int idx = (nNodes + 1) * 8;
            int start = idx;
            for (int i = idx; i < buf.length; i++) {
                if (buf[i] == 0) {
                    names.add(new String(buf, start, i - start, "UTF-8"));
                    start = i + 1;
                }
            }
            ecoNames = names.toArray(new String[names.size()]);
        } catch (IOException ex) {
            throw new RuntimeException("Can't read ECO database");
        }
        try {
            if (nodesBuffer.length > 0) {
                Position pos = TextIO.readFEN(TextIO.startPosFEN);
                populateCache(pos, 0);
            }
        } catch (ChessParseError e) {
        }
    }

    /** Initialize popHashToNodeIdx. */
    private void populateCache(Position pos, int nodeIdx) {
        if (posHashToNodeIdx.get(pos.zobristHash()) == null)
            posHashToNodeIdx.put(pos.zobristHash(), (short)nodeIdx);
        Node node = readNode(nodeIdx);
        int child = node.firstChild;
        UndoInfo ui = new UndoInfo();
        while (child != -1) {
            node = readNode(child);
            Move m = Move.fromCompressed(node.move);
            pos.makeMove(m, ui);
            populateCache(pos, child);
            pos.unMakeMove(m, ui);
            child = node.nextSibling;
        }
    }

    private Node readNode(int index) {
        return readNode(index, nodesBuffer);
    }

    private static Node readNode(int index, byte[] buf) {
        Node n = new Node();
        int o = index * 8;
        n.move = getU16(buf, o);
        n.nameIdx = getS16(buf, o + 2);
        n.firstChild = getS16(buf, o + 4);
        n.nextSibling = getS16(buf, o + 6);
        return n;
    }

    private static int getU16(byte[] buf, int offs) {
        int b0 = buf[offs] & 255;
        int b1 = buf[offs + 1] & 255;
        return (b0 << 8) + b1;
    }

    private static int getS16(byte[] buf, int offs) {
        int ret = getU16(buf, offs);
        if (ret >= 0x8000)
            ret -= 0x10000;
        return ret;
    }

    /** A Cache where the keys are weak references and the cache automatically
     *  shrinks when it becomes too large, using approximate LRU ordering.
     *  This cache is not designed to store null values. */
    private static class WeakLRUCache<K, V> {
        private WeakHashMap<K, V> mapNew; // Most recently used entries
        private WeakHashMap<K, V> mapOld; // Older entries
        private int maxSize;

        public WeakLRUCache(int maxSize) {
            mapNew = new WeakHashMap<K, V>();
            mapOld = new WeakHashMap<K, V>();
            this.maxSize = maxSize;
        }

        /** Insert a value in the map, replacing any old value with the same key. */
        public void put(K key, V val) {
            if (mapNew.containsKey(key)) {
                mapNew.put(key, val);
            } else {
                if (mapOld.containsKey(key))
                    mapOld.remove(key);
                insertNew(key, val);
            }
        }

        /** Returns the value corresponding to key, or null if not found. */
        public V get(K key) {
            V val = mapNew.get(key);
            if (val != null)
                return val;
            val = mapOld.get(key);
            if (val != null) {
                mapOld.remove(key);
                insertNew(key, val);
            }
            return val;
        }

        private void insertNew(K key, V val) {
            if (mapNew.size() >= maxSize) {
                WeakHashMap<K, V> tmp = mapNew;
                mapNew = mapOld;
                mapOld = tmp;
                mapNew.clear();
            }
            mapNew.put(key, val);
        }
    }
}
