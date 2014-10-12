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
 * moveGen.cpp
 *
 *  Created on: Feb 25, 2012
 *      Author: petero
 */

#include "moveGen.hpp"


template void MoveGen::pseudoLegalMoves<true>(const Position& pos, MoveList& moveList);
template void MoveGen::pseudoLegalMoves<false>(const Position& pos, MoveList& moveList);

template <bool wtm>
void
MoveGen::pseudoLegalMoves(const Position& pos, MoveList& moveList) {
    typedef ColorTraits<wtm> MyColor;
    const U64 occupied = pos.occupiedBB();

    // Queen moves
    U64 squares = pos.pieceTypeBB(MyColor::QUEEN);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = (BitBoard::rookAttacks(sq, occupied) | BitBoard::bishopAttacks(sq, occupied)) & ~pos.colorBB(wtm);
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // Rook moves
    squares = pos.pieceTypeBB(MyColor::ROOK);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = BitBoard::rookAttacks(sq, occupied) & ~pos.colorBB(wtm);
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // Bishop moves
    squares = pos.pieceTypeBB(MyColor::BISHOP);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = BitBoard::bishopAttacks(sq, occupied) & ~pos.colorBB(wtm);
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // King moves
    {
        int sq = pos.getKingSq(wtm);
        U64 m = BitBoard::kingAttacks[sq] & ~pos.colorBB(wtm);
        addMovesByMask(moveList, sq, m);
        const int k0 = wtm ? 4 : 60;
        if (sq == k0) {
            const U64 OO_SQ = wtm ? 0x60ULL : 0x6000000000000000ULL;
            const U64 OOO_SQ = wtm ? 0xEULL : 0xE00000000000000ULL;
            const int hCastle = wtm ? Position::H1_CASTLE : Position::H8_CASTLE;
            const int aCastle = wtm ? Position::A1_CASTLE : Position::A8_CASTLE;
            if (((pos.getCastleMask() & (1 << hCastle)) != 0) &&
                ((OO_SQ & occupied) == 0) &&
                (pos.getPiece(k0 + 3) == MyColor::ROOK) &&
                !sqAttacked(pos, k0) &&
                !sqAttacked(pos, k0 + 1)) {
                moveList.addMove(k0, k0 + 2, Piece::EMPTY);
            }
            if (((pos.getCastleMask() & (1 << aCastle)) != 0) &&
                ((OOO_SQ & occupied) == 0) &&
                (pos.getPiece(k0 - 4) == MyColor::ROOK) &&
                !sqAttacked(pos, k0) &&
                !sqAttacked(pos, k0 - 1)) {
                moveList.addMove(k0, k0 - 2, Piece::EMPTY);
            }
        }
    }

    // Knight moves
    U64 knights = pos.pieceTypeBB(MyColor::KNIGHT);
    while (knights != 0) {
        int sq = BitBoard::numberOfTrailingZeros(knights);
        U64 m = BitBoard::knightAttacks[sq] & ~pos.colorBB(wtm);
        addMovesByMask(moveList, sq, m);
        knights &= knights-1;
    }

    // Pawn moves
    const U64 pawns = pos.pieceTypeBB(MyColor::PAWN);
    const int epSquare = pos.getEpSquare();
    const U64 epMask = (epSquare >= 0) ? (1ULL << epSquare) : 0ULL;
    if (wtm) {
        U64 m = (pawns << 8) & ~occupied;
        addPawnMovesByMask<wtm>(moveList, m, -8, true);
        m = ((m & BitBoard::maskRow3) << 8) & ~occupied;
        addPawnDoubleMovesByMask(moveList, m, -16);

        m = (pawns << 7) & BitBoard::maskAToGFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, -7, true);

        m = (pawns << 9) & BitBoard::maskBToHFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, -9, true);
    } else {
        U64 m = (pawns >> 8) & ~occupied;
        addPawnMovesByMask<wtm>(moveList, m, 8, true);
        m = ((m & BitBoard::maskRow6) >> 8) & ~occupied;
        addPawnDoubleMovesByMask(moveList, m, 16);

        m = (pawns >> 9) & BitBoard::maskAToGFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, 9, true);

        m = (pawns >> 7) & BitBoard::maskBToHFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, 7, true);
    }
}

template void MoveGen::checkEvasions<true>(const Position& pos, MoveList& moveList);
template void MoveGen::checkEvasions<false>(const Position& pos, MoveList& moveList);

