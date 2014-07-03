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

package org.petero.droidfish.gamelogic;

import java.util.ArrayList;


/**
 *
 * @author petero
 */
public class MoveGen {
    public static MoveGen instance;
    static {
        instance = new MoveGen();
    }

    /** Generate and return a list of legal moves. */
    public final ArrayList<Move> legalMoves(Position pos) {
        ArrayList<Move> moveList = pseudoLegalMoves(pos);
        moveList = MoveGen.removeIllegal(pos, moveList);
        return moveList;
    }

    /**
     * Generate and return a list of pseudo-legal moves.
     * Pseudo-legal means that the moves don't necessarily defend from check threats.
     */
    public final ArrayList<Move> pseudoLegalMoves(Position pos) {
        ArrayList<Move> moveList = getMoveListObj();
        final boolean wtm = pos.whiteMove;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int sq = Position.getSquare(x, y);
                int p = pos.getPiece(sq);
                if ((p == Piece.EMPTY) || (Piece.isWhite(p) != wtm)) {
                    continue;
                }
                if ((p == Piece.WROOK) || (p == Piece.BROOK) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addDirection(moveList, pos, sq, 7-x,  1)) return moveList;
                    if (addDirection(moveList, pos, sq, 7-y,  8)) return moveList;
                    if (addDirection(moveList, pos, sq,   x, -1)) return moveList;
                    if (addDirection(moveList, pos, sq,   y, -8)) return moveList;
                }
                if ((p == Piece.WBISHOP) || (p == Piece.BBISHOP) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addDirection(moveList, pos, sq, Math.min(7-x, 7-y),  9)) return moveList;
                    if (addDirection(moveList, pos, sq, Math.min(  x, 7-y),  7)) return moveList;
                    if (addDirection(moveList, pos, sq, Math.min(  x,   y), -9)) return moveList;
                    if (addDirection(moveList, pos, sq, Math.min(7-x,   y), -7)) return moveList;
                }
                if ((p == Piece.WKNIGHT) || (p == Piece.BKNIGHT)) {
                    if (x < 6 && y < 7 && addDirection(moveList, pos, sq, 1,  10)) return moveList;
                    if (x < 7 && y < 6 && addDirection(moveList, pos, sq, 1,  17)) return moveList;
                    if (x > 0 && y < 6 && addDirection(moveList, pos, sq, 1,  15)) return moveList;
                    if (x > 1 && y < 7 && addDirection(moveList, pos, sq, 1,   6)) return moveList;
                    if (x > 1 && y > 0 && addDirection(moveList, pos, sq, 1, -10)) return moveList;
                    if (x > 0 && y > 1 && addDirection(moveList, pos, sq, 1, -17)) return moveList;
                    if (x < 7 && y > 1 && addDirection(moveList, pos, sq, 1, -15)) return moveList;
                    if (x < 6 && y > 0 && addDirection(moveList, pos, sq, 1,  -6)) return moveList;
                }
                if ((p == Piece.WKING) || (p == Piece.BKING)) {
                    if (x < 7          && addDirection(moveList, pos, sq, 1,  1)) return moveList;
                    if (x < 7 && y < 7 && addDirection(moveList, pos, sq, 1,  9)) return moveList;
                    if (         y < 7 && addDirection(moveList, pos, sq, 1,  8)) return moveList;
                    if (x > 0 && y < 7 && addDirection(moveList, pos, sq, 1,  7)) return moveList;
                    if (x > 0          && addDirection(moveList, pos, sq, 1, -1)) return moveList;
                    if (x > 0 && y > 0 && addDirection(moveList, pos, sq, 1, -9)) return moveList;
                    if (         y > 0 && addDirection(moveList, pos, sq, 1, -8)) return moveList;
                    if (x < 7 && y > 0 && addDirection(moveList, pos, sq, 1, -7)) return moveList;

                    int k0 = wtm ? Position.getSquare(4,0) : Position.getSquare(4,7);
                    if (Position.getSquare(x,y) == k0) {
                        int aCastle = wtm ? Position.A1_CASTLE : Position.A8_CASTLE;
                        int hCastle = wtm ? Position.H1_CASTLE : Position.H8_CASTLE;
                        int rook = wtm ? Piece.WROOK : Piece.BROOK;
                        if (((pos.getCastleMask() & (1 << hCastle)) != 0) &&
                                (pos.getPiece(k0 + 1) == Piece.EMPTY) &&
                                (pos.getPiece(k0 + 2) == Piece.EMPTY) &&
                                (pos.getPiece(k0 + 3) == rook) &&
                                !sqAttacked(pos, k0) &&
                                !sqAttacked(pos, k0 + 1)) {
                            moveList.add(getMoveObj(k0, k0 + 2, Piece.EMPTY));
                        }
                        if (((pos.getCastleMask() & (1 << aCastle)) != 0) &&
                                (pos.getPiece(k0 - 1) == Piece.EMPTY) &&
                                (pos.getPiece(k0 - 2) == Piece.EMPTY) &&
                                (pos.getPiece(k0 - 3) == Piece.EMPTY) &&
                                (pos.getPiece(k0 - 4) == rook) &&
                                !sqAttacked(pos, k0) &&
                                !sqAttacked(pos, k0 - 1)) {
                            moveList.add(getMoveObj(k0, k0 - 2, Piece.EMPTY));
                        }
                    }
                }
                if ((p == Piece.WPAWN) || (p == Piece.BPAWN)) {
                    int yDir = wtm ? 8 : -8;
                    if (pos.getPiece(sq + yDir) == Piece.EMPTY) { // non-capture
                        addPawnMoves(moveList, sq, sq + yDir);
                        if ((y == (wtm ? 1 : 6)) &&
                                (pos.getPiece(sq + 2 * yDir) == Piece.EMPTY)) { // double step
                            addPawnMoves(moveList, sq, sq + yDir * 2);
                        }
                    }
                    if (x > 0) { // Capture to the left
                        int toSq = sq + yDir - 1;
                        int cap = pos.getPiece(toSq);
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != wtm) {
                                if (cap == (wtm ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(sq, toSq, Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, sq, toSq);
                                }
                            }
                        } else if (toSq == pos.getEpSquare()) {
                            addPawnMoves(moveList, sq, toSq);
                        }
                    }
                    if (x < 7) { // Capture to the right
                        int toSq = sq + yDir + 1;
                        int cap = pos.getPiece(toSq);
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != wtm) {
                                if (cap == (wtm ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(sq, toSq, Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, sq, toSq);
                                }
                            }
                        } else if (toSq == pos.getEpSquare()) {
                            addPawnMoves(moveList, sq, toSq);
                        }
                    }
                }
            }
        }
        return moveList;
    }

    /**
     * Return true if the side to move is in check.
     */
    public static final boolean inCheck(Position pos) {
        int kingSq = pos.getKingSq(pos.whiteMove);
        if (kingSq < 0)
            return false;
        return sqAttacked(pos, kingSq);
    }

    /**
     * Return true if a square is attacked by the opposite side.
     */
    public static final boolean sqAttacked(Position pos, int sq) {
        int x = Position.getX(sq);
        int y = Position.getY(sq);
        boolean isWhiteMove = pos.whiteMove;

        final int oQueen= isWhiteMove ? Piece.BQUEEN: Piece.WQUEEN;
        final int oRook = isWhiteMove ? Piece.BROOK : Piece.WROOK;
        final int oBish = isWhiteMove ? Piece.BBISHOP : Piece.WBISHOP;
        final int oKnight = isWhiteMove ? Piece.BKNIGHT : Piece.WKNIGHT;

        int p;
        if (y > 0) {
            p = checkDirection(pos, sq,   y, -8); if ((p == oQueen) || (p == oRook)) return true;
            p = checkDirection(pos, sq, Math.min(  x,   y), -9); if ((p == oQueen) || (p == oBish)) return true;
            p = checkDirection(pos, sq, Math.min(7-x,   y), -7); if ((p == oQueen) || (p == oBish)) return true;
            if (x > 1         ) { p = checkDirection(pos, sq, 1, -10); if (p == oKnight) return true; }
            if (x > 0 && y > 1) { p = checkDirection(pos, sq, 1, -17); if (p == oKnight) return true; }
            if (x < 7 && y > 1) { p = checkDirection(pos, sq, 1, -15); if (p == oKnight) return true; }
            if (x < 6         ) { p = checkDirection(pos, sq, 1,  -6); if (p == oKnight) return true; }

            if (!isWhiteMove) {
                if (x < 7 && y > 1) { p = checkDirection(pos, sq, 1, -7); if (p == Piece.WPAWN) return true; }
                if (x > 0 && y > 1) { p = checkDirection(pos, sq, 1, -9); if (p == Piece.WPAWN) return true; }
            }
        }
        if (y < 7) {
            p = checkDirection(pos, sq, 7-y,  8); if ((p == oQueen) || (p == oRook)) return true;
            p = checkDirection(pos, sq, Math.min(7-x, 7-y),  9); if ((p == oQueen) || (p == oBish)) return true;
            p = checkDirection(pos, sq, Math.min(  x, 7-y),  7); if ((p == oQueen) || (p == oBish)) return true;
            if (x < 6         ) { p = checkDirection(pos, sq, 1,  10); if (p == oKnight) return true; }
            if (x < 7 && y < 6) { p = checkDirection(pos, sq, 1,  17); if (p == oKnight) return true; }
            if (x > 0 && y < 6) { p = checkDirection(pos, sq, 1,  15); if (p == oKnight) return true; }
            if (x > 1         ) { p = checkDirection(pos, sq, 1,   6); if (p == oKnight) return true; }
            if (isWhiteMove) {
                if (x < 7 && y < 6) { p = checkDirection(pos, sq, 1, 9); if (p == Piece.BPAWN) return true; }
                if (x > 0 && y < 6) { p = checkDirection(pos, sq, 1, 7); if (p == Piece.BPAWN) return true; }
            }
        }
        p = checkDirection(pos, sq, 7-x,  1); if ((p == oQueen) || (p == oRook)) return true;
        p = checkDirection(pos, sq,   x, -1); if ((p == oQueen) || (p == oRook)) return true;

        int oKingSq = pos.getKingSq(!isWhiteMove);
        if (oKingSq >= 0) {
            int ox = Position.getX(oKingSq);
            int oy = Position.getY(oKingSq);
            if ((Math.abs(x - ox) <= 1) && (Math.abs(y - oy) <= 1))
                return true;
        }

        return false;
    }

    /**
     * Remove all illegal moves from moveList.
     * "moveList" is assumed to be a list of pseudo-legal moves.
     * This function removes the moves that don't defend from check threats.
     */
    public static final ArrayList<Move> removeIllegal(Position pos, ArrayList<Move> moveList) {
        ArrayList<Move> ret = new ArrayList<Move>();
        UndoInfo ui = new UndoInfo();
        int mlSize = moveList.size();
        for (int mi = 0; mi < mlSize; mi++) {
            Move m = moveList.get(mi);
            pos.makeMove(m, ui);
            pos.setWhiteMove(!pos.whiteMove);
            if (!inCheck(pos))
                ret.add(m);
            pos.setWhiteMove(!pos.whiteMove);
            pos.unMakeMove(m, ui);
        }
        return ret;
    }

    /**
     * Add all moves from square sq0 in direction delta.
     * @param maxSteps Max steps until reaching a border. Set to 1 for non-sliding pieces.
     * @ return True if the enemy king could be captured, false otherwise.
     */
    private final boolean addDirection(ArrayList<Move> moveList, Position pos, int sq0, int maxSteps, int delta) {
        int sq = sq0;
        boolean wtm = pos.whiteMove;
        final int oKing = (wtm ? Piece.BKING : Piece.WKING);
        while (maxSteps > 0) {
            sq += delta;
            int p = pos.getPiece(sq);
            if (p == Piece.EMPTY) {
                moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
            } else {
                if (Piece.isWhite(p) != wtm) {
                    if (p == oKing) {
                        returnMoveList(moveList);
                        moveList = getMoveListObj(); // Ugly! this only works because we get back the same object
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                        return true;
                    } else {
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                    }
                }
                break;
            }
            maxSteps--;
        }
        return false;
    }

    /**
     * Generate all possible pawn moves from (x0,y0) to (x1,y1), taking pawn promotions into account.
     */
    private final void addPawnMoves(ArrayList<Move> moveList, int sq0, int sq1) {
        if (sq1 >= 56) { // White promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.WQUEEN));
            moveList.add(getMoveObj(sq0, sq1, Piece.WKNIGHT));
            moveList.add(getMoveObj(sq0, sq1, Piece.WROOK));
            moveList.add(getMoveObj(sq0, sq1, Piece.WBISHOP));
        } else if (sq1 < 8) { // Black promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.BQUEEN));
            moveList.add(getMoveObj(sq0, sq1, Piece.BKNIGHT));
            moveList.add(getMoveObj(sq0, sq1, Piece.BROOK));
            moveList.add(getMoveObj(sq0, sq1, Piece.BBISHOP));
        } else { // No promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.EMPTY));
        }
    }

    /**
     * Check if there is an attacking piece in a given direction starting from sq.
     * The direction is given by delta.
     * @param maxSteps Max steps until reaching a border. Set to 1 for non-sliding pieces.
     * @return The first piece in the given direction, or EMPTY if there is no piece
     *         in that direction.
     */
    private static final int checkDirection(Position pos, int sq, int maxSteps, int delta) {
        while (maxSteps > 0) {
            sq += delta;
            int p = pos.getPiece(sq);
            if (p != Piece.EMPTY)
                return p;
            maxSteps--;
        }
        return Piece.EMPTY;
    }

    // Code to handle the Move cache.

    private Move[] moveCache = new Move[2048];
    private int movesInCache = 0;
    private Object[] moveListCache = new Object[200];
    private int moveListsInCache = 0;

    private final Move getMoveObj(int from, int to, int promoteTo) {
        if (movesInCache > 0) {
            Move m = moveCache[--movesInCache];
            m.from = from;
            m.to = to;
            m.promoteTo = promoteTo;
            return m;
        }
        return new Move(from, to, promoteTo);
    }

    @SuppressWarnings("unchecked")
    private final ArrayList<Move> getMoveListObj() {
        if (moveListsInCache > 0) {
            return (ArrayList<Move>)moveListCache[--moveListsInCache];
        }
        return new ArrayList<Move>(60);
    }

    /** Return all move objects in moveList to the move cache. */
    public final void returnMoveList(ArrayList<Move> moveList) {
        if (movesInCache + moveList.size() <= moveCache.length) {
            int mlSize = moveList.size();
            for (int mi = 0; mi < mlSize; mi++) {
                moveCache[movesInCache++] = moveList.get(mi);
            }
        }
        moveList.clear();
        if (moveListsInCache < moveListCache.length) {
            moveListCache[moveListsInCache++] = moveList;
        }
    }

    public final void returnMove(Move m) {
        if (movesInCache < moveCache.length) {
            moveCache[movesInCache++] = m;
        }
    }
}
