LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

UTIL_SRC_FILES := nativeutil.cpp

CPU_FEATS := cpu_features
CPU_FEATS_SRC_FILES += $(CPU_FEATS)/src/filesystem.c \
			$(CPU_FEATS)/src/stack_line_reader.c \
			$(CPU_FEATS)/src/string_view.c \
			$(CPU_FEATS)/src/hwcaps.c \
			$(CPU_FEATS)/src/unix_features_aggregator.c

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    UTIL_SRC_FILES += $(CPU_FEATS_SRC_FILES) $(CPU_FEATS)/src/cpuinfo_arm.c
endif
#ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
#    UTIL_SRC_FILES += $(CPU_FEATS_SRC_FILES) $(CPU_FEATS)/src/cpuinfo_aarch64.c
#endif
ifeq ($(TARGET_ARCH_ABI),x86)
    UTIL_SRC_FILES += $(CPU_FEATS_SRC_FILES) $(CPU_FEATS)/src/cpuinfo_x86.c
endif
#ifeq ($(TARGET_ARCH_ABI),x86_64)
#    UTIL_SRC_FILES += $(CPU_FEATS_SRC_FILES) $(CPU_FEATS)/src/cpuinfo_x86.c
#endif

LOCAL_MODULE    := nativeutil
LOCAL_SRC_FILES := $(UTIL_SRC_FILES)
LOCAL_CFLAGS    := -I$(LOCAL_PATH)/$(CPU_FEATS)/include \
		   -I$(LOCAL_PATH)/$(CPU_FEATS)/include/internal \
		   -DHAVE_DLFCN_H=1 -DSTACK_LINE_READER_BUFFER_SIZE=1024 \
		   -fPIC -s
LOCAL_LDFLAGS   := -fPIC -s
include $(BUILD_SHARED_LIBRARY)
