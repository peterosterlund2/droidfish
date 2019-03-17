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
 * bitBoard.cpp
 *
 *  Created on: Feb 25, 2012
 *      Author: petero
 */

#include "bitBoard.hpp"
#include "position.hpp"
#include <cassert>
#include <iostream>

U64 BitBoard::kingAttacks[64];
U64 BitBoard::knightAttacks[64];
U64 BitBoard::wPawnAttacks[64];
U64 BitBoard::bPawnAttacks[64];

const U64 BitBoard::maskFile[8] = {
    0x0101010101010101ULL,
    0x0202020202020202ULL,
    0x0404040404040404ULL,
    0x0808080808080808ULL,
    0x1010101010101010ULL,
    0x2020202020202020ULL,
    0x4040404040404040ULL,
    0x8080808080808080ULL
};

U64 BitBoard::epMaskW[8];
U64 BitBoard::epMaskB[8];

const U64 BitBoard::maskRow1;
const U64 BitBoard::maskRow2;
const U64 BitBoard::maskRow3;
const U64 BitBoard::maskRow4;
const U64 BitBoard::maskRow5;
const U64 BitBoard::maskRow6;
const U64 BitBoard::maskRow7;
const U64 BitBoard::maskRow8;
const U64 BitBoard::maskRow1Row8;
const U64 BitBoard::maskDarkSq;
const U64 BitBoard::maskLightSq;
const U64 BitBoard::maskCorners;

U64* BitBoard::rTables[64];
U64 BitBoard::rMasks[64];
int BitBoard::rBits[64] = { 12, 11, 11, 11, 11, 11, 11, 12,
                            11, 10, 10, 11, 10, 10, 10, 11,
                            11, 10, 10, 10, 10, 10, 10, 11,
                            11, 10, 10, 10, 10, 10, 10, 11,
                            11, 10, 10, 10, 10, 10, 10, 11,
                            11, 10, 10, 11, 10, 10, 10, 11,
                            10,  9,  9,  9,  9,  9, 10, 10,
                            11, 10, 10, 10, 10, 11, 10, 11 };
const U64 BitBoard::rMagics[64] = {
    0x19a80065ff2bffffULL, 0x3fd80075ffebffffULL, 0x4010000df6f6fffeULL, 0x0050001faffaffffULL,
    0x0050028004ffffb0ULL, 0x7f600280089ffff1ULL, 0x7f5000b0029ffffcULL, 0x5b58004848a7fffaULL,
    0x002a90005547ffffULL, 0x000050007f13ffffULL, 0x007fa0006013ffffULL, 0x006a9005656fffffULL,
    0x007f600f600affffULL, 0x007ec007e6bfffe2ULL, 0x007ec003eebffffbULL, 0x0071d002382fffdaULL,
    0x009f803000e7fffaULL, 0x00680030008bffffULL, 0x00606060004f3ffcULL, 0x001a00600bff9ffdULL,
    0x000d006005ff9fffULL, 0x0001806003005fffULL, 0x00000300040bfffaULL, 0x000192500065ffeaULL,
    0x00fff112d0006800ULL, 0x007ff037d000c004ULL, 0x003fd062001a3ff8ULL, 0x00087000600e1ffcULL,
    0x000fff0100100804ULL, 0x0007ff0100080402ULL, 0x0003ffe0c0060003ULL, 0x0001ffd53000d300ULL,
    0x00fffd3000600061ULL, 0x007fff7f95900040ULL, 0x003fff8c00600060ULL, 0x001ffe2587a01860ULL,
    0x000fff3fbf40180cULL, 0x0007ffc73f400c06ULL, 0x0003ff86d2c01405ULL, 0x0001fffeaa700100ULL,
    0x00fffdfdd8005000ULL, 0x007fff80ebffb000ULL, 0x003fffdf603f6000ULL, 0x001fffe050405000ULL,
    0x000fff400700c00cULL, 0x0007ff6007bf600aULL, 0x0003ffeebffec005ULL, 0x0001fffdf3feb001ULL,
    0x00ffff39ff484a00ULL, 0x007fff3fff486300ULL, 0x003fff99ffac2e00ULL, 0x001fff31ff2a6a00ULL,
    0x000fff19ff15b600ULL, 0x0007fff5fff28600ULL, 0x0003fffddffbfee0ULL, 0x0001fff5f63c96a0ULL,
    0x00ffff5dff65cfb6ULL, 0x007fffbaffd1c5aeULL, 0x003fff71ff6cbceaULL, 0x001fffd9ffd4756eULL,
    0x000ffff5fff338e6ULL, 0x0007fffdfffe24f6ULL, 0x0003ffef27eebe74ULL, 0x0001ffff23ff605eULL
};

