#
# Copyright (C) 2014 MediaTek Inc.
# Modification based on code covered by the mentioned copyright
# and/or permission notice(s).
#
# Copyright 2011 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_LIBRARIES := telephony-common

LOCAL_PACKAGE_NAME := TPMessageTests

# Apk must be signed with platform signature in order to send test broadcasts.
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
