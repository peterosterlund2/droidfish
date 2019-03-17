/*
    Texel - A UCI chess engine.
    Copyright (C) 2012-2013  Peter Österlund, peterosterlund2@gmail.com

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
 * piece.hpp
 *
 *  Created on: Feb 25, 2012
 *      Author: petero
 */

#ifndef PIECE_HPP_
#define PIECE_HPP_

/**
 * Constants for different piece types.
 */
class Piece {
public:
    enum Type {
      EMPTY = 0,
      WKING = 1,
      WQUEEN = 2,
      WROOK = 3,
      WBISHOP = 4,
      WKNIGHT = 5,
      WPAWN = 6,

      BKING = 7,
      BQUEEN = 8,
      BROOK = 9,
      BBISHOP = 10,
      BKNIGHT = 11,
      BPAWN = 12,

      nPieceTypes = 13
    };

    /**
     * Return true if p is a white piece, false otherwise.
     * Note that if p is EMPTY, an unspecified value is returned.
     */
    static bool isWhite(int pType);
};

template <bool wtm> struct ColorTraits {
};

template<> struct ColorTraits<true> {
    static const Piece::Type KING   = Piece::WKING;
    static const Piece::Type QUEEN  = Piece::WQUEEN;
    static const Piece::Type ROOK   = Piece::WROOK;
    static const Piece::Type BISHOP = Piece::WBISHOP;
    static const Piece::Type KNIGHT = Piece::WKNIGHT;
    static const Piece::Type PAWN   = Piece::WPAWN;
};

template<> struct ColorTraits<false> {
    static const Piece::Type KING   = Piece::BKING;
    static const Piece::Type QUEEN  = Piece::BQUEEN;
    static const Piece::Type ROOK   = Piece::BROOK;
    static const Piece::Type BISHOP = Piece::BBISHOP;
    static const Piece::Type KNIGHT = Piece::BKNIGHT;
    static const Piece::Type PAWN   = Piece::BPAWN;
};

inline bool
Piece::isWhite(int pType) {
    return pType < BKING;
}

#endif /* PIECE_HPP_ */
