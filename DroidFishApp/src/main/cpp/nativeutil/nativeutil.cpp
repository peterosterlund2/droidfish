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

#if defined(__arm__)
  #include "cpuinfo_arm.h"
#elif defined(__aarch64__)
  #include "cpuinfo_aarch64.h"
#elif defined(__i386__)
  #include "cpuinfo_x86.h"
#endif

/*
 * Class:     org_petero_droidfish_engine_EngineUtil
 * Method:    chmod
 * Signature: (Ljava/lang/String;)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_org_petero_droidfish_engine_EngineUtil_chmod
  (JNIEnv *env, jclass, jstring jExePath) {
    const char* exePath = env->GetStringUTFChars(jExePath, NULL);
    if (!exePath)
        return static_cast<jboolean>(false);
    bool ret = chmod(exePath, 0744) == 0;
    env->ReleaseStringUTFChars(jExePath, exePath);
    return static_cast<jboolean>(ret);
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

/*
 * Class:     org_petero_droidfish_engine_EngineUtil
 * Method:    isSimdSupported
 * Signature: ()Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_org_petero_droidfish_engine_EngineUtil_isSimdSupported
    (JNIEnv *env, jclass) {
#if defined(__arm__)
    using namespace cpu_features;
    ArmFeatures features = GetArmInfo().features;
    return features.neon ? JNI_TRUE : JNI_FALSE;
#elif defined(__aarch64__)
    using namespace cpu_features;
    Aarch64Features features = GetAarch64Info().features;
    return features.asimddp ? JNI_TRUE : JNI_FALSE;
#elif defined(__i386__)
    using namespace cpu_features;
    X86Features features = GetX86Info().features;
    return features.sse4_1 ? JNI_TRUE : JNI_FALSE;
#endif
    return JNI_TRUE;
}
