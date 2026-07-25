cmake_minimum_required(VERSION 3.22.1)

project("gps-tracker")

add_library(
        mbrainSDK
        SHARED
        mbrain-jni.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        mbrainSDK
        ${log-lib})
