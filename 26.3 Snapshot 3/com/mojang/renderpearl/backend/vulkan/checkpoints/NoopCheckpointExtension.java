package com.mojang.renderpearl.backend.vulkan.checkpoints;

import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import java.util.List;
import java.util.function.Supplier;
import org.lwjgl.vulkan.VkCommandBuffer;

public class NoopCheckpointExtension implements CheckpointExtension {
   public static final NoopCheckpointExtension INSTANCE = new NoopCheckpointExtension();

   @Override
   public CheckpointExtension.CheckpointStorage createStorage(final VulkanDevice device, final VulkanQueue queue, final int maxFramesInFlight) {
      return NoopCheckpointExtension.NoopCheckpointStorage.INSTANCE;
   }

   @Override
   public List<CheckpointExtension.QueueCheckpoints> retrieveCheckpoints(final boolean isDeviceLost) {
      return List.of();
   }

   @Override
   public void close() {
   }

   private static class NoopCheckpointStorage implements CheckpointExtension.CheckpointStorage {
      private static final NoopCheckpointExtension.NoopCheckpointStorage INSTANCE = new NoopCheckpointExtension.NoopCheckpointStorage();

      @Override
      public void rotate() {
      }

      @Override
      public void recordCheckpoint(final VkCommandBuffer commandBuffer, final CheckpointExtension.CheckpointType type, final Supplier<String> label) {
      }
   }
}
