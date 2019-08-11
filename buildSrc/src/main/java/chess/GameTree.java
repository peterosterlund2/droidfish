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

package chess;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GameTree {
    // Data from the seven tag roster (STR) part of the PGN standard
    String event, site, date, round, white, black, result;

    public Position startPos;

    // Non-standard tags
    static private final class TagPair {
        String tagName;
        String tagValue;
    }
    private List<TagPair> tagPairs;

    public Node rootNode;
    public Node currentNode;
    public Position currentPos;    // Cached value. Computable from "currentNode".

    /** Creates an empty GameTree starting at the standard start position. */
    public GameTree() {
        try {
            setStartPos(TextIO.readFEN(TextIO.startPosFEN));
        } catch (ChessParseError e) {
        }
    }

    /** Set start position. Drops the whole game tree. */
    final void setStartPos(Position pos) {
        event = "?";
        site = "?";
        date = "????.??.??";
        round = "?";
        white = "?";
        black = "?";
        startPos = pos;
        tagPairs = new ArrayList<>();
        rootNode = new Node();
        currentNode = rootNode;
        currentPos = new Position(startPos);
    }

    final static private class PgnScanner {
        String data;
        int idx;
        List<PgnToken> savedTokens;

        PgnScanner(String pgn) {
            savedTokens = new ArrayList<>();
            // Skip "escape" lines, ie lines starting with a '%' character
            StringBuilder sb = new StringBuilder();
            int len = pgn.length();
            boolean col0 = true;
            for (int i = 0; i < len; i++) {
                char c = pgn.charAt(i);
                if (c == '%' && col0) {
                    while (i + 1 < len) {
                        char nextChar = pgn.charAt(i + 1);
                        if ((nextChar == '\n') || (nextChar == '\r'))
                            break;
                        i++;
                    }
                    col0 = true;
                } else {
                    sb.append(c);
                    col0 = ((c == '\n') || (c == '\r'));
                }
            }
            sb.append('\n'); // Terminating whitespace simplifies the tokenizer
            data = sb.toString();
            idx = 0;
        }

        final void putBack(PgnToken tok) {
            savedTokens.add(tok);
        }

        final PgnToken nextToken() {
            if (savedTokens.size() > 0) {
                int len = savedTokens.size();
                PgnToken ret = savedTokens.get(len - 1);
                savedTokens.remove(len - 1);
                return ret;
            }

            PgnToken ret = new PgnToken(PgnToken.EOF, null);
            try {
                while (true) {
                    char c = data.charAt(idx++);
                    if (Character.isWhitespace(c) || c == '\u00a0') {
                        // Skip
                    } else if (c == '.') {
                        ret.type = PgnToken.PERIOD;
                        break;
                    } else if (c == '*') {
                        ret.type = PgnToken.ASTERISK;
                        break;
                    } else if (c == '[') {
                        ret.type = PgnToken.LEFT_BRACKET;
                        break;
                    } else if (c == ']') {
                        ret.type = PgnToken.RIGHT_BRACKET;
                        break;
                    } else if (c == '(') {
                        ret.type = PgnToken.LEFT_PAREN;
                        break;
                    } else if (c == ')') {
                        ret.type = PgnToken.RIGHT_PAREN;
                        break;
                    } else if (c == '{') {
                        ret.type = PgnToken.COMMENT;
                        StringBuilder sb = new StringBuilder();
                        while ((c = data.charAt(idx++)) != '}') {
                            sb.append(c);
                        }
                        ret.token = sb.toString();
                        break;
                    } else if (c == ';') {
                        ret.type = PgnToken.COMMENT;
                        StringBuilder sb = new StringBuilder();
                        while (true) {
                            c = data.charAt(idx++);
                            if ((c == '\n') || (c == '\r'))
                                break;
                            sb.append(c);
                        }
                        ret.token = sb.toString();
                        break;
                    } else if (c == '"') {
                        ret.type = PgnToken.STRING;
                        StringBuilder sb = new StringBuilder();
                        while (true) {
                            c = data.charAt(idx++);
                            if (c == '"') {
                                break;
                            } else if (c == '\\') {
                                c = data.charAt(idx++);
                            }
                            sb.append(c);
                        }
                        ret.token = sb.toString();
                        break;
                    } else if (c == '$') {
                        ret.type = PgnToken.NAG;
                        StringBuilder sb = new StringBuilder();
                        while (true) {
                            c = data.charAt(idx++);
                            if (!Character.isDigit(c)) {
                                idx--;
                                break;
                            }
                            sb.append(c);
                        }
                        ret.token = sb.toString();
                        break;
                    } else { // Start of symbol or integer
                        ret.type = PgnToken.SYMBOL;
                        StringBuilder sb = new StringBuilder();
                        sb.append(c);
                        boolean onlyDigits = Character.isDigit(c);
                        final String term = ".*[](){;\"$";
                        while (true) {
                            c = data.charAt(idx++);
                            if (Character.isWhitespace(c) || (term.indexOf(c) >= 0)) {
                                idx--;
                                break;
                            }
                            sb.append(c);
                            if (!Character.isDigit(c))
                                onlyDigits = false;
                        }
                        if (onlyDigits) {
                            ret.type = PgnToken.INTEGER;
                        }
                        ret.token = sb.toString();
                        break;
                    }
                }
            } catch (StringIndexOutOfBoundsException e) {
                ret.type = PgnToken.EOF;
            }
            return ret;
        }

        final PgnToken nextTokenDropComments() {
            while (true) {
                PgnToken tok = nextToken();
                if (tok.type != PgnToken.COMMENT)
                    return tok;
            }
        }
    }

    /** Import PGN data. */
    public final boolean readPGN(String pgn) throws ChessParseError {
        PgnScanner scanner = new PgnScanner(pgn);
        PgnToken tok = scanner.nextToken();

        // Parse tag section
        List<TagPair> tagPairs = new ArrayList<>();
        while (tok.type == PgnToken.LEFT_BRACKET) {
            TagPair tp = new TagPair();
            tok = scanner.nextTokenDropComments();
            if (tok.type != PgnToken.SYMBOL)
                break;
            tp.tagName = tok.token;
            tok = scanner.nextTokenDropComments();
            if (tok.type != PgnToken.STRING)
                break;
            tp.tagValue = tok.token;
            tok = scanner.nextTokenDropComments();
            if (tok.type != PgnToken.RIGHT_BRACKET) {
                // In a well-formed PGN, there is nothing between the string
                // and the right bracket, but broken headers with non-escaped
                // " characters sometimes occur. Try to do something useful
                // for such headers here.
                PgnToken prevTok = new PgnToken(PgnToken.STRING, "");
                while ((tok.type == PgnToken.STRING) || (tok.type == PgnToken.SYMBOL)) {
                    if (tok.type != prevTok.type)
                        tp.tagValue += '"';
                    if ((tok.type == PgnToken.SYMBOL) && (prevTok.type == PgnToken.SYMBOL))
                        tp.tagValue += ' ';
                    tp.tagValue += tok.token;
                    prevTok = tok;
                    tok = scanner.nextTokenDropComments();
                }
            }
            tagPairs.add(tp);
            tok = scanner.nextToken();
        }
        scanner.putBack(tok);

        // Parse move section
        Node gameRoot = new Node();
        Node.parsePgn(scanner, gameRoot);

        if (tagPairs.size() == 0) {
            gameRoot.verifyChildren(TextIO.readFEN(TextIO.startPosFEN));
            if (gameRoot.children.size() == 0)
                return false;
        }

        // Store parsed data in GameTree
        String fen = TextIO.startPosFEN;
        int nTags = tagPairs.size();
        for (int i = 0; i < nTags; i++) {
            if (tagPairs.get(i).tagName.equals("FEN")) {
                fen = tagPairs.get(i).tagValue;
            }
        }
        setStartPos(TextIO.readFEN(fen));

        result = "";
        for (int i = 0; i < nTags; i++) {
            String name = tagPairs.get(i).tagName;
            String val = tagPairs.get(i).tagValue;
            if (name.equals("FEN") || name.equals("SetUp")) {
                // Already handled
            } else if (name.equals("Event")) {
                event = val;
            } else if (name.equals("Site")) {
                site = val;
            } else if (name.equals("Date")) {
                date = val;
            } else if (name.equals("Round")) {
                round = val;
            } else if (name.equals("White")) {
                white = val;
            } else if (name.equals("Black")) {
                black = val;
            } else if (name.equals("Result")) {
                result = val;
            } else {
                this.tagPairs.add(tagPairs.get(i));
            }
        }

        rootNode = gameRoot;
        currentNode = rootNode;

        return true;
    }

    /** Go backward in game tree. */
    public final void goBack() {
        if (currentNode.parent != null) {
            currentPos.unMakeMove(currentNode.move, currentNode.ui);
            currentNode = currentNode.parent;
        }
    }

    /** Go forward in game tree.
     * @param variation Which variation to follow. -1 to follow default variation.
     */
    public final void goForward(int variation) {
        currentNode.verifyChildren(currentPos);
        if (variation < 0)
            variation = currentNode.defaultChild;
        int numChildren = currentNode.children.size();
        if (variation >= numChildren)
            variation = 0;
        currentNode.defaultChild = variation;
        if (numChildren > 0) {
            currentNode = currentNode.children.get(variation);
            currentPos.makeMove(currentNode.move, currentNode.ui);
            TextIO.fixupEPSquare(currentPos);
        }
    }

    /** List of possible continuation moves. */
    public final ArrayList<Move> variations() {
        currentNode.verifyChildren(currentPos);
        ArrayList<Move> ret = new ArrayList<>();
        for (Node child : currentNode.children)
            ret.add(child.move);
        return ret;
    }

    /**
     *  A node object represents a position in the game tree.
     *  The position is defined by the move that leads to the position from the parent position.
     *  The root node is special in that it doesn't have a move.
     */
    private static class Node {
        String moveStr;             // String representation of move leading to this node. Empty string in root node.
        public Move move;           // Computed on demand for better PGN parsing performance.
                                    // Subtrees of invalid moves will be dropped when detected.
                                    // Always valid for current node.
        private UndoInfo ui;        // Computed when move is computed

        int nag;                    // Numeric annotation glyph
        String preComment;          // Comment before move
        String postComment;         // Comment after move

        private Node parent;        // Null if root node
        int defaultChild;
        private ArrayList<Node> children;

        public Node() {
            this.moveStr = "";
            this.move = null;
            this.ui = null;
            this.parent = null;
            this.children = new ArrayList<>();
            this.defaultChild = 0;
            this.nag = 0;
            this.preComment = "";
            this.postComment = "";
        }

        public Node getParent() {
            return parent;
        }

        /** nodePos must represent the same position as this Node object. */
        private boolean verifyChildren(Position nodePos) {
            return verifyChildren(nodePos, null);
        }
        private boolean verifyChildren(Position nodePos, ArrayList<Move> moves) {
            boolean anyToRemove = false;
            for (Node child : children) {
                if (child.move == null) {
                    if (moves == null)
                        moves = MoveGen.instance.legalMoves(nodePos);
                    Move move = TextIO.stringToMove(nodePos, child.moveStr, moves);
                    if (move != null) {
                        child.moveStr = TextIO.moveToString(nodePos, move, false, moves);
                        child.move = move;
                        child.ui = new UndoInfo();
                    } else {
                        anyToRemove = true;
                    }
                }
            }
            if (anyToRemove) {
                ArrayList<Node> validChildren = new ArrayList<>();
                for (Node child : children)
                    if (child.move != null)
                        validChildren.add(child);
                children = validChildren;
            }
            return anyToRemove;
        }

        final ArrayList<Integer> getPathFromRoot() {
            ArrayList<Integer> ret = new ArrayList<>(64);
            Node node = this;
            while (node.parent != null) {
                ret.add(node.getChildNo());
                node = node.parent;
            }
            Collections.reverse(ret);
            return ret;
        }

        /** Return this node's position in the parent node child list. */
        public final int getChildNo() {
            Node p = parent;
            for (int i = 0; i < p.children.size(); i++)
                if (p.children.get(i) == this)
                    return i;
            throw new RuntimeException();
        }

        private Node addChild(Node child) {
            child.parent = this;
            children.add(child);
            return child;
        }

        public static void parsePgn(PgnScanner scanner, Node node) {
            Node nodeToAdd = new Node();
            boolean moveAdded = false;
            while (true) {
                PgnToken tok = scanner.nextToken();
                switch (tok.type) {
                case PgnToken.INTEGER:
                case PgnToken.PERIOD:
                    break;
                case PgnToken.LEFT_PAREN:
                    if (moveAdded) {
                        node = node.addChild(nodeToAdd);
                        nodeToAdd = new Node();
                        moveAdded = false;
                    }
                    if (node.parent != null) {
                        parsePgn(scanner, node.parent);
                    } else {
                        int nestLevel = 1;
                        while (nestLevel > 0) {
                            switch (scanner.nextToken().type) {
                            case PgnToken.LEFT_PAREN: nestLevel++; break;
                            case PgnToken.RIGHT_PAREN: nestLevel--; break;
                            case PgnToken.EOF: return; // Broken PGN file. Just give up.
                            }
                        }
                    }
                    break;
                case PgnToken.NAG:
                    if (moveAdded) { // NAG must be after move
                        try {
                            nodeToAdd.nag = Integer.parseInt(tok.token);
                        } catch (NumberFormatException e) {
                            nodeToAdd.nag = 0;
                        }
                    }
                    break;
                case PgnToken.SYMBOL:
                    if (tok.token.equals("1-0") || tok.token.equals("0-1") || tok.token.equals("1/2-1/2")) {
                        if (moveAdded) node.addChild(nodeToAdd);
                        return;
                    }
                    char lastChar = tok.token.charAt(tok.token.length() - 1);
                    if (lastChar == '+')
                        tok.token = tok.token.substring(0, tok.token.length() - 1);
                    if ((lastChar == '!') || (lastChar == '?')) {
                        int movLen = tok.token.length() - 1;
                        while (movLen > 0) {
                            char c = tok.token.charAt(movLen - 1);
                            if ((c == '!') || (c == '?'))
                                movLen--;
                            else
                                break;
                        }
                        String ann = tok.token.substring(movLen);
                        tok.token = tok.token.substring(0, movLen);
                        int nag = 0;
                        if      (ann.equals("!"))  nag = 1;
                        else if (ann.equals("?"))  nag = 2;
                        else if (ann.equals("!!")) nag = 3;
                        else if (ann.equals("??")) nag = 4;
                        else if (ann.equals("!?")) nag = 5;
                        else if (ann.equals("?!")) nag = 6;
                        if (nag > 0)
                            scanner.putBack(new PgnToken(PgnToken.NAG, Integer.valueOf(nag).toString()));
                    }
                    if (tok.token.length() > 0) {
                        if (moveAdded) {
                            node = node.addChild(nodeToAdd);
                            nodeToAdd = new Node();
                            moveAdded = false;
                        }
                        nodeToAdd.moveStr = tok.token;
                        moveAdded = true;
                    }
                    break;
                case PgnToken.COMMENT:
                    if (moveAdded)
                        nodeToAdd.postComment += tok.token;
                    else
                        nodeToAdd.preComment += tok.token;
                    break;
                case PgnToken.ASTERISK:
                case PgnToken.LEFT_BRACKET:
                case PgnToken.RIGHT_BRACKET:
                case PgnToken.STRING:
                case PgnToken.RIGHT_PAREN:
                case PgnToken.EOF:
                    if (moveAdded) node.addChild(nodeToAdd);
                    return;
                }
            }
        }
    }

    /** Get PGN header tags and values. */
    public void getHeaders(Map<String,String> headers) {
        headers.put("Event", event);
        headers.put("Site",  site);
        headers.put("Date",  date);
        headers.put("Round", round);
        headers.put("White", white);
        headers.put("Black", black);
        headers.put("Result", result);
        for (int i = 0; i < tagPairs.size(); i++) {
            TagPair tp = tagPairs.get(i);
            headers.put(tp.tagName, tp.tagValue);
        }
    }
}
