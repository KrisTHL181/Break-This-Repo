cmake_minimum_required(VERSION 3.16)

set(_lua_h "${CMAKE_CURRENT_SOURCE_DIR}/src/lua.h")

file(STRINGS "${_lua_h}" _lua_line_major REGEX "^#define[ \t]+LUA_VERSION_MAJOR[ \t]+\"[0-9]+\"")
file(STRINGS "${_lua_h}" _lua_line_minor REGEX "^#define[ \t]+LUA_VERSION_MINOR[ \t]+\"[0-9]+\"")
file(STRINGS "${_lua_h}" _lua_line_release REGEX "^#define[ \t]+LUA_VERSION_RELEASE[ \t]+\"[0-9]+\"")
foreach(_v major minor release)
  if(NOT _lua_line_${_v})
    message(FATAL_ERROR "Failed to parse LUA_VERSION_${_v} from ${_lua_h}")
  endif()
endforeach()
string(REGEX REPLACE "^#define[ \t]+LUA_VERSION_MAJOR[ \t]+\"([0-9]+)\".*" "\\1" LUA_VERSION_MAJOR "${_lua_line_major}")
string(REGEX REPLACE "^#define[ \t]+LUA_VERSION_MINOR[ \t]+\"([0-9]+)\".*" "\\1" LUA_VERSION_MINOR "${_lua_line_minor}")
string(REGEX REPLACE "^#define[ \t]+LUA_VERSION_RELEASE[ \t]+\"([0-9]+)\".*" "\\1" LUA_VERSION_RELEASE "${_lua_line_release}")

project(lua VERSION ${LUA_VERSION_MAJOR}.${LUA_VERSION_MINOR}.${LUA_VERSION_RELEASE} LANGUAGES C)

# ---------------------------------------------------------------------------
# Options
# ---------------------------------------------------------------------------
if(CMAKE_SYSTEM_NAME STREQUAL "iOS")
  set(_lua_shared_default OFF)      # on iOS a static xcframework is the norm
  set(_lua_exe_default OFF)         # plain executables make no sense on iOS
else()
  set(_lua_shared_default ON)
  set(_lua_exe_default ON)
endif()

option(LUA_BUILD_STATIC      "Build the static liblua archive"              ON)
option(LUA_BUILD_SHARED      "Build the shared liblua library"              ${_lua_shared_default})
option(LUA_BUILD_EXECUTABLES "Build the lua and luac executables"           ${_lua_exe_default})

if(NOT LUA_BUILD_STATIC AND NOT LUA_BUILD_SHARED)
  message(FATAL_ERROR "At least one of LUA_BUILD_STATIC / LUA_BUILD_SHARED must be ON")
endif()

# ---------------------------------------------------------------------------
# Platform feature macros (only needed while compiling the Lua sources)
# ---------------------------------------------------------------------------
set(LUA_FEATURE_DEFS "")
if(ANDROID)
  list(APPEND LUA_FEATURE_DEFS LUA_USE_DLOPEN)
elseif(APPLE)
  if(CMAKE_SYSTEM_NAME STREQUAL "iOS")
    list(APPEND LUA_FEATURE_DEFS LUA_USE_IOS)
  else()
    list(APPEND LUA_FEATURE_DEFS LUA_USE_MACOSX)
  endif()
elseif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
  list(APPEND LUA_FEATURE_DEFS LUA_USE_LINUX)
elseif(UNIX)
  list(APPEND LUA_FEATURE_DEFS LUA_USE_POSIX LUA_USE_DLOPEN)
endif()
if(MSVC)
  list(APPEND LUA_FEATURE_DEFS _CRT_SECURE_NO_WARNINGS)
endif()

# libs needed on top of the C runtime (math + dlopen). glibc only.
set(LUA_EXTRA_LIBS "")
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
  list(APPEND LUA_EXTRA_LIBS m dl)
endif()

# ---------------------------------------------------------------------------
# Sources: every .c except the two executables
# ---------------------------------------------------------------------------
set(LUA_CORE_SRC
  src/lapi.c       src/lauxlib.c     src/lbaselib.c    src/lcode.c
  src/lcorolib.c   src/lctype.c      src/ldblib.c      src/ldebug.c
  src/ldo.c        src/ldump.c       src/lfunc.c       src/lgc.c
  src/linit.c      src/liolib.c      src/llex.c        src/lmathlib.c
  src/lmem.c       src/loadlib.c     src/lobject.c     src/lopcodes.c
  src/loslib.c     src/lparser.c     src/lstate.c      src/lstring.c
  src/lstrlib.c    src/ltable.c      src/ltablib.c     src/ltm.c
  src/lundump.c    src/lutf8lib.c    src/lvm.c         src/lzio.c
)

set(LUA_PUBLIC_HEADERS
  src/lua.h src/luaconf.h src/lualib.h src/lauxlib.h src/lua.hpp
)

