LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := stockfish
LOCAL_SRC_FILES := \
	benchmark.cpp  main.cpp	   movegen.cpp	 pawns.cpp     thread.cpp   uci.cpp \
	bitbase.cpp    endgame.cpp   material.cpp  movepick.cpp  position.cpp  timeman.cpp  ucioption.cpp \
	bitboard.cpp   evaluate.cpp  misc.cpp	   notation.cpp  search.cpp    tt.cpp tbprobe.cpp

LOCAL_CFLAGS    := -std=c++11 -O2
LOCAL_LDFLAGS	:= -static

include $(BUILD_EXECUTABLE)
