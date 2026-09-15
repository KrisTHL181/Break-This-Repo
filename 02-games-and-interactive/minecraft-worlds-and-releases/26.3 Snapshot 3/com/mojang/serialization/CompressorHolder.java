package com.mojang.serialization;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;

public abstract class CompressorHolder implements Compressable {
   private final Map<DynamicOps<?>, KeyCompressor<?>> compressors = new Object2ObjectArrayMap();

   @Override
   public <T> KeyCompressor<T> compressor(DynamicOps<T> ops) {
      return (KeyCompressor<T>)this.compressors.computeIfAbsent(ops, k -> new KeyCompressor<>(ops, this.keys(ops)));
   }
}
