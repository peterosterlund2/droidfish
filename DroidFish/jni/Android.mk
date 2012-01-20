LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := nativeutil
LOCAL_SRC_FILES := nativeutil.cpp

LOCAL_CFLAGS    := \
    -mandroid \
	-DTARGET_OS=android -D__ANDROID__ \
	-isystem $(SYSROOT)/usr/include

include $(BUILD_SHARED_LIBRARY)

include jni/stockfish/Android.mk

include jni/gtb/Android.mk
