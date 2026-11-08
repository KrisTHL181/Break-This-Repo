# Project-level configuration.
cmake_minimum_required(VERSION 3.14)
project(network LANGUAGES CXX)

# The name of the executable created for the application. Change this to change
# the on-disk name of your application.
set(BINARY_NAME "ProxyPin")

# Explicitly opt in to modern CMake behaviors to avoid warnings with recent
# versions of CMake.
cmake_policy(SET CMP0063 NEW)

# Define build configuration option.
get_property(IS_MULTICONFIG GLOBAL PROPERTY GENERATOR_IS_MULTI_CONFIG)
if(IS_MULTICONFIG)
  set(CMAKE_CONFIGURATION_TYPES "Debug;Profile;Release"
    CACHE STRING "" FORCE)
else()
  if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
    set(CMAKE_BUILD_TYPE "Debug" CACHE
      STRING "Flutter build mode" FORCE)
    set_property(CACHE CMAKE_BUILD_TYPE PROPERTY STRINGS
      "Debug" "Profile" "Release")
  endif()
endif()
# Define settings for the Profile build mode.
set(CMAKE_EXE_LINKER_FLAGS_PROFILE "${CMAKE_EXE_LINKER_FLAGS_RELEASE}")
set(CMAKE_SHARED_LINKER_FLAGS_PROFILE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE}")
set(CMAKE_C_FLAGS_PROFILE "${CMAKE_C_FLAGS_RELEASE}")
set(CMAKE_CXX_FLAGS_PROFILE "${CMAKE_CXX_FLAGS_RELEASE}")

# Use Unicode for all projects.
add_definitions(-DUNICODE -D_UNICODE)

# Compilation settings that should be applied to most targets.
#
# Be cautious about adding new options here, as plugins use this function by
# default. In most cases, you should add new options to specific targets instead
# of modifying this function.
function(APPLY_STANDARD_SETTINGS TARGET)
  target_compile_features(${TARGET} PUBLIC cxx_std_17)
  target_compile_options(${TARGET} PRIVATE /W4 /WX /wd"4100")
  target_compile_options(${TARGET} PRIVATE /EHsc)
  target_compile_definitions(${TARGET} PRIVATE "_HAS_EXCEPTIONS=0")
  target_compile_definitions(${TARGET} PRIVATE "$<$<CONFIG:Debug>:_DEBUG>")
endfunction()

# Flutter library and tool build rules.
set(FLUTTER_MANAGED_DIR "${CMAKE_CURRENT_SOURCE_DIR}/flutter")
add_subdirectory(${FLUTTER_MANAGED_DIR})

# Application build; see runner/CMakeLists.txt.
add_subdirectory("runner")


# Generated plugin build rules, which manage building the plugins and adding
# them to the application.
include(flutter/generated_plugins.cmake)


