################################################################
# A minimal CMake file that is compatible with sbt-jni         #
#                                                              #
# All settings required by sbt-jni have been marked so, please #
# add/modify/remove settings to build your specific library.   #
################################################################

cmake_minimum_required(VERSION 3.12)

option(SBT "Set if invoked from sbt-jni" OFF)

set(CMAKE_BUILD_TYPE Debug)

# Define project and related variables
# (required by sbt-jni) please use semantic versioning
#
project (nativeMLIR)
set(PROJECT_VERSION_MAJOR 0)
set(PROJECT_VERSION_MINOR 0)
set(PROJECT_VERSION_PATCH 0)

# Setup JNI
find_package(JNI REQUIRED)
if (JNI_FOUND)
    message (STATUS "JNI include directories: ${JNI_INCLUDE_DIRS}")
endif()

# Include directories
include_directories(.)
include_directories(include)
include_directories(${JNI_INCLUDE_DIRS})

# Include generated headers for what we're implementing.
include_directories("${CMAKE_SOURCE_DIR}/../../../core/target/native/include")

# Include MLIR C API.
add_library(MLIR-C SHARED IMPORTED)
# TODO: no rpath, use full path? install it?
# TODO: or, make this cmakelists actually build the relevant mlir capi
# TODO: or, install the mlir capi and depend on it!
set_target_properties(MLIR-C PROPERTIES
  IMPORTED_LOCATION "/Users/mikeu/circt/build/lib/libMLIR-C.dylib"
  INTERFACE_INCLUDE_DIRECTORIES "/Users/mikeu/circt/llvm/mlir/include"
)

# Dirty hacking to squirt the relevant RPATH into the library.
# There is probably a better way.
list(APPEND CMAKE_INSTALL_RPATH /Users/mikeu/circt/build/lib)
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

# Sources
file(GLOB LIB_SRC
  "*.c"
  "*.cc"
  "*.cpp"
)

# Setup installation targets
# (required by sbt-jni) major version should always be appended to library name
#
set (LIB_NAME ${PROJECT_NAME}${PROJECT_VERSION_MAJOR})
add_library(${LIB_NAME} SHARED ${LIB_SRC})
target_link_libraries(${LIB_NAME} MLIR-C)

install(TARGETS ${LIB_NAME} LIBRARY DESTINATION .)
