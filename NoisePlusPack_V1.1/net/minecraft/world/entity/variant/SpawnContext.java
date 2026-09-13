package net.minecraft.world.entity.variant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

public record SpawnContext(BlockPos pos, ServerLevelAccessor level, EnvironmentAttributeReader environmentAttributes, Holder<Biome> biome) {
   public static SpawnContext create(ServerLevelAccessor p_391608_, BlockPos p_394119_) {
      Holder<Biome> holder = p_391608_.getBiome(p_394119_);
      return new SpawnContext(p_394119_, p_391608_, p_391608_.environmentAttributes(), holder);
   }
}
