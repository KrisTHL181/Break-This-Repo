FILE(GLOB raknetCPPSources ./RaknetSources/*.cpp)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -m64")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -m64 -fno-sized-deallocation")
add_library(raknet ${raknetCPPSources})
