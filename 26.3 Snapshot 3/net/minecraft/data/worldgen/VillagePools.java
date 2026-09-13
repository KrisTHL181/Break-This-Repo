package net.minecraft.data.worldgen;

import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class VillagePools {
   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      PlainVillagePools.bootstrap(context);
      SnowyVillagePools.bootstrap(context);
      SavannaVillagePools.bootstrap(context);
      DesertVillagePools.bootstrap(context);
      TaigaVillagePools.bootstrap(context);
   }
}
