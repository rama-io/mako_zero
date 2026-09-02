cmake_minimum_required(VERSION 3.22.1)

project("mako_zero")

add_library(
mako_zero
SHARED
src/main/cpp/main.cpp
${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c
)

find_library(
android-lib
android
)

find_library(
log-lib
log
)

target_include_directories(
mako_zero
PRIVATE
${ANDROID_NDK}/sources/android/native_app_glue
)

find_library(
    log-lib
    log
)

target_link_libraries(
    mako_zero
    ${android-lib}
    ${log-lib}
)