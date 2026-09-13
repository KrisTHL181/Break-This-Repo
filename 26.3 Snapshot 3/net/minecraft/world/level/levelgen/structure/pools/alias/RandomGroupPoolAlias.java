package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public record RandomGroupPoolAlias(WeightedList<List<PoolAliasBinding>> groups) implements PoolAliasBinding {
   public static final MapCodec<RandomGroupPoolAlias> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(WeightedList.nonEmptyCodec(Codec.list(PoolAliasBinding.CODEC)).fieldOf("groups").forGetter(RandomGroupPoolAlias::groups))
         .apply(i, RandomGroupPoolAlias::new)
   );

   @Override
   public void forEachResolved(
      final RandomSource random, final BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> aliasAndTargetConsumer
   ) {
      this.groups.getRandomOrThrow(random).forEach(binding -> binding.forEachResolved(random, aliasAndTargetConsumer));
   }

   @Override
   public Stream<ResourceKey<StructureTemplatePool>> allTargets() {
      return this.groups.unwrap().stream().flatMap(weightedEntry -> weightedEntry.value().stream()).flatMap(PoolAliasBinding::allTargets);
   }

   @Override
   public MapCodec<RandomGroupPoolAlias> codec() {
      return CODEC;
   }
}
