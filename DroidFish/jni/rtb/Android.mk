LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := rtb
LOCAL_SRC_FILES := \
	bitBoard.cpp material.cpp moveGen.cpp position.cpp rtb-probe.cpp tbprobe.cpp \
	RtbProbe.cpp

LOCAL_CFLAGS    := --std=c++11 \
	-I $(LOCAL_PATH)/sysport/ -I -DNDEBUG -Wall

LOCAL_LDLIBS	+= -latomic

include $(BUILD_SHARED_LIBRARY)
