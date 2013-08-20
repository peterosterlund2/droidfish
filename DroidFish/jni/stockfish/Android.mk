LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := stockfish
LOCAL_SRC_FILES := \
	benchmark.cpp  book.cpp      main.cpp	   movegen.cpp	 pawns.cpp     thread.cpp   uci.cpp \
	bitbase.cpp    endgame.cpp   material.cpp  movepick.cpp  position.cpp  timeman.cpp  ucioption.cpp \
	bitboard.cpp   evaluate.cpp  misc.cpp	   notation.cpp  search.cpp    tt.cpp

LOCAL_CFLAGS    := -DNO_PREFETCH=1 -O2

include $(BUILD_EXECUTABLE)
