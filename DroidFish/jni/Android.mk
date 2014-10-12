LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := nativeutil
LOCAL_SRC_FILES := nativeutil.cpp

include $(BUILD_SHARED_LIBRARY)

include jni/stockfish/Android.mk

include jni/gtb/Android.mk

include jni/rtb/Android.mk