U64* BitBoard::bTables[64];
U64 BitBoard::bMasks[64];
const int BitBoard::bBits[64] = { 5, 4, 5, 5, 5, 5, 4, 5,
                                  4, 4, 5, 5, 5, 5, 4, 4,
                                  4, 4, 7, 7, 7, 7, 4, 4,
                                  5, 5, 7, 9, 9, 7, 5, 5,
                                  5, 5, 7, 9, 9, 7, 5, 5,
                                  4, 4, 7, 7, 7, 7, 4, 4,
                                  4, 4, 5, 5, 5, 5, 4, 4,
                                  5, 4, 5, 5, 5, 5, 4, 5 };
const U64 BitBoard::bMagics[64] = {
    0x0006eff5367ff600ULL, 0x00345835ba77ff2bULL, 0x00145f68a3f5dab6ULL, 0x003a1863fb56f21dULL,
    0x0012eb6bfe9d93cdULL, 0x000d82827f3420d6ULL, 0x00074bcd9c7fec97ULL, 0x000034fe99f9ffffULL,
    0x0000746f8d6717f6ULL, 0x00003acb32e1a3f7ULL, 0x0000185daf1ffb8aULL, 0x00003a1867f17067ULL,
    0x0000038ee0ccf92eULL, 0x000002a2b7ff926eULL, 0x000006c9aa93ff14ULL, 0x00000399b5e5bf87ULL,
    0x00400f342c951ffcULL, 0x0020230579ed8ff0ULL, 0x007b008a0077dbfdULL, 0x001d00010c13fd46ULL,
    0x00040022031c1ffbULL, 0x000fa00fd1cbff79ULL, 0x000400a4bc9affdfULL, 0x000200085e9cffdaULL,
    0x002a14560a3dbfbdULL, 0x000a0a157b9eafd1ULL, 0x00060600fd002ffaULL, 0x004006000c009010ULL,
    0x001a002042008040ULL, 0x001a00600fd1ffc0ULL, 0x000d0ace50bf3f8dULL, 0x000183a48434efd1ULL,
    0x001fbd7670982a0dULL, 0x000fe24301d81a0fULL, 0x0007fbf82f040041ULL, 0x000040c800008200ULL,
    0x007fe17018086006ULL, 0x003b7ddf0ffe1effULL, 0x001f92f861df4a0aULL, 0x000fd713ad98a289ULL,
    0x000fd6aa751e400cULL, 0x0007f2a63ae9600cULL, 0x0003ff7dfe0e3f00ULL, 0x000003fd2704ce04ULL,
    0x00007fc421601d40ULL, 0x007fff5f70900120ULL, 0x003fa66283556403ULL, 0x001fe31969aec201ULL,
    0x0007fdfc18ac14bbULL, 0x0003fb96fb568a47ULL, 0x000003f72ea4954dULL, 0x00000003f8dc0383ULL,
    0x0000007f3a814490ULL, 0x00007dc5c9cf62a6ULL, 0x007f23d3342897acULL, 0x003fee36eee1565cULL,
    0x0003ff3e99fcccc7ULL, 0x000003ecfcfac5feULL, 0x00000003f97f7453ULL, 0x0000000003f8dc03ULL,
    0x000000007efa8146ULL, 0x0000007ed3e2ef60ULL, 0x00007f47243adcd6ULL, 0x007fb65afabfb3b5ULL
};

std::vector<U64> BitBoard::tableData;

const S8 BitBoard::dirTable[] = {
       -9,  0,  0,  0,  0,  0,  0, -8,  0,  0,  0,  0,  0,  0, -7,
    0,  0, -9,  0,  0,  0,  0,  0, -8,  0,  0,  0,  0,  0, -7,  0,
    0,  0,  0, -9,  0,  0,  0,  0, -8,  0,  0,  0,  0, -7,  0,  0,
    0,  0,  0,  0, -9,  0,  0,  0, -8,  0,  0,  0, -7,  0,  0,  0,
    0,  0,  0,  0,  0, -9,  0,  0, -8,  0,  0, -7,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0, -9,-17, -8,-15, -7,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,-10, -9, -8, -7, -6,  0,  0,  0,  0,  0,
    0, -1, -1, -1, -1, -1, -1, -1,  0,  1,  1,  1,  1,  1,  1,  1,
    0,  0,  0,  0,  0,  0,  6,  7,  8,  9, 10,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  7, 15,  8, 17,  9,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  7,  0,  0,  8,  0,  0,  9,  0,  0,  0,  0,
    0,  0,  0,  0,  7,  0,  0,  0,  8,  0,  0,  0,  9,  0,  0,  0,
    0,  0,  0,  7,  0,  0,  0,  0,  8,  0,  0,  0,  0,  9,  0,  0,
    0,  0,  7,  0,  0,  0,  0,  0,  8,  0,  0,  0,  0,  0,  9,  0,
    0,  7,  0,  0,  0,  0,  0,  0,  8,  0,  0,  0,  0,  0,  0,  9
};

