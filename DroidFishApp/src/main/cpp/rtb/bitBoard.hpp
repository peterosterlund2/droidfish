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
 * bitBoard.hpp
 *
 *  Created on: Feb 25, 2012
 *      Author: petero
 */

#ifndef BITBOARD_HPP_
#define BITBOARD_HPP_

#include "util.hpp"
#include <vector>

enum Square {
    A1, B1, C1, D1, E1, F1, G1, H1,
    A2, B2, C2, D2, E2, F2, G2, H2,
    A3, B3, C3, D3, E3, F3, G3, H3,
    A4, B4, C4, D4, E4, F4, G4, H4,
    A5, B5, C5, D5, E5, F5, G5, H5,
    A6, B6, C6, D6, E6, F6, G6, H6,
    A7, B7, C7, D7, E7, F7, G7, H7,
    A8, B8, C8, D8, E8, F8, G8, H8
};

class BitBoard {
public:
    /** Squares attacked by a king on a given square. */
    static U64 kingAttacks[64];
    static U64 knightAttacks[64];
    static U64 wPawnAttacks[64], bPawnAttacks[64];

    static const U64 maskAToGFiles = 0x7F7F7F7F7F7F7F7FULL;
    static const U64 maskBToHFiles = 0xFEFEFEFEFEFEFEFEULL;
    static const U64 maskAToFFiles = 0x3F3F3F3F3F3F3F3FULL;
    static const U64 maskCToHFiles = 0xFCFCFCFCFCFCFCFCULL;

    static const U64 maskFile[8];

    // Masks for squares where enemy pawn can capture en passant, indexed by file
    static U64 epMaskW[8], epMaskB[8];

    static const U64 maskRow1      = 0x00000000000000FFULL;
    static const U64 maskRow2      = 0x000000000000FF00ULL;
    static const U64 maskRow3      = 0x0000000000FF0000ULL;
    static const U64 maskRow4      = 0x00000000FF000000ULL;
    static const U64 maskRow5      = 0x000000FF00000000ULL;
    static const U64 maskRow6      = 0x0000FF0000000000ULL;
    static const U64 maskRow7      = 0x00FF000000000000ULL;
    static const U64 maskRow8      = 0xFF00000000000000ULL;
    static const U64 maskRow1Row8  = 0xFF000000000000FFULL;

    static const U64 maskDarkSq    = 0xAA55AA55AA55AA55ULL;
    static const U64 maskLightSq   = 0x55AA55AA55AA55AAULL;

    static const U64 maskCorners   = 0x8100000000000081ULL;


    static U64 bishopAttacks(int sq, U64 occupied) {
        return bTables[sq][(int)(((occupied & bMasks[sq]) * bMagics[sq]) >> (64 - bBits[sq]))];
    }

    static U64 rookAttacks(int sq, U64 occupied) {
        return rTables[sq][(int)(((occupied & rMasks[sq]) * rMagics[sq]) >> (64 - rBits[sq]))];
    }

    static U64 squaresBetween[64][64];

    static int getDirection(int from, int to) {
        int offs = to + (to|7) - from - (from|7) + 0x77;
        return dirTable[offs];
    }

    static int numberOfTrailingZeros(U64 mask) {
        return trailingZ[(int)(((mask & -mask) * 0x07EDD5E59A4E28C2ULL) >> 58)];
    }

    /** Return number of 1 bits in mask. */
    static int bitCount(U64 mask) {
        const U64 k1 = 0x5555555555555555ULL;
        const U64 k2 = 0x3333333333333333ULL;
        const U64 k4 = 0x0f0f0f0f0f0f0f0fULL;
        const U64 kf = 0x0101010101010101ULL;
        U64 t = mask;
        t -= (t >> 1) & k1;
        t = (t & k2) + ((t >> 2) & k2);
        t = (t + (t >> 4)) & k4;
        t = (t * kf) >> 56;
        return (int)t;
    }

    /** Initialize static data. */
    static void staticInitialize();

private:
    static U64* rTables[64];
    static U64 rMasks[64];
    static int rBits[64];
    static const U64 rMagics[64];

    static U64* bTables[64];
    static U64 bMasks[64];
    static const int bBits[64];
    static const U64 bMagics[64];

    static std::vector<U64> tableData;

    static const S8 dirTable[];
    static const S8 kingDistTable[];
    static const S8 taxiDistTable[];
    static const int trailingZ[64];
};


#endif /* BITBOARD_HPP_ */
