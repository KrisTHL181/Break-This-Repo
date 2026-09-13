package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.Arrays;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.Util;

public class SectionBufferBuilderPack implements AutoCloseable {
   public static final int TOTAL_BUFFERS_SIZE = Arrays.stream(ChunkSectionLayer.values()).mapToInt(ChunkSectionLayer::bufferSize).sum();
   private final Map<ChunkSectionLayer, ByteBufferBuilder> buffers = Util.makeEnumMap(
      ChunkSectionLayer.class, layer -> new ByteBufferBuilder(layer.bufferSize())
   );

   public ByteBufferBuilder buffer(final ChunkSectionLayer layer) {
      return this.buffers.get(layer);
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
