/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2012  Peter Österlund, peterosterlund2@gmail.com

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
#include "RtbProbe.h"
#include "tbprobe.hpp"
#include <algorithm>


static bool initOk = false;

JNIEXPORT jboolean
JNICALL Java_org_petero_droidfish_tb_RtbProbe_init(
        JNIEnv* env, jclass cls, jstring jTbPath)
{
    initOk = false;
    const char* tbPath = (*env).GetStringUTFChars(jTbPath, NULL);
    if (!tbPath)
        return false;
    std::string rtbPath(tbPath);
    (*env).ReleaseStringUTFChars(jTbPath, tbPath);

    TBProbe::initialize(rtbPath);
    initOk = true;
    return true;
}

JNIEXPORT void
JNICALL Java_org_petero_droidfish_tb_RtbProbe_probe(
        JNIEnv* env, jobject ths, jbyteArray jSquares, jboolean wtm,
        jint epSq, jint castleMask,
        jint halfMoveClock, jint fullMoveCounter,
        jintArray result)
{
    if ((*env).GetArrayLength(result) < 2)
        return;

    jint res[2];
    res[0] = 1000;
    res[1] = 1000;
    (*env).SetIntArrayRegion(result, 0, 2, res);

    if (!initOk)
        return;

    const int len = (*env).GetArrayLength(jSquares);
    if (len != 64)
        return;

    Position pos;
    jbyte* jbPtr = (*env).GetByteArrayElements(jSquares, NULL);
    for (int i = 0; i < 64; i++)
        pos.setPiece(i, jbPtr[i]);
    (*env).ReleaseByteArrayElements(jSquares, jbPtr, 0);

    pos.setWhiteMove(wtm);
    pos.setEpSquare(epSq);
    pos.setCastleMask(castleMask);
    pos.setHalfMoveClock(halfMoveClock);
    pos.setFullMoveCounter(fullMoveCounter);

    int score;
    if (TBProbe::rtbProbeWDL(pos, score))
        res[0] = score;
    if (TBProbe::rtbProbeDTZ(pos, score))
        res[1] = score;

    (*env).SetIntArrayRegion(result, 0, 2, res);
}
