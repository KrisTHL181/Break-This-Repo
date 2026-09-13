package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

@FunctionalInterface
public interface PoolAliasLookup {
   PoolAliasLookup EMPTY = key -> key;

   ResourceKey<StructureTemplatePool> lookup(final ResourceKey<StructureTemplatePool> alias);

   static PoolAliasLookup create(final List<PoolAliasBinding> poolAliasBindings, final BlockPos pos, final long seed) {
      if (poolAliasBindings.isEmpty()) {
         return EMPTY;
      }

      RandomSource random = RandomSource.create(seed).forkPositional().at(pos);
      Builder<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> builder = ImmutableMap.builder();
      poolAliasBindings.forEach(binding -> binding.forEachResolved(random, builder::put));
      Map<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> aliasMappings = builder.build();
      return resourceKey -> Objects.requireNonNull(
         aliasMappings.getOrDefault(resourceKey, resourceKey), () -> "alias " + resourceKey.identifier() + " was mapped to null value"
      );
   }
}
