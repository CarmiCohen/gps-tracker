cmake_minimum_required(VERSION 3.22.1)

project("gps-tracker")

# Issue #628: 16KB Page Size Support
# Ensures the native library is aligned for Android 15+ 16KB page size devices.
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# Issue #212: Neutral hardware namespace to eliminate Samsung framework collisions (R212).
add_library(
        jdHardware
        SHARED
        jdhardware-jni.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        jdHardware
        ${log-lib})
