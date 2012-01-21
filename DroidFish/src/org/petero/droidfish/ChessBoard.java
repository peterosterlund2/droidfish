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

package org.petero.droidfish;

import java.util.ArrayList;
import java.util.List;

import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.MoveGen;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.UndoInfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ChessBoard extends View {
    public Position pos;

    public int selectedSquare;
    public float cursorX, cursorY;
    public boolean cursorVisible;
    protected int x0, y0, sqSize;
    int pieceXDelta, pieceYDelta; // top/left pixel draw position relative to square
    public boolean flipped;
    public boolean drawSquareLabels;
    boolean oneTouchMoves;

    List<Move> moveHints;

    /** Decoration for a square. Currently the only possible decoration is a number. */
    public final static class SquareDecoration {
        int sq;
        int number;
        public SquareDecoration(int sq, int number) {
            this.sq = sq;
            this.number = number;
        }
    }
    private ArrayList<SquareDecoration> decorations;

    protected Paint darkPaint;
    protected Paint brightPaint;
    private Paint selectedSquarePaint;
    private Paint cursorSquarePaint;
    private Paint whitePiecePaint;
    private Paint blackPiecePaint;
    private Paint labelPaint;
    private Paint decorationPaint;
    private ArrayList<Paint> moveMarkPaint;

    public ChessBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        pos = new Position();
        selectedSquare = -1;
        cursorX = cursorY = 0;
        cursorVisible = false;
        x0 = y0 = sqSize = 0;
        pieceXDelta = pieceYDelta = -1;
        flipped = false;
        drawSquareLabels = false;
        oneTouchMoves = false;

        darkPaint = new Paint();
        brightPaint = new Paint();

        selectedSquarePaint = new Paint();
        selectedSquarePaint.setStyle(Paint.Style.STROKE);
        selectedSquarePaint.setAntiAlias(true);

        cursorSquarePaint = new Paint();
        cursorSquarePaint.setStyle(Paint.Style.STROKE);
        cursorSquarePaint.setAntiAlias(true);

        whitePiecePaint = new Paint();
        whitePiecePaint.setAntiAlias(true);

        blackPiecePaint = new Paint();
        blackPiecePaint.setAntiAlias(true);

        labelPaint = new Paint();
        labelPaint.setAntiAlias(true);

        decorationPaint = new Paint();
        decorationPaint.setAntiAlias(true);

        moveMarkPaint = new ArrayList<Paint>();
        for (int i = 0; i < 6; i++) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setAntiAlias(true);
            moveMarkPaint.add(p);
        }

        if (isInEditMode())
            return;

        Typeface chessFont = Typeface.createFromAsset(getContext().getAssets(), "ChessCases.ttf");
        whitePiecePaint.setTypeface(chessFont);
        blackPiecePaint.setTypeface(chessFont);

        setColors();
    }

    /** Must be called for new color theme to take effect. */
    final void setColors() {
        ColorTheme ct = ColorTheme.instance();
        darkPaint.setColor(ct.getColor(ColorTheme.DARK_SQUARE));
        brightPaint.setColor(ct.getColor(ColorTheme.BRIGHT_SQUARE));
        selectedSquarePaint.setColor(ct.getColor(ColorTheme.SELECTED_SQUARE));
        cursorSquarePaint.setColor(ct.getColor(ColorTheme.CURSOR_SQUARE));
        whitePiecePaint.setColor(ct.getColor(ColorTheme.BRIGHT_PIECE));
        blackPiecePaint.setColor(ct.getColor(ColorTheme.DARK_PIECE));
        labelPaint.setColor(ct.getColor(ColorTheme.SQUARE_LABEL));
        decorationPaint.setColor(ct.getColor(ColorTheme.DECORATION));
        for (int i = 0; i < 6; i++)
            moveMarkPaint.get(i).setColor(ct.getColor(ColorTheme.ARROW_0 + i));

        invalidate();
    }

    private Handler handlerTimer = new Handler();

    private final class AnimInfo {
        AnimInfo() { startTime = -1; }
        boolean paused;
        long posHash;   // Position the animation is valid for
        long startTime; // Time in milliseconds when animation was started
        long stopTime;  // Time in milliseconds when animation should stop
        long now;       // Current time in milliseconds
        int piece1, from1, to1, hide1;
        int piece2, from2, to2, hide2;

        public final boolean updateState() {
            now = System.currentTimeMillis();
            return animActive();
        }
        private final boolean animActive() {
            if (paused || (startTime < 0) || (now >= stopTime) || (posHash != pos.zobristHash()))
                return false;
            return true;
        }
        public final boolean squareHidden(int sq) {
            if (!animActive())
                return false;
            return (sq == hide1) || (sq == hide2);
        }
        public final void draw(Canvas canvas) {
            if (!animActive())
                return;
            double animState = (now - startTime) / (double)(stopTime - startTime);
            drawAnimPiece(canvas, piece2, from2, to2, animState);
            drawAnimPiece(canvas, piece1, from1, to1, animState);
            long now2 = System.currentTimeMillis();
            long delay = 20 - (now2 - now);
//          System.out.printf("delay:%d\n", delay);
            if (delay < 1) delay = 1;
            handlerTimer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    invalidate();
                }
            }, delay);
        }
        private void drawAnimPiece(Canvas canvas, int piece, int from, int to, double animState) {
            if (piece == Piece.EMPTY)
                return;
            final int xCrd1 = getXCrd(Position.getX(from));
            final int yCrd1 = getYCrd(Position.getY(from));
            final int xCrd2 = getXCrd(Position.getX(to));
            final int yCrd2 = getYCrd(Position.getY(to));
            final int xCrd = xCrd1 + (int)Math.round((xCrd2 - xCrd1) * animState);
            final int yCrd = yCrd1 + (int)Math.round((yCrd2 - yCrd1) * animState);
            drawPiece(canvas, xCrd, yCrd, piece);
        }
    }
    private AnimInfo anim = new AnimInfo();

    /**
     * Set up move animation. The animation will start the next time setPosition is called.
     * @param sourcePos The source position for the animation.
     * @param move      The move leading to the target position.
     * @param forward   True if forward direction, false for undo move.
     */
    public final void setAnimMove(Position sourcePos, Move move, boolean forward) {
        anim.startTime = -1;
        anim.paused = true; // Animation starts at next position update
        if (forward) {
            // The animation will be played when pos == targetPos
            Position targetPos = new Position(sourcePos);
            UndoInfo ui = new UndoInfo();
            targetPos.makeMove(move, ui);
            anim.posHash = targetPos.zobristHash();
        } else {
            anim.posHash = sourcePos.zobristHash();
        }
        int animTime; // Animation duration in milliseconds.
        {
            int dx = Position.getX(move.to) - Position.getX(move.from);
            int dy = Position.getY(move.to) - Position.getY(move.from);
            double dist = Math.sqrt(dx * dx + dy * dy);
            double t = Math.sqrt(dist) * 100;
            animTime = (int)Math.round(t);
        }
        if (animTime > 0) {
            anim.startTime = System.currentTimeMillis();
            anim.stopTime = anim.startTime + animTime;
            anim.piece2 = Piece.EMPTY;
            anim.from2 = -1;
            anim.to2 = -1;
            anim.hide2 = -1;
            if (forward) {
                int p = sourcePos.getPiece(move.from);
                anim.piece1 = p;
                anim.from1 = move.from;
                anim.to1 = move.to;
                anim.hide1 = anim.to1;
                int p2 = sourcePos.getPiece(move.to);
                if (p2 != Piece.EMPTY) { // capture
                    anim.piece2 = p2;
                    anim.from2 = move.to;
                    anim.to2 = move.to;
                } else if ((p == Piece.WKING) || (p == Piece.BKING)) {
                    boolean wtm = Piece.isWhite(p);
                    if (move.to == move.from + 2) { // O-O
                        anim.piece2 = wtm ? Piece.WROOK : Piece.BROOK;
                        anim.from2 = move.to + 1;
                        anim.to2 = move.to - 1;
                        anim.hide2 = anim.to2;
                    } else if (move.to == move.from - 2) { // O-O-O
                        anim.piece2 = wtm ? Piece.WROOK : Piece.BROOK;
                        anim.from2 = move.to - 2;
                        anim.to2 = move.to + 1;
                        anim.hide2 = anim.to2;
                    }
                }
            } else {
                int p = sourcePos.getPiece(move.from);
                anim.piece1 = p;
                if (move.promoteTo != Piece.EMPTY)
                    anim.piece1 = Piece.isWhite(anim.piece1) ? Piece.WPAWN : Piece.BPAWN;
                anim.from1 = move.to;
                anim.to1 = move.from;
                anim.hide1 = anim.to1;
                if ((p == Piece.WKING) || (p == Piece.BKING)) {
                    boolean wtm = Piece.isWhite(p);
                    if (move.to == move.from + 2) { // O-O
                        anim.piece2 = wtm ? Piece.WROOK : Piece.BROOK;
                        anim.from2 = move.to - 1;
                        anim.to2 = move.to + 1;
                        anim.hide2 = anim.to2;
                    } else if (move.to == move.from - 2) { // O-O-O
                        anim.piece2 = wtm ? Piece.WROOK : Piece.BROOK;
                        anim.from2 = move.to + 1;
                        anim.to2 = move.to - 2;
                        anim.hide2 = anim.to2;
                    }
                }
            }
        }
    }

    /**
     * Set the board to a given state.
     * @param pos
     */
    final public void setPosition(Position pos) {
        boolean doInvalidate = false;
        if (anim.paused = true) {
            anim.paused = false;
            doInvalidate = true;
        }
        if (!this.pos.equals(pos)) {
            this.pos = new Position(pos);
            doInvalidate = true;
        }
        if (doInvalidate)
            invalidate();
    }

    /**
     * Set/clear the board flipped status.
     * @param flipped
     */
    final public void setFlipped(boolean flipped) {
        if (this.flipped != flipped) {
            this.flipped = flipped;
            invalidate();
        }
    }

    /**
     * Set/clear the board flipped status.
     * @param flipped
     */
    final public void setDrawSquareLabels(boolean drawSquareLabels) {
        if (this.drawSquareLabels != drawSquareLabels) {
            this.drawSquareLabels = drawSquareLabels;
            invalidate();
        }
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

    protected int getWidth(int sqSize) { return sqSize * 8 + 4; }
    protected int getHeight(int sqSize) { return sqSize * 8 + 4; }
    protected int getSqSizeW(int width) { return (width - 4) / 8; }
    protected int getSqSizeH(int height) { return (height - 4) / 8; }

    protected int getMaxHeightPercentage() { return 75; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int sqSizeW = getSqSizeW(width);
        int sqSizeH = getSqSizeH(height);
        int sqSize = Math.min(sqSizeW, sqSizeH);
        pieceXDelta = pieceYDelta = -1;
        labelBounds = null;
        if (height > width) {
            int p = getMaxHeightPercentage();
            height = Math.min(getHeight(sqSize), height * p / 100);
        } else {
            width = Math.min(getWidth(sqSize), width * 65 / 100);
        }
        setMeasuredDimension(width, height);
    }

    protected void computeOrigin(int width, int height) {
        x0 = (width - sqSize * 8) / 2;
        y0 = (height - sqSize * 8) / 2;
    }

    protected int getXFromSq(int sq) { return Position.getX(sq); }
    protected int getYFromSq(int sq) { return Position.getY(sq); }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode())
            return;
