cmake_minimum_required(VERSION 3.22.1)
project("vulkancube")

add_library(vulkancube SHARED vulkan_main.cpp)

Link hardware libraries specifically needed for Native Vulkan drawing on Android

target_link_libraries(vulkancube android log vulkan)