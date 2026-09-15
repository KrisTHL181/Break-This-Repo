LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := RakNet

MY_PREFIX := $(LOCAL_PATH)/RaknetSources/
MY_SOURCES := $(wildcard $(MY_PREFIX)*.cpp)
LOCAL_SRC_FILES += $(MY_SOURCES:$(MY_PREFIX)%=RaknetSources/%)

LOCAL_CFLAGS := -Wno-psabi $(LOCAL_CFLAGS)
LOCAL_CPPFLAGS += -frtti

include $(BUILD_STATIC_LIBRARY)
