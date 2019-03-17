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
 * position.cpp
 *
 *  Created on: Feb 25, 2012
 *      Author: petero
 */

#include "position.hpp"


Position::Position() {
    for (int i = 0; i < 64; i++)
        squares[i] = Piece::EMPTY;
    for (int i = 0; i < Piece::nPieceTypes; i++)
        pieceTypeBB_[i] = 0;
    whiteBB_ = blackBB_ = 0;
    whiteMove = true;
    castleMask = 0;
    epSquare = -1;
    halfMoveClock = 0;
    fullMoveCounter = 1;
    matId = {};
    for (int sq = 0; sq < 64; sq++) {
        int p = squares[sq];
        matId.addPiece(p);
    }
    wKingSq_ = bKingSq_ = -1;
}

void
Position::setPiece(int square, int piece) {
    int removedPiece = squares[square];
    squares[square] = piece;

    // Update material identifier
    matId.removePiece(removedPiece);
    matId.addPiece(piece);

    // Update bitboards
    const U64 sqMask = 1ULL << square;
    pieceTypeBB_[removedPiece] &= ~sqMask;
    pieceTypeBB_[piece] |= sqMask;

    if (removedPiece != Piece::EMPTY) {
        if (Piece::isWhite(removedPiece)) {
            whiteBB_ &= ~sqMask;
        } else {
            blackBB_ &= ~sqMask;
        }
    }

    if (piece != Piece::EMPTY) {
        if (Piece::isWhite(piece)) {
            whiteBB_ |= sqMask;
            if (piece == Piece::WKING)
                wKingSq_ = square;
        } else {
            blackBB_ |= sqMask;
            if (piece == Piece::BKING)
                bKingSq_ = square;
        }
    }
}

void
Position::makeMove(const Move& move, UndoInfo& ui) {
    ui.capturedPiece = squares[move.to()];
    ui.castleMask = castleMask;
    ui.epSquare = epSquare;
    ui.halfMoveClock = halfMoveClock;
    bool wtm = whiteMove;

    const int p = squares[move.from()];
    int capP = squares[move.to()];
    U64 fromMask = 1ULL << move.from();

    int prevEpSquare = epSquare;
    setEpSquare(-1);

    if ((capP != Piece::EMPTY) || ((pieceTypeBB(Piece::WPAWN, Piece::BPAWN) & fromMask) != 0)) {
        halfMoveClock = 0;

        // Handle en passant and epSquare
        if (p == Piece::WPAWN) {
            if (move.to() - move.from() == 2 * 8) {
                int x = getX(move.to());
                if (BitBoard::epMaskW[x] & pieceTypeBB(Piece::BPAWN))
                    setEpSquare(move.from() + 8);
            } else if (move.to() == prevEpSquare) {
                setPiece(move.to() - 8, Piece::EMPTY);
            }
        } else if (p == Piece::BPAWN) {
            if (move.to() - move.from() == -2 * 8) {
                int x = getX(move.to());
                if (BitBoard::epMaskB[x] & pieceTypeBB(Piece::WPAWN))
                    setEpSquare(move.from() - 8);
            } else if (move.to() == prevEpSquare) {
                setPiece(move.to() + 8, Piece::EMPTY);
            }
        }

        if ((pieceTypeBB(Piece::WKING, Piece::BKING) & fromMask) != 0) {
            if (wtm) {
                setCastleMask(castleMask & ~(1 << A1_CASTLE));
                setCastleMask(castleMask & ~(1 << H1_CASTLE));
            } else {
                setCastleMask(castleMask & ~(1 << A8_CASTLE));
                setCastleMask(castleMask & ~(1 << H8_CASTLE));
            }
        }

        // Perform move
        setPiece(move.from(), Piece::EMPTY);
        // Handle promotion
        if (move.promoteTo() != Piece::EMPTY) {
            setPiece(move.to(), move.promoteTo());
        } else {
            setPiece(move.to(), p);
        }
    } else {
        halfMoveClock++;

        // Handle castling
        if ((pieceTypeBB(Piece::WKING, Piece::BKING) & fromMask) != 0) {
            int k0 = move.from();
            if (move.to() == k0 + 2) { // O-O
                movePieceNotPawn(k0 + 3, k0 + 1);
            } else if (move.to() == k0 - 2) { // O-O-O
                movePieceNotPawn(k0 - 4, k0 - 1);
            }
            if (wtm) {
                setCastleMask(castleMask & ~(1 << A1_CASTLE));
                setCastleMask(castleMask & ~(1 << H1_CASTLE));
            } else {
                setCastleMask(castleMask & ~(1 << A8_CASTLE));
                setCastleMask(castleMask & ~(1 << H8_CASTLE));
            }
        }

        // Perform move
        movePieceNotPawn(move.from(), move.to());
    }
    if (wtm) {
        // Update castling rights when rook moves
        if ((BitBoard::maskCorners & fromMask) != 0) {
            if (p == Piece::WROOK)
                removeCastleRights(move.from());
        }
        if ((BitBoard::maskCorners & (1ULL << move.to())) != 0) {
            if (capP == Piece::BROOK)
                removeCastleRights(move.to());
        }
    } else {
        fullMoveCounter++;
        // Update castling rights when rook moves
        if ((BitBoard::maskCorners & fromMask) != 0) {
            if (p == Piece::BROOK)
                removeCastleRights(move.from());
        }
        if ((BitBoard::maskCorners & (1ULL << move.to())) != 0) {
            if (capP == Piece::WROOK)
                removeCastleRights(move.to());
        }
    }

    whiteMove = !wtm;
}

void
Position::movePieceNotPawn(int from, int to) {
    const int piece = squares[from];

    squares[from] = Piece::EMPTY;
    squares[to] = piece;

    const U64 sqMaskF = 1ULL << from;
    const U64 sqMaskT = 1ULL << to;
    pieceTypeBB_[piece] &= ~sqMaskF;
    pieceTypeBB_[piece] |= sqMaskT;
    if (Piece::isWhite(piece)) {
        whiteBB_ &= ~sqMaskF;
        whiteBB_ |= sqMaskT;
        if (piece == Piece::WKING)
            wKingSq_ = to;
    } else {
        blackBB_ &= ~sqMaskF;
        blackBB_ |= sqMaskT;
        if (piece == Piece::BKING)
            bKingSq_ = to;
    }
}