const S8 BitBoard::kingDistTable[] = {
       7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    0, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7,
    0, 7, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 7,
    0, 7, 6, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 3, 3, 3, 3, 3, 3, 3, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 3, 2, 1, 1, 1, 2, 3, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 3, 2, 1, 1, 1, 2, 3, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 3, 3, 3, 3, 3, 3, 3, 4, 5, 6, 7,
    0, 7, 6, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 6, 7,
    0, 7, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 7,
    0, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7,
    0, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
};

const S8 BitBoard::taxiDistTable[] = {
      14,13,12,11,10, 9, 8, 7, 8, 9,10,11,12,13,14,
    0,13,12,11,10, 9, 8, 7, 6, 7, 8, 9,10,11,12,13,
    0,12,11,10, 9, 8, 7, 6, 5, 6, 7, 8, 9,10,11,12,
    0,11,10, 9, 8, 7, 6, 5, 4, 5, 6, 7, 8, 9,10,11,
    0,10, 9, 8, 7, 6, 5, 4, 3, 4, 5, 6, 7, 8, 9,10,
    0, 9, 8, 7, 6, 5, 4, 3, 2, 3, 4, 5, 6, 7, 8, 9,
    0, 8, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 8,
    0, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7,
    0, 8, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 8,
    0, 9, 8, 7, 6, 5, 4, 3, 2, 3, 4, 5, 6, 7, 8, 9,
    0,10, 9, 8, 7, 6, 5, 4, 3, 4, 5, 6, 7, 8, 9,10,
    0,11,10, 9, 8, 7, 6, 5, 4, 5, 6, 7, 8, 9,10,11,
    0,12,11,10, 9, 8, 7, 6, 5, 6, 7, 8, 9,10,11,12,
    0,13,12,11,10, 9, 8, 7, 6, 7, 8, 9,10,11,12,13,
    0,14,13,12,11,10, 9, 8, 7, 8, 9,10,11,12,13,14
};

const int BitBoard::trailingZ[64] = {
    63,  0, 58,  1, 59, 47, 53,  2,
    60, 39, 48, 27, 54, 33, 42,  3,
    61, 51, 37, 40, 49, 18, 28, 20,
    55, 30, 34, 11, 43, 14, 22,  4,
    62, 57, 46, 52, 38, 26, 32, 41,
    50, 36, 17, 19, 29, 10, 13, 21,
    56, 45, 25, 31, 35, 16,  9, 12,
    44, 24, 15,  8, 23,  7,  6,  5
};

U64 BitBoard::squaresBetween[64][64];

static U64 createPattern(int i, U64 mask) {
    U64 ret = 0ULL;
    for (int j = 0; ; j++) {
        U64 nextMask = mask & (mask - 1);
        U64 bit = mask ^ nextMask;
        if ((i & (1ULL << j)) != 0)
            ret |= bit;
        mask = nextMask;
        if (mask == 0)
            break;
    }
    return ret;
}

static U64 addRay(U64 mask, int x, int y, int dx, int dy,
                  U64 occupied, bool inner) {
    int lo = inner ? 1 : 0;
    int hi = inner ? 6 : 7;
    while (true) {
        if (dx != 0) {
            x += dx; if ((x < lo) || (x > hi)) break;
        }
        if (dy != 0) {
            y += dy; if ((y < lo) || (y > hi)) break;
        }
        int sq = Position::getSquare(x, y);
        mask |= 1ULL << sq;
        if ((occupied & (1ULL << sq)) != 0)
            break;
    }
    return mask;
}

static U64 addRookRays(int x, int y, U64 occupied, bool inner) {
    U64 mask = 0;
    mask = addRay(mask, x, y,  1,  0, occupied, inner);
    mask = addRay(mask, x, y, -1,  0, occupied, inner);
    mask = addRay(mask, x, y,  0,  1, occupied, inner);
    mask = addRay(mask, x, y,  0, -1, occupied, inner);
    return mask;
}

static U64 addBishopRays(int x, int y, U64 occupied, bool inner) {
    U64 mask = 0;
    mask = addRay(mask, x, y,  1,  1, occupied, inner);
    mask = addRay(mask, x, y, -1, -1, occupied, inner);
    mask = addRay(mask, x, y,  1, -1, occupied, inner);
    mask = addRay(mask, x, y, -1,  1, occupied, inner);
    return mask;
}

static StaticInitializer<BitBoard> bbInit;

