package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public record RandomPoolAlias(ResourceKey<StructureTemplatePool> alias, WeightedList<ResourceKey<StructureTemplatePool>> targets) implements PoolAliasBinding {
   static MapCodec<RandomPoolAlias> CODEC = RecordCodecBuilder.mapCodec(
      p_397352_ -> p_397352_.group(
            ResourceKey.codec(Registries.TEMPLATE_POOL).fieldOf("alias").forGetter(RandomPoolAlias::alias),
            WeightedList.nonEmptyCodec(ResourceKey.codec(Registries.TEMPLATE_POOL)).fieldOf("targets").forGetter(RandomPoolAlias::targets)
         )
         .apply(p_397352_, RandomPoolAlias::new)
   );

   @Override
   public void forEachResolved(RandomSource p_395984_, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> p_394323_) {
      this.targets.getRandom(p_395984_).ifPresent(p_397713_ -> p_394323_.accept(this.alias, (ResourceKey<StructureTemplatePool>)p_397713_));
   }

   @Override
   public Stream<ResourceKey<StructureTemplatePool>> allTargets() {
      return this.targets.unwrap().stream().map(Weighted::value);
   }

   @Override
   public MapCodec<RandomPoolAlias> codec() {
      return CODEC;
   }
}
