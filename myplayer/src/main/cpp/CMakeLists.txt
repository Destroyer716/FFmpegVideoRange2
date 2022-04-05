cmake_minimum_required(VERSION 3.4.1)

include_directories(/include)
include_directories(/soundtouch/include)
include_directories(/soundtouch/SoundTouch)



add_library(native-lib
             SHARED
        native-lib.cpp
        FAvFrameHelper.cpp
        JavaCallHelper.cpp
        KzgFFmpeg.cpp
        KzgAudio.cpp
        SafeQueue.cpp
        KzgPlayerStatus.cpp
        KzgVideo.cpp
        AVFrameQueue.cpp
        soundtouch/SoundTouch/AAFilter.cpp
        soundtouch/SoundTouch/FIFOSampleBuffer.cpp
        soundtouch/SoundTouch/FIRFilter.cpp
        soundtouch/SoundTouch/cpu_detect_x86.cpp
        soundtouch/SoundTouch/sse_optimized.cpp
        soundtouch/SoundTouch/RateTransposer.cpp
        soundtouch/SoundTouch/SoundTouch.cpp
        soundtouch/SoundTouch/InterpolateCubic.cpp
        soundtouch/SoundTouch/InterpolateLinear.cpp
        soundtouch/SoundTouch/InterpolateShannon.cpp
        soundtouch/SoundTouch/TDStretch.cpp
        soundtouch/SoundTouch/PeakFinder.cpp
        mediametadataretriever/ffmpeg_utils.c
        mediametadataretriever/ffmpeg_mediametadataretriever.c
        mediametadataretriever/wseemann_media_MediaMetadataRetriever.cpp
        mediametadataretriever/mediametadataretriever.cpp
        )

message(WARNING "CMAKE_BINARY_DIR = ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavcodec.so")

add_library(avcodec SHARED IMPORTED)
set_target_properties(
        avcodec
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavcodec.so)

add_library(avfilter SHARED IMPORTED)
set_target_properties(
        avfilter
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavfilter.so)

add_library(avformat SHARED IMPORTED)
set_target_properties(
        avformat
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavformat.so)

add_library(avutil SHARED IMPORTED)
set_target_properties(
        avutil
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavutil.so)

add_library(avdevice SHARED IMPORTED)
set_target_properties(
        avdevice
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavdevice.so)

add_library(postproc SHARED IMPORTED)
set_target_properties(
        postproc
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libpostproc.so)

add_library(swresample SHARED IMPORTED)
set_target_properties(
        swresample
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libswresample.so)

add_library(swscale SHARED IMPORTED)
set_target_properties(
        swscale
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libswscale.so)


find_library(
              log-lib
              log )


target_link_libraries(
                       native-lib
                       avcodec
                       avfilter
                       avformat
                       avutil
                       avdevice
                       postproc
                       swresample
                       swscale
                       OpenSLES
                        android
                       ${log-lib} )