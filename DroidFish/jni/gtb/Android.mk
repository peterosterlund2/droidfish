LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := gtb
LOCAL_SRC_FILES := \
	gtb-probe.c gtb-dec.c gtb-att.c sysport/sysport.c compression/wrap.c \
	compression/huffman/hzip.c compression/lzma/LzmaEnc.c compression/lzma/LzmaDec.c \
	compression/lzma/Alloc.c compression/lzma/LzFind.c compression/lzma/Lzma86Enc.c \
	compression/lzma/Lzma86Dec.c compression/lzma/Bra86.c compression/zlib/zcompress.c \
	compression/zlib/uncompr.c compression/zlib/inflate.c compression/zlib/deflate.c \
	compression/zlib/adler32.c compression/zlib/crc32.c compression/zlib/infback.c \
	compression/zlib/inffast.c compression/zlib/inftrees.c compression/zlib/trees.c \
	compression/zlib/zutil.c compression/liblzf/lzf_c.c compression/liblzf/lzf_d.c \
	GtbProbe.cpp

LOCAL_CFLAGS    := \
	-I $(LOCAL_PATH)/sysport/ -I $(LOCAL_PATH)/compression/ \
	-I $(LOCAL_PATH)/compression/liblzf/ -I $(LOCAL_PATH)/compression/zlib/ \
	-I $(LOCAL_PATH)/compression/lzma/ -I $(LOCAL_PATH)/compression/huffman/ \
	-D Z_PREFIX -D NDEBUG -Wall\
	 -mandroid -DTARGET_OS=android -D__ANDROID__

include $(BUILD_SHARED_LIBRARY)
