/*
    Texel - A UCI chess engine.
    Copyright (C) 2012-2014  Peter Österlund, peterosterlund2@gmail.com

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

/*
 * position.hpp
 *
 *  Created on: Feb 25, 2012
 *      Author: petero
 */

#ifndef POSITION_HPP_
#define POSITION_HPP_

#include "move.hpp"
#include "undoInfo.hpp"
#include "bitBoard.hpp"
#include "piece.hpp"
#include "material.hpp"
#include <algorithm>
#include <iostream>

/**
 * Stores the state of a chess position.
 * All required state is stored, except for all previous positions
 * since the last capture or pawn move. That state is only needed
 * for three-fold repetition draw detection, and is better stored
 * in a separate hash table.
 */
class Position {
public:
    /** Bit definitions for the castleMask bit mask. */
    static const int A1_CASTLE = 0; /** White long castle. */
    static const int H1_CASTLE = 1; /** White short castle. */
    static const int A8_CASTLE = 2; /** Black long castle. */
    static const int H8_CASTLE = 3; /** Black short castle. */

    /** Initialize board to empty position. */
    Position();

    /** Return the material identifier. */
    int materialId() const;

    bool isWhiteMove() const;
    void setWhiteMove(bool whiteMove);

    /** Return piece occupying a square. */
    int getPiece(int square) const;

    /** Set a square to a piece value. */
    void setPiece(int square, int piece);

    /** Bitmask describing castling rights. */
    int getCastleMask() const;
    void setCastleMask(int castleMask);

    /** En passant square, or -1 if no en passant possible. */
    int getEpSquare() const;

    void setEpSquare(int epSquare);

    int getKingSq(bool white) const;

    /** Apply a move to the current position. */
    void makeMove(const Move& move, UndoInfo& ui);

    void unMakeMove(const Move& move, const UndoInfo& ui);

    int getFullMoveCounter() const;
    void setFullMoveCounter(int fm);
    int getHalfMoveClock() const;
    void setHalfMoveClock(int hm);

    /** BitBoard for all squares occupied by a piece type. */
    U64 pieceTypeBB(Piece::Type piece) const;
    /** BitBoard for all squares occupied by several piece types. */
    template <typename Piece0, typename... Pieces> U64 pieceTypeBB(Piece0 piece0, Pieces... pieces) const;

    /** BitBoard for all squares occupied by white pieces. */
    U64 whiteBB() const;
    /** BitBoard for all squares occupied by black pieces. */
    U64 blackBB() const;
    /** BitBoard for all squares occupied by white or black pieces. */
    U64 colorBB(int wtm) const;

    /** BitBoard for all squares occupied by white and black pieces. */
    U64 occupiedBB() const;

    int wKingSq() const;
    int bKingSq() const;

    /** Return index in squares[] vector corresponding to (x,y). */
    static int getSquare(int x, int y);

    /** Return x position (file) corresponding to a square. */
    static int getX(int square);

    /** Return y position (rank) corresponding to a square. */
    static int getY(int square);

    /** Return true if (x,y) is a dark square. */
    static bool darkSquare(int x, int y);

    /** Initialize static data. */
    static void staticInitialize();

private:
    /** Move a non-pawn piece to an empty square. */
    void movePieceNotPawn(int from, int to);

    void removeCastleRights(int square);


    int wKingSq_, bKingSq_;  // Cached king positions

    int squares[64];

    // Bitboards
    U64 pieceTypeBB_[Piece::nPieceTypes];
    U64 whiteBB_, blackBB_;

    bool whiteMove;

    /** Number of half-moves since last 50-move reset. */
    int halfMoveClock;

    /** Game move number, starting from 1. */
    int fullMoveCounter;

    int castleMask;
    int epSquare;

    MatId matId;           // Cached material identifier
};

inline int
Position::materialId() const {
    return matId();
}

inline bool
Position::isWhiteMove() const {
    return whiteMove;
}

inline void
Position::setWhiteMove(bool whiteMove) {
    this->whiteMove = whiteMove;
}