//      long t0 = System.currentTimeMillis();
        boolean animActive = anim.updateState();
        final int width = getWidth();
        final int height = getHeight();
        sqSize = Math.min(getSqSizeW(width), getSqSizeH(height));
        blackPiecePaint.setTextSize(sqSize);
        whitePiecePaint.setTextSize(sqSize);
        labelPaint.setTextSize(sqSize/4.0f);
        decorationPaint.setTextSize(sqSize/3.0f);
        computeOrigin(width, height);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                final int xCrd = getXCrd(x);
                final int yCrd = getYCrd(y);
                Paint paint = Position.darkSquare(x, y) ? darkPaint : brightPaint;
                canvas.drawRect(xCrd, yCrd, xCrd+sqSize, yCrd+sqSize, paint);

                int sq = Position.getSquare(x, y);
                if (!animActive || !anim.squareHidden(sq)) {
                    int p = pos.getPiece(sq);
                    drawPiece(canvas, xCrd, yCrd, p);
                }
                if (drawSquareLabels) {
                    if (x == (flipped ? 7 : 0)) {
                        drawLabel(canvas, xCrd, yCrd, false, false, "12345678".charAt(y));
                    }
                    if (y == (flipped ? 7 : 0)) {
                        drawLabel(canvas, xCrd, yCrd, true, true, "abcdefgh".charAt(x));
                    }
                }
            }
        }
        drawExtraSquares(canvas);
        if (!animActive && (selectedSquare != -1)) {
            int selX = getXFromSq(selectedSquare);
            int selY = getYFromSq(selectedSquare);
            selectedSquarePaint.setStrokeWidth(sqSize/(float)16);
            int x0 = getXCrd(selX);
            int y0 = getYCrd(selY);
            canvas.drawRect(x0, y0, x0 + sqSize, y0 + sqSize, selectedSquarePaint);
        }
        if (cursorVisible) {
            int x = Math.round(cursorX);
            int y = Math.round(cursorY);
            int x0 = getXCrd(x);
            int y0 = getYCrd(y);
            cursorSquarePaint.setStrokeWidth(sqSize/(float)16);
            canvas.drawRect(x0, y0, x0 + sqSize, y0 + sqSize, cursorSquarePaint);
        }
        if (!animActive) {
            drawMoveHints(canvas);
            drawDecorations(canvas);
        }

        anim.draw(canvas);
