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
 * tbprobe.cpp
 *
 *  Created on: Jun 2, 2014
 *      Author: petero
 */

#include "tbprobe.hpp"
#include "rtb-probe.hpp"
#include "bitBoard.hpp"
#include "position.hpp"
#include "moveGen.hpp"
#include <unordered_map>
#include <cassert>


static std::string currentRtbPath;

void
TBProbe::initialize(const std::string& rtbPath) {
    if (rtbPath != currentRtbPath) {
        Syzygy::init(rtbPath);
        currentRtbPath = rtbPath;
    }
}

bool
TBProbe::rtbProbeDTZ(Position& pos, int& score) {
    const int nPieces = BitBoard::bitCount(pos.occupiedBB());
    if (nPieces > Syzygy::TBLargest)
        return false;
    if (pos.getCastleMask())
        return false;
    if (MoveGen::canTakeKing(pos))
        return false;

    int success;
    const int dtz = Syzygy::probe_dtz(pos, &success);
    if (!success)
        return false;
    if (dtz == 0) {
        score = 0;
        return true;
    }
    const int maxHalfMoveClock = std::abs(dtz) + pos.getHalfMoveClock();
    if (abs(dtz) <= 2) {
        if (maxHalfMoveClock > 101) {
            score = 0;
            return true;
        } else if (maxHalfMoveClock == 101)
            return false; // DTZ can be wrong when mate-in-1
    } else {
        if (maxHalfMoveClock > 100) {
            score = 0;
            return true;
        }
    }
    score = dtz;
    return true;
}

bool
TBProbe::rtbProbeWDL(Position& pos, int& score) {
    if (BitBoard::bitCount(pos.occupiedBB()) > Syzygy::TBLargest)
        return false;
    if (pos.getCastleMask())
        return false;
    if (MoveGen::canTakeKing(pos))
        return false;

    int success;
    int wdl = Syzygy::probe_wdl(pos, &success);
    if (!success)
        return false;
    switch (wdl) {
    case 0: case 1: case -1:
        score = 0;
        break;
    case 2:
        score = 1;
        break;
    case -2:
        score = -1;
        break;
    default:
        return false;
    }

    return true;
}