inline int
Position::getPiece(int square) const {
    return squares[square];
}

inline int
Position::getCastleMask() const {
    return castleMask;
}

inline void
Position::setCastleMask(int castleMask) {
    this->castleMask = castleMask;
}

inline int
Position::getEpSquare() const {
    return epSquare;
}

inline void
Position::setEpSquare(int epSquare) {
    this->epSquare = epSquare;
}

inline int
Position::getKingSq(bool white) const {
    return white ? wKingSq() : bKingSq();
}

inline void
Position::unMakeMove(const Move& move, const UndoInfo& ui) {
    whiteMove = !whiteMove;
    int p = squares[move.to()];
    setPiece(move.from(), p);
    setPiece(move.to(), ui.capturedPiece);
    setCastleMask(ui.castleMask);
    setEpSquare(ui.epSquare);
    halfMoveClock = ui.halfMoveClock;
    bool wtm = whiteMove;
    if (move.promoteTo() != Piece::EMPTY) {
        p = wtm ? Piece::WPAWN : Piece::BPAWN;
        setPiece(move.from(), p);
    }
    if (!wtm)
        fullMoveCounter--;

    // Handle castling
    int king = wtm ? Piece::WKING : Piece::BKING;
    if (p == king) {
        int k0 = move.from();
        if (move.to() == k0 + 2) { // O-O
            movePieceNotPawn(k0 + 1, k0 + 3);
        } else if (move.to() == k0 - 2) { // O-O-O
            movePieceNotPawn(k0 - 1, k0 - 4);
        }
    }

    // Handle en passant
    if (move.to() == epSquare) {
        if (p == Piece::WPAWN) {
            setPiece(move.to() - 8, Piece::BPAWN);
        } else if (p == Piece::BPAWN) {
            setPiece(move.to() + 8, Piece::WPAWN);
        }
    }
}

inline int
Position::getSquare(int x, int y) {
    return y * 8 + x;
}

/** Return x position (file) corresponding to a square. */
inline int
Position::getX(int square) {
    return square & 7;
}

/** Return y position (rank) corresponding to a square. */
inline int
Position::getY(int square) {
    return square >> 3;
}

/** Return true if (x,y) is a dark square. */
inline bool
Position::darkSquare(int x, int y) {
    return (x & 1) == (y & 1);
}

inline void
Position::removeCastleRights(int square) {
    if (square == getSquare(0, 0)) {
        setCastleMask(castleMask & ~(1 << A1_CASTLE));
    } else if (square == getSquare(7, 0)) {
        setCastleMask(castleMask & ~(1 << H1_CASTLE));
    } else if (square == getSquare(0, 7)) {
        setCastleMask(castleMask & ~(1 << A8_CASTLE));
    } else if (square == getSquare(7, 7)) {
        setCastleMask(castleMask & ~(1 << H8_CASTLE));
    }
}

inline int Position::getFullMoveCounter() const {
    return fullMoveCounter;
}

inline void Position::setFullMoveCounter(int fm) {
    fullMoveCounter = fm;
}

inline int Position::getHalfMoveClock() const {
    return halfMoveClock;
}

inline void Position::setHalfMoveClock(int hm) {
    halfMoveClock = hm;
}

inline U64 Position::pieceTypeBB(Piece::Type piece) const {
    return pieceTypeBB_[piece];
}

template <typename Piece0, typename... Pieces>
inline U64 Position::pieceTypeBB(Piece0 piece0, Pieces... pieces) const {
    return pieceTypeBB(piece0) | pieceTypeBB(pieces...);
}

inline U64 Position::whiteBB() const {
    return whiteBB_;
}

inline U64 Position::blackBB() const {
    return blackBB_;
};

inline U64 Position::colorBB(int wtm) const {
    return wtm ? whiteBB_ : blackBB_;
}

inline U64 Position::occupiedBB() const {
    return whiteBB() | blackBB();
}

inline int Position::wKingSq() const {
    return wKingSq_;
}

inline int Position::bKingSq() const {
    return bKingSq_;
}

#endif /* POSITION_HPP_ */
