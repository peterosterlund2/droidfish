/*
    DroidFish - An Android chess program.
    Copyright (C) 2020  Peter Österlund, peterosterlund2@gmail.com

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

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Piece;

public class ChessBoardPlayListener implements View.OnTouchListener {
    private DroidFish df;
    private ChessBoardPlay cb;

    private boolean pending = false;
    private boolean pendingClick = false;
    private int sq0 = -1;
    private boolean isValidDragSquare; // True if dragging starting at "sq0" is valid
    private int dragSquare = -1;
    private float scrollX = 0;
    private float scrollY = 0;
    private float prevX = 0;
    private float prevY = 0;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            pending = false;
            handler.removeCallbacks(runnable);
            df.reShowDialog(DroidFish.BOARD_MENU_DIALOG);
        }
    };

    ChessBoardPlayListener(DroidFish df, ChessBoardPlay cb) {
        this.df = df;
        this.cb = cb;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            handler.postDelayed(runnable, ViewConfiguration.getLongPressTimeout());
            pending = true;
            pendingClick = true;
            sq0 = cb.eventToSquare(event);
            isValidDragSquare = cb.isValidDragSquare(sq0);
            dragSquare = -1;
            scrollX = 0;
            scrollY = 0;
            prevX = event.getX();
            prevY = event.getY();
            break;
        case MotionEvent.ACTION_MOVE:
            if (pending) {
                int sq = cb.eventToSquare(event);
                if (sq != sq0) {
                    handler.removeCallbacks(runnable);
                    pendingClick = false;
                }
                float currX = event.getX();
                float currY = event.getY();
                if (onMove(event)) {
                    handler.removeCallbacks(runnable);
                    pendingClick = false;
                }
                prevX = currX;
                prevY = currY;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (pending) {
                pending = false;
                handler.removeCallbacks(runnable);
                if (df.ctrl.humansTurn()) {
                    int sq = cb.eventToSquare(event);
                    if (dragSquare != -1) {
                        if (sq != -1 && sq != sq0) {
                            cb.setSelection(cb.highlightLastMove ? sq : -1);
                            cb.userSelectedSquare = false;
                            Move m = new Move(sq0, sq, Piece.EMPTY);
                            df.setAutoMode(DroidFish.AutoMode.OFF);
                            df.ctrl.makeHumanMove(m, false);
                        }
                    } else if (pendingClick && (sq == sq0)) {
                        Move m = cb.mousePressed(sq);
                        if (m != null) {
                            df.setAutoMode(DroidFish.AutoMode.OFF);
                            df.ctrl.makeHumanMove(m, true);
                        }
                        df.setEgtbHints(cb.getSelectedSquare());
                    }
                }
                cb.setDragState(-1, 0, 0);
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            pending = false;
            cb.setDragState(-1, 0, 0);
            handler.removeCallbacks(runnable);
            break;
        }
        return true;
    }

    /** Process an ACTION_MOVE event. Return true if a gesture is detected,
     *  which means that a click will not happen when ACTION_UP is received. */
    private boolean onMove(MotionEvent event) {
        if (df.dragMoveEnabled && isValidDragSquare) {
            return onDrag(event);
        } else {
            return onScroll(event.getX() - prevX, event.getY() - prevY);
        }
    }

    private boolean onDrag(MotionEvent event) {
        if (dragSquare == -1) {
            int sq = cb.eventToSquare(event);
            if (sq != sq0)
                dragSquare = sq0;
        }
        if (dragSquare != -1)
            if (!cb.setDragState(dragSquare, (int)event.getX(), (int)event.getY()))
                dragSquare = -1;
        return false;
    }

    private boolean onScroll(float distanceX, float distanceY) {
        if (df.invertScrollDirection) {
            distanceX = -distanceX;
            distanceY = -distanceY;
        }
        if ((df.scrollSensitivity > 0) && (cb.sqSize > 0)) {
            scrollX += distanceX;
            scrollY += distanceY;
            final float scrollUnit = cb.sqSize * df.scrollSensitivity;
            if (Math.abs(scrollX) >= Math.abs(scrollY)) {
                // Undo/redo
                int nRedo = 0, nUndo = 0;
                while (scrollX > scrollUnit) {
                    nRedo++;
                    scrollX -= scrollUnit;
                }
                while (scrollX < -scrollUnit) {
                    nUndo++;
                    scrollX += scrollUnit;
                }
                if (nUndo + nRedo > 0) {
                    scrollY = 0;
                    df.setAutoMode(DroidFish.AutoMode.OFF);
                }
                if (nRedo + nUndo > 1) {
                    boolean analysis = df.gameMode.analysisMode();
                    boolean human = df.gameMode.playerWhite() || df.gameMode.playerBlack();
                    if (analysis || !human)
                        df.ctrl.setGameMode(new GameMode(GameMode.TWO_PLAYERS));
                }
                if (df.scrollGames) {
                    if (nRedo > 0) {
                        UIAction nextGame = df.actionFactory.getAction("nextGame");
                        if (nextGame.enabled())
                            for (int i = 0; i < nRedo; i++)
                                nextGame.run();
                    }
                    if (nUndo > 0) {
                        UIAction prevGame = df.actionFactory.getAction("prevGame");
                        if (prevGame.enabled())
                            for (int i = 0; i < nUndo; i++)
                                prevGame.run();
                    }
                } else {
                    for (int i = 0; i < nRedo; i++) df.ctrl.redoMove();
                    for (int i = 0; i < nUndo; i++) df.ctrl.undoMove();
                }
                df.ctrl.setGameMode(df.gameMode);
                return nRedo + nUndo > 0;
            } else {
                // Next/previous variation
                int varDelta = 0;
                while (scrollY > scrollUnit) {
                    varDelta++;
                    scrollY -= scrollUnit;
                }
                while (scrollY < -scrollUnit) {
                    varDelta--;
                    scrollY += scrollUnit;
                }
                if (varDelta != 0) {
                    scrollX = 0;
                    df.setAutoMode(DroidFish.AutoMode.OFF);
                    df.ctrl.changeVariation(varDelta);
                }
                return varDelta != 0;
            }
        }
        return false;
    }
}
