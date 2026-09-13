package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.timeline.Timeline;

public interface TimelineTags {
   TagKey<Timeline> UNIVERSAL = create("universal");
   TagKey<Timeline> IN_OVERWORLD = create("in_overworld");
   TagKey<Timeline> IN_NETHER = create("in_nether");
   TagKey<Timeline> IN_END = create("in_end");

   private static TagKey<Timeline> create(String p_452380_) {
      return TagKey.create(Registries.TIMELINE, Identifier.withDefaultNamespace(p_452380_));
   }
}
