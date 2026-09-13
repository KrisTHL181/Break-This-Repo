package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class PoolAliasBindings {
   public static MapCodec<? extends PoolAliasBinding> bootstrap(Registry<MapCodec<? extends PoolAliasBinding>> p_311587_) {
      Registry.register(p_311587_, "random", RandomPoolAlias.CODEC);
      Registry.register(p_311587_, "random_group", RandomGroupPoolAlias.CODEC);
      return Registry.register(p_311587_, "direct", DirectPoolAlias.CODEC);
   }

   public static void registerTargetsAsPools(
      BootstrapContext<StructureTemplatePool> p_330797_, Holder<StructureTemplatePool> p_311163_, List<PoolAliasBinding> p_310821_
   ) {
      p_310821_.stream()
         .flatMap(PoolAliasBinding::allTargets)
         .map(p_450040_ -> p_450040_.identifier().getPath())
         .forEach(
            p_327483_ -> Pools.register(
               p_330797_,
               p_327483_,
               new StructureTemplatePool(p_311163_, List.of(Pair.of(StructurePoolElement.single(p_327483_), 1)), StructureTemplatePool.Projection.RIGID)
            )
         );
   }
}
