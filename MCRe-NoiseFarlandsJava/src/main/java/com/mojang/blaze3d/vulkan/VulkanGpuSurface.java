package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.SurfaceException;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkMemoryBarrier2;
import org.lwjgl.vulkan.VkOffset3D;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR.Buffer;

@OnlyIn(Dist.CLIENT)
public class VulkanGpuSurface implements GpuSurfaceBackend {
    private static final int NO_CURRENT_IMAGE = -1;
    private final VulkanDevice device;
    private final VkQueue presentQueue;
    private final long surface;
    private final int swapchainImageFormat;
    private long swapchain;
    private int swapchainWidth;
    private int swapchainHeight;
    private final LongList swapchainImages = new LongArrayList();
    private final long[] acquireSemaphores = new long[2];
    private int currentAcquireSemaphore = 0;
    private long[] presentSemaphores = LongArrays.EMPTY_ARRAY;
    private int currentImageIndex = -1;
    private @Nullable SurfaceException eatenException = null;
    private boolean swapchainSuboptimal;
    private boolean swapchainOutOfDate;
    private final Set<GpuSurface.PresentMode> supportedPresentModes;

    public VulkanGpuSurface(final VulkanDevice device, final long windowHandle) {
        this.device = device;
        this.presentQueue = device.graphicsQueue().vkQueue();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer handlePtr = stack.longs(0L);
            VulkanUtils.crashIfFailure(
                device,
                GLFWVulkan.glfwCreateWindowSurface(device.vkDevice().getPhysicalDevice().getInstance(), windowHandle, null, handlePtr),
                "Failed to create window surface"
            );
            this.surface = handlePtr.get(0);
            IntBuffer countPtr = stack.callocInt(1);
            VulkanUtils.crashIfFailure(
                device,
                KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device.vkDevice().getPhysicalDevice(), this.surface, countPtr, null),
                "Failed to enumerate surface present modes"
            );
            int presentModeCount = countPtr.get(0);
            IntBuffer presentModes = stack.callocInt(presentModeCount);
            VulkanUtils.crashIfFailure(
                device,
                KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device.vkDevice().getPhysicalDevice(), this.surface, countPtr, presentModes),
                "Failed to enumerate surface present modes"
            );
            this.supportedPresentModes = Collections.unmodifiableSet(this.convertPresentModes(presentModes));
            IntBuffer formatCount = stack.callocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.vkDevice().getPhysicalDevice(), this.surface, formatCount, null);
            Buffer formatsBuffer = VkSurfaceFormatKHR.calloc(formatCount.get(0));
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.vkDevice().getPhysicalDevice(), this.surface, formatCount, formatsBuffer);
            this.swapchainImageFormat = this.pickSwapchainSurfaceFormat(formatsBuffer).format();
            MemoryUtil.memFree(formatsBuffer);
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
            LongBuffer semaphoreHandlePtr = stack.callocLong(1);

            for (int i = 0; i < this.acquireSemaphores.length; i++) {
                VulkanUtils.crashIfFailure(
                    device, VK12.vkCreateSemaphore(device.vkDevice(), semaphoreCreateInfo, null, semaphoreHandlePtr), "Failed to create VkSemaphore(binary)"
                );
                this.acquireSemaphores[i] = semaphoreHandlePtr.get(0);
            }
        }
    }

    private Set<GpuSurface.PresentMode> convertPresentModes(final IntBuffer presentModes) {
        Set<GpuSurface.PresentMode> result = EnumSet.noneOf(GpuSurface.PresentMode.class);

        for (int i = 0; i < presentModes.limit(); i++) {
            int mode = presentModes.get(i);
            switch (mode) {
                case 0:
                    result.add(GpuSurface.PresentMode.IMMEDIATE);
                    break;
                case 1:
                    result.add(GpuSurface.PresentMode.MAILBOX);
                    break;
                case 2:
                    result.add(GpuSurface.PresentMode.FIFO);
                    break;
                case 3:
                    result.add(GpuSurface.PresentMode.FIFO_RELAXED);
            }
        }

        return result;
    }

    @Override
    public Collection<GpuSurface.PresentMode> supportedPresentModes() {
        return this.supportedPresentModes;
    }

    public VkSurfaceFormatKHR pickSwapchainSurfaceFormat(final Buffer formats) {
        for (VkSurfaceFormatKHR format : formats) {
            if (format.colorSpace() == 0 && (format.format() == 37 || format.format() == 44)) {
                return format;
            }
        }

        throw new IllegalStateException("Could not find compatible swapchain format");
    }

    public static void throwIfFailure(final int result, final String message) throws SurfaceException {
        if (result < 0) {
            throw new SurfaceException(VulkanUtils.resultToString(result) + ": " + message);
        }
    }

    @Override
    public void close() {
        this.destroySwapchain();

        for (int i = 0; i < this.acquireSemaphores.length; i++) {
            VK12.vkDestroySemaphore(this.device.vkDevice(), this.acquireSemaphores[i], null);
        }

        KHRSurface.vkDestroySurfaceKHR(this.device.instance().vkInstance(), this.surface, null);
    }

    private void destroySwapchain() {
        if (this.swapchain != 0L) {
            this.device.graphicsQueue().waitIdle();
            KHRSwapchain.vkDestroySwapchainKHR(this.device.vkDevice(), this.swapchain, null);

            for (int i = 0; i < this.presentSemaphores.length; i++) {
                VK12.vkDestroySemaphore(this.device.vkDevice(), this.presentSemaphores[i], null);
            }

            this.presentSemaphores = LongArrays.EMPTY_ARRAY;
            this.swapchain = 0L;
        }
    }

    @Override
    public void configure(final GpuSurface.Configuration config) throws SurfaceException {
        this.destroySwapchain();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            throwIfFailure(
                KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(this.device.vkDevice().getPhysicalDevice(), this.surface, surfaceCapabilities),
                "Failed to get surface capabilities"
            );
            VkExtent2D minExtent = surfaceCapabilities.minImageExtent();
            VkExtent2D maxExtent = surfaceCapabilities.maxImageExtent();
            if (config.width() < minExtent.width()
                || maxExtent.width() < config.width()
                || config.height() < minExtent.height()
                || maxExtent.height() < config.height()) {
                throw new SurfaceException(
                    String.format(
                        Locale.ROOT,
                        "Requested swapchain extent (%d x %d) not within allowed extent: min(%d x %d) max(%d x %d)",
                        config.width(),
                        config.height(),
                        minExtent.width(),
                        minExtent.height(),
                        maxExtent.width(),
                        maxExtent.height()
                    )
                );
            }

            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack).sType$Default();
            swapchainCreateInfo.surface(this.surface);
            int currentPresentMode = VulkanConst.toVk(config.presentMode());
            int requestedImageCount = 3;
            swapchainCreateInfo.minImageCount(Math.max(3, surfaceCapabilities.minImageCount()));
            swapchainCreateInfo.imageFormat(this.swapchainImageFormat);
            swapchainCreateInfo.imageColorSpace(0);
            swapchainCreateInfo.imageExtent(VkExtent2D.calloc(stack).set(config.width(), config.height()));
            swapchainCreateInfo.imageArrayLayers(1);
            swapchainCreateInfo.imageUsage(2);
            swapchainCreateInfo.imageSharingMode(0);
            swapchainCreateInfo.preTransform(surfaceCapabilities.currentTransform());
            swapchainCreateInfo.compositeAlpha(1);
            swapchainCreateInfo.presentMode(currentPresentMode);
            swapchainCreateInfo.clipped(true);
            LongBuffer swapchainPtr = stack.callocLong(1);
            throwIfFailure(KHRSwapchain.vkCreateSwapchainKHR(this.device.vkDevice(), swapchainCreateInfo, null, swapchainPtr), "Failed to create Swapchain");
            this.swapchain = swapchainPtr.get(0);
            IntBuffer imageCountPtr = stack.callocInt(1);
            throwIfFailure(
                KHRSwapchain.vkGetSwapchainImagesKHR(this.device.vkDevice(), this.swapchain, imageCountPtr, null), "Failed to get swapchain image count"
            );
            int swapchainImageCount = imageCountPtr.get(0);
            LongBuffer swapchainImagesPtr = stack.callocLong(swapchainImageCount);
            throwIfFailure(
                KHRSwapchain.vkGetSwapchainImagesKHR(this.device.vkDevice(), this.swapchain, imageCountPtr, swapchainImagesPtr),
                "Failed to get swapchain images"
            );
            this.swapchainImages.clear();

            for (int i = 0; i < swapchainImageCount; i++) {
                this.swapchainImages.add(swapchainImagesPtr.get(i));
            }

            this.presentSemaphores = new long[swapchainImageCount];
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
            LongBuffer semaphoreHandlePtr = stack.callocLong(1);

            for (int i = 0; i < this.presentSemaphores.length; i++) {
                throwIfFailure(
                    VK12.vkCreateSemaphore(this.device.vkDevice(), semaphoreCreateInfo, null, semaphoreHandlePtr), "Failed to create VkSemaphore(binary)"
                );
                this.presentSemaphores[i] = semaphoreHandlePtr.get(0);
            }

            this.swapchainSuboptimal = false;
            this.swapchainOutOfDate = false;
            this.swapchainWidth = config.width();
            this.swapchainHeight = config.height();
        } catch (SurfaceException e) {
            this.swapchainSuboptimal = true;
            this.swapchainOutOfDate = true;
            throw e;
        }
    }

    @Override
    public boolean isSuboptimal() {
        return this.swapchainSuboptimal;
    }

    @Override
    public void acquireNextTexture() throws SurfaceException {
        if (this.eatenException != null) {
            SurfaceException toThrow = this.eatenException;
            this.eatenException = null;
            throw new SurfaceException(toThrow);
        }

        if (this.swapchainOutOfDate) {
            throw new IllegalStateException("Attempt to use out of date swapchain");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer frameIndexPtr = stack.callocInt(1);
            frameIndexPtr.put(0, -1);
            this.currentAcquireSemaphore++;
            this.currentAcquireSemaphore = this.currentAcquireSemaphore % this.acquireSemaphores.length;
            long acquireSemaphore = this.acquireSemaphores[this.currentAcquireSemaphore];
            int result = KHRSwapchain.vkAcquireNextImageKHR(this.device.vkDevice(), this.swapchain, 5000000000L, acquireSemaphore, 0L, frameIndexPtr);
            if (result == 2) {
                List<CheckpointExtension.QueueCheckpoints> checkpoints = this.device.checkpointExtension().retrieveCheckpoints(false);
                throw new IllegalStateException("GPU timeout attempting to acquire next frame: " + VulkanUtils.formatCheckpoints(checkpoints));
            }

            this.currentImageIndex = frameIndexPtr.get(0);
            if (result == -1000001004) {
                this.swapchainSuboptimal = true;
                this.swapchainOutOfDate = true;
                throw new SurfaceException("Failed to acquire image, swapchain out of date");
            }

            if (result == 1000001003) {
                this.swapchainSuboptimal = true;
            } else {
                VulkanUtils.crashIfFailure(this.device, result, "Failed to acquire image");
            }
        }
    }

    @Override
    public void blitFromTexture(final CommandEncoderBackend commandEncoder, final GpuTextureView textureView) {
        if (this.swapchainOutOfDate) {
            throw new IllegalStateException("Attempt to use out of date swapchain");
        }

        assert this.currentImageIndex != -1;
        VulkanCommandEncoder vulkanCommandEncoder = (VulkanCommandEncoder)commandEncoder;
        VkCommandBuffer blitCommandBuffer = vulkanCommandEncoder.allocateAndBeginTransientCommandBuffer();
        long swapchainImage = this.swapchainImages.getLong(this.currentImageIndex);
        MemoryStack stack = MemoryStack.stackGet();

        try (MemoryStack var8 = stack.push()) {
            org.lwjgl.vulkan.VkImageMemoryBarrier2.Buffer imageBarrier = VkImageMemoryBarrier2.calloc(1, stack).sType$Default();
            imageBarrier.srcStageMask(0L);
            imageBarrier.srcAccessMask(0L);
            imageBarrier.dstStageMask(4096L);
            imageBarrier.dstAccessMask(4096L);
            imageBarrier.oldLayout(0);
            imageBarrier.newLayout(7);
            imageBarrier.srcQueueFamilyIndex(-1);
            imageBarrier.dstQueueFamilyIndex(-1);
            imageBarrier.image(swapchainImage);
            VkImageSubresourceRange subresourceRange = imageBarrier.subresourceRange();
            subresourceRange.aspectMask(1);
            subresourceRange.baseMipLevel(0);
            subresourceRange.levelCount(1);
            subresourceRange.baseArrayLayer(0);
            subresourceRange.layerCount(1);
            VkDependencyInfo depinfo = VkDependencyInfo.calloc(stack).sType$Default();
            depinfo.pImageMemoryBarriers(imageBarrier);
            KHRSynchronization2.vkCmdPipelineBarrier2KHR(blitCommandBuffer, depinfo);
        }

        try (MemoryStack var22 = stack.push()) {
            int copyWidth = Math.min(this.swapchainWidth, textureView.getWidth(0));
            int copyHeight = Math.min(this.swapchainHeight, textureView.getHeight(0));
            org.lwjgl.vulkan.VkOffset3D.Buffer srcOffsets = VkOffset3D.calloc(2, stack);
            srcOffsets.x(0).y(0).z(0);
            srcOffsets.position(1);
            srcOffsets.x(copyWidth).y(copyHeight).z(1);
            srcOffsets.position(0);
            org.lwjgl.vulkan.VkOffset3D.Buffer dstOffsets = VkOffset3D.calloc(2, stack);
            dstOffsets.x(0).y(copyHeight).z(0);
            dstOffsets.position(1);
            dstOffsets.x(copyWidth).y(0).z(1);
            dstOffsets.position(0);
            VkImageSubresourceLayers srcSubresource = VkImageSubresourceLayers.calloc(stack);
            srcSubresource.aspectMask(1);
            srcSubresource.mipLevel(textureView.baseMipLevel());
            srcSubresource.baseArrayLayer(0);
            srcSubresource.layerCount(1);
            VkImageSubresourceLayers dstSubresource = VkImageSubresourceLayers.calloc(stack);
            dstSubresource.aspectMask(1);
            dstSubresource.mipLevel(0);
            dstSubresource.baseArrayLayer(0);
            dstSubresource.layerCount(1);
            org.lwjgl.vulkan.VkImageBlit.Buffer blitRegion = VkImageBlit.calloc(1, stack);
            blitRegion.srcSubresource(srcSubresource);
            blitRegion.srcOffsets(srcOffsets);
            blitRegion.dstSubresource(dstSubresource);
            blitRegion.dstOffsets(dstOffsets);
            VK12.vkCmdBlitImage(blitCommandBuffer, ((VulkanGpuTexture)textureView.texture()).vkImage(), 1, swapchainImage, 7, blitRegion, 0);
        }

        try (MemoryStack var23 = stack.push()) {
            org.lwjgl.vulkan.VkImageMemoryBarrier2.Buffer imageBarrier = VkImageMemoryBarrier2.calloc(1, stack).sType$Default();
            imageBarrier.srcStageMask(4096L);
            imageBarrier.srcAccessMask(4096L);
            imageBarrier.dstStageMask(65536L);
            imageBarrier.dstAccessMask(0L);
            imageBarrier.oldLayout(7);
            imageBarrier.newLayout(1000001002);
            imageBarrier.srcQueueFamilyIndex(-1);
            imageBarrier.dstQueueFamilyIndex(-1);
            imageBarrier.image(swapchainImage);
            VkImageSubresourceRange subresourceRange = imageBarrier.subresourceRange();
            subresourceRange.aspectMask(1);
            subresourceRange.baseMipLevel(0);
            subresourceRange.levelCount(1);
            subresourceRange.baseArrayLayer(0);
            subresourceRange.layerCount(1);
            org.lwjgl.vulkan.VkMemoryBarrier2.Buffer memoryBarrier = VkMemoryBarrier2.calloc(1, stack).sType$Default();
            memoryBarrier.srcStageMask(4096L);
            memoryBarrier.srcAccessMask(2048L);
            memoryBarrier.dstStageMask(65536L);
            memoryBarrier.dstAccessMask(98304L);
            VkDependencyInfo depinfo = VkDependencyInfo.calloc(stack).sType$Default();
            depinfo.pMemoryBarriers(memoryBarrier);
            depinfo.pImageMemoryBarriers(imageBarrier);
            KHRSynchronization2.vkCmdPipelineBarrier2KHR(blitCommandBuffer, depinfo);
        }

        VulkanUtils.crashIfFailure(this.device, VK12.vkEndCommandBuffer(blitCommandBuffer), "Failed to end VkCommandBuffer");
        vulkanCommandEncoder.waitSemaphore(this.acquireSemaphores[this.currentAcquireSemaphore], 0L, 65536L);
        vulkanCommandEncoder.execute(blitCommandBuffer);
        vulkanCommandEncoder.signalSemaphore(this.presentSemaphores[this.currentImageIndex], 0L, 4096L);
    }

    @Override
    public void present() {
        if (this.swapchainOutOfDate) {
            throw new IllegalStateException("Attempt to use out of date swapchain");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack).sType$Default();
            presentInfo.pWaitSemaphores(stack.longs(this.presentSemaphores[this.currentImageIndex]));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(this.swapchain));
            presentInfo.pImageIndices(stack.ints(this.currentImageIndex));
            this.currentImageIndex = -1;
            int result = KHRSwapchain.vkQueuePresentKHR(this.presentQueue, presentInfo);
            if (result == -1000001004) {
                this.swapchainSuboptimal = true;
                this.swapchainOutOfDate = true;
                this.eatenException = new SurfaceException("Failed to present image, swapchain out of date");
            } else if (result == 1000001003) {
                this.swapchainSuboptimal = true;
            } else {
                VulkanUtils.crashIfFailure(this.device, result, "Failed to present image");
            }
        }
    }
}