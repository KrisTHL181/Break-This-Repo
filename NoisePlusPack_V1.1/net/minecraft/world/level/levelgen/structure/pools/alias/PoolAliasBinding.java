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

   void forEachResolved(RandomSource var1, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> var2);

   Stream<ResourceKey<StructureTemplatePool>> allTargets();

   static DirectPoolAlias direct(String p_310882_, String p_311396_) {
      return direct(Pools.createKey(p_310882_), Pools.createKey(p_311396_));
   }

   static DirectPoolAlias direct(ResourceKey<StructureTemplatePool> p_311763_, ResourceKey<StructureTemplatePool> p_312427_) {
      return new DirectPoolAlias(p_311763_, p_312427_);
   }

   static RandomPoolAlias random(String p_311792_, WeightedList<String> p_395164_) {
      WeightedList.Builder<ResourceKey<StructureTemplatePool>> builder = WeightedList.builder();
      p_395164_.unwrap().forEach(p_391073_ -> builder.add(Pools.createKey(p_391073_.value()), p_391073_.weight()));
      return random(Pools.createKey(p_311792_), builder.build());
   }

   static RandomPoolAlias random(ResourceKey<StructureTemplatePool> p_311453_, WeightedList<ResourceKey<StructureTemplatePool>> p_391616_) {
      return new RandomPoolAlias(p_311453_, p_391616_);
   }

   static RandomGroupPoolAlias randomGroup(WeightedList<List<PoolAliasBinding>> p_391677_) {
      return new RandomGroupPoolAlias(p_391677_);
   }

   MapCodec<? extends PoolAliasBinding> codec();
}
