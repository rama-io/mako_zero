#include <android/native_activity.h>
#include <android/log.h>

#define LOG_TAG "Mako Forever"

void onCreate(
    ANativeActivity* activity,
    void* savedState,
    size_t savedStateSize
) {
    __android_log_print(
        ANDROID_LOG_INFO,
        LOG_TAG,
        "Mako Forever started"
    );
}

extern "C"
void ANativeActivity_onCreate(
    ANativeActivity* activity,
    void* savedState,
    size_t savedStateSize
) {
    activity->callbacks->onStart = nullptr;

    onCreate(
        activity,
        savedState,
        savedStateSize
    );
}