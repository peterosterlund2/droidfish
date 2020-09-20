LOCAL_SRC_FILES := $(SF_SRC_FILES)
LOCAL_CFLAGS    := -std=c++17 -O3 -fno-exceptions -DNNUE_EMBEDDING_OFF -DUSE_PTHREADS \
                   -fPIE $(MY_ARCH_DEF) -s -flto=thin
LOCAL_LDFLAGS	+= -fPIE -s -flto=thin
include $(BUILD_EXECUTABLE)
