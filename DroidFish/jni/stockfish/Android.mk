LOCAL_PATH := $(call my-dir)

SF_SRC_FILES := \
	benchmark.cpp  main.cpp	   movegen.cpp	 pawns.cpp     thread.cpp   uci.cpp \
	bitbase.cpp    endgame.cpp   material.cpp  movepick.cpp  position.cpp  timeman.cpp  ucioption.cpp \
	bitboard.cpp   evaluate.cpp  misc.cpp	   notation.cpp  search.cpp    tt.cpp tbprobe.cpp

include $(CLEAR_VARS)
LOCAL_MODULE    := stockfish-nopie
LOCAL_SRC_FILES := $(SF_SRC_FILES)
LOCAL_CFLAGS    := -std=c++11 -O2
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE    := stockfish
LOCAL_SRC_FILES := $(SF_SRC_FILES)
LOCAL_CFLAGS    := -std=c++11 -O2 -fPIE
LOCAL_LDFLAGS	+= -fPIE -pie
include $(BUILD_EXECUTABLE)
