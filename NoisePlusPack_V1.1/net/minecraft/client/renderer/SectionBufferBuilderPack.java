package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.Arrays;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionBufferBuilderPack implements AutoCloseable {
   public static final int TOTAL_BUFFERS_SIZE = Arrays.stream(ChunkSectionLayer.values()).mapToInt(ChunkSectionLayer::bufferSize).sum();
   private final Map<ChunkSectionLayer, ByteBufferBuilder> buffers = Util.makeEnumMap(
      ChunkSectionLayer.class, p_404984_ -> new ByteBufferBuilder(p_404984_.bufferSize())
   );

   public ByteBufferBuilder buffer(ChunkSectionLayer p_405919_) {
      return this.buffers.get(p_405919_);
   }

   public void clearAll() {
      this.buffers.values().forEach(ByteBufferBuilder::clear);
   }

   public void discardAll() {
      this.buffers.values().forEach(ByteBufferBuilder::discard);
   }

   @Override
   public void close() {
      this.buffers.values().forEach(ByteBufferBuilder::close);
   }
}
