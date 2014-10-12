LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := rtb
LOCAL_SRC_FILES := \
	bitBoard.cpp material.cpp moveGen.cpp position.cpp rtb-probe.cpp tbprobe.cpp \
	RtbProbe.cpp

LOCAL_CFLAGS    := --std=c++11 \
	-I $(LOCAL_PATH)/sysport/ -I -DNDEBUG -Wall \
	 -mandroid -DTARGET_OS=android -D__ANDROID__ \
	-D__STDC_INT64__ -D_GLIBCXX_USE_C99_STDINT_TR1 \
	-D_GLIBCXX_HAS_GTHREADS -D_GLIBCXX__PTHREADS

include $(BUILD_SHARED_LIBRARY)
