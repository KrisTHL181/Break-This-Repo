package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.village.poi.PoiType;

public class PoiTypeTags {
   public static final TagKey<PoiType> ACQUIRABLE_JOB_SITE = create("acquirable_job_site");
   public static final TagKey<PoiType> VILLAGE = create("village");
   public static final TagKey<PoiType> BEE_HOME = create("bee_home");

   private PoiTypeTags() {
   }

   private static TagKey<PoiType> create(String p_215881_) {
      return TagKey.create(Registries.POINT_OF_INTEREST_TYPE, Identifier.withDefaultNamespace(p_215881_));
   }
}
