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
 * move.hpp
 *
 *  Created on: Feb 25, 2012
 *      Author: petero
 */

#ifndef MOVE_HPP_
#define MOVE_HPP_

#include "util.hpp"

/** Represents a chess move. */
class Move {
public:
    /** Create empty move object. */
    Move() : from_(0), to_(0), promoteTo_(0) { }

    /** Create a move object. */
    Move(int from, int to, int promoteTo);

    /** Copy constructor. */
    Move(const Move& m);

    int from() const;
    int to() const;
    int promoteTo() const;

    /** Not declared "nothrow". Avoids nullptr check in generated assembly code when using placement new. */
    void* operator new (size_t size, void* ptr) { return ptr; }

private:
    /** From square, 0-63. */
    int from_;

    /** To square, 0-63. */
    int to_;

    /** Promotion piece. */
    int promoteTo_;
};

inline
Move::Move(int from, int to, int promoteTo) {
    from_ = from;
    to_ = to;
    promoteTo_ = promoteTo;
}

inline
Move::Move(const Move& m) {
    from_ = m.from_;
    to_ = m.to_;
    promoteTo_ = m.promoteTo_;
}

inline int
Move::from() const {
    return from_;
}

inline int
Move::to() const {
    return to_;
}

inline int
Move::promoteTo() const {
    return promoteTo_;
}

#endif /* MOVE_HPP_ */