void
BitBoard::staticInitialize() {
    for (int f = 0; f < 8; f++) {
        U64 m = 0;
        if (f > 0) m |= 1ULL << Position::getSquare(f-1, 3);
        if (f < 7) m |= 1ULL << Position::getSquare(f+1, 3);
        epMaskW[f] = m;

        m = 0;
        if (f > 0) m |= 1ULL << Position::getSquare(f-1, 4);
        if (f < 7) m |= 1ULL << Position::getSquare(f+1, 4);
        epMaskB[f] = m;
    }

    // Compute king attacks
    for (int sq = 0; sq < 64; sq++) {
        U64 m = 1ULL << sq;
        U64 mask = (((m >> 1) | (m << 7) | (m >> 9)) & maskAToGFiles) |
                   (((m << 1) | (m << 9) | (m >> 7)) & maskBToHFiles) |
                    (m << 8) | (m >> 8);
        kingAttacks[sq] = mask;
    }

    // Compute knight attacks
    for (int sq = 0; sq < 64; sq++) {
        U64 m = 1ULL << sq;
        U64 mask = (((m <<  6) | (m >> 10)) & maskAToFFiles) |
                   (((m << 15) | (m >> 17)) & maskAToGFiles) |
                   (((m << 17) | (m >> 15)) & maskBToHFiles) |
                   (((m << 10) | (m >>  6)) & maskCToHFiles);
        knightAttacks[sq] = mask;
    }

    // Compute pawn attacks
    for (int sq = 0; sq < 64; sq++) {
        U64 m = 1ULL << sq;
        U64 mask = ((m << 7) & maskAToGFiles) | ((m << 9) & maskBToHFiles);
        wPawnAttacks[sq] = mask;
        mask = ((m >> 9) & maskAToGFiles) | ((m >> 7) & maskBToHFiles);
        bPawnAttacks[sq] = mask;
    }

    // Rook magics
    int rTableSize = 0;
    for (int sq = 0; sq < 64; sq++)
        rTableSize += 1 << rBits[sq];
    int bTableSize = 0;
    for (int sq = 0; sq < 64; sq++)
        bTableSize += 1 << bBits[sq];
    tableData.resize(rTableSize + bTableSize);

    int tableUsed = 0;
    for (int sq = 0; sq < 64; sq++) {
        int x = Position::getX(sq);
        int y = Position::getY(sq);
        rMasks[sq] = addRookRays(x, y, 0ULL, true);
        int tableSize = 1 << rBits[sq];
        U64* table = &tableData[tableUsed];
        tableUsed += tableSize;
        const U64 unInit = 0xffffffffffffffffULL;
        for (int i = 0; i < tableSize; i++) table[i] = unInit;
        int nPatterns = 1 << BitBoard::bitCount(rMasks[sq]);
        for (int i = 0; i < nPatterns; i++) {
            U64 p = createPattern(i, rMasks[sq]);
            int entry = (int)((p * rMagics[sq]) >> (64 - rBits[sq]));
            U64 atks = addRookRays(x, y, p, false);
            if (table[entry] == unInit) {
                table[entry] = atks;
            } else {
                assert(table[entry] == atks);
            }
        }
        rTables[sq] = table;
    }

    // Bishop magics
    for (int sq = 0; sq < 64; sq++) {
        int x = Position::getX(sq);
        int y = Position::getY(sq);
        bMasks[sq] = addBishopRays(x, y, 0ULL, true);
        int tableSize = 1 << bBits[sq];
        U64* table = &tableData[tableUsed];
        tableUsed += tableSize;
        const U64 unInit = 0xffffffffffffffffULL;
        for (int i = 0; i < tableSize; i++) table[i] = unInit;
        int nPatterns = 1 << BitBoard::bitCount(bMasks[sq]);
        for (int i = 0; i < nPatterns; i++) {
            U64 p = createPattern(i, bMasks[sq]);
            int entry = (int)((p * bMagics[sq]) >> (64 - bBits[sq]));
            U64 atks = addBishopRays(x, y, p, false);
            if (table[entry] == unInit) {
                table[entry] = atks;
            } else {
                assert(table[entry] == atks);
            }
        }
        bTables[sq] = table;
    }

    // squaresBetween
    for (int sq1 = 0; sq1 < 64; sq1++) {
        for (int j = 0; j < 64; j++)
            squaresBetween[sq1][j] = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx == 0) && (dy == 0))
                    continue;
                U64 m = 0;
                int x = Position::getX(sq1);
                int y = Position::getY(sq1);
                while (true) {
                    x += dx; y += dy;
                    if ((x < 0) || (x > 7) || (y < 0) || (y > 7))
                        break;
                    int sq2 = Position::getSquare(x, y);
                    squaresBetween[sq1][sq2] = m;
                    m |= 1ULL << sq2;
                }
            }
        }
    }
}
