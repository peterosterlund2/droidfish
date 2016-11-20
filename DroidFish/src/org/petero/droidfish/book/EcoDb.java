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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.petero.droidfish.DroidFishApp;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.GameTree;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.UndoInfo;

/** ECO code database. */
@SuppressLint("UseSparseArrays")
public class EcoDb {
    private static EcoDb instance;

    /** Get singleton instance. */
    public static EcoDb getInstance() {
        if (instance == null) {
            instance = new EcoDb();
        }
        return instance;
    }

    /** Get ECO classification for a given tree node. Also returns distance in plies to "ECO tree". */
    public Pair<String,Integer> getEco(GameTree gt) {
        ArrayList<Integer> treePath = new ArrayList<Integer>(); // Path to restore gt to original node
        ArrayList<Pair<GameTree.Node,Boolean>> toCache = new ArrayList<Pair<GameTree.Node,Boolean>>();

        int nodeIdx = -1;
        int distToEcoTree = 0;

        // Find matching node furtherest from root in the ECO tree
        boolean checkForDup = true;
        while (true) {
            GameTree.Node node = gt.currentNode;
            CacheEntry e = findNode(node);
            if (e != null) {
                nodeIdx = e.nodeIdx;
                distToEcoTree = e.distToEcoTree;
                checkForDup = false;
                break;
            }
            Short idx = posHashToNodeIdx.get(gt.currentPos.zobristHash());
            boolean inEcoTree = idx != null;
            toCache.add(new Pair<GameTree.Node,Boolean>(node, inEcoTree));

            if (idx != null) {
                Node ecoNode = readNode(idx);
                if (ecoNode.nameIdx != -1) {
                    nodeIdx = idx;
                    break;
                }
            }

            if (node == gt.rootNode)
                break;

            treePath.add(node.getChildNo());
            gt.goBack();
        }

        // Handle duplicates in ECO tree (same position reachable from more than one path)
        if (nodeIdx != -1 && checkForDup && gt.startPos.zobristHash() == startPosHash) {
            ArrayList<Short> dups = posHashToNodeIdx2.get(gt.currentPos.zobristHash());
            if (dups != null) {
                while (gt.currentNode != gt.rootNode) {
                    treePath.add(gt.currentNode.getChildNo());
                    gt.goBack();
                }

                int currEcoNode = 0;
                boolean foundDup = false;
                while (!treePath.isEmpty()) {
                    gt.goForward(treePath.get(treePath.size() - 1));
                    treePath.remove(treePath.size() - 1);
                    int m = gt.currentNode.move.getCompressedMove();

                    Node ecoNode = readNode(currEcoNode);
                    boolean foundChild = false;
                    int child = ecoNode.firstChild;
                    while (child != -1) {
                        ecoNode = readNode(child);
                        if (ecoNode.move == m) {
                            foundChild = true;
                            break;
                        }
                        child = ecoNode.nextSibling;
                    }
                    if (!foundChild)
                        break;
                    currEcoNode = child;
                    for (Short dup : dups) {
                        if (dup == currEcoNode) {
                            nodeIdx = currEcoNode;
                            foundDup = true;
                            break;
                        }
                    }
                    if (foundDup)
                        break;
                }
            }
        }

        for (int i = treePath.size() - 1; i >= 0; i--)
            gt.goForward(treePath.get(i));
        for (int i = toCache.size() - 1; i >= 0; i--) {
            Pair<GameTree.Node,Boolean> p = toCache.get(i);
            distToEcoTree++;
            if (p.second)
                distToEcoTree = 0;
            cacheNode(p.first, nodeIdx, distToEcoTree);
        }

        if (nodeIdx != -1) {
            Node n = readNode(nodeIdx);
            if (n.nameIdx >= 0)
                return new Pair<String, Integer>(ecoNames[n.nameIdx], distToEcoTree);
        }
        return new Pair<String, Integer>("", 0);
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
    private HashMap<Long, ArrayList<Short>> posHashToNodeIdx2; // Handles collisions
    private final long startPosHash; // Zobrist hash for standard starting position

    private static class CacheEntry {
        final int nodeIdx;
        final int distToEcoTree;
        CacheEntry(int n, int d) {
            nodeIdx = n;
            distToEcoTree = d;
        }
    }
    private WeakLRUCache<GameTree.Node, CacheEntry> gtNodeToIdx;

    /** Return cached Node index corresponding to a GameTree.Node, or -1 if not found. */
    private CacheEntry findNode(GameTree.Node node) {
        return gtNodeToIdx.get(node);
    }

    /** Store GameTree.Node to Node index in cache. */
    private void cacheNode(GameTree.Node node, int nodeIdx, int distToEcoTree) {
        gtNodeToIdx.put(node, new CacheEntry(nodeIdx, distToEcoTree));
    }

    /** Constructor. */
    private EcoDb() {
        posHashToNodeIdx = new HashMap<Long, Short>();
        posHashToNodeIdx2 = new HashMap<Long, ArrayList<Short>>();
        gtNodeToIdx = new WeakLRUCache<GameTree.Node, CacheEntry>(50);
        try {
            ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
            InputStream inStream = DroidFishApp.getContext().getAssets().open("eco.dat");
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
            Position pos = TextIO.readFEN(TextIO.startPosFEN);
            startPosHash = pos.zobristHash();
            if (nodesBuffer.length > 0) {
                populateCache(pos, 0);
            }
        } catch (ChessParseError e) {
            throw new RuntimeException("Internal error");
        }
    }

    /** Initialize posHashToNodeIdx. */
    private void populateCache(Position pos, int nodeIdx) {
        Node node = readNode(nodeIdx);
        long hash = pos.zobristHash();
        if (posHashToNodeIdx.get(hash) == null) {
            posHashToNodeIdx.put(hash, (short)nodeIdx);
        } else if (node.nameIdx != -1) {
            ArrayList<Short> lst = null;
            if (posHashToNodeIdx2.get(hash) == null) {
                lst = new ArrayList<Short>();
                posHashToNodeIdx2.put(hash, lst);
            } else {
                lst = posHashToNodeIdx2.get(hash);
            }
            lst.add((short)nodeIdx);
        }
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
