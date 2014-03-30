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

package org.petero.droidfish.activities;

import org.petero.droidfish.ChessBoard;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;

/** Chess board widget suitable for edit mode. */
public class ChessBoardEdit extends ChessBoard {
    private final boolean landScape;

    public ChessBoardEdit(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawSquareLabels = true;
        Configuration config = getResources().getConfiguration();
        landScape = (config.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private final static int getGap(int sqSize) {
        return sqSize / 4;
    }

    @Override
    protected int getWidth(int sqSize) {
        return landScape ? sqSize * 10 + getGap(sqSize) : sqSize * 8;
    }
    @Override
    protected int getHeight(int sqSize) {
        return landScape ? sqSize * 8 : sqSize * 10 + getGap(sqSize);
    }
    @Override
    protected int getSqSizeW(int width) {
        return landScape ? (width - getGap(sqSize)) / 10 : width / 8;
    }
    @Override
    protected int getSqSizeH(int height) {
        return landScape ? height / 8 : (height - getGap(sqSize)) / 10;
    }
    @Override
    protected int getMaxHeightPercentage() { return 85; }
    @Override
    protected int getMaxWidthPercentage() { return 75; }

    @Override
    protected void computeOrigin(int width, int height) {
        x0 = (width - getWidth(sqSize)) / 2;
        y0 = landScape ? 0 : (height - getHeight(sqSize)) / 2;
    }

    private final int extraPieces(int x, int y) {
        if (landScape) {
            if (x == 8) {
                switch (y) {
                case 0: return Piece.WKING;
                case 1: return Piece.WQUEEN;
                case 2: return Piece.WROOK;
                case 3: return Piece.WBISHOP;
                case 4: return Piece.WKNIGHT;
                case 5: return Piece.WPAWN;
                }
            } else if (x == 9) {
                switch (y) {
                case 0: return Piece.BKING;
                case 1: return Piece.BQUEEN;
                case 2: return Piece.BROOK;
                case 3: return Piece.BBISHOP;
                case 4: return Piece.BKNIGHT;
                case 5: return Piece.BPAWN;
                }
            }
        } else {
            if (y == -1) {
                switch (x) {
                case 0: return Piece.WKING;
                case 1: return Piece.WQUEEN;
                case 2: return Piece.WROOK;
                case 3: return Piece.WBISHOP;
                case 4: return Piece.WKNIGHT;
                case 5: return Piece.WPAWN;
                }
            } else if (y == -2) {
                switch (x) {
                case 0: return Piece.BKING;
                case 1: return Piece.BQUEEN;
                case 2: return Piece.BROOK;
                case 3: return Piece.BBISHOP;
                case 4: return Piece.BKNIGHT;
                case 5: return Piece.BPAWN;
                }
            }
        }
        return Piece.EMPTY;
    }

    @Override
    protected int getXFromSq(int sq) {
        if (sq >= 0) {
            return Position.getX(sq);
        } else {
            int p = -2 - sq;
            if (landScape) {
                return Piece.isWhite(p) ? 8 : 9;
            } else {
                switch (p) {
                case Piece.WKING:   case Piece.BKING:   return 0;
                case Piece.WQUEEN:  case Piece.BQUEEN:  return 1;
                case Piece.WROOK:   case Piece.BROOK:   return 2;
                case Piece.WBISHOP: case Piece.BBISHOP: return 3;
                case Piece.WKNIGHT: case Piece.BKNIGHT: return 4;
                case Piece.WPAWN:   case Piece.BPAWN:   return 5;
                default: return 6;
                }
            }
        }
    }

    @Override
    protected int getYFromSq(int sq) {
        if (sq >= 0) {
            return Position.getY(sq);
        } else {
            int p = -2 - sq;
            if (landScape) {
                switch (p) {
                case Piece.WKING:   case Piece.BKING:   return 0;
                case Piece.WQUEEN:  case Piece.BQUEEN:  return 1;
                case Piece.WROOK:   case Piece.BROOK:   return 2;
                case Piece.WBISHOP: case Piece.BBISHOP: return 3;
                case Piece.WKNIGHT: case Piece.BKNIGHT: return 4;
                case Piece.WPAWN:   case Piece.BPAWN:   return 5;
                default: return 6;
                }
            } else {
                return Piece.isWhite(p) ? -1 : -2;
            }
        }
    }

    @Override
    protected int getSquare(int x, int y) {
        if ((y >= 0) && (x < 8)) {
            return Position.getSquare(x, y);
        } else {
            int p = extraPieces(x, y);
            return -p - 2;
        }
    }

    @Override
    protected void drawExtraSquares(Canvas canvas) {
        int xMin = landScape ?  8 :  0;
        int xMax = landScape ? 10 :  8;
        int yMin = landScape ?  0 : -2;
        int yMax = landScape ?  8 :  0;
        for (int x = xMin; x < xMax; x++) {
            for (int y = yMin; y < yMax; y++) {
                final int xCrd = getXCrd(x);
                final int yCrd = getYCrd(y);
                Paint paint = Position.darkSquare(x, y) ? darkPaint : brightPaint;
                canvas.drawRect(xCrd, yCrd, xCrd+sqSize, yCrd+sqSize, paint);
                int p = extraPieces(x, y);
                drawPiece(canvas, xCrd, yCrd, p);
            }
        }
    }

    @Override
    public
    Move mousePressed(int sq) {
        if (sq == -1)
            return null;
        cursorVisible = false;
        if (selectedSquare != -1) {
            if (sq != selectedSquare) {
                Move m = new Move(selectedSquare, sq, Piece.EMPTY);
                setSelection(sq);
                return m;
            }
            setSelection(-1);
        } else {
            setSelection(sq);
        }
        return null;
    }

    @Override
    protected int minValidY() {
        return landScape ? 0 : -2;
    }
    @Override
    protected int maxValidX() {
        return landScape ? 9 : 7;
    }

    @Override
    protected int getXCrd(int x) {
        return x0 + sqSize * x + ((x >= 8) ? getGap(sqSize) : 0);
    }

    @Override
    protected int getYCrd(int y) {
        return y0 + sqSize * (7 - y) + ((y < 0) ? getGap(sqSize) : 0);
    }

    @Override
    protected int getXSq(int xCrd) {
        int x = (xCrd - x0) / sqSize;
        if (x < 8)
            return x;
        return (xCrd - x0 - getGap(sqSize)) / sqSize;
    }

    @Override
    protected int getYSq(int yCrd) {
        int y = 7 - (yCrd - y0) / sqSize;
        if (y >= 0)
            return y;
        return 7 - (yCrd - y0 - getGap(sqSize)) / sqSize;
    }

    /**
     * Compute the square corresponding to the coordinates of a mouse event.
     * @param evt Details about the mouse event.
     * @return The square corresponding to the mouse event, or -1 if outside board.
     */
    @Override
    public int eventToSquare(MotionEvent evt) {
        int sq = super.eventToSquare(evt);
        if (sq != -1)
            return sq;

        int xCrd = (int)(evt.getX());
        int yCrd = (int)(evt.getY());

        if (sqSize > 0) {
            int x = getXSq(xCrd);
            int y = getYSq(yCrd);
            if ( landScape && (x >= 0) && (x < 10) && (y >= 0) && (y < 8) ||
                !landScape && (x >= 0) && (x < 8) && (y >= -2) && (y < 0)) {
                int p = extraPieces(x, y);
                sq = -p - 2;
            }
        }
        return sq;
    }
}
