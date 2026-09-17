cmake_minimum_required(VERSION 3.22.1)

project("autoroid_native")

add_library(
    autoroid_native
    SHARED
    native_engine.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    autoroid_native
    ${log-lib}
)
