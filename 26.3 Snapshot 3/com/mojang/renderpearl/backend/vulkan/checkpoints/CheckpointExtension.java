package com.mojang.renderpearl.backend.vulkan.checkpoints;

import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import java.util.List;
import java.util.function.Supplier;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface CheckpointExtension extends AutoCloseable {
   CheckpointExtension.CheckpointStorage createStorage(VulkanDevice device, VulkanQueue queue, int maxFramesInFlight);

   List<CheckpointExtension.QueueCheckpoints> retrieveCheckpoints(boolean isDeviceLost);

   @Override
   void close();

   interface CheckpointStorage {
      void rotate();

      void recordCheckpoint(VkCommandBuffer commandBuffer, CheckpointExtension.CheckpointType type, Supplier<String> label);
   }

   enum CheckpointType {
      BEGIN_RENDER_PASS,
      END_RENDER_PASS;
   }

   record QueueCheckpoints(long queue, List<CheckpointExtension.StageCheckpoint> checkpoints) {
   }

   record StageCheckpoint(long stage, CheckpointExtension.CheckpointType type, String label) {
   }
}
