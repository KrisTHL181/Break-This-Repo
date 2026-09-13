package net.minecraft.world.entity.variant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

public record SpawnContext(BlockPos pos, ServerLevelAccessor level, EnvironmentAttributeReader environmentAttributes, Holder<Biome> biome) {
   public static SpawnContext create(final ServerLevelAccessor level, final BlockPos pos) {
      Holder<Biome> biome = level.getBiome(pos);
      return new SpawnContext(pos, level, level.environmentAttributes(), biome);
   }
}
