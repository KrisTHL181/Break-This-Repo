package net.minecraft.client.renderer;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionBufferBuilderPool {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Queue<SectionBufferBuilderPack> freeBuffers;
   private volatile int freeBufferCount;

   private SectionBufferBuilderPool(List<SectionBufferBuilderPack> p_312374_) {
      this.freeBuffers = Queues.newArrayDeque(p_312374_);
      this.freeBufferCount = this.freeBuffers.size();
   }

   public static SectionBufferBuilderPool allocate(int p_310783_) {
      int i = Math.max(1, (int)(Runtime.getRuntime().maxMemory() * 0.3) / SectionBufferBuilderPack.TOTAL_BUFFERS_SIZE);
      int j = Math.max(1, Math.min(p_310783_, i));
      List<SectionBufferBuilderPack> list = new ArrayList<>(j);

      try {
         for (int k = 0; k < j; k++) {
            list.add(new SectionBufferBuilderPack());
         }
      } catch (OutOfMemoryError outofmemoryerror) {
         LOGGER.warn("Allocated only {}/{} buffers", list.size(), j);
         int l = Math.min(list.size() * 2 / 3, list.size() - 1);

         for (int i1 = 0; i1 < l; i1++) {
            list.remove(list.size() - 1).close();
         }
      }

      return new SectionBufferBuilderPool(list);
   }

   public @Nullable SectionBufferBuilderPack acquire() {
      SectionBufferBuilderPack sectionbufferbuilderpack = this.freeBuffers.poll();
      if (sectionbufferbuilderpack != null) {
         this.freeBufferCount = this.freeBuffers.size();
         return sectionbufferbuilderpack;
      } else {
         return null;
      }
   }

   public void release(SectionBufferBuilderPack p_310220_) {
      this.freeBuffers.add(p_310220_);
      this.freeBufferCount = this.freeBuffers.size();
   }

   public boolean isEmpty() {
      return this.freeBuffers.isEmpty();
   }

   public int getFreeBufferCount() {
      return this.freeBufferCount;
   }
}
