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

#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/resource.h>

/*
 * Class:     org_petero_droidfish_engine_EngineUtil
 * Method:    chmod
 * Signature: (Ljava/lang/String;)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_org_petero_droidfish_engine_EngineUtil_chmod
  (JNIEnv *env, jclass, jstring jExePath) {
    const char* exePath = (*env).GetStringUTFChars(jExePath, NULL);
    if (!exePath)
        return false;
    bool ret = chmod(exePath, 0744) == 0;
    (*env).ReleaseStringUTFChars(jExePath, exePath);
    return ret;
}

/*
 * Class:     org_petero_droidfish_engine_EngineUtil
 * Method:    reNice
 * Signature: (II)V
 */
extern "C" JNIEXPORT void JNICALL Java_org_petero_droidfish_engine_EngineUtil_reNice
  (JNIEnv *env, jclass, jint pid, jint prio) {
    setpriority(PRIO_PROCESS, pid, prio);
}

