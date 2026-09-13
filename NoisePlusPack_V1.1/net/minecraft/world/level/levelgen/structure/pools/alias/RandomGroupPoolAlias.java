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
   static MapCodec<RandomGroupPoolAlias> CODEC = RecordCodecBuilder.mapCodec(
      p_396543_ -> p_396543_.group(WeightedList.nonEmptyCodec(Codec.list(PoolAliasBinding.CODEC)).fieldOf("groups").forGetter(RandomGroupPoolAlias::groups))
         .apply(p_396543_, RandomGroupPoolAlias::new)
   );

   @Override
   public void forEachResolved(RandomSource p_392696_, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> p_394487_) {
      this.groups.getRandom(p_392696_).ifPresent(p_393046_ -> p_393046_.forEach(p_397892_ -> p_397892_.forEachResolved(p_392696_, p_394487_)));
   }

   @Override
   public Stream<ResourceKey<StructureTemplatePool>> allTargets() {
      return this.groups.unwrap().stream().flatMap(p_393614_ -> p_393614_.value().stream()).flatMap(PoolAliasBinding::allTargets);
   }

   @Override
   public MapCodec<RandomGroupPoolAlias> codec() {
      return CODEC;
   }
}
