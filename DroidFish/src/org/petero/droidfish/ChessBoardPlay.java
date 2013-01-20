/*
    DroidFish - An Android chess program.
    Copyright (C) 2012  Peter Österlund, peterosterlund2@gmail.com

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

import java.util.ArrayList;

import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.MoveGen;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Toast;

/** Chess board widget suitable for play mode. */
public class ChessBoardPlay extends ChessBoard {
    private PGNOptions pgnOptions = null;
    boolean oneTouchMoves;

    public ChessBoardPlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        oneTouchMoves = false;
    }

    public void setPgnOptions(PGNOptions pgnOptions) {
        this.pgnOptions = pgnOptions;
    }

    @Override
    protected int getXCrd(int x) { return x0 + sqSize * (flipped ? 7 - x : x); }
    @Override
    protected int getYCrd(int y) { return y0 + sqSize * (flipped ? y : 7 - y); }
    @Override
    protected int getXSq(int xCrd) { int t = (xCrd - x0) / sqSize; return flipped ? 7 - t : t; }
    @Override
    protected int getYSq(int yCrd) { int t = (yCrd - y0) / sqSize; return flipped ? t : 7 - t; }

    @Override
    protected int getWidth(int sqSize) { return sqSize * 8; }
    @Override
    protected int getHeight(int sqSize) { return sqSize * 8; }
    @Override
    protected int getSqSizeW(int width) { return (width) / 8; }
    @Override
    protected int getSqSizeH(int height) { return (height) / 8; }
    @Override
    protected int getMaxHeightPercentage() { return 75; }
    @Override
    protected int getMaxWidthPercentage() { return 65; }

    @Override
    protected void computeOrigin(int width, int height) {
        x0 = (width - sqSize * 8) / 2;
        Configuration config = getResources().getConfiguration();
        boolean landScape = (config.orientation == Configuration.ORIENTATION_LANDSCAPE);
        y0 = landScape ? 0 : (height - sqSize * 8) / 2;
    }
    @Override
    protected int getXFromSq(int sq) { return Position.getX(sq); }
    @Override
    protected int getYFromSq(int sq) { return Position.getY(sq); }

    @Override
    protected int minValidY() { return 0; }
    @Override
    protected int maxValidX() { return 7; }
    @Override
    protected int getSquare(int x, int y) { return Position.getSquare(x, y); }

    @Override
    protected void drawExtraSquares(Canvas canvas) {
    }

    private final boolean myColor(int piece) {
        return (piece != Piece.EMPTY) && (Piece.isWhite(piece) == pos.whiteMove);
    }

    public Move mousePressed(int sq) {
        if (sq < 0)
            return null;
        cursorVisible = false;
        if ((selectedSquare != -1) && !userSelectedSquare)
            setSelection(-1); // Remove selection of opponents last moving piece

        if (!oneTouchMoves) {
            int p = pos.getPiece(sq);
            if (selectedSquare != -1) {
                if (sq == selectedSquare) {
                    if (toggleSelection)
                        setSelection(-1);
                    return null;
                }
                if (!myColor(p)) {
                    Move m = new Move(selectedSquare, sq, Piece.EMPTY);
                    setSelection(highlightLastMove ? sq : -1);
                    userSelectedSquare = false;
                    return m;
                } else
                    setSelection(sq);
            } else {
                if (myColor(p))
                    setSelection(sq);
            }
        } else {
            int prevSq = userSelectedSquare ? selectedSquare : -1;
            if (prevSq == sq) {
                if (toggleSelection)
                    setSelection(-1);
                return null;
            }
            ArrayList<Move> moves = new MoveGen().legalMoves(pos);
            Move matchingMove = null;
            if (prevSq >= 0)
                matchingMove = matchingMove(prevSq, sq, moves).first;
            boolean anyMatch = false;
            if  (matchingMove == null) {
                Pair<Move, Boolean> match = matchingMove(-1, sq, moves);
                matchingMove = match.first;
                anyMatch = match.second;
            }
            if (matchingMove != null) {
                setSelection(highlightLastMove ? matchingMove.to : -1);
                userSelectedSquare = false;
                return matchingMove;
            }
            if (!anyMatch && (sq >= 0)) {
                int p = pos.getPiece(sq);
                if (myColor(p)) {
                    String msg = getContext().getString(R.string.piece_can_not_be_moved);
                    int pieceType = (pgnOptions == null) ? PGNOptions.PT_LOCAL
                                                         : pgnOptions.view.pieceType;
                    msg += ": " + TextIO.pieceAndSquareToString(pieceType, p, sq);
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
            setSelection(anyMatch ? sq : -1);
        }
        return null;
    }

    /**
     * Determine if there is a unique legal move corresponding to one or two selected squares.
     * @param sq1   First square, or -1.
     * @param sq2   Second square.
     * @param moves List of legal moves.
     * @return      Matching move if unique.
     *              Boolean indicating if there was at least one match.
     */
    private final Pair<Move, Boolean> matchingMove(int sq1, int sq2, ArrayList<Move> moves) {
        Move matchingMove = null;
        boolean anyMatch = false;
        for (Move m : moves) {
            boolean match;
            if (sq1 == -1)
                match = (m.from == sq2) || (m.to == sq2);
            else
                match = (m.from == sq1) && (m.to == sq2) ||
                        (m.from == sq2) && (m.to == sq1);
            if (match) {
                if (matchingMove == null) {
                    matchingMove = m;
                    anyMatch = true;
                } else {
                    if ((matchingMove.from == m.from) &&
                        (matchingMove.to == m.to)) {
                        matchingMove.promoteTo = Piece.EMPTY;
                    } else {
                        matchingMove = null;
                        break;
                    }
                }
            }
        }
        return new Pair<Move, Boolean>(matchingMove, anyMatch);
    }
}
