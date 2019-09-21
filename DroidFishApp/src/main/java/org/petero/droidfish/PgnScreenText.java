/*
    DroidFish - An Android chess program.
    Copyright (C) 2019  Peter Österlund, peterosterlund2@gmail.com

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

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;

import org.petero.droidfish.gamelogic.GameTree.Node;
import org.petero.droidfish.gamelogic.PgnToken;
import org.petero.droidfish.view.MoveListView;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** PngTokenReceiver implementation that renders PGN data for screen display. */
class PgnScreenText implements PgnToken.PgnTokenReceiver,
                               MoveListView.OnLinkClickListener {
    private SpannableStringBuilder sb = new SpannableStringBuilder();
    private TreeMap<Integer, Node> offs2Node = new TreeMap<>();
    private int prevType = PgnToken.EOF;
    private int nestLevel = 0;
    private boolean col0 = true;
    private final static int indentStep = 15;
    private int currPos = 0, endPos = 0;
    private boolean upToDate = false;
    private PGNOptions options;
    private DroidFish df;

    private static class NodeInfo {
        int l0, l1;
        NodeInfo(int ls, int le) {
            l0 = ls;
            l1 = le;
        }
    }
    private HashMap<Node, NodeInfo> nodeToCharPos = new HashMap<>();

    PgnScreenText(DroidFish df, PGNOptions options) {
        this.df = df;
        this.options = options;
    }

    public final CharSequence getText() {
        return sb;
    }

    public final int getCurrPos() {
        return currPos;
    }

    @Override
    public boolean isUpToDate() {
        return upToDate;
    }

    private int paraStart = 0;
    private int paraIndent = 0;
    private boolean paraBold = false;

    private void newLine() {
        newLine(false);
    }

    private void newLine(boolean eof) {
        if (!col0) {
            if (paraIndent > 0) {
                int paraEnd = sb.length();
                int indent = paraIndent * indentStep;
                sb.setSpan(new LeadingMarginSpan.Standard(indent), paraStart, paraEnd,
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (paraBold) {
                int paraEnd = sb.length();
                sb.setSpan(new StyleSpan(Typeface.BOLD), paraStart, paraEnd,
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (!eof)
                sb.append('\n');
            paraStart = sb.length();
            paraIndent = nestLevel;
            paraBold = false;
        }
        col0 = true;
    }

    private boolean pendingNewLine = false;

    private void addMoveLink(Node node, int l0, int l1) {
        offs2Node.put(l0, node);
        offs2Node.put(l1, null);
    }

    @Override
    public boolean onLinkClick(int offs) {
        Map.Entry<Integer, Node> e = offs2Node.floorEntry(offs);
        if (e == null)
            return false;
        Node node = e.getValue();
        if (node == null && e.getKey() == offs) {
            e = offs2Node.lowerEntry(e.getKey());
            if (e != null)
                node = e.getValue();
        }
        if (node == null)
            return false;

        df.goNode(node);
        return true;
    }

    @Override
    public void processToken(Node node, int type, String token) {
        if (    (prevType == PgnToken.RIGHT_BRACKET) &&
                (type != PgnToken.LEFT_BRACKET))  {
            if (options.view.headers) {
                col0 = false;
                newLine();
            } else {
                sb.clear();
                paraBold = false;
            }
        }
        if (pendingNewLine) {
            if (type != PgnToken.RIGHT_PAREN) {
                newLine();
                pendingNewLine = false;
            }
        }
        switch (type) {
        case PgnToken.STRING:
            sb.append(" \"");
            sb.append(token);
            sb.append('"');
            break;
        case PgnToken.INTEGER:
            if (    (prevType != PgnToken.LEFT_PAREN) &&
                    (prevType != PgnToken.RIGHT_BRACKET) && !col0)
                sb.append(' ');
            sb.append(token);
            col0 = false;
            break;
        case PgnToken.PERIOD:
            sb.append('.');
            col0 = false;
            break;
        case PgnToken.ASTERISK:      sb.append(" *");  col0 = false; break;
        case PgnToken.LEFT_BRACKET:  sb.append('[');   col0 = false; break;
        case PgnToken.RIGHT_BRACKET: sb.append("]\n"); col0 = false; break;
        case PgnToken.LEFT_PAREN:
            nestLevel++;
            if (col0)
                paraIndent++;
            newLine();
            sb.append('(');
            col0 = false;
            break;
        case PgnToken.RIGHT_PAREN:
            sb.append(')');
            nestLevel--;
            pendingNewLine = true;
            break;
        case PgnToken.NAG:
            sb.append(Node.nagStr(Integer.parseInt(token)));
            col0 = false;
            break;
        case PgnToken.SYMBOL: {
            if ((prevType != PgnToken.RIGHT_BRACKET) && (prevType != PgnToken.LEFT_BRACKET) && !col0)
                sb.append(' ');
            int l0 = sb.length();
            sb.append(token);
            int l1 = sb.length();
            nodeToCharPos.put(node, new NodeInfo(l0, l1));
            addMoveLink(node, l0, l1);
            if (endPos < l0)
                endPos = l0;
            col0 = false;
            if (nestLevel == 0)
                paraBold = true;
            break;
        }
        case PgnToken.COMMENT:
            if (prevType != PgnToken.RIGHT_BRACKET) {
                if (nestLevel == 0) {
                    nestLevel++;
                    newLine();
                    nestLevel--;
                } else {
                    if ((prevType != PgnToken.LEFT_PAREN) && !col0)
                        sb.append(' ');
                }
            }
            int l0 = sb.length();
            sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
            int l1 = sb.length();
            int color = ColorTheme.instance().getColor(ColorTheme.PGN_COMMENT);
            sb.setSpan(new ForegroundColorSpan(color), l0, l1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            col0 = false;
            if (nestLevel == 0)
                newLine();
            break;
        case PgnToken.EOF:
            newLine(true);
            upToDate = true;
            break;
        }
        prevType = type;
    }

    @Override
    public void clear() {
        sb = new SpannableStringBuilder();
        offs2Node.clear();
        prevType = PgnToken.EOF;
        nestLevel = 0;
        col0 = true;
        currPos = 0;
        endPos = 0;
        nodeToCharPos.clear();
        paraStart = 0;
        paraIndent = 0;
        paraBold = false;
        pendingNewLine = false;

        upToDate = false;
    }

    private BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

    @Override
    public void setCurrent(Node node) {
        sb.removeSpan(bgSpan);
        NodeInfo ni = nodeToCharPos.get(node);
        if ((ni == null) && (node != null) && (node.getParent() != null))
            ni = nodeToCharPos.get(node.getParent());
        if (ni != null) {
            int color = ColorTheme.instance().getColor(ColorTheme.CURRENT_MOVE);
            bgSpan = new BackgroundColorSpan(color);
            sb.setSpan(bgSpan, ni.l0, ni.l1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            currPos = ni.l0;
        } else {
            currPos = 0;
        }
    }
}