# ---------------------------------------------------------------------------
# Core objects, compiled once without any export decorations.
# PIC is on so the same objects can also go into the shared library.
# ---------------------------------------------------------------------------
if(NOT CMAKE_SYSTEM_NAME STREQUAL "iOS")
  add_library(lua_objects OBJECT ${LUA_CORE_SRC})
  set_target_properties(lua_objects PROPERTIES POSITION_INDEPENDENT_CODE ON)
  target_include_directories(lua_objects PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/src")
  target_compile_definitions(lua_objects PRIVATE ${LUA_FEATURE_DEFS})
endif()

# ---------------------------------------------------------------------------
# Static library
# ---------------------------------------------------------------------------
if(LUA_BUILD_STATIC)
  if(CMAKE_SYSTEM_NAME STREQUAL "iOS")
    add_library(lua_static STATIC ${LUA_CORE_SRC})
    target_include_directories(lua_static PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/src")
    target_compile_definitions(lua_static PRIVATE ${LUA_FEATURE_DEFS})
  else()
    add_library(lua_static STATIC $<TARGET_OBJECTS:lua_objects>)
  endif()
  # MSVC static archive: lua.lib  (the shared one is named lua54, so no clash)
  # Other toolchains:   liblua.a
  set_target_properties(lua_static PROPERTIES OUTPUT_NAME lua)
  # Let CMake consumers of the static archive pick up -lm/-ldl automatically.
  if(LUA_EXTRA_LIBS)
    target_link_libraries(lua_static INTERFACE ${LUA_EXTRA_LIBS})
  endif()
  add_library(Lua::Static ALIAS lua_static)
endif()

# ---------------------------------------------------------------------------
# Shared library
#
#  * MSVC: the objects must be compiled with LUA_BUILD_AS_DLL so that
#    LUA_API/LUALIB_API become __declspec(dllexport). We therefore compile a
#    dedicated object set for the DLL (each Lua .c defines LUA_CORE/LUA_LIB
#    itself, so dllexport kicks in automatically). Naming: lua54.dll + lua54.lib
#    to keep the import lib distinct from the static lua.lib.
#  * MinGW: gcc auto-exports everything, plain objects are fine.
#  * Unix/macOS/Android: plain objects are fine (ELF default / Mach-O export all).
#  * Android: no VERSION/SOVERSION -> plain liblua.so (JNI-friendly, no symlinks).
# ---------------------------------------------------------------------------
if(LUA_BUILD_SHARED)
  if(MSVC)
    add_library(lua_dll_objects OBJECT ${LUA_CORE_SRC})
    set_target_properties(lua_dll_objects PROPERTIES POSITION_INDEPENDENT_CODE ON)
    target_include_directories(lua_dll_objects PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/src")
    target_compile_definitions(lua_dll_objects PRIVATE ${LUA_FEATURE_DEFS} LUA_BUILD_AS_DLL)

    add_library(lua_shared SHARED $<TARGET_OBJECTS:lua_dll_objects>)
    set_target_properties(lua_shared PROPERTIES OUTPUT_NAME lua54)
  else()
    add_library(lua_shared SHARED $<TARGET_OBJECTS:lua_objects>)
    set_target_properties(lua_shared PROPERTIES OUTPUT_NAME lua)
    if(NOT ANDROID)
      set_target_properties(lua_shared PROPERTIES
        VERSION   ${PROJECT_VERSION}
        SOVERSION ${LUA_VERSION_MAJOR}.${LUA_VERSION_MINOR})
    endif()
  endif()

  if(LUA_EXTRA_LIBS)
    target_link_libraries(lua_shared PRIVATE ${LUA_EXTRA_LIBS})
  endif()
  add_library(Lua::Shared ALIAS lua_shared)
endif()

# ---------------------------------------------------------------------------
# Executables (always linked against the static lib when it exists)
# ---------------------------------------------------------------------------
if(LUA_BUILD_EXECUTABLES)
  add_executable(lua  src/lua.c)
  add_executable(luac src/luac.c)

  foreach(_exe lua luac)
    target_include_directories(${_exe} PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/src")
    target_compile_definitions(${_exe} PRIVATE ${LUA_FEATURE_DEFS})
    if(TARGET lua_static)
      target_link_libraries(${_exe} PRIVATE lua_static)
    else()
      target_link_libraries(${_exe} PRIVATE lua_shared)
    endif()
  endforeach()
endif()

# ---------------------------------------------------------------------------
# Install rules
# ---------------------------------------------------------------------------
include(GNUInstallDirs)

set(_install_targets "")
if(TARGET lua_static)
  list(APPEND _install_targets lua_static)
endif()
if(TARGET lua_shared)
  list(APPEND _install_targets lua_shared)
endif()
if(TARGET lua)
  list(APPEND _install_targets lua)
endif()
if(TARGET luac)
  list(APPEND _install_targets luac)
endif()

if(_install_targets)
  install(TARGETS ${_install_targets}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
    INCLUDES DESTINATION ${CMAKE_INSTALL_INCLUDEDIR})
endif()

install(FILES ${LUA_PUBLIC_HEADERS} DESTINATION ${CMAKE_INSTALL_INCLUDEDIR})

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
message(STATUS "Lua ${PROJECT_VERSION}")
message(STATUS "  static library:    ${LUA_BUILD_STATIC}")
message(STATUS "  shared library:    ${LUA_BUILD_SHARED}")
message(STATUS "  executables:       ${LUA_BUILD_EXECUTABLES}")
message(STATUS "  platform defs:     ${LUA_FEATURE_DEFS}")