# === Installation ===
# Support files are copied into place next to the executable, so that it can
# run in place. This is done instead of making a separate bundle (as on Linux)
# so that building and running from within Visual Studio will work.
set(BUILD_BUNDLE_DIR "$<TARGET_FILE_DIR:${BINARY_NAME}>")
# Make the "install" step default, as it's required to run.
set(CMAKE_VS_INCLUDE_INSTALL_TO_DEFAULT_BUILD 1)
if(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
  set(CMAKE_INSTALL_PREFIX "${BUILD_BUNDLE_DIR}" CACHE PATH "..." FORCE)
endif()

set(INSTALL_BUNDLE_DATA_DIR "${CMAKE_INSTALL_PREFIX}/data")
set(INSTALL_BUNDLE_LIB_DIR "${CMAKE_INSTALL_PREFIX}")

# Ship the Visual C++ runtime next to the executable (app-local deployment).
#
# Why: since VS 2022 17.10 the STL constructs std::mutex inline via a constexpr
# constructor instead of calling the exported _Mtx_init_in_situ(). A binary built
# with the new toolset but loading an OLDER msvcp140.dll passes a mutex the old
# _Mtx_lock() considers uninitialized, which dereferences a null pointer and
# crashes on startup (EXCEPTION_ACCESS_VIOLATION, READ at 0x0).
#
# The Windows loader searches the executable's directory before System32, so
# placing these here shadows any outdated copy already on the machine. This
# needs no administrator rights and leaves System32 untouched.
#
# Only these three are required -- verified with `dumpbin /dependents` over every
# shipped binary: nothing references msvcp140_1/_2/_atomic_wait/_codecvt_ids or
# concrt140, and msvcp140.dll does not depend on them either. Do not add the rest
# of the redist directory without re-checking.
#
# Note: this covers the VC++ CRT only. The UCRT layer (ucrtbase.dll,
# api-ms-win-crt-*) is part of Windows 10+; on Windows 8.1 it comes from update
# KB2999226. A machine missing it fails earlier and differently -- at load time,
# reporting a missing api-ms-win-crt-* DLL rather than crashing like this.
set(CMAKE_INSTALL_UCRT_LIBRARIES OFF)
set(CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS_SKIP ON)
include(InstallRequiredSystemLibraries)

set(VC_RUNTIME_WANTED "msvcp140.dll" "vcruntime140.dll" "vcruntime140_1.dll")
set(VC_RUNTIME_FILES)
foreach(lib ${CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS})
  get_filename_component(lib_name "${lib}" NAME)
  if(lib_name IN_LIST VC_RUNTIME_WANTED)
    list(APPEND VC_RUNTIME_FILES "${lib}")
  endif()
endforeach()

foreach(wanted ${VC_RUNTIME_WANTED})
  set(found FALSE)
  foreach(lib ${VC_RUNTIME_FILES})
    get_filename_component(lib_name "${lib}" NAME)
    if(lib_name STREQUAL wanted)
      set(found TRUE)
    endif()
  endforeach()
  if(NOT found)
    message(FATAL_ERROR
      "Required Visual C++ runtime '${wanted}' was not found by "
      "InstallRequiredSystemLibraries. Shipping without it would crash on "
      "machines that carry an older msvcp140.dll in System32.")
  endif()
endforeach()

install(FILES ${VC_RUNTIME_FILES} DESTINATION "${INSTALL_BUNDLE_LIB_DIR}"
  COMPONENT Runtime)

install(TARGETS ${BINARY_NAME} RUNTIME DESTINATION "${CMAKE_INSTALL_PREFIX}"
  COMPONENT Runtime)

install(FILES "${FLUTTER_ICU_DATA_FILE}" DESTINATION "${INSTALL_BUNDLE_DATA_DIR}"
  COMPONENT Runtime)

install(FILES "${FLUTTER_LIBRARY}" DESTINATION "${INSTALL_BUNDLE_LIB_DIR}"
  COMPONENT Runtime)

if(PLUGIN_BUNDLED_LIBRARIES)
  install(FILES "${PLUGIN_BUNDLED_LIBRARIES}"
    DESTINATION "${INSTALL_BUNDLE_LIB_DIR}"
    COMPONENT Runtime)
endif()

# Fully re-copy the assets directory on each build to avoid having stale files
# from a previous install.
set(FLUTTER_ASSET_DIR_NAME "flutter_assets")
install(CODE "
  file(REMOVE_RECURSE \"${INSTALL_BUNDLE_DATA_DIR}/${FLUTTER_ASSET_DIR_NAME}\")
  " COMPONENT Runtime)
install(DIRECTORY "${PROJECT_BUILD_DIR}/${FLUTTER_ASSET_DIR_NAME}"
  DESTINATION "${INSTALL_BUNDLE_DATA_DIR}" COMPONENT Runtime)

# Install the AOT library on non-Debug builds only.
install(FILES "${AOT_LIBRARY}" DESTINATION "${INSTALL_BUNDLE_DATA_DIR}"
  CONFIGURATIONS Profile;Release
  COMPONENT Runtime)
