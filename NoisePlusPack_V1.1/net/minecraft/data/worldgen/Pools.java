package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Pools {
   public static final ResourceKey<StructureTemplatePool> EMPTY = createKey("empty");

   public static ResourceKey<StructureTemplatePool> createKey(Identifier p_455872_) {
      return ResourceKey.create(Registries.TEMPLATE_POOL, p_455872_);
   }

   public static ResourceKey<StructureTemplatePool> createKey(String p_256439_) {
      return createKey(Identifier.withDefaultNamespace(p_256439_));
   }

   public static ResourceKey<StructureTemplatePool> parseKey(String p_344725_) {
      return createKey(Identifier.parse(p_344725_));
   }

   public static void register(BootstrapContext<StructureTemplatePool> p_335139_, String p_255837_, StructureTemplatePool p_256161_) {
      p_335139_.register(createKey(p_255837_), p_256161_);
   }

   public static void bootstrap(BootstrapContext<StructureTemplatePool> p_332528_) {
      HolderGetter<StructureTemplatePool> holdergetter = p_332528_.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> holder = holdergetter.getOrThrow(EMPTY);
      p_332528_.register(EMPTY, new StructureTemplatePool(holder, ImmutableList.of(), StructureTemplatePool.Projection.RIGID));
      BastionPieces.bootstrap(p_332528_);
      PillagerOutpostPools.bootstrap(p_332528_);
      VillagePools.bootstrap(p_332528_);
      AncientCityStructurePieces.bootstrap(p_332528_);
      TrailRuinsStructurePools.bootstrap(p_332528_);
      TrialChambersStructurePools.bootstrap(p_332528_);
   }
}
