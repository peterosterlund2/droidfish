/*
    Texel - A UCI chess engine.
    Copyright (C) 2013-2014  Peter Österlund, peterosterlund2@gmail.com

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
 * material.hpp
 *
 *  Created on: May 1, 2013
 *      Author: petero
 */

#ifndef MATERIAL_HPP_
#define MATERIAL_HPP_

#include "piece.hpp"

/**
 * An incrementally updated material identifier.
 * For each legal piece configuration, a unique identifier is computed.
 */
class MatId {
public:
    static const int WP = 1;
    static const int WR = 9;
    static const int WN = 91;
    static const int WB = 767;
    static const int WQ = 5903;

    static const int BP = 1 << 16;
    static const int BR = 9 << 16;
    static const int BN = 91 << 16;
    static const int BB = 767 << 16;
    static const int BQ = 5903 << 16;

    MatId() : hash(0) {}

    /** Add a piece to the material configuration. */
    void addPiece(int pType);

    /** Remove a piece from the material configuration. */
    void removePiece(int pType);

    /** Add cnt pieces of tyep ptype to the material configuration. */
    void addPieceCnt(int pType, int cnt);

    /** Get the material configuration identifier. */
    int operator()() const;

    /** Get ID for black/white mirror position. */
    static int mirror(int id);

private:
    int hash;
    static const int materialId[Piece::nPieceTypes];
};

inline void
MatId::addPiece(int pType) {
    hash += materialId[pType];
}

inline void
MatId::removePiece(int pType) {
    hash -= materialId[pType];
}

inline void
MatId::addPieceCnt(int pType, int cnt) {
    hash += materialId[pType] * cnt;
}

inline int
MatId::operator()() const {
    return hash;
}

inline int
MatId::mirror(int h) {
    unsigned int ret = h;
    return (ret >> 16) | ((ret & 0xffff) << 16);
}

#endif /* MATERIAL_HPP_ */
