LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := nativeutil
LOCAL_SRC_FILES := nativeutil.cpp

include $(BUILD_SHARED_LIBRARY)

include src/main/cpp/stockfish/Android.mk

include src/main/cpp/gtb/Android.mk

include src/main/cpp/rtb/Android.mk
