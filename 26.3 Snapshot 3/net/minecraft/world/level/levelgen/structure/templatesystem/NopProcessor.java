package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;

public class NopProcessor implements StructureProcessor {
   public static final MapCodec<NopProcessor> MAP_CODEC = MapCodec.unit(() -> NopProcessor.INSTANCE);
   public static final NopProcessor INSTANCE = new NopProcessor();

   private NopProcessor() {
   }

   @Override
   public MapCodec<NopProcessor> codec() {
      return MAP_CODEC;
   }
}
