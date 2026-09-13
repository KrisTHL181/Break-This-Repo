package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TimelineTags;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.timeline.Timelines;

public class TimelineTagsProvider extends KeyTagProvider<Timeline> {
   public TimelineTagsProvider(PackOutput p_450824_, CompletableFuture<HolderLookup.Provider> p_450149_) {
      super(p_450824_, Registries.TIMELINE, p_450149_);
   }

   @Override
   protected void addTags(HolderLookup.Provider p_455008_) {
      this.tag(TimelineTags.UNIVERSAL).add(Timelines.VILLAGER_SCHEDULE);
      this.tag(TimelineTags.IN_OVERWORLD).addTag(TimelineTags.UNIVERSAL).add(Timelines.DAY, Timelines.MOON, Timelines.EARLY_GAME);
      this.tag(TimelineTags.IN_NETHER).addTag(TimelineTags.UNIVERSAL);
      this.tag(TimelineTags.IN_END).addTag(TimelineTags.UNIVERSAL);
   }
}
