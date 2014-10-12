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
#include "GtbProbe.h"
#include "gtb-probe.h"
#include <algorithm>

using namespace std;

static bool isInitialized = false;
static bool initOk = false;
static const char** paths = NULL;


JNIEXPORT jboolean
JNICALL Java_org_petero_droidfish_tb_GtbProbe_init(
        JNIEnv* env, jclass cls, jstring jTbPath)
{
    initOk = false;
    const char* tbPath = (*env).GetStringUTFChars(jTbPath, NULL);
    if (!tbPath)
        return false;

    if (isInitialized && paths)
        tbpaths_done(paths);
    paths = tbpaths_init();
    if (paths == NULL) {
        (*env).ReleaseStringUTFChars(jTbPath, tbPath);
        return false;
    }
    paths = tbpaths_add(paths, tbPath);
    if (paths == NULL) {
        (*env).ReleaseStringUTFChars(jTbPath, tbPath);
        return false;
    }

    TB_compression_scheme scheme = tb_CP4;
    int verbose = 0;
    int cacheSize = 4*1024*1024;
    int wdlFraction = 8;
    if (isInitialized) {
        tb_restart (verbose, scheme, paths);
        tbcache_restart(cacheSize, wdlFraction);
    } else {
        tb_init(verbose, scheme, paths);
        tbcache_init(cacheSize, wdlFraction);
    }
    isInitialized = true;

    (*env).ReleaseStringUTFChars(jTbPath, tbPath);
    initOk = true;
    return true;
}

#define WHITE_TO_MOVE 0
#define BLACK_TO_MOVE 1

JNIEXPORT jboolean
JNICALL Java_org_petero_droidfish_tb_GtbProbe_probeHard(
        JNIEnv* env, jobject ths,
        jboolean wtm, jint epSq, jint castleMask,
        jintArray whiteSquares, jintArray blackSquares,
        jbyteArray whitePieces, jbyteArray blackPieces,
        jintArray result)
{
    if (!initOk)
        return false;
    if ((*env).GetArrayLength(result) < 2)
        return false;

    const int MAXLEN = 17;
    unsigned char wp[MAXLEN];
    unsigned int  ws[MAXLEN];
    unsigned char bp[MAXLEN];
    unsigned int  bs[MAXLEN];

    int len = (*env).GetArrayLength(whiteSquares);
    jint* jiPtr = (*env).GetIntArrayElements(whiteSquares, NULL);
    for (int i = 0; i < min(len, MAXLEN); i++)
            ws[i] = jiPtr[i];
    (*env).ReleaseIntArrayElements(whiteSquares, jiPtr, 0);

    len = (*env).GetArrayLength(blackSquares);
    jiPtr = (*env).GetIntArrayElements(blackSquares, NULL);
    for (int i = 0; i < min(len, MAXLEN); i++)
            bs[i] = jiPtr[i];
    (*env).ReleaseIntArrayElements(blackSquares, jiPtr, 0);

    len = (*env).GetArrayLength(whitePieces);
    jbyte* jcPtr = (*env).GetByteArrayElements(whitePieces, NULL);
    for (int i = 0; i < min(len, MAXLEN); i++)
            wp[i] = jcPtr[i];
    (*env).ReleaseByteArrayElements(whitePieces, jcPtr, 0);

    len = (*env).GetArrayLength(blackPieces);
    jcPtr = (*env).GetByteArrayElements(blackPieces, NULL);
    for (int i = 0; i < min(len, MAXLEN); i++)
            bp[i] = jcPtr[i];
    (*env).ReleaseByteArrayElements(blackPieces, jcPtr, 0);

    unsigned int tbInfo;
    unsigned int plies;
    int ret = tb_probe_hard(wtm ? WHITE_TO_MOVE : BLACK_TO_MOVE,
                            epSq, castleMask,
                            ws,
                            bs,
                            wp,
                            bp,
                            &tbInfo, &plies);
    jint res[2];
    res[0] = tbInfo;
    res[1] = plies;

    (*env).SetIntArrayRegion(result, 0, 2, res);
    return ret != 0;
}