template <bool wtm>
void
MoveGen::checkEvasions(const Position& pos, MoveList& moveList) {
    typedef ColorTraits<wtm> MyColor;
    typedef ColorTraits<!wtm> OtherColor;
    const U64 occupied = pos.occupiedBB();

    const int kingSq = pos.getKingSq(wtm);
    U64 kingThreats = pos.pieceTypeBB(OtherColor::KNIGHT) & BitBoard::knightAttacks[kingSq];
    U64 rookPieces = pos.pieceTypeBB(OtherColor::ROOK, OtherColor::QUEEN);
    if (rookPieces != 0)
        kingThreats |= rookPieces & BitBoard::rookAttacks(kingSq, occupied);
    U64 bishPieces = pos.pieceTypeBB(OtherColor::BISHOP, OtherColor::QUEEN);
    if (bishPieces != 0)
        kingThreats |= bishPieces & BitBoard::bishopAttacks(kingSq, occupied);
    const U64 myPawnAttacks = wtm ? BitBoard::wPawnAttacks[kingSq] : BitBoard::bPawnAttacks[kingSq];
    kingThreats |= pos.pieceTypeBB(OtherColor::PAWN) & myPawnAttacks;
    U64 validTargets = 0;
    if ((kingThreats != 0) && ((kingThreats & (kingThreats-1)) == 0)) { // Exactly one attacking piece
        int threatSq = BitBoard::numberOfTrailingZeros(kingThreats);
        validTargets = kingThreats | BitBoard::squaresBetween[kingSq][threatSq];
    }
    validTargets |= pos.pieceTypeBB(OtherColor::KING);
    // Queen moves
    U64 squares = pos.pieceTypeBB(MyColor::QUEEN);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = (BitBoard::rookAttacks(sq, occupied) | BitBoard::bishopAttacks(sq, occupied)) &
                    ~pos.colorBB(wtm) & validTargets;
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // Rook moves
    squares = pos.pieceTypeBB(MyColor::ROOK);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = BitBoard::rookAttacks(sq, occupied) & ~pos.colorBB(wtm) & validTargets;
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // Bishop moves
    squares = pos.pieceTypeBB(MyColor::BISHOP);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = BitBoard::bishopAttacks(sq, occupied) & ~pos.colorBB(wtm) & validTargets;
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // King moves
    {
        int sq = pos.getKingSq(wtm);
        U64 m = BitBoard::kingAttacks[sq] & ~pos.colorBB(wtm);
        addMovesByMask(moveList, sq, m);
    }

    // Knight moves
    U64 knights = pos.pieceTypeBB(MyColor::KNIGHT);
    while (knights != 0) {
        int sq = BitBoard::numberOfTrailingZeros(knights);
        U64 m = BitBoard::knightAttacks[sq] & ~pos.colorBB(wtm) & validTargets;
        addMovesByMask(moveList, sq, m);
        knights &= knights-1;
    }

    // Pawn moves
    const U64 pawns = pos.pieceTypeBB(MyColor::PAWN);
    const int epSquare = pos.getEpSquare();
    const U64 epMask = (epSquare >= 0) ? (1ULL << epSquare) : 0ULL;
    if (wtm) {
        U64 m = (pawns << 8) & ~occupied;
        addPawnMovesByMask<wtm>(moveList, m & validTargets, -8, true);
        m = ((m & BitBoard::maskRow3) << 8) & ~occupied;
        addPawnDoubleMovesByMask(moveList, m & validTargets, -16);

        m = (pawns << 7) & BitBoard::maskAToGFiles & ((pos.colorBB(!wtm) & validTargets) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, -7, true);

        m = (pawns << 9) & BitBoard::maskBToHFiles & ((pos.colorBB(!wtm) & validTargets) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, -9, true);
    } else {
        U64 m = (pawns >> 8) & ~occupied;
        addPawnMovesByMask<wtm>(moveList, m & validTargets, 8, true);
        m = ((m & BitBoard::maskRow6) >> 8) & ~occupied;
        addPawnDoubleMovesByMask(moveList, m & validTargets, 16);

        m = (pawns >> 9) & BitBoard::maskAToGFiles & ((pos.colorBB(!wtm) & validTargets) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, 9, true);

        m = (pawns >> 7) & BitBoard::maskBToHFiles & ((pos.colorBB(!wtm) & validTargets) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, 7, true);
    }
}

template void MoveGen::pseudoLegalCaptures<true>(const Position& pos, MoveList& moveList);
template void MoveGen::pseudoLegalCaptures<false>(const Position& pos, MoveList& moveList);

