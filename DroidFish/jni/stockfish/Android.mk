LOCAL_PATH := $(call my-dir)

SF_SRC_FILES := \
	benchmark.cpp main.cpp movegen.cpp pawns.cpp thread.cpp uci.cpp psqt.cpp \
	bitbase.cpp endgame.cpp material.cpp movepick.cpp position.cpp timeman.cpp ucioption.cpp \
	bitboard.cpp evaluate.cpp misc.cpp search.cpp tt.cpp syzygy/tbprobe.cpp

MY_ARCH_DEF :=
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
  MY_ARCH_DEF += -DIS_64BIT
endif
ifeq ($(TARGET_ARCH_ABI),x86_64)
  MY_ARCH_DEF += -DIS_64BIT
endif
ifeq ($(TARGET_ARCH_ABI),mips64)
  MY_ARCH_DEF += -DIS_64BIT
endif

include $(CLEAR_VARS)
LOCAL_MODULE    := stockfish-nopie
LOCAL_SRC_FILES := $(SF_SRC_FILES)
LOCAL_CFLAGS    := -std=c++11 -O2 $(MY_ARCH_DEF)
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE    := stockfish
LOCAL_SRC_FILES := $(SF_SRC_FILES)
LOCAL_CFLAGS    := -std=c++11 -O2 -fPIE $(MY_ARCH_DEF)
LOCAL_LDFLAGS	+= -fPIE -pie
include $(BUILD_EXECUTABLE)
