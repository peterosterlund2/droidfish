LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := jni
LOCAL_SRC_FILES := jni.cpp

LOCAL_CFLAGS    := \
	 -mandroid \
	-DTARGET_OS=android -D__ANDROID__ \
	-isystem $(SYSROOT)/usr/include

LOCAL_STATIC_LIBRARIES := stockfish

include $(BUILD_SHARED_LIBRARY)

include jni/stockfish/Android.mk
