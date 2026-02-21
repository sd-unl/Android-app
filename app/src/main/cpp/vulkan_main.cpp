#include <jni.h>
#include <android/native_window_jni.h>
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <android/log.h>
#include <vector>
#include <cmath>
#include <chrono>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VulkanJNI", VA_ARGS)

VkInstance instance = VK_NULL_HANDLE;
VkSurfaceKHR surface = VK_NULL_HANDLE;
VkDevice device = VK_NULL_HANDLE;
VkSwapchainKHR swapchain = VK_NULL_HANDLE;
VkQueue queue = VK_NULL_HANDLE;
VkCommandPool cmdPool = VK_NULL_HANDLE;
std::vector<VkImage> swapchainImages;
std::vector<VkCommandBuffer> cmdBuffers;
VkSemaphore imageAvailableSemaphore = VK_NULL_HANDLE;
VkSemaphore renderFinishedSemaphore = VK_NULL_HANDLE;

ANativeWindow* window = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_helloworld_VulkanActivity_initVulkan(JNIEnv* env, jobject thiz, jobject jSurface) {
window = ANativeWindow_fromSurface(env, jSurface);
if (!window) return JNI_FALSE;

code
Code
download
content_copy
expand_less
const char* extNames[] = { "VK_KHR_surface", "VK_KHR_android_surface" };
VkInstanceCreateInfo instInfo = {};
instInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
instInfo.enabledExtensionCount = 2;
instInfo.ppEnabledExtensionNames = extNames;
if (vkCreateInstance(&instInfo, nullptr, &instance) != VK_SUCCESS) return JNI_FALSE;

VkAndroidSurfaceCreateInfoKHR surfaceInfo = {};
surfaceInfo.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
surfaceInfo.window = window;
auto fpCreateAndroidSurfaceKHR = (PFN_vkCreateAndroidSurfaceKHR)vkGetInstanceProcAddr(instance, "vkCreateAndroidSurfaceKHR");
if (!fpCreateAndroidSurfaceKHR || fpCreateAndroidSurfaceKHR(instance, &surfaceInfo, nullptr, &surface) != VK_SUCCESS) return JNI_FALSE;

uint32_t gpuCount = 0;
vkEnumeratePhysicalDevices(instance, &gpuCount, nullptr);
if (gpuCount == 0) return JNI_FALSE;
std::vector<VkPhysicalDevice> gpus(gpuCount);
vkEnumeratePhysicalDevices(instance, &gpuCount, gpus.data());
VkPhysicalDevice physicalDevice = gpus[0];

const char* devExtNames[] = { "VK_KHR_swapchain" };
float queuePriority = 1.0f;
VkDeviceQueueCreateInfo queueInfo = {VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
queueInfo.queueFamilyIndex = 0; 
queueInfo.queueCount = 1;
queueInfo.pQueuePriorities = &queuePriority;

VkDeviceCreateInfo deviceInfo = {VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
deviceInfo.queueCreateInfoCount = 1;
deviceInfo.pQueueCreateInfos = &queueInfo;
deviceInfo.enabledExtensionCount = 1;
deviceInfo.ppEnabledExtensionNames = devExtNames;
if (vkCreateDevice(physicalDevice, &deviceInfo, nullptr, &device) != VK_SUCCESS) return JNI_FALSE;

vkGetDeviceQueue(device, 0, 0, &queue);

VkSwapchainCreateInfoKHR swapchainInfo = {VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR};
swapchainInfo.surface = surface;
swapchainInfo.minImageCount = 2;
swapchainInfo.imageFormat = VK_FORMAT_R8G8B8A8_UNORM;
swapchainInfo.imageColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
swapchainInfo.imageExtent.width = ANativeWindow_getWidth(window);
swapchainInfo.imageExtent.height = ANativeWindow_getHeight(window);
swapchainInfo.imageArrayLayers = 1;
swapchainInfo.imageUsage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
swapchainInfo.preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
swapchainInfo.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
swapchainInfo.presentMode = VK_PRESENT_MODE_FIFO_KHR;
if (vkCreateSwapchainKHR(device, &swapchainInfo, nullptr, &swapchain) != VK_SUCCESS) return JNI_FALSE;

uint32_t imageCount = 0;
vkGetSwapchainImagesKHR(device, swapchain, &imageCount, nullptr);
swapchainImages.resize(imageCount);
vkGetSwapchainImagesKHR(device, swapchain, &imageCount, swapchainImages.data());

VkCommandPoolCreateInfo poolInfo = {VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO};
poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
poolInfo.queueFamilyIndex = 0;
vkCreateCommandPool(device, &poolInfo, nullptr, &cmdPool);

cmdBuffers.resize(imageCount);
VkCommandBufferAllocateInfo allocInfo = {VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO};
allocInfo.commandPool = cmdPool;
allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
allocInfo.commandBufferCount = imageCount;
vkAllocateCommandBuffers(device, &allocInfo, cmdBuffers.data());

VkSemaphoreCreateInfo semInfo = {VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
vkCreateSemaphore(device, &semInfo, nullptr, &imageAvailableSemaphore);
vkCreateSemaphore(device, &semInfo, nullptr, &renderFinishedSemaphore);

return JNI_TRUE;

}

void transitionImageLayout(VkCommandBuffer cmd, VkImage image, VkImageLayout oldLayout, VkImageLayout newLayout) {
VkImageMemoryBarrier barrier = {VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER};
barrier.oldLayout = oldLayout;
barrier.newLayout = newLayout;
barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
barrier.image = image;
barrier.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
barrier.subresourceRange.levelCount = 1;
barrier.subresourceRange.layerCount = 1;

code
Code
download
content_copy
expand_less
VkPipelineStageFlags srcStage, dstStage;
if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
    barrier.srcAccessMask = 0; barrier.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT; dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
} else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {
    barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT; barrier.dstAccessMask = 0;
    srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT; dstStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
} else return;

vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, 0, nullptr, 0, nullptr, 1, &barrier);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_helloworld_VulkanActivity_renderFrame(JNIEnv* env, jobject thiz) {
if (!device) return JNI_FALSE;

code
Code
download
content_copy
expand_less
uint32_t imageIndex;
if (vkAcquireNextImageKHR(device, swapchain, UINT64_MAX, imageAvailableSemaphore, VK_NULL_HANDLE, &imageIndex) != VK_SUCCESS) return JNI_FALSE;

VkCommandBuffer cmd = cmdBuffers[imageIndex];
VkCommandBufferBeginInfo beginInfo = {VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};
vkBeginCommandBuffer(cmd, &beginInfo);

transitionImageLayout(cmd, swapchainImages[imageIndex], VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

static auto startTime = std::chrono::high_resolution_clock::now();
auto currentTime = std::chrono::high_resolution_clock::now();
float time = std::chrono::duration<float, std::chrono::seconds::period>(currentTime - startTime).count();

VkClearColorValue clearColor = {{ std::abs(std::sin(time)), 0.1f, std::abs(std::cos(time)), 1.0f }};
VkImageSubresourceRange range = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
vkCmdClearColorImage(cmd, swapchainImages[imageIndex], VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, &clearColor, 1, &range);

transitionImageLayout(cmd, swapchainImages[imageIndex], VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
vkEndCommandBuffer(cmd);

VkSubmitInfo submitInfo = {VK_STRUCTURE_TYPE_SUBMIT_INFO};
VkPipelineStageFlags waitStages[] = {VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT};
submitInfo.waitSemaphoreCount = 1;
submitInfo.pWaitSemaphores = &imageAvailableSemaphore;
submitInfo.pWaitDstStageMask = waitStages;
submitInfo.commandBufferCount = 1;
submitInfo.pCommandBuffers = &cmd;
submitInfo.signalSemaphoreCount = 1;
submitInfo.pSignalSemaphores = &renderFinishedSemaphore;

vkQueueSubmit(queue, 1, &submitInfo, VK_NULL_HANDLE);

VkPresentInfoKHR presentInfo = {VK_STRUCTURE_TYPE_PRESENT_INFO_KHR};
presentInfo.waitSemaphoreCount = 1;
presentInfo.pWaitSemaphores = &renderFinishedSemaphore;
presentInfo.swapchainCount = 1;
presentInfo.pSwapchains = &swapchain;
presentInfo.pImageIndices = &imageIndex;

vkQueuePresentKHR(queue, &presentInfo);
vkQueueWaitIdle(queue); // Lazy Sync
return JNI_TRUE;

}

extern "C" JNIEXPORT void JNICALL
Java_com_example_helloworld_VulkanActivity_cleanupVulkan(JNIEnv* env, jobject thiz) {
if (device) {
vkDeviceWaitIdle(device);
vkDestroySemaphore(device, renderFinishedSemaphore, nullptr);
vkDestroySemaphore(device, imageAvailableSemaphore, nullptr);
vkFreeCommandBuffers(device, cmdPool, cmdBuffers.size(), cmdBuffers.data());
vkDestroyCommandPool(device, cmdPool, nullptr);
vkDestroySwapchainKHR(device, swapchain, nullptr);
vkDestroyDevice(device, nullptr);
device = VK_NULL_HANDLE;
}
if (instance) {
auto fpDestroyAndroidSurfaceKHR = (PFN_vkDestroySurfaceKHR)vkGetInstanceProcAddr(instance, "vkDestroySurfaceKHR");
if (fpDestroyAndroidSurfaceKHR) fpDestroyAndroidSurfaceKHR(instance, surface, nullptr);
vkDestroyInstance(instance, nullptr);
instance = VK_NULL_HANDLE;
}
if (window) {
ANativeWindow_release(window);
window = nullptr;
}
}