template <bool wtm>
void
MoveGen::pseudoLegalCaptures(const Position& pos, MoveList& moveList) {
    typedef ColorTraits<wtm> MyColor;
    const U64 occupied = pos.occupiedBB();

    // Queen moves
    U64 squares = pos.pieceTypeBB(MyColor::QUEEN);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = (BitBoard::rookAttacks(sq, occupied) | BitBoard::bishopAttacks(sq, occupied)) & pos.colorBB(!wtm);
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // Rook moves
    squares = pos.pieceTypeBB(MyColor::ROOK);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = BitBoard::rookAttacks(sq, occupied) & pos.colorBB(!wtm);
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // Bishop moves
    squares = pos.pieceTypeBB(MyColor::BISHOP);
    while (squares != 0) {
        int sq = BitBoard::numberOfTrailingZeros(squares);
        U64 m = BitBoard::bishopAttacks(sq, occupied) & pos.colorBB(!wtm);
        addMovesByMask(moveList, sq, m);
        squares &= squares-1;
    }

    // Knight moves
    U64 knights = pos.pieceTypeBB(MyColor::KNIGHT);
    while (knights != 0) {
        int sq = BitBoard::numberOfTrailingZeros(knights);
        U64 m = BitBoard::knightAttacks[sq] & pos.colorBB(!wtm);
        addMovesByMask(moveList, sq, m);
        knights &= knights-1;
    }

    // King moves
    int sq = pos.getKingSq(wtm);
    U64 m = BitBoard::kingAttacks[sq] & pos.colorBB(!wtm);
    addMovesByMask(moveList, sq, m);

    // Pawn moves
    const U64 pawns = pos.pieceTypeBB(MyColor::PAWN);
    const int epSquare = pos.getEpSquare();
    const U64 epMask = (epSquare >= 0) ? (1ULL << epSquare) : 0ULL;
    if (wtm) {
        m = (pawns << 8) & ~occupied;
        m &= BitBoard::maskRow8;
        addPawnMovesByMask<wtm>(moveList, m, -8, false);

        m = (pawns << 7) & BitBoard::maskAToGFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, -7, false);
        m = (pawns << 9) & BitBoard::maskBToHFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, -9, false);
    } else {
        m = (pawns >> 8) & ~occupied;
        m &= BitBoard::maskRow1;
        addPawnMovesByMask<wtm>(moveList, m, 8, false);

        m = (pawns >> 9) & BitBoard::maskAToGFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, 9, false);
        m = (pawns >> 7) & BitBoard::maskBToHFiles & (pos.colorBB(!wtm) | epMask);
        addPawnMovesByMask<wtm>(moveList, m, 7, false);
    }
}

bool
MoveGen::isLegal(Position& pos, const Move& m, bool isInCheck) {
    UndoInfo ui;
    int kSq = pos.getKingSq(pos.isWhiteMove());
    const int epSquare = pos.getEpSquare();
    if (isInCheck) {
        if ((m.from() != kSq) && (m.to() != epSquare)) {
            U64 occupied = pos.occupiedBB();
            U64 toMask = 1ULL << m.to();
            Piece::Type knight = pos.isWhiteMove() ? Piece::BKNIGHT : Piece::WKNIGHT;
            if (((BitBoard::rookAttacks(kSq, occupied) & toMask) == 0) &&
                ((BitBoard::bishopAttacks(kSq, occupied) & toMask) == 0) &&
                ((BitBoard::knightAttacks[kSq] & pos.pieceTypeBB(knight) & toMask) == 0))
                return false;
        }
        pos.makeMove(m, ui);
        bool legal = !canTakeKing(pos);
        pos.unMakeMove(m, ui);
        return legal;
    } else {
        if (m.from() == kSq) {
            U64 occupied = pos.occupiedBB() & ~(1ULL<<m.from());
            return !MoveGen::sqAttacked(pos, m.to(), occupied);
        } else {
            if (m.to() != epSquare) {
                U64 occupied = pos.occupiedBB();
                U64 fromMask = 1ULL << m.from();
                if (((BitBoard::rookAttacks(kSq, occupied) & fromMask) == 0) &&
                    ((BitBoard::bishopAttacks(kSq, occupied) & fromMask) == 0))
                    return true;
                else if (BitBoard::getDirection(kSq, m.from()) == BitBoard::getDirection(kSq, m.to()))
                    return true;
            }
            pos.makeMove(m, ui);
            bool legal = !canTakeKing(pos);
            pos.unMakeMove(m, ui);
            return legal;
        }
    }
}
