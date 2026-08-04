cmake_minimum_required(VERSION 3.22.1)

project("gps-tracker")

# Issue #628: 16KB Page Size Support
# Ensures the native library is aligned for Android 15+ 16KB page size devices.
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(
        jdMbrain
        SHARED
        mbrain-jni.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        jdMbrain
        ${log-lib})
