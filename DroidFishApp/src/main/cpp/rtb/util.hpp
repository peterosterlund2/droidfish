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
 * util.hpp
 *
 *  Created on: Feb 26, 2012
 *      Author: petero
 */

#ifndef UTIL_HPP_
#define UTIL_HPP_

#include <cstdint>

typedef uint64_t U64;
typedef int64_t  S64;
typedef uint32_t U32;
typedef int32_t  S32;
typedef uint16_t U16;
typedef int16_t  S16;
typedef int8_t   S8;
typedef uint8_t  U8;


/** Helper class to perform static initialization of a class T. */
template <typename T>
class StaticInitializer {
public:
    StaticInitializer() {
        T::staticInitialize();
    }
};

#endif /* UTIL_HPP_ */
