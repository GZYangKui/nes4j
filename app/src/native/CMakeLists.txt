cmake_minimum_required(VERSION 3.22)
project(native C)

#开启测试
enable_testing()

find_package(JNI REQUIRED)
find_package(ALSA REQUIRED)

set(CMAKE_C_STANDARD 99)

aux_source_directory(. SRC_DIR)
aux_source_directory(jni JNI_DIR)

list(APPEND SRC_DIR ${JNI_DIR})

add_library(nes4j SHARED ${SRC_DIR})
add_executable(nes4j_test ${SRC_DIR})

include_directories(nes4j ${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2})
include_directories(nes4j_test ${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2})

target_link_libraries(nes4j ${ALSA_LIBRARIES} m)
target_link_libraries(nes4j_test ${ALSA_LIBRARIES} m)


add_test(NAME test COMMAND nes4j_test 2 3)