//      long t1 = System.currentTimeMillis();
//      System.out.printf("draw: %d\n", t1-t0);
    }

    private final void drawMoveHints(Canvas canvas) {
        if (moveHints == null)
            return;
        float h = (float)(sqSize / 2.0);
        float d = (float)(sqSize / 8.0);
        double v = 35 * Math.PI / 180;
        double cosv = Math.cos(v);
        double sinv = Math.sin(v);
        double tanv = Math.tan(v);
        int n = Math.min(moveMarkPaint.size(), moveHints.size());
        for (int i = 0; i < n; i++) {
            Move m = moveHints.get(i);
            if (m.from == m.to)
                continue;
            float x0 = getXCrd(Position.getX(m.from)) + h;
            float y0 = getYCrd(Position.getY(m.from)) + h;
            float x1 = getXCrd(Position.getX(m.to)) + h;
            float y1 = getYCrd(Position.getY(m.to)) + h;

            float x2 = (float)(Math.hypot(x1 - x0, y1 - y0) + d);
            float y2 = 0;
            float x3 = (float)(x2 - h * cosv);
            float y3 = (float)(y2 - h * sinv);
            float x4 = (float)(x3 - d * sinv);
            float y4 = (float)(y3 + d * cosv);
            float x5 = (float)(x4 + (-d/2 - y4) / tanv);
            float y5 = (float)(-d / 2);
            float x6 = 0;
            float y6 = y5 / 2;
            Path path = new Path();
            path.moveTo(x2, y2);
            path.lineTo(x3, y3);
//          path.lineTo(x4, y4);
            path.lineTo(x5, y5);
            path.lineTo(x6, y6);
            path.lineTo(x6, -y6);
            path.lineTo(x5, -y5);
//          path.lineTo(x4, -y4);
            path.lineTo(x3, -y3);
            path.close();
            Matrix mtx = new Matrix();
            mtx.postRotate((float)(Math.atan2(y1 - y0, x1 - x0) * 180 / Math.PI));
            mtx.postTranslate(x0, y0);
            path.transform(mtx);
            Paint p = moveMarkPaint.get(i);
            canvas.drawPath(path, p);
        }
    }

    protected void drawExtraSquares(Canvas canvas) {
    }

    protected final void drawPiece(Canvas canvas, int xCrd, int yCrd, int p) {
        String psb, psw;
        boolean rotate = false;
        switch (p) {
            default:
            case Piece.EMPTY:   psb = null; psw = null; break;
            case Piece.WKING:   psb = "H"; psw = "k"; break;
            case Piece.WQUEEN:  psb = "I"; psw = "l"; break;
            case Piece.WROOK:   psb = "J"; psw = "m"; break;
            case Piece.WBISHOP: psb = "K"; psw = "n"; break;
            case Piece.WKNIGHT: psb = "L"; psw = "o"; break;
            case Piece.WPAWN:   psb = "M"; psw = "p"; break;
            case Piece.BKING:   psb = "N"; psw = "q"; rotate = true; break;
            case Piece.BQUEEN:  psb = "O"; psw = "r"; rotate = true; break;
            case Piece.BROOK:   psb = "P"; psw = "s"; rotate = true; break;
            case Piece.BBISHOP: psb = "Q"; psw = "t"; rotate = true; break;
            case Piece.BKNIGHT: psb = "R"; psw = "u"; rotate = true; break;
            case Piece.BPAWN:   psb = "S"; psw = "v"; rotate = true; break;
        }
        if (psb != null) {
            if (pieceXDelta < 0) {
                Rect bounds = new Rect();
                blackPiecePaint.getTextBounds("H", 0, 1, bounds);
                pieceXDelta = (sqSize - (bounds.left + bounds.right)) / 2;
                pieceYDelta = (sqSize - (bounds.top + bounds.bottom)) / 2;
            }
            rotate ^= flipped;
            rotate = false; // Disabled for now
            if (rotate) {
                canvas.save();
                canvas.rotate(180, xCrd + sqSize * 0.5f, yCrd + sqSize * 0.5f);
            }
            xCrd += pieceXDelta;
            yCrd += pieceYDelta;
            canvas.drawText(psw, xCrd, yCrd, whitePiecePaint);
            canvas.drawText(psb, xCrd, yCrd, blackPiecePaint);
            if (rotate)
                canvas.restore();
        }
    }

    private Rect labelBounds = null;

    private final void drawLabel(Canvas canvas, int xCrd, int yCrd, boolean right,
                                 boolean bottom, char c) {
        String s = Character.toString(c);
        if (labelBounds == null) {
            labelBounds = new Rect();
            labelPaint.getTextBounds("f", 0, 1, labelBounds);
        }
        int margin = sqSize / 16;
        if (right) {
                xCrd += sqSize - labelBounds.right - margin;
            } else {
            xCrd += -labelBounds.left + margin;
        }
        if (bottom) {
            yCrd += sqSize - labelBounds.bottom - margin;
        } else {
            yCrd += -labelBounds.top + margin;
        }
        canvas.drawText(s, xCrd, yCrd, labelPaint);
    }

    protected int getXCrd(int x) { return x0 + sqSize * (flipped ? 7 - x : x); }
    protected int getYCrd(int y) { return y0 + sqSize * (flipped ? y : 7 - y); }
    protected int getXSq(int xCrd) { int t = (xCrd - x0) / sqSize; return flipped ? 7 - t : t; }
    protected int getYSq(int yCrd) { int t = (yCrd - y0) / sqSize; return flipped ? t : 7 - t; }

    /**
     * Compute the square corresponding to the coordinates of a mouse event.
     * @param evt Details about the mouse event.
     * @return The square corresponding to the mouse event, or -1 if outside board.
     */
    public int eventToSquare(MotionEvent evt) {
        int xCrd = (int)(evt.getX());
        int yCrd = (int)(evt.getY());

        int sq = -1;
        if (sqSize > 0) {
            int x = getXSq(xCrd);
            int y = getYSq(yCrd);
            if ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
                sq = Position.getSquare(x, y);
            }
        }
        return sq;
    }

    private final boolean myColor(int piece) {
        return (piece != Piece.EMPTY) && (Piece.isWhite(piece) == pos.whiteMove);
    }

    public Move mousePressed(int sq) {
        if (sq < 0)
            return null;
        cursorVisible = false;
        if (selectedSquare != -1) {
            int p = pos.getPiece(selectedSquare);
            if (!myColor(p)) {
                setSelection(-1); // Remove selection of opponents last moving piece
            }
        }

        int p = pos.getPiece(sq);
        if (selectedSquare != -1) {
            if (sq != selectedSquare) {
                if (!myColor(p)) {
                    Move m = new Move(selectedSquare, sq, Piece.EMPTY);
                    setSelection(sq);
                    return m;
                }
            }
            setSelection(-1);
        } else {
            if (oneTouchMoves) {
                ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
                moves = MoveGen.removeIllegal(pos, moves);
                Move matchingMove = null;
                int toSq = -1;
                for (Move m : moves) {
                    if ((m.from == sq) || (m.to == sq)) {
                        if (matchingMove == null) {
                            matchingMove = m;
                            toSq = m.to;
                        } else {
                            matchingMove = null;
                            break;
                        }
                    }
                }
                if (matchingMove != null) {
                    setSelection(toSq);
                    return matchingMove;
                }
            }
            if (myColor(p)) {
                setSelection(sq);
            }
        }
        return null;
    }

    public static class OnTrackballListener {
        public void onTrackballEvent(MotionEvent event) { }
    }
    private OnTrackballListener otbl = null;
    public final void setOnTrackballListener(OnTrackballListener onTrackballListener) {
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

    protected int minValidY() { return 0; }
    protected int getSquare(int x, int y) { return Position.getSquare(x, y); }

    public final Move handleTrackballEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            invalidate();
            if (cursorVisible) {
                int x = Math.round(cursorX);
                int y = Math.round(cursorY);
                cursorX = x;
                cursorY = y;
                int sq = getSquare(x, y);
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
        if (cursorY < minValidY()) cursorY = minValidY();
        if (cursorY > 7) cursorY = 7;
        invalidate();
        return null;
    }

    public final void setMoveHints(List<Move> moveHints) {
        boolean equal = false;
        if ((this.moveHints == null) || (moveHints == null)) {
            equal = this.moveHints == moveHints;
        } else {
            equal = this.moveHints.equals(moveHints);
        }
        if (!equal) {
            this.moveHints = moveHints;
            invalidate();
        }
    }

    public final void setSquareDecorations(ArrayList<SquareDecoration> decorations) {
        boolean equal = false;
        if ((this.decorations == null) || (decorations == null)) {
            equal = this.decorations == decorations;
        } else {
            equal = this.decorations.equals(decorations);
        }
        if (!equal) {
            this.decorations = decorations;
            invalidate();
        }
    }

    private final void drawDecorations(Canvas canvas) {
        if (decorations == null)
            return;
        for (SquareDecoration sd : decorations) {
            int sq = sd.sq;
            if ((sd.sq < 0) || (sd.sq >= 64))
                continue;
            int xCrd = getXCrd(Position.getX(sq));
            int yCrd = getYCrd(Position.getY(sq));

            int num = sd.number;
            String s;
            if (num > 0)
                s = "+" + String.valueOf(num);
            else if (num < 0)
                s = String.valueOf(num);
            else
                s = "0";

            Rect bounds = new Rect();
            decorationPaint.getTextBounds(s, 0, s.length(), bounds);
            xCrd += (sqSize - (bounds.left + bounds.right)) / 2;
            yCrd += (sqSize - (bounds.top + bounds.bottom)) / 2;
            canvas.drawText(s, xCrd, yCrd, decorationPaint);
        }
    }

    public final int getSelectedSquare() {
        return selectedSquare;
    }
}
