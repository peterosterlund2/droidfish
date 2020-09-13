LOCAL_SRC_FILES := $(SF_SRC_FILES)
LOCAL_CFLAGS    := -std=c++17 -O2 -fno-exceptions -DNNUE_EMBEDDING_OFF \
                   -fPIE $(MY_ARCH_DEF) -s
LOCAL_LDFLAGS	+= -fPIE -pie -s
include $(BUILD_EXECUTABLE)
