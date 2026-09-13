package net.minecraft.data.worldgen;

import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class VillagePools {
   public static void bootstrap(BootstrapContext<StructureTemplatePool> p_334012_) {
      PlainVillagePools.bootstrap(p_334012_);
      SnowyVillagePools.bootstrap(p_334012_);
      SavannaVillagePools.bootstrap(p_334012_);
      DesertVillagePools.bootstrap(p_334012_);
      TaigaVillagePools.bootstrap(p_334012_);
   }
}
