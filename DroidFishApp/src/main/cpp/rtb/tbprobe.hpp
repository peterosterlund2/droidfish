/*
    Texel - A UCI chess engine.
    Copyright (C) 2014  Peter Österlund, peterosterlund2@gmail.com

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
 * tbprobe.hpp
 *
 *  Created on: Jun 2, 2014
 *      Author: petero
 */

#ifndef TBPROBE_HPP_
#define TBPROBE_HPP_

#include "moveGen.hpp"

#include <string>


class Position;

/**
 * Handle tablebase probing.
 */
class TBProbe {
public:
    /** Initialize tablebases. */
    static void initialize(const std::string& rtbPath);

    /**
     * Probe syzygy DTZ tablebases.
     * @param pos  The position to probe. The position can be temporarily modified
     *             but is restored to original state before function returns.
     * @param score The tablebase score. Only modified for tablebase hits.
     *              The returned score is either 0 or a mate bound. The bound
     *              is computed by considering the DTZ value and the maximum number
     *              of zeroing moves before mate.
     */
    static bool rtbProbeDTZ(Position& pos, int& score);

    /**
     * Probe syzygy WDL tablebases.
     * @param pos  The position to probe. The position can be temporarily modified
     *             but is restored to original state before function returns.
     * @param score The tablebase score. Only modified for tablebase hits.
     *              The returned score is either 0 or +/- 1.
     */
    static bool rtbProbeWDL(Position& pos, int& score);
};


#endif /* TBPROBE_HPP_ */
