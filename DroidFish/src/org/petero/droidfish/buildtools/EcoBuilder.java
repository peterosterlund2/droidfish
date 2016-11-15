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

package org.petero.droidfish.buildtools;


import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.petero.droidfish.FileUtil;
import org.petero.droidfish.PGNOptions;
import org.petero.droidfish.gamelogic.Game;
import org.petero.droidfish.gamelogic.GameTree;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.TimeControlData;

/** Build the ECO data file from eco.pgn. */
public class EcoBuilder {
    public static void main(String[] args) throws Throwable {
        String ecoPgnFile = args[0];
        String ecoDatFile = args[1];
        (new EcoBuilder()).createECOFile(ecoPgnFile, ecoDatFile);
    }

    private static class Node {
        int index;   // Index in nodes array
        Move move;   // Move leading to the position corresponding to this node
        int nameIdx; // Index in names array, or -1
        ArrayList<Node> children = new ArrayList<Node>();
        Node parent;
    }
    private ArrayList<Node> nodes;
    private ArrayList<String> names;
    private HashMap<String, Integer> nameToIndex;

    /** Constructor. */
    private EcoBuilder() {
        nodes = new ArrayList<Node>();
        names = new ArrayList<String>();
        nameToIndex = new HashMap<String, Integer>();
        Node rootNode = new Node();
        rootNode.index = 0;
        rootNode.move = new Move(0, 0, 0);
        rootNode.nameIdx = -1;
        nodes.add(rootNode);
    }

    /** Read pgn text file, write binary file. */
    private void createECOFile(String ecoPgnFile, String ecoDatFile) throws Throwable {
        String[] ecoPgn = FileUtil.readFile(ecoPgnFile);
        StringBuilder pgn = new StringBuilder();
        boolean gotMoves = false;
        for (String line : ecoPgn) {
            boolean isHeader = line.startsWith("[");
            if (gotMoves && isHeader) {
                readGame(pgn.toString());
                pgn = new StringBuilder();
                gotMoves = false;
            }
            pgn.append(line);
            pgn.append('\n');
            gotMoves |= !isHeader;
        }
        readGame(pgn.toString());

        writeDataFile(ecoDatFile);
    }

    /** Read and process one game. */
    private void readGame(String pgn) throws Throwable {
        if (pgn.isEmpty())
            return;
        Game game = new Game(null, new TimeControlData());
        PGNOptions options = new PGNOptions();
        game.readPGN(pgn, options);

        // Determine name of opening
        HashMap<String,String> headers = new HashMap<String,String>();
        GameTree tree = game.tree;
        tree.getHeaders(headers);
        String eco = headers.get("ECO");
        String opening = headers.get("Opening");
        String variation = headers.get("Variation");
        String name = eco + ": " + opening;
        if (variation != null)
            name = name + ", " + variation;

        // Add name to data structures
        Integer nameIdx = nameToIndex.get(name);
        if (nameIdx == null) {
            nameIdx = nameToIndex.size();
            nameToIndex.put(name, nameIdx);
            names.add(name);
        }

        // Add corresponding moves to data structures
        Node parent = nodes.get(0);
        while (true) {
            ArrayList<Move> moves = tree.variations();
            if (moves.isEmpty()) {
                parent.nameIdx = nameIdx;
                break;
            }
            Move m = moves.get(0);
            tree.goForward(0);
            int oldIdx = -1; 
            for (int i = 0; i < parent.children.size(); i++) {
                if (parent.children.get(i).move.equals(m)) {
                    oldIdx = i;
                    break;
                }
            }
            if (oldIdx == -1) {
                Node node = new Node();
                node.index = nodes.size();
                node.move = m;
                node.nameIdx = -1;
                node.parent = parent;
                nodes.add(node);
                parent.children.add(node);
                parent = node;
            } else {
                parent = parent.children.get(oldIdx);
            }
        }
    }

    /** Write the binary ECO code data file. */
    private void writeDataFile(String ecoDatFile) throws Throwable {
        FileOutputStream out = new FileOutputStream(ecoDatFile);

        // Write nodes
        byte[] buf = new byte[8];
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            int cm = n.move == null ? 0 : n.move.getCompressedMove();
            buf[0] = (byte)(cm >> 8);             // Move, high byte
            buf[1] = (byte)(cm & 255);            // Move, low byte
            buf[2] = (byte)(n.nameIdx >> 8);      // Index, high byte
            buf[3] = (byte)(n.nameIdx & 255);     // Index, low byte
            int firstChild = -1;
            if (n.children.size() > 0)
                firstChild = n.children.get(0).index;
            buf[4] = (byte)(firstChild >> 8);
            buf[5] = (byte)(firstChild & 255);
            int nextSibling = -1;
            if (n.parent != null) {
                ArrayList<Node> siblings = n.parent.children;
                for (int j = 0; j < siblings.size()-1; j++) {
                    if (siblings.get(j).move.equals(n.move)) {
                        nextSibling = siblings.get(j+1).index;
                        break;
                    }
                }
            }
            buf[6] = (byte)(nextSibling >> 8);
            buf[7] = (byte)(nextSibling & 255);
            out.write(buf);
        }
        for (int i = 0; i < buf.length; i++)
            buf[i] = -1;
        out.write(buf);

        // Write names
        buf = new byte[]{0};
        for (String name : names) {
            out.write(name.getBytes("UTF-8"));
            out.write(buf);
        }

        out.close();
    }
}
