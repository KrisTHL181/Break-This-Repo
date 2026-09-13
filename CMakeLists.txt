cmake_minimum_required(VERSION 3.16)

project(break_this_repo LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(fozu fozu.cpp)
add_executable(what what.cpp)

install(TARGETS fozu what)

if(WIN32)
    add_executable(beep_win WIN32 beep_win.cpp)
    install(TARGETS beep_win)
endif()