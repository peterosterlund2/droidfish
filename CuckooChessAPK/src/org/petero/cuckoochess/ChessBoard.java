/*
    CuckooChess - A java chess program.
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

package org.petero.cuckoochess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import chess.Move;
import chess.Piece;
import chess.Position;

public class ChessBoard extends View {
    private Position pos;
    private int selectedSquare;
    private float cursorX, cursorY;
    private boolean cursorVisible;
    private int x0, y0, sqSize;
    private boolean flipped;

    private Paint darkPaint;
    private Paint brightPaint;
    private Paint redOutline;
    private Paint greenOutline;
    private Paint whitePiecePaint;
    private Paint blackPiecePaint;
    
    public ChessBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        pos = new Position();
        selectedSquare = -1;
        cursorX = cursorY = 0;
        cursorVisible = false;
        x0 = y0 = sqSize = 0;
        flipped = false;

        darkPaint = new Paint();
        darkPaint.setARGB(255, 128, 128, 128);

        brightPaint = new Paint();
        brightPaint.setARGB(255, 190, 190, 90);

        redOutline = new Paint();
        redOutline.setARGB(255, 255, 0, 0);
        redOutline.setStyle(Paint.Style.STROKE);
        redOutline.setAntiAlias(true);
        
        greenOutline = new Paint();
        greenOutline.setARGB(255, 0, 255, 0);
        greenOutline.setStyle(Paint.Style.STROKE);
        greenOutline.setAntiAlias(true);
        
        whitePiecePaint = new Paint();
        whitePiecePaint.setARGB(255, 255, 255, 255);
        whitePiecePaint.setAntiAlias(true);
        
        blackPiecePaint = new Paint();
        blackPiecePaint.setARGB(255, 0, 0, 0);
        blackPiecePaint.setAntiAlias(true);
    }

    public void setFont(Typeface tf) {
        whitePiecePaint.setTypeface(tf);
        blackPiecePaint.setTypeface(tf);
        invalidate();
    }

    /**
     * Set the board to a given state.
     * @param pos
     */
    final public void setPosition(Position pos) {
        this.pos = pos;
        invalidate();
    }

    /**
     * Set/clear the board flipped status.
     * @param flipped
     */
    final public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        invalidate();
    }

    /**
     * Set/clear the selected square.
     * @param square The square to select, or -1 to clear selection.
     */
    final public void setSelection(int square) {
        if (square != selectedSquare) {
            selectedSquare = square;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int minSize = Math.min(width, height);
        setMeasuredDimension(minSize, minSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        sqSize = (Math.min(width, height) - 4) / 8;
        x0 = (width - sqSize * 8) / 2;
        y0 = (height - sqSize * 8) / 2;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                final int xCrd = getXCrd(x);
                final int yCrd = getYCrd(y);
                Paint paint = Position.darkSquare(x, y) ? darkPaint : brightPaint;
                canvas.drawRect(xCrd, yCrd, xCrd+sqSize, yCrd+sqSize, paint);

                int sq = Position.getSquare(x, y);
                int p = pos.getPiece(sq);
                drawPiece(canvas, xCrd + sqSize / 2, yCrd + sqSize / 2, p);
            }
        }
        if (selectedSquare >= 0) {
            int selX = Position.getX(selectedSquare);
            int selY = Position.getY(selectedSquare);
            redOutline.setStrokeWidth(sqSize/(float)16);
            int x0 = getXCrd(selX);
            int y0 = getYCrd(selY);
            canvas.drawRect(x0, y0, x0 + sqSize, y0 + sqSize, redOutline);
        }
        if (cursorVisible) {
            int x = Math.round(cursorX);
            int y = Math.round(cursorY);
            int x0 = getXCrd(x);
            int y0 = getYCrd(y);
            greenOutline.setStrokeWidth(sqSize/(float)16);
            canvas.drawRect(x0, y0, x0 + sqSize, y0 + sqSize, greenOutline);
        }
    }

    private final void drawPiece(Canvas canvas, int xCrd, int yCrd, int p) {
        String ps;
        switch (p) {
            case Piece.EMPTY:
                ps = "";
                break;
            case Piece.WKING:
                ps = "k";
                break;
            case Piece.WQUEEN:
                ps = "q";
                break;
            case Piece.WROOK:
                ps = "r";
                break;
            case Piece.WBISHOP:
                ps = "b";
                break;
            case Piece.WKNIGHT:
                ps = "n";
                break;
            case Piece.WPAWN:
                ps = "p";
                break;
            case Piece.BKING:
                ps = "l";
                break;
            case Piece.BQUEEN:
                ps = "w";
                break;
            case Piece.BROOK:
                ps = "t";
                break;
            case Piece.BBISHOP:
                ps = "v";
                break;
            case Piece.BKNIGHT:
                ps = "m";
                break;
            case Piece.BPAWN:
                ps = "o";
                break;
            default:
                ps = "?";
                break;
        }
        if (ps.length() > 0) {
            Paint paint = Piece.isWhite(p) ? whitePiecePaint : blackPiecePaint;
            paint.setTextSize(sqSize);
            Rect bounds = new Rect();
            paint.getTextBounds(ps, 0, ps.length(), bounds);
            int xCent = bounds.centerX();
            int yCent = bounds.centerY();
            canvas.drawText(ps, xCrd - xCent, yCrd - yCent, paint);
        }
    }

    private final int getXCrd(int x) {
        return x0 + sqSize * (flipped ? 7 - x : x);
    }
    private final int getYCrd(int y) {
        return y0 + sqSize * (flipped ? y : (7 - y));
    }

    /**
     * Compute the square corresponding to the coordinates of a mouse event.
     * @param evt Details about the mouse event.
     * @return The square corresponding to the mouse event, or -1 if outside board.
     */
    final int eventToSquare(MotionEvent evt) {
        int xCrd = (int)(evt.getX());
        int yCrd = (int)(evt.getY());

        int sq = -1;
        if ((xCrd >= x0) && (yCrd >= y0) && (sqSize > 0)) {
            int x = (xCrd - x0) / sqSize;
            int y = 7 - (yCrd - y0) / sqSize;
            if ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
                if (flipped) {
                    x = 7 - x;
                    y = 7 - y;
                }
                sq = Position.getSquare(x, y);
            }
        }
        return sq;
    }

    final Move mousePressed(int sq) {
        if (sq < 0)
            return null;
        cursorVisible = false;
        if (selectedSquare >= 0) {
            int p = pos.getPiece(selectedSquare);
            if ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.whiteMove)) {
                setSelection(-1); // Remove selection of opponents last moving piece
            }
        }

        int p = pos.getPiece(sq);
        if (selectedSquare >= 0) {
            if (sq != selectedSquare) {
                if ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.whiteMove)) {
                    Move m = new Move(selectedSquare, sq, Piece.EMPTY);
                    setSelection(sq);
                    return m;
                }
            }
            setSelection(-1);
        } else {
            boolean myColor = (p != Piece.EMPTY) && (Piece.isWhite(p) == pos.whiteMove);
            if (myColor) {
                setSelection(sq);
            }
        }
        return null;
    }

    public static class OnTrackballListener {
        public void onTrackballEvent(MotionEvent event) { }
    }
    OnTrackballListener otbl = null;
    public void setOnTrackballListener(OnTrackballListener onTrackballListener) {
        otbl = onTrackballListener;
    }
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (otbl != null) {
            otbl.onTrackballEvent(event);
            return true;
        }
        return false;
    }
    
    public Move handleTrackballEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            invalidate();
            if (cursorVisible) {
                int x = Math.round(cursorX);
                int y = Math.round(cursorY);
                cursorX = x;
                cursorY = y;
                int sq = Position.getSquare(x, y);
                return mousePressed(sq);
            }
            return null;
        }
        cursorVisible = true;
        int c = flipped ? -1 : 1;
        cursorX += c * event.getX();
        cursorY -= c * event.getY();
        if (cursorX < 0) cursorX = 0;
        if (cursorX > 7) cursorX = 7;
        if (cursorY < 0) cursorY = 0;
        if (cursorY > 7) cursorY = 7;
        invalidate();
        return null;
    }
}
