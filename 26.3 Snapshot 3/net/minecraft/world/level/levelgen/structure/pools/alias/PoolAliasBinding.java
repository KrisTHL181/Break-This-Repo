package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public interface PoolAliasBinding {
   Codec<PoolAliasBinding> CODEC = BuiltInRegistries.POOL_ALIAS_BINDING_TYPE.byNameCodec().dispatch(PoolAliasBinding::codec, Function.identity());

   void forEachResolved(
      final RandomSource random, final BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> aliasAndTargetConsumer
   );

   Stream<ResourceKey<StructureTemplatePool>> allTargets();

   static DirectPoolAlias direct(final String id, final String target) {
      return direct(Pools.createKey(id), Pools.createKey(target));
   }

   static DirectPoolAlias direct(final ResourceKey<StructureTemplatePool> alias, final ResourceKey<StructureTemplatePool> target) {
      return new DirectPoolAlias(alias, target);
   }

   static RandomPoolAlias random(final String id, final WeightedList<String> targets) {
      WeightedList.Builder<ResourceKey<StructureTemplatePool>> targetPools = WeightedList.builder();
      targets.unwrap().forEach(wrapper -> targetPools.add(Pools.createKey(wrapper.value()), wrapper.weight()));
      return random(Pools.createKey(id), targetPools.build());
   }

   static RandomPoolAlias random(final ResourceKey<StructureTemplatePool> id, final WeightedList<ResourceKey<StructureTemplatePool>> targets) {
      return new RandomPoolAlias(id, targets);
   }

   static RandomGroupPoolAlias randomGroup(final WeightedList<List<PoolAliasBinding>> combinations) {
      return new RandomGroupPoolAlias(combinations);
   }

   MapCodec<? extends PoolAliasBinding> codec();
}